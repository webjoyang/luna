package com.luna.lunasrv.module.lunaerp.service.finance.ledger.listener;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.service.business.salesreturn.event.SalesReturnPostedEvent;
import com.luna.lunasrv.module.lunaerp.service.business.salesreturn.event.SalesReturnPostedItemDTO;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants.SALES_RETURN_EXCHANGE_RATE_MISSING;

/**
 * 销售退货过账事件财务记账监听器（finance 域公共内核，ADR-0043，Q5，Issue 06）
 *
 * <p>
 * 监听 business.salesreturn 财务确认过账事件（SalesReturnPostedEvent）：
 * 1. 同事务 BEFORE_COMMIT 反查原发货 AR_INC 分录以提取原发货入账汇率；
 * 2. 逐行调用 LedgerService.recordArReturnReversal 追加 AR_RETURN_REVERSAL 反向冲减分录；
 * 3. 若原发货汇率缺失报 014 异常，导致过账整事务回滚。
 * </p>
 *
 * @author luna
 */
@Component
@Slf4j
public class SalesReturnLedgerListener {

    private static final String REF_TYPE_SHIPMENT_ITEM = "SHIPMENT_ITEM";
    private static final String CATEGORY_AR_INC = "AR_INC";

    @Resource
    private LedgerService ledgerService;

    /**
     * 销售退货财务确认过账事件处理（同事务 BEFORE_COMMIT 写入 AR_RETURN_REVERSAL）
     *
     * @param event 销售退货过账事件
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onSalesReturnPosted(SalesReturnPostedEvent event) {
        if (event == null || event.getReturnId() == null || CollUtil.isEmpty(event.getItems())) {
            return;
        }

        log.info("[SalesReturnLedgerListener] 收到销售退货过账事件: returnId={}, returnNo={}, customerId={}, currency={}",
                event.getReturnId(), event.getReturnNo(), event.getCustomerId(), event.getCurrency());

        // 1. 批量反查原发货单的 AR_INC 分录以提取入账汇率（防 N+1，coding-standards.md §2）
        List<LedgerEntryDO> origEntries = event.getShipmentId() != null
                ? ledgerService.getEntriesByRef(REF_TYPE_SHIPMENT_ITEM, event.getShipmentId())
                : Collections.emptyList();

        Map<Long, LedgerEntryDO> entryByItemId = CollUtil.isEmpty(origEntries) ? Collections.emptyMap()
                : origEntries.stream()
                        .filter(e -> CATEGORY_AR_INC.equals(e.getCategory()))
                        .collect(Collectors.toMap(LedgerEntryDO::getRefItemId, Function.identity(), (a, b) -> a));

        // 2. 逐行校验汇率并调用 LedgerService.recordArReturnReversal
        for (SalesReturnPostedItemDTO item : event.getItems()) {
            if (item == null || item.getShipmentItemId() == null) {
                log.error("[SalesReturnLedgerListener] 退货明细行或原发货明细血缘缺失: returnId={}", event.getReturnId());
                throw exception(SALES_RETURN_EXCHANGE_RATE_MISSING);
            }

            LedgerEntryDO origEntry = entryByItemId.get(item.getShipmentItemId());
            if (origEntry == null || origEntry.getExchangeRate() == null) {
                log.error("[SalesReturnLedgerListener] 原发货入账汇率缺失: shipmentId={}, shipmentItemId={}, returnId={}",
                        event.getShipmentId(), item.getShipmentItemId(), event.getReturnId());
                throw exception(SALES_RETURN_EXCHANGE_RATE_MISSING);
            }

            ledgerService.recordArReturnReversal(
                    event.getCustomerId(),
                    item.getShipmentItemId(),
                    item.getReturnQty(),
                    item.getAmount(),
                    event.getCurrency(),
                    origEntry.getExchangeRate(),
                    event.getReturnId()
            );
        }

        log.info("[SalesReturnLedgerListener] 销售退货应收冲减记账成功: returnId={}", event.getReturnId());
    }

}
