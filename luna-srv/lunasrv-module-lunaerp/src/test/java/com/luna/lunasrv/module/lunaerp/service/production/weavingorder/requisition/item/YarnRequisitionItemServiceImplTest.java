package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition.item;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.YarnRequisitionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.item.YarnRequisitionItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouselocation.WarehouseLocationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.YarnRequisitionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.item.YarnRequisitionItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouselocation.WarehouseLocationMapper;
import com.luna.lunasrv.module.lunaerp.service.stock.stockledger.StockLedgerService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link YarnRequisitionItemServiceImpl} 单元测试
 *
 * 覆盖 Issue 09 / Issue 10 领纱明细独立子资源与发纱/撤销红冲规格：
 * 1. 创建明细：父级必须为 DRAFT，自动补齐规格名称与材质快照，缺省 unit=KG、dispatchedQty=0；
 * 2. 用量校验：需求量必须大于 0；
 * 3. 状态守卫：父级非 DRAFT 态禁止新增、更新、删除；
 * 4. 规格存在性校验；
 * 5. 发纱端点：同事务写 YARN_OUT（负数）、回加 dispatchedQty、推导领纱单状态（首次发→PARTIAL、发齐→DISPATCHED）；
 * 6. 库存不足拦截：可用量 < 发纱量，报 1-030-015-311（返回 required / available）；
 * 7. 领纱单状态守卫（312）：非 ACTIVE/PARTIAL 态禁止发纱；
 * 8. 撤销发纱（红冲）：写 YARN_OUT_REVERSAL（正数）、回减 dispatchedQty、状态回退（DISPATCHED→PARTIAL/ACTIVE）；
 * 9. 防重复红冲与发纱流水记录列表。
 *
 * @author luna
 */
class YarnRequisitionItemServiceImplTest {

    @Mock
    private YarnRequisitionItemMapper yarnRequisitionItemMapper;

    @Mock
    private YarnRequisitionMapper yarnRequisitionMapper;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @Mock
    private StockLedgerService stockLedgerService;

    @Mock
    private StockLedgerMapper stockLedgerMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private WarehouseLocationMapper warehouseLocationMapper;

