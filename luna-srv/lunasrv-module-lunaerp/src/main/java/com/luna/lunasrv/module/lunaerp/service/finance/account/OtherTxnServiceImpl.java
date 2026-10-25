package com.luna.lunasrv.module.lunaerp.service.finance.account;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.OtherTxnRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.OtherTxnDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.OtherTxnMapper;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 财务管理 - 其他收支服务实现类（QT 单）
 *
 * <p>两类单据登记即生效、不可改删（写账凭证，错账红冲二期）。不提供 update/delete 端点。</p>
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class OtherTxnServiceImpl implements OtherTxnService {

    public static final String TXN_NO_PREFIX = "QT";
    public static final String DIRECTION_DEBIT = "DEBIT";
    public static final String DIRECTION_CREDIT = "CREDIT";
    public static final String REF_TYPE_OTHER_TXN = "OTHER_TXN";
    public static final String PARTY_TYPE_NONE = "NONE";

    public static final String LEDGER_CATEGORY_IN_OTHER = "IN_OTHER";
    public static final String LEDGER_CATEGORY_OUT_OTHER = "OUT_OTHER";

    private static final Set<String> DEBIT_CATEGORIES = Set.of("INTEREST_IN", "WASTE_IN", "OTHER_IN");
    private static final Set<String> CREDIT_CATEGORIES = Set.of("FEE_OUT", "COST_OUT", "OTHER_OUT");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int NO_MIN_DIGITS = 3;
    private static final int MAX_GENERATE_RETRY = 20;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("\\d+");

    @Resource
    private OtherTxnMapper otherTxnMapper;

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private FinancePostingService financePostingService;

    @Resource
    private ExchangeRateService exchangeRateService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_ACCOUNT", subType = "OTHER_TXN", bizNo = "{{#result}}",
            success = "登记其他收支单【{{#result}}】")
    public Long createOtherTxn(OtherTxnCreateReqVO createReqVO) {
        if (createReqVO == null) {
            throw exception(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS);
        }

        // 1. 类别字典 2287 与 direction 后端匹配校验（020_110）
        validateCategoryAndDirection(createReqVO.getCategory(), createReqVO.getDirection());

        // 2. 查验资金账户（020_100 不存在、020_101 停用）
        AccountDO account = accountMapper.selectById(createReqVO.getAccountId());
        if (account == null) {
            throw exception(FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS);
        }
        if (!AccountServiceImpl.STATUS_ENABLE.equals(account.getStatus())) {
            log.warn("[createOtherTxn] 资金账户已停用: accountId={}, status={}", account.getId(), account.getStatus());
            throw exception(FinanceErrorCodeConstants.ACCOUNT_DISABLED);
        }

        // 3. 汇率取价与本位币折算：外币按登记日 SPOT 取价，RMB 恒为 1.00000000（取不到抛 020_200）
        BigDecimal exchangeRate = exchangeRateService.getRate(account.getCurrency(), createReqVO.getTxnDate());
        BigDecimal originalAmount = createReqVO.getOriginalAmount();
        BigDecimal baseAmount = originalAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

        // 4. 按天取号（QT+YYYYMMDD+3位流水，遇冲突顺延重试）
        String txnNo = generateTxnNoWithRetry(createReqVO.getTxnDate());

        // 5. 保存其他收支单（快照账户名称与本位币金额）
        OtherTxnDO otherTxn = new OtherTxnDO();
        otherTxn.setTxnNo(txnNo);
        otherTxn.setAccountId(account.getId());
        otherTxn.setAccountName(account.getAccountName());
        otherTxn.setDirection(createReqVO.getDirection().toUpperCase());
        otherTxn.setCategory(createReqVO.getCategory().toUpperCase());
        otherTxn.setCurrency(account.getCurrency());
        otherTxn.setOriginalAmount(originalAmount);
        otherTxn.setExchangeRate(exchangeRate);
        otherTxn.setBaseAmount(baseAmount);
        otherTxn.setTxnDate(createReqVO.getTxnDate());
        otherTxn.setRemark(createReqVO.getRemark());
        otherTxnMapper.insert(otherTxn);

        // 6. 统一通道过账：登记即写 DEBIT/IN_OTHER 或 CREDIT/OUT_OTHER 分录（ref_type=OTHER_TXN）
        String ledgerCategory = DIRECTION_DEBIT.equalsIgnoreCase(createReqVO.getDirection())
                ? LEDGER_CATEGORY_IN_OTHER : LEDGER_CATEGORY_OUT_OTHER;

        PostingReq postingReq = new PostingReq()
                .setAccountId(account.getId())
                .setDirection(createReqVO.getDirection().toUpperCase())
                .setCategory(ledgerCategory)
                .setPartyType(PARTY_TYPE_NONE)
                .setPartyId(null)
                .setPartyName(null)
                .setCurrency(account.getCurrency())
                .setOriginalAmount(originalAmount)
                .setExchangeRate(exchangeRate)
                .setBizDate(createReqVO.getTxnDate())
                .setRefType(REF_TYPE_OTHER_TXN)
                .setRefId(otherTxn.getId())
                .setRefNo(otherTxn.getTxnNo())
                .setRemark(createReqVO.getRemark());

        financePostingService.post(postingReq);

        log.info("[createOtherTxn] 登记其他收支单完成: id={}, txnNo={}, accountId={}, direction={}, category={}, originalAmount={}, baseAmount={}",
                otherTxn.getId(), txnNo, account.getId(), otherTxn.getDirection(), otherTxn.getCategory(), originalAmount, baseAmount);
        return otherTxn.getId();
    }

    @Override
    public OtherTxnRespVO getOtherTxn(Long id) {
        if (id == null) {
            throw exception(FinanceErrorCodeConstants.OTHER_TXN_NOT_EXISTS);
        }
        OtherTxnDO txn = otherTxnMapper.selectById(id);
        if (txn == null) {
            throw exception(FinanceErrorCodeConstants.OTHER_TXN_NOT_EXISTS);
        }
        return BeanUtils.toBean(txn, OtherTxnRespVO.class);
    }

    @Override
    public PageResult<OtherTxnRespVO> getOtherTxnPage(OtherTxnPageReqVO pageReqVO) {
        LambdaQueryWrapperX<OtherTxnDO> queryWrapper = new LambdaQueryWrapperX<OtherTxnDO>()
                .likeIfPresent(OtherTxnDO::getTxnNo, pageReqVO.getTxnNo())
                .eqIfPresent(OtherTxnDO::getAccountId, pageReqVO.getAccountId())
                .eqIfPresent(OtherTxnDO::getDirection, pageReqVO.getDirection())
                .eqIfPresent(OtherTxnDO::getCategory, pageReqVO.getCategory())
                .geIfPresent(OtherTxnDO::getTxnDate, pageReqVO.getTxnDateStart())
                .leIfPresent(OtherTxnDO::getTxnDate, pageReqVO.getTxnDateEnd())
                .orderByDesc(OtherTxnDO::getTxnDate)
                .orderByDesc(OtherTxnDO::getId);

        PageResult<OtherTxnDO> pageResult = otherTxnMapper.selectPage(pageReqVO, queryWrapper);
        return BeanUtils.toBean(pageResult, OtherTxnRespVO.class);
    }

    private void validateCategoryAndDirection(String category, String direction) {
        if (StrUtil.isBlank(category) || StrUtil.isBlank(direction)) {
            throw exception(FinanceErrorCodeConstants.OTHER_TXN_CATEGORY_DIRECTION_MISMATCH);
        }
        String catUpper = category.trim().toUpperCase();
        String dirUpper = direction.trim().toUpperCase();

        if (DIRECTION_DEBIT.equals(dirUpper)) {
            if (!DEBIT_CATEGORIES.contains(catUpper)) {
                log.warn("[validateCategoryAndDirection] 类别与流入方向不匹配: category={}, direction={}", category, direction);
                throw exception(FinanceErrorCodeConstants.OTHER_TXN_CATEGORY_DIRECTION_MISMATCH);
            }
        } else if (DIRECTION_CREDIT.equals(dirUpper)) {
            if (!CREDIT_CATEGORIES.contains(catUpper)) {
                log.warn("[validateCategoryAndDirection] 类别与流出方向不匹配: category={}, direction={}", category, direction);
                throw exception(FinanceErrorCodeConstants.OTHER_TXN_CATEGORY_DIRECTION_MISMATCH);
            }
        } else {
            log.warn("[validateCategoryAndDirection] 非法收支方向: direction={}", direction);
            throw exception(FinanceErrorCodeConstants.OTHER_TXN_CATEGORY_DIRECTION_MISMATCH);
        }
    }

    private String generateTxnNoWithRetry(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String prefix = TXN_NO_PREFIX + DATE_FORMATTER.format(targetDate);
        List<OtherTxnDO> candidates = otherTxnMapper.selectListByTxnNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;

        if (CollUtil.isNotEmpty(candidates)) {
            for (OtherTxnDO candidate : candidates) {
                String no = candidate.getTxnNo();
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
            if (otherTxnMapper.selectByTxnNoIncludingDeleted(candidateNo) == null) {
                return candidateNo;
            }
        }
        log.error("[generateTxnNoWithRetry] 其他收支单号生成重试耗尽: prefix={}, maxNum={}", prefix, maxNum);
        throw exception(FinanceErrorCodeConstants.OTHER_TXN_NO_EXHAUSTED);
    }

}
