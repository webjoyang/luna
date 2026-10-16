package com.luna.lunasrv.module.lunaerp.approval.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.framework.common.util.collection.CollectionUtils;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.EntryAllocatedSumDTO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.LedgerCategory;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentRefType;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentStatus;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountService;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.luna.lunasrv.module.lunaerp.service.finance.ledger.LedgerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 客户收款单审批通过副作用处理器（对齐 ADR-0017 / ADR-0043 与 Issue 04）
 *
 * 在 BEFORE_COMMIT 事务内执行同事务四写：
 * 1. 资金账户 current_balance += amt 条件更新；
 * 2. 经营分录账追加 RECEIPT 入账分录；
 * 3. 经营分录账追加 RECEIPT_WRITE_OFF 核销分录，回填 payment_ref 的 ledger_entry_id 与 base_amount；
 * 4. 收款单主单 status 置为 COMPLETED，approval_status 置为 APPROVED。
 *
 * @author luna
 */
@Component
@Slf4j
public class CustomerReceiptApprovalHandler implements ApprovalHandler {

    @Resource
    private PaymentMapper paymentMapper;

    @Resource
    private PaymentRefMapper paymentRefMapper;

    @Resource
    private AccountService accountService;

    @Resource
    private LedgerService ledgerService;

    @Resource
    private ExchangeRateService exchangeRateService;

    @Resource
    private com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper ledgerEntryMapper;

    @Override
    public boolean supports(String bizType) {
        return BIZ_TYPE_CUSTOMER_RECEIPT.equalsIgnoreCase(bizType);
    }

    @Override
    public void onApproved(Long bizId) {
        log.info("[CustomerReceiptApprovalHandler] 监听到客户收款单审批通过，开始执行同事务四写过账: bizId={}", bizId);
        if (bizId == null) {
            return;
        }

        // 1. 查询收款单
        PaymentDO payment = paymentMapper.selectById(bizId);
        if (payment == null || !"IN".equalsIgnoreCase(payment.getDirection())) {
            throw exception(RECEIPT_NOT_EXISTS);
        }

        // 若已是 COMPLETED 则幂等返回
        if (PaymentStatus.COMPLETED.getValue().equalsIgnoreCase(payment.getStatus())) {
            log.info("[CustomerReceiptApprovalHandler] 收款单已完成过账，幂等忽略: bizId={}", bizId);
            return;
        }

        // 2. 校验资金账户可用（存在且 ENABLE）
        AccountDO account = accountService.validateAccountAvailable(payment.getAccountId());
        if (account == null || !"ENABLE".equalsIgnoreCase(account.getStatus())) {
            throw exception(ACCOUNT_DISABLED);
        }
        if (StrUtil.isNotBlank(account.getCurrency()) && StrUtil.isNotBlank(payment.getCurrency())
                && !account.getCurrency().equalsIgnoreCase(payment.getCurrency())) {
            throw exception(RECEIPT_CURRENCY_MISMATCH);
        }

        // 3. 获取过账生效汇率
        LocalDate receiptDate = payment.getReceiptDate() != null ? payment.getReceiptDate() : LocalDate.now();
        BigDecimal rate = exchangeRateService.getRate(payment.getCurrency(), receiptDate);
        if (rate == null) {
            rate = BigDecimal.ONE;
        }

        // 4. 查询拟核销分配行
        List<PaymentRefDO> refs = paymentRefMapper.selectList(new LambdaQueryWrapperX<PaymentRefDO>()
                .eq(PaymentRefDO::getPaymentId, bizId)
                .eq(PaymentRefDO::getRefType, PaymentRefType.AR_ENTRY.getValue()));

        // 5. 校验并发超额核销（逐行未核销额重验，冲突抛出 RECEIPT_POST_CONFLICT）
        validateNoPostConflict(refs, payment.getCurrency());

        // 6. 写 1：资金账户余额条件增加
        BigDecimal payAmount = payment.getPayAmount() != null ? payment.getPayAmount() : BigDecimal.ZERO;
        accountService.updateBalance(account.getId(), payAmount);

        // 7. 写 2：经营分录追加 RECEIPT 或 ADVANCE 入账分录
        String receiptCategory = com.luna.lunasrv.module.lunaerp.enums.finance.ReceiptType.ADVANCE.getValue().equalsIgnoreCase(payment.getReceiptType())
                ? LedgerCategory.ADVANCE.getValue()
                : LedgerCategory.RECEIPT.getValue();

        BigDecimal baseAmount = payAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        LedgerEntryDO receiptEntry = new LedgerEntryDO();
        receiptEntry.setBizDate(receiptDate);
        receiptEntry.setPartyType("CUSTOMER");
        receiptEntry.setPartyId(payment.getCustomerId());
        receiptEntry.setAccountId(account.getId());
        receiptEntry.setCategory(receiptCategory);
        receiptEntry.setDirection("CREDIT");
        receiptEntry.setCurrency(payment.getCurrency());
        receiptEntry.setExchangeRate(rate);
        receiptEntry.setOriginalAmount(payAmount);
        receiptEntry.setBaseAmount(baseAmount);
        receiptEntry.setRefType("RECEIPT");
        receiptEntry.setRefId(payment.getId());
        receiptEntry.setRemark(payment.getRemark());
        ledgerService.append(receiptEntry);

        // 8. 写 3：逐行写 RECEIPT_WRITE_OFF 核销分录并回填 payment_ref
        if (CollUtil.isNotEmpty(refs)) {
            for (PaymentRefDO ref : refs) {
                if (ref.getAllocateAmount() == null || ref.getAllocateAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal refBaseAmount = ref.getAllocateAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);

                LedgerEntryDO writeOffEntry = new LedgerEntryDO();
                writeOffEntry.setBizDate(receiptDate);
                writeOffEntry.setPartyType("CUSTOMER");
                writeOffEntry.setPartyId(payment.getCustomerId());
                writeOffEntry.setAccountId(null); // 核销不影响资金流水视图
                writeOffEntry.setCategory(LedgerCategory.RECEIPT_WRITE_OFF.getValue());
                writeOffEntry.setDirection("CREDIT");
                writeOffEntry.setCurrency(payment.getCurrency());
                writeOffEntry.setExchangeRate(rate);
                writeOffEntry.setOriginalAmount(ref.getAllocateAmount());
                writeOffEntry.setBaseAmount(refBaseAmount);
                writeOffEntry.setRefType("RECEIPT");
                writeOffEntry.setRefId(payment.getId());
                writeOffEntry.setRefItemId(ref.getRefId());
                writeOffEntry.setRemark("核销：" + (ref.getEntryNo() != null ? ref.getEntryNo() : ""));
                Long writeOffEntryId = ledgerService.append(writeOffEntry);

                ref.setLedgerEntryId(writeOffEntryId);
                ref.setBaseAmount(refBaseAmount);
                ref.setBizDate(receiptDate);
                paymentRefMapper.updateById(ref);
            }
        }

        // 9. 写 4：更新收款单主表状态为 COMPLETED，approval_status 置为 APPROVED
        payment.setStatus(PaymentStatus.COMPLETED.getValue());
        payment.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        payment.setExchangeRate(rate);
        paymentMapper.updateById(payment);

        log.info("[CustomerReceiptApprovalHandler] 客户收款单同事务四写过账完成: paymentId={}, paymentNo={}",
                payment.getId(), payment.getPaymentNo());
    }

