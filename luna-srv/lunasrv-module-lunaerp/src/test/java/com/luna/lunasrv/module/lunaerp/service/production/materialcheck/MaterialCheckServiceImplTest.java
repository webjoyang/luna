package com.luna.lunasrv.module.lunaerp.service.production.materialcheck;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalanceRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WEAVING_ORDER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

/**
 * {@link MaterialCheckServiceImpl} 单元测试
 *
 * 覆盖 Issue 05 核心规格：
 * 接缝 1：织造单有效性与租户/存在性校验
 * 接缝 2：分区分类与非正常单据过滤（缺BOM / 缺重量）
 * 接缝 3：纱线需求合并与贡献展开
 * 接缝 4：可用库存与在途采购量批量聚合
 * 接缝 5：缺口核算、建议采购量与汇总指标
 */
class MaterialCheckServiceImplTest {

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @Mock
    private YarnRecipeBomMapper yarnRecipeBomMapper;

    @Mock
    private YarnRecipeBomItemMapper yarnRecipeBomItemMapper;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @Mock
    private StockLedgerMapper stockLedgerMapper;

    @Mock
    private YarnOrderItemMapper yarnOrderItemMapper;

    @InjectMocks
    private MaterialCheckServiceImpl materialCheckService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WeavingOrderDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), YarnRecipeBomDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), YarnRecipeBomItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), YarnSpecDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), YarnTypeDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), StockLedgerDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), YarnOrderItemDO.class);
    }

    // ==================== 接缝 1：织造单有效性与租户/存在性校验 ====================

    @Test
    @DisplayName("租户隔离与存在性：传入不存在或他租户织造单 ID -> 抛 WEAVING_ORDER_NOT_EXISTS 异常")
    void testCalculate_nonExistentOrOtherTenantOrder_throwsException() {
        MaterialCheckReqVO req = new MaterialCheckReqVO();
        req.setWeavingOrderIds(List.of(100L, 101L));

        // 仅返回 100L，101L 为他租户或不存在
        WeavingOrderDO order100 = buildWeavingOrder(100L, "WO-100", 1L, "P1", "纯棉汗布", 50, new BigDecimal("1000.00"), "ACTIVE");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order100));

        ServiceException ex = assertThrows(ServiceException.class, () -> materialCheckService.calculateMaterialCheck(req));
        assertEquals(WEAVING_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("缺省全选：未传 weavingOrderIds 时查询当前租户全部 DRAFT/ACTIVE 织造单，若无则返回空结果")
    void testCalculate_emptyWeavingOrderIds_queriesAllDraftAndActive_returnsEmptyWhenNone() {
        MaterialCheckReqVO req = new MaterialCheckReqVO();
        req.setWeavingOrderIds(Collections.emptyList());

        when(weavingOrderMapper.selectList(any())).thenReturn(Collections.emptyList());

        MaterialCheckRespVO resp = materialCheckService.calculateMaterialCheck(req);

        assertNotNull(resp);
        assertTrue(resp.getRows().isEmpty());
        assertTrue(resp.getMissingBomOrders().isEmpty());
        assertTrue(resp.getMissingWeightOrders().isEmpty());
        assertEquals(0, resp.getShortageSpecCount());
        assertEquals(BigDecimal.ZERO, resp.getTotalShortageQty());
    }

    // ==================== 接缝 2：分区分类与非正常单据过滤 ====================

    @Test
    @DisplayName("非正常单据过滤：无 BOM 单入 missingBomOrders、缺计划重量单入 missingWeightOrders，均不污染聚合行")
    void testCalculate_missingBomAndMissingWeight_partitionedCorrectly_notInRows() {
        // order1: 缺 BOM（productId = 999，库内无 BOM）
        WeavingOrderDO order1 = buildWeavingOrder(101L, "WO-101", 999L, "P999", "无BOM布", 50, new BigDecimal("1000.00"), "ACTIVE");
        // order2: 缺计划重量（plannedWeight 为 null）
        WeavingOrderDO order2 = buildWeavingOrder(102L, "WO-102", 1L, "P1", "纯棉汗布", 50, null, "ACTIVE");
        // order3: 缺计划匹数（plannedQty 为 0）
        WeavingOrderDO order3 = buildWeavingOrder(103L, "WO-103", 1L, "P1", "纯棉汗布", 0, new BigDecimal("1000.00"), "ACTIVE");
        // order4: 正常单据
        WeavingOrderDO order4 = buildWeavingOrder(104L, "WO-104", 1L, "P1", "纯棉汗布", 50, new BigDecimal("1000.00"), "ACTIVE");

        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order1, order2, order3, order4));

        // productId=1L 有 BOM
        YarnRecipeBomDO bom1 = buildBom(10L, 1L, "ACTIVE");
        when(yarnRecipeBomMapper.selectList(any())).thenReturn(List.of(bom1));

        YarnRecipeBomItemDO item1 = buildBomItem(1L, 10L, 201L, 301L, new BigDecimal("100.00"));
        when(yarnRecipeBomItemMapper.selectListByRecipeBomIds(any())).thenReturn(List.of(item1));

        YarnSpecDO spec1 = buildYarnSpec(201L, 301L, "32S 棉");
        when(yarnSpecMapper.selectBatchIds(any())).thenReturn(List.of(spec1));

        YarnTypeDO type1 = buildYarnType(301L, "棉");
        when(yarnTypeMapper.selectBatchIds(any())).thenReturn(List.of(type1));

        when(stockLedgerMapper.sumQuantityByYarnSpecIdsAndWarehouseIds(any(), nullable(Collection.class))).thenReturn(Collections.emptyList());
        when(yarnOrderItemMapper.sumInTransitQtyByYarnSpecIds(any())).thenReturn(Collections.emptyList());

        MaterialCheckReqVO req = new MaterialCheckReqVO();
        req.setWeavingOrderIds(List.of(101L, 102L, 103L, 104L));

        MaterialCheckRespVO resp = materialCheckService.calculateMaterialCheck(req);

        // missingBomOrders 应含 order1
        assertEquals(1, resp.getMissingBomOrders().size());
        assertEquals("WO-101", resp.getMissingBomOrders().get(0).getWeavingOrderCode());

        // missingWeightOrders 应含 order2 和 order3
        assertEquals(2, resp.getMissingWeightOrders().size());
        assertTrue(resp.getMissingWeightOrders().stream().anyMatch(o -> "WO-102".equals(o.getWeavingOrderCode())));
        assertTrue(resp.getMissingWeightOrders().stream().anyMatch(o -> "WO-103".equals(o.getWeavingOrderCode())));

        // rows 仅聚合 order4 的需求量 1000.00 KG
        assertEquals(1, resp.getRows().size());
        MaterialCheckRowVO row = resp.getRows().get(0);
        assertEquals(new BigDecimal("1000.00"), row.getRequiredQty());
        assertEquals(1, row.getContributions().size());
        assertEquals("WO-104", row.getContributions().get(0).getWeavingOrderCode());
    }

    // ==================== 接缝 3：纱线需求合并与贡献展开 ====================

    @Test
    @DisplayName("需求合并：两织造单同产品按 yarnSpecId 合并需求 = Σ(plannedWeight × ratio/100)，每单贡献可展开")
    void testCalculate_twoOrdersSameProduct_mergesRequirementByYarnSpecId_withContributions() {
        // 织造单 1：计划重量 1000.00 KG
        WeavingOrderDO order1 = buildWeavingOrder(101L, "WO-101", 1L, "P1", "纯棉汗布", 50, new BigDecimal("1000.00"), "ACTIVE");
        // 织造单 2：计划重量 500.00 KG
        WeavingOrderDO order2 = buildWeavingOrder(102L, "WO-102", 1L, "P1", "纯棉汗布", 25, new BigDecimal("500.00"), "ACTIVE");

        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order1, order2));

        // 产品 1 配比：201L (32S 棉) 占比 60.00%，202L (40S 涤) 占比 40.00%
        YarnRecipeBomDO bom1 = buildBom(10L, 1L, "ACTIVE");
        when(yarnRecipeBomMapper.selectList(any())).thenReturn(List.of(bom1));

        YarnRecipeBomItemDO item1 = buildBomItem(1L, 10L, 201L, 301L, new BigDecimal("60.00"));
        YarnRecipeBomItemDO item2 = buildBomItem(2L, 10L, 202L, 302L, new BigDecimal("40.00"));
        when(yarnRecipeBomItemMapper.selectListByRecipeBomIds(any())).thenReturn(List.of(item1, item2));

        YarnSpecDO spec1 = buildYarnSpec(201L, 301L, "32S 棉");
        YarnSpecDO spec2 = buildYarnSpec(202L, 302L, "40S 涤");
        when(yarnSpecMapper.selectBatchIds(any())).thenReturn(List.of(spec1, spec2));

        YarnTypeDO type1 = buildYarnType(301L, "棉");
        YarnTypeDO type2 = buildYarnType(302L, "涤纶");
        when(yarnTypeMapper.selectBatchIds(any())).thenReturn(List.of(type1, type2));

        when(stockLedgerMapper.sumQuantityByYarnSpecIdsAndWarehouseIds(any(), nullable(Collection.class))).thenReturn(Collections.emptyList());
        when(yarnOrderItemMapper.sumInTransitQtyByYarnSpecIds(any())).thenReturn(Collections.emptyList());

        MaterialCheckReqVO req = new MaterialCheckReqVO();
        req.setWeavingOrderIds(List.of(101L, 102L));

        MaterialCheckRespVO resp = materialCheckService.calculateMaterialCheck(req);

        // 预期两个纱线规格行
        assertEquals(2, resp.getRows().size());

        // 规格 201L (32S 棉)：
        // order1 需求 = 1000 * 60% = 600.00
        // order2 需求 = 500 * 60% = 300.00
        // 合并需求 = 900.00
        MaterialCheckRowVO rowCotton = resp.getRows().stream().filter(r -> r.getYarnSpecId().equals(201L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("900.00"), rowCotton.getRequiredQty());
        assertEquals("32S 棉", rowCotton.getYarnSpecName());
        assertEquals("棉", rowCotton.getYarnTypeName());
        assertEquals(2, rowCotton.getContributions().size());
        assertEquals(new BigDecimal("600.00"), rowCotton.getContributions().get(0).getRequiredQty());
        assertEquals(new BigDecimal("300.00"), rowCotton.getContributions().get(1).getRequiredQty());

        // 规格 202L (40S 涤)：
        // order1 需求 = 1000 * 40% = 400.00
        // order2 需求 = 500 * 40% = 200.00
        // 合并需求 = 600.00
        MaterialCheckRowVO rowPoly = resp.getRows().stream().filter(r -> r.getYarnSpecId().equals(202L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("600.00"), rowPoly.getRequiredQty());
        assertEquals(2, rowPoly.getContributions().size());
    }

    // ==================== 接缝 4：可用库存与在途采购量批量聚合 ====================

    @Test
    @DisplayName("库存与在途批量聚合：可用库存含 YARN_OUT 负流水扣减、仓库筛选生效、在途仅计 ACTIVE 且无 N+1")
    void testCalculate_stockAndInTransitBatchAggregation_withoutNPlusOne() {
        WeavingOrderDO order = buildWeavingOrder(101L, "WO-101", 1L, "P1", "纯棉汗布", 50, new BigDecimal("1000.00"), "ACTIVE");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        YarnRecipeBomDO bom1 = buildBom(10L, 1L, "ACTIVE");
        when(yarnRecipeBomMapper.selectList(any())).thenReturn(List.of(bom1));

        YarnRecipeBomItemDO item1 = buildBomItem(1L, 10L, 201L, 301L, new BigDecimal("100.00"));
        when(yarnRecipeBomItemMapper.selectListByRecipeBomIds(any())).thenReturn(List.of(item1));

        YarnSpecDO spec1 = buildYarnSpec(201L, 301L, "32S 棉");
        when(yarnSpecMapper.selectBatchIds(any())).thenReturn(List.of(spec1));
        when(yarnTypeMapper.selectBatchIds(any())).thenReturn(List.of(buildYarnType(301L, "棉")));

        // 可用库存：800.00 KG（台账实时求和，已含 YARN_OUT 负数）
        InventoryBalanceRespVO balance = new InventoryBalanceRespVO();
        balance.setYarnSpecId(201L);
        balance.setQuantity(new BigDecimal("800.00"));
        when(stockLedgerMapper.sumQuantityByYarnSpecIdsAndWarehouseIds(any(), eq(List.of(1L, 2L))))
                .thenReturn(List.of(balance));

        // 在途采购量：150.00 KG（来自 ACTIVE 纱线采购单）
        YarnOrderItemDO inTransitItem = new YarnOrderItemDO();
        inTransitItem.setYarnSpecId(201L);
        inTransitItem.setPurchaseQty(new BigDecimal("150.00"));
        when(yarnOrderItemMapper.sumInTransitQtyByYarnSpecIds(any()))
                .thenReturn(List.of(inTransitItem));

        MaterialCheckReqVO req = new MaterialCheckReqVO();
        req.setWeavingOrderIds(List.of(101L));
        req.setWarehouseIds(List.of(1L, 2L)); // 指定仓库过滤

        MaterialCheckRespVO resp = materialCheckService.calculateMaterialCheck(req);

        assertEquals(1, resp.getRows().size());
        MaterialCheckRowVO row = resp.getRows().get(0);
        assertEquals(new BigDecimal("1000.00"), row.getRequiredQty());
        assertEquals(new BigDecimal("800.00"), row.getAvailableQty());
        assertEquals(new BigDecimal("150.00"), row.getInTransitQty());

        // 验证防 N+1：库存与在途批量聚合各仅调用 1 次
        verify(stockLedgerMapper, times(1)).sumQuantityByYarnSpecIdsAndWarehouseIds(any(), any());
        verify(yarnOrderItemMapper, times(1)).sumInTransitQtyByYarnSpecIds(any());
    }

    // ==================== 接缝 5：缺口核算、建议采购量与汇总指标 ====================

    @Test
    @DisplayName("缺口与建议采购量：齐套、在途可覆盖、缺料三种状态下缺口与建议采购量计算正确")
    void testCalculate_shortageAndSuggestedPurchase_threeStates() {
        // 单据计划重量 1000.00 KG
        WeavingOrderDO order = buildWeavingOrder(101L, "WO-101", 1L, "P1", "三色混纺布", 50, new BigDecimal("1000.00"), "ACTIVE");
        when(weavingOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        YarnRecipeBomDO bom1 = buildBom(10L, 1L, "ACTIVE");
        when(yarnRecipeBomMapper.selectList(any())).thenReturn(List.of(bom1));

        // 规格 A (201L): 配比 30% -> 需求 300.00 KG
        // 规格 B (202L): 配比 30% -> 需求 300.00 KG
        // 规格 C (203L): 配比 40% -> 需求 400.00 KG
        YarnRecipeBomItemDO itemA = buildBomItem(1L, 10L, 201L, 301L, new BigDecimal("30.00"));
        YarnRecipeBomItemDO itemB = buildBomItem(2L, 10L, 202L, 301L, new BigDecimal("30.00"));
        YarnRecipeBomItemDO itemC = buildBomItem(3L, 10L, 203L, 301L, new BigDecimal("40.00"));
        when(yarnRecipeBomItemMapper.selectListByRecipeBomIds(any())).thenReturn(List.of(itemA, itemB, itemC));

        when(yarnSpecMapper.selectBatchIds(any())).thenReturn(List.of(
                buildYarnSpec(201L, 301L, "纱A"),
                buildYarnSpec(202L, 301L, "纱B"),
                buildYarnSpec(203L, 301L, "纱C")
        ));
        when(yarnTypeMapper.selectBatchIds(any())).thenReturn(List.of(buildYarnType(301L, "通用材质")));

        // 规格 A：需求 300，可用 350，在途 0 -> 齐套
        // 缺口(不含在途)=max(0, 300-350)=0, 缺口(含在途)=max(0, 300-350-0)=0, 建议采购量=0
        // 规格 B：需求 300，可用 100，在途 250 -> 在途可覆盖
        // 缺口(不含在途)=max(0, 300-100)=200, 缺口(含在途)=max(0, 300-100-250)=0, 建议采购量=0
        // 规格 C：需求 400，可用 100，在途 150 -> 缺料
        // 缺口(不含在途)=max(0, 400-100)=300, 缺口(含在途)=max(0, 400-100-150)=150, 建议采购量=150
        InventoryBalanceRespVO bA = new InventoryBalanceRespVO();
        bA.setYarnSpecId(201L);
        bA.setQuantity(new BigDecimal("350.00"));
        InventoryBalanceRespVO bB = new InventoryBalanceRespVO();
        bB.setYarnSpecId(202L);
        bB.setQuantity(new BigDecimal("100.00"));
        InventoryBalanceRespVO bC = new InventoryBalanceRespVO();
        bC.setYarnSpecId(203L);
        bC.setQuantity(new BigDecimal("100.00"));

        when(stockLedgerMapper.sumQuantityByYarnSpecIdsAndWarehouseIds(any(), nullable(Collection.class))).thenReturn(List.of(bA, bB, bC));

        YarnOrderItemDO oB = new YarnOrderItemDO();
        oB.setYarnSpecId(202L);
        oB.setPurchaseQty(new BigDecimal("250.00"));
        YarnOrderItemDO oC = new YarnOrderItemDO();
        oC.setYarnSpecId(203L);
        oC.setPurchaseQty(new BigDecimal("150.00"));

        when(yarnOrderItemMapper.sumInTransitQtyByYarnSpecIds(any())).thenReturn(List.of(oB, oC));

        // 1. 默认 includeInTransit = false
        MaterialCheckReqVO reqExclude = new MaterialCheckReqVO();
        reqExclude.setWeavingOrderIds(List.of(101L));
        reqExclude.setIncludeInTransit(false);

        MaterialCheckRespVO respExclude = materialCheckService.calculateMaterialCheck(reqExclude);

        MaterialCheckRowVO rowA = respExclude.getRows().stream().filter(r -> r.getYarnSpecId().equals(201L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("0.00"), rowA.getShortageExcludeInTransit());
        assertEquals(new BigDecimal("0.00"), rowA.getShortageIncludeInTransit());
        assertEquals(new BigDecimal("0.00"), rowA.getSuggestedPurchaseQty());

        MaterialCheckRowVO rowB = respExclude.getRows().stream().filter(r -> r.getYarnSpecId().equals(202L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("200.00"), rowB.getShortageExcludeInTransit());
        assertEquals(new BigDecimal("0.00"), rowB.getShortageIncludeInTransit());
        assertEquals(new BigDecimal("0.00"), rowB.getSuggestedPurchaseQty());

        MaterialCheckRowVO rowC = respExclude.getRows().stream().filter(r -> r.getYarnSpecId().equals(203L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("300.00"), rowC.getShortageExcludeInTransit());
        assertEquals(new BigDecimal("150.00"), rowC.getShortageIncludeInTransit());
        assertEquals(new BigDecimal("150.00"), rowC.getSuggestedPurchaseQty());

        // includeInTransit=false 时徽标：缺料规格数 2 (B和C均不含在途有缺口)，合计缺口 200 + 300 = 500.00
        assertEquals(2, respExclude.getShortageSpecCount());
        assertEquals(new BigDecimal("500.00"), respExclude.getTotalShortageQty());

        // 2. includeInTransit = true
        MaterialCheckReqVO reqInclude = new MaterialCheckReqVO();
        reqInclude.setWeavingOrderIds(List.of(101L));
        reqInclude.setIncludeInTransit(true);

        MaterialCheckRespVO respInclude = materialCheckService.calculateMaterialCheck(reqInclude);

        // includeInTransit=true 时徽标：缺料规格数 1 (仅C含在途仍有缺口)，合计缺口 150.00
        assertEquals(1, respInclude.getShortageSpecCount());
        assertEquals(new BigDecimal("150.00"), respInclude.getTotalShortageQty());
    }

    // ==================== 辅助方法 ====================

    private WeavingOrderDO buildWeavingOrder(Long id, String code, Long productId, String productCode, String productName,
                                             Integer plannedQty, BigDecimal plannedWeight, String status) {
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(id);
        order.setCode(code);
        order.setProductId(productId);
        order.setProductCode(productCode);
        order.setProductName(productName);
        order.setPlannedQty(plannedQty);
        order.setPlannedWeight(plannedWeight);
        order.setStatus(status);
        return order;
    }

    private YarnRecipeBomDO buildBom(Long id, Long productId, String status) {
        YarnRecipeBomDO bom = new YarnRecipeBomDO();
        bom.setId(id);
        bom.setProductId(productId);
        bom.setStatus(status);
        return bom;
    }

    private YarnRecipeBomItemDO buildBomItem(Long id, Long bomId, Long yarnSpecId, Long yarnTypeId, BigDecimal ratio) {
        YarnRecipeBomItemDO item = new YarnRecipeBomItemDO();
        item.setId(id);
        item.setRecipeBomId(bomId);
        item.setYarnSpecId(yarnSpecId);
        item.setYarnTypeId(yarnTypeId);
        item.setRatio(ratio);
        return item;
    }

    private YarnSpecDO buildYarnSpec(Long id, Long yarnTypeId, String name) {
        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(id);
        spec.setYarnTypeId(yarnTypeId);
        spec.setSpecName(name);
        return spec;
    }

    private YarnTypeDO buildYarnType(Long id, String name) {
        YarnTypeDO type = new YarnTypeDO();
        type.setId(id);
        type.setName(name);
        return type;
    }

}
