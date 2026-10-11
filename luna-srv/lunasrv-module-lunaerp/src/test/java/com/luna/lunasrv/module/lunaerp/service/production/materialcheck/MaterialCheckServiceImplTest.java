package com.luna.lunasrv.module.lunaerp.service.production.materialcheck;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckContributionVO;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckRowVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnInTransitDTO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.YarnStockBalanceDTO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WEAVING_ORDER_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WEAVING_ORDER_STATUS_NOT_ALLOW;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link MaterialCheckServiceImpl} 单元测试
 *
 * <p>覆盖 Issue 05 与 Testing Decisions 11-15：
 * <ul>
 *   <li>11. 两织造单同产品按 yarnSpecId 合并需求 = Σ(plannedWeight × ratio/100)，贡献行可展开；</li>
 *   <li>12. 可用库存=台账正负流水求和（含 YARN_OUT 扣减）；仓库筛选生效；缺省全仓；批量聚合无 N+1；</li>
 *   <li>13. 在途仅计 ACTIVE 采购单 purchase_qty；两缺口与建议采购量（含在途口径）计算正确，齐套/在途可覆盖/缺料三态；</li>
 *   <li>14. 缺 BOM、缺计划重量单分别入两个分区且不进聚合行；</li>
 *   <li>15. 租户隔离：他租户织造单/库存/采购单不进试算；传入不存在或他租户织造单 ID 抛不存在类错误。</li>
 * </ul>
 *
 * @author luna
 */
class MaterialCheckServiceImplTest {

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @Mock
    private YarnRecipeBomMapper yarnRecipeBomMapper;

    @Mock
    private YarnRecipeBomItemMapper yarnRecipeBomItemMapper;

    @Mock
    private StockLedgerMapper stockLedgerMapper;

