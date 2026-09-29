package com.luna.lunasrv.module.lunaerp.service.production.greigedispatch;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigedispatch.GreigeDispatchRecordDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigedispatch.GreigeDispatchRecordMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link GreigeDispatchRecordServiceImpl} 纯 Mockito 单元测试。
 *
 * 覆盖：
 * 1. 创建成功：写 PROCESS_OUT 负流水，记录关联流水ID
 * 2. 余额不足拦截：返回需求量/可用量
 * 3. 删除成功：写 PROCESS_OUT_REVERSAL 正红冲，软删记录
 * 4. 受限编辑：仅染色单/染厂/备注可改
 * 5. 补填染色单：非同合同报错
 * 6. 胚布单不存在/已取消报错
 * 7. 仓库未启用报错
 * 8. 重复删除报错
 *
 * @author luna
 */
class GreigeDispatchRecordServiceImplTest {

    @Mock
    private GreigeDispatchRecordMapper greigeDispatchRecordMapper;

    @Mock
    private GreigeOrderMapper greigeOrderMapper;

    @Mock
    private DyeingOrderMapper dyeingOrderMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private StockLedgerMapper stockLedgerMapper;

    @Mock
    private StockLedgerService stockLedgerService;

    @InjectMocks
    private GreigeDispatchRecordServiceImpl greigeDispatchRecordService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GreigeDispatchRecordDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), StockLedgerDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GreigeOrderDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DyeingOrderDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), WarehouseDO.class);
    }

    private GreigeOrderDO createGreigeOrder(Long id, String status, Boolean deleted) {
        GreigeOrderDO order = new GreigeOrderDO();
        order.setId(id);
        order.setOrderNo("PB0928-001");
        order.setSalesContractId(100L);
        order.setContractNo("HT-001");
        order.setProductId(500L);
        order.setProductCode("P001");
        order.setProductName("全棉府绸");
        order.setStatus(status);
        order.setDeleted(deleted);
        return order;
    }

    private WarehouseDO createWarehouse(Long id, String status) {
        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setId(id);
        warehouse.setName("主仓");
        warehouse.setStatus(status);
        warehouse.setDeleted(false);
        return warehouse;
    }

    private DyeingOrderDO createDyeingOrder(Long id, Long salesContractId) {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(id);
        order.setSalesContractId(salesContractId);
        order.setContractNo("HT-001");
        order.setDeleted(false);
        return order;
    }

    private GreigeDispatchRecordCreateReqVO createReqVO() {
        return GreigeDispatchRecordCreateReqVO.builder()
                .greigeOrderId(1L)
                .warehouseId(10L)
                .dispatchDate(LocalDate.of(2026, 9, 28))
                .quantity(new BigDecimal("500.00"))
                .pieces(20)
                .remark("急单优先")
                .build();
    }

    @Test
    @DisplayName("创建成功：写 PROCESS_OUT 负流水，记录关联流水ID")
    void create_success() {
        GreigeDispatchRecordCreateReqVO reqVO = createReqVO();
        GreigeOrderDO greigeOrder = createGreigeOrder(1L, "ACTIVE", false);
        WarehouseDO warehouse = createWarehouse(10L, "ACTIVE");

        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse);
        when(stockLedgerService.getProductBalance(10L, 500L, "KG")).thenReturn(new BigDecimal("1000.00"));
        when(stockLedgerMapper.insert(any(StockLedgerDO.class))).thenAnswer(inv -> {
            StockLedgerDO ledger = inv.getArgument(0);
            ledger.setId(9001L);
            return 1;
        });
        when(greigeDispatchRecordMapper.insert(any(GreigeDispatchRecordDO.class))).thenAnswer(inv -> {
            GreigeDispatchRecordDO record = inv.getArgument(0);
            record.setId(2001L);
            return 1;
        });
        when(stockLedgerMapper.updateById(any(StockLedgerDO.class))).thenReturn(1);

        Long id = greigeDispatchRecordService.createGreigeDispatchRecord(reqVO);

        assertEquals(2001L, id);

        // 验证流水
        ArgumentCaptor<StockLedgerDO> ledgerCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper).insert(ledgerCaptor.capture());
        StockLedgerDO ledger = ledgerCaptor.getValue();
        assertEquals("PRODUCT", ledger.getItemType());
        assertEquals(500L, ledger.getProductId());
        assertEquals("P001", ledger.getProductCode());
        assertEquals("全棉府绸", ledger.getProductName());
        assertEquals(10L, ledger.getWarehouseId());
        assertEquals("主仓", ledger.getWarehouseName());
        assertEquals("PROCESS_OUT", ledger.getTxnType());
        assertEquals(new BigDecimal("-500.00"), ledger.getQuantity());
        assertEquals("KG", ledger.getUnit());
        assertEquals("GREIGE_DISPATCH", ledger.getRelatedDocType());
        // 验证流水备注含合同信息
        assertTrue(ledger.getRemark().contains("合同[100:HT-001]"));
        assertTrue(ledger.getRemark().contains("急单优先"));

        // 验证记录
        ArgumentCaptor<GreigeDispatchRecordDO> recordCaptor = ArgumentCaptor.forClass(GreigeDispatchRecordDO.class);
        verify(greigeDispatchRecordMapper).insert(recordCaptor.capture());
        GreigeDispatchRecordDO record = recordCaptor.getValue();
        assertEquals(1L, record.getGreigeOrderId());
        assertEquals(10L, record.getWarehouseId());
        assertEquals("主仓", record.getWarehouseName());
        assertEquals(500L, record.getProductId());
        assertEquals(new BigDecimal("500.00"), record.getQuantity());
        assertEquals(20, record.getPieces());
        assertEquals(9001L, record.getStockLedgerId());
    }

    @Test
    @DisplayName("余额不足拦截：返回需求量/可用量")
    void create_stock_not_enough() {
        GreigeDispatchRecordCreateReqVO reqVO = createReqVO();
        GreigeOrderDO greigeOrder = createGreigeOrder(1L, "ACTIVE", false);
        WarehouseDO warehouse = createWarehouse(10L, "ACTIVE");

        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse);
        when(stockLedgerService.getProductBalance(10L, 500L, "KG")).thenReturn(new BigDecimal("300.00"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> greigeDispatchRecordService.createGreigeDispatchRecord(reqVO));
        assertEquals(GREIGE_DISPATCH_STOCK_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("胚布单不存在报错")
    void create_greige_order_not_exists() {
        GreigeDispatchRecordCreateReqVO reqVO = createReqVO();
        when(greigeOrderMapper.selectById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> greigeDispatchRecordService.createGreigeDispatchRecord(reqVO));
        assertEquals(GREIGE_DISPATCH_GREIGE_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("胚布单已取消报错")
    void create_greige_order_cancelled() {
        GreigeDispatchRecordCreateReqVO reqVO = createReqVO();
        GreigeOrderDO greigeOrder = createGreigeOrder(1L, "CANCELLED", false);
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> greigeDispatchRecordService.createGreigeDispatchRecord(reqVO));
        assertEquals(GREIGE_DISPATCH_GREIGE_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("仓库未启用报错")
    void create_warehouse_not_enabled() {
        GreigeDispatchRecordCreateReqVO reqVO = createReqVO();
        GreigeOrderDO greigeOrder = createGreigeOrder(1L, "ACTIVE", false);
        WarehouseDO warehouse = createWarehouse(10L, "INACTIVE");

        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> greigeDispatchRecordService.createGreigeDispatchRecord(reqVO));
        assertEquals(GREIGE_DISPATCH_WAREHOUSE_NOT_ENABLED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("补填染色单：非同合同报错")
    void create_dyeing_order_contract_mismatch() {
        GreigeDispatchRecordCreateReqVO reqVO = createReqVO();
        reqVO.setDyeingOrderId(2L);
        GreigeOrderDO greigeOrder = createGreigeOrder(1L, "ACTIVE", false);
        WarehouseDO warehouse = createWarehouse(10L, "ACTIVE");
        DyeingOrderDO dyeingOrder = createDyeingOrder(2L, 999L); // 不同合同

        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse);
        when(stockLedgerService.getProductBalance(10L, 500L, "KG")).thenReturn(new BigDecimal("1000.00"));
        when(dyeingOrderMapper.selectById(2L)).thenReturn(dyeingOrder);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> greigeDispatchRecordService.createGreigeDispatchRecord(reqVO));
        assertEquals(GREIGE_DISPATCH_DYEING_ORDER_CONTRACT_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除成功：写 PROCESS_OUT_REVERSAL 正红冲，软删记录")
    void delete_success() {
        GreigeDispatchRecordDO record = new GreigeDispatchRecordDO();
        record.setId(2001L);
        record.setGreigeOrderId(1L);
        record.setStockLedgerId(9001L);
        record.setQuantity(new BigDecimal("500.00"));
        record.setDeleted(false);

        StockLedgerDO origLedger = new StockLedgerDO();
        origLedger.setId(9001L);
        origLedger.setItemType("PRODUCT");
        origLedger.setProductId(500L);
        origLedger.setProductCode("P001");
        origLedger.setProductName("全棉府绸");
        origLedger.setWarehouseId(10L);
        origLedger.setWarehouseName("主仓");
        origLedger.setTxnType("PROCESS_OUT");
        origLedger.setQuantity(new BigDecimal("-500.00"));
        origLedger.setUnit("KG");
        origLedger.setRelatedDocType("GREIGE_DISPATCH");
        origLedger.setRelatedDocId(2001L);

        when(greigeDispatchRecordMapper.selectById(2001L)).thenReturn(record);
        when(stockLedgerMapper.selectById(9001L)).thenReturn(origLedger);
        when(stockLedgerMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(Collections.emptyList());
        when(stockLedgerMapper.insert(any(StockLedgerDO.class))).thenReturn(1);
        when(greigeDispatchRecordMapper.deleteById(2001L)).thenReturn(1);

        greigeDispatchRecordService.deleteGreigeDispatchRecord(2001L);

        // 验证红冲流水
        ArgumentCaptor<StockLedgerDO> ledgerCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper).insert(ledgerCaptor.capture());
        StockLedgerDO reversal = ledgerCaptor.getValue();
        assertEquals("PROCESS_OUT_REVERSAL", reversal.getTxnType());
        assertEquals(new BigDecimal("500.00"), reversal.getQuantity()); // 正数
        assertEquals("GREIGE_DISPATCH", reversal.getRelatedDocType());
        assertEquals(2001L, reversal.getRelatedDocId());
        assertTrue(reversal.getRemark().contains("REVERSE_LEDGER_9001"));

        // 验证软删
        verify(greigeDispatchRecordMapper).deleteById(2001L);
    }

    @Test
    @DisplayName("重复删除报错")
    void delete_already_reversed() {
        GreigeDispatchRecordDO record = new GreigeDispatchRecordDO();
        record.setId(2001L);
        record.setStockLedgerId(9001L);
        record.setDeleted(false);

        StockLedgerDO origLedger = new StockLedgerDO();
        origLedger.setId(9001L);

        StockLedgerDO existingReversal = new StockLedgerDO();
        existingReversal.setId(9002L);

        when(greigeDispatchRecordMapper.selectById(2001L)).thenReturn(record);
        when(stockLedgerMapper.selectById(9001L)).thenReturn(origLedger);
        when(stockLedgerMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(existingReversal));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> greigeDispatchRecordService.deleteGreigeDispatchRecord(2001L));
        assertEquals(GREIGE_DISPATCH_ALREADY_REVERSED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("受限编辑：仅染色单/染厂/备注可改")
    void update_restricted_fields() {
        GreigeDispatchRecordDO record = new GreigeDispatchRecordDO();
        record.setId(2001L);
        record.setGreigeOrderId(1L);
        record.setDyeingOrderId(null);
        record.setDyeingFactoryId(null);
        record.setRemark("旧备注");
        record.setQuantity(new BigDecimal("500.00"));
        record.setDeleted(false);

        GreigeOrderDO greigeOrder = createGreigeOrder(1L, "ACTIVE", false);
        DyeingOrderDO dyeingOrder = createDyeingOrder(2L, 100L); // 同合同

        when(greigeDispatchRecordMapper.selectById(2001L)).thenReturn(record);
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder);
        when(dyeingOrderMapper.selectById(2L)).thenReturn(dyeingOrder);
        when(greigeDispatchRecordMapper.updateById(any(GreigeDispatchRecordDO.class))).thenReturn(1);

        GreigeDispatchRecordUpdateReqVO reqVO = GreigeDispatchRecordUpdateReqVO.builder()
                .id(2001L)
                .dyeingOrderId(2L)
                .dyeingFactoryId(300L)
                .dyeingFactoryName("XX染厂")
                .remark("新备注")
                .build();

        greigeDispatchRecordService.updateGreigeDispatchRecord(reqVO);

        ArgumentCaptor<GreigeDispatchRecordDO> captor = ArgumentCaptor.forClass(GreigeDispatchRecordDO.class);
        verify(greigeDispatchRecordMapper).updateById(captor.capture());
        GreigeDispatchRecordDO updated = captor.getValue();
        assertEquals(2L, updated.getDyeingOrderId());
        assertEquals(300L, updated.getDyeingFactoryId());
        assertEquals("XX染厂", updated.getDyeingFactoryName());
        assertEquals("新备注", updated.getRemark());
        // 数量未变
        assertEquals(new BigDecimal("500.00"), updated.getQuantity());
    }

    @Test
    @DisplayName("更新时染色单非同合同报错")
    void update_dyeing_order_contract_mismatch() {
        GreigeDispatchRecordDO record = new GreigeDispatchRecordDO();
        record.setId(2001L);
        record.setGreigeOrderId(1L);
        record.setDeleted(false);

        GreigeOrderDO greigeOrder = createGreigeOrder(1L, "ACTIVE", false);
        DyeingOrderDO dyeingOrder = createDyeingOrder(2L, 999L); // 不同合同

        when(greigeDispatchRecordMapper.selectById(2001L)).thenReturn(record);
        when(greigeOrderMapper.selectById(1L)).thenReturn(greigeOrder);
        when(dyeingOrderMapper.selectById(2L)).thenReturn(dyeingOrder);

        GreigeDispatchRecordUpdateReqVO reqVO = GreigeDispatchRecordUpdateReqVO.builder()
                .id(2001L)
                .dyeingOrderId(2L)
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> greigeDispatchRecordService.updateGreigeDispatchRecord(reqVO));
        assertEquals(GREIGE_DISPATCH_DYEING_ORDER_CONTRACT_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("按胚布单ID集合批量统计已发染量")
    void sum_dispatched_qty() {
        GreigeDispatchRecordDO r1 = new GreigeDispatchRecordDO();
        r1.setGreigeOrderId(1L);
        r1.setQuantity(new BigDecimal("300.00"));

        GreigeDispatchRecordDO r2 = new GreigeDispatchRecordDO();
        r2.setGreigeOrderId(1L);
        r2.setQuantity(new BigDecimal("200.00"));

        GreigeDispatchRecordDO r3 = new GreigeDispatchRecordDO();
        r3.setGreigeOrderId(2L);
        r3.setQuantity(new BigDecimal("500.00"));

        when(greigeDispatchRecordMapper.selectListByGreigeOrderIds(List.of(1L, 2L)))
                .thenReturn(List.of(r1, r2, r3));

        Map<Long, BigDecimal> result = greigeDispatchRecordService.sumDispatchedQtyByGreigeOrderIds(List.of(1L, 2L));

        assertEquals(new BigDecimal("500.00"), result.get(1L));
        assertEquals(new BigDecimal("500.00"), result.get(2L));
    }

}
