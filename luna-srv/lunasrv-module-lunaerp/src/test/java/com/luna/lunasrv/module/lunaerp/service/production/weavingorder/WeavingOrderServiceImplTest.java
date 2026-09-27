package com.luna.lunasrv.module.lunaerp.service.production.weavingorder;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderWeavingProgressVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderSaveRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link WeavingOrderServiceImpl} 的纯 Mockito 单元测试。
 *
 * 覆盖 Issue 03 核心规格：
 * 1. 编号派生（ZZ-{胚布单号去符号}-{2位序号}）、重试与手填查重；
 * 2. 来源胚布单校验（限 SELF 且未取消）；
 * 3. 计划匹数软警告（超量警告但允许保存）；
 * 4. DRAFT 状态修改与删除守卫，级联逻辑删除监听触发；
 * 5. 手动取消终态守卫；
 * 6. 胚布单自织进度提供者聚合。
 *
 * @author luna
 */
class WeavingOrderServiceImplTest {

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @Mock
    private GreigeOrderMapper greigeOrderMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private WeavingOrderCascadeDeleteListener cascadeDeleteListener;

    @Mock
    private com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper weavingTicketMapper;

    @Mock
    private com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper machineTaskMapper;

    @Mock
    private com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.YarnRequisitionMapper yarnRequisitionMapper;

    @Mock
    private com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.item.YarnRequisitionItemMapper yarnRequisitionItemMapper;

    @InjectMocks
    private WeavingOrderServiceImpl weavingOrderService;

