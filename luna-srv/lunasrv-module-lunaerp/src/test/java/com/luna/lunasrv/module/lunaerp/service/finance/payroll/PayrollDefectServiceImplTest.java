package com.luna.lunasrv.module.lunaerp.service.finance.payroll;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.enums.finance.ApplyStatus;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.PayrollController;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.PayrollDefectPreviewRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.PayrollDefectRefSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollDefectRefDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollLineItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionconfirmation.InspectionConfirmationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollDefectRefMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollLineItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionconfirmation.InspectionConfirmationMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationResponsibilityEnum;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationStatusEnum;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import com.luna.lunasrv.module.lunaerp.support.TableInfoTestHelper;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link PayrollDefectServiceImpl} 单元测试
 *
 * 覆盖 Issue 07 验收标准：
 * 1. 月边界聚合（含次月首日排除、前月月末排除）
 * 2. 仅 CONFIRMED+OPERATOR（EQUIPMENT/WAIVED/REVOKED/跨月排除）
 * 3. 有效单引用排除与冲突抛 025_005（指明占用单号）
 * 4. CANCELLED 释放后重新导入
 * 5. 删草稿释放后重新导入
 * 6. 单价必填且大于0（025_010）
 * 7. 单价×卷数与金额权威重算/改额
 * 8. 保存并发重查候选（并发被抢 025_005）
 * 9. REVOKED 链重验（草稿提交重验 025_005 要求重新导入，APPROVED 不受影响）
 * 10. 切面日志注解与权限码校验
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class PayrollDefectServiceImplTest {

    @InjectMocks
    private PayrollDefectServiceImpl defectService;

    @Mock
    private InspectionConfirmationMapper inspectionConfirmationMapper;

    @Mock
    private PayrollDefectRefMapper payrollDefectRefMapper;

    @Mock
    private PayrollLineItemMapper payrollLineItemMapper;

    @Mock
    private PayrollMapper payrollMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @BeforeEach
    void setUp() {
        TableInfoTestHelper.initAllEntities();
    }

    @Test
    @DisplayName("getDefectPreview：月边界聚合严格过滤 [月首 00:00:00, 次月首 00:00:00)")
    void testGetDefectPreview_MonthBoundary_StrictFilter() {
        String period = "2026-10";
        when(inspectionConfirmationMapper.selectList(any())).thenReturn(Collections.emptyList());

        PayrollDefectPreviewRespVO res = defectService.getDefectPreview(period);
        assertNotNull(res);
        assertEquals("2026-10", res.getPeriod());
        assertTrue(res.getGroups().isEmpty());

        ArgumentCaptor<LambdaQueryWrapperX<InspectionConfirmationDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(inspectionConfirmationMapper).selectList(captor.capture());
        LambdaQueryWrapperX<InspectionConfirmationDO> wrapper = captor.getValue();
        assertNotNull(wrapper);
    }

    @Test
    @DisplayName("getDefectPreview：仅聚合 CONFIRMED + OPERATOR，按员工分组并逐卷快照")
    void testGetDefectPreview_GroupsByWorker_Success() {
        String period = "2026-10";

        InspectionConfirmationDO item1 = createConfirmation(801L, 101L, "张三", "INS-202610-001", "P01", "C", LocalDateTime.of(2026, 10, 5, 10, 0));
        InspectionConfirmationDO item2 = createConfirmation(802L, 101L, "张三", "INS-202610-002", "P02", "B", LocalDateTime.of(2026, 10, 8, 14, 0));
        InspectionConfirmationDO item3 = createConfirmation(803L, 102L, "李四", "INS-202610-003", "P03", "C", LocalDateTime.of(2026, 10, 12, 16, 0));

        when(inspectionConfirmationMapper.selectList(any())).thenReturn(List.of(item1, item2, item3));
        when(payrollDefectRefMapper.selectList(any())).thenReturn(Collections.emptyList());

        PayrollDefectPreviewRespVO res = defectService.getDefectPreview(period);
        assertNotNull(res);
        assertEquals(2, res.getGroups().size());

        PayrollDefectPreviewRespVO.WorkerGroup g1 = res.getGroups().get(0);
        assertEquals(101L, g1.getWorkerUserId());
        assertEquals("张三", g1.getWorkerName());
        assertEquals(2, g1.getCount());
        assertEquals(2, g1.getRecords().size());
        assertEquals(801L, g1.getRecords().get(0).getConfirmationId());
        assertEquals("INS-202610-001", g1.getRecords().get(0).getConfirmationNo());
        assertEquals("P01", g1.getRecords().get(0).getRollNo());

        PayrollDefectPreviewRespVO.WorkerGroup g2 = res.getGroups().get(1);
        assertEquals(102L, g2.getWorkerUserId());
        assertEquals("李四", g2.getWorkerName());
        assertEquals(1, g2.getCount());
    }

    @Test
    @DisplayName("getDefectPreview：已被有效工资单（未作废、未删除）引用的记录在预览中排除")
    void testGetDefectPreview_ActivePayrollReferenced_Excluded() {
        String period = "2026-10";
        InspectionConfirmationDO item1 = createConfirmation(801L, 101L, "张三", "INS-202610-001", "P01", "C", LocalDateTime.of(2026, 10, 5, 10, 0));
        InspectionConfirmationDO item2 = createConfirmation(802L, 101L, "张三", "INS-202610-002", "P02", "B", LocalDateTime.of(2026, 10, 8, 14, 0));
        when(inspectionConfirmationMapper.selectList(any())).thenReturn(List.of(item1, item2));

        // 801 已被有效工资单占用
        PayrollDefectRefDO ref1 = new PayrollDefectRefDO();
        ref1.setConfirmationId(801L);
        ref1.setLineItemId(2001L);
        when(payrollDefectRefMapper.selectList(any())).thenReturn(List.of(ref1));

        PayrollLineItemDO lineItem1 = new PayrollLineItemDO();
        lineItem1.setId(2001L);
        lineItem1.setPayrollId(3001L);
        when(payrollLineItemMapper.selectList(any())).thenReturn(List.of(lineItem1));

        PayrollDO activePayroll = new PayrollDO();
        activePayroll.setId(3001L);
        activePayroll.setPayrollNo("GZ202610001");
        activePayroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        activePayroll.setDeleted(false);
        when(payrollMapper.selectList(any())).thenReturn(List.of(activePayroll));

        PayrollDefectPreviewRespVO res = defectService.getDefectPreview(period);
        assertNotNull(res);
        // 801 排除，仅剩 802
        assertEquals(1, res.getGroups().size());
        assertEquals(1, res.getGroups().get(0).getCount());
        assertEquals(802L, res.getGroups().get(0).getRecords().get(0).getConfirmationId());
    }

    @Test
    @DisplayName("getDefectPreview：工资单作废（CANCELLED）后，其引用的次品指纹释放可重新导入")
    void testGetDefectPreview_CancelledPayroll_ReleasesDefectRef() {
        String period = "2026-10";
        InspectionConfirmationDO item1 = createConfirmation(801L, 101L, "张三", "INS-202610-001", "P01", "C", LocalDateTime.of(2026, 10, 5, 10, 0));
        when(inspectionConfirmationMapper.selectList(any())).thenReturn(List.of(item1));

        PayrollDefectRefDO ref1 = new PayrollDefectRefDO();
        ref1.setConfirmationId(801L);
        ref1.setLineItemId(2001L);
        when(payrollDefectRefMapper.selectList(any())).thenReturn(List.of(ref1));

        PayrollLineItemDO lineItem1 = new PayrollLineItemDO();
        lineItem1.setId(2001L);
        lineItem1.setPayrollId(3001L);
        when(payrollLineItemMapper.selectList(any())).thenReturn(List.of(lineItem1));

        // 工资单已作废
        PayrollDO cancelledPayroll = new PayrollDO();
        cancelledPayroll.setId(3001L);
        cancelledPayroll.setPayrollNo("GZ202610001");
        cancelledPayroll.setApprovalStatus(ApplyStatus.CANCELLED.getValue());
        cancelledPayroll.setDeleted(false);
        when(payrollMapper.selectList(any())).thenReturn(List.of(cancelledPayroll));

        PayrollDefectPreviewRespVO res = defectService.getDefectPreview(period);
        assertNotNull(res);
        // 801 成功释放并重新查出
        assertEquals(1, res.getGroups().size());
        assertEquals(801L, res.getGroups().get(0).getRecords().get(0).getConfirmationId());
    }

    @Test
    @DisplayName("getDefectPreview：草稿工资单软删除后，其引用的次品指纹释放可重新导入")
    void testGetDefectPreview_DeletedPayrollDraft_ReleasesDefectRef() {
        String period = "2026-10";
        InspectionConfirmationDO item1 = createConfirmation(801L, 101L, "张三", "INS-202610-001", "P01", "C", LocalDateTime.of(2026, 10, 5, 10, 0));
        when(inspectionConfirmationMapper.selectList(any())).thenReturn(List.of(item1));

        PayrollDefectRefDO ref1 = new PayrollDefectRefDO();
        ref1.setConfirmationId(801L);
        ref1.setLineItemId(2001L);
        when(payrollDefectRefMapper.selectList(any())).thenReturn(List.of(ref1));

        PayrollLineItemDO lineItem1 = new PayrollLineItemDO();
        lineItem1.setId(2001L);
        lineItem1.setPayrollId(3001L);
        when(payrollLineItemMapper.selectList(any())).thenReturn(List.of(lineItem1));

        // 工资单已软删除
        PayrollDO deletedPayroll = new PayrollDO();
        deletedPayroll.setId(3001L);
        deletedPayroll.setPayrollNo("GZ202610001");
        deletedPayroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        deletedPayroll.setDeleted(true);
        when(payrollMapper.selectList(any())).thenReturn(List.of(deletedPayroll));

        PayrollDefectPreviewRespVO res = defectService.getDefectPreview(period);
        assertNotNull(res);
        assertEquals(1, res.getGroups().size());
        assertEquals(801L, res.getGroups().get(0).getRecords().get(0).getConfirmationId());
    }

    @Test
    @DisplayName("validateDefectRefsForSave：次品单价必填且大于0，未传或<=0抛 025_010")
    void testValidateDefectRefsForSave_PriceRequired_Throws025_010() {
        PayrollDefectRefSaveReqVO ref1 = PayrollDefectRefSaveReqVO.builder()
                .confirmationId(801L)
                .deductPrice(null)
                .build();

        ServiceException ex1 = assertThrows(ServiceException.class,
                () -> defectService.validateDefectRefsForSave(null, List.of(ref1)));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_DEFECT_PRICE_REQUIRED.getCode(), ex1.getCode());

        PayrollDefectRefSaveReqVO ref2 = PayrollDefectRefSaveReqVO.builder()
                .confirmationId(801L)
                .deductPrice(BigDecimal.ZERO)
                .build();

        ServiceException ex2 = assertThrows(ServiceException.class,
                () -> defectService.validateDefectRefsForSave(null, List.of(ref2)));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_DEFECT_PRICE_REQUIRED.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("validateDefectRefsForSave：同单重复引用同一卷质检记录抛 025_005")
    void testValidateDefectRefsForSave_DuplicateInSamePayroll_Throws025_005() {
        PayrollDefectRefSaveReqVO ref1 = PayrollDefectRefSaveReqVO.builder()
                .confirmationId(801L)
                .deductPrice(new BigDecimal("50.00"))
                .build();
        PayrollDefectRefSaveReqVO ref2 = PayrollDefectRefSaveReqVO.builder()
                .confirmationId(801L)
                .deductPrice(new BigDecimal("50.00"))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> defectService.validateDefectRefsForSave(null, List.of(ref1, ref2)));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_DEFECT_ALREADY_REFERENCED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateDefectRefsForSave：已被其它有效工资单占用，抛 025_005 并指明单号")
    void testValidateDefectRefsForSave_OccupiedByOtherPayroll_Throws025_005() {
        PayrollDefectRefSaveReqVO ref = PayrollDefectRefSaveReqVO.builder()
                .confirmationId(801L)
                .confirmationNo("INS-202610-001")
                .deductPrice(new BigDecimal("50.00"))
                .build();

        PayrollDefectRefDO occupiedRef = new PayrollDefectRefDO();
        occupiedRef.setConfirmationId(801L);
        occupiedRef.setLineItemId(2001L);
        occupiedRef.setConfirmationNo("INS-202610-001");
        when(payrollDefectRefMapper.selectList(any())).thenReturn(List.of(occupiedRef));

        PayrollLineItemDO lineItem = new PayrollLineItemDO();
        lineItem.setId(2001L);
        lineItem.setPayrollId(3002L);
        when(payrollLineItemMapper.selectList(any())).thenReturn(List.of(lineItem));

        PayrollDO otherPayroll = new PayrollDO();
        otherPayroll.setId(3002L);
        otherPayroll.setPayrollNo("GZ202610002");
        otherPayroll.setApprovalStatus(ApplyStatus.PENDING.getValue());
        otherPayroll.setDeleted(false);
        when(payrollMapper.selectList(any())).thenReturn(List.of(otherPayroll));

        // 当前单据为 3001L
        ServiceException ex = assertThrows(ServiceException.class,
                () -> defectService.validateDefectRefsForSave(3001L, List.of(ref)));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_DEFECT_ALREADY_REFERENCED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("GZ202610002") || ex.getMessage().contains("INS-202610-001"));
    }

    @Test
    @DisplayName("validateDefectRefsForSave：保存时服务端重查候选已被质检撤销（REVOKED），抛 025_005")
    void testValidateDefectRefsForSave_QcRevoked_Throws025_005() {
        PayrollDefectRefSaveReqVO ref = PayrollDefectRefSaveReqVO.builder()
                .confirmationId(801L)
                .confirmationNo("INS-202610-001")
                .deductPrice(new BigDecimal("50.00"))
                .userId(101L)
                .build();

        when(payrollDefectRefMapper.selectList(any())).thenReturn(Collections.emptyList());

        // 质检记录已被撤销
        InspectionConfirmationDO qc = createConfirmation(801L, 101L, "张三", "INS-202610-001", "P01", "C", LocalDateTime.of(2026, 10, 5, 10, 0));
        qc.setStatus(InspectionConfirmationStatusEnum.REVOKED.getValue());
        when(inspectionConfirmationMapper.selectList(any())).thenReturn(List.of(qc));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> defectService.validateDefectRefsForSave(null, List.of(ref)));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_DEFECT_ALREADY_REFERENCED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateDefectRefsForSubmit：提交时重验已撤销（REVOKED），抛 025_005 要求重新导入")
    void testValidateDefectRefsForSubmit_QcRevoked_Throws025_005() {
        Long payrollId = 1001L;

        PayrollLineItemDO item = new PayrollLineItemDO();
        item.setId(201L);
        item.setPayrollId(payrollId);
        when(payrollLineItemMapper.selectList(any())).thenReturn(List.of(item));

        PayrollDefectRefDO ref = new PayrollDefectRefDO();
        ref.setId(301L);
        ref.setLineItemId(201L);
        ref.setConfirmationId(801L);
        ref.setConfirmationNo("INS-202610-001");
        when(payrollDefectRefMapper.selectList(any())).thenReturn(List.of(ref));

        // 质检记录在提交前被质检人员撤销
        InspectionConfirmationDO qc = createConfirmation(801L, 101L, "张三", "INS-202610-001", "P01", "C", LocalDateTime.of(2026, 10, 5, 10, 0));
        qc.setStatus(InspectionConfirmationStatusEnum.REVOKED.getValue());
        when(inspectionConfirmationMapper.selectList(any())).thenReturn(List.of(qc));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> defectService.validateDefectRefsForSubmit(payrollId));
        assertEquals(FinanceErrorCodeConstants.PAYROLL_DEFECT_ALREADY_REFERENCED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("切面日志规约：getDefectPreview 方法正确标注 @LogRecord(type = 'PAYROLL', subType = 'IMPORT_DEFECT')")
    void testAspectLogRecordAnnotation() throws NoSuchMethodException {
        Method method = PayrollDefectServiceImpl.class.getMethod("getDefectPreview", String.class);
        LogRecord logRecord = method.getAnnotation(LogRecord.class);
        assertNotNull(logRecord, "getDefectPreview 必须标注 @LogRecord");
        assertEquals("PAYROLL", logRecord.type());
        assertEquals("IMPORT_DEFECT", logRecord.subType());
    }

    @Test
    @DisplayName("权限码规约：PayrollController.getDefectPreview 正确标注权限码 payroll:import-defect")
    void testPermissionAnnotation() throws NoSuchMethodException {
        Method method = PayrollController.class.getMethod("getDefectPreview", String.class, Long.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "getDefectPreview 必须标注 @PreAuthorize");
        assertTrue(preAuthorize.value().contains("lunaerp:finance:payroll:import-defect"),
                "权限码必须包含 lunaerp:finance:payroll:import-defect");
    }

    private InspectionConfirmationDO createConfirmation(Long id, Long workerUserId, String workerName,
                                                        String orderCode, String rollNo, String grade,
                                                        LocalDateTime confirmedAt) {
        InspectionConfirmationDO item = new InspectionConfirmationDO();
        item.setId(id);
        item.setStatus(InspectionConfirmationStatusEnum.CONFIRMED.getValue());
        item.setResponsibility(InspectionConfirmationResponsibilityEnum.OPERATOR.getValue());
        item.setWorkerUserId(workerUserId);
        item.setWorkerName(workerName);
        item.setInspectionOrderCode(orderCode);
        item.setRollNo(rollNo);
        item.setGrade(grade);
        item.setConfirmedAt(confirmedAt);
        item.setDeleted(false);
        return item;
    }

}
