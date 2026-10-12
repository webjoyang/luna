package com.luna.lunasrv.module.lunaerp.service.finance.ledger.listener;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerDirectionEnum;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.event.ShipmentCancelledEvent;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.List;

/**
 * 财务管理 - 发货单红冲事件监听器（应收红冲点）
 *
 * 监听 business 侧发布的 {@link ShipmentCancelledEvent}，在当前事务提交前（BEFORE_COMMIT）
 * 查找发货单原 AR_INC 分录，逐条追加 AR_INC_REVERSAL 分录（reversal_of_id 回指原分录，direction=CREDIT）。
 * 记账失败抛出异常，同事务回滚发货红冲。
 *
 * @author luna
 */
@Slf4j
@Component
public class ShipmentCancelFinanceListener {

    @Resource
    private LedgerService ledgerService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onShipmentCancelled(ShipmentCancelledEvent event) {
        if (event == null || event.getShipmentId() == null) {
            log.warn("[onShipmentCancelled] 监听到空发货红冲事件，忽略");
            return;
        }
        log.info("[onShipmentCancelled] 监听到发货红冲事件: shipmentId={}, shipmentNo={}, customerId={}",
                event.getShipmentId(), event.getShipmentNo(), event.getCustomerId());

        // 查找发货单下所有原分录
        List<LedgerEntryDO> originalEntries = ledgerService.getEntriesByRefId(event.getShipmentId());
        if (CollUtil.isEmpty(originalEntries)) {
            log.warn("[onShipmentCancelled] 发货单无关联分录: shipmentId={}", event.getShipmentId());
            return;
        }

        for (LedgerEntryDO orig : originalEntries) {
            // 仅对未红冲的 AR_INC 分录追加红冲行
            if (!FinanceLedgerCategoryEnum.AR_INC.getValue().equals(orig.getCategory())) {
                continue;
            }

            // 防重红冲检查：若已存在针对该分录的 reversal 行则跳过
            List<LedgerEntryDO> existingReversals = ledgerService.getEntriesByReversalOfId(orig.getId());
            if (CollUtil.isNotEmpty(existingReversals)) {
                log.info("[onShipmentCancelled] 原分录已被红冲，跳过: origId={}", orig.getId());
                continue;
            }

            LedgerEntryDO reversal = LedgerEntryDO.builder()
                    .bizDate(LocalDate.now())
                    .partyType(orig.getPartyType())
                    .partyId(orig.getPartyId())
                    .category(FinanceLedgerCategoryEnum.AR_INC_REVERSAL.getValue())
                    .direction(FinanceLedgerDirectionEnum.CREDIT.getValue())
                    .currency(orig.getCurrency())
                    .exchangeRate(orig.getExchangeRate())
                    .originalAmount(orig.getOriginalAmount())
                    .baseAmount(orig.getBaseAmount())
                    .refType(orig.getRefType())
                    .refId(orig.getRefId())
                    .refItemId(orig.getRefItemId())
                    .reversalOfId(orig.getId())
                    .remark(event.getShipmentNo() + " - 发货红冲 (原分录=" + orig.getId() + ")")
                    .build();

            ledgerService.append(reversal);
        }
    }

}
