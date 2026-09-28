package com.luna.lunasrv.module.lunaerp.service.production.yarnrecipebom;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link YarnRecipeBomServiceImpl} 的单元测试。
 */
class YarnRecipeBomServiceImplTest {

    @Mock
    private YarnRecipeBomMapper yarnRecipeBomMapper;

    @Mock
    private YarnRecipeBomItemMapper yarnRecipeBomItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @InjectMocks
    private YarnRecipeBomServiceImpl yarnRecipeBomService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(yarnRecipeBomMapper.insert(any(YarnRecipeBomDO.class))).thenAnswer(invocation -> {
            YarnRecipeBomDO dobj = invocation.getArgument(0);
            dobj.setId(100L);
            return 1;
        });
    }

    @Test
    void createYarnRecipeBom_productNotExists_throws1030001000() {
        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setProductId(999L);

        when(productMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnRecipeBomService.createYarnRecipeBom(req));
        assertEquals(1_030_001_000, ex.getCode());
        verify(yarnRecipeBomMapper, never()).insert(any(YarnRecipeBomDO.class));
    }

    @Test
    void createYarnRecipeBom_productBomAlreadyExists_throws1030008007() {
        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setProductId(1L);

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P001");
        product.setName("全棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        YarnRecipeBomDO existing = new YarnRecipeBomDO();
        existing.setId(50L);
        existing.setProductId(1L);
        when(yarnRecipeBomMapper.selectByProductIdIncludingDeleted(1L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnRecipeBomService.createYarnRecipeBom(req));
        assertEquals(1_030_008_007, ex.getCode());
        verify(yarnRecipeBomMapper, never()).insert(any(YarnRecipeBomDO.class));
    }

    @Test
    void createYarnRecipeBom_specNotExists_throws1030008002() {
        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setProductId(1L);

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P001");
        product.setName("全棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);
        when(yarnRecipeBomMapper.selectByProductIdIncludingDeleted(1L)).thenReturn(null);

        YarnRecipeBomItemSaveReqVO item = new YarnRecipeBomItemSaveReqVO();
        item.setYarnTypeId(10L);
        item.setYarnSpecId(999L);
        item.setRatio(new BigDecimal("100.00"));
        req.setItems(Collections.singletonList(item));

        when(yarnSpecMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnRecipeBomService.createYarnRecipeBom(req));
        assertEquals(1_030_008_002, ex.getCode());
        verify(yarnRecipeBomMapper, never()).insert(any(YarnRecipeBomDO.class));
    }

    @Test
    void createYarnRecipeBom_success_savesBomAndItems() {
        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setProductId(1L);
        req.setRemark("标准配比");

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P001");
        product.setName("全棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);
        when(yarnRecipeBomMapper.selectByProductIdIncludingDeleted(1L)).thenReturn(null);

        YarnRecipeBomItemSaveReqVO item1 = new YarnRecipeBomItemSaveReqVO();
        item1.setYarnTypeId(1L);
        item1.setYarnSpecId(101L);
        item1.setRatio(new BigDecimal("60.00"));
        item1.setSpinMethod("环锭纺");
        item1.setRefPrice(new BigDecimal("30.0000"));

        YarnRecipeBomItemSaveReqVO item2 = new YarnRecipeBomItemSaveReqVO();
        item2.setYarnTypeId(2L);
        item2.setYarnSpecId(102L);
        item2.setRatio(new BigDecimal("40.00"));

        req.setItems(Arrays.asList(item1, item2));

        YarnSpecDO spec1 = new YarnSpecDO();
        spec1.setId(101L);
        when(yarnSpecMapper.selectById(101L)).thenReturn(spec1);

        YarnSpecDO spec2 = new YarnSpecDO();
        spec2.setId(102L);
        spec2.setRefPrice(new BigDecimal("25.0000"));
        spec2.setSpinMethod("气流纺");
        when(yarnSpecMapper.selectById(102L)).thenReturn(spec2);

        Long createdId = yarnRecipeBomService.createYarnRecipeBom(req);
        assertEquals(100L, createdId);

        // 校验主表插入
        ArgumentCaptor<YarnRecipeBomDO> bomCaptor = ArgumentCaptor.forClass(YarnRecipeBomDO.class);
        verify(yarnRecipeBomMapper).insert(bomCaptor.capture());
        YarnRecipeBomDO savedBom = bomCaptor.getValue();
        assertEquals(1L, savedBom.getProductId());
        assertEquals("P001", savedBom.getProductCode());
        assertEquals("全棉汗布", savedBom.getProductName());
        assertEquals("ACTIVE", savedBom.getStatus());
        assertEquals("标准配比", savedBom.getRemark());

        // 校验明细插入 2 行
        ArgumentCaptor<YarnRecipeBomItemDO> itemCaptor = ArgumentCaptor.forClass(YarnRecipeBomItemDO.class);
        verify(yarnRecipeBomItemMapper, times(2)).insert(itemCaptor.capture());
        List<YarnRecipeBomItemDO> savedItems = itemCaptor.getAllValues();
        assertEquals(2, savedItems.size());
        assertEquals(100L, savedItems.get(0).getRecipeBomId());
        assertEquals(new BigDecimal("60.00"), savedItems.get(0).getRatio());
        assertEquals(100L, savedItems.get(1).getRecipeBomId());
        assertEquals(new BigDecimal("40.00"), savedItems.get(1).getRatio());
        // item2 兜底回填规格默认值
        assertEquals("气流纺", savedItems.get(1).getSpinMethod());
        assertEquals(new BigDecimal("25.0000"), savedItems.get(1).getRefPrice());
    }

    @Test
    void updateYarnRecipeBom_notExists_throws1030008004() {
        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setId(999L);
        req.setProductId(1L);

        when(yarnRecipeBomMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnRecipeBomService.updateYarnRecipeBom(req));
        assertEquals(1_030_008_004, ex.getCode());
        verify(yarnRecipeBomMapper, never()).updateById(any(YarnRecipeBomDO.class));
    }

    // ==================== 纱线配比BOM明细差异保存（ADR-0032） ====================

    @Test
    void updateYarnRecipeBom_allExistingItems_updatesItemsZeroDeleteZeroInsert() {
        YarnRecipeBomDO existingBom = stubBomDO(100L, 1L);
        when(yarnRecipeBomMapper.selectById(100L)).thenReturn(existingBom);
        stubProduct(1L, "P001", "全棉汗布");

        YarnRecipeBomItemDO item1 = stubItemDO(201L, 100L, 1L, 101L, "60.00", "环锭纺", "经纱", "30.0000");
        YarnRecipeBomItemDO item2 = stubItemDO(202L, 100L, 2L, 102L, "40.00", "气流纺", "纬纱", "25.0000");
        when(yarnRecipeBomItemMapper.selectListByRecipeBomId(100L)).thenReturn(Arrays.asList(item1, item2));

        stubSpec(101L, "环锭纺", "30.0000");
        stubSpec(102L, "气流纺", "25.0000");

        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setId(100L);
        req.setProductId(1L);
        req.setItems(Arrays.asList(
                itemReq(201L, 1L, 101L, "60.00", "环锭纺", "经纱", "30.0000"),
                itemReq(202L, 2L, 102L, "40.00", "气流纺", "纬纱", "25.0000")
        ));

        yarnRecipeBomService.updateYarnRecipeBom(req);

        verify(yarnRecipeBomMapper).updateById(any(YarnRecipeBomDO.class));
        verify(yarnRecipeBomItemMapper, never()).deleteByIds(any());
        verify(yarnRecipeBomItemMapper, never()).deleteByRecipeBomId(any());
        verify(yarnRecipeBomItemMapper, never()).insert(any(YarnRecipeBomItemDO.class));

        ArgumentCaptor<YarnRecipeBomItemDO> captor = ArgumentCaptor.forClass(YarnRecipeBomItemDO.class);
        verify(yarnRecipeBomItemMapper, times(2)).updateById(captor.capture());
        List<YarnRecipeBomItemDO> updated = captor.getAllValues();
        assertEquals(201L, updated.get(0).getId());
        assertEquals(new BigDecimal("60.00"), updated.get(0).getRatio());
        assertEquals(202L, updated.get(1).getId());
        assertEquals(new BigDecimal("40.00"), updated.get(1).getRatio());
    }

    @Test
    void updateYarnRecipeBom_modifyItem_updatesTargetItemWithNewValues() {
        YarnRecipeBomDO existingBom = stubBomDO(100L, 1L);
        when(yarnRecipeBomMapper.selectById(100L)).thenReturn(existingBom);
        stubProduct(1L, "P001", "全棉汗布");

        YarnRecipeBomItemDO item1 = stubItemDO(201L, 100L, 1L, 101L, "60.00", "环锭纺", "经纱", "30.0000");
        YarnRecipeBomItemDO item2 = stubItemDO(202L, 100L, 2L, 102L, "40.00", "气流纺", "纬纱", "25.0000");
        when(yarnRecipeBomItemMapper.selectListByRecipeBomId(100L)).thenReturn(Arrays.asList(item1, item2));

        stubSpec(101L, "环锭纺", "30.0000");
        stubSpec(102L, "气流纺", "25.0000");

        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setId(100L);
        req.setProductId(1L);
        // 修改 item1 的配比与工艺与备注，item2 维持原样
        req.setItems(Arrays.asList(
                itemReq(201L, 1L, 101L, "70.00", "紧密纺", "改备注", "32.0000"),
                itemReq(202L, 2L, 102L, "30.00", "气流纺", "纬纱", "25.0000")
        ));

        yarnRecipeBomService.updateYarnRecipeBom(req);

        verify(yarnRecipeBomItemMapper, never()).deleteByIds(any());
        verify(yarnRecipeBomItemMapper, never()).insert(any(YarnRecipeBomItemDO.class));

        ArgumentCaptor<YarnRecipeBomItemDO> captor = ArgumentCaptor.forClass(YarnRecipeBomItemDO.class);
        verify(yarnRecipeBomItemMapper, times(2)).updateById(captor.capture());
        YarnRecipeBomItemDO updatedItem1 = captor.getAllValues().stream()
                .filter(i -> i.getId().equals(201L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("70.00"), updatedItem1.getRatio());
        assertEquals("紧密纺", updatedItem1.getSpinMethod());
        assertEquals("改备注", updatedItem1.getRemark());
        assertEquals(new BigDecimal("32.0000"), updatedItem1.getRefPrice());
    }

    @Test
    void updateYarnRecipeBom_removeItem_logicallyDeletesRemovedItem() {
        YarnRecipeBomDO existingBom = stubBomDO(100L, 1L);
        when(yarnRecipeBomMapper.selectById(100L)).thenReturn(existingBom);
        stubProduct(1L, "P001", "全棉汗布");

        YarnRecipeBomItemDO item1 = stubItemDO(201L, 100L, 1L, 101L, "60.00", "环锭纺", "经纱", "30.0000");
        YarnRecipeBomItemDO item2 = stubItemDO(202L, 100L, 2L, 102L, "40.00", "气流纺", "纬纱", "25.0000");
        when(yarnRecipeBomItemMapper.selectListByRecipeBomId(100L)).thenReturn(Arrays.asList(item1, item2));

        stubSpec(101L, "环锭纺", "30.0000");

        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setId(100L);
        req.setProductId(1L);
        // 仅提交 201，删除 202
        req.setItems(Collections.singletonList(
                itemReq(201L, 1L, 101L, "60.00", "环锭纺", "经纱", "30.0000")
        ));

        yarnRecipeBomService.updateYarnRecipeBom(req);

        verify(yarnRecipeBomItemMapper).deleteByIds(eq(Collections.singleton(202L)));
        verify(yarnRecipeBomItemMapper, times(1)).updateById(any(YarnRecipeBomItemDO.class));
        verify(yarnRecipeBomItemMapper, never()).insert(any(YarnRecipeBomItemDO.class));
    }

    @Test
    void updateYarnRecipeBom_addItem_insertsNewItemAndPreservesExisting() {
        YarnRecipeBomDO existingBom = stubBomDO(100L, 1L);
        when(yarnRecipeBomMapper.selectById(100L)).thenReturn(existingBom);
        stubProduct(1L, "P001", "全棉汗布");

        YarnRecipeBomItemDO item1 = stubItemDO(201L, 100L, 1L, 101L, "60.00", "环锭纺", "经纱", "30.0000");
        when(yarnRecipeBomItemMapper.selectListByRecipeBomId(100L)).thenReturn(Collections.singletonList(item1));

        stubSpec(101L, "环锭纺", "30.0000");
        stubSpec(102L, "气流纺", "25.0000");

        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setId(100L);
        req.setProductId(1L);
        // 提交已存 201 + 新增行（id=null）
        req.setItems(Arrays.asList(
                itemReq(201L, 1L, 101L, "60.00", "环锭纺", "经纱", "30.0000"),
                itemReq(null, 2L, 102L, "40.00", "气流纺", "新加纱线", "25.0000")
        ));

        yarnRecipeBomService.updateYarnRecipeBom(req);

        verify(yarnRecipeBomItemMapper, never()).deleteByIds(any());
        verify(yarnRecipeBomItemMapper, times(1)).updateById(any(YarnRecipeBomItemDO.class));

        ArgumentCaptor<YarnRecipeBomItemDO> insertCaptor = ArgumentCaptor.forClass(YarnRecipeBomItemDO.class);
        verify(yarnRecipeBomItemMapper, times(1)).insert(insertCaptor.capture());
        YarnRecipeBomItemDO inserted = insertCaptor.getValue();
        assertEquals(100L, inserted.getRecipeBomId());
        assertEquals(2L, inserted.getYarnTypeId());
        assertEquals(102L, inserted.getYarnSpecId());
        assertEquals(new BigDecimal("40.00"), inserted.getRatio());
    }

    @Test
    void updateYarnRecipeBom_itemNotBelongingToBom_throwsInvalidParamException() {
        YarnRecipeBomDO existingBom = stubBomDO(100L, 1L);
        when(yarnRecipeBomMapper.selectById(100L)).thenReturn(existingBom);
        stubProduct(1L, "P001", "全棉汗布");

        YarnRecipeBomItemDO item1 = stubItemDO(201L, 100L, 1L, 101L, "60.00", "环锭纺", "经纱", "30.0000");
        when(yarnRecipeBomItemMapper.selectListByRecipeBomId(100L)).thenReturn(Collections.singletonList(item1));

        stubSpec(101L, "环锭纺", "30.0000");

        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setId(100L);
        req.setProductId(1L);
        // 携带不属于本 BOM 的 id=999
        req.setItems(Collections.singletonList(
                itemReq(999L, 1L, 101L, "100.00", "环锭纺", "跨父行", "30.0000")
        ));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnRecipeBomService.updateYarnRecipeBom(req));
        assertEquals(400, ex.getCode());

        verify(yarnRecipeBomItemMapper, never()).updateById(any(YarnRecipeBomItemDO.class));
        verify(yarnRecipeBomItemMapper, never()).deleteByIds(any());
        verify(yarnRecipeBomItemMapper, never()).insert(any(YarnRecipeBomItemDO.class));
    }

    @Test
    void updateYarnRecipeBom_duplicateItemIds_throwsInvalidParamException() {
        YarnRecipeBomDO existingBom = stubBomDO(100L, 1L);
        when(yarnRecipeBomMapper.selectById(100L)).thenReturn(existingBom);
        stubProduct(1L, "P001", "全棉汗布");

        YarnRecipeBomItemDO item1 = stubItemDO(201L, 100L, 1L, 101L, "60.00", "环锭纺", "经纱", "30.0000");
        when(yarnRecipeBomItemMapper.selectListByRecipeBomId(100L)).thenReturn(Collections.singletonList(item1));

        stubSpec(101L, "环锭纺", "30.0000");

        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setId(100L);
        req.setProductId(1L);
        // 提交两个重复 id=201
        req.setItems(Arrays.asList(
                itemReq(201L, 1L, 101L, "50.00", "环锭纺", "重复1", "30.0000"),
                itemReq(201L, 1L, 101L, "50.00", "环锭纺", "重复2", "30.0000")
        ));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnRecipeBomService.updateYarnRecipeBom(req));
        assertEquals(400, ex.getCode());

        verify(yarnRecipeBomItemMapper, never()).updateById(any(YarnRecipeBomItemDO.class));
        verify(yarnRecipeBomItemMapper, never()).deleteByIds(any());
        verify(yarnRecipeBomItemMapper, never()).insert(any(YarnRecipeBomItemDO.class));
    }

    @Test
    void updateYarnRecipeBom_toEmptyItems_deletesAllOldItems() {
        YarnRecipeBomDO existingBom = stubBomDO(100L, 1L);
        when(yarnRecipeBomMapper.selectById(100L)).thenReturn(existingBom);
        stubProduct(1L, "P001", "全棉汗布");

        YarnRecipeBomItemDO item1 = stubItemDO(201L, 100L, 1L, 101L, "60.00", "环锭纺", "经纱", "30.0000");
        YarnRecipeBomItemDO item2 = stubItemDO(202L, 100L, 2L, 102L, "40.00", "气流纺", "纬纱", "25.0000");
        when(yarnRecipeBomItemMapper.selectListByRecipeBomId(100L)).thenReturn(Arrays.asList(item1, item2));

        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setId(100L);
        req.setProductId(1L);
        req.setItems(Collections.emptyList());

        yarnRecipeBomService.updateYarnRecipeBom(req);

        verify(yarnRecipeBomMapper).updateById(any(YarnRecipeBomDO.class));
        verify(yarnRecipeBomItemMapper).deleteByIds(eq(new HashSet<>(Arrays.asList(201L, 202L))));
        verify(yarnRecipeBomItemMapper, never()).updateById(any(YarnRecipeBomItemDO.class));
        verify(yarnRecipeBomItemMapper, never()).insert(any(YarnRecipeBomItemDO.class));
    }

    @Test
    void deleteYarnRecipeBom_success_cascadesItems() {
        YarnRecipeBomDO existingBom = new YarnRecipeBomDO();
        existingBom.setId(100L);
        when(yarnRecipeBomMapper.selectById(100L)).thenReturn(existingBom);

        yarnRecipeBomService.deleteYarnRecipeBom(100L);

        // 置 INACTIVE + 逻辑删除
        verify(yarnRecipeBomMapper).updateById(any(YarnRecipeBomDO.class));
        verify(yarnRecipeBomMapper).deleteById(100L);
        // 级联逻辑删除明细
        verify(yarnRecipeBomItemMapper).deleteByRecipeBomId(100L);
    }

    @Test
    void getYarnRecipeBom_success() {
        YarnRecipeBomDO bom = new YarnRecipeBomDO();
        bom.setId(100L);
        bom.setProductId(1L);
        bom.setProductCode("P001");
        bom.setProductName("全棉汗布");
        bom.setStatus("ACTIVE");
        when(yarnRecipeBomMapper.selectById(100L)).thenReturn(bom);

        YarnRecipeBomItemDO item = new YarnRecipeBomItemDO();
        item.setId(201L);
        item.setRecipeBomId(100L);
        item.setYarnTypeId(1L);
        item.setYarnSpecId(10L);
        item.setRatio(new BigDecimal("100.00"));
        when(yarnRecipeBomItemMapper.selectListByRecipeBomId(100L)).thenReturn(Collections.singletonList(item));

        YarnTypeDO yarnType = new YarnTypeDO();
        yarnType.setId(1L);
        yarnType.setName("棉");
        when(yarnTypeMapper.selectByIds(any())).thenReturn(Collections.singletonList(yarnType));

        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(10L);
        yarnSpec.setSpecName("32S 棉");
        when(yarnSpecMapper.selectByIds(any())).thenReturn(Collections.singletonList(yarnSpec));

        YarnRecipeBomRespVO resp = yarnRecipeBomService.getYarnRecipeBom(100L);
        assertNotNull(resp);
        assertEquals(100L, resp.getId());
        assertEquals("P001", resp.getProductCode());
        assertEquals(1, resp.getItems().size());
        assertEquals("棉", resp.getItems().get(0).getYarnTypeName());
        assertEquals("32S 棉", resp.getItems().get(0).getYarnSpecName());
        assertEquals(1, resp.getItemCount());
    }

    @Test
    void getYarnRecipeBomByProductId_success() {
        YarnRecipeBomDO bom = new YarnRecipeBomDO();
        bom.setId(100L);
        bom.setProductId(1L);
        bom.setProductCode("P001");
        bom.setProductName("全棉汗布");
        when(yarnRecipeBomMapper.selectByProductId(1L)).thenReturn(bom);

        YarnRecipeBomItemDO item = new YarnRecipeBomItemDO();
        item.setId(201L);
        item.setRecipeBomId(100L);
        item.setYarnTypeId(1L);
        item.setYarnSpecId(10L);
        item.setRatio(new BigDecimal("100.00"));
        when(yarnRecipeBomItemMapper.selectListByRecipeBomId(100L)).thenReturn(Collections.singletonList(item));

        YarnRecipeBomRespVO resp = yarnRecipeBomService.getYarnRecipeBomByProductId(1L);
        assertNotNull(resp);
        assertEquals(100L, resp.getId());
        assertEquals(1L, resp.getProductId());
        assertEquals(1, resp.getItems().size());
    }

    @Test
    void getYarnRecipeBomPage_success() {
        YarnRecipeBomPageReqVO req = new YarnRecipeBomPageReqVO();
        req.setProductCode("P001");

        YarnRecipeBomDO bom = new YarnRecipeBomDO();
        bom.setId(100L);
        bom.setProductId(1L);
        bom.setProductCode("P001");
        bom.setProductName("全棉汗布");

        PageResult<YarnRecipeBomDO> pageResult = new PageResult<>(Collections.singletonList(bom), 1L);
        when(yarnRecipeBomMapper.selectPage(req)).thenReturn(pageResult);

        YarnRecipeBomItemDO item1 = new YarnRecipeBomItemDO();
        item1.setId(1L);
        item1.setRecipeBomId(100L);
        YarnRecipeBomItemDO item2 = new YarnRecipeBomItemDO();
        item2.setId(2L);
        item2.setRecipeBomId(100L);
        when(yarnRecipeBomItemMapper.selectListByRecipeBomIds(any())).thenReturn(Arrays.asList(item1, item2));

        PageResult<YarnRecipeBomRespVO> respPage = yarnRecipeBomService.getYarnRecipeBomPage(req);
        assertEquals(1, respPage.getList().size());
        assertEquals(2, respPage.getList().get(0).getItemCount());
    }

    // ==================== 辅助方法 ====================

    private YarnRecipeBomDO stubBomDO(Long id, Long productId) {
        YarnRecipeBomDO bom = new YarnRecipeBomDO();
        bom.setId(id);
        bom.setProductId(productId);
        bom.setProductCode("P001");
        bom.setProductName("全棉汗布");
        bom.setStatus("ACTIVE");
        return bom;
    }

    private ProductDO stubProduct(Long productId, String code, String name) {
        ProductDO product = new ProductDO();
        product.setId(productId);
        product.setCode(code);
        product.setName(name);
        when(productMapper.selectById(productId)).thenReturn(product);
        return product;
    }

    private YarnSpecDO stubSpec(Long specId, String spinMethod, String refPrice) {
        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(specId);
        spec.setSpinMethod(spinMethod);
        if (refPrice != null) {
            spec.setRefPrice(new BigDecimal(refPrice));
        }
        when(yarnSpecMapper.selectById(specId)).thenReturn(spec);
        return spec;
    }

    private YarnRecipeBomItemDO stubItemDO(Long id, Long bomId, Long typeId, Long specId, String ratio, String spinMethod, String remark, String refPrice) {
        YarnRecipeBomItemDO item = new YarnRecipeBomItemDO();
        item.setId(id);
        item.setRecipeBomId(bomId);
        item.setYarnTypeId(typeId);
        item.setYarnSpecId(specId);
        item.setRatio(new BigDecimal(ratio));
        item.setSpinMethod(spinMethod);
        item.setRemark(remark);
        item.setRefPrice(refPrice != null ? new BigDecimal(refPrice) : null);
        return item;
    }

    private YarnRecipeBomItemSaveReqVO itemReq(Long id, Long typeId, Long specId, String ratio, String spinMethod, String remark, String refPrice) {
        YarnRecipeBomItemSaveReqVO req = new YarnRecipeBomItemSaveReqVO();
        req.setId(id);
        req.setYarnTypeId(typeId);
        req.setYarnSpecId(specId);
        req.setRatio(new BigDecimal(ratio));
        req.setSpinMethod(spinMethod);
        req.setRemark(remark);
        req.setRefPrice(refPrice != null ? new BigDecimal(refPrice) : null);
        return req;
    }

}
