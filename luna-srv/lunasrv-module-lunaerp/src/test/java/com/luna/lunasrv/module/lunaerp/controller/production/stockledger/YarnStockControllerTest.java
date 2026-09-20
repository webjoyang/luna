package com.luna.lunasrv.module.lunaerp.controller.production.stockledger;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.stockledger.vo.YarnStockBalanceRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.stockledger.StockLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link YarnStockController} 的单元测试。
 */
class YarnStockControllerTest {

    @Mock
    private StockLedgerService stockLedgerService;

    @InjectMocks
    private YarnStockController yarnStockController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getBalance_returnsSuccessResult() {
        Long yarnSpecId = 10L;
        BigDecimal stockQty = new BigDecimal("350.00");
        when(stockLedgerService.getBalance(yarnSpecId)).thenReturn(stockQty);

        CommonResult<YarnStockBalanceRespVO> result = yarnStockController.getBalance(yarnSpecId);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(yarnSpecId, result.getData().getYarnSpecId());
        assertEquals(stockQty, result.getData().getStockQty());
        verify(stockLedgerService).getBalance(yarnSpecId);
    }

    @Test
    void getBalances_returnsSuccessResult() {
        List<Long> specIds = Arrays.asList(10L, 20L);
        Map<Long, BigDecimal> map = new HashMap<>();
        map.put(10L, new BigDecimal("100.00"));
        map.put(20L, new BigDecimal("200.00"));
        when(stockLedgerService.getBalances(specIds)).thenReturn(map);

        CommonResult<List<YarnStockBalanceRespVO>> result = yarnStockController.getBalances(specIds);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        List<YarnStockBalanceRespVO> data = result.getData();
        assertEquals(2, data.size());
        assertEquals(10L, data.get(0).getYarnSpecId());
        assertEquals(new BigDecimal("100.00"), data.get(0).getStockQty());
        assertEquals(20L, data.get(1).getYarnSpecId());
        assertEquals(new BigDecimal("200.00"), data.get(1).getStockQty());
        verify(stockLedgerService).getBalances(specIds);
    }

    @Test
    void getBalances_emptyList_returnsEmptyWithoutCallingService() {
        CommonResult<List<YarnStockBalanceRespVO>> result = yarnStockController.getBalances(Collections.emptyList());

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData().isEmpty());
        verify(stockLedgerService, never()).getBalances(any());
    }

}
