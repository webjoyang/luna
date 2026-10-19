package com.luna.lunasrv.module.lunaerp.service.finance.payroll;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollEmployeeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollLineItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link PayrollRecycleBinHandler} 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class PayrollRecycleBinHandlerTest {

    @Mock
    private PayrollMapper payrollMapper;

    @Mock
    private PayrollEmployeeMapper payrollEmployeeMapper;

    @Mock
    private PayrollLineItemMapper payrollLineItemMapper;

    @InjectMocks
    private PayrollRecycleBinHandler handler;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("getBizType 契约：返回固定值 PAYROLL")
    void testGetBizType() {
        assertEquals("PAYROLL", handler.getBizType());
    }

    @Test
    @DisplayName("pageDeleted：正确构建分页并委托 PayrollMapper")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizId(1001L);
            item.setBizType("PAYROLL");
            item.setDisplayName("GZ202610001");
            page.setRecords(List.of(item));
            page.setTotal(1L);
            return page;
        }).when(payrollMapper).selectRecyclePage(any(), eq(TENANT_ID));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("GZ202610001", result.getList().get(0).getDisplayName());
        verify(payrollMapper).selectRecyclePage(any(), eq(TENANT_ID));
    }

    @Test
    @DisplayName("validateRestore：不存在或非删除状态抛 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testValidateRestore_NotFound() {
        when(payrollMapper.selectDeletedById(999L, TENANT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(999L));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("restore：恢复主表及员工行、款项行，状态置 PENDING")
    void testRestore_Success() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        when(payrollMapper.selectDeletedById(1001L, TENANT_ID)).thenReturn(payroll);
        when(payrollMapper.restorePayroll(eq(1001L), eq(TENANT_ID), any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> handler.restore(1001L));

        verify(payrollMapper).restorePayroll(eq(1001L), eq(TENANT_ID), any(), any());
        verify(payrollEmployeeMapper).restorePayrollEmployees(eq(1001L), eq(TENANT_ID), any(), any());
        verify(payrollLineItemMapper).restorePayrollLineItems(eq(1001L), eq(TENANT_ID), any(), any());
    }

    @Test
    @DisplayName("validatePurge：不存在抛 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testValidatePurge_NotFound() {
        when(payrollMapper.selectDeletedById(999L, TENANT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validatePurge(999L));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("purge：彻底物理删除款项明细、员工汇总及主表")
    void testPurge_Success() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        when(payrollMapper.selectDeletedById(1001L, TENANT_ID)).thenReturn(payroll);
        when(payrollMapper.purgePayroll(1001L, TENANT_ID)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(1001L));

        verify(payrollLineItemMapper).purgePayrollLineItems(1001L, TENANT_ID);
        verify(payrollEmployeeMapper).purgePayrollEmployees(1001L, TENANT_ID);
        verify(payrollMapper).purgePayroll(1001L, TENANT_ID);
    }

    @Test
    @DisplayName("hasActiveReference：草稿单恒返回 false")
    void testHasActiveReference() {
        assertFalse(handler.hasActiveReference(1001L));
    }

}
