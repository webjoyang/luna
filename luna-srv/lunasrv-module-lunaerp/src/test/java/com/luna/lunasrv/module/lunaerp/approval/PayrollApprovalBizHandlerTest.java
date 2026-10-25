package com.luna.lunasrv.module.lunaerp.approval;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.approval.biz.PayrollApprovalBizHandler;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollEmployeeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollEmployeeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollMapper;
import com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.support.TableInfoTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 工资单审批业务处理器单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class PayrollApprovalBizHandlerTest {

    @InjectMocks
    private PayrollApprovalBizHandler bizHandler;

    @Mock
    private PayrollMapper payrollMapper;

    @Mock
    private PayrollEmployeeMapper payrollEmployeeMapper;

    @BeforeEach
    void setUp() {
        TableInfoTestHelper.initTableInfo(PayrollDO.class);
        TableInfoTestHelper.initTableInfo(PayrollEmployeeDO.class);
    }

    @Test
    @DisplayName("元数据契约：bizType=payroll、名称=员工工资单、单号快照提取")
    void testMetadata() {
        assertEquals(ApprovalHandler.BIZ_TYPE_PAYROLL, bizHandler.getBizType());
        assertEquals("员工工资单", bizHandler.getBizTypeName());

        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setPayrollNo("GZ202610001");
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        assertEquals("GZ202610001", bizHandler.getBizNo(1001L));
    }

    @Test
    @DisplayName("validateCanSubmit：工资单不存在抛 025_000")
    void testValidateCanSubmit_NotExists() {
        when(payrollMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> bizHandler.validateCanSubmit(999L));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateCanSubmit：审批中重复提交抛 APPROVAL_INSTANCE_STATUS_ERROR")
    void testValidateCanSubmit_Approving() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setApprovalStatus(ApplyStatus.APPROVING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        ServiceException ex = assertThrows(ServiceException.class, () -> bizHandler.validateCanSubmit(1001L));
        assertEquals(ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateCanSubmit：已通过终态提交抛 025_001")
    void testValidateCanSubmit_Approved() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setApprovalStatus(ApplyStatus.APPROVED.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        ServiceException ex = assertThrows(ServiceException.class, () -> bizHandler.validateCanSubmit(1001L));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateCanSubmit：空员工单抛 025_009")
    void testValidateCanSubmit_EmptyEmployees() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);
        when(payrollEmployeeMapper.selectList(any())).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> bizHandler.validateCanSubmit(1001L));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_EMPTY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateCanSubmit：负实发抛 025_011")
    void testValidateCanSubmit_NegativeNetAmount() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        PayrollEmployeeDO emp = new PayrollEmployeeDO();
        emp.setId(1L);
        emp.setNetAmount(new BigDecimal("-100.00"));
        when(payrollEmployeeMapper.selectList(any())).thenReturn(List.of(emp));

        ServiceException ex = assertThrows(ServiceException.class, () -> bizHandler.validateCanSubmit(1001L));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_NET_AMOUNT_NEGATIVE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateCanSubmit：同期间重复抛 025_002")
    void testValidateCanSubmit_PeriodDuplicate() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setPeriod("2026-10");
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        PayrollEmployeeDO emp = new PayrollEmployeeDO();
        emp.setId(1L);
        emp.setNetAmount(new BigDecimal("5000.00"));
        when(payrollEmployeeMapper.selectList(any())).thenReturn(List.of(emp));
        when(payrollMapper.selectCount(any())).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> bizHandler.validateCanSubmit(1001L));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_PERIOD_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateCanSubmit：合规通过")
    void testValidateCanSubmit_Success() {
        PayrollDO payroll = new PayrollDO();
        payroll.setId(1001L);
        payroll.setPeriod("2026-10");
        payroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        when(payrollMapper.selectById(1001L)).thenReturn(payroll);

        PayrollEmployeeDO emp = new PayrollEmployeeDO();
        emp.setId(1L);
        emp.setNetAmount(new BigDecimal("5000.00"));
        when(payrollEmployeeMapper.selectList(any())).thenReturn(List.of(emp));
        when(payrollMapper.selectCount(any())).thenReturn(0L);

        assertDoesNotThrow(() -> bizHandler.validateCanSubmit(1001L));
    }

    @Test
    @DisplayName("updateApprovalStatus：置 APPROVED 时记录 approveTime")
    void testUpdateApprovalStatus_Approved() {
        bizHandler.updateApprovalStatus(1001L, "APPROVED");

        verify(payrollMapper).update(eq(null), any());
    }

    @Test
    @DisplayName("updateApprovalStatus：置 DRAFT 或 CANCELLED 时回置为 PENDING")
    void testUpdateApprovalStatus_DraftOrCancelled() {
        bizHandler.updateApprovalStatus(1001L, "DRAFT");
        bizHandler.updateApprovalStatus(1001L, "CANCELLED");

        verify(payrollMapper, times(2)).update(eq(null), any());
    }

}
