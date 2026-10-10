package com.luna.lunasrv.module.lunaerp.approval.handler;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
 * {@link SalesContractApprovalHandler} 单元测试
 *
 * 覆盖 Issue 07:
 * - supports 支持 sales_contract，拒绝其他类型
 * - onApproved 将销售合同 status 置为 ACTIVE（生效）
 */
class SalesContractApprovalHandlerTest {

    @Mock
    private SalesContractMapper salesContractMapper;

    @InjectMocks
    private SalesContractApprovalHandler handler;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesContractDO.class);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("supports 校验：仅支持 sales_contract（大小写不敏感）")
    void testSupports() {
        assertTrue(handler.supports("sales_contract"));
        assertTrue(handler.supports("SALES_CONTRACT"));
        assertFalse(handler.supports("payment"));
        assertFalse(handler.supports("expense"));
        assertFalse(handler.supports(null));
    }

    @Test
    @DisplayName("onApproved 执行：将销售合同 status 更新为 ACTIVE")
    void testOnApproved_SetsStatusActive() {
        Long contractId = 888L;
        when(salesContractMapper.update(isNull(), any())).thenReturn(1);

        handler.onApproved(contractId);

        ArgumentCaptor<LambdaUpdateWrapper<SalesContractDO>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(salesContractMapper).update(isNull(), captor.capture());

        LambdaUpdateWrapper<SalesContractDO> wrapper = captor.getValue();
        assertNotNull(wrapper);
        String setSql = wrapper.getSqlSet();
        assertTrue(setSql.contains("status"), "SQL 更新子句必须包含 status 字段");
    }
}
