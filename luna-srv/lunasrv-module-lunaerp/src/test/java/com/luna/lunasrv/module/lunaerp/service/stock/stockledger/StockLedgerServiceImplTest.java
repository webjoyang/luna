package com.luna.lunasrv.module.lunaerp.service.stock.stockledger;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalancePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalanceRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.StockLedgerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.StockLedgerRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link StockLedgerServiceImpl} 单元测试
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("库存台账与余额服务测试")
class StockLedgerServiceImplTest {

    @InjectMocks
    private StockLedgerServiceImpl stockLedgerService;

    @Mock
    private StockLedgerMapper stockLedgerMapper;

    @Test
    @DisplayName("获得库存余额分页 - 返回聚合结果")
    void testGetInventoryBalancePage_success() {
        InventoryBalancePageReqVO reqVO = new InventoryBalancePageReqVO();
        reqVO.setProductKeyword("棉纱");
        reqVO.setWarehouseId(1L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        InventoryBalanceRespVO balance = new InventoryBalanceRespVO();
        balance.setProductId(10L);
        balance.setProductCode("P001");
        balance.setProductName("精梳棉纱");
        balance.setWarehouseId(1L);
        balance.setWarehouseName("1号仓");
        balance.setUnit("KG");
        balance.setQuantity(new BigDecimal("1500.50"));

        PageResult<InventoryBalanceRespVO> expectedResult = new PageResult<>(List.of(balance), 1L);
        when(stockLedgerMapper.selectBalancePage(reqVO)).thenReturn(expectedResult);

        PageResult<InventoryBalanceRespVO> actualResult = stockLedgerService.getInventoryBalancePage(reqVO);

        assertNotNull(actualResult);
        assertEquals(1L, actualResult.getTotal());
        assertEquals(1, actualResult.getList().size());
        InventoryBalanceRespVO row = actualResult.getList().get(0);
        assertEquals(10L, row.getProductId());
        assertEquals("P001", row.getProductCode());
        assertEquals("精梳棉纱", row.getProductName());
        assertEquals(new BigDecimal("1500.50"), row.getQuantity());
        verify(stockLedgerMapper).selectBalancePage(reqVO);
    }

    @Test
    @DisplayName("获得库存流水分页 - 转换并返回流水记录")
    void testGetStockLedgerPage_success() {
        StockLedgerPageReqVO reqVO = new StockLedgerPageReqVO();
        reqVO.setProductKeyword("P001");
        reqVO.setWarehouseId(1L);
        reqVO.setTxnType("STOCK_IN");

        StockLedgerDO ledgerDO = new StockLedgerDO();
        ledgerDO.setId(100L);
        ledgerDO.setProductId(10L);
        ledgerDO.setProductCode("P001");
        ledgerDO.setProductName("精梳棉纱");
        ledgerDO.setWarehouseId(1L);
        ledgerDO.setWarehouseName("1号仓");
        ledgerDO.setLocationId(5L);
        ledgerDO.setLocationCode("A-01");
        ledgerDO.setBatchNo("B20260901");
        ledgerDO.setTxnType("STOCK_IN");
        ledgerDO.setQuantity(new BigDecimal("500.00"));
        ledgerDO.setUnit("KG");
        ledgerDO.setUnitPrice(new BigDecimal("20.00"));
        ledgerDO.setAmount(new BigDecimal("10000.00"));
        ledgerDO.setRelatedDocType("STOCK_IN");
        ledgerDO.setRelatedDocId(2001L);
        ledgerDO.setRelatedDocNo("IN-20260925-001");
        ledgerDO.setRemark("正常入库");
        ledgerDO.setCreateTime(LocalDateTime.now());

        PageResult<StockLedgerDO> doPageResult = new PageResult<>(List.of(ledgerDO), 1L);
        when(stockLedgerMapper.selectLedgerPage(reqVO)).thenReturn(doPageResult);

        PageResult<StockLedgerRespVO> respPageResult = stockLedgerService.getStockLedgerPage(reqVO);

        assertNotNull(respPageResult);
        assertEquals(1L, respPageResult.getTotal());
        assertEquals(1, respPageResult.getList().size());
        StockLedgerRespVO resp = respPageResult.getList().get(0);
        assertEquals(100L, resp.getId());
        assertEquals("P001", resp.getProductCode());
        assertEquals("A-01", resp.getLocationCode());
        assertEquals(new BigDecimal("500.00"), resp.getQuantity());
        assertEquals("STOCK_IN", resp.getTxnType());
        assertEquals("IN-20260925-001", resp.getRelatedDocNo());
        verify(stockLedgerMapper).selectLedgerPage(reqVO);
    }

    @Test
    @DisplayName("获得库存流水分页 - 空列表正常返回")
    void testGetStockLedgerPage_empty() {
        StockLedgerPageReqVO reqVO = new StockLedgerPageReqVO();
        PageResult<StockLedgerDO> doPageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(stockLedgerMapper.selectLedgerPage(reqVO)).thenReturn(doPageResult);

        PageResult<StockLedgerRespVO> respPageResult = stockLedgerService.getStockLedgerPage(reqVO);

        assertNotNull(respPageResult);
        assertEquals(0L, respPageResult.getTotal());
        assertTrue(respPageResult.getList().isEmpty());
    }

}
