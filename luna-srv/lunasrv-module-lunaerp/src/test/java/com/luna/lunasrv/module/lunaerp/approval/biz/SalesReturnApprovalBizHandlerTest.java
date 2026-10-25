package com.luna.lunasrv.module.lunaerp.approval.biz;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnMapper;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link SalesReturnApprovalBizHandler} 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class SalesReturnApprovalBizHandlerTest {

    @InjectMocks
    private SalesReturnApprovalBizHandler handler;

    @Mock
    private SalesReturnMapper salesReturnMapper;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesReturnDO.class);
    }

    @Test
    @DisplayName("基础元数据：bizType 为 sales_return，名称为 销售退货")
    void testMetadata() {
        assertEquals(ApprovalHandler.BIZ_TYPE_SALES_RETURN, handler.getBizType());
        assertEquals("销售退货", handler.getBizTypeName());
    }

    @Test
    @DisplayName("获取单据单号快照")
    void testGetBizNo() {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(10L);
        ret.setReturnNo("RT-20261018-001");
        when(salesReturnMapper.selectById(10L)).thenReturn(ret);

        assertEquals("RT-20261018-001", handler.getBizNo(10L));
    }

    @Test
    @DisplayName("提交校验：单据不存在抛 000，非 DRAFT 或非有效审批状态抛 002")
    void testValidateCanSubmit() {
        when(salesReturnMapper.selectById(999L)).thenReturn(null);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> handler.validateCanSubmit(999L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS.getCode(), ex1.getCode());

        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(10L);
        ret.setStatus("APPROVED");
        ret.setApprovalStatus("APPROVED");
        when(salesReturnMapper.selectById(10L)).thenReturn(ret);

        ServiceException ex2 = assertThrows(ServiceException.class, () -> handler.validateCanSubmit(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex2.getCode());

        // DRAFT + PENDING / REJECTED / APPROVING 均校验通过
        ret.setStatus("DRAFT");
        ret.setApprovalStatus("PENDING");
        assertDoesNotThrow(() -> handler.validateCanSubmit(10L));

        ret.setApprovalStatus("REJECTED");
        assertDoesNotThrow(() -> handler.validateCanSubmit(10L));

        ret.setApprovalStatus("APPROVING");
        assertDoesNotThrow(() -> handler.validateCanSubmit(10L));

        ret.setApprovalStatus("CANCELLED");
        assertThrows(ServiceException.class, () -> handler.validateCanSubmit(10L));
    }

    @Test
    @DisplayName("更新审批状态：回写 approval_status 字段，DRAFT 映射回 PENDING")
    void testUpdateApprovalStatus() {
        handler.updateApprovalStatus(10L, "DRAFT");
        verify(salesReturnMapper).update(any(), any(LambdaUpdateWrapper.class));

        handler.updateApprovalStatus(10L, "APPROVING");
        verify(salesReturnMapper, times(2)).update(any(), any(LambdaUpdateWrapper.class));

        handler.updateApprovalStatus(10L, "REJECTED");
        verify(salesReturnMapper, times(3)).update(any(), any(LambdaUpdateWrapper.class));
    }


}
