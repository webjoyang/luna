package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyType;
import com.luna.lunasrv.module.lunaerp.enums.finance.ReconStatus;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentApplyServiceImpl} 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class PaymentApplyServiceImplTest {

    @InjectMocks
    private PaymentApplyServiceImpl paymentApplyService;

    @Mock
    private PaymentApplyMapper paymentApplyMapper;

    @Mock
    private SupplierReconMapper supplierReconMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentRefMapper paymentRefMapper;

    @Mock
    private ApprovalInstanceService approvalInstanceService;

    private MockedStatic<SecurityFrameworkUtils> mockedSecurity;

    private static final Long TENANT_ID = 1L;
    private static final Long LOGIN_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        mockedSecurity = mockStatic(SecurityFrameworkUtils.class);
        mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
    }

    @AfterEach
    void tearDown() {
        mockedSecurity.close();
        TenantContextHolder.clear();
    }

    // ==================== 1. 创建付款申请（RECON） ====================

    @Test
    @DisplayName("创建 RECON 申请：对账单不存在或非 CONFIRMED，抛 023_002")
    void testCreateRecon_reconNotConfirmed() {
        PaymentApplyCreateReqVO req = new PaymentApplyCreateReqVO();
        req.setApplyType(ApplyType.RECON.getValue());
        req.setReconId(2001L);
        req.setAmount(new BigDecimal("1000.00"));

        // Case 1: 对账单不存在
        when(supplierReconMapper.selectById(2001L)).thenReturn(null);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> paymentApplyService.createPaymentApply(req));
        assertEquals(PAYMENT_APPLY_RECON_NOT_CONFIRMED.getCode(), ex1.getCode());

        // Case 2: 对账单处于 DRAFT 状态
        SupplierReconDO reconDraft = new SupplierReconDO();
        reconDraft.setId(2001L);
        reconDraft.setTenantId(TENANT_ID);
        reconDraft.setStatus(ReconStatus.DRAFT.getValue());
        reconDraft.setTotalAmount(new BigDecimal("5000.00"));
        when(supplierReconMapper.selectById(2001L)).thenReturn(reconDraft);

        ServiceException ex2 = assertThrows(ServiceException.class, () -> paymentApplyService.createPaymentApply(req));
        assertEquals(PAYMENT_APPLY_RECON_NOT_CONFIRMED.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("创建 RECON 申请：金额超过对账单确认金额，抛 022_007 并带有累计超额")
    void testCreateRecon_amountExceed() {
        PaymentApplyCreateReqVO req = new PaymentApplyCreateReqVO();
        req.setApplyType(ApplyType.RECON.getValue());
        req.setReconId(2001L);
        req.setAmount(new BigDecimal("3000.00"));

        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(2001L);
        recon.setTenantId(TENANT_ID);
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTotalAmount(new BigDecimal("5000.00"));
        when(supplierReconMapper.selectById(2001L)).thenReturn(recon);

        // 已有一笔有效申请 3000 元（排除 REJECTED/CANCELLED）
        PaymentApplyDO existingApply = new PaymentApplyDO();
        existingApply.setId(101L);
        existingApply.setReconId(2001L);
        existingApply.setAmount(new BigDecimal("3000.00"));
        existingApply.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(paymentApplyMapper.selectList(any())).thenReturn(List.of(existingApply));

        // 3000 + 3000 = 6000 > 5000
        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.createPaymentApply(req));
        assertEquals(SUPPLIER_RECON_APPLY_AMOUNT_EXCEED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("6000.00"), "错误消息必须带累计金额");
    }

    @Test
    @DisplayName("创建 RECON 申请成功：快照回填、按天生成 FK 单号、初始状态为 PENDING")
    void testCreateRecon_success() {
        PaymentApplyCreateReqVO req = new PaymentApplyCreateReqVO();
        req.setApplyType(ApplyType.RECON.getValue());
        req.setReconId(2001L);
        req.setAmount(new BigDecimal("2000.00"));
        req.setRemark("测试对账付款");

        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(2001L);
        recon.setTenantId(TENANT_ID);
        recon.setReconNo("SR20261015001");
        recon.setSupplierId(3001L);
        recon.setSupplierName("某某印染织造厂");
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTotalAmount(new BigDecimal("5000.00"));
        when(supplierReconMapper.selectById(2001L)).thenReturn(recon);

        // 没有其他申请
        when(paymentApplyMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(paymentApplyMapper.selectListByApplyNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(paymentApplyMapper.selectByApplyNoIncludingDeleted(any())).thenReturn(null);

        doAnswer(invocation -> {
            PaymentApplyDO applyDO = invocation.getArgument(0);
            applyDO.setId(1001L);
            return 1;
        }).when(paymentApplyMapper).insert(any(PaymentApplyDO.class));

        Long id = paymentApplyService.createPaymentApply(req);
        assertEquals(1001L, id);

        ArgumentCaptor<PaymentApplyDO> captor = ArgumentCaptor.forClass(PaymentApplyDO.class);
        verify(paymentApplyMapper).insert(captor.capture());
        PaymentApplyDO saved = captor.getValue();

        assertTrue(saved.getApplyNo().startsWith("FK"), "单号必须以 FK 开头");
        assertEquals(ApplyType.RECON.getValue(), saved.getApplyType());
        assertEquals(3001L, saved.getSupplierId());
        assertEquals("某某印染织造厂", saved.getSupplierName());
        assertEquals(2001L, saved.getReconId());
        assertEquals("SR20261015001", saved.getReconNo());
        assertEquals(new BigDecimal("2000.00"), saved.getAmount());
        assertEquals(ApplyStatus.PENDING.getValue(), saved.getApprovalStatus());
    }

    // ==================== 2. 创建付款申请（PREPAY） ====================

    @Test
    @DisplayName("创建 PREPAY 申请：缺少 purpose 报错")
    void testCreatePrepay_missingPurpose() {
        PaymentApplyCreateReqVO req = new PaymentApplyCreateReqVO();
        req.setApplyType(ApplyType.PREPAY.getValue());
        req.setSupplierId(3001L);
        req.setAmount(new BigDecimal("5000.00"));
        req.setPurpose("   "); // 空白

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.createPaymentApply(req));
        assertTrue(ex.getMessage().contains("用途") || ex.getMessage().contains("purpose"));
    }

    @Test
    @DisplayName("创建 PREPAY 申请：供应商不存在抛 022_002")
    void testCreatePrepay_supplierNotExists() {
        PaymentApplyCreateReqVO req = new PaymentApplyCreateReqVO();
        req.setApplyType(ApplyType.PREPAY.getValue());
        req.setSupplierId(9999L);
        req.setAmount(new BigDecimal("5000.00"));
        req.setPurpose("定金");

        when(supplierMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.createPaymentApply(req));
        assertEquals(SUPPLIER_RECON_SUPPLIER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建 PREPAY 申请成功：供应商存在、用途必填、快照银行信息")
    void testCreatePrepay_success() {
        PaymentApplyCreateReqVO req = new PaymentApplyCreateReqVO();
        req.setApplyType(ApplyType.PREPAY.getValue());
        req.setSupplierId(3001L);
        req.setAmount(new BigDecimal("8000.00"));
        req.setPurpose("纱线采购预付款");
        req.setBankName("工商银行柯桥支行");
        req.setBankAccount("622202100011223344");

        SupplierDO supplier = new SupplierDO();
        supplier.setId(3001L);
        supplier.setTenantId(TENANT_ID);
        supplier.setCompanyName("绍兴某某化纤厂");
        when(supplierMapper.selectById(3001L)).thenReturn(supplier);

        when(paymentApplyMapper.selectListByApplyNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(paymentApplyMapper.selectByApplyNoIncludingDeleted(any())).thenReturn(null);

        doAnswer(invocation -> {
            PaymentApplyDO applyDO = invocation.getArgument(0);
            applyDO.setId(1002L);
            return 1;
        }).when(paymentApplyMapper).insert(any(PaymentApplyDO.class));

        Long id = paymentApplyService.createPaymentApply(req);
        assertEquals(1002L, id);

        ArgumentCaptor<PaymentApplyDO> captor = ArgumentCaptor.forClass(PaymentApplyDO.class);
        verify(paymentApplyMapper).insert(captor.capture());
        PaymentApplyDO saved = captor.getValue();

        assertEquals(ApplyType.PREPAY.getValue(), saved.getApplyType());
        assertEquals("绍兴某某化纤厂", saved.getSupplierName());
        assertEquals("纱线采购预付款", saved.getPurpose());
        assertEquals("工商银行柯桥支行", saved.getBankName());
        assertEquals("622202100011223344", saved.getBankAccount());
        assertNull(saved.getReconId());
        assertNull(saved.getReconNo());
    }

    // ==================== 3. 修改付款申请（PUT /update） ====================

    @Test
    @DisplayName("修改申请：仅 DRAFT/PENDING/REJECTED 可修改，APPROVING/APPROVED/CANCELLED 抛 023_001")
    void testUpdate_statusNotAllowed() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setTenantId(TENANT_ID);
        apply.setApprovalStatus(ApplyStatus.APPROVING.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        PaymentApplyUpdateReqVO req = new PaymentApplyUpdateReqVO();
        req.setId(1001L);
        req.setAmount(new BigDecimal("3000.00"));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.updatePaymentApply(req));
        assertEquals(PAYMENT_APPLY_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("修改 RECON 申请：修改金额后超额抛 022_007")
    void testUpdate_reconAmountExceed() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setTenantId(TENANT_ID);
        apply.setApplyType(ApplyType.RECON.getValue());
        apply.setReconId(2001L);
        apply.setAmount(new BigDecimal("1000.00"));
        apply.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        SupplierReconDO recon = new SupplierReconDO();
        recon.setId(2001L);
        recon.setTenantId(TENANT_ID);
        recon.setStatus(ReconStatus.CONFIRMED.getValue());
        recon.setTotalAmount(new BigDecimal("5000.00"));
        when(supplierReconMapper.selectById(2001L)).thenReturn(recon);

        // 另一笔有效申请 3500 元
        PaymentApplyDO otherApply = new PaymentApplyDO();
        otherApply.setId(1002L);
        otherApply.setReconId(2001L);
        otherApply.setAmount(new BigDecimal("3500.00"));
        otherApply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        when(paymentApplyMapper.selectList(any())).thenReturn(List.of(apply, otherApply));

        // 本次尝试改成 2000 元（3500 + 2000 = 5500 > 5000）
        PaymentApplyUpdateReqVO req = new PaymentApplyUpdateReqVO();
        req.setId(1001L);
        req.setAmount(new BigDecimal("2000.00"));

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.updatePaymentApply(req));
        assertEquals(SUPPLIER_RECON_APPLY_AMOUNT_EXCEED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("修改 REJECTED 申请成功：允许重新调整金额与备注")
    void testUpdate_rejectedSuccess() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setTenantId(TENANT_ID);
        apply.setApplyType(ApplyType.PREPAY.getValue());
        apply.setAmount(new BigDecimal("1000.00"));
        apply.setPurpose("初版用途");
        apply.setApprovalStatus(ApplyStatus.REJECTED.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        PaymentApplyUpdateReqVO req = new PaymentApplyUpdateReqVO();
        req.setId(1001L);
        req.setAmount(new BigDecimal("800.00"));
        req.setPurpose("修改后用途");
        req.setRemark("按主管要求下调金额");

        paymentApplyService.updatePaymentApply(req);

        verify(paymentApplyMapper).updateById(apply);
        assertEquals(new BigDecimal("800.00"), apply.getAmount());
        assertEquals("修改后用途", apply.getPurpose());
        assertEquals("按主管要求下调金额", apply.getRemark());
    }

    // ==================== 4. 删除付款申请（DELETE） ====================

    @Test
    @DisplayName("删除申请：仅 DRAFT/PENDING 可删，非此状态报 023_001")
    void testDelete_statusNotAllowed() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setTenantId(TENANT_ID);
        apply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.deletePaymentApply(1001L));
        assertEquals(PAYMENT_APPLY_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除申请成功：DRAFT 软删")
    void testDelete_success() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setTenantId(TENANT_ID);
        apply.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        paymentApplyService.deletePaymentApply(1001L);
        verify(paymentApplyMapper).deleteById(1001L);
    }

    // ==================== 5. 提交审批（POST /submit） ====================

    @Test
    @DisplayName("提交审批：状态为 APPROVING/APPROVED 时抛 023_001")
    void testSubmit_statusNotAllowed() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setTenantId(TENANT_ID);
        apply.setApprovalStatus(ApplyStatus.APPROVING.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.submitPaymentApply(1001L));
        assertEquals(PAYMENT_APPLY_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批：REJECTED 驳回状态重新提交，调用 approvalInstanceService 发起新流程")
    void testSubmit_rejectedReSubmit() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setTenantId(TENANT_ID);
        apply.setApplyType(ApplyType.PREPAY.getValue());
        apply.setAmount(new BigDecimal("1000.00"));
        apply.setPurpose("用途");
        apply.setApprovalStatus(ApplyStatus.REJECTED.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        paymentApplyService.submitPaymentApply(1001L);

        ArgumentCaptor<ApprovalInstanceSubmitReqVO> captor = ArgumentCaptor.forClass(ApprovalInstanceSubmitReqVO.class);
        verify(approvalInstanceService).submitInstance(captor.capture());
        assertEquals("payment", captor.getValue().getBizType());
        assertEquals(1001L, captor.getValue().getBizId());
    }

    // ==================== 6. 撤回审批（POST /cancel-approve） ====================

    @Test
    @DisplayName("撤回审批：非 APPROVING 状态报 023_001")
    void testCancelApprove_statusNotAllowed() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setTenantId(TENANT_ID);
        apply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.cancelApprove(1001L));
        assertEquals(PAYMENT_APPLY_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("撤回审批成功：APPROVING 调用 withdrawInstance 撤回流程")
    void testCancelApprove_success() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setTenantId(TENANT_ID);
        apply.setApprovalStatus(ApplyStatus.APPROVING.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        ApprovalInstanceRespVO instance = new ApprovalInstanceRespVO();
        instance.setId(5001L);
        instance.setStatus(ApplyStatus.APPROVING.getValue());
        when(approvalInstanceService.getLatestInstanceByBiz("payment", 1001L)).thenReturn(instance);

        paymentApplyService.cancelApprove(1001L);

        verify(approvalInstanceService).withdrawInstance(eq(5001L), any(ApprovalInstanceWithdrawReqVO.class));
    }

    // ==================== 7. 预付余额聚合 ====================

    @Test
    @DisplayName("预付余额聚合：Σ(PREPAY allocate_amount) - Σ(prepay_used_amount)")
    void testGetPrepayBalance() {
        Long supplierId = 3001L;
        // 供应商 PREPAY 申请已核销累计 10000 元
        when(paymentApplyMapper.selectSumAllocatedAmountBySupplierAndType(supplierId, ApplyType.PREPAY.getValue(), TENANT_ID))
                .thenReturn(new BigDecimal("10000.00"));
        // 付款登记中已抵扣使用预付累计 3500 元
        when(paymentMapper.selectSumPrepayUsedAmountBySupplier(supplierId, TENANT_ID))
                .thenReturn(new BigDecimal("3500.00"));

        BigDecimal balance = paymentApplyService.getPrepayBalance(supplierId);
        assertEquals(new BigDecimal("6500.00"), balance);
    }

    // ==================== 8. 详情与已核销聚合 ====================

    @Test
    @DisplayName("详情查询：内嵌已核销金额与剩余金额")
    void testGetPaymentApply() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setTenantId(TENANT_ID);
        apply.setApplyNo("FK20261015001");
        apply.setAmount(new BigDecimal("5000.00"));
        apply.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        // 已核销 2000 元
        when(paymentRefMapper.selectSumAllocatedAmountByApplyId(1001L))
                .thenReturn(new BigDecimal("2000.00"));

        PaymentApplyRespVO resp = paymentApplyService.getPaymentApply(1001L);
        assertNotNull(resp);
        assertEquals(new BigDecimal("2000.00"), resp.getAllocatedAmount());
        assertEquals(new BigDecimal("3000.00"), resp.getRemainAmount());
    }

    // ==================== 9. 租户隔离 ====================

    @Test
    @DisplayName("租户隔离：他人租户申请访问抛 023_000")
    void testTenantIsolation() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setTenantId(999L); // 不同租户
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.getPaymentApply(1001L));
        assertEquals(PAYMENT_APPLY_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ==================== 10. 取号耗尽 ====================

    @Test
    @DisplayName("发号重试耗尽：50 次候选全部碰撞时抛 023_008")
    void testCreatePaymentApply_generateNoExhausted_throwsException() {
        PaymentApplyCreateReqVO req = new PaymentApplyCreateReqVO();
        req.setApplyType(ApplyType.PREPAY.getValue());
        req.setSupplierId(100L);
        req.setAmount(new BigDecimal("1000.00"));
        req.setPurpose("采购定金");

        SupplierDO supplier = new SupplierDO();
        supplier.setId(100L);
        supplier.setCompanyName("供应商A");
        supplier.setTenantId(TENANT_ID);
        when(supplierMapper.selectById(100L)).thenReturn(supplier);

        // 模拟每次取号查重均已存在
        when(paymentApplyMapper.selectByApplyNoIncludingDeleted(anyString())).thenReturn(new PaymentApplyDO());

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.createPaymentApply(req));
        assertEquals(PAYMENT_APPLY_NO_EXHAUSTED.getCode(), ex.getCode());
    }

}
