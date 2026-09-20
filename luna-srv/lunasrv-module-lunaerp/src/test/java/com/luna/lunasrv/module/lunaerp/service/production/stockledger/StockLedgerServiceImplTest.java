package com.luna.lunasrv.module.lunaerp.service.production.stockledger;

import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.stockledger.StockLedgerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.stockledger.dto.StockBalanceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link StockLedgerServiceImpl} 的单元测试。
 */
class StockLedgerServiceImplTest {

    @Mock
    private StockLedgerMapper stockLedgerMapper;

    @InjectMocks
    private StockLedgerServiceImpl stockLedgerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getBalance_withRecords_returnsSumQuantity() {
        Long yarnSpecId = 1L;
        BigDecimal expectedBalance = new BigDecimal("150.50");
        when(stockLedgerMapper.selectBalanceByYarnSpecId(yarnSpecId)).thenReturn(expectedBalance);

        BigDecimal balance = stockLedgerService.getBalance(yarnSpecId);

        assertEquals(expectedBalance, balance);
        verify(stockLedgerMapper).selectBalanceByYarnSpecId(yarnSpecId);
    }

    @Test
    void getBalance_noRecords_returnsZero() {
        Long yarnSpecId = 2L;
        when(stockLedgerMapper.selectBalanceByYarnSpecId(yarnSpecId)).thenReturn(null);

        BigDecimal balance = stockLedgerService.getBalance(yarnSpecId);

        assertEquals(BigDecimal.ZERO, balance);
        verify(stockLedgerMapper).selectBalanceByYarnSpecId(yarnSpecId);
    }

    @Test
    void getBalances_multipleSpecs_returnsMapWithZeroDefaults() {
        List<Long> specIds = Arrays.asList(1L, 2L, 3L);
        List<StockBalanceDTO> dtoList = Arrays.asList(
                new StockBalanceDTO(1L, new BigDecimal("100.00")),
                new StockBalanceDTO(2L, new BigDecimal("250.00"))
        );
        when(stockLedgerMapper.selectBalancesByYarnSpecIds(specIds)).thenReturn(dtoList);

        Map<Long, BigDecimal> balances = stockLedgerService.getBalances(specIds);

        assertNotNull(balances);
        assertEquals(3, balances.size());
        assertEquals(new BigDecimal("100.00"), balances.get(1L));
        assertEquals(new BigDecimal("250.00"), balances.get(2L));
        assertEquals(BigDecimal.ZERO, balances.get(3L));
        verify(stockLedgerMapper).selectBalancesByYarnSpecIds(specIds);
    }

    @Test
    void getBalances_emptyCollection_returnsEmptyMapWithoutQueryingMapper() {
        Map<Long, BigDecimal> balances = stockLedgerService.getBalances(Collections.emptyList());

        assertNotNull(balances);
        assertTrue(balances.isEmpty());
        verify(stockLedgerMapper, never()).selectBalancesByYarnSpecIds(any());
    }

    @Test
    void writeStockIn_validParams_insertsStockLedgerDO() {
        Long yarnSpecId = 10L;
        BigDecimal quantity = new BigDecimal("500.00");
        String relatedDocType = "YARN_ORDER";
        Long relatedDocId = 100L;
        String batchNo = "BATCH20260920001";
        String remark = "采购入库";

        stockLedgerService.writeStockIn(yarnSpecId, quantity, relatedDocType, relatedDocId, batchNo, remark);

        ArgumentCaptor<StockLedgerDO> captor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper).insert(captor.capture());

        StockLedgerDO inserted = captor.getValue();
        assertNotNull(inserted);
        assertEquals(yarnSpecId, inserted.getYarnSpecId());
        assertEquals("STOCK_IN", inserted.getType());
        assertEquals(quantity, inserted.getQuantity());
        assertEquals(relatedDocType, inserted.getRelatedDocType());
        assertEquals(relatedDocId, inserted.getRelatedDocId());
        assertEquals(batchNo, inserted.getBatchNo());
        assertEquals(remark, inserted.getRemark());
    }

    @Test
    void existsByRelatedDoc_whenExists_returnsTrue() {
        when(stockLedgerMapper.selectCountByRelatedDoc("YARN_ORDER", 100L)).thenReturn(1L);

        boolean exists = stockLedgerService.existsByRelatedDoc("YARN_ORDER", 100L);

        assertTrue(exists);
        verify(stockLedgerMapper).selectCountByRelatedDoc("YARN_ORDER", 100L);
    }

    @Test
    void existsByRelatedDoc_whenNotExists_returnsFalse() {
        when(stockLedgerMapper.selectCountByRelatedDoc("YARN_ORDER", 100L)).thenReturn(0L);

        boolean exists = stockLedgerService.existsByRelatedDoc("YARN_ORDER", 100L);

        assertFalse(exists);
        verify(stockLedgerMapper).selectCountByRelatedDoc("YARN_ORDER", 100L);
    }

}