    @InjectMocks
    private WeavingOrderWeavingProgressProviderImpl progressProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), WeavingOrderDO.class);
        List<WeavingOrderCascadeDeleteListener> listeners = new ArrayList<>();
        listeners.add(cascadeDeleteListener);
        ReflectionTestUtils.setField(weavingOrderService, "cascadeDeleteListeners", listeners);
        ReflectionTestUtils.setField(progressProvider, "weavingOrderMapper", weavingOrderMapper);
    }

    // ==================== 1. 创建校验：胚布单存在性与自织模式 ====================

    @Test
    void createWeavingOrder_fails_whenGreigeOrderNotExists() {
        WeavingOrderSaveReqVO reqVO = buildBaseSaveReqVO(100L, 50);
        when(greigeOrderMapper.selectById(100L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingOrderService.createWeavingOrder(reqVO));
        assertEquals(WEAVING_ORDER_GREIGE_ORDER_INVALID.getCode(), ex.getCode());
    }

    @Test
    void createWeavingOrder_fails_whenGreigeOrderNotSelf() {
        WeavingOrderSaveReqVO reqVO = buildBaseSaveReqVO(100L, 50);
        GreigeOrderDO greigeOrder = buildGreigeOrder(100L, "PB0919001", "OUTSOURCE", "ACTIVE", 100);
        when(greigeOrderMapper.selectById(100L)).thenReturn(greigeOrder);

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingOrderService.createWeavingOrder(reqVO));
        assertEquals(WEAVING_ORDER_GREIGE_ORDER_INVALID.getCode(), ex.getCode());
    }

    @Test
    void createWeavingOrder_fails_whenGreigeOrderCancelled() {
        WeavingOrderSaveReqVO reqVO = buildBaseSaveReqVO(100L, 50);
        GreigeOrderDO greigeOrder = buildGreigeOrder(100L, "PB0919001", "SELF", "CANCELLED", 100);
        when(greigeOrderMapper.selectById(100L)).thenReturn(greigeOrder);

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingOrderService.createWeavingOrder(reqVO));
        assertEquals(WEAVING_ORDER_GREIGE_ORDER_INVALID.getCode(), ex.getCode());
    }

    @Test
    void createWeavingOrder_fails_whenPlannedQtyInvalid() {
        WeavingOrderSaveReqVO reqVO = buildBaseSaveReqVO(100L, 0);
        GreigeOrderDO greigeOrder = buildGreigeOrder(100L, "PB0919001", "SELF", "ACTIVE", 100);
        when(greigeOrderMapper.selectById(100L)).thenReturn(greigeOrder);

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingOrderService.createWeavingOrder(reqVO));
        assertEquals(WEAVING_ORDER_PLANNED_QTY_INVALID.getCode(), ex.getCode());
    }

    // ==================== 2. 编号派生式生成与撞号查重 ====================

    @Test
    void createWeavingOrder_success_withDerivedCode_andSnapshots() {
        WeavingOrderSaveReqVO reqVO = buildBaseSaveReqVO(100L, 50);
        GreigeOrderDO greigeOrder = buildGreigeOrder(100L, "PB-0919-001", "SELF", "ACTIVE", 100);
        greigeOrder.setContractNo("CH20260001");
        greigeOrder.setProductId(200L);
        greigeOrder.setProductCode("P001");
        greigeOrder.setProductName("全棉汗布");
        greigeOrder.setComposition("100% 棉");
        greigeOrder.setGsm("220");
        greigeOrder.setWidth("165cm");

        when(greigeOrderMapper.selectById(100L)).thenReturn(greigeOrder);
        when(weavingOrderMapper.selectListByCodePrefixIncludingDeleted("ZZ-PB0919001-"))
                .thenReturn(Collections.emptyList());
        when(weavingOrderMapper.selectActiveListByGreigeOrderId(100L))
                .thenReturn(Collections.emptyList());

        WeavingOrderSaveRespVO resp = weavingOrderService.createWeavingOrder(reqVO);

        assertNotNull(resp);
        assertEquals("ZZ-PB0919001-01", resp.getCode());
        assertNull(resp.getWarning());

        ArgumentCaptor<WeavingOrderDO> captor = ArgumentCaptor.forClass(WeavingOrderDO.class);
        verify(weavingOrderMapper).insert(captor.capture());
        WeavingOrderDO inserted = captor.getValue();
        assertEquals("ZZ-PB0919001-01", inserted.getCode());
        assertEquals("DRAFT", inserted.getStatus());
        assertEquals("PB-0919-001", inserted.getGreigeOrderNo());
        assertEquals("CH20260001", inserted.getContractNo());
        assertEquals(200L, inserted.getProductId());
        assertEquals("P001", inserted.getProductCode());
        assertEquals("全棉汗布", inserted.getProductName());
        assertEquals("100% 棉", inserted.getComposition());
        assertEquals("220", inserted.getGsm());
        assertEquals("165cm", inserted.getWidth());
    }

    @Test
    void createWeavingOrder_success_incrementsSequence_whenOrdersExist() {
        WeavingOrderSaveReqVO reqVO = buildBaseSaveReqVO(100L, 30);
        GreigeOrderDO greigeOrder = buildGreigeOrder(100L, "PB0919001", "SELF", "ACTIVE", 100);
        when(greigeOrderMapper.selectById(100L)).thenReturn(greigeOrder);

        WeavingOrderDO existing = new WeavingOrderDO();
        existing.setCode("ZZ-PB0919001-01");
        when(weavingOrderMapper.selectListByCodePrefixIncludingDeleted("ZZ-PB0919001-"))
                .thenReturn(Collections.singletonList(existing));
        when(weavingOrderMapper.selectActiveListByGreigeOrderId(100L))
                .thenReturn(Collections.singletonList(existing));

        WeavingOrderSaveRespVO resp = weavingOrderService.createWeavingOrder(reqVO);

        assertNotNull(resp);
        assertEquals("ZZ-PB0919001-02", resp.getCode());
    }

    @Test
    void createWeavingOrder_fails_whenManualCodeDuplicate() {
        WeavingOrderSaveReqVO reqVO = buildBaseSaveReqVO(100L, 50);
        reqVO.setCode("ZZ-MANUAL-01");
        GreigeOrderDO greigeOrder = buildGreigeOrder(100L, "PB0919001", "SELF", "ACTIVE", 100);
        when(greigeOrderMapper.selectById(100L)).thenReturn(greigeOrder);

        WeavingOrderDO duplicate = new WeavingOrderDO();
        duplicate.setId(999L);
        duplicate.setCode("ZZ-MANUAL-01");
        when(weavingOrderMapper.selectByCodeIncludingDeleted("ZZ-MANUAL-01")).thenReturn(duplicate);

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingOrderService.createWeavingOrder(reqVO));
        assertEquals(WEAVING_ORDER_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    // ==================== 3. 计划匹数软警告（超量警告但允许保存） ====================

    @Test
    void createWeavingOrder_returnsWarning_whenTotalPlannedExceedsGreigePieceCount() {
        WeavingOrderSaveReqVO reqVO = buildBaseSaveReqVO(100L, 60);
        GreigeOrderDO greigeOrder = buildGreigeOrder(100L, "PB0919001", "SELF", "ACTIVE", 100);
        when(greigeOrderMapper.selectById(100L)).thenReturn(greigeOrder);

        // 已有织造单 plannedQty = 50，本次再建 60 -> 总计 110 > 100
        WeavingOrderDO order1 = new WeavingOrderDO();
        order1.setId(1L);
        order1.setPlannedQty(50);
        when(weavingOrderMapper.selectActiveListByGreigeOrderId(100L))
                .thenReturn(Collections.singletonList(order1));
        when(weavingOrderMapper.selectListByCodePrefixIncludingDeleted("ZZ-PB0919001-"))
                .thenReturn(Collections.singletonList(order1));

        WeavingOrderSaveRespVO resp = weavingOrderService.createWeavingOrder(reqVO);

        assertNotNull(resp);
        assertNotNull(resp.getWarning());
        assertTrue(resp.getWarning().contains("超出胚布单总匹数(100匹)"));
        assertTrue(resp.getWarning().contains("110匹"));
        verify(weavingOrderMapper).insert((WeavingOrderDO) any());
    }

    // ==================== 4. 修改操作与状态守卫 ====================

    @Test
    void updateWeavingOrder_success_whenStatusIsDraft() {
        WeavingOrderSaveReqVO updateReqVO = buildBaseSaveReqVO(100L, 80);
        updateReqVO.setId(1L);
        updateReqVO.setRemark("更新备注");

        WeavingOrderDO exist = new WeavingOrderDO();
        exist.setId(1L);
        exist.setStatus("DRAFT");
        exist.setGreigeOrderId(100L);
        exist.setPlannedQty(50);

        GreigeOrderDO greigeOrder = buildGreigeOrder(100L, "PB0919001", "SELF", "ACTIVE", 100);

        when(weavingOrderMapper.selectById(1L)).thenReturn(exist);
        when(greigeOrderMapper.selectById(100L)).thenReturn(greigeOrder);
        when(weavingOrderMapper.selectActiveListByGreigeOrderId(100L)).thenReturn(Collections.singletonList(exist));

        WeavingOrderSaveRespVO resp = weavingOrderService.updateWeavingOrder(updateReqVO);

        assertNotNull(resp);
        assertEquals(1L, resp.getId());
        verify(weavingOrderMapper).updateById(exist);
        assertEquals(80, exist.getPlannedQty());
        assertEquals("更新备注", exist.getRemark());
    }

    @Test
    void updateWeavingOrder_fails_whenStatusIsNotDraft() {
        WeavingOrderSaveReqVO updateReqVO = buildBaseSaveReqVO(100L, 80);
        updateReqVO.setId(1L);

        WeavingOrderDO exist = new WeavingOrderDO();
        exist.setId(1L);
        exist.setStatus("ACTIVE"); // 生产中不允许修改

        when(weavingOrderMapper.selectById(1L)).thenReturn(exist);

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingOrderService.updateWeavingOrder(updateReqVO));
        assertEquals(WEAVING_ORDER_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    // ==================== 5. 删除操作：DRAFT 守卫与级联逻辑删除 ====================

    @Test
    void deleteWeavingOrder_success_whenDraft_andTriggersCascadeListener() {
        WeavingOrderDO exist = new WeavingOrderDO();
        exist.setId(10L);
        exist.setStatus("DRAFT");

        when(weavingOrderMapper.selectById(10L)).thenReturn(exist);

        weavingOrderService.deleteWeavingOrder(10L);

        verify(weavingOrderMapper).deleteById(10L);
        verify(cascadeDeleteListener).onWeavingOrderDeleted(10L);
    }

    @Test
    void deleteWeavingOrder_fails_whenNotDraft() {
        WeavingOrderDO exist = new WeavingOrderDO();
        exist.setId(10L);
        exist.setStatus("ACTIVE");

        when(weavingOrderMapper.selectById(10L)).thenReturn(exist);

        ServiceException ex = assertThrows(ServiceException.class, () -> weavingOrderService.deleteWeavingOrder(10L));
        assertEquals(WEAVING_ORDER_STATUS_NOT_ALLOW.getCode(), ex.getCode());
        verify(weavingOrderMapper, never()).deleteById(any());
        verify(cascadeDeleteListener, never()).onWeavingOrderDeleted(any());
    }

    // ==================== 6. 手动取消 ====================

    @Test
    void cancelWeavingOrder_success_whenDraftOrActive() {
        WeavingOrderDO exist = new WeavingOrderDO();
        exist.setId(15L);
        exist.setStatus("DRAFT");

        when(weavingOrderMapper.selectById(15L)).thenReturn(exist);

        weavingOrderService.cancelWeavingOrder(15L);

        ArgumentCaptor<WeavingOrderDO> captor = ArgumentCaptor.forClass(WeavingOrderDO.class);
        verify(weavingOrderMapper).updateById(captor.capture());
        assertEquals("CANCELLED", captor.getValue().getStatus());
    }

    @Test
    void cancelWeavingOrder_fails_whenAlreadyCancelledOrCompleted() {
        WeavingOrderDO cancelled = new WeavingOrderDO();
        cancelled.setId(15L);
        cancelled.setStatus("CANCELLED");
        when(weavingOrderMapper.selectById(15L)).thenReturn(cancelled);

        ServiceException ex1 = assertThrows(ServiceException.class, () -> weavingOrderService.cancelWeavingOrder(15L));
        assertEquals(WEAVING_ORDER_STATUS_NOT_ALLOW.getCode(), ex1.getCode());

        WeavingOrderDO completed = new WeavingOrderDO();
        completed.setId(16L);
        completed.setStatus("COMPLETED");
        when(weavingOrderMapper.selectById(16L)).thenReturn(completed);

        ServiceException ex2 = assertThrows(ServiceException.class, () -> weavingOrderService.cancelWeavingOrder(16L));
        assertEquals(WEAVING_ORDER_STATUS_NOT_ALLOW.getCode(), ex2.getCode());
    }

    // ==================== 7. 预览下一个编号 ====================

    @Test
    void getNextWeavingOrderCode_success() {
        GreigeOrderDO greigeOrder = buildGreigeOrder(100L, "PB0919001", "SELF", "ACTIVE", 100);
        when(greigeOrderMapper.selectById(100L)).thenReturn(greigeOrder);

        WeavingOrderDO order1 = new WeavingOrderDO();
        order1.setCode("ZZ-PB0919001-01");
        WeavingOrderDO order2 = new WeavingOrderDO();
        order2.setCode("ZZ-PB0919001-02");
        when(weavingOrderMapper.selectListByCodePrefixIncludingDeleted("ZZ-PB0919001-"))
                .thenReturn(List.of(order1, order2));

        String nextCode = weavingOrderService.getNextWeavingOrderCode(100L);
        assertEquals("ZZ-PB0919001-03", nextCode);
    }

    // ==================== 8. 详情与自织进度提供者 ====================

    @Test
    void getWeavingOrder_success() {
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(10L);
        order.setCode("ZZ-PB0919001-01");
        order.setGreigeOrderId(100L);
        order.setStatus("COMPLETED");
        order.setPlannedQty(80);

        GreigeOrderDO greigeOrder = buildGreigeOrder(100L, "PB0919001", "SELF", "ACTIVE", 100);

        when(weavingOrderMapper.selectById(10L)).thenReturn(order);
        when(greigeOrderMapper.selectById(100L)).thenReturn(greigeOrder);
        when(weavingOrderMapper.selectActiveListByGreigeOrderId(100L)).thenReturn(Collections.singletonList(order));

        WeavingOrderRespVO resp = weavingOrderService.getWeavingOrder(10L);
        assertNotNull(resp);
        assertEquals(10L, resp.getId());
        assertEquals(80, resp.getCompletedQty());
    }

    @Test
    void progressProvider_aggregatesPlannedAndCompletedPieces() {
        WeavingOrderDO o1 = new WeavingOrderDO();
        o1.setId(1L);
        o1.setPlannedQty(40);
        o1.setStatus("ACTIVE");

        WeavingOrderDO o2 = new WeavingOrderDO();
        o2.setId(2L);
        o2.setPlannedQty(30);
        o2.setStatus("COMPLETED");

        when(weavingOrderMapper.selectActiveListByGreigeOrderId(100L)).thenReturn(List.of(o1, o2));

        GreigeOrderWeavingProgressVO progress = progressProvider.getWeavingProgress(100L);
        assertNotNull(progress);
        assertEquals(70, progress.getPlannedPieces());
        assertEquals(30, progress.getCompletedPieces());
    }

    @Test
    void testGetWeavingOrderStats() {
        Long orderId = 1024L;
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(orderId);
        order.setCode("ZZ-PB0919001-01");
        order.setPlannedQty(100);
        order.setPlannedWeight(new BigDecimal("2500.00"));
        order.setStatus("ACTIVE");

        when(weavingOrderMapper.selectById(orderId)).thenReturn(order);

        com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO t1 =
                new com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO();
        t1.setId(1L);
        t1.setStatus("COMPLETED");

        com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO t2 =
                new com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO();
        t2.setId(2L);
        t2.setStatus("STARTED");

        com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO t3 =
                new com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO();
        t3.setId(3L);
        t3.setStatus("VOID");

        when(weavingTicketMapper.selectListByWeavingOrderId(orderId)).thenReturn(List.of(t1, t2, t3));

        com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO task =
                new com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO();
        task.setId(10L);
        task.setCode("MT-ZZPB091900101-01");
        task.setLoomId(1L);
        task.setLoomNo("L001");
        task.setWorkerName("张三");
        task.setPlannedQty(50);
        task.setActualQty(25);
        task.setStatus("IN_PROGRESS");

        when(machineTaskMapper.selectListByWeavingOrderId(orderId)).thenReturn(List.of(task));

        com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.YarnRequisitionDO req =
                new com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.YarnRequisitionDO();
        req.setId(100L);
        when(yarnRequisitionMapper.selectListByWeavingOrderId(orderId)).thenReturn(List.of(req));

        com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.item.YarnRequisitionItemDO item =
                new com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.item.YarnRequisitionItemDO();
        item.setId(200L);
        item.setYarnSpecId(1L);
        item.setYarnSpecName("32S 棉");
        item.setRequiredQty(new BigDecimal("100.00"));
        item.setDispatchedQty(new BigDecimal("80.00"));
        item.setUnit("KG");
        when(yarnRequisitionItemMapper.selectListByRequisitionIds(any())).thenReturn(List.of(item));

        com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderStatsRespVO stats =
                weavingOrderService.getWeavingOrderStats(orderId);

        assertNotNull(stats);
        assertEquals(orderId, stats.getWeavingOrderId());
        assertEquals(100, stats.getPlannedQty());
        assertEquals(3, stats.getTotalTickets());
        assertEquals(2, stats.getValidTickets());
        assertEquals(1, stats.getVoidTickets());
        assertEquals(1, stats.getCompletedTickets());
        assertEquals(1, stats.getInProgressTickets());
        assertEquals(1.0, stats.getCompletionRate()); // 1 / 100 * 100 = 1.0%
        assertNotNull(stats.getTaskStats());
        assertEquals(1, stats.getTaskStats().size());
        assertEquals(50.0, stats.getTaskStats().get(0).getTaskCompletionRate());
        assertEquals(new BigDecimal("80.00"), stats.getTotalYarnDispatchedWeight());
        assertNotNull(stats.getYarnStats());
        assertEquals(1, stats.getYarnStats().size());
        assertEquals(80.0, stats.getYarnStats().get(0).getDispatchRate());
    }

    // ==================== 私有辅助方法 ====================

    private WeavingOrderSaveReqVO buildBaseSaveReqVO(Long greigeOrderId, Integer plannedQty) {
        WeavingOrderSaveReqVO vo = new WeavingOrderSaveReqVO();
        vo.setGreigeOrderId(greigeOrderId);
        vo.setPlannedQty(plannedQty);
        vo.setPlannedWeight(new BigDecimal("1250.00"));
        return vo;
    }

    private GreigeOrderDO buildGreigeOrder(Long id, String orderNo, String productionMode, String status, Integer pieceCount) {
        GreigeOrderDO order = new GreigeOrderDO();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setProductionMode(productionMode);
        order.setStatus(status);
        order.setPieceCount(pieceCount);
        order.setDeleted(false);
        return order;
    }

}
