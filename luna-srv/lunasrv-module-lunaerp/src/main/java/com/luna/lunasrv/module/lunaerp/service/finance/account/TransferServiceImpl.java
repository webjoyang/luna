package com.luna.lunasrv.module.lunaerp.service.finance.account;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.TransferRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.TransferDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.TransferMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.account.dto.PostingReq;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 财务管理 - 资金账户间转账服务实现类（ZZ 单）
 *
 * <p>两类单据登记即生效、不可改删（写账凭证，错账红冲二期）。不提供 update/delete 端点。</p>
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class TransferServiceImpl implements TransferService {

    public static final String TRANSFER_NO_PREFIX = "ZZ";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String REF_TYPE_TRANSFER = "TRANSFER";
    public static final String CATEGORY_TRANSFER_OUT = "TRANSFER_OUT";
    public static final String CATEGORY_TRANSFER_IN = "TRANSFER_IN";
    public static final String PARTY_TYPE_NONE = "NONE";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int NO_MIN_DIGITS = 3;
    private static final int MAX_GENERATE_RETRY = 20;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("\\d+");

    @Resource
    private TransferMapper transferMapper;

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private FinancePostingService financePostingService;

    @Resource
    private ExchangeRateService exchangeRateService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_ACCOUNT", subType = "TRANSFER", bizNo = "{{#result}}",
            success = "登记账户间转账【{{#result}}】")
    public Long createTransfer(TransferCreateReqVO createReqVO) {
        if (createReqVO == null) {
            throw exception(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS);
        }

        // 1. 转出转入账户不可相同（020_107）
        if (createReqVO.getFromAccountId().equals(createReqVO.getToAccountId())) {
            log.warn("[createTransfer] 转出转入账户相同: accountId={}", createReqVO.getFromAccountId());
            throw exception(FinanceErrorCodeConstants.TRANSFER_ACCOUNT_SAME);
        }

        // 2. 查验两方资金账户（020_100 不存在、020_101 停用）
        AccountDO fromAccount = accountMapper.selectById(createReqVO.getFromAccountId());
        AccountDO toAccount = accountMapper.selectById(createReqVO.getToAccountId());
        if (fromAccount == null || toAccount == null) {
            throw exception(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS);
        }
        if (!AccountServiceImpl.STATUS_ENABLE.equals(fromAccount.getStatus())
                || !AccountServiceImpl.STATUS_ENABLE.equals(toAccount.getStatus())) {
            log.warn("[createTransfer] 资金账户已停用: fromStatus={}, toStatus={}",
                    fromAccount.getStatus(), toAccount.getStatus());
            throw exception(FinanceErrorCodeConstants.ACCOUNT_DISABLED);
        }

        // 3. 必须同币种转账（020_106）
        if (!fromAccount.getCurrency().equalsIgnoreCase(toAccount.getCurrency())
                || (StrUtil.isNotBlank(createReqVO.getCurrency())
                && !fromAccount.getCurrency().equalsIgnoreCase(createReqVO.getCurrency().trim()))) {
            log.warn("[createTransfer] 转账币种不一致: fromCurrency={}, toCurrency={}, reqCurrency={}",
                    fromAccount.getCurrency(), toAccount.getCurrency(), createReqVO.getCurrency());
            throw exception(FinanceErrorCodeConstants.ACCOUNT_CURRENCY_MISMATCH);
        }

        // 4. 汇率取价：外币按登记日 SPOT 取价，RMB 为 1.00000000（取不到抛 020_200）
        BigDecimal exchangeRate = exchangeRateService.getRate(fromAccount.getCurrency(), createReqVO.getTransferDate());

        // 5. 按天取号（ZZ+YYYYMMDD+3位流水，遇冲突顺延重试）
        String transferNo = generateTransferNoWithRetry(createReqVO.getTransferDate());

        // 6. 保存转账单主表（快照账户名称，status=COMPLETED 固定）
        TransferDO transfer = new TransferDO();
        transfer.setTransferNo(transferNo);
        transfer.setFromAccountId(fromAccount.getId());
        transfer.setFromAccountName(fromAccount.getAccountName());
        transfer.setToAccountId(toAccount.getId());
        transfer.setToAccountName(toAccount.getAccountName());
        transfer.setCurrency(fromAccount.getCurrency());
        transfer.setAmount(createReqVO.getAmount());
        transfer.setTransferDate(createReqVO.getTransferDate());
        transfer.setStatus(STATUS_COMPLETED);
        transfer.setRemark(createReqVO.getRemark());
        transferMapper.insert(transfer);

        // 7. 同事务两账户按 id 升序经统一通道各过一笔（防死锁 + 无半转整体回滚）
        // from: CREDIT / TRANSFER_OUT
        // to: DEBIT / TRANSFER_IN
        PostingReq fromPosting = buildPostingReq(fromAccount.getId(), FinancePostingService.DIRECTION_CREDIT,
                CATEGORY_TRANSFER_OUT, transfer, exchangeRate);
        PostingReq toPosting = buildPostingReq(toAccount.getId(), FinancePostingService.DIRECTION_DEBIT,
                CATEGORY_TRANSFER_IN, transfer, exchangeRate);

        if (fromAccount.getId() < toAccount.getId()) {
            financePostingService.post(fromPosting);
            financePostingService.post(toPosting);
        } else {
            financePostingService.post(toPosting);
            financePostingService.post(fromPosting);
        }

        log.info("[createTransfer] 账户间转账完成: transferId={}, transferNo={}, fromAccountId={}, toAccountId={}, amount={}",
                transfer.getId(), transferNo, fromAccount.getId(), toAccount.getId(), transfer.getAmount());
        return transfer.getId();
    }

    @Override
    public TransferRespVO getTransfer(Long id) {
        if (id == null) {
            throw exception(FinanceErrorCodeConstants.TRANSFER_NOT_EXISTS);
        }
        TransferDO transfer = transferMapper.selectById(id);
        if (transfer == null) {
            throw exception(FinanceErrorCodeConstants.TRANSFER_NOT_EXISTS);
        }
        return BeanUtils.toBean(transfer, TransferRespVO.class);
    }

    @Override
    public PageResult<TransferRespVO> getTransferPage(TransferPageReqVO pageReqVO) {
        LambdaQueryWrapperX<TransferDO> queryWrapper = new LambdaQueryWrapperX<TransferDO>()
                .likeIfPresent(TransferDO::getTransferNo, pageReqVO.getTransferNo())
                .eqIfPresent(TransferDO::getFromAccountId, pageReqVO.getFromAccountId())
                .eqIfPresent(TransferDO::getToAccountId, pageReqVO.getToAccountId())
                .geIfPresent(TransferDO::getTransferDate, pageReqVO.getTransferDateStart())
                .leIfPresent(TransferDO::getTransferDate, pageReqVO.getTransferDateEnd())
                .orderByDesc(TransferDO::getTransferDate)
                .orderByDesc(TransferDO::getId);

        PageResult<TransferDO> pageResult = transferMapper.selectPage(pageReqVO, queryWrapper);
        return BeanUtils.toBean(pageResult, TransferRespVO.class);
    }

    private String generateTransferNoWithRetry(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String prefix = TRANSFER_NO_PREFIX + DATE_FORMATTER.format(targetDate);
        List<TransferDO> candidates = transferMapper.selectListByTransferNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;

        if (CollUtil.isNotEmpty(candidates)) {
            for (TransferDO candidate : candidates) {
                String no = candidate.getTransferNo();
                if (no == null || no.length() <= prefix.length()) {
                    continue;
                }
                String suffix = no.substring(prefix.length());
                Matcher matcher = NO_SUFFIX_NUMBER_PATTERN.matcher(suffix);
                if (!matcher.find()) {
                    continue;
                }
                String digits = matcher.group();
                try {
                    long num = Long.parseLong(digits);
                    if (num > maxNum) {
                        maxNum = num;
                        maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
                    }
                } catch (NumberFormatException ignore) {
                    // 忽略异常编号
                }
            }
        }

        for (int i = 1; i <= MAX_GENERATE_RETRY; i++) {
            long seq = maxNum + i;
            String candidateNo = prefix + String.format("%0" + maxDigits + "d", seq);
            if (transferMapper.selectByTransferNoIncludingDeleted(candidateNo) == null) {
                return candidateNo;
            }
        }
        log.error("[generateTransferNoWithRetry] 转账单号生成重试耗尽: prefix={}, maxNum={}", prefix, maxNum);
        throw exception(FinanceErrorCodeConstants.TRANSFER_NO_EXHAUSTED);
    }

    private PostingReq buildPostingReq(Long accountId, String direction, String category,
                                      TransferDO transfer, BigDecimal exchangeRate) {
        return new PostingReq()
                .setAccountId(accountId)
                .setDirection(direction)
                .setCategory(category)
                .setPartyType(PARTY_TYPE_NONE)
                .setPartyId(null)
                .setPartyName(null)
                .setCurrency(transfer.getCurrency())
                .setOriginalAmount(transfer.getAmount())
                .setExchangeRate(exchangeRate)
                .setBizDate(transfer.getTransferDate())
                .setRefType(REF_TYPE_TRANSFER)
                .setRefId(transfer.getId())
                .setRefNo(transfer.getTransferNo())
                .setRemark(transfer.getRemark());
    }

}
