package com.luna.lunasrv.module.lunaerp.service.finance.ledger.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.event.ShipmentAuditedEvent;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.event.ShipmentCancelledEvent;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.event.ShipmentItemSnapshotDTO;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 发货事件财务记账监听器（finance 域公共内核，ADR-0043，Q1）
 *
 * <p>
 * 监听 business.shipment 审核出库与红冲事件：
 * 1. 审核出库（ShipmentAuditedEvent）：同事务 BEFORE_COMMIT 在分录账追加 AR_INC 分录（逐明细行 + 运费头级行）；
 * 2. 红冲（ShipmentCancelledEvent）：同事务 BEFORE_COMMIT 追加 AR_INC_REVERSAL（reversal_of_id 回指原分录）；
 * 3. 记账失败抛出异常直接导致发货审核/红冲事务回滚。
 * </p>
 *
 * @author luna
 */
@Component
@Slf4j
public class ShipmentFinanceEventListener {

    public static final String PARTY_TYPE_CUSTOMER = "CUSTOMER";
    public static final String CATEGORY_AR_INC = "AR_INC";
    public static final String CATEGORY_AR_INC_REVERSAL = "AR_INC_REVERSAL";
    public static final String DIRECTION_DEBIT = "DEBIT";
    public static final String DIRECTION_CREDIT = "CREDIT";
    public static final String REF_TYPE_SHIPMENT_ITEM = "SHIPMENT_ITEM";
    public static final String REF_TYPE_SHIPMENT_FREIGHT = "SHIPMENT_FREIGHT";
    public static final String DEFAULT_CURRENCY = "RMB";

    @Resource
    private LedgerService ledgerService;

    @Resource
    private ExchangeRateService exchangeRateService;

