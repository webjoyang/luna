package com.luna.lunasrv.module.lunaerp.service.production.yarnorder;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.luna.lunasrv.module.lunaerp.service.production.stockledger.StockLedgerService;
import com.luna.lunasrv.module.lunaerp.service.production.yarnrecipebom.YarnRecipeBomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link YarnOrderServiceImpl} 的单元测试。
 */
class YarnOrderServiceImplTest {

    @Mock
    private YarnOrderMapper yarnOrderMapper;

    @Mock
    private YarnOrderItemMapper yarnOrderItemMapper;

    @Mock
    private GreigeOrderMapper greigeOrderMapper;

    @Mock
    private YarnRecipeBomService yarnRecipeBomService;

    @Mock
    private StockLedgerService stockLedgerService;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private YarnOrderServiceImpl yarnOrderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(yarnOrderMapper.insert(any(YarnOrderDO.class))).thenAnswer(invocation -> {
            YarnOrderDO order = invocation.getArgument(0);
            order.setId(100L);
            return 1;
        });
    }

    // ==================== calculate 测算测试 ====================

    @Test
    void calculate_emptyGreigeOrderIds_throws1030009004() {
        YarnOrderCalculateReqVO req = new YarnOrderCalculateReqVO();
        req.setGreigeOrderIds(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> yarnOrderService.calculateYarnOrder(req));
        assertEquals(1_030_009_004, ex.getCode());
    }

    @Test
    void calculate_greigeOrderNotExists_throws1030006000() {
        YarnOrderCalculateReqVO req = new YarnOrderCalculateReqVO();
        req.setGreigeOrderIds(Collections.singletonList(999L));

        when(greigeOrderMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.calculateYarnOrder(req));
        assertEquals(1_030_006_000, ex.getCode());
    }

    @Test
    void calculate_greigeOrderWithoutProductId_throws1030009003() {
        YarnOrderCalculateReqVO req = new YarnOrderCalculateReqVO();
        req.setGreigeOrderIds(Collections.singletonList(101L));

        GreigeOrderDO greigeOrder = new GreigeOrderDO();
        greigeOrder.setId(101L);
        greigeOrder.setProductId(null); // 无 product_id
        when(greigeOrderMapper.selectById(101L)).thenReturn(greigeOrder);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.calculateYarnOrder(req));
        assertEquals(1_030_009_003, ex.getCode());
    }

    @Test
    void calculate_bomNotExists_throws1030008004() {
        YarnOrderCalculateReqVO req = new YarnOrderCalculateReqVO();
        req.setGreigeOrderIds(Collections.singletonList(101L));

        GreigeOrderDO greigeOrder = new GreigeOrderDO();
        greigeOrder.setId(101L);
        greigeOrder.setProductId(1L);
        when(greigeOrderMapper.selectById(101L)).thenReturn(greigeOrder);

        when(yarnRecipeBomService.getYarnRecipeBomByProductId(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.calculateYarnOrder(req));
        assertEquals(1_030_008_004, ex.getCode());
    }

    @Test
    void calculate_success_multiOrdersMultiBomsAggregation() {
        YarnOrderCalculateReqVO req = new YarnOrderCalculateReqVO();
        req.setGreigeOrderIds(Arrays.asList(101L, 102L));
        req.setLossRate(new BigDecimal("5.00")); // 5% 损耗

        // 胚布单 1
        GreigeOrderDO order1 = new GreigeOrderDO();
        order1.setId(101L);
        order1.setOrderNo("PB0920001");
        order1.setProductId(1L);
        order1.setProductCode("P001");
        order1.setProductName("全棉汗布");
        order1.setFinishedQty(new BigDecimal("1000.00"));
        order1.setSuggestedLoss(new BigDecimal("0.05"));
        when(greigeOrderMapper.selectById(101L)).thenReturn(order1);

        // 胚布单 2
        GreigeOrderDO order2 = new GreigeOrderDO();
        order2.setId(102L);
        order2.setOrderNo("PB0920002");
        order2.setProductId(2L);
        order2.setProductCode("P002");
        order2.setProductName("棉氨汗布");
        order2.setFinishedQty(new BigDecimal("2000.00"));
        order2.setSuggestedLoss(new BigDecimal("0.05"));
        when(greigeOrderMapper.selectById(102L)).thenReturn(order2);

        // 配比 BOM 1 (产品 1): 32S 棉 (201) 60%, 40S 涤 (202) 40%
        YarnRecipeBomRespVO bom1 = new YarnRecipeBomRespVO();
        bom1.setProductId(1L);
        YarnRecipeBomItemRespVO b1Item1 = new YarnRecipeBomItemRespVO();
        b1Item1.setYarnTypeId(1L);
        b1Item1.setYarnSpecId(201L);
        b1Item1.setRatio(new BigDecimal("60.00"));
        YarnRecipeBomItemRespVO b1Item2 = new YarnRecipeBomItemRespVO();
        b1Item2.setYarnTypeId(2L);
        b1Item2.setYarnSpecId(202L);
        b1Item2.setRatio(new BigDecimal("40.00"));
        bom1.setItems(Arrays.asList(b1Item1, b1Item2));
        when(yarnRecipeBomService.getYarnRecipeBomByProductId(1L)).thenReturn(bom1);

        // 配比 BOM 2 (产品 2): 32S 棉 (201) 50%, 氨纶 40D (203) 50%
        YarnRecipeBomRespVO bom2 = new YarnRecipeBomRespVO();
        bom2.setProductId(2L);
        YarnRecipeBomItemRespVO b2Item1 = new YarnRecipeBomItemRespVO();
        b2Item1.setYarnTypeId(1L);
        b2Item1.setYarnSpecId(201L);
        b2Item1.setRatio(new BigDecimal("50.00"));
        YarnRecipeBomItemRespVO b2Item2 = new YarnRecipeBomItemRespVO();
        b2Item2.setYarnTypeId(3L);
        b2Item2.setYarnSpecId(203L);
        b2Item2.setRatio(new BigDecimal("50.00"));
        bom2.setItems(Arrays.asList(b2Item1, b2Item2));
        when(yarnRecipeBomService.getYarnRecipeBomByProductId(2L)).thenReturn(bom2);

        // 库存在库量
        Map<Long, BigDecimal> balances = new HashMap<>();
        balances.put(201L, new BigDecimal("500.00"));
        balances.put(202L, new BigDecimal("100.00"));
        balances.put(203L, BigDecimal.ZERO);
        when(stockLedgerService.getBalances(any())).thenReturn(balances);

        // 历史价: 201 有最近已下达价格 28.50; 202/203 无最近价格，走规格 refPrice 兜底
        when(yarnOrderItemMapper.selectRecentUnitPriceBySpecId(201L)).thenReturn(new BigDecimal("28.5000"));
        when(yarnOrderItemMapper.selectRecentUnitPriceBySpecId(202L)).thenReturn(null);
        when(yarnOrderItemMapper.selectRecentUnitPriceBySpecId(203L)).thenReturn(null);

        // 规格与材质主数据
        YarnSpecDO spec201 = new YarnSpecDO();
        spec201.setId(201L);
        spec201.setYarnTypeId(1L);
        spec201.setSpecName("32S 棉");
        spec201.setRefPrice(new BigDecimal("30.0000"));
        when(yarnSpecMapper.selectById(201L)).thenReturn(spec201);

        YarnSpecDO spec202 = new YarnSpecDO();
        spec202.setId(202L);
        spec202.setYarnTypeId(2L);
        spec202.setSpecName("40S 涤");
        spec202.setRefPrice(new BigDecimal("18.0000"));
        when(yarnSpecMapper.selectById(202L)).thenReturn(spec202);

        YarnSpecDO spec203 = new YarnSpecDO();
        spec203.setId(203L);
        spec203.setYarnTypeId(3L);
        spec203.setSpecName("氨纶 40D");
        spec203.setRefPrice(new BigDecimal("45.0000"));
        when(yarnSpecMapper.selectById(203L)).thenReturn(spec203);

        YarnTypeDO type1 = new YarnTypeDO();
        type1.setId(1L);
        type1.setName("棉");
        when(yarnTypeMapper.selectById(1L)).thenReturn(type1);

        YarnTypeDO type2 = new YarnTypeDO();
        type2.setId(2L);
        type2.setName("涤");
        when(yarnTypeMapper.selectById(2L)).thenReturn(type2);

        YarnTypeDO type3 = new YarnTypeDO();
        type3.setId(3L);
        type3.setName("氨纶");
        when(yarnTypeMapper.selectById(3L)).thenReturn(type3);

        // 执行测算
        YarnOrderCalculateRespVO resp = yarnOrderService.calculateYarnOrder(req);
        assertNotNull(resp);
        assertEquals(2, resp.getSourceGreigeOrders().size());
        assertEquals(3, resp.getItems().size());

        // 查找 201 规格行：
        // baseQty = 1000 * 60% + 2000 * 50% = 600 + 1000 = 1600.00
        // requiredQty = 1600 * (1 + 5/100) = 1680.00
        // stockQty = 500.00
        // purchaseQty = max(0, 1680 - 500) = 1180.00
        // refUnitPrice = 28.5000 (最近采购单价格)
        // sourceGreigeOrderIds = [101, 102], sourceGreigeOrderNos = ["PB0920001", "PB0920002"]
        YarnOrderCalculateItemRespVO item201 = resp.getItems().stream()
                .filter(i -> i.getYarnSpecId().equals(201L)).findFirst().orElse(null);
        assertNotNull(item201);
        assertEquals(new BigDecimal("1600.00"), item201.getBaseQty());
        assertEquals(new BigDecimal("1680.00"), item201.getRequiredQty());
        assertEquals(new BigDecimal("500.00"), item201.getStockQty());
        assertEquals(new BigDecimal("1180.00"), item201.getPurchaseQty());
        assertEquals(new BigDecimal("28.5000"), item201.getRefUnitPrice());
        assertEquals(Arrays.asList(101L, 102L), item201.getSourceGreigeOrderIds());
        assertEquals(Arrays.asList("PB0920001", "PB0920002"), item201.getSourceGreigeOrderNos());

        // 查找 202 规格行：
        // baseQty = 1000 * 40% = 400.00
        // requiredQty = 400 * 1.05 = 420.00
        // stockQty = 100.00
        // purchaseQty = 320.00
        // refUnitPrice = 18.0000 (规格兜底价)
        // sourceGreigeOrderIds = [101]
        YarnOrderCalculateItemRespVO item202 = resp.getItems().stream()
                .filter(i -> i.getYarnSpecId().equals(202L)).findFirst().orElse(null);
        assertNotNull(item202);
        assertEquals(new BigDecimal("400.00"), item202.getBaseQty());
        assertEquals(new BigDecimal("420.00"), item202.getRequiredQty());
        assertEquals(new BigDecimal("100.00"), item202.getStockQty());
        assertEquals(new BigDecimal("320.00"), item202.getPurchaseQty());
        assertEquals(new BigDecimal("18.0000"), item202.getRefUnitPrice());
        assertEquals(Collections.singletonList(101L), item202.getSourceGreigeOrderIds());

        // 查找 203 规格行：
        // baseQty = 2000 * 50% = 1000.00
        // requiredQty = 1000 * 1.05 = 1050.00
        // stockQty = 0.00
        // purchaseQty = 1050.00
        // refUnitPrice = 45.0000 (规格兜底价)
        // sourceGreigeOrderIds = [102]
        YarnOrderCalculateItemRespVO item203 = resp.getItems().stream()
                .filter(i -> i.getYarnSpecId().equals(203L)).findFirst().orElse(null);
        assertNotNull(item203);
        assertEquals(new BigDecimal("1000.00"), item203.getBaseQty());
        assertEquals(new BigDecimal("1050.00"), item203.getRequiredQty());
        assertEquals(BigDecimal.ZERO.setScale(2), item203.getStockQty());
        assertEquals(new BigDecimal("1050.00"), item203.getPurchaseQty());
        assertEquals(new BigDecimal("45.0000"), item203.getRefUnitPrice());
        assertEquals(Collections.singletonList(102L), item203.getSourceGreigeOrderIds());
    }

    // ==================== create 测试 ====================

    @Test
    void createYarnOrder_defaultOrderNo_and_authoritativeTotalAmount() {
        YarnOrderSaveReqVO req = new YarnOrderSaveReqVO();
        req.setOrderDate(LocalDate.of(2026, 9, 20));

        when(yarnOrderMapper.selectListByOrderNoPrefixIncludingDeleted("YP20260920"))
                .thenReturn(Collections.emptyList());

        YarnOrderItemSaveReqVO item = new YarnOrderItemSaveReqVO();
        item.setYarnTypeId(1L);
        item.setYarnSpecId(101L);
        item.setPurchaseQty(new BigDecimal("100.00"));
        item.setUnitPrice(new BigDecimal("25.0000"));
        item.setTotalAmount(new BigDecimal("9999.99")); // 前端传的错误金额应被覆盖
        req.setItems(Collections.singletonList(item));

        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(101L);
        when(yarnSpecMapper.selectById(101L)).thenReturn(spec);

        Long id = yarnOrderService.createYarnOrder(req);
        assertEquals(100L, id);

        ArgumentCaptor<YarnOrderDO> orderCaptor = ArgumentCaptor.forClass(YarnOrderDO.class);
        verify(yarnOrderMapper).insert(orderCaptor.capture());
        YarnOrderDO savedOrder = orderCaptor.getValue();
        assertEquals("YP20260920001", savedOrder.getOrderNo());
        assertEquals("DRAFT", savedOrder.getStatus()); // 缺省 DRAFT

        ArgumentCaptor<List<YarnOrderItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(yarnOrderItemMapper).insertBatch(itemsCaptor.capture());
        List<YarnOrderItemDO> savedItems = itemsCaptor.getValue();
        assertEquals(1, savedItems.size());
        assertEquals(new BigDecimal("2500.00"), savedItems.get(0).getTotalAmount()); // 权威重算 100 * 25
    }

    @Test
    void createYarnOrder_duplicateOrderNo_throws1030009001() {
        YarnOrderSaveReqVO req = new YarnOrderSaveReqVO();
        req.setOrderNo("YP20260920001");

        YarnOrderDO duplicate = new YarnOrderDO();
        duplicate.setId(50L);
        duplicate.setOrderNo("YP20260920001");
        when(yarnOrderMapper.selectByOrderNoIncludingDeleted("YP20260920001")).thenReturn(duplicate);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.createYarnOrder(req));
        assertEquals(1_030_009_001, ex.getCode());
        verify(yarnOrderMapper, never()).insert(any(YarnOrderDO.class));
    }

    @Test
    void createYarnOrder_sourceInstructionGreigeOrderNotExists_throws1030009003() {
        YarnOrderSaveReqVO req = new YarnOrderSaveReqVO();
        req.setSourceInstructions("[999]");

        when(greigeOrderMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.createYarnOrder(req));
        assertEquals(1_030_009_003, ex.getCode());
    }

    @Test
    void createYarnOrder_specNotExists_throws1030009003() {
        YarnOrderSaveReqVO req = new YarnOrderSaveReqVO();
        YarnOrderItemSaveReqVO item = new YarnOrderItemSaveReqVO();
        item.setYarnTypeId(1L);
        item.setYarnSpecId(999L);
        req.setItems(Collections.singletonList(item));

        when(yarnSpecMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.createYarnOrder(req));
        assertEquals(1_030_009_003, ex.getCode());
    }

    @Test
    void createYarnOrder_activeStatus_directlyActive() {
        YarnOrderSaveReqVO req = new YarnOrderSaveReqVO();
        req.setStatus("ACTIVE");
        req.setOrderDate(LocalDate.of(2026, 9, 20));

        when(yarnOrderMapper.selectListByOrderNoPrefixIncludingDeleted("YP20260920"))
                .thenReturn(Collections.emptyList());

        yarnOrderService.createYarnOrder(req);

        ArgumentCaptor<YarnOrderDO> orderCaptor = ArgumentCaptor.forClass(YarnOrderDO.class);
        verify(yarnOrderMapper).insert(orderCaptor.capture());
        assertEquals("ACTIVE", orderCaptor.getValue().getStatus());
    }

    // ==================== update 测试 ====================

    @Test
    void updateYarnOrder_notDraft_throws1030009002() {
        YarnOrderSaveReqVO req = new YarnOrderSaveReqVO();
        req.setId(100L);

        YarnOrderDO existing = new YarnOrderDO();
        existing.setId(100L);
        existing.setStatus("ACTIVE"); // 已下达
        when(yarnOrderMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.updateYarnOrder(req));
        assertEquals(1_030_009_002, ex.getCode());
        verify(yarnOrderMapper, never()).updateById(any(YarnOrderDO.class));
    }

    @Test
    void updateYarnOrder_notExists_throws1030009000() {
        YarnOrderSaveReqVO req = new YarnOrderSaveReqVO();
        req.setId(999L);

        when(yarnOrderMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.updateYarnOrder(req));
        assertEquals(1_030_009_000, ex.getCode());
    }

    @Test
    void updateYarnOrder_success_replacesItems() {
        YarnOrderSaveReqVO req = new YarnOrderSaveReqVO();
        req.setId(100L);
        req.setOrderNo("YP20260920001");
        req.setRemark("更新备注");

        YarnOrderDO existing = new YarnOrderDO();
        existing.setId(100L);
        existing.setOrderNo("YP20260920001");
        existing.setStatus("DRAFT");
        when(yarnOrderMapper.selectById(100L)).thenReturn(existing);

        YarnOrderItemSaveReqVO item = new YarnOrderItemSaveReqVO();
        item.setYarnTypeId(1L);
        item.setYarnSpecId(101L);
        item.setPurchaseQty(new BigDecimal("50.00"));
        item.setUnitPrice(new BigDecimal("20.0000"));
        req.setItems(Collections.singletonList(item));

        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(101L);
        when(yarnSpecMapper.selectById(101L)).thenReturn(spec);

        yarnOrderService.updateYarnOrder(req);

        verify(yarnOrderMapper).updateById(any(YarnOrderDO.class));
        verify(yarnOrderItemMapper).deleteByOrderId(100L);
        verify(yarnOrderItemMapper).insertBatch(anyList());
    }

    // ==================== delete 测试 ====================

    @Test
    void deleteYarnOrder_notDraft_throws1030009002() {
        YarnOrderDO existing = new YarnOrderDO();
        existing.setId(100L);
        existing.setStatus("ACTIVE"); // 进行中不可删
        when(yarnOrderMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.deleteYarnOrder(100L));
        assertEquals(1_030_009_002, ex.getCode());
        verify(yarnOrderMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteYarnOrder_draft_success() {
        YarnOrderDO existing = new YarnOrderDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(yarnOrderMapper.selectById(100L)).thenReturn(existing);

        yarnOrderService.deleteYarnOrder(100L);

        verify(yarnOrderMapper).deleteById(100L);
        verify(yarnOrderItemMapper).deleteByOrderId(100L);
    }

    // ==================== 状态流转测试 ====================

    @Test
    void effectYarnOrder_draftToActive_success() {
        YarnOrderDO existing = new YarnOrderDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(yarnOrderMapper.selectById(100L)).thenReturn(existing);

        yarnOrderService.effectYarnOrder(100L);

        ArgumentCaptor<YarnOrderDO> captor = ArgumentCaptor.forClass(YarnOrderDO.class);
        verify(yarnOrderMapper).updateById(captor.capture());
        assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    @Test
    void effectYarnOrder_notDraft_throws1030009002() {
        YarnOrderDO existing = new YarnOrderDO();
        existing.setId(100L);
        existing.setStatus("ACTIVE");
        when(yarnOrderMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.effectYarnOrder(100L));
        assertEquals(1_030_009_002, ex.getCode());
    }

    @Test
    void completeYarnOrder_activeToCompleted_success() {
        YarnOrderDO existing = new YarnOrderDO();
        existing.setId(100L);
        existing.setStatus("ACTIVE");
        when(yarnOrderMapper.selectById(100L)).thenReturn(existing);

        yarnOrderService.completeYarnOrder(100L);

        ArgumentCaptor<YarnOrderDO> captor = ArgumentCaptor.forClass(YarnOrderDO.class);
        verify(yarnOrderMapper).updateById(captor.capture());
        assertEquals("COMPLETED", captor.getValue().getStatus());
    }

    @Test
    void completeYarnOrder_notActive_throws1030009002() {
        YarnOrderDO existing = new YarnOrderDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(yarnOrderMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.completeYarnOrder(100L));
        assertEquals(1_030_009_002, ex.getCode());
    }

    @Test
    void cancelYarnOrder_activeToCancelled_success() {
        YarnOrderDO existing = new YarnOrderDO();
        existing.setId(100L);
        existing.setStatus("ACTIVE");
        when(yarnOrderMapper.selectById(100L)).thenReturn(existing);

        yarnOrderService.cancelYarnOrder(100L);

        ArgumentCaptor<YarnOrderDO> captor = ArgumentCaptor.forClass(YarnOrderDO.class);
        verify(yarnOrderMapper).updateById(captor.capture());
        assertEquals("CANCELLED", captor.getValue().getStatus());
    }

    @Test
    void cancelYarnOrder_notActive_throws1030009002() {
        YarnOrderDO existing = new YarnOrderDO();
        existing.setId(100L);
        existing.setStatus("COMPLETED");
        when(yarnOrderMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnOrderService.cancelYarnOrder(100L));
        assertEquals(1_030_009_002, ex.getCode());
    }

}
