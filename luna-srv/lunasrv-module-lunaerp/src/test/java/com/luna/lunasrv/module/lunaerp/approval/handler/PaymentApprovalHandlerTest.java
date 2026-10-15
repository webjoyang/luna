package com.luna.lunasrv.module.lunaerp.approval.handler;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentApplyMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentApprovalHandler} 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class PaymentApprovalHandlerTest {

    @InjectMocks
    private PaymentApprovalHandler paymentApprovalHandler;

    @Mock
    private PaymentApplyMapper paymentApplyMapper;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentApplyDO.class);
    }

    @Test
    @DisplayName("supports 契约：支持 payment 业务单据类型")
    void testSupports() {
        assertTrue(paymentApprovalHandler.supports("payment"));
        assertTrue(paymentApprovalHandler.supports("PAYMENT"));
        assertFalse(paymentApprovalHandler.supports("sales_contract"));
    }

    @Test
    @DisplayName("onApproved 关键语义：仅更新 approval_status=APPROVED，零账务写入")
    void testOnApproved_onlyUpdatesStatusAndZeroLedgerWrites() {
        Long bizId = 1001L;
        when(paymentApplyMapper.update(isNull(), any())).thenReturn(1);

        paymentApprovalHandler.onApproved(bizId);

        // 验证且仅验证调用了 paymentApplyMapper.update
        verify(paymentApplyMapper, times(1)).update(isNull(), any());

        // 结构审查：PaymentApprovalHandler 类中除 PaymentApplyMapper 之外，严禁注入任何资金账户、分录账或收付款登记 Mapper/Service
        Field[] fields = PaymentApprovalHandler.class.getDeclaredFields();
        for (Field field : fields) {
            String typeName = field.getType().getSimpleName();
            assertFalse(typeName.contains("Account"), "PaymentApprovalHandler 严禁注入资金账户组件 (零账务写入规约)");
            assertFalse(typeName.contains("Ledger"), "PaymentApprovalHandler 严禁注入总账分录组件 (零账务写入规约)");
            assertFalse(typeName.contains("PaymentMapper"), "PaymentApprovalHandler 严禁注入收付款单主表 (付款登记前禁止生单)");
        }
    }

}
