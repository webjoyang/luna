package com.luna.lunasrv.module.lunaerp.service.stock.warehouse;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehousePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehouseRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehouseSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouselocation.WarehouseLocationMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_HAS_LOCATION;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_NO_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_REFERENCED_BY_STOCK_IN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WarehouseServiceImpl} 的纯 Mockito 单元测试
 *
 * 覆盖关键业务行为：
 * 1. 编号自动取号（WH+年份+4位流水）、查重与重试
 * 2. 手填编号查重（含已删除行，撞号报错）
 * 3. 详情与修改
 * 4. 删除守卫：存在库位禁止删除（1_030_013_002）
 * 5. 删除守卫：被入库单引用禁止删除（1_030_013_003）
 * 6. 下拉与预览下一个编号
 *
 * @author luna
 */
class WarehouseServiceImplTest {

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private WarehouseLocationMapper warehouseLocationMapper;

    @Mock
    private StockInMapper stockInMapper;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    private static final String YEAR_PREFIX = "WH" + LocalDate.now().getYear();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), WarehouseDO.class);
        when(warehouseMapper.insert(any(WarehouseDO.class))).thenAnswer(invocation -> {
            WarehouseDO dobj = invocation.getArgument(0);
            dobj.setId(100L);
            return 1;
        });
    }

    private WarehouseSaveReqVO buildCreateReqVO() {
        WarehouseSaveReqVO req = new WarehouseSaveReqVO();
        req.setName("柯桥成品一号仓");
        req.setAddress("浙江省绍兴市柯桥区仓库路1号");
        req.setRemark("主仓库");
        return req;
    }

    // ==================== 创建与取号 ====================

    @Test
    void createWarehouse_noWarehouseNo_generatesWHYearSeq() {
        WarehouseSaveReqVO req = buildCreateReqVO();
        req.setWarehouseNo(null);
        when(warehouseMapper.selectListByWarehouseNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(warehouseMapper.selectByWarehouseNoIncludingDeleted(anyString())).thenReturn(null);

        Long id = warehouseService.createWarehouse(req);

        assertEquals(100L, id);
        ArgumentCaptor<WarehouseDO> captor = ArgumentCaptor.forClass(WarehouseDO.class);
        verify(warehouseMapper).insert(captor.capture());
        WarehouseDO inserted = captor.getValue();
        assertEquals(YEAR_PREFIX + "0001", inserted.getWarehouseNo());
        assertEquals("ACTIVE", inserted.getStatus());
        assertEquals("柯桥成品一号仓", inserted.getName());
    }

    @Test
    void createWarehouse_withManualWarehouseNo_success() {
        WarehouseSaveReqVO req = buildCreateReqVO();
        req.setWarehouseNo("WH20268888");
        when(warehouseMapper.selectByWarehouseNoIncludingDeleted("WH20268888")).thenReturn(null);

        Long id = warehouseService.createWarehouse(req);

        assertEquals(100L, id);
        ArgumentCaptor<WarehouseDO> captor = ArgumentCaptor.forClass(WarehouseDO.class);
        verify(warehouseMapper).insert(captor.capture());
        assertEquals("WH20268888", captor.getValue().getWarehouseNo());
    }

    @Test
    void createWarehouse_duplicateWarehouseNo_throwsException() {
        WarehouseSaveReqVO req = buildCreateReqVO();
        req.setWarehouseNo("WH20260001");
        WarehouseDO exist = new WarehouseDO();
        exist.setId(99L);
        exist.setWarehouseNo("WH20260001");
        when(warehouseMapper.selectByWarehouseNoIncludingDeleted("WH20260001")).thenReturn(exist);

        ServiceException ex = assertThrows(ServiceException.class, () -> warehouseService.createWarehouse(req));
        assertEquals(WAREHOUSE_NO_DUPLICATE.getCode(), ex.getCode());
        verify(warehouseMapper, never()).insert(any(WarehouseDO.class));
    }

    @Test
    void createWarehouse_autoGenClash_retriesAndThrowsIfExhausted() {
        WarehouseSaveReqVO req = buildCreateReqVO();
        req.setWarehouseNo(null);
        when(warehouseMapper.selectListByWarehouseNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        WarehouseDO clash = new WarehouseDO();
        clash.setId(88L);
        clash.setWarehouseNo(YEAR_PREFIX + "0001");
        when(warehouseMapper.selectByWarehouseNoIncludingDeleted(anyString())).thenReturn(clash);

        ServiceException ex = assertThrows(ServiceException.class, () -> warehouseService.createWarehouse(req));
        assertEquals(WAREHOUSE_NO_DUPLICATE.getCode(), ex.getCode());
        verify(warehouseMapper, never()).insert(any(WarehouseDO.class));
    }

    // ==================== 更新 ====================

    @Test
    void updateWarehouse_success() {
        WarehouseSaveReqVO req = buildCreateReqVO();
        req.setId(100L);
        req.setWarehouseNo(null);
        req.setName("新仓库名");

        WarehouseDO exist = new WarehouseDO();
        exist.setId(100L);
        exist.setWarehouseNo(YEAR_PREFIX + "0001");
        when(warehouseMapper.selectById(100L)).thenReturn(exist);

        warehouseService.updateWarehouse(req);

        ArgumentCaptor<WarehouseDO> captor = ArgumentCaptor.forClass(WarehouseDO.class);
        verify(warehouseMapper).updateById(captor.capture());
        WarehouseDO updated = captor.getValue();
        assertEquals("新仓库名", updated.getName());
        assertNull(updated.getWarehouseNo());
    }

    @Test
    void updateWarehouse_notExists_throwsException() {
        WarehouseSaveReqVO req = buildCreateReqVO();
        req.setId(999L);
        when(warehouseMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> warehouseService.updateWarehouse(req));
        assertEquals(WAREHOUSE_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void updateWarehouse_duplicateWarehouseNo_throwsException() {
        WarehouseSaveReqVO req = buildCreateReqVO();
        req.setId(100L);
        req.setWarehouseNo("WH20269999");

        WarehouseDO exist = new WarehouseDO();
        exist.setId(100L);
        when(warehouseMapper.selectById(100L)).thenReturn(exist);

        WarehouseDO other = new WarehouseDO();
        other.setId(200L);
        other.setWarehouseNo("WH20269999");
        when(warehouseMapper.selectByWarehouseNoIncludingDeleted("WH20269999")).thenReturn(other);

        ServiceException ex = assertThrows(ServiceException.class, () -> warehouseService.updateWarehouse(req));
        assertEquals(WAREHOUSE_NO_DUPLICATE.getCode(), ex.getCode());
    }

    // ==================== 删除与守卫 ====================

    @Test
    void deleteWarehouse_notExists_throwsException() {
        when(warehouseMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> warehouseService.deleteWarehouse(999L));
        assertEquals(WAREHOUSE_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void deleteWarehouse_hasLocation_throwsException() {
        WarehouseDO exist = new WarehouseDO();
        exist.setId(100L);
        when(warehouseMapper.selectById(100L)).thenReturn(exist);
        when(warehouseLocationMapper.selectCountByWarehouseId(100L)).thenReturn(2L);

        ServiceException ex = assertThrows(ServiceException.class, () -> warehouseService.deleteWarehouse(100L));
        assertEquals(WAREHOUSE_HAS_LOCATION.getCode(), ex.getCode());
        verify(warehouseMapper, never()).deleteById(100L);
    }

    @Test
    void deleteWarehouse_referencedByStockIn_throwsException() {
        WarehouseDO exist = new WarehouseDO();
        exist.setId(100L);
        when(warehouseMapper.selectById(100L)).thenReturn(exist);
        when(warehouseLocationMapper.selectCountByWarehouseId(100L)).thenReturn(0L);
        when(stockInMapper.selectCountByWarehouseId(100L)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> warehouseService.deleteWarehouse(100L));
        assertEquals(WAREHOUSE_REFERENCED_BY_STOCK_IN.getCode(), ex.getCode());
        verify(warehouseMapper, never()).deleteById(100L);
    }

    @Test
    void deleteWarehouse_success_setsInactiveAndDeletes() {
        WarehouseDO exist = new WarehouseDO();
        exist.setId(100L);
        exist.setStatus("ACTIVE");
        when(warehouseMapper.selectById(100L)).thenReturn(exist);
        when(warehouseLocationMapper.selectCountByWarehouseId(100L)).thenReturn(0L);
        when(stockInMapper.selectCountByWarehouseId(100L)).thenReturn(0L);

        warehouseService.deleteWarehouse(100L);

        ArgumentCaptor<WarehouseDO> captor = ArgumentCaptor.forClass(WarehouseDO.class);
        verify(warehouseMapper).updateById(captor.capture());
        assertEquals("INACTIVE", captor.getValue().getStatus());
        verify(warehouseMapper).deleteById(100L);
    }

    // ==================== 查询与预览 ====================

    @Test
    void getWarehouse_success() {
        WarehouseDO exist = new WarehouseDO();
        exist.setId(100L);
        exist.setWarehouseNo(YEAR_PREFIX + "0001");
        exist.setName("成品一号仓");
        when(warehouseMapper.selectById(100L)).thenReturn(exist);

        WarehouseRespVO resp = warehouseService.getWarehouse(100L);
        assertNotNull(resp);
        assertEquals("成品一号仓", resp.getName());
    }

    @Test
    void getWarehousePage_delegatesToMapper() {
        WarehousePageReqVO req = new WarehousePageReqVO();
        req.setSearch("成品");
        WarehouseDO item = new WarehouseDO();
        item.setId(100L);
        item.setName("成品一号仓");
        PageResult<WarehouseDO> pageResult = new PageResult<>(List.of(item), 1L);
        when(warehouseMapper.selectPage(req)).thenReturn(pageResult);

        PageResult<WarehouseRespVO> result = warehouseService.getWarehousePage(req);
        assertEquals(1L, result.getTotal());
        assertEquals("成品一号仓", result.getList().get(0).getName());
    }

    @Test
    void getNextWarehouseNo_returnsPreviewWithoutWriting() {
        when(warehouseMapper.selectListByWarehouseNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());

        String nextNo = warehouseService.getNextWarehouseNo();

        assertEquals(YEAR_PREFIX + "0001", nextNo);
        verify(warehouseMapper, never()).insert(any(WarehouseDO.class));
    }

}
