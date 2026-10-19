package com.luna.lunasrv.module.lunaerp.service.finance.payroll;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.*;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.*;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.finance.PayrollPaymentStatusEnum;
import com.luna.lunasrv.module.lunaerp.support.TableInfoTestHelper;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link PayrollServiceImpl} 核心单测
 *
 * 覆盖 Issue 06 所有关键契约与边界用例：
 * 1. 期间并发双单 025_002（部分唯一索引兜底）
 * 2. 员工重复 025_003
 * 3. 空单 025_009
 * 4. 负实发 025_011
 * 5. 金额服务端重算与计件 qty×price 权威计算
 * 6. APPROVED 无写账（Handler 空实现，业务状态流转）
 * 7. 有发放禁作废 025_006（批次聚合 mock）
 * 8. 作废原因必填与 APPROVED 零发放可作废（审批记录保留）
 * 9. print-data 仅 APPROVED
 * 10. 横向越权报不存在 025_000
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class PayrollServiceImplTest {

    @InjectMocks
    private PayrollServiceImpl payrollService;

    @Mock
    private PayrollMapper payrollMapper;

    @Mock
    private PayrollEmployeeMapper payrollEmployeeMapper;

    @Mock
    private PayrollLineItemMapper payrollLineItemMapper;

    @Mock
    private PayrollDefectRefMapper payrollDefectRefMapper;

    @Mock
    private PayrollPaymentMapper payrollPaymentMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private ApprovalInstanceService approvalInstanceService;

    @Mock
    private PayrollDefectService payrollDefectService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        TableInfoTestHelper.initTableInfo(PayrollDO.class);
        TableInfoTestHelper.initTableInfo(PayrollEmployeeDO.class);
        TableInfoTestHelper.initTableInfo(PayrollLineItemDO.class);
        TableInfoTestHelper.initTableInfo(PayrollDefectRefDO.class);
        TableInfoTestHelper.initTableInfo(PayrollPaymentDO.class);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // =========================================================================
    // 1. 创建测试（含并发双单、唯一索引兜底）
    // =========================================================================

    @Test
    @DisplayName("createPayroll：期间有效重复（应用层守卫拦截）抛 025_002")
    void testCreatePayroll_PeriodDuplicate_AppCheck() {
        PayrollCreateReqVO req = PayrollCreateReqVO.builder()
                .period("2026-10")
                .title("2026-10 工资表")
                .build();

        // mock 应用层查重发现已有有效工资单
        when(payrollMapper.selectCount(any())).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.createPayroll(req));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_PERIOD_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createPayroll：并发双单（部分唯一索引 uk_lfp_tenant_period 兜底）抛 025_002")
    void testCreatePayroll_PeriodDuplicate_UniqueIndexDbFallback() {
        PayrollCreateReqVO req = PayrollCreateReqVO.builder()
                .period("2026-10")
                .build();

        when(payrollMapper.selectCount(any())).thenReturn(0L);
        when(payrollMapper.selectListByPayrollNoPrefixIncludingDeleted("GZ202610")).thenReturn(Collections.emptyList());

        // 模拟并发插入触发数据库 partial unique index 冲突
        doThrow(new DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint \"uk_lfp_tenant_period\""))
                .when(payrollMapper).insert(any(PayrollDO.class));

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.createPayroll(req));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_PERIOD_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createPayroll：正常新建草稿单，初始总额为0，自动生成 GZ 开头月流水号")
    void testCreatePayroll_Success() {
        PayrollCreateReqVO req = PayrollCreateReqVO.builder()
                .period("2026-10")
                .build();

        when(payrollMapper.selectCount(any())).thenReturn(0L);
        when(payrollMapper.selectListByPayrollNoPrefixIncludingDeleted("GZ202610")).thenReturn(Collections.emptyList());

        doAnswer(invocation -> {
            PayrollDO payroll = invocation.getArgument(0);
            payroll.setId(1001L);
            return 1;
        }).when(payrollMapper).insert(any(PayrollDO.class));

        Long id = payrollService.createPayroll(req);
        assertEquals(1001L, id);

        ArgumentCaptor<PayrollDO> captor = ArgumentCaptor.forClass(PayrollDO.class);
        verify(payrollMapper).insert(captor.capture());
        PayrollDO inserted = captor.getValue();
        assertEquals("GZ202610001", inserted.getPayrollNo());
        assertEquals("2026-10", inserted.getPeriod());
        assertEquals("2026-10 工资表", inserted.getTitle());
        assertEquals(ApplyStatus.PENDING.getValue(), inserted.getApprovalStatus());
        assertEquals(new BigDecimal("0.00"), inserted.getEarningTotal());
        assertEquals(new BigDecimal("0.00"), inserted.getDeductionTotal());
        assertEquals(new BigDecimal("0.00"), inserted.getNetAmount());
    }

    // =========================================================================
    // 2. 更新测试（空单 025_009、员工重复 025_003、用户不存在 025_012、负实发 025_011、重算）
    // =========================================================================

    @Test
    @DisplayName("updatePayroll：空单（≥1员工行守卫）抛 025_009")
    void testUpdatePayroll_EmptyEmployees() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        PayrollUpdateReqVO req = PayrollUpdateReqVO.builder()
                .id(1001L)
                .employees(Collections.emptyList())
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.updatePayroll(req));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_EMPTY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("updatePayroll：员工重复抛 025_003")
    void testUpdatePayroll_EmployeeDuplicate() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        PayrollEmployeeSaveReqVO emp1 = PayrollEmployeeSaveReqVO.builder().userId(101L).build();
        PayrollEmployeeSaveReqVO emp2 = PayrollEmployeeSaveReqVO.builder().userId(101L).build();

        PayrollUpdateReqVO req = PayrollUpdateReqVO.builder()
                .id(1001L)
                .employees(List.of(emp1, emp2))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.updatePayroll(req));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_EMPLOYEE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("updatePayroll：员工用户在 AdminUserApi 不存在抛 025_012")
    void testUpdatePayroll_UserNotExists() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        PayrollEmployeeSaveReqVO emp = PayrollEmployeeSaveReqVO.builder().userId(999L).build();
        PayrollUpdateReqVO req = PayrollUpdateReqVO.builder()
                .id(1001L)
                .employees(List.of(emp))
                .build();

        when(adminUserApi.getUserMap(anySet())).thenReturn(Collections.emptyMap());

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.updatePayroll(req));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_USER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("updatePayroll：任一员工实发为负数（扣款>应发）抛 025_011")
    void testUpdatePayroll_NegativeNetAmount() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(101L);
        user.setNickname("张三");
        when(adminUserApi.getUserMap(anySet())).thenReturn(Map.of(101L, user));

        PayrollLineItemSaveReqVO earning = PayrollLineItemSaveReqVO.builder()
                .itemKind("EARNING")
                .itemType("BASE")
                .amount(new BigDecimal("1000.00"))
                .build();

        PayrollLineItemSaveReqVO deduction = PayrollLineItemSaveReqVO.builder()
                .itemKind("DEDUCTION")
                .itemType("OTHER")
                .amount(new BigDecimal("1500.00"))
                .build();

        PayrollEmployeeSaveReqVO emp = PayrollEmployeeSaveReqVO.builder()
                .userId(101L)
                .items(List.of(earning, deduction))
                .build();

        PayrollUpdateReqVO req = PayrollUpdateReqVO.builder()
                .id(1001L)
                .employees(List.of(emp))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.updatePayroll(req));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_NET_AMOUNT_NEGATIVE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("updatePayroll：款项行金额<=0 抛 025_004")
    void testUpdatePayroll_LineAmountInvalid_Zero() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(101L);
        user.setNickname("张三");
        when(adminUserApi.getUserMap(anySet())).thenReturn(Map.of(101L, user));

        PayrollLineItemSaveReqVO earning = PayrollLineItemSaveReqVO.builder()
                .itemKind("EARNING")
                .itemType("BASE")
                .amount(new BigDecimal("0.00"))
                .build();

        PayrollEmployeeSaveReqVO emp = PayrollEmployeeSaveReqVO.builder()
                .userId(101L)
                .items(List.of(earning))
                .build();

        PayrollUpdateReqVO req = PayrollUpdateReqVO.builder()
                .id(1001L)
                .employees(List.of(emp))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.updatePayroll(req));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_LINE_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("updatePayroll：手工录入行严禁 DEFECT（次品行仅导入生成，07 票）抛 025_004")
    void testUpdatePayroll_DefectNotAllowedInManual() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(101L);
        user.setNickname("张三");
        when(adminUserApi.getUserMap(anySet())).thenReturn(Map.of(101L, user));

        PayrollLineItemSaveReqVO defect = PayrollLineItemSaveReqVO.builder()
                .itemKind("DEDUCTION")
                .itemType("DEFECT")
                .amount(new BigDecimal("100.00"))
                .build();

        PayrollEmployeeSaveReqVO emp = PayrollEmployeeSaveReqVO.builder()
                .userId(101L)
                .items(List.of(defect))
                .build();

        PayrollUpdateReqVO req = PayrollUpdateReqVO.builder()
                .id(1001L)
                .employees(List.of(emp))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.updatePayroll(req));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_LINE_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("updatePayroll：计件 qty×price 服务端权威重算，单据级与员工行级金额快照完整一致")
    void testUpdatePayroll_AmountRecalculationAndPieceQtyPrice() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        AdminUserRespDTO user1 = new AdminUserRespDTO();
        user1.setId(101L);
        user1.setNickname("张三");

        AdminUserRespDTO user2 = new AdminUserRespDTO();
        user2.setId(102L);
        user2.setNickname("李四");

        when(adminUserApi.getUserMap(anySet())).thenReturn(Map.of(101L, user1, 102L, user2));

        // 张三：基本工资 5000 + 计件 120件 * 15.50元 = 1860 - 其他扣款 200 = 应发 6860，扣款 200，实发 6660
        PayrollLineItemSaveReqVO u1Base = PayrollLineItemSaveReqVO.builder()
                .itemKind("EARNING").itemType("BASE").amount(new BigDecimal("5000.00")).build();
        PayrollLineItemSaveReqVO u1Piece = PayrollLineItemSaveReqVO.builder()
                .itemKind("EARNING").itemType("PIECE").qty(new BigDecimal("120.00")).unitPrice(new BigDecimal("15.50")).build();
        PayrollLineItemSaveReqVO u1Deduct = PayrollLineItemSaveReqVO.builder()
                .itemKind("DEDUCTION").itemType("OTHER").amount(new BigDecimal("200.00")).build();
        PayrollEmployeeSaveReqVO emp1 = PayrollEmployeeSaveReqVO.builder()
                .userId(101L).items(List.of(u1Base, u1Piece, u1Deduct)).build();

        // 李四：基本工资 6000 + 补贴 500 = 应发 6500，扣款 0，实发 6500
        PayrollLineItemSaveReqVO u2Base = PayrollLineItemSaveReqVO.builder()
                .itemKind("EARNING").itemType("BASE").amount(new BigDecimal("6000.00")).build();
        PayrollLineItemSaveReqVO u2Subsidy = PayrollLineItemSaveReqVO.builder()
                .itemKind("EARNING").itemType("SUBSIDY").amount(new BigDecimal("500.00")).build();
        PayrollEmployeeSaveReqVO emp2 = PayrollEmployeeSaveReqVO.builder()
                .userId(102L).items(List.of(u2Base, u2Subsidy)).build();

        PayrollUpdateReqVO req = PayrollUpdateReqVO.builder()
                .id(1001L)
                .title("2026-10 全员核算表")
                .employees(List.of(emp1, emp2))
                .build();

        payrollService.updatePayroll(req);

        // 验证主表权威重算值：earning = 6860 + 6500 = 13360, deduction = 200, net = 13160
        ArgumentCaptor<PayrollDO> updateCaptor = ArgumentCaptor.forClass(PayrollDO.class);
        verify(payrollMapper).updateById(updateCaptor.capture());
        PayrollDO updated = updateCaptor.getValue();
        assertEquals(0, updated.getEarningTotal().compareTo(new BigDecimal("13360.00")));
        assertEquals(0, updated.getDeductionTotal().compareTo(new BigDecimal("200.00")));
        assertEquals(0, updated.getNetAmount().compareTo(new BigDecimal("13160.00")));
        assertEquals("2026-10 全员核算表", updated.getTitle());

        // 验证旧行删除后插入新行
        verify(payrollLineItemMapper).delete(any());
        verify(payrollEmployeeMapper).delete(any());
        verify(payrollEmployeeMapper, times(2)).insert(any(PayrollEmployeeDO.class));
        verify(payrollLineItemMapper, times(5)).insert(any(PayrollLineItemDO.class));
    }

    @Test
    @DisplayName("updatePayroll：APPROVED 状态禁止修改抛 025_001")
    void testUpdatePayroll_StatusNotAllowed() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        PayrollUpdateReqVO req = PayrollUpdateReqVO.builder()
                .id(1001L)
                .employees(List.of(PayrollEmployeeSaveReqVO.builder().userId(101L).build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.updatePayroll(req));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    // =========================================================================
    // 3. 提交与撤回审批测试（空 Handler 两步制、biz_type=payroll）
    // =========================================================================

    @Test
    @DisplayName("submitPayroll：守卫全过后发起 biz_type=payroll 审批实例")
    void testSubmitPayroll_Success() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setPeriod("2026-10");
        payroll.setPayrollNo("GZ202610001");
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        PayrollEmployeeDO emp = new PayrollEmployeeDO();
        emp.setId(10L);
        emp.setPayrollId(1001L);
        emp.setNetAmount(new BigDecimal("5000.00"));
        when(payrollEmployeeMapper.selectList(any())).thenReturn(List.of(emp));
        when(payrollMapper.selectCount(any())).thenReturn(0L);

        payrollService.submitPayroll(1001L);

        ArgumentCaptor<PayrollDO> submitCaptor = ArgumentCaptor.forClass(PayrollDO.class);
        verify(payrollMapper).updateById(submitCaptor.capture());
        assertNotNull(submitCaptor.getValue().getSubmitTime());

        ArgumentCaptor<ApprovalInstanceSubmitReqVO> captor = ArgumentCaptor.forClass(ApprovalInstanceSubmitReqVO.class);
        verify(approvalInstanceService).submitInstance(captor.capture());
        assertEquals("payroll", captor.getValue().getBizType());
        assertEquals(1001L, captor.getValue().getBizId());
    }

    @Test
    @DisplayName("submitPayroll：无员工明细行（空单提交）抛 025_009")
    void testSubmitPayroll_EmptyEmployees() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);
        when(payrollEmployeeMapper.selectList(any())).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.submitPayroll(1001L));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_EMPTY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("cancelApprove：APPROVING 状态撤回审批置 PENDING")
    void testCancelApprove_Success() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.APPROVING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        ApprovalInstanceRespVO instance = new ApprovalInstanceRespVO();
        instance.setId(888L);
        instance.setStatus(ApplyStatus.APPROVING.getValue());
        when(approvalInstanceService.getLatestInstanceByBiz("payroll", 1001L)).thenReturn(instance);

        payrollService.cancelApprove(1001L);

        verify(approvalInstanceService).withdrawInstance(eq(888L), any(ApprovalInstanceWithdrawReqVO.class));
    }

    @Test
    @DisplayName("cancelApprove：非 APPROVING 状态不可撤回抛 025_001")
    void testCancelApprove_StatusNotAllowed() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.cancelApprove(1001L));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    // =========================================================================
    // 4. 作废测试（有发放记录禁作废 025_006、APPROVED零发放可作废且审批记录保留）
    // =========================================================================

    @Test
    @DisplayName("cancelPayroll：存在发放批次记录禁止作废，抛 025_006")
    void testCancelPayroll_HasPaymentNoCancel() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        // mock 发放批次已存在 1 条记录
        when(payrollPaymentMapper.selectCount(any())).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.cancelPayroll(1001L, "错误核算"));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_HAS_PAYMENT_NO_CANCEL.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("cancelPayroll：APPROVED 且零发放可成功作废，置 CANCELLED、留痕且审批记录保留")
    void testCancelPayroll_ApprovedZeroPaymentSuccess() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        // 零发放记录
        when(payrollPaymentMapper.selectCount(any())).thenReturn(0L);

        payrollService.cancelPayroll(1001L, "生产线计件数据录入有误");

        ArgumentCaptor<PayrollDO> cancelCaptor = ArgumentCaptor.forClass(PayrollDO.class);
        verify(payrollMapper).updateById(cancelCaptor.capture());
        PayrollDO cancelled = cancelCaptor.getValue();
        assertEquals("CANCELLED", cancelled.getApprovalStatus());
        assertEquals("生产线计件数据录入有误", cancelled.getCancelReason());
        assertNotNull(cancelled.getCancelledAt());
    }

    @Test
    @DisplayName("cancelPayroll：作废原因必填校验，非 APPROVED 状态禁止作废抛 025_001")
    void testCancelPayroll_StatusNotAllowedAndReasonRequired() {
        // 原因空白抛异常
        assertThrows(ServiceException.class, () -> payrollService.cancelPayroll(1001L, "   "));

        // PENDING 草稿状态不允许作废
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.cancelPayroll(1001L, "原因合法"));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    // =========================================================================
    // 5. 草稿删除与回收站
    // =========================================================================

    @Test
    @DisplayName("deletePayroll：仅 PENDING/REJECTED 草稿可删除，级联软删员工行与款项行")
    void testDeletePayroll_DraftSuccess() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        payrollService.deletePayroll(1001L);

        verify(payrollLineItemMapper).delete(any());
        verify(payrollEmployeeMapper).delete(any());
        verify(payrollMapper).deleteById(1001L);
    }

    @Test
    @DisplayName("deletePayroll：APPROVED 状态禁止删除抛 025_001")
    void testDeletePayroll_StatusNotAllowed() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.deletePayroll(1001L));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    // =========================================================================
    // 6. 套打数据与详情聚合、横向越权安全
    // =========================================================================

    @Test
    @DisplayName("getPayrollPrintData：仅限 APPROVED 终态可取，非通过单据抛 025_001")
    void testGetPayrollPrintData_OnlyApproved() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.getPayrollPrintData(1001L));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getPayrollPrintData：APPROVED 终态返回员工每人一行应发/扣款/实发/签收栏")
    void testGetPayrollPrintData_ApprovedSuccess() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setPayrollNo("GZ202610001");
        payroll.setPeriod("2026-10");
        payroll.setTitle("2026-10 工资表");
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        payroll.setEarningTotal(new BigDecimal("10000.00"));
        payroll.setDeductionTotal(new BigDecimal("500.00"));
        payroll.setNetAmount(new BigDecimal("9500.00"));
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        PayrollEmployeeDO emp = new PayrollEmployeeDO();
        emp.setId(1L);
        emp.setUserId(101L);
        emp.setNickname("张三");
        emp.setEarningTotal(new BigDecimal("10000.00"));
        emp.setDeductionTotal(new BigDecimal("500.00"));
        emp.setNetAmount(new BigDecimal("9500.00"));
        when(payrollEmployeeMapper.selectList(any())).thenReturn(List.of(emp));

        PayrollPrintRespVO printVO = payrollService.getPayrollPrintData(1001L);
        assertNotNull(printVO);
        assertEquals("PAYROLL", printVO.getBizType());
        assertEquals("GZ202610001", printVO.getPayrollNo());
        assertEquals(1, printVO.getItems().size());
        assertEquals("张三", printVO.getItems().get(0).getNickname());
        assertEquals("张三", printVO.getItems().get(0).getSignature());
        assertEquals(new BigDecimal("9500.00"), printVO.getItems().get(0).getNetAmount());
    }

    @Test
    @DisplayName("横向越权防御：租户不匹配抛 025_000 PAYROLL_NOT_EXISTS")
    void testHorizontalPrivilegeCheck_ThrowsNotExists() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(999L); // 属于其他租户
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        ServiceException ex = assertThrows(ServiceException.class, () -> payrollService.getPayroll(1001L));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getPayroll：派生发放态计算（0 未发 / 区间内 部分发 / =net 已发清）")
    void testGetPayroll_DerivedPaymentStatus() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setNetAmount(new BigDecimal("10000.00"));
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);
        when(payrollEmployeeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(payrollLineItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        // 1. 无发放批次 -> UNPAID
        when(payrollPaymentMapper.selectList(any())).thenReturn(Collections.emptyList());
        PayrollRespVO resp1 = payrollService.getPayroll(1001L);
        assertEquals(PayrollPaymentStatusEnum.UNPAID.getValue(), resp1.getPaymentStatus());
        assertEquals(new BigDecimal("0.00"), resp1.getPaidAmount());

        // 2. 部分发放 4000.00 -> PARTIAL
        PayrollPaymentDO p1 = new PayrollPaymentDO();
        p1.setOriginalAmount(new BigDecimal("4000.00"));
        when(payrollPaymentMapper.selectList(any())).thenReturn(List.of(p1));
        PayrollRespVO resp2 = payrollService.getPayroll(1001L);
        assertEquals(PayrollPaymentStatusEnum.PARTIAL.getValue(), resp2.getPaymentStatus());
        assertEquals(new BigDecimal("4000.00"), resp2.getPaidAmount());

        // 3. 已发清 10000.00 -> PAID
        PayrollPaymentDO p2 = new PayrollPaymentDO();
        p2.setOriginalAmount(new BigDecimal("6000.00"));
        when(payrollPaymentMapper.selectList(any())).thenReturn(List.of(p1, p2));
        PayrollRespVO resp3 = payrollService.getPayroll(1001L);
        assertEquals(PayrollPaymentStatusEnum.PAID.getValue(), resp3.getPaymentStatus());
        assertEquals(new BigDecimal("10000.00"), resp3.getPaidAmount());
    }

    @Test
    @DisplayName("updatePayroll：包含次品扣款行与逐卷快照，单价×卷数与金额重算成功，持久化 refs")
    void testUpdatePayroll_WithDefectRefs_Success() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(101L);
        user.setNickname("张三");
        when(adminUserApi.getUserMap(anySet())).thenReturn(Map.of(101L, user));

        PayrollDefectRefSaveReqVO ref1 = PayrollDefectRefSaveReqVO.builder()
                .confirmationId(801L)
                .confirmationNo("INS-001")
                .rollNo("P01")
                .grade("C")
                .confirmedAt(LocalDateTime.of(2026, 10, 5, 10, 0))
                .deductPrice(new BigDecimal("50.00"))
                .build();
        PayrollDefectRefSaveReqVO ref2 = PayrollDefectRefSaveReqVO.builder()
                .confirmationId(802L)
                .confirmationNo("INS-002")
                .rollNo("P02")
                .grade("B")
                .confirmedAt(LocalDateTime.of(2026, 10, 8, 14, 0))
                .deductPrice(new BigDecimal("50.00"))
                .build();

        // 收入行 5000，次品扣款行（2卷 × 50元 = 100元）
        PayrollLineItemSaveReqVO earning = PayrollLineItemSaveReqVO.builder()
                .itemKind("EARNING")
                .itemType("BASE")
                .amount(new BigDecimal("5000.00"))
                .build();
        PayrollLineItemSaveReqVO defect = PayrollLineItemSaveReqVO.builder()
                .itemKind("DEDUCTION")
                .itemType("DEFECT")
                .defectRefs(List.of(ref1, ref2))
                .build();

        PayrollEmployeeSaveReqVO emp = PayrollEmployeeSaveReqVO.builder()
                .userId(101L)
                .items(List.of(earning, defect))
                .build();

        PayrollUpdateReqVO req = PayrollUpdateReqVO.builder()
                .id(1001L)
                .employees(List.of(emp))
                .build();

        payrollService.updatePayroll(req);

        // 验证调用了次品校验
        verify(payrollDefectService).validateDefectRefsForSave(eq(1001L), any());
        // 验证插入了 2 条次品快照
        verify(payrollDefectRefMapper, times(2)).insert(any(PayrollDefectRefDO.class));
        // 验证主表 netAmount = 5000 - 100 = 4900.00
        assertEquals(new BigDecimal("4900.00"), payroll.getNetAmount());
    }

    @Test
    @DisplayName("updatePayroll：次品扣款行金额可手工改额（改额后尊重手工金额）")
    void testUpdatePayroll_WithDefectRefs_CustomAmount() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(101L);
        user.setNickname("张三");
        when(adminUserApi.getUserMap(anySet())).thenReturn(Map.of(101L, user));

        PayrollDefectRefSaveReqVO ref1 = PayrollDefectRefSaveReqVO.builder()
                .confirmationId(801L)
                .confirmationNo("INS-001")
                .rollNo("P01")
                .grade("C")
                .confirmedAt(LocalDateTime.of(2026, 10, 5, 10, 0))
                .deductPrice(new BigDecimal("50.00"))
                .build();

        // 收入行 5000，次品扣款手工改额为 80.00（非 50.00）
        PayrollLineItemSaveReqVO earning = PayrollLineItemSaveReqVO.builder()
                .itemKind("EARNING")
                .itemType("BASE")
                .amount(new BigDecimal("5000.00"))
                .build();
        PayrollLineItemSaveReqVO defect = PayrollLineItemSaveReqVO.builder()
                .itemKind("DEDUCTION")
                .itemType("DEFECT")
                .amount(new BigDecimal("80.00"))
                .defectRefs(List.of(ref1))
                .build();

        PayrollEmployeeSaveReqVO emp = PayrollEmployeeSaveReqVO.builder()
                .userId(101L)
                .items(List.of(earning, defect))
                .build();

        PayrollUpdateReqVO req = PayrollUpdateReqVO.builder()
                .id(1001L)
                .employees(List.of(emp))
                .build();

        payrollService.updatePayroll(req);

        // 验证主表 netAmount = 5000 - 80 = 4920.00
        assertEquals(new BigDecimal("4920.00"), payroll.getNetAmount());
    }

    @Test
    @DisplayName("deletePayroll：删除草稿工资单时连带软删其下属次品引用")
    void testDeletePayroll_DeletesDefectRefs() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setTenantId(TENANT_ID);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        PayrollLineItemDO item = new PayrollLineItemDO();
        item.setId(2001L);
        item.setPayrollId(1001L);
        when(payrollLineItemMapper.selectList(any())).thenReturn(List.of(item));

        payrollService.deletePayroll(1001L);

        verify(payrollDefectRefMapper).delete(any());
        verify(payrollLineItemMapper).delete(any());
        verify(payrollEmployeeMapper).delete(any());
        verify(payrollMapper).deleteById(1001L);
    }

}
