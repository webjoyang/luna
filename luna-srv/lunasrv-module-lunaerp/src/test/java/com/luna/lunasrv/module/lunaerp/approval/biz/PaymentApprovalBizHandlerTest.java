package com.luna.lunasrv.module.lunaerp.approval.biz;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.PAYMENT_APPLY_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentApprovalBizHandler} 单元测试
 */
class PaymentApprovalBizHandlerTest {

    @Mock
    private PaymentApplyMapper paymentApplyMapper;

    @InjectMocks
    private PaymentApprovalBizHandler handler;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentApplyDO.class);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("基本信息校验：bizType 为 payment，bizTypeName 为 付款申请")
    void testBasicInfo() {
        assertEquals("payment", handler.getBizType());
        assertEquals("付款申请", handler.getBizTypeName());
    }

    @Test
    @DisplayName("getBizNo：返回 applyNo")
    void testGetBizNo() {
        PaymentApplyDO apply = PaymentApplyDO.builder().id(100L).applyNo("FK20261011001").build();
        when(paymentApplyMapper.selectById(100L)).thenReturn(apply);

        assertEquals("FK20261011001", handler.getBizNo(100L));
    }

    @Test
    @DisplayName("validateCanSubmit：不存在抛 023_000，审批中抛不可重复提交")
    void testValidateCanSubmit() {
        when(paymentApplyMapper.selectById(1L)).thenReturn(null);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> handler.validateCanSubmit(1L));
        assertEquals(PAYMENT_APPLY_NOT_EXISTS.getCode(), ex1.getCode());

        PaymentApplyDO approvingApply = PaymentApplyDO.builder().id(2L).approvalStatus("APPROVING").build();
        when(paymentApplyMapper.selectById(2L)).thenReturn(approvingApply);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> handler.validateCanSubmit(2L));
        assertEquals(APPROVAL_INSTANCE_STATUS_ERROR.getCode(), ex2.getCode());

        PaymentApplyDO pendingApply = PaymentApplyDO.builder().id(3L).approvalStatus("PENDING").build();
        when(paymentApplyMapper.selectById(3L)).thenReturn(pendingApply);
        assertDoesNotThrow(() -> handler.validateCanSubmit(3L));
    }

    @Test
    @DisplayName("updateApprovalStatus：更新状态字段")
    void testUpdateApprovalStatus() {
        when(paymentApplyMapper.update(isNull(), any())).thenReturn(1);

        handler.updateApprovalStatus(100L, "APPROVED");

        ArgumentCaptor<LambdaUpdateWrapper<PaymentApplyDO>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(paymentApplyMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<PaymentApplyDO> wrapper = captor.getValue();
        assertNotNull(wrapper);
        assertTrue(wrapper.getSqlSet().contains("approval_status"));
    }

}
