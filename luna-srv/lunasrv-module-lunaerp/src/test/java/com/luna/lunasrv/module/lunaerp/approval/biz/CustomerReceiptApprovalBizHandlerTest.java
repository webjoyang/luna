package com.luna.lunasrv.module.lunaerp.approval.biz;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link CustomerReceiptApprovalBizHandler} 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class CustomerReceiptApprovalBizHandlerTest {

    @InjectMocks
    private CustomerReceiptApprovalBizHandler handler;

    @Mock
    private PaymentMapper paymentMapper;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentDO.class);
    }

    @Test
    @DisplayName("基础元数据：bizType 为 CUSTOMER_RECEIPT，名称为 客户收款")
    void testMetadata() {
        assertEquals("CUSTOMER_RECEIPT", handler.getBizType());
        assertEquals("客户收款", handler.getBizTypeName());
    }

    @Test
    @DisplayName("获取单据单号快照")
    void testGetBizNo() {
        PaymentDO payment = new PaymentDO();
        payment.setId(101L);
        payment.setPaymentNo("RC-20261016-001");
        when(paymentMapper.selectById(101L)).thenReturn(payment);

        assertEquals("RC-20261016-001", handler.getBizNo(101L));
    }

    @Test
    @DisplayName("提交校验：单据不存在抛 RECEIPT_NOT_EXISTS，非 PENDING/REJECTED 抛 RECEIPT_STATUS_NOT_ALLOW")
    void testValidateCanSubmit() {
        when(paymentMapper.selectById(999L)).thenReturn(null);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> handler.validateCanSubmit(999L));
        assertEquals(FinanceErrorCodeConstants.RECEIPT_NOT_EXISTS.getCode(), ex1.getCode());

        PaymentDO payment = new PaymentDO();
        payment.setId(101L);
        payment.setDirection("IN");
        payment.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        when(paymentMapper.selectById(101L)).thenReturn(payment);

        ServiceException ex2 = assertThrows(ServiceException.class, () -> handler.validateCanSubmit(101L));
        assertEquals(FinanceErrorCodeConstants.RECEIPT_STATUS_NOT_ALLOW.getCode(), ex2.getCode());

        // 仅 PENDING 校验通过，REJECTED 亦不可提交
        payment.setApprovalStatus(ApplyStatus.PENDING.getValue());
        assertDoesNotThrow(() -> handler.validateCanSubmit(101L));

        payment.setApprovalStatus(ApplyStatus.REJECTED.getValue());
        assertThrows(ServiceException.class, () -> handler.validateCanSubmit(101L));
    }

    @Test
    @DisplayName("更新审批状态：回写 approval_status 字段")
    void testUpdateApprovalStatus() {
        handler.updateApprovalStatus(101L, ApplyStatus.APPROVING.getValue());
        verify(paymentMapper).update(any(), any(LambdaUpdateWrapper.class));
    }

}
