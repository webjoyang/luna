package com.luna.lunasrv.module.lunaerp.service.finance.payment;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplySaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentApplyTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentDirectionEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentStatusEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.SupplierReconStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentApplyServiceImpl} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PaymentApplyServiceImplTest {

    @Mock
    private PaymentApplyMapper paymentApplyMapper;

    @Mock
    private SupplierReconMapper supplierReconMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private PaymentRefMapper paymentRefMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private ApprovalInstanceService approvalInstanceService;

    @InjectMocks
    private PaymentApplyServiceImpl paymentApplyService;

    @Test
    @DisplayName("RECON 挂非 CONFIRMED 对账单抛 023_002 PAYMENT_APPLY_RECON_NOT_CONFIRMED")
    void createPaymentApply_reconNotConfirmed_throws023002() {
        Long reconId = 100L;
        SupplierReconDO draftRecon = SupplierReconDO.builder()
                .id(reconId)
                .reconNo("SR20261011001")
                .status(SupplierReconStatusEnum.DRAFT.getValue())
                .totalAmount(new BigDecimal("10000.00"))
                .supplierId(1L)
                .supplierName("测试供应商")
                .build();
        when(supplierReconMapper.selectById(reconId)).thenReturn(draftRecon);

        PaymentApplySaveReqVO reqVO = PaymentApplySaveReqVO.builder()
                .applyType(PaymentApplyTypeEnum.RECON.getValue())
                .reconId(reconId)
                .amount(new BigDecimal("5000.00"))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.createPaymentApply(reqVO));
        assertEquals(PAYMENT_APPLY_RECON_NOT_CONFIRMED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("并发超额：累计申请额 + 本次 > total_amount 抛 022_007 SUPPLIER_RECON_APPLY_AMOUNT_EXCEED")
    void createPaymentApply_concurrentExceedAmount_throws022007() {
        Long reconId = 100L;
        SupplierReconDO confirmedRecon = SupplierReconDO.builder()
                .id(reconId)
                .reconNo("SR20261011001")
                .status(SupplierReconStatusEnum.CONFIRMED.getValue())
                .totalAmount(new BigDecimal("10000.00"))
                .supplierId(1L)
                .supplierName("测试供应商")
                .build();
        when(supplierReconMapper.selectById(reconId)).thenReturn(confirmedRecon);

        // 已有一笔有效申请 8000
        PaymentApplyDO existingApply = PaymentApplyDO.builder()
                .id(1L)
                .reconId(reconId)
                .amount(new BigDecimal("8000.00"))
                .approvalStatus("PENDING")
                .build();
        when(paymentApplyMapper.selectList(any())).thenReturn(Collections.singletonList(existingApply));

        // 本次申请 3000 (8000 + 3000 = 11000 > 10000)
        PaymentApplySaveReqVO reqVO = PaymentApplySaveReqVO.builder()
                .applyType(PaymentApplyTypeEnum.RECON.getValue())
                .reconId(reconId)
                .amount(new BigDecimal("3000.00"))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.createPaymentApply(reqVO));
        assertEquals(SUPPLIER_RECON_APPLY_AMOUNT_EXCEED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("PREPAY 缺少 purpose 必填校验抛异常")
    void createPaymentApply_prepayMissingPurpose_throwsError() {
        Long supplierId = 200L;
        SupplierDO supplier = new SupplierDO();
        supplier.setId(supplierId);
        supplier.setCompanyName("测试供应商");
        when(supplierMapper.selectById(supplierId)).thenReturn(supplier);

        PaymentApplySaveReqVO reqVO = PaymentApplySaveReqVO.builder()
                .applyType(PaymentApplyTypeEnum.PREPAY.getValue())
                .supplierId(supplierId)
                .amount(new BigDecimal("5000.00"))
                .purpose("") // 空用途
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.createPaymentApply(reqVO));
        assertEquals(PAYMENT_APPLY_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("REJECTED 改后再提：修改并重新提交触发审批流程")
    void updateAndResubmit_rejectedApply_reTriggersApproval() {
        Long applyId = 300L;
        PaymentApplyDO rejectedApply = PaymentApplyDO.builder()
                .id(applyId)
                .applyNo("FK20261011001")
                .applyType(PaymentApplyTypeEnum.PREPAY.getValue())
                .supplierId(200L)
                .amount(new BigDecimal("5000.00"))
                .purpose("初次用途")
                .approvalStatus("REJECTED")
                .build();
        when(paymentApplyMapper.selectById(applyId)).thenReturn(rejectedApply);

        // 1. 修改
        PaymentApplySaveReqVO updateReq = PaymentApplySaveReqVO.builder()
                .id(applyId)
                .applyType(PaymentApplyTypeEnum.PREPAY.getValue())
                .amount(new BigDecimal("6000.00"))
                .purpose("修改后用途")
                .build();
        paymentApplyService.updatePaymentApply(updateReq);

        verify(paymentApplyMapper).updateById(rejectedApply);
        assertEquals(new BigDecimal("6000.00"), rejectedApply.getAmount());
        assertEquals("修改后用途", rejectedApply.getPurpose());

        // 2. 重新提交审批
        when(approvalInstanceService.submitInstance(any())).thenReturn(999L);
        Long instanceId = paymentApplyService.submitPaymentApply(applyId);

        assertEquals(999L, instanceId);
        ArgumentCaptor<ApprovalInstanceSubmitReqVO> captor = ArgumentCaptor.forClass(ApprovalInstanceSubmitReqVO.class);
        verify(approvalInstanceService).submitInstance(captor.capture());
        assertEquals("payment", captor.getValue().getBizType());
        assertEquals(applyId, captor.getValue().getBizId());
        assertEquals("APPROVING", rejectedApply.getApprovalStatus());
    }

    @Test
    @DisplayName("预付余额聚合口径：Σ(PREPAY核销) - Σ(付款登记已用预付)")
    void getPrepayBalance_aggregationLogic() {
        Long supplierId = 200L;

        // 供应商有两笔 PREPAY 申请 10L 和 20L
        PaymentApplyDO apply1 = PaymentApplyDO.builder().id(10L).supplierId(supplierId).applyType(PaymentApplyTypeEnum.PREPAY.getValue()).build();
        PaymentApplyDO apply2 = PaymentApplyDO.builder().id(20L).supplierId(supplierId).applyType(PaymentApplyTypeEnum.PREPAY.getValue()).build();
        when(paymentApplyMapper.selectList(any())).thenReturn(Arrays.asList(apply1, apply2));

        // 核销子表中该两笔申请分别被核销了 5000.00 与 3000.00
        PaymentRefDO ref1 = PaymentRefDO.builder().refId(10L).allocateAmount(new BigDecimal("5000.00")).build();
        PaymentRefDO ref2 = PaymentRefDO.builder().refId(20L).allocateAmount(new BigDecimal("3000.00")).build();
        when(paymentRefMapper.selectListByRefTypeAndRefIds(eq("PAY_APPLY"), anyCollection()))
                .thenReturn(Arrays.asList(ref1, ref2));

        // 付款登记中已使用预付 2500.00
        PaymentDO payment = PaymentDO.builder()
                .supplierId(supplierId)
                .direction(PaymentDirectionEnum.OUT.getValue())
                .status(PaymentStatusEnum.COMPLETED.getValue())
                .prepayUsedAmount(new BigDecimal("2500.00"))
                .build();
        when(paymentMapper.selectList(any())).thenReturn(Collections.singletonList(payment));

        BigDecimal balance = paymentApplyService.getPrepayBalance(supplierId);

        // 5000 + 3000 - 2500 = 5500.00
        assertEquals(new BigDecimal("5500.00"), balance);
    }

    @Test
    @DisplayName("非草稿/待提交状态删除报 023_001 PAYMENT_APPLY_STATUS_NOT_ALLOWED")
    void deletePaymentApply_nonDraftStatus_throws023001() {
        Long applyId = 400L;
        PaymentApplyDO approvingApply = PaymentApplyDO.builder()
                .id(applyId)
                .approvalStatus("APPROVING")
                .build();
        when(paymentApplyMapper.selectById(applyId)).thenReturn(approvingApply);

        ServiceException ex = assertThrows(ServiceException.class, () -> paymentApplyService.deletePaymentApply(applyId));
        assertEquals(PAYMENT_APPLY_STATUS_NOT_ALLOWED.getCode(), ex.getCode());

        PaymentApplyDO approvedApply = PaymentApplyDO.builder()
                .id(applyId)
                .approvalStatus("APPROVED")
                .build();
        when(paymentApplyMapper.selectById(applyId)).thenReturn(approvedApply);

        ServiceException ex2 = assertThrows(ServiceException.class, () -> paymentApplyService.deletePaymentApply(applyId));
        assertEquals(PAYMENT_APPLY_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("APPROVING 状态撤回：调用审批引擎 withdraw 并将状态置为 PENDING")
    void cancelApprove_approvingStatus_withdrawsAndRollsBackToPending() {
        Long applyId = 500L;
        PaymentApplyDO approvingApply = PaymentApplyDO.builder()
                .id(applyId)
                .approvalStatus("APPROVING")
                .build();
        when(paymentApplyMapper.selectById(applyId)).thenReturn(approvingApply);

        ApprovalInstanceRespVO instanceResp = new ApprovalInstanceRespVO();
        instanceResp.setId(888L);
        when(approvalInstanceService.getLatestInstanceByBiz("payment", applyId)).thenReturn(instanceResp);

        paymentApplyService.cancelApprovePaymentApply(applyId);

        verify(approvalInstanceService).withdrawInstance(eq(888L), any(ApprovalInstanceWithdrawReqVO.class));
        assertEquals("PENDING", approvingApply.getApprovalStatus());
        verify(paymentApplyMapper).updateById(approvingApply);
    }

}
