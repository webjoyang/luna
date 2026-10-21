package com.luna.lunasrv.module.lunaerp.service.h5.workshop;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTaskRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTasksPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper;
import com.luna.lunasrv.module.lunaerp.support.TableInfoTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link WorkshopH5ServiceImpl} 单元测试
 *
 * @author luna
 */
class WorkshopH5ServiceImplTest {

    @InjectMocks
    private WorkshopH5ServiceImpl workshopH5Service;

    @Mock
    private MachineTaskMapper machineTaskMapper;

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @Mock
    private WeavingTicketMapper weavingTicketMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoTestHelper.initTableInfo(MachineTaskDO.class, WeavingOrderDO.class, WeavingTicketDO.class);
    }

    @Test
    @DisplayName("my-tasks: 正常获取我的机台任务分页，附带织造单快照与布票状态计数（total 排除作废票）")
    void testGetMyTasksPage_success() {
        Long workerUserId = 1001L;
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(workerUserId);

            // 1. Mock 机台任务数据
            MachineTaskDO task1 = MachineTaskDO.builder()
                    .id(10L)
                    .code("MT-ZZ20261021-01")
                    .weavingOrderId(200L)
                    .loomId(1L)
                    .loomNo("LM01")
                    .workerUserId(workerUserId)
                    .workerName("挡车工甲")
                    .plannedQty(20)
                    .actualQty(5)
                    .status("IN_PROGRESS")
                    .plannedStart(LocalDate.of(2026, 10, 20))
                    .plannedEnd(LocalDate.of(2026, 10, 25))
                    .build();

            MachineTaskDO task2 = MachineTaskDO.builder()
                    .id(11L)
                    .code("MT-ZZ20261021-02")
                    .weavingOrderId(201L)
                    .loomId(2L)
                    .loomNo("LM02")
                    .workerUserId(workerUserId)
                    .workerName("挡车工甲")
                    .plannedQty(10)
                    .actualQty(0)
                    .status("PENDING")
                    .build();

            when(machineTaskMapper.selectPage(any(WorkshopMyTasksPageReqVO.class), any(LambdaQueryWrapperX.class)))
                    .thenReturn(new PageResult<>(List.of(task1, task2), 2L));

            // 2. Mock 织造单批量快照
            WeavingOrderDO order1 = new WeavingOrderDO();
            order1.setId(200L);
            order1.setCode("ZZ-20261021-01");
            order1.setProductName("40支精梳纯棉汗布");
            order1.setProductCode("PRD-COTTON-40S");

            WeavingOrderDO order2 = new WeavingOrderDO();
            order2.setId(201L);
            order2.setCode("ZZ-20261021-02");
            order2.setProductName("32支涤棉混纺布");
            order2.setProductCode("PRD-TC-32S");

            when(weavingOrderMapper.selectBatchIds(any(Collection.class))).thenReturn(List.of(order1, order2));

            // 3. Mock 布票投影统计（含 VOID 作废票和 STOCKED 入库票）
            WeavingTicketDO ticket1 = WeavingTicketDO.builder().machineTaskId(10L).status("GENERATED").build();
            WeavingTicketDO ticket2 = WeavingTicketDO.builder().machineTaskId(10L).status("STARTED").build();
            WeavingTicketDO ticket3 = WeavingTicketDO.builder().machineTaskId(10L).status("COMPLETED").build();
            WeavingTicketDO ticket4 = WeavingTicketDO.builder().machineTaskId(10L).status("STOCKED").build();
            WeavingTicketDO ticketVoid = WeavingTicketDO.builder().machineTaskId(10L).status("VOID").build();
            WeavingTicketDO ticket5 = WeavingTicketDO.builder().machineTaskId(11L).status("EXCEPTION").build();

            when(weavingTicketMapper.selectList(any(LambdaQueryWrapperX.class)))
                    .thenReturn(List.of(ticket1, ticket2, ticket3, ticket4, ticketVoid, ticket5));

            // 4. 调用
            WorkshopMyTasksPageReqVO reqVO = new WorkshopMyTasksPageReqVO();
            reqVO.setPageNo(1);
            reqVO.setPageSize(10);

            PageResult<WorkshopMyTaskRespVO> result = workshopH5Service.getMyTasksPage(reqVO);

            // 5. 校验结果
            assertNotNull(result);
            assertEquals(2L, result.getTotal());
            assertEquals(2, result.getList().size());

            WorkshopMyTaskRespVO vo1 = result.getList().get(0);
            assertEquals(10L, vo1.getId());
            assertEquals("MT-ZZ20261021-01", vo1.getCode());
            assertEquals("ZZ-20261021-01", vo1.getWeavingOrderCode());
            assertEquals("40支精梳纯棉汗布", vo1.getProductName());
            assertEquals("PRD-COTTON-40S", vo1.getProductCode());
            assertEquals("LM01", vo1.getLoomNo());
            assertEquals(20, vo1.getPlannedQty());
            assertEquals(5, vo1.getActualQty());
            assertEquals("IN_PROGRESS", vo1.getStatus());

            assertNotNull(vo1.getTicketCounts());
            // total 严格排除 VOID 票：1 GENERATED + 1 STARTED + 1 COMPLETED + 1 STOCKED = 4
            assertEquals(4, vo1.getTicketCounts().getTotal());
            assertEquals(1, vo1.getTicketCounts().getGenerated());
            assertEquals(1, vo1.getTicketCounts().getStarted());
            assertEquals(1, vo1.getTicketCounts().getCompleted());
            assertEquals(1, vo1.getTicketCounts().getStocked());
            assertEquals(1, vo1.getTicketCounts().getVoided());
            assertEquals(0, vo1.getTicketCounts().getInspected());

            WorkshopMyTaskRespVO vo2 = result.getList().get(1);
            assertEquals(11L, vo2.getId());
            assertEquals("ZZ-20261021-02", vo2.getWeavingOrderCode());
            assertEquals(1, vo2.getTicketCounts().getTotal());
            assertEquals(1, vo2.getTicketCounts().getException());

            // 6. 验证查询条件使用了当前登录人硬过滤
            ArgumentCaptor<LambdaQueryWrapperX> queryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
            verify(machineTaskMapper).selectPage(eq(reqVO), queryCaptor.capture());
            String sql = queryCaptor.getValue().getCustomSqlSegment();
            assertNotNull(sql);
        }
    }

    @Test
    @DisplayName("my-tasks: 自定义状态参数过滤")
    void testGetMyTasksPage_withCustomStatus() {
        Long workerUserId = 1002L;
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(workerUserId);

            when(machineTaskMapper.selectPage(any(WorkshopMyTasksPageReqVO.class), any(LambdaQueryWrapperX.class)))
                    .thenReturn(PageResult.empty(0L));

            WorkshopMyTasksPageReqVO reqVO = new WorkshopMyTasksPageReqVO();
            reqVO.setStatus("IN_PROGRESS, HALTED");

            PageResult<WorkshopMyTaskRespVO> result = workshopH5Service.getMyTasksPage(reqVO);
            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertTrue(result.getList().isEmpty());

            verify(weavingOrderMapper, never()).selectBatchIds(any());
            verify(weavingTicketMapper, never()).selectList(any());
        }
    }

    @Test
    @DisplayName("my-tasks: 未登录时抛出 401 UNAUTHORIZED")
    void testGetMyTasksPage_unauthorized() {
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            WorkshopMyTasksPageReqVO reqVO = new WorkshopMyTasksPageReqVO();
            ServiceException ex = assertThrows(ServiceException.class, () -> workshopH5Service.getMyTasksPage(reqVO));
            assertEquals(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), ex.getCode());
        }
    }

    @Test
    @DisplayName("my-tasks: 任务列表为空时正确短路返回")
    void testGetMyTasksPage_emptyResult() {
        Long workerUserId = 1003L;
        try (MockedStatic<SecurityFrameworkUtils> securityMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(workerUserId);

            when(machineTaskMapper.selectPage(any(WorkshopMyTasksPageReqVO.class), any(LambdaQueryWrapperX.class)))
                    .thenReturn(PageResult.empty(0L));

            WorkshopMyTasksPageReqVO reqVO = new WorkshopMyTasksPageReqVO();
            PageResult<WorkshopMyTaskRespVO> result = workshopH5Service.getMyTasksPage(reqVO);

            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertTrue(result.getList().isEmpty());
            verifyNoInteractions(weavingOrderMapper);
            verifyNoInteractions(weavingTicketMapper);
        }
    }

}