    private void validateNoPostConflict(List<PaymentRefDO> refs, String currency) {
        if (CollUtil.isEmpty(refs)) {
            return;
        }

        List<Long> entryIds = refs.stream()
                .map(PaymentRefDO::getRefId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(entryIds)) {
            return;
        }

        // 防 N+1 批量快照模式：带行级悲观锁（FOR UPDATE）批量锁定待核销的 AR 分录，防止幻读超核
        List<LedgerEntryDO> origEntries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .inOrEmpty(LedgerEntryDO::getId, entryIds)
                .last("FOR UPDATE"));
        Map<Long, LedgerEntryDO> entryMap = CollectionUtils.convertMap(origEntries, LedgerEntryDO::getId);

        // 1. 发货红冲冲减 (AR_INC_REVERSAL)
        List<LedgerEntryDO> reversals = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .inOrEmpty(LedgerEntryDO::getReversalOfId, entryIds));
        Map<Long, BigDecimal> reversalMap = new HashMap<>();
        if (CollUtil.isNotEmpty(reversals)) {
            for (LedgerEntryDO rev : reversals) {
                if (rev.getReversalOfId() != null && rev.getOriginalAmount() != null) {
                    reversalMap.merge(rev.getReversalOfId(), rev.getOriginalAmount(), BigDecimal::add);
                }
            }
        }

        // 2. 批量查询已核销金额 (COMPLETED payment_ref)
        Map<Long, BigDecimal> allocatedMap = EntryAllocatedSumDTO.toMap(paymentRefMapper.selectSumAllocatedAmountByEntryIds(entryIds));

        // 3. 退货应收冲减 (AR_RETURN_REVERSAL)
        List<LedgerEntryDO> returnReversals = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getCategory, "AR_RETURN_REVERSAL")
                .inOrEmpty(LedgerEntryDO::getRefItemId, entryIds));
        Map<Long, BigDecimal> returnReversalMap = new HashMap<>();
        if (CollUtil.isNotEmpty(returnReversals)) {
            for (LedgerEntryDO ret : returnReversals) {
                if (ret.getRefItemId() != null && ret.getOriginalAmount() != null) {
                    returnReversalMap.merge(ret.getRefItemId(), ret.getOriginalAmount(), BigDecimal::add);
                }
            }
        }

        for (PaymentRefDO ref : refs) {
            if (ref == null || ref.getRefId() == null || ref.getAllocateAmount() == null) {
                continue;
            }
            LedgerEntryDO origEntry = entryMap.get(ref.getRefId());
            if (origEntry == null) {
                throw exception(RECEIPT_POST_CONFLICT);
            }
            if (origEntry.getCurrency() != null && currency != null
                    && !origEntry.getCurrency().equalsIgnoreCase(currency)) {
                throw exception(RECEIPT_CURRENCY_MISMATCH);
            }

            BigDecimal orig = origEntry.getOriginalAmount() != null ? origEntry.getOriginalAmount() : BigDecimal.ZERO;
            BigDecimal rev = reversalMap.getOrDefault(ref.getRefId(), BigDecimal.ZERO);
            BigDecimal alloc = allocatedMap.getOrDefault(ref.getRefId(), BigDecimal.ZERO);
            BigDecimal ret = returnReversalMap.getOrDefault(ref.getRefId(), BigDecimal.ZERO);

            BigDecimal currentRemain = orig.subtract(rev).subtract(alloc).subtract(ret);
            if (currentRemain.compareTo(BigDecimal.ZERO) < 0) {
                currentRemain = BigDecimal.ZERO;
            }

            // 若本次待核销金额大于当前剩余可核销余额，抛过账冲突异常
            if (ref.getAllocateAmount().compareTo(currentRemain) > 0) {
                log.warn("[validateNoPostConflict] 收款单过账超核冲突: entryId={}, allocateAmount={}, currentRemain={}",
                        ref.getRefId(), ref.getAllocateAmount(), currentRemain);
                throw exception(RECEIPT_POST_CONFLICT);
            }
        }
    }

}
