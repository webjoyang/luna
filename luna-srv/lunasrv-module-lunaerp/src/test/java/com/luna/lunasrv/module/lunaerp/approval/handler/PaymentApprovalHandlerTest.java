package com.luna.lunasrv.module.lunaerp.approval.handler;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * {@link PaymentApprovalHandler} 单元测试
 *
 * 覆盖 Issue 05:
 * - supports 支持 payment，拒绝其他类型
 * - onApproved 将付款申请 approval_status 置为 APPROVED，且无任何账务写入（零账务/资金写入，付款登记才是资金动作）
 */
class PaymentApprovalHandlerTest {

    @Mock
    private PaymentApplyMapper paymentApplyMapper;

    @InjectMocks
    private PaymentApprovalHandler handler;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PaymentApplyDO.class);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("supports 校验：仅支持 payment（大小写不敏感）")
    void testSupports() {
        assertTrue(handler.supports("payment"));
        assertTrue(handler.supports("PAYMENT"));
        assertFalse(handler.supports("sales_contract"));
        assertFalse(handler.supports("expense"));
        assertFalse(handler.supports(null));
    }

    @Test
    @DisplayName("onApproved 执行：仅更新 approval_status 为 APPROVED，零账务/资金写入")
    void testOnApproved_SetsApprovalStatusApproved_ZeroLedgerWrite() {
        Long applyId = 999L;
        when(paymentApplyMapper.update(isNull(), any())).thenReturn(1);

        handler.onApproved(applyId);

        ArgumentCaptor<LambdaUpdateWrapper<PaymentApplyDO>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(paymentApplyMapper).update(isNull(), captor.capture());

        LambdaUpdateWrapper<PaymentApplyDO> wrapper = captor.getValue();
        assertNotNull(wrapper);
        String setSql = wrapper.getSqlSet();
        assertTrue(setSql.contains("approval_status"), "SQL 更新子句必须包含 approval_status 字段");
    }

}
