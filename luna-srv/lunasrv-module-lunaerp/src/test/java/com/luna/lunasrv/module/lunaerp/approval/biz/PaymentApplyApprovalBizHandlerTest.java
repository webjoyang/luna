package com.luna.lunasrv.module.lunaerp.approval.biz;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.PAYMENT_APPLY_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentApplyApprovalBizHandler} 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class PaymentApplyApprovalBizHandlerTest {

    @InjectMocks
    private PaymentApplyApprovalBizHandler handler;

    @Mock
    private PaymentApplyMapper paymentApplyMapper;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentApplyDO.class);
    }

    @Test
    @DisplayName("契约：bizType 为 payment，中文名称为 付款申请")
    void testBizTypeAndName() {
        assertEquals("payment", handler.getBizType());
        assertEquals("付款申请", handler.getBizTypeName());
    }

    @Test
    @DisplayName("getBizNo：返回付款申请单号")
    void testGetBizNo() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setApplyNo("FK20261015001");
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        assertEquals("FK20261015001", handler.getBizNo(1001L));
        assertNull(handler.getBizNo(9999L));
    }

    @Test
    @DisplayName("validateCanSubmit：单据不存在报 023_000")
    void testValidateCanSubmit_notExists() {
        when(paymentApplyMapper.selectById(1001L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateCanSubmit(1001L));
        assertEquals(PAYMENT_APPLY_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateCanSubmit：审批中不可重复提交")
    void testValidateCanSubmit_approvingThrows() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);
        apply.setApprovalStatus(ApplyStatus.APPROVING.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateCanSubmit(1001L));
        assertEquals(APPROVAL_INSTANCE_STATUS_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateCanSubmit：PENDING/DRAFT/REJECTED 允许提交")
    void testValidateCanSubmit_validStatuses() {
        PaymentApplyDO apply = new PaymentApplyDO();
        apply.setId(1001L);

        apply.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(paymentApplyMapper.selectById(1001L)).thenReturn(apply);
        assertDoesNotThrow(() -> handler.validateCanSubmit(1001L));

        apply.setApprovalStatus("DRAFT");
        assertDoesNotThrow(() -> handler.validateCanSubmit(1001L));

        apply.setApprovalStatus(ApplyStatus.REJECTED.getValue());
        assertDoesNotThrow(() -> handler.validateCanSubmit(1001L));
    }

    @Test
    @DisplayName("updateApprovalStatus：更新单据 approval_status")
    void testUpdateApprovalStatus() {
        when(paymentApplyMapper.update(isNull(), any())).thenReturn(1);
        handler.updateApprovalStatus(1001L, "APPROVING");
        verify(paymentApplyMapper).update(isNull(), any());
    }

}