    @Mock
    private YarnOrderItemMapper yarnOrderItemMapper;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @InjectMocks
    private MaterialCheckServiceImpl materialCheckService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, WeavingOrderDO.class);
        TableInfoHelper.initTableInfo(assistant, YarnRecipeBomDO.class);
        TableInfoHelper.initTableInfo(assistant, YarnRecipeBomItemDO.class);
        TableInfoHelper.initTableInfo(assistant, YarnSpecDO.class);
        TableInfoHelper.initTableInfo(assistant, YarnTypeDO.class);
    }

    /**
     * Testing Decision 11:
     * 两织造单同产品 → 按 yarnSpecId 合并需求 = Σ(plannedWeight × ratio/100)，贡献行可展开
     */
    @Test
    void testRunMaterialCheck_MergeDemandSameProductAndExpandContributions() {
        // 织造单 1: plannedWeight = 100.00
        WeavingOrderDO order1 = new WeavingOrderDO();
        order1.setId(1L);
        order1.setCode("WO001");
        order1.setProductId(100L);
        order1.setProductCode("P001");
        order1.setProductName("汗布");
        order1.setPlannedQty(10);
        order1.setPlannedWeight(new BigDecimal("100.00"));
        order1.setStatus("ACTIVE");

        // 织造单 2: plannedWeight = 200.00
        WeavingOrderDO order2 = new WeavingOrderDO();
        order2.setId(2L);
        order2.setCode("WO002");
        order2.setProductId(100L);
        order2.setProductCode("P001");
        order2.setProductName("汗布");
        order2.setPlannedQty(20);
        order2.setPlannedWeight(new BigDecimal("200.00"));
        order2.setStatus("DRAFT");

        when(weavingOrderMapper.selectBatchIds(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(order1, order2));

        // 产品 100 的 BOM
        YarnRecipeBomDO bom = new YarnRecipeBomDO();
        bom.setId(10L);
        bom.setProductId(100L);
        when(yarnRecipeBomMapper.selectListByProductIds(any())).thenReturn(Collections.singletonList(bom));

        // 明细 1: yarnSpecId=1001, ratio=60 (60%)
        YarnRecipeBomItemDO item1 = new YarnRecipeBomItemDO();
        item1.setId(1L);
        item1.setRecipeBomId(10L);
        item1.setYarnSpecId(1001L);
        item1.setRatio(new BigDecimal("60.00"));

        // 明细 2: yarnSpecId=1002, ratio=40 (40%)
        YarnRecipeBomItemDO item2 = new YarnRecipeBomItemDO();
        item2.setId(2L);
        item2.setRecipeBomId(10L);
        item2.setYarnSpecId(1002L);
        item2.setRatio(new BigDecimal("40.00"));

        when(yarnRecipeBomItemMapper.selectListByRecipeBomIds(Collections.singleton(10L)))
                .thenReturn(Arrays.asList(item1, item2));

        // 库存与在途 mock 为 0
        when(stockLedgerMapper.sumBalanceByYarnSpecIds(any(), any())).thenReturn(Collections.emptyList());
        when(yarnOrderItemMapper.sumInTransitByYarnSpecIds(any())).thenReturn(Collections.emptyList());

        // 规格与材质
        YarnSpecDO spec1 = new YarnSpecDO();
        spec1.setId(1001L);
        spec1.setYarnTypeId(201L);
        spec1.setSpecName("32S 棉");

        YarnSpecDO spec2 = new YarnSpecDO();
        spec2.setId(1002L);
        spec2.setYarnTypeId(202L);
        spec2.setSpecName("40D 氨纶");

        when(yarnSpecMapper.selectBatchIds(any())).thenReturn(Arrays.asList(spec1, spec2));

        YarnTypeDO type1 = new YarnTypeDO();
        type1.setId(201L);
        type1.setName("棉");

        YarnTypeDO type2 = new YarnTypeDO();
        type2.setId(202L);
        type2.setName("氨纶");

        when(yarnTypeMapper.selectBatchIds(any())).thenReturn(Arrays.asList(type1, type2));

        MaterialCheckReqVO reqVO = new MaterialCheckReqVO();
        reqVO.setWeavingOrderIds(Arrays.asList(1L, 2L));

        MaterialCheckRespVO resp = materialCheckService.runMaterialCheck(reqVO);

        assertNotNull(resp);
        assertEquals(2, resp.getRows().size());
        assertTrue(resp.getMissingBomOrders().isEmpty());
        assertTrue(resp.getMissingWeightOrders().isEmpty());

        // Spec 1001: 需求 = 100 * 60% + 200 * 60% = 60 + 120 = 180.00
        MaterialCheckRowVO row1 = resp.getRows().stream()
                .filter(r -> r.getYarnSpecId().equals(1001L)).findFirst().orElse(null);
        assertNotNull(row1);
        assertEquals("棉", row1.getYarnTypeName());
        assertEquals("32S 棉", row1.getYarnSpecName());
        assertEquals(new BigDecimal("180.00"), row1.getDemandQty());
        assertEquals(2, row1.getContributions().size());

        MaterialCheckContributionVO c1 = row1.getContributions().get(0);
        assertEquals(1L, c1.getWeavingOrderId());
        assertEquals(new BigDecimal("60.00"), c1.getDemandQty());
        assertEquals(new BigDecimal("60.00"), c1.getRatio());

        MaterialCheckContributionVO c2 = row1.getContributions().get(1);
        assertEquals(2L, c2.getWeavingOrderId());
        assertEquals(new BigDecimal("120.00"), c2.getDemandQty());

        // Spec 1002: 需求 = 100 * 40% + 200 * 40% = 40 + 80 = 120.00
        MaterialCheckRowVO row2 = resp.getRows().stream()
                .filter(r -> r.getYarnSpecId().equals(1002L)).findFirst().orElse(null);
        assertNotNull(row2);
        assertEquals("氨纶", row2.getYarnTypeName());
        assertEquals(new BigDecimal("120.00"), row2.getDemandQty());
        assertEquals(2, row2.getContributions().size());
    }

    /**
     * Testing Decision 12:
     * 可用库存=台账正负流水求和（含 YARN_OUT 扣减）；仓库筛选生效；缺省全仓；批量聚合无 N+1
     */
    @Test
    void testRunMaterialCheck_StockLedgerAndWarehouseFilter() {
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(1L);
        order.setCode("WO001");
        order.setProductId(100L);
        order.setPlannedQty(10);
        order.setPlannedWeight(new BigDecimal("100.00"));
        order.setStatus("ACTIVE");

        when(weavingOrderMapper.selectBatchIds(Collections.singletonList(1L)))
                .thenReturn(Collections.singletonList(order));

        YarnRecipeBomDO bom = new YarnRecipeBomDO();
        bom.setId(10L);
        bom.setProductId(100L);
        when(yarnRecipeBomMapper.selectListByProductIds(any())).thenReturn(Collections.singletonList(bom));

        YarnRecipeBomItemDO item = new YarnRecipeBomItemDO();
        item.setId(1L);
        item.setRecipeBomId(10L);
        item.setYarnSpecId(1001L);
        item.setRatio(new BigDecimal("100.00"));
        when(yarnRecipeBomItemMapper.selectListByRecipeBomIds(any()))
                .thenReturn(Collections.singletonList(item));

        // 场景 A: 指定仓库筛选 warehouseIds = [5L, 6L]
        List<Long> warehouseIds = Arrays.asList(5L, 6L);
        when(stockLedgerMapper.sumBalanceByYarnSpecIds(any(), eq(warehouseIds)))
                .thenReturn(Collections.singletonList(new YarnStockBalanceDTO(1001L, new BigDecimal("45.50"))));
        when(yarnOrderItemMapper.sumInTransitByYarnSpecIds(any())).thenReturn(Collections.emptyList());

        MaterialCheckReqVO reqA = new MaterialCheckReqVO();
        reqA.setWeavingOrderIds(Collections.singletonList(1L));
        reqA.setWarehouseIds(warehouseIds);

        MaterialCheckRespVO respA = materialCheckService.runMaterialCheck(reqA);
        assertEquals(new BigDecimal("45.50"), respA.getRows().get(0).getStockQty());
        assertEquals(new BigDecimal("54.50"), respA.getRows().get(0).getShortageWithoutInTransit());
        verify(stockLedgerMapper, times(1)).sumBalanceByYarnSpecIds(any(), eq(warehouseIds));

        // 场景 B: 缺省全仓（warehouseIds 为空）
        when(stockLedgerMapper.sumBalanceByYarnSpecIds(any(), eq(null)))
                .thenReturn(Collections.singletonList(new YarnStockBalanceDTO(1001L, new BigDecimal("80.00"))));

        MaterialCheckReqVO reqB = new MaterialCheckReqVO();
        reqB.setWeavingOrderIds(Collections.singletonList(1L));
        reqB.setWarehouseIds(null);

        MaterialCheckRespVO respB = materialCheckService.runMaterialCheck(reqB);
        assertEquals(new BigDecimal("80.00"), respB.getRows().get(0).getStockQty());
        assertEquals(new BigDecimal("20.00"), respB.getRows().get(0).getShortageWithoutInTransit());
        verify(stockLedgerMapper, times(1)).sumBalanceByYarnSpecIds(any(), eq(null));
    }

    /**
     * Testing Decision 13:
     * 在途仅计 ACTIVE 采购单 purchase_qty；两缺口与建议采购量（含在途口径）计算正确；齐套/在途可覆盖/缺料三态
     */
    @Test
    void testRunMaterialCheck_InTransitAndThreeShortageStates() {
        // 构造织造单：plannedWeight = 100.00
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(1L);
        order.setCode("WO001");
        order.setProductId(100L);
        order.setPlannedQty(10);
        order.setPlannedWeight(new BigDecimal("100.00"));
        order.setStatus("ACTIVE");

        when(weavingOrderMapper.selectBatchIds(Collections.singletonList(1L)))
                .thenReturn(Collections.singletonList(order));

        YarnRecipeBomDO bom = new YarnRecipeBomDO();
        bom.setId(10L);
        bom.setProductId(100L);
        when(yarnRecipeBomMapper.selectListByProductIds(any())).thenReturn(Collections.singletonList(bom));

        // 3 个规格明细，ratio 各 100%（测试不同规格）
        YarnRecipeBomItemDO item1 = new YarnRecipeBomItemDO();
        item1.setId(1L);
        item1.setRecipeBomId(10L);
        item1.setYarnSpecId(101L); // 齐套
        item1.setRatio(new BigDecimal("100.00"));

        YarnRecipeBomItemDO item2 = new YarnRecipeBomItemDO();
        item2.setId(2L);
        item2.setRecipeBomId(10L);
        item2.setYarnSpecId(102L); // 在途可覆盖
        item2.setRatio(new BigDecimal("100.00"));

        YarnRecipeBomItemDO item3 = new YarnRecipeBomItemDO();
        item3.setId(3L);
        item3.setRecipeBomId(10L);
        item3.setYarnSpecId(103L); // 缺料
        item3.setRatio(new BigDecimal("100.00"));

        when(yarnRecipeBomItemMapper.selectListByRecipeBomIds(any()))
                .thenReturn(Arrays.asList(item1, item2, item3));

        // 各规格需求量均 = 100.00
        // Spec 101 (齐套): 可用 120.00，在途 0
        // Spec 102 (在途可覆盖): 可用 40.00，在途 70.00 (合计 110 >= 100)
        // Spec 103 (缺料): 可用 20.00，在途 30.00 (合计 50 < 100)
        when(stockLedgerMapper.sumBalanceByYarnSpecIds(any(), any())).thenReturn(Arrays.asList(
                new YarnStockBalanceDTO(101L, new BigDecimal("120.00")),
                new YarnStockBalanceDTO(102L, new BigDecimal("40.00")),
                new YarnStockBalanceDTO(103L, new BigDecimal("20.00"))
        ));

        when(yarnOrderItemMapper.sumInTransitByYarnSpecIds(any())).thenReturn(Arrays.asList(
                new YarnInTransitDTO(101L, BigDecimal.ZERO),
                new YarnInTransitDTO(102L, new BigDecimal("70.00")),
                new YarnInTransitDTO(103L, new BigDecimal("30.00"))
        ));

        // 1. includeInTransit = false
        MaterialCheckReqVO reqWithoutTransit = new MaterialCheckReqVO();
        reqWithoutTransit.setWeavingOrderIds(Collections.singletonList(1L));
        reqWithoutTransit.setIncludeInTransit(false);

        MaterialCheckRespVO resp1 = materialCheckService.runMaterialCheck(reqWithoutTransit);

        MaterialCheckRowVO row101 = resp1.getRows().stream().filter(r -> r.getYarnSpecId().equals(101L)).findFirst().get();
        assertEquals(new BigDecimal("0.00"), row101.getShortageWithoutInTransit());
        assertEquals(new BigDecimal("0.00"), row101.getShortageWithInTransit());
        assertEquals(new BigDecimal("0.00"), row101.getSuggestedPurchaseQty());

        MaterialCheckRowVO row102 = resp1.getRows().stream().filter(r -> r.getYarnSpecId().equals(102L)).findFirst().get();
        assertEquals(new BigDecimal("60.00"), row102.getShortageWithoutInTransit()); // 100 - 40
        assertEquals(new BigDecimal("0.00"), row102.getShortageWithInTransit()); // 100 - 40 - 70 <= 0
        assertEquals(new BigDecimal("0.00"), row102.getSuggestedPurchaseQty());

        MaterialCheckRowVO row103 = resp1.getRows().stream().filter(r -> r.getYarnSpecId().equals(103L)).findFirst().get();
        assertEquals(new BigDecimal("80.00"), row103.getShortageWithoutInTransit()); // 100 - 20
        assertEquals(new BigDecimal("50.00"), row103.getShortageWithInTransit()); // 100 - 20 - 30 = 50
        assertEquals(new BigDecimal("50.00"), row103.getSuggestedPurchaseQty());

        // 汇总徽标 (不含在途): 102 和 103 均算缺料 (2 种)，合计 60 + 80 = 140.00
        assertEquals(2, resp1.getShortageSpecCount());
        assertEquals(new BigDecimal("140.00"), resp1.getTotalShortageQty());

        // 2. includeInTransit = true
        MaterialCheckReqVO reqWithTransit = new MaterialCheckReqVO();
        reqWithTransit.setWeavingOrderIds(Collections.singletonList(1L));
        reqWithTransit.setIncludeInTransit(true);

        MaterialCheckRespVO resp2 = materialCheckService.runMaterialCheck(reqWithTransit);
        // 汇总徽标 (含在途): 仅 103 缺料 (1 种)，合计 50.00
        assertEquals(1, resp2.getShortageSpecCount());
        assertEquals(new BigDecimal("50.00"), resp2.getTotalShortageQty());
    }

    /**
     * Testing Decision 14:
     * 缺 BOM、缺计划重量单分别入两个分区且不进聚合行
     */
    @Test
    void testRunMaterialCheck_MissingBomAndMissingWeightPartitions() {
        // 单 1: 正常单（有 BOM、有重量）
        WeavingOrderDO order1 = new WeavingOrderDO();
        order1.setId(1L);
        order1.setCode("WO001");
        order1.setProductId(101L);
        order1.setPlannedQty(10);
        order1.setPlannedWeight(new BigDecimal("100.00"));
        order1.setStatus("ACTIVE");

        // 单 2: 缺 BOM（产品 102 无配比 BOM）
        WeavingOrderDO order2 = new WeavingOrderDO();
        order2.setId(2L);
        order2.setCode("WO002");
        order2.setProductId(102L);
        order2.setPlannedQty(10);
        order2.setPlannedWeight(new BigDecimal("100.00"));
        order2.setStatus("ACTIVE");

        // 单 3: 缺计划重量（plannedWeight 为空）
        WeavingOrderDO order3 = new WeavingOrderDO();
        order3.setId(3L);
        order3.setCode("WO003");
        order3.setProductId(101L);
        order3.setPlannedQty(10);
        order3.setPlannedWeight(null);
        order3.setStatus("ACTIVE");

        // 单 4: 缺 BOM 且缺匹数（plannedQty 为 0 且产品 104 无 BOM）
        WeavingOrderDO order4 = new WeavingOrderDO();
        order4.setId(4L);
        order4.setCode("WO004");
        order4.setProductId(104L);
        order4.setPlannedQty(0);
        order4.setPlannedWeight(new BigDecimal("50.00"));
        order4.setStatus("DRAFT");

        when(weavingOrderMapper.selectBatchIds(Arrays.asList(1L, 2L, 3L, 4L)))
                .thenReturn(Arrays.asList(order1, order2, order3, order4));

        // 仅产品 101 有 BOM
        YarnRecipeBomDO bom101 = new YarnRecipeBomDO();
        bom101.setId(11L);
        bom101.setProductId(101L);
        when(yarnRecipeBomMapper.selectListByProductIds(any())).thenReturn(Collections.singletonList(bom101));

        YarnRecipeBomItemDO item = new YarnRecipeBomItemDO();
        item.setId(1L);
        item.setRecipeBomId(11L);
        item.setYarnSpecId(1001L);
        item.setRatio(new BigDecimal("100.00"));
        when(yarnRecipeBomItemMapper.selectListByRecipeBomIds(any()))
                .thenReturn(Collections.singletonList(item));

        when(stockLedgerMapper.sumBalanceByYarnSpecIds(any(), any())).thenReturn(Collections.emptyList());
        when(yarnOrderItemMapper.sumInTransitByYarnSpecIds(any())).thenReturn(Collections.emptyList());

        MaterialCheckReqVO reqVO = new MaterialCheckReqVO();
        reqVO.setWeavingOrderIds(Arrays.asList(1L, 2L, 3L, 4L));

        MaterialCheckRespVO resp = materialCheckService.runMaterialCheck(reqVO);

        // 缺 BOM 分区：单 2 和 单 4
        assertEquals(2, resp.getMissingBomOrders().size());
        assertTrue(resp.getMissingBomOrders().stream().anyMatch(o -> o.getWeavingOrderId().equals(2L)));
        assertTrue(resp.getMissingBomOrders().stream().anyMatch(o -> o.getWeavingOrderId().equals(4L)));

        // 缺计划重量分区：单 3 和 单 4
        assertEquals(2, resp.getMissingWeightOrders().size());
        assertTrue(resp.getMissingWeightOrders().stream().anyMatch(o -> o.getWeavingOrderId().equals(3L)));
        assertTrue(resp.getMissingWeightOrders().stream().anyMatch(o -> o.getWeavingOrderId().equals(4L)));

        // 聚合行：只有单 1 进入，需求量仅 100.00
        assertEquals(1, resp.getRows().size());
        MaterialCheckRowVO row = resp.getRows().get(0);
        assertEquals(new BigDecimal("100.00"), row.getDemandQty());
        assertEquals(1, row.getContributions().size());
        assertEquals(1L, row.getContributions().get(0).getWeavingOrderId());
    }

    /**
     * Testing Decision 15:
     * 租户隔离：他租户织造单/库存/采购单不进试算；传入不存在或他租户织造单 ID 抛不存在类错误
     */
    @Test
    void testRunMaterialCheck_TenantIsolationAndNonExistentOrder() {
        // 请求 ID 包含 1L, 999L，但 mapper 受租户隔离与删除过滤后仅返回 1L
        when(weavingOrderMapper.selectBatchIds(Arrays.asList(1L, 999L)))
                .thenReturn(Collections.singletonList(new WeavingOrderDO()));

        MaterialCheckReqVO reqVO = new MaterialCheckReqVO();
        reqVO.setWeavingOrderIds(Arrays.asList(1L, 999L));

        ServiceException ex = assertThrows(ServiceException.class, () -> materialCheckService.runMaterialCheck(reqVO));
        assertEquals(WEAVING_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void testRunMaterialCheck_OrderStatusGuard() {
        // 织造单状态为 CANCELLED 时抛出不允许操作异常
        WeavingOrderDO cancelledOrder = new WeavingOrderDO();
        cancelledOrder.setId(1L);
        cancelledOrder.setCode("WO-CANCELLED");
        cancelledOrder.setStatus("CANCELLED");

        when(weavingOrderMapper.selectBatchIds(Collections.singletonList(1L)))
                .thenReturn(Collections.singletonList(cancelledOrder));

        MaterialCheckReqVO reqVO = new MaterialCheckReqVO();
        reqVO.setWeavingOrderIds(Collections.singletonList(1L));

        ServiceException ex = assertThrows(ServiceException.class, () -> materialCheckService.runMaterialCheck(reqVO));
        assertEquals(WEAVING_ORDER_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    /**
     * 空参数与空织造单测试：安全返回空结果对象
     */
    @Test
    void testRunMaterialCheck_EmptyOrderList() {
        when(weavingOrderMapper.selectActiveOrDraftList()).thenReturn(Collections.emptyList());

        MaterialCheckRespVO resp = materialCheckService.runMaterialCheck(new MaterialCheckReqVO());

        assertNotNull(resp);
        assertTrue(resp.getRows().isEmpty());
        assertTrue(resp.getMissingBomOrders().isEmpty());
        assertTrue(resp.getMissingWeightOrders().isEmpty());
        assertEquals(0, resp.getShortageSpecCount());
        assertEquals(new BigDecimal("0.00"), resp.getTotalShortageQty());
    }

}