    @InjectMocks
    private YarnRequisitionItemServiceImpl yarnRequisitionItemService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), YarnRequisitionItemDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), StockLedgerDO.class);
    }

    @Test
    @DisplayName("创建明细 - 成功创建，自动补齐规格名称与材质快照")
    void testCreateYarnRequisitionItem_Success() {
        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("DRAFT");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(10L);
        spec.setSpecName("32S 棉");
        spec.setYarnTypeId(1L);
        when(yarnSpecMapper.selectById(10L)).thenReturn(spec);

        doAnswer(invocation -> {
            YarnRequisitionItemDO arg = invocation.getArgument(0);
            arg.setId(1024L);
            return 1;
        }).when(yarnRequisitionItemMapper).insert(any(YarnRequisitionItemDO.class));

        YarnRequisitionItemSaveReqVO vo = new YarnRequisitionItemSaveReqVO();
        vo.setRequisitionId(100L);
        vo.setYarnSpecId(10L);
        vo.setRequiredQty(new BigDecimal("120.50"));
        vo.setRemark("经纱领用");

        Long id = yarnRequisitionItemService.createYarnRequisitionItem(vo);
        assertEquals(1024L, id);

        ArgumentCaptor<YarnRequisitionItemDO> captor = ArgumentCaptor.forClass(YarnRequisitionItemDO.class);
        verify(yarnRequisitionItemMapper).insert(captor.capture());
        YarnRequisitionItemDO saved = captor.getValue();
        assertEquals(100L, saved.getRequisitionId());
        assertEquals(10L, saved.getYarnSpecId());
        assertEquals("32S 棉", saved.getYarnSpecName());
        assertEquals(1L, saved.getYarnTypeId());
        assertEquals(new BigDecimal("120.50"), saved.getRequiredQty());
        assertEquals(BigDecimal.ZERO, saved.getDispatchedQty());
        assertEquals("KG", saved.getUnit());
    }

    @Test
    @DisplayName("创建明细 - 领纱单处于非 DRAFT 状态时禁止添加明细")
    void testCreateYarnRequisitionItem_NonDraftBlocked() {
        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("ACTIVE");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        YarnRequisitionItemSaveReqVO vo = new YarnRequisitionItemSaveReqVO();
        vo.setRequisitionId(100L);
        vo.setYarnSpecId(10L);
        vo.setRequiredQty(new BigDecimal("100.00"));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionItemService.createYarnRequisitionItem(vo));
        assertEquals(YARN_REQUISITION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建明细 - 需求量小于等于0拦截")
    void testCreateYarnRequisitionItem_NonPositiveQtyBlocked() {
        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("DRAFT");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        YarnRequisitionItemSaveReqVO vo = new YarnRequisitionItemSaveReqVO();
        vo.setRequisitionId(100L);
        vo.setYarnSpecId(10L);
        vo.setRequiredQty(BigDecimal.ZERO);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionItemService.createYarnRequisitionItem(vo));
        assertEquals(YARN_REQUISITION_ITEM_QTY_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("修改明细 - 仅当所属领纱单为 DRAFT 时允许修改")
    void testUpdateYarnRequisitionItem_DraftAllowed() {
        YarnRequisitionItemDO exist = new YarnRequisitionItemDO();
        exist.setId(1L);
        exist.setRequisitionId(100L);
        exist.setYarnSpecId(10L);
        exist.setRequiredQty(new BigDecimal("100.00"));
        when(yarnRequisitionItemMapper.selectById(1L)).thenReturn(exist);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("DRAFT");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(10L);
        spec.setSpecName("32S 棉");
        when(yarnSpecMapper.selectById(10L)).thenReturn(spec);

        YarnRequisitionItemSaveReqVO updateVO = new YarnRequisitionItemSaveReqVO();
        updateVO.setId(1L);
        updateVO.setYarnSpecId(10L);
        updateVO.setRequiredQty(new BigDecimal("150.00"));

        yarnRequisitionItemService.updateYarnRequisitionItem(updateVO);

        verify(yarnRequisitionItemMapper).updateById(exist);
        assertEquals(new BigDecimal("150.00"), exist.getRequiredQty());
    }

    @Test
    @DisplayName("修改明细 - 所属领纱单已提交为 ACTIVE 时禁止修改")
    void testUpdateYarnRequisitionItem_ActiveBlocked() {
        YarnRequisitionItemDO exist = new YarnRequisitionItemDO();
        exist.setId(1L);
        exist.setRequisitionId(100L);
        when(yarnRequisitionItemMapper.selectById(1L)).thenReturn(exist);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("ACTIVE");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        YarnRequisitionItemSaveReqVO updateVO = new YarnRequisitionItemSaveReqVO();
        updateVO.setId(1L);
        updateVO.setYarnSpecId(10L);
        updateVO.setRequiredQty(new BigDecimal("150.00"));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionItemService.updateYarnRequisitionItem(updateVO));
        assertEquals(YARN_REQUISITION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除明细 - 仅 DRAFT 态允许删除")
    void testDeleteYarnRequisitionItem_Success() {
        YarnRequisitionItemDO exist = new YarnRequisitionItemDO();
        exist.setId(1L);
        exist.setRequisitionId(100L);
        when(yarnRequisitionItemMapper.selectById(1L)).thenReturn(exist);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("DRAFT");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        yarnRequisitionItemService.deleteYarnRequisitionItem(1L);
        verify(yarnRequisitionItemMapper).deleteById(1L);
    }

    @Test
    @DisplayName("发纱 - 成功首次发纱，同事务写 YARN_OUT 负数、回加 dispatchedQty、状态推导为 PARTIAL")
    void testDispatchYarn_Success_Partial() {
        YarnRequisitionItemDO item = new YarnRequisitionItemDO();
        item.setId(10L);
        item.setRequisitionId(100L);
        item.setYarnSpecId(5L);
        item.setYarnSpecName("32S 棉");
        item.setRequiredQty(new BigDecimal("100.00"));
        item.setDispatchedQty(BigDecimal.ZERO);
        item.setUnit("KG");
        when(yarnRequisitionItemMapper.selectById(10L)).thenReturn(item);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setCode("YS-ZZ001-01");
        req.setStatus("ACTIVE");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setId(1L);
        warehouse.setName("一号原料仓");
        warehouse.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);

        when(stockLedgerService.getYarnSpecBalance(1L, 5L)).thenReturn(new BigDecimal("200.00"));
        when(yarnRequisitionItemMapper.selectListByRequisitionId(100L)).thenReturn(Collections.singletonList(item));

        YarnRequisitionDispatchReqVO vo = new YarnRequisitionDispatchReqVO();
        vo.setItemId(10L);
        vo.setWarehouseId(1L);
        vo.setDispatchQty(new BigDecimal("40.00"));
        vo.setRemark("首批领纱");

        yarnRequisitionItemService.dispatchYarn(vo);

        // 验证流水写入
        ArgumentCaptor<StockLedgerDO> ledgerCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper).insert(ledgerCaptor.capture());
        StockLedgerDO ledger = ledgerCaptor.getValue();
        assertEquals("YARN_SPEC", ledger.getItemType());
        assertEquals(5L, ledger.getYarnSpecId());
        assertEquals("YARN_OUT", ledger.getTxnType());
        assertEquals(new BigDecimal("-40.00"), ledger.getQuantity());
        assertEquals("YARN_REQUISITION_ITEM", ledger.getRelatedDocType());
        assertEquals(10L, ledger.getRelatedDocId());
        assertEquals("YS-ZZ001-01", ledger.getRelatedDocNo());

        // 验证已发量回加
        assertEquals(new BigDecimal("40.00"), item.getDispatchedQty());
        verify(yarnRequisitionItemMapper).updateById(item);

        // 验证状态推导为 PARTIAL
        assertEquals("PARTIAL", req.getStatus());
        verify(yarnRequisitionMapper).updateById(req);
    }

    @Test
    @DisplayName("发纱 - 全部明细发齐后推导为 DISPATCHED 状态")
    void testDispatchYarn_Success_Dispatched() {
        YarnRequisitionItemDO item1 = new YarnRequisitionItemDO();
        item1.setId(10L);
        item1.setRequisitionId(100L);
        item1.setYarnSpecId(5L);
        item1.setRequiredQty(new BigDecimal("50.00"));
        item1.setDispatchedQty(new BigDecimal("50.00"));

        YarnRequisitionItemDO item2 = new YarnRequisitionItemDO();
        item2.setId(20L);
        item2.setRequisitionId(100L);
        item2.setYarnSpecId(6L);
        item2.setRequiredQty(new BigDecimal("50.00"));
        item2.setDispatchedQty(BigDecimal.ZERO);

        when(yarnRequisitionItemMapper.selectById(20L)).thenReturn(item2);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setCode("YS-ZZ001-01");
        req.setStatus("PARTIAL");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setId(1L);
        warehouse.setName("原料仓");
        warehouse.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);

        when(stockLedgerService.getYarnSpecBalance(1L, 6L)).thenReturn(new BigDecimal("100.00"));
        when(yarnRequisitionItemMapper.selectListByRequisitionId(100L)).thenReturn(Arrays.asList(item1, item2));

        YarnRequisitionDispatchReqVO vo = new YarnRequisitionDispatchReqVO();
        vo.setItemId(20L);
        vo.setWarehouseId(1L);
        vo.setDispatchQty(new BigDecimal("50.00"));

        yarnRequisitionItemService.dispatchYarn(vo);

        // 验证状态推导为 DISPATCHED
        assertEquals(new BigDecimal("50.00"), item2.getDispatchedQty());
        assertEquals("DISPATCHED", req.getStatus());
        verify(yarnRequisitionMapper).updateById(req);
    }

    @Test
    @DisplayName("发纱 - 库存不足拦截报 1-030-015-311，不允许负库存")
    void testDispatchYarn_StockInsufficient() {
        YarnRequisitionItemDO item = new YarnRequisitionItemDO();
        item.setId(10L);
        item.setRequisitionId(100L);
        item.setYarnSpecId(5L);
        item.setRequiredQty(new BigDecimal("100.00"));
        item.setDispatchedQty(BigDecimal.ZERO);
        when(yarnRequisitionItemMapper.selectById(10L)).thenReturn(item);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("ACTIVE");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setId(1L);
        warehouse.setName("原料仓");
        warehouse.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);

        // 可用库存仅 30.00，发纱需求 50.00
        when(stockLedgerService.getYarnSpecBalance(1L, 5L)).thenReturn(new BigDecimal("30.00"));

        YarnRequisitionDispatchReqVO vo = new YarnRequisitionDispatchReqVO();
        vo.setItemId(10L);
        vo.setWarehouseId(1L);
        vo.setDispatchQty(new BigDecimal("50.00"));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionItemService.dispatchYarn(vo));
        assertEquals(YARN_REQUISITION_STOCK_NOT_ENOUGH.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("50.00") && ex.getMessage().contains("30.00"));

        // 校验未写流水，未修改明细
        verify(stockLedgerMapper, never()).insert(any(StockLedgerDO.class));
        verify(yarnRequisitionItemMapper, never()).updateById(any(YarnRequisitionItemDO.class));
    }

    @Test
    @DisplayName("发纱 - 领纱单处于 DRAFT 或 CLOSED 状态时报 312 拦截")
    void testDispatchYarn_StatusNotAllow() {
        YarnRequisitionItemDO item = new YarnRequisitionItemDO();
        item.setId(10L);
        item.setRequisitionId(100L);
        when(yarnRequisitionItemMapper.selectById(10L)).thenReturn(item);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("DRAFT");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        YarnRequisitionDispatchReqVO vo = new YarnRequisitionDispatchReqVO();
        vo.setItemId(10L);
        vo.setWarehouseId(1L);
        vo.setDispatchQty(new BigDecimal("50.00"));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionItemService.dispatchYarn(vo));
        assertEquals(YARN_REQUISITION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("撤销发纱 - 成功红冲，写 YARN_OUT_REVERSAL 正数、回减 dispatchedQty、DISPATCHED 状态回退至 PARTIAL")
    void testReverseDispatch_Success_RollbackToPartial() {
        StockLedgerDO orig = new StockLedgerDO();
        orig.setId(500L);
        orig.setItemType("YARN_SPEC");
        orig.setYarnSpecId(5L);
        orig.setWarehouseId(1L);
        orig.setWarehouseName("原料仓");
        orig.setTxnType("YARN_OUT");
        orig.setQuantity(new BigDecimal("-40.00"));
        orig.setUnit("KG");
        orig.setRelatedDocType("YARN_REQUISITION_ITEM");
        orig.setRelatedDocId(10L);
        orig.setRelatedDocNo("YS-ZZ001-01");
        when(stockLedgerMapper.selectById(500L)).thenReturn(orig);
        when(stockLedgerMapper.selectList(any())).thenReturn(Collections.emptyList());

        YarnRequisitionItemDO item = new YarnRequisitionItemDO();
        item.setId(10L);
        item.setRequisitionId(100L);
        item.setRequiredQty(new BigDecimal("100.00"));
        item.setDispatchedQty(new BigDecimal("100.00"));
        when(yarnRequisitionItemMapper.selectById(10L)).thenReturn(item);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("DISPATCHED");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        when(yarnRequisitionItemMapper.selectListByRequisitionId(100L)).thenReturn(Collections.singletonList(item));

        YarnRequisitionDispatchReversalReqVO vo = new YarnRequisitionDispatchReversalReqVO();
        vo.setStockLedgerId(500L);
        vo.setRemark("工人发错数量撤销");

        yarnRequisitionItemService.reverseDispatch(vo);

        // 验证红冲流水写入为正数
        ArgumentCaptor<StockLedgerDO> revCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper).insert(revCaptor.capture());
        StockLedgerDO rev = revCaptor.getValue();
        assertEquals("YARN_OUT_REVERSAL", rev.getTxnType());
        assertEquals(new BigDecimal("40.00"), rev.getQuantity());
        assertTrue(rev.getRemark().contains("REVERSE_LEDGER_500"));

        // 验证 dispatchedQty 回减
        assertEquals(new BigDecimal("60.00"), item.getDispatchedQty());
        verify(yarnRequisitionItemMapper).updateById(item);

        // 验证状态回退至 PARTIAL
        assertEquals("PARTIAL", req.getStatus());
        verify(yarnRequisitionMapper).updateById(req);
    }

    @Test
    @DisplayName("撤销发纱 - 全部已发量冲完后，状态回退至 ACTIVE")
    void testReverseDispatch_Success_RollbackToActive() {
        StockLedgerDO orig = new StockLedgerDO();
        orig.setId(500L);
        orig.setTxnType("YARN_OUT");
        orig.setQuantity(new BigDecimal("-30.00"));
        orig.setUnit("KG");
        orig.setRelatedDocType("YARN_REQUISITION_ITEM");
        orig.setRelatedDocId(10L);
        when(stockLedgerMapper.selectById(500L)).thenReturn(orig);
        when(stockLedgerMapper.selectList(any())).thenReturn(Collections.emptyList());

        YarnRequisitionItemDO item = new YarnRequisitionItemDO();
        item.setId(10L);
        item.setRequisitionId(100L);
        item.setRequiredQty(new BigDecimal("100.00"));
        item.setDispatchedQty(new BigDecimal("30.00"));
        when(yarnRequisitionItemMapper.selectById(10L)).thenReturn(item);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("PARTIAL");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        when(yarnRequisitionItemMapper.selectListByRequisitionId(100L)).thenReturn(Collections.singletonList(item));

        YarnRequisitionDispatchReversalReqVO vo = new YarnRequisitionDispatchReversalReqVO();
        vo.setStockLedgerId(500L);

        yarnRequisitionItemService.reverseDispatch(vo);

        // 回退至 0 KG，状态彻底回退到 ACTIVE
        assertEquals(0, BigDecimal.ZERO.compareTo(item.getDispatchedQty()));
        assertEquals("ACTIVE", req.getStatus());
        verify(yarnRequisitionMapper).updateById(req);
    }

    @Test
    @DisplayName("撤销发纱 - 重复撤销同一笔发纱记录被拦截")
    void testReverseDispatch_AlreadyReversedBlocked() {
        StockLedgerDO orig = new StockLedgerDO();
        orig.setId(500L);
        orig.setTxnType("YARN_OUT");
        orig.setRelatedDocType("YARN_REQUISITION_ITEM");
        orig.setRelatedDocId(10L);
        when(stockLedgerMapper.selectById(500L)).thenReturn(orig);

        StockLedgerDO existingRev = new StockLedgerDO();
        existingRev.setId(501L);
        existingRev.setRemark("REVERSE_LEDGER_500: 已撤销");
        when(stockLedgerMapper.selectList(any())).thenReturn(Collections.singletonList(existingRev));

        YarnRequisitionDispatchReversalReqVO vo = new YarnRequisitionDispatchReversalReqVO();
        vo.setStockLedgerId(500L);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionItemService.reverseDispatch(vo));
        assertEquals(YARN_REQUISITION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("发纱记录列表 - 正常获取并正确标记已撤销记录")
    void testGetDispatchRecordList() {
        StockLedgerDO out1 = new StockLedgerDO();
        out1.setId(101L);
        out1.setRelatedDocId(10L);
        out1.setTxnType("YARN_OUT");
        out1.setQuantity(new BigDecimal("-30.00"));
        out1.setUnit("KG");
        out1.setWarehouseName("原料仓");

        StockLedgerDO out2 = new StockLedgerDO();
        out2.setId(102L);
        out2.setRelatedDocId(10L);
        out2.setTxnType("YARN_OUT");
        out2.setQuantity(new BigDecimal("-20.00"));
        out2.setUnit("KG");
        out2.setWarehouseName("原料仓");

        StockLedgerDO rev2 = new StockLedgerDO();
        rev2.setId(103L);
        rev2.setRelatedDocId(10L);
        rev2.setTxnType("YARN_OUT_REVERSAL");
        rev2.setRemark("REVERSE_LEDGER_102: 误发撤销");
        rev2.setCreateTime(LocalDateTime.now());

        when(stockLedgerMapper.selectList(any())).thenReturn(Arrays.asList(rev2, out2, out1));

        List<YarnDispatchRecordRespVO> list = yarnRequisitionItemService.getDispatchRecordList(10L);
        assertEquals(2, list.size());

        YarnDispatchRecordRespVO record2 = list.get(0);
        assertEquals(102L, record2.getId());
        assertTrue(record2.getReversed());
        assertEquals(new BigDecimal("20.00"), record2.getQuantity());

        YarnDispatchRecordRespVO record1 = list.get(1);
        assertEquals(101L, record1.getId());
        assertFalse(record1.getReversed());
        assertEquals(new BigDecimal("30.00"), record1.getQuantity());
    }

    @Test
    @DisplayName("并发校验 - 两次扣减超出可用库存拦截第二次")
    void testConcurrency_BalanceCheck() {
        YarnRequisitionItemDO item = new YarnRequisitionItemDO();
        item.setId(10L);
        item.setRequisitionId(100L);
        item.setYarnSpecId(5L);
        item.setRequiredQty(new BigDecimal("100.00"));
        item.setDispatchedQty(BigDecimal.ZERO);
        when(yarnRequisitionItemMapper.selectById(10L)).thenReturn(item);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("ACTIVE");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setId(1L);
        warehouse.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);

        // 第一次发纱成功：可用 100，发 60
        when(stockLedgerService.getYarnSpecBalance(1L, 5L)).thenReturn(new BigDecimal("100.00"));
        when(yarnRequisitionItemMapper.selectListByRequisitionId(100L)).thenReturn(Collections.singletonList(item));

        YarnRequisitionDispatchReqVO vo1 = new YarnRequisitionDispatchReqVO();
        vo1.setItemId(10L);
        vo1.setWarehouseId(1L);
        vo1.setDispatchQty(new BigDecimal("60.00"));
        yarnRequisitionItemService.dispatchYarn(vo1);

        // 第二次并发请求到来时，可用库存变为 40，尝试发 60 拦截
        when(stockLedgerService.getYarnSpecBalance(1L, 5L)).thenReturn(new BigDecimal("40.00"));

        YarnRequisitionDispatchReqVO vo2 = new YarnRequisitionDispatchReqVO();
        vo2.setItemId(10L);
        vo2.setWarehouseId(1L);
        vo2.setDispatchQty(new BigDecimal("60.00"));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionItemService.dispatchYarn(vo2));
        assertEquals(YARN_REQUISITION_STOCK_NOT_ENOUGH.getCode(), ex.getCode());
    }

}
