package com.luna.lunasrv.module.lunaerp.service.finance.expense;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
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
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.account.AccountServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.finance.account.FinancePostingService;
import com.luna.lunasrv.module.lunaerp.service.finance.account.dto.PostingReq;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ExpenseServiceImpl} 单元测试（Seam 1）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class ExpenseServiceImplTest {

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    @Mock
    private ExpenseMapper expenseMapper;

    @Mock
    private ExpenseItemMapper expenseItemMapper;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private FinancePostingService financePostingService;

    @Mock
    private ApprovalInstanceService approvalInstanceService;

    private MockedStatic<TenantContextHolder> tenantContextMock;
    private MockedStatic<SecurityFrameworkUtils> securityMock;

    @BeforeEach
    public void setUp() {
        tenantContextMock = mockStatic(TenantContextHolder.class);
        tenantContextMock.when(TenantContextHolder::getRequiredTenantId).thenReturn(1L);
        tenantContextMock.when(TenantContextHolder::getTenantId).thenReturn(1L);

        securityMock = mockStatic(SecurityFrameworkUtils.class);
        securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);
    }

    @AfterEach
    public void tearDown() {
        if (tenantContextMock != null) {
            tenantContextMock.close();
        }
        if (securityMock != null) {
            securityMock.close();
        }
    }

    private AccountDO createMockAccount(Long id, String status, String currency) {
        AccountDO account = new AccountDO();
        account.setId(id);
        account.setAccountName("测试基本户");
        account.setStatus(status);
        account.setCurrency(currency);
        return account;
    }

    private AdminUserRespDTO createMockUser(Long id, String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname(nickname);
        return user;
    }

    private ExpenseItemSaveReqVO createItem(String category, BigDecimal amount, LocalDate occurDate, String remark) {
        ExpenseItemSaveReqVO item = new ExpenseItemSaveReqVO();
        item.setCategory(category);
        item.setOriginalAmount(amount);
        item.setOccurDate(occurDate);
        item.setRemark(remark);
        return item;
    }

    @Test
    @DisplayName("行守卫：明细行至少一行（024_002）")
    public void testCreateExpense_ItemRequired() {
        ExpenseCreateReqVO req = new ExpenseCreateReqVO();
        req.setAccountId(1001L);
        req.setItems(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> expenseService.createExpense(req));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_ITEM_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("行守卫：金额必须大于0（024_004）")
    public void testCreateExpense_AmountInvalid() {
        when(accountMapper.selectById(1001L)).thenReturn(createMockAccount(1001L, AccountServiceImpl.STATUS_ENABLE, "RMB"));
        when(adminUserApi.getUser(10L)).thenReturn(createMockUser(10L, "张三"));

        ExpenseCreateReqVO req = new ExpenseCreateReqVO();
        req.setAccountId(1001L);
        req.setItems(List.of(createItem("TRAVEL", BigDecimal.ZERO, LocalDate.now(), "打车")));

        ServiceException ex = assertThrows(ServiceException.class, () -> expenseService.createExpense(req));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("行守卫：发生日期不能晚于提交日/当日（024_007）")
    public void testCreateExpense_OccurDateInvalid() {
        when(accountMapper.selectById(1001L)).thenReturn(createMockAccount(1001L, AccountServiceImpl.STATUS_ENABLE, "RMB"));
        when(adminUserApi.getUser(10L)).thenReturn(createMockUser(10L, "张三"));

        ExpenseCreateReqVO req = new ExpenseCreateReqVO();
        req.setAccountId(1001L);
        req.setItems(List.of(createItem("TRAVEL", new BigDecimal("100"), LocalDate.now().plusDays(1), "打车")));

        ServiceException ex = assertThrows(ServiceException.class, () -> expenseService.createExpense(req));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_OCCUR_DATE_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("附件守卫：附件最多10个（024_005）")
    public void testCreateExpense_AttachmentLimit() {
        ExpenseCreateReqVO req = new ExpenseCreateReqVO();
        req.setAccountId(1001L);
        List<String> attachments = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            attachments.add("url" + i);
        }
        req.setAttachments(attachments);
        req.setItems(List.of(createItem("TRAVEL", new BigDecimal("100"), LocalDate.now(), "打车")));

        ServiceException ex = assertThrows(ServiceException.class, () -> expenseService.createExpense(req));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_ATTACHMENT_LIMIT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("账户守卫：账户不存在或停用（024_003）")
    public void testCreateExpense_AccountDisabled() {
        when(accountMapper.selectById(1001L)).thenReturn(createMockAccount(1001L, AccountServiceImpl.STATUS_DISABLE, "RMB"));

        ExpenseCreateReqVO req = new ExpenseCreateReqVO();
        req.setAccountId(1001L);
        req.setItems(List.of(createItem("TRAVEL", new BigDecimal("100"), LocalDate.now(), "打车")));

        ServiceException ex = assertThrows(ServiceException.class, () -> expenseService.createExpense(req));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_ACCOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("代填报人守卫：指定报销人不存在（024_006）")
    public void testCreateExpense_UserNotExists() {
        when(accountMapper.selectById(1001L)).thenReturn(createMockAccount(1001L, AccountServiceImpl.STATUS_ENABLE, "RMB"));
        when(adminUserApi.getUser(999L)).thenReturn(null);

        ExpenseCreateReqVO req = new ExpenseCreateReqVO();
        req.setApplicantUserId(999L);
        req.setAccountId(1001L);
        req.setItems(List.of(createItem("TRAVEL", new BigDecimal("100"), LocalDate.now(), "打车")));

        ServiceException ex = assertThrows(ServiceException.class, () -> expenseService.createExpense(req));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_USER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建成功：单号BX生成、服务端重算总额与最大发生日期")
    public void testCreateExpense_Success() {
        when(accountMapper.selectById(1001L)).thenReturn(createMockAccount(1001L, AccountServiceImpl.STATUS_ENABLE, "RMB"));
        when(adminUserApi.getUser(10L)).thenReturn(createMockUser(10L, "张三"));
        when(expenseMapper.selectListByExpenseNoPrefixIncludingDeleted(anyString())).thenReturn(Collections.emptyList());
        when(expenseMapper.selectByExpenseNoIncludingDeleted(anyString())).thenReturn(null);
        doAnswer(inv -> {
            ExpenseDO entity = inv.getArgument(0);
            entity.setId(5001L);
            return 1;
        }).when(expenseMapper).insert(any(ExpenseDO.class));

        ExpenseCreateReqVO req = new ExpenseCreateReqVO();
        req.setAccountId(1001L);
        LocalDate d1 = LocalDate.now().minusDays(2);
        LocalDate d2 = LocalDate.now().minusDays(1);
        req.setItems(List.of(
                createItem("TRAVEL", new BigDecimal("200.00"), d1, "交通"),
                createItem("OFFICE", new BigDecimal("300.00"), d2, "办公")
        ));

        Long id = expenseService.createExpense(req);
        assertEquals(5001L, id);

        ArgumentCaptor<ExpenseDO> captor = ArgumentCaptor.forClass(ExpenseDO.class);
        verify(expenseMapper).insert(captor.capture());
        ExpenseDO inserted = captor.getValue();
        assertTrue(inserted.getExpenseNo().startsWith("BX"));
        assertEquals(new BigDecimal("500.00"), inserted.getOriginalTotal());
        assertEquals(new BigDecimal("500.00"), inserted.getTotalAmount());
        assertEquals("PENDING", inserted.getApprovalStatus());
        assertEquals(d2, inserted.getExpenseDate());
        verify(expenseItemMapper, times(2)).insert(any(ExpenseItemDO.class));
    }

    @Test
    @DisplayName("终态守卫：APPROVED 不可改/删（024_001）")
    public void testApproved_CannotUpdateOrDelete() {
        ExpenseDO approved = new ExpenseDO();
        approved.setId(5001L);
        approved.setApprovalStatus("APPROVED");
        approved.setTenantId(1L);
        when(expenseMapper.selectById(5001L)).thenReturn(approved);

        ExpenseUpdateReqVO updateReq = new ExpenseUpdateReqVO();
        updateReq.setId(5001L);
        updateReq.setAccountId(1001L);
        updateReq.setItems(List.of(createItem("TRAVEL", new BigDecimal("100"), LocalDate.now(), "打车")));

        ServiceException exUpdate = assertThrows(ServiceException.class, () -> expenseService.updateExpense(updateReq));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_STATUS_NOT_ALLOWED.getCode(), exUpdate.getCode());

        ServiceException exDelete = assertThrows(ServiceException.class, () -> expenseService.deleteExpense(5001L));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_STATUS_NOT_ALLOWED.getCode(), exDelete.getCode());
    }

    @Test
    @DisplayName("提交审批：校验账户状态并调用 02 号审批引擎")
    public void testSubmitExpense_Success() {
        ExpenseDO expense = new ExpenseDO();
        expense.setId(5001L);
        expense.setAccountId(1001L);
        expense.setApprovalStatus("PENDING");
        expense.setTenantId(1L);
        when(expenseMapper.selectById(5001L)).thenReturn(expense);
        when(accountMapper.selectById(1001L)).thenReturn(createMockAccount(1001L, AccountServiceImpl.STATUS_ENABLE, "RMB"));
        ExpenseItemDO mockItem = new ExpenseItemDO();
        mockItem.setOriginalAmount(new BigDecimal("100.00"));
        mockItem.setOccurDate(LocalDate.now());
        when(expenseItemMapper.selectList(any())).thenReturn(List.of(mockItem));

        expenseService.submitExpense(5001L);

        ArgumentCaptor<ApprovalInstanceSubmitReqVO> captor = ArgumentCaptor.forClass(ApprovalInstanceSubmitReqVO.class);
        verify(approvalInstanceService).submitInstance(captor.capture());
        assertEquals("expense", captor.getValue().getBizType());
        assertEquals(5001L, captor.getValue().getBizId());
    }

    @Test
    @DisplayName("撤回审批：仅 APPROVING 可撤回并回置 PENDING")
    public void testCancelApprove_Success() {
        ExpenseDO expense = new ExpenseDO();
        expense.setId(5001L);
        expense.setApprovalStatus("APPROVING");
        expense.setTenantId(1L);
        when(expenseMapper.selectById(5001L)).thenReturn(expense);

        ApprovalInstanceRespVO instance = new ApprovalInstanceRespVO();
        instance.setId(888L);
        instance.setStatus("APPROVING");
        when(approvalInstanceService.getLatestInstanceByBiz("expense", 5001L)).thenReturn(instance);

        expenseService.cancelApprove(5001L);

        verify(approvalInstanceService).withdrawInstance(eq(888L), any(ApprovalInstanceWithdrawReqVO.class));
    }

    @Test
    @DisplayName("套打数据：仅限 APPROVED 可取（024_001），豁免金额脱敏并返回明细与签章")
    public void testGetExpensePrintData_OnlyApproved() {
        ExpenseDO pending = new ExpenseDO();
        pending.setId(5001L);
        pending.setApprovalStatus("PENDING");
        pending.setTenantId(1L);
        when(expenseMapper.selectById(5001L)).thenReturn(pending);

        ServiceException ex = assertThrows(ServiceException.class, () -> expenseService.getExpensePrintData(5001L));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_STATUS_NOT_ALLOWED.getCode(), ex.getCode());

        ExpenseDO approved = new ExpenseDO();
        approved.setId(5002L);
        approved.setExpenseNo("BX20261019001");
        approved.setApprovalStatus("APPROVED");
        approved.setTenantId(1L);
        approved.setOriginalTotal(new BigDecimal("600.00"));
        approved.setTotalAmount(new BigDecimal("600.00"));
        approved.setBaseTotal(new BigDecimal("600.00"));
        approved.setExchangeRate(BigDecimal.ONE);
        approved.setApplicantName("张三");
        approved.setAttachments(List.of("https://oss.example.com/inv1.pdf"));
        when(expenseMapper.selectById(5002L)).thenReturn(approved);

        ExpenseItemDO item = new ExpenseItemDO();
        item.setId(1L);
        item.setExpenseId(5002L);
        item.setCategory("TRAVEL");
        item.setOriginalAmount(new BigDecimal("600.00"));
        item.setOccurDate(LocalDate.now());
        item.setRemark("打车");
        when(expenseItemMapper.selectList(any())).thenReturn(List.of(item));

        ExpensePrintRespVO printData = expenseService.getExpensePrintData(5002L);
        assertNotNull(printData);
        assertEquals("EXPENSE", printData.getBizType());
        assertEquals("BX20261019001", printData.getExpenseNo());
        assertEquals(new BigDecimal("600.00"), printData.getTotalAmount());
        assertEquals(1, printData.getItems().size());
        assertEquals("张三", printData.getApplicantSign());
        assertTrue(printData.getAttachmentNames().contains("inv1.pdf"));
    }

    @Test
    @DisplayName("租户隔离与横向越权防范")
    public void testTenantIsolation() {
        ExpenseDO foreignTenant = new ExpenseDO();
        foreignTenant.setId(5001L);
        foreignTenant.setTenantId(99L);
        when(expenseMapper.selectById(5001L)).thenReturn(foreignTenant);

        ServiceException ex = assertThrows(ServiceException.class, () -> expenseService.getExpense(5001L));
        assertEquals(FinanceErrorCodeConstants.EXPENSE_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("改再提：REJECTED 状态允许修改并重新提交")
    public void testRejected_CanUpdate() {
        ExpenseDO rejected = new ExpenseDO();
        rejected.setId(5001L);
        rejected.setApplicantUserId(10L);
        rejected.setApprovalStatus("REJECTED");
        rejected.setTenantId(1L);
        when(expenseMapper.selectById(5001L)).thenReturn(rejected);
        when(accountMapper.selectById(1001L)).thenReturn(createMockAccount(1001L, AccountServiceImpl.STATUS_ENABLE, "RMB"));
        when(adminUserApi.getUser(10L)).thenReturn(createMockUser(10L, "张三"));

        ExpenseUpdateReqVO updateReq = new ExpenseUpdateReqVO();
        updateReq.setId(5001L);
        updateReq.setAccountId(1001L);
        updateReq.setItems(List.of(createItem("TRAVEL", new BigDecimal("150.00"), LocalDate.now(), "重新提交打车费")));

        assertDoesNotThrow(() -> expenseService.updateExpense(updateReq));
        verify(expenseMapper).updateById(any(ExpenseDO.class));
    }

    @Test
    @DisplayName("过账：外币账户按通过日 SPOT 汇率折算本位币并发起统一过账")
    public void testApproveExpenseAndPost_ForeignCurrency() {
        ExpenseDO expense = new ExpenseDO();
        expense.setId(5003L);
        expense.setExpenseNo("BX20261019003");
        expense.setAccountId(2001L);
        expense.setCurrency("USD");
        expense.setOriginalTotal(new BigDecimal("100.00"));
        expense.setApprovalStatus("APPROVING");
        expense.setTenantId(1L);
        expense.setApplicantUserId(10L);
        expense.setApplicantName("张三");
        when(expenseMapper.selectById(5003L)).thenReturn(expense);
        when(accountMapper.selectById(2001L)).thenReturn(createMockAccount(2001L, AccountServiceImpl.STATUS_ENABLE, "USD"));
        when(exchangeRateService.getRate(eq("USD"), any(LocalDate.class)))
                .thenReturn(new BigDecimal("7.15000000"));

        expenseService.approveExpenseAndPost(5003L);

        ArgumentCaptor<PostingReq> postingCaptor = ArgumentCaptor.forClass(PostingReq.class);
        verify(financePostingService).post(postingCaptor.capture());
        PostingReq req = postingCaptor.getValue();
        assertEquals("USD", req.getCurrency());
        assertEquals(new BigDecimal("100.00"), req.getOriginalAmount());
        assertEquals(new BigDecimal("7.15000000"), req.getExchangeRate());
        assertEquals("OUT_EXPENSE", req.getCategory());
        assertEquals(FinancePostingService.DIRECTION_CREDIT, req.getDirection());

        ArgumentCaptor<ExpenseDO> expenseCaptor = ArgumentCaptor.forClass(ExpenseDO.class);
        verify(expenseMapper).updateById(expenseCaptor.capture());
        ExpenseDO updated = expenseCaptor.getValue();
        assertEquals("APPROVED", updated.getApprovalStatus());
        assertEquals(new BigDecimal("715.00"), updated.getBaseTotal());
        assertEquals(new BigDecimal("715.00"), updated.getTotalAmount());
    }

}
