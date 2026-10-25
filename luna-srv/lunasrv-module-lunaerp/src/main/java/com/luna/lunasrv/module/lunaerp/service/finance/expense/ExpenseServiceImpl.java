package com.luna.lunasrv.module.lunaerp.service.finance.expense;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense.ExpenseItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense.ExpenseMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.account.FinancePostingService;
import com.luna.lunasrv.module.lunaerp.service.finance.account.dto.PostingReq;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 财务管理 - 员工费用报销服务实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    public static final String EXPENSE_NO_PREFIX = "BX";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int NO_MIN_DIGITS = 3;
    private static final int MAX_GENERATE_RETRY = 20;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("\\d+");

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "TRAVEL", "OFFICE", "TRANSPORT", "ENTERTAIN", "RENT", "UTILITY", "COMM", "OTHER"
    );

    @Resource
    private ExpenseMapper expenseMapper;

    @Resource
    private ExpenseItemMapper expenseItemMapper;

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private ExchangeRateService exchangeRateService;

    @Resource
    private FinancePostingService financePostingService;

    @Resource
    private ApprovalInstanceService approvalInstanceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "EXPENSE", subType = "CREATE", bizNo = "{{#result}}",
            success = "创建报销单【{{#result}}】")
    public Long createExpense(ExpenseCreateReqVO createReqVO) {
        if (createReqVO == null) {
            throw exception(EXPENSE_ITEM_REQUIRED);
        }

        // 1. 行守卫：至少1行
        List<ExpenseItemSaveReqVO> items = createReqVO.getItems();
        if (CollUtil.isEmpty(items)) {
            throw exception(EXPENSE_ITEM_REQUIRED);
        }

        // 2. 附件守卫：≤10
        if (createReqVO.getAttachments() != null && createReqVO.getAttachments().size() > 10) {
            throw exception(EXPENSE_ATTACHMENT_LIMIT);
        }

        // 3. 账户守卫：必须存在且启用
        AccountDO account = validateAndGetAccount(createReqVO.getAccountId());

        // 4. 报销人守卫：缺省当前登录用户，校验有效性与昵称快照
        Long applicantUserId = createReqVO.getApplicantUserId();
        if (applicantUserId == null) {
            applicantUserId = SecurityFrameworkUtils.getLoginUserId();
        }
        AdminUserRespDTO applicantUser = validateAndGetUser(applicantUserId);

        // 5. 明细行字段与金额/日期守卫
        validateItems(items);

        // 6. 服务端汇总计算
        BigDecimal originalTotal = BigDecimal.ZERO;
        LocalDate maxOccurDate = null;
        for (ExpenseItemSaveReqVO item : items) {
            originalTotal = originalTotal.add(item.getOriginalAmount());
            if (maxOccurDate == null || item.getOccurDate().isAfter(maxOccurDate)) {
                maxOccurDate = item.getOccurDate();
            }
        }
        originalTotal = originalTotal.setScale(2, RoundingMode.HALF_UP);

        // 7. 生成单号
        String expenseNo = generateExpenseNoWithRetry(LocalDate.now());

        // 8. 插入主表
        ExpenseDO expense = new ExpenseDO();
        expense.setExpenseNo(expenseNo);
        expense.setApplicantUserId(applicantUserId);
        expense.setApplicantName(applicantUser.getNickname());
        expense.setAccountId(account.getId());
        expense.setAccountName(account.getAccountName());
        expense.setCurrency(account.getCurrency());
        expense.setOriginalTotal(originalTotal);
        expense.setExchangeRate(BigDecimal.ONE.setScale(8, RoundingMode.HALF_UP));
        expense.setBaseTotal(originalTotal);
        expense.setTotalAmount(originalTotal);
        expense.setExpenseDate(maxOccurDate != null ? maxOccurDate : LocalDate.now());
        expense.setApprovalStatus(ApplyStatus.PENDING.getValue());
        expense.setAttachments(createReqVO.getAttachments() != null ? createReqVO.getAttachments() : Collections.emptyList());
        expense.setRemark(createReqVO.getRemark());
        expenseMapper.insert(expense);

        // 9. 插入明细行
        insertExpenseItems(expense.getId(), items);

        log.info("[createExpense] 报销单创建成功: id={}, expenseNo={}", expense.getId(), expenseNo);
        return expense.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "EXPENSE", subType = "UPDATE", bizNo = "{{#updateReqVO.id}}",
            success = "修改报销单【{{#updateReqVO.id}}】")
    public void updateExpense(ExpenseUpdateReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) {
            throw exception(EXPENSE_NOT_EXISTS);
        }

        // 1. 查验报销单与租户
        ExpenseDO expense = validateExpenseExistsAndTenant(updateReqVO.getId());

        // 2. 状态守卫：仅 PENDING / REJECTED 可修改
        if (!isPendingOrRejected(expense.getApprovalStatus())) {
            throw exception(EXPENSE_STATUS_NOT_ALLOWED);
        }

        // 3. 行守卫：至少1行
        List<ExpenseItemSaveReqVO> items = updateReqVO.getItems();
        if (CollUtil.isEmpty(items)) {
            throw exception(EXPENSE_ITEM_REQUIRED);
        }

        // 4. 附件守卫：≤10
        if (updateReqVO.getAttachments() != null && updateReqVO.getAttachments().size() > 10) {
            throw exception(EXPENSE_ATTACHMENT_LIMIT);
        }

        // 5. 账户守卫：联动币种
        AccountDO account = validateAndGetAccount(updateReqVO.getAccountId());

        // 6. 报销人守卫
        Long applicantUserId = updateReqVO.getApplicantUserId();
        if (applicantUserId == null) {
            applicantUserId = expense.getApplicantUserId();
        }
        AdminUserRespDTO applicantUser = validateAndGetUser(applicantUserId);

        // 7. 明细行合法性校验
        validateItems(items);

        // 8. 重新汇总
        BigDecimal originalTotal = BigDecimal.ZERO;
        LocalDate maxOccurDate = null;
        for (ExpenseItemSaveReqVO item : items) {
            originalTotal = originalTotal.add(item.getOriginalAmount());
            if (maxOccurDate == null || item.getOccurDate().isAfter(maxOccurDate)) {
                maxOccurDate = item.getOccurDate();
            }
        }
        originalTotal = originalTotal.setScale(2, RoundingMode.HALF_UP);

        // 9. 更新主表
        expense.setApplicantUserId(applicantUserId);
        expense.setApplicantName(applicantUser.getNickname());
        expense.setAccountId(account.getId());
        expense.setAccountName(account.getAccountName());
        expense.setCurrency(account.getCurrency());
        expense.setOriginalTotal(originalTotal);
        expense.setBaseTotal(originalTotal);
        expense.setTotalAmount(originalTotal);
        expense.setExpenseDate(maxOccurDate != null ? maxOccurDate : LocalDate.now());
        expense.setAttachments(updateReqVO.getAttachments() != null ? updateReqVO.getAttachments() : Collections.emptyList());
        expense.setRemark(updateReqVO.getRemark());
        expenseMapper.updateById(expense);

        // 10. 重置明细行
        expenseItemMapper.delete(new LambdaQueryWrapper<ExpenseItemDO>()
                .eq(ExpenseItemDO::getExpenseId, expense.getId()));
        insertExpenseItems(expense.getId(), items);

        log.info("[updateExpense] 报销单修改成功: id={}", expense.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "EXPENSE", subType = "DELETE", bizNo = "{{#id}}",
            success = "删除报销单【{{#id}}】进回收站")
    public void deleteExpense(Long id) {
        if (id == null) {
            throw exception(EXPENSE_NOT_EXISTS);
        }
        ExpenseDO expense = validateExpenseExistsAndTenant(id);

        // 终态守卫：仅 PENDING / REJECTED 可删进回收站
        if (!isPendingOrRejected(expense.getApprovalStatus())) {
            throw exception(EXPENSE_STATUS_NOT_ALLOWED);
        }

        expenseMapper.deleteById(id);
        expenseItemMapper.delete(new LambdaQueryWrapper<ExpenseItemDO>()
                .eq(ExpenseItemDO::getExpenseId, id));
        log.info("[deleteExpense] 报销单软删进回收站: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "EXPENSE", subType = "SUBMIT", bizNo = "{{#id}}",
            success = "提交报销单【{{#id}}】审批")
    public void submitExpense(Long id) {
        if (id == null) {
            throw exception(EXPENSE_NOT_EXISTS);
        }
        ExpenseDO expense = validateExpenseExistsAndTenant(id);

        // 状态守卫：仅 PENDING / REJECTED 可提交
        if (!isPendingOrRejected(expense.getApprovalStatus())) {
            throw exception(EXPENSE_STATUS_NOT_ALLOWED);
        }

        // 账户启用守卫（024_003）
        validateAndGetAccount(expense.getAccountId());

        // 明细行守卫（024_002）
        List<ExpenseItemDO> items = expenseItemMapper.selectList(new LambdaQueryWrapperX<ExpenseItemDO>()
                .eq(ExpenseItemDO::getExpenseId, id));
        if (CollUtil.isEmpty(items)) {
            throw exception(EXPENSE_ITEM_REQUIRED);
        }
        for (ExpenseItemDO item : items) {
            if (item.getOriginalAmount() == null || item.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(EXPENSE_AMOUNT_INVALID);
            }
            if (item.getOccurDate() == null || item.getOccurDate().isAfter(LocalDate.now())) {
                throw exception(EXPENSE_OCCUR_DATE_INVALID);
            }
        }

        // 附件守卫（024_005）
        if (expense.getAttachments() != null && expense.getAttachments().size() > 10) {
            throw exception(EXPENSE_ATTACHMENT_LIMIT);
        }

        // 更新提交时间
        expense.setSubmitTime(LocalDateTime.now());
        expenseMapper.updateById(expense);

        // 发起 02 号审批流实例
        ApprovalInstanceSubmitReqVO submitReqVO = new ApprovalInstanceSubmitReqVO();
        submitReqVO.setBizType(ApprovalHandler.BIZ_TYPE_EXPENSE);
        submitReqVO.setBizId(id);
        submitReqVO.setComment("提交费用报销单审批");
        approvalInstanceService.submitInstance(submitReqVO);

        log.info("[submitExpense] 报销单提交审批成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "EXPENSE", subType = "CANCEL_APPROVE", bizNo = "{{#id}}",
            success = "撤回报销单【{{#id}}】审批")
    public void cancelApprove(Long id) {
        if (id == null) {
            throw exception(EXPENSE_NOT_EXISTS);
        }
        ExpenseDO expense = validateExpenseExistsAndTenant(id);

        // 仅 APPROVING 可撤回
        if (!ApplyStatus.APPROVING.getValue().equalsIgnoreCase(expense.getApprovalStatus())) {
            throw exception(EXPENSE_STATUS_NOT_ALLOWED);
        }

        ApprovalInstanceRespVO instance = approvalInstanceService.getLatestInstanceByBiz(ApprovalHandler.BIZ_TYPE_EXPENSE, id);
        if (instance == null || !ApplyStatus.APPROVING.getValue().equalsIgnoreCase(instance.getStatus())) {
            throw exception(EXPENSE_STATUS_NOT_ALLOWED);
        }

        ApprovalInstanceWithdrawReqVO withdrawReqVO = new ApprovalInstanceWithdrawReqVO();
        withdrawReqVO.setReason("发起人撤回费用报销审批");
        approvalInstanceService.withdrawInstance(instance.getId(), withdrawReqVO);

        log.info("[cancelApprove] 报销单审批已撤回: id={}", id);
    }

    @Override
    public ExpenseRespVO getExpense(Long id) {
        if (id == null) {
            throw exception(EXPENSE_NOT_EXISTS);
        }
        ExpenseDO expense = validateExpenseExistsAndTenant(id);
        ExpenseRespVO respVO = BeanUtils.toBean(expense, ExpenseRespVO.class);

        // 关联明细行
        List<ExpenseItemDO> items = expenseItemMapper.selectList(new LambdaQueryWrapperX<ExpenseItemDO>()
                .eq(ExpenseItemDO::getExpenseId, id)
                .orderByAsc(ExpenseItemDO::getId));
        respVO.setItems(BeanUtils.toBean(items, ExpenseItemRespVO.class));

        // 审批进度摘要
        try {
            ApprovalInstanceRespVO instance = approvalInstanceService.getLatestInstanceByBiz(ApprovalHandler.BIZ_TYPE_EXPENSE, id);
            respVO.setApprovalProgress(instance);
        } catch (Exception e) {
            log.warn("[getExpense] 获取审批进度摘要异常: id={}, error={}", id, e.getMessage());
        }

        return respVO;
    }

    @Override
    public PageResult<ExpenseRespVO> getExpensePage(ExpensePageReqVO pageReqVO) {
        LambdaQueryWrapperX<ExpenseDO> queryWrapper = new LambdaQueryWrapperX<ExpenseDO>()
                .likeIfPresent(ExpenseDO::getExpenseNo, pageReqVO.getExpenseNo())
                .eqIfPresent(ExpenseDO::getApplicantUserId, pageReqVO.getApplicantUserId())
                .likeIfPresent(ExpenseDO::getApplicantName, pageReqVO.getApplicantName())
                .eqIfPresent(ExpenseDO::getAccountId, pageReqVO.getAccountId())
                .eqIfPresent(ExpenseDO::getApprovalStatus, pageReqVO.getApprovalStatus())
                .geIfPresent(ExpenseDO::getExpenseDate, pageReqVO.getExpenseDateStart())
                .leIfPresent(ExpenseDO::getExpenseDate, pageReqVO.getExpenseDateEnd())
                .orderByDesc(ExpenseDO::getExpenseDate)
                .orderByDesc(ExpenseDO::getId);

        if (StrUtil.isNotBlank(pageReqVO.getCategory())) {
            queryWrapper.apply("EXISTS (SELECT 1 FROM lunaerp_finance_expense_item item "
                    + "WHERE item.expense_id = lunaerp_finance_expense.id AND item.category = {0} AND item.deleted = 0)",
                    pageReqVO.getCategory().trim().toUpperCase());
        }

        PageResult<ExpenseDO> pageResult = expenseMapper.selectPage(pageReqVO, queryWrapper);
        return BeanUtils.toBean(pageResult, ExpenseRespVO.class);
    }

    @Override
    @LogRecord(type = "EXPENSE", subType = "PRINT", bizNo = "{{#id}}",
            success = "获取报销单【{{#id}}】A4套打数据")
    public ExpensePrintRespVO getExpensePrintData(Long id) {
        if (id == null) {
            throw exception(EXPENSE_NOT_EXISTS);
        }
        ExpenseDO expense = validateExpenseExistsAndTenant(id);

        // 仅限 APPROVED 终态可取 A4 套打数据
        if (!ApplyStatus.APPROVED.getValue().equalsIgnoreCase(expense.getApprovalStatus())) {
            throw exception(EXPENSE_STATUS_NOT_ALLOWED);
        }

        List<ExpenseItemDO> items = expenseItemMapper.selectList(new LambdaQueryWrapperX<ExpenseItemDO>()
                .eq(ExpenseItemDO::getExpenseId, id)
                .orderByAsc(ExpenseItemDO::getId));

        List<ExpensePrintItemRespVO> printItems = new ArrayList<>();
        int lineNo = 1;
        for (ExpenseItemDO item : items) {
            printItems.add(ExpensePrintItemRespVO.builder()
                    .lineNo(lineNo++)
                    .category(item.getCategory())
                    .originalAmount(item.getOriginalAmount())
                    .occurDate(item.getOccurDate())
                    .remark(item.getRemark())
                    .build());
        }

        List<String> attachmentNames = new ArrayList<>();
        if (CollUtil.isNotEmpty(expense.getAttachments())) {
            for (String url : expense.getAttachments()) {
                if (StrUtil.isNotBlank(url)) {
                    attachmentNames.add(FileUtil.getName(url));
                }
            }
        }

        return ExpensePrintRespVO.builder()
                .bizType("EXPENSE")
                .title("员工费用报销单")
                .id(expense.getId())
                .expenseNo(expense.getExpenseNo())
                .applicantUserId(expense.getApplicantUserId())
                .applicantName(expense.getApplicantName())
                .accountId(expense.getAccountId())
                .accountName(expense.getAccountName())
                .currency(expense.getCurrency())
                .originalTotal(expense.getOriginalTotal())
                .exchangeRate(expense.getExchangeRate())
                .baseTotal(expense.getBaseTotal())
                .totalAmount(expense.getTotalAmount())
                .expenseDate(expense.getExpenseDate())
                .approvalStatus(expense.getApprovalStatus())
                .submitTime(expense.getSubmitTime())
                .approveTime(expense.getApproveTime())
                .remark(expense.getRemark())
                .creator(expense.getCreator())
                .createTime(expense.getCreateTime())
                .printTime(LocalDateTime.now())
                .items(printItems)
                .attachmentNames(attachmentNames)
                .applicantSign(expense.getApplicantName())
                .departmentSign("")
                .financeSign("")
                .managerSign("")
                .cashierSign("")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "EXPENSE", subType = "POST", bizNo = "{{#id}}",
            success = "报销单【{{#id}}】审批通过并同事务过账")
    public void approveExpenseAndPost(Long id) {
        if (id == null) {
            throw exception(EXPENSE_NOT_EXISTS);
        }
        ExpenseDO expense = validateExpenseExistsAndTenant(id);

        // 幂等守卫：若已 APPROVED 则直接返回
        if (ApplyStatus.APPROVED.getValue().equalsIgnoreCase(expense.getApprovalStatus())) {
            log.info("[approveExpenseAndPost] 报销单已处于 APPROVED 终态，幂等忽略: id={}", id);
            return;
        }

        // 校验付款账户存在与启用
        AccountDO account = validateAndGetAccount(expense.getAccountId());

        // 外币按通过日 SPOT 汇率快照，RMB 恒为 1.00000000
        BigDecimal exchangeRate;
        if ("RMB".equalsIgnoreCase(account.getCurrency())) {
            exchangeRate = BigDecimal.ONE.setScale(8, RoundingMode.HALF_UP);
        } else {
            exchangeRate = exchangeRateService.getRate(account.getCurrency(), LocalDate.now());
        }

        BigDecimal originalTotal = expense.getOriginalTotal();
        BigDecimal baseTotal = originalTotal.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = "RMB".equalsIgnoreCase(account.getCurrency()) ? originalTotal : baseTotal;

        // 统一过账请求组装：CREDIT / OUT_EXPENSE，party=EMPLOYEE
        PostingReq req = new PostingReq()
                .setAccountId(account.getId())
                .setDirection(FinancePostingService.DIRECTION_CREDIT)
                .setCategory("OUT_EXPENSE")
                .setPartyType("EMPLOYEE")
                .setPartyId(expense.getApplicantUserId())
                .setPartyName(expense.getApplicantName())
                .setCurrency(account.getCurrency())
                .setOriginalAmount(originalTotal)
                .setExchangeRate(exchangeRate)
                .setBizDate(LocalDate.now())
                .setRefType("EXPENSE")
                .setRefId(expense.getId())
                .setRefNo(expense.getExpenseNo())
                .setRemark(expense.getRemark());

        // 统一过账扣款（余额不足将抛异常级联回滚整个审批事务）
        financePostingService.post(req);

        // 更新报销单状态为 APPROVED 终态
        expense.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        expense.setApproveTime(LocalDateTime.now());
        expense.setExchangeRate(exchangeRate);
        expense.setBaseTotal(baseTotal);
        expense.setTotalAmount(totalAmount);
        expenseMapper.updateById(expense);

        log.info("[approveExpenseAndPost] 报销单审批通过并过账成功: id={}, expenseNo={}, totalAmount={}",
                expense.getId(), expense.getExpenseNo(), totalAmount);
    }

    private ExpenseDO validateExpenseExistsAndTenant(Long id) {
        ExpenseDO expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw exception(EXPENSE_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && expense.getTenantId() != null && !tenantId.equals(expense.getTenantId())) {
            throw exception(EXPENSE_NOT_EXISTS);
        }
        return expense;
    }

    private AccountDO validateAndGetAccount(Long accountId) {
        if (accountId == null) {
            throw exception(EXPENSE_ACCOUNT_INVALID);
        }
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null || !AccountServiceImpl.STATUS_ENABLE.equals(account.getStatus())) {
            throw exception(EXPENSE_ACCOUNT_INVALID);
        }
        return account;
    }

    private AdminUserRespDTO validateAndGetUser(Long userId) {
        if (userId == null) {
            throw exception(EXPENSE_USER_NOT_EXISTS);
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            throw exception(EXPENSE_USER_NOT_EXISTS);
        }
        return user;
    }

    private void validateItems(List<ExpenseItemSaveReqVO> items) {
        LocalDate today = LocalDate.now();
        for (ExpenseItemSaveReqVO item : items) {
            if (StrUtil.isBlank(item.getCategory()) || !VALID_CATEGORIES.contains(item.getCategory().trim().toUpperCase())) {
                throw exception(EXPENSE_ITEM_REQUIRED);
            }
            if (item.getOriginalAmount() == null || item.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(EXPENSE_AMOUNT_INVALID);
            }
            if (item.getOccurDate() == null || item.getOccurDate().isAfter(today)) {
                throw exception(EXPENSE_OCCUR_DATE_INVALID);
            }
            if (StrUtil.isBlank(item.getRemark())) {
                throw exception(EXPENSE_ITEM_REQUIRED);
            }
        }
    }

    private void insertExpenseItems(Long expenseId, List<ExpenseItemSaveReqVO> items) {
        for (ExpenseItemSaveReqVO reqItem : items) {
            ExpenseItemDO itemDO = new ExpenseItemDO();
            itemDO.setExpenseId(expenseId);
            itemDO.setCategory(reqItem.getCategory().trim().toUpperCase());
            itemDO.setOriginalAmount(reqItem.getOriginalAmount());
            itemDO.setOccurDate(reqItem.getOccurDate());
            itemDO.setRemark(reqItem.getRemark().trim());
            expenseItemMapper.insert(itemDO);
        }
    }

    private boolean isPendingOrRejected(String approvalStatus) {
        return ApplyStatus.isDraft(approvalStatus)
                || ApplyStatus.REJECTED.getValue().equalsIgnoreCase(approvalStatus);
    }

    private String generateExpenseNoWithRetry(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String prefix = EXPENSE_NO_PREFIX + DATE_FORMATTER.format(targetDate);
        List<ExpenseDO> candidates = expenseMapper.selectListByExpenseNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;

        if (CollUtil.isNotEmpty(candidates)) {
            for (ExpenseDO candidate : candidates) {
                String no = candidate.getExpenseNo();
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
                    // 忽略格式异常单号
                }
            }
        }

        for (int i = 1; i <= MAX_GENERATE_RETRY; i++) {
            long seq = maxNum + i;
            String candidateNo = prefix + String.format("%0" + maxDigits + "d", seq);
            if (expenseMapper.selectByExpenseNoIncludingDeleted(candidateNo) == null) {
                return candidateNo;
            }
        }
        log.error("[generateExpenseNoWithRetry] 报销单号生成重试耗尽: prefix={}, maxNum={}", prefix, maxNum);
        throw exception(EXPENSE_STATUS_NOT_ALLOWED);
    }

}
