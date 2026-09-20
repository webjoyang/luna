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
import java.util.List;

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

    @Test
    void updateYarnRecipeBom_success_replacesItems() {
        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setId(100L);
        req.setProductId(1L);
        req.setRemark("更新配比");

        YarnRecipeBomDO existingBom = new YarnRecipeBomDO();
        existingBom.setId(100L);
        existingBom.setProductId(1L);
        when(yarnRecipeBomMapper.selectById(100L)).thenReturn(existingBom);

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P001");
        product.setName("全棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        YarnRecipeBomItemSaveReqVO item = new YarnRecipeBomItemSaveReqVO();
        item.setYarnTypeId(1L);
        item.setYarnSpecId(101L);
        item.setRatio(new BigDecimal("100.00"));
        req.setItems(Collections.singletonList(item));

        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(101L);
        when(yarnSpecMapper.selectById(101L)).thenReturn(spec);

        yarnRecipeBomService.updateYarnRecipeBom(req);

        // 校验主表更新
        verify(yarnRecipeBomMapper).updateById(any(YarnRecipeBomDO.class));
        // 校验明细先删后插
        verify(yarnRecipeBomItemMapper).deleteByRecipeBomId(100L);
        verify(yarnRecipeBomItemMapper, times(1)).insert(any(YarnRecipeBomItemDO.class));
    }

    @Test
    void updateYarnRecipeBom_zeroItems_success() {
        YarnRecipeBomSaveReqVO req = new YarnRecipeBomSaveReqVO();
        req.setId(100L);
        req.setProductId(1L);
        req.setItems(Collections.emptyList());

        YarnRecipeBomDO existingBom = new YarnRecipeBomDO();
        existingBom.setId(100L);
        existingBom.setProductId(1L);
        when(yarnRecipeBomMapper.selectById(100L)).thenReturn(existingBom);

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P001");
        product.setName("全棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        yarnRecipeBomService.updateYarnRecipeBom(req);

        verify(yarnRecipeBomMapper).updateById(any(YarnRecipeBomDO.class));
        verify(yarnRecipeBomItemMapper).deleteByRecipeBomId(100L);
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

}