    /**
     * 发货审核出库完成事件处理（同事务 BEFORE_COMMIT 写入 AR_INC）
     *
     * @param event 发货审核事件
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onShipmentAudited(ShipmentAuditedEvent event) {
        if (event == null || event.getShipmentId() == null) {
            return;
        }

        log.info("[ShipmentFinanceEventListener] 收到发货审核事件: shipmentId={}, shipmentNo={}, customerId={}, currency={}",
                event.getShipmentId(), event.getShipmentNo(), event.getCustomerId(), event.getCurrency());

        // 1. 查询入账即期汇率（缺汇率直接抛出 EXCHANGE_RATE_NOT_FOUND 回滚事务）
        String currency = StrUtil.isNotBlank(event.getCurrency()) ? event.getCurrency().trim() : DEFAULT_CURRENCY;
        LocalDate bizDate = event.getShipmentDate() != null ? event.getShipmentDate() : LocalDate.now();
        BigDecimal rate = exchangeRateService.getRate(currency, bizDate);

        // 2. 逐明细行追加 AR_INC 分录
        if (CollUtil.isNotEmpty(event.getItems())) {
            for (ShipmentItemSnapshotDTO item : event.getItems()) {
                if (item == null) {
                    continue;
                }
                BigDecimal origAmount = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
                BigDecimal baseAmount = origAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

                LedgerEntryDO entry = new LedgerEntryDO();
                entry.setBizDate(bizDate);
                entry.setPartyType(PARTY_TYPE_CUSTOMER);
                entry.setPartyId(event.getCustomerId());
                entry.setAccountId(null); // 往来分录，不动资金账户
                entry.setCategory(CATEGORY_AR_INC);
                entry.setDirection(DIRECTION_DEBIT);
                entry.setCurrency(currency);
                entry.setExchangeRate(rate);
                entry.setOriginalAmount(origAmount);
                entry.setBaseAmount(baseAmount);
                entry.setRefType(REF_TYPE_SHIPMENT_ITEM);
                entry.setRefId(event.getShipmentId());
                entry.setRefItemId(item.getShipmentItemId());
                entry.setReversalOfId(null);
                entry.setRemark(item.getProductName());

                ledgerService.append(entry);
            }
        }

        // 3. 运费头级行追加 AR_INC 分录（freightFee > 0 时）
        if (event.getFreightFee() != null && event.getFreightFee().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal freightAmount = event.getFreightFee();
            BigDecimal freightBaseAmount = freightAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

            LedgerEntryDO freightEntry = new LedgerEntryDO();
            freightEntry.setBizDate(bizDate);
            freightEntry.setPartyType(PARTY_TYPE_CUSTOMER);
            freightEntry.setPartyId(event.getCustomerId());
            freightEntry.setAccountId(null);
            freightEntry.setCategory(CATEGORY_AR_INC);
            freightEntry.setDirection(DIRECTION_DEBIT);
            freightEntry.setCurrency(currency);
            freightEntry.setExchangeRate(rate);
            freightEntry.setOriginalAmount(freightAmount);
            freightEntry.setBaseAmount(freightBaseAmount);
            freightEntry.setRefType(REF_TYPE_SHIPMENT_FREIGHT);
            freightEntry.setRefId(event.getShipmentId());
            freightEntry.setRefItemId(null);
            freightEntry.setReversalOfId(null);
            freightEntry.setRemark("发货运费：" + event.getShipmentNo());

            ledgerService.append(freightEntry);
        }

        log.info("[ShipmentFinanceEventListener] 发货审核 AR_INC 记账成功: shipmentId={}", event.getShipmentId());
    }

    /**
     * 发货红冲完成事件处理（同事务 BEFORE_COMMIT 写入 AR_INC_REVERSAL）
     *
     * @param event 发货红冲事件
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onShipmentCancelled(ShipmentCancelledEvent event) {
        if (event == null || event.getShipmentId() == null) {
            return;
        }

        log.info("[ShipmentFinanceEventListener] 收到发货红冲事件: shipmentId={}, shipmentNo={}, customerId={}",
                event.getShipmentId(), event.getShipmentNo(), event.getCustomerId());

        LocalDate cancelDate = event.getCancelDate() != null ? event.getCancelDate() : LocalDate.now();

        // 1. 反查本发货单对应的所有明细与运费 AR_INC 分录
        List<LedgerEntryDO> origEntries = new ArrayList<>();
        origEntries.addAll(ledgerService.getEntriesByRef(REF_TYPE_SHIPMENT_ITEM, event.getShipmentId()));
        origEntries.addAll(ledgerService.getEntriesByRef(REF_TYPE_SHIPMENT_FREIGHT, event.getShipmentId()));

        if (CollUtil.isEmpty(origEntries)) {
            log.warn("[ShipmentFinanceEventListener] 发货单未检索到原 AR_INC 分录，跳过红冲记账: shipmentId={}", event.getShipmentId());
            return;
        }

        // 2. 批量反查已存在的红冲分录，防 N+1（coding-standards §2）
        Set<Long> origIds = origEntries.stream()
                .map(LedgerEntryDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<LedgerEntryDO> existingReversals = ledgerService.getReversalEntriesByReversalOfIds(origIds);
        Set<Long> reversedOrigIds = existingReversals.stream()
                .map(LedgerEntryDO::getReversalOfId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. 逐条追加红冲分录
        for (LedgerEntryDO orig : origEntries) {
            if (orig == null || !CATEGORY_AR_INC.equals(orig.getCategory())) {
                continue;
            }

            // 防重红冲检查：已存在回指该分录的红冲行时跳过
            if (reversedOrigIds.contains(orig.getId())) {
                log.info("[ShipmentFinanceEventListener] 原分录已存在红冲记录，跳过: origEntryId={}", orig.getId());
                continue;
            }

            LedgerEntryDO reversal = new LedgerEntryDO();
            reversal.setBizDate(cancelDate);
            reversal.setPartyType(orig.getPartyType());
            reversal.setPartyId(orig.getPartyId());
            reversal.setAccountId(null);
            reversal.setCategory(CATEGORY_AR_INC_REVERSAL);
            reversal.setDirection(DIRECTION_CREDIT);
            reversal.setCurrency(orig.getCurrency());
            reversal.setExchangeRate(orig.getExchangeRate()); // 沿用原发货入账汇率
            reversal.setOriginalAmount(orig.getOriginalAmount());
            reversal.setBaseAmount(orig.getBaseAmount());
            reversal.setRefType(orig.getRefType());
            reversal.setRefId(orig.getRefId());
            reversal.setRefItemId(orig.getRefItemId());
            reversal.setReversalOfId(orig.getId()); // 关键回指
            reversal.setRemark("红冲：" + (orig.getRemark() != null ? orig.getRemark() : ""));

            ledgerService.append(reversal);
        }

        log.info("[ShipmentFinanceEventListener] 发货红冲 AR_INC_REVERSAL 记账成功: shipmentId={}", event.getShipmentId());
    }

}
