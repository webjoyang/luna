package com.luna.lunasrv.module.lunaerp.service.finance.ledger.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinancePartyTypeEnum;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.event.ShipmentAuditedEvent;
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

/**
 * 财务管理 - 发货单审核事件监听器（应收成立点）
 *
 * 监听 business 侧发布的 {@link ShipmentAuditedEvent}，在当前事务提交前（BEFORE_COMMIT）
 * 按发货日生效汇率折算本位币，在经营分录账追加：
 * 1. 逐明细行的 AR_INC 分录（refType=SHIPMENT_ITEM, direction=DEBIT）；
 * 2. freightFee > 0 时运费头级行 AR_INC 分录（refType=SHIPMENT_FREIGHT, direction=DEBIT）。
 * 记账失败抛出异常，同事务回滚发货审核。
 *
 * @author luna
 */
@Slf4j
@Component
public class ShipmentAuditFinanceListener {

    @Resource
    private LedgerService ledgerService;

    @Resource
    private ExchangeRateService exchangeRateService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onShipmentAudited(ShipmentAuditedEvent event) {
        if (event == null || event.getShipmentId() == null) {
            log.warn("[onShipmentAudited] 监听到空发货审核事件，忽略");
            return;
        }
        log.info("[onShipmentAudited] 监听到发货审核事件: shipmentId={}, shipmentNo={}, customerId={}, currency={}",
                event.getShipmentId(), event.getShipmentNo(), event.getCustomerId(), event.getCurrency());

        String currency = StrUtil.isNotBlank(event.getCurrency()) ? event.getCurrency() : "CNY";
        LocalDate bizDate = event.getShipmentDate() != null ? event.getShipmentDate() : LocalDate.now();

        // 1. 获取汇率（汇率不存在抛 1_030_020_200 EXCHANGE_RATE_NOT_FOUND，触发事务回滚）
        BigDecimal rate = exchangeRateService.getRate(currency, bizDate);

        // 2. 逐明细行写入 AR_INC 分录
        if (CollUtil.isNotEmpty(event.getItems())) {
            for (ShipmentAuditedEvent.Item item : event.getItems()) {
                BigDecimal itemAmount = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
                BigDecimal baseAmount = itemAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

                LedgerEntryDO entry = LedgerEntryDO.builder()
                        .bizDate(bizDate)
                        .partyType(FinancePartyTypeEnum.CUSTOMER.getValue())
                        .partyId(event.getCustomerId())
                        .category(FinanceLedgerCategoryEnum.AR_INC.getValue())
                        .direction(FinanceLedgerDirectionEnum.DEBIT.getValue())
                        .currency(currency)
                        .exchangeRate(rate)
                        .originalAmount(itemAmount)
                        .baseAmount(baseAmount)
                        .refType(FinanceLedgerRefTypeEnum.SHIPMENT_ITEM.getValue())
                        .refId(event.getShipmentId())
                        .refItemId(item.getShipmentItemId())
                        .remark(event.getShipmentNo() + " - " + (item.getProductName() != null ? item.getProductName() : ""))
                        .build();

                ledgerService.append(entry);
            }
        }

        // 3. 运费大于 0 时，写入一条头级运费 AR_INC 分录
        if (event.getFreightFee() != null && event.getFreightFee().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal freightAmount = event.getFreightFee();
            BigDecimal baseFreight = freightAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

            LedgerEntryDO freightEntry = LedgerEntryDO.builder()
                    .bizDate(bizDate)
                    .partyType(FinancePartyTypeEnum.CUSTOMER.getValue())
                    .partyId(event.getCustomerId())
                    .category(FinanceLedgerCategoryEnum.AR_INC.getValue())
                    .direction(FinanceLedgerDirectionEnum.DEBIT.getValue())
                    .currency(currency)
                    .exchangeRate(rate)
                    .originalAmount(freightAmount)
                    .baseAmount(baseFreight)
                    .refType(FinanceLedgerRefTypeEnum.SHIPMENT_FREIGHT.getValue())
                    .refId(event.getShipmentId())
                    .refItemId(null)
                    .remark(event.getShipmentNo() + " - 运费")
                    .build();

            ledgerService.append(freightEntry);
        }
    }

}
