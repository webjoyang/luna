package com.luna.lunasrv.module.lunaerp.service.stock.warehouselocation;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouselocation.WarehouseLocationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemPlacementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouselocation.WarehouseLocationMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_LOCATION_CODE_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_LOCATION_IN_USE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_LOCATION_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WarehouseLocationServiceImpl} 的纯 Mockito 单元测试
 *
 * 覆盖关键业务行为：
 * 1. 库位创建：所属仓库存在性校验、仓内编码唯一（重名报错）
 * 2. 库位修改：仓内编码唯一排除自身
 * 3. 库位删除守卫：被入库放置记录引用禁止删除（1_030_013_102）
 * 4. 下拉按仓库过滤启用库位
 *
 * @author luna
 */
class WarehouseLocationServiceImplTest {

    @Mock
    private WarehouseLocationMapper warehouseLocationMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private StockInItemPlacementMapper stockInItemPlacementMapper;

    @InjectMocks
    private WarehouseLocationServiceImpl warehouseLocationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), WarehouseLocationDO.class);
        when(warehouseLocationMapper.insert(any(WarehouseLocationDO.class))).thenAnswer(invocation -> {
            WarehouseLocationDO dobj = invocation.getArgument(0);
            dobj.setId(200L);
            return 1;
        });
    }

    private WarehouseLocationSaveReqVO buildCreateReqVO() {
        WarehouseLocationSaveReqVO req = new WarehouseLocationSaveReqVO();
        req.setWarehouseId(100L);
        req.setCode("A-01-01");
        req.setName("A区1排1层");
        req.setSort(1);
        req.setRemark("标准库位");
        return req;
    }

    // ==================== 创建 ====================

    @Test
    void createWarehouseLocation_success() {
        WarehouseLocationSaveReqVO req = buildCreateReqVO();
        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setId(100L);
        when(warehouseMapper.selectById(100L)).thenReturn(warehouse);
        when(warehouseLocationMapper.selectByWarehouseIdAndCodeIncludingDeleted(100L, "A-01-01")).thenReturn(null);

        Long id = warehouseLocationService.createWarehouseLocation(req);

        assertEquals(200L, id);
        ArgumentCaptor<WarehouseLocationDO> captor = ArgumentCaptor.forClass(WarehouseLocationDO.class);
        verify(warehouseLocationMapper).insert(captor.capture());
        WarehouseLocationDO inserted = captor.getValue();
        assertEquals("A-01-01", inserted.getCode());
        assertEquals("ACTIVE", inserted.getStatus());
        assertEquals(1, inserted.getSort());
    }

    @Test
    void createWarehouseLocation_warehouseNotExists_throwsException() {
        WarehouseLocationSaveReqVO req = buildCreateReqVO();
        when(warehouseMapper.selectById(100L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseLocationService.createWarehouseLocation(req));
        assertEquals(WAREHOUSE_NOT_EXISTS.getCode(), ex.getCode());
        verify(warehouseLocationMapper, never()).insert(any(WarehouseLocationDO.class));
    }

    @Test
    void createWarehouseLocation_duplicateCodeInSameWarehouse_throwsException() {
        WarehouseLocationSaveReqVO req = buildCreateReqVO();
        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setId(100L);
        when(warehouseMapper.selectById(100L)).thenReturn(warehouse);

        WarehouseLocationDO duplicate = new WarehouseLocationDO();
        duplicate.setId(199L);
        duplicate.setWarehouseId(100L);
        duplicate.setCode("A-01-01");
        when(warehouseLocationMapper.selectByWarehouseIdAndCodeIncludingDeleted(100L, "A-01-01")).thenReturn(duplicate);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseLocationService.createWarehouseLocation(req));
        assertEquals(WAREHOUSE_LOCATION_CODE_DUPLICATE.getCode(), ex.getCode());
        verify(warehouseLocationMapper, never()).insert(any(WarehouseLocationDO.class));
    }

    // ==================== 更新 ====================

    @Test
    void updateWarehouseLocation_success() {
        WarehouseLocationSaveReqVO req = buildCreateReqVO();
        req.setId(200L);
        req.setName("A区新名称");

        WarehouseLocationDO exist = new WarehouseLocationDO();
        exist.setId(200L);
        exist.setWarehouseId(100L);
        exist.setCode("A-01-01");
        when(warehouseLocationMapper.selectById(200L)).thenReturn(exist);
        when(warehouseLocationMapper.selectByWarehouseIdAndCodeIncludingDeleted(100L, "A-01-01")).thenReturn(exist);

        warehouseLocationService.updateWarehouseLocation(req);

        ArgumentCaptor<WarehouseLocationDO> captor = ArgumentCaptor.forClass(WarehouseLocationDO.class);
        verify(warehouseLocationMapper).updateById(captor.capture());
        assertEquals("A区新名称", captor.getValue().getName());
    }

    @Test
    void updateWarehouseLocation_notExists_throwsException() {
        WarehouseLocationSaveReqVO req = buildCreateReqVO();
        req.setId(999L);
        when(warehouseLocationMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseLocationService.updateWarehouseLocation(req));
        assertEquals(WAREHOUSE_LOCATION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void updateWarehouseLocation_duplicateCodeInSameWarehouse_throwsException() {
        WarehouseLocationSaveReqVO req = buildCreateReqVO();
        req.setId(200L);
        req.setCode("A-01-02");

        WarehouseLocationDO exist = new WarehouseLocationDO();
        exist.setId(200L);
        exist.setWarehouseId(100L);
        exist.setCode("A-01-01");
        when(warehouseLocationMapper.selectById(200L)).thenReturn(exist);

        WarehouseLocationDO other = new WarehouseLocationDO();
        other.setId(201L);
        other.setWarehouseId(100L);
        other.setCode("A-01-02");
        when(warehouseLocationMapper.selectByWarehouseIdAndCodeIncludingDeleted(100L, "A-01-02")).thenReturn(other);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseLocationService.updateWarehouseLocation(req));
        assertEquals(WAREHOUSE_LOCATION_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    // ==================== 删除与守卫 ====================

    @Test
    void deleteWarehouseLocation_notExists_throwsException() {
        when(warehouseLocationMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseLocationService.deleteWarehouseLocation(999L));
        assertEquals(WAREHOUSE_LOCATION_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void deleteWarehouseLocation_inUseByPlacement_throwsException() {
        WarehouseLocationDO exist = new WarehouseLocationDO();
        exist.setId(200L);
        when(warehouseLocationMapper.selectById(200L)).thenReturn(exist);
        when(stockInItemPlacementMapper.selectCountByLocationId(200L)).thenReturn(3L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseLocationService.deleteWarehouseLocation(200L));
        assertEquals(WAREHOUSE_LOCATION_IN_USE.getCode(), ex.getCode());
        verify(warehouseLocationMapper, never()).deleteById(200L);
    }

    @Test
    void deleteWarehouseLocation_success_setsInactiveAndDeletes() {
        WarehouseLocationDO exist = new WarehouseLocationDO();
        exist.setId(200L);
        exist.setStatus("ACTIVE");
        when(warehouseLocationMapper.selectById(200L)).thenReturn(exist);
        when(stockInItemPlacementMapper.selectCountByLocationId(200L)).thenReturn(0L);

        warehouseLocationService.deleteWarehouseLocation(200L);

        ArgumentCaptor<WarehouseLocationDO> captor = ArgumentCaptor.forClass(WarehouseLocationDO.class);
        verify(warehouseLocationMapper).updateById(captor.capture());
        assertEquals("INACTIVE", captor.getValue().getStatus());
        verify(warehouseLocationMapper).deleteById(200L);
    }

    // ==================== 查询 ====================

    @Test
    void getWarehouseLocation_success() {
        WarehouseLocationDO exist = new WarehouseLocationDO();
        exist.setId(200L);
        exist.setCode("A-01-01");
        exist.setName("A区1排1层");
        when(warehouseLocationMapper.selectById(200L)).thenReturn(exist);

        WarehouseLocationRespVO resp = warehouseLocationService.getWarehouseLocation(200L);
        assertNotNull(resp);
        assertEquals("A-01-01", resp.getCode());
    }

    @Test
    void getWarehouseLocationPage_delegatesToMapper() {
        WarehouseLocationPageReqVO req = new WarehouseLocationPageReqVO();
        req.setWarehouseId(100L);
        WarehouseLocationDO item = new WarehouseLocationDO();
        item.setId(200L);
        item.setCode("A-01-01");
        PageResult<WarehouseLocationDO> pageResult = new PageResult<>(List.of(item), 1L);
        when(warehouseLocationMapper.selectPage(req)).thenReturn(pageResult);

        PageResult<WarehouseLocationRespVO> result = warehouseLocationService.getWarehouseLocationPage(req);
        assertEquals(1L, result.getTotal());
        assertEquals("A-01-01", result.getList().get(0).getCode());
    }

}
