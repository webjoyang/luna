package com.luna.lunasrv.module.lunaerp.service.finance.payment.seam;

import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinancePartyTypeEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 经营分录账服务 Seam 实现（桥接至真实 LedgerService）
 *
 * @author luna
 */
@Slf4j
@Service
public class FinanceLedgerSeamImpl implements FinanceLedgerSeam {

    @Resource
    private LedgerService ledgerService;

    @Override
    public void appendSupplierPaymentEntry(Long paymentId, String paymentNo, Long supplierId,
                                           BigDecimal totalAmount, BigDecimal payAmount, BigDecimal prepayUsedAmount) {
        log.info("[FinanceLedgerSeam] 追加供应商付款分录 OUT_SUPPLIER: paymentId={}, paymentNo={}, supplierId={}, "
                + "totalAmount={}, payAmount={}, prepayUsedAmount={}",
                paymentId, paymentNo, supplierId, totalAmount, payAmount, prepayUsedAmount);
        BigDecimal entryAmount = payAmount != null ? payAmount : totalAmount;
        if (entryAmount == null) {
            entryAmount = BigDecimal.ZERO;
        }

        LedgerEntryDO entry = LedgerEntryDO.builder()
                .bizDate(LocalDate.now())
                .partyType(FinancePartyTypeEnum.SUPPLIER.getValue())
                .partyId(supplierId)
                .category(FinanceLedgerCategoryEnum.OUT_SUPPLIER.getValue())
                .direction(FinanceLedgerDirectionEnum.CREDIT.getValue())
                .currency("CNY")
                .exchangeRate(BigDecimal.ONE.setScale(8, RoundingMode.UNNECESSARY))
                .originalAmount(entryAmount)
                .baseAmount(entryAmount)
                .refType(FinanceLedgerRefTypeEnum.PAYMENT.getValue())
                .refId(paymentId)
                .remark("供应商付款登记，单号=" + paymentNo)
                .build();

        ledgerService.append(entry);
    }

}
