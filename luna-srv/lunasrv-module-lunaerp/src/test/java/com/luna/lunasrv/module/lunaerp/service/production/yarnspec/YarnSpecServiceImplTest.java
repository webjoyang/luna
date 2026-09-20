package com.luna.lunasrv.module.lunaerp.service.production.yarnspec;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecCascadeOptionVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link YarnSpecServiceImpl} 的单元测试。
 */
class YarnSpecServiceImplTest {

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @InjectMocks
    private YarnSpecServiceImpl yarnSpecService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(yarnSpecMapper.insert(any(YarnSpecDO.class))).thenAnswer(invocation -> {
            YarnSpecDO dobj = invocation.getArgument(0);
            dobj.setId(200L);
            return 1;
        });
    }

    @Test
    void createYarnSpec_yarnTypeNotExists_throws1030008000() {
        YarnSpecSaveReqVO req = new YarnSpecSaveReqVO();
        req.setYarnTypeId(999L);
        req.setSpecName("32S 棉");
        req.setStandardName("32S-COTTON");

        when(yarnTypeMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnSpecService.createYarnSpec(req));
        assertEquals(1_030_008_000, ex.getCode());
        verify(yarnSpecMapper, never()).insert(any(YarnSpecDO.class));
    }

    @Test
    void createYarnSpec_standardNameDuplicate_throws1030008003() {
        YarnSpecSaveReqVO req = new YarnSpecSaveReqVO();
        req.setYarnTypeId(1L);
        req.setSpecName("32S 棉");
        req.setStandardName("32S-COTTON");

        YarnTypeDO yarnType = new YarnTypeDO();
        yarnType.setId(1L);
        when(yarnTypeMapper.selectById(1L)).thenReturn(yarnType);

        YarnSpecDO existing = new YarnSpecDO();
        existing.setId(10L);
        existing.setYarnTypeId(1L);
        existing.setStandardName("32S-COTTON");
        when(yarnSpecMapper.selectByYarnTypeIdAndStandardNameIncludingDeleted(1L, "32S-COTTON"))
                .thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnSpecService.createYarnSpec(req));
        assertEquals(1_030_008_003, ex.getCode());
        verify(yarnSpecMapper, never()).insert(any(YarnSpecDO.class));
    }

    @Test
    void createYarnSpec_success() {
        YarnSpecSaveReqVO req = new YarnSpecSaveReqVO();
        req.setYarnTypeId(1L);
        req.setSpecName("32S 棉");
        req.setStandardName("32S-COTTON");
        req.setSpinMethod("环锭纺");
        req.setLuster("半光");
        req.setRefPrice(new BigDecimal("25.5000"));
        req.setStatus("ACTIVE");

        YarnTypeDO yarnType = new YarnTypeDO();
        yarnType.setId(1L);
        when(yarnTypeMapper.selectById(1L)).thenReturn(yarnType);
        when(yarnSpecMapper.selectByYarnTypeIdAndStandardNameIncludingDeleted(1L, "32S-COTTON"))
                .thenReturn(null);

        Long id = yarnSpecService.createYarnSpec(req);

        assertEquals(200L, id);
        ArgumentCaptor<YarnSpecDO> captor = ArgumentCaptor.forClass(YarnSpecDO.class);
        verify(yarnSpecMapper).insert(captor.capture());
        YarnSpecDO saved = captor.getValue();
        assertEquals(1L, saved.getYarnTypeId());
        assertEquals("32S 棉", saved.getSpecName());
        assertEquals("32S-COTTON", saved.getStandardName());
        assertEquals("环锭纺", saved.getSpinMethod());
        assertEquals("半光", saved.getLuster());
        assertEquals(new BigDecimal("25.5000"), saved.getRefPrice());
        assertEquals("ACTIVE", saved.getStatus());
    }

    @Test
    void updateYarnSpec_notExists_throws1030008002() {
        YarnSpecSaveReqVO req = new YarnSpecSaveReqVO();
        req.setId(999L);
        req.setYarnTypeId(1L);
        req.setSpecName("32S 棉");
        req.setStandardName("32S-COTTON");

        when(yarnSpecMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnSpecService.updateYarnSpec(req));
        assertEquals(1_030_008_002, ex.getCode());
        verify(yarnSpecMapper, never()).updateById(any(YarnSpecDO.class));
    }

    @Test
    void updateYarnSpec_standardNameDuplicate_throws1030008003() {
        YarnSpecSaveReqVO req = new YarnSpecSaveReqVO();
        req.setId(10L);
        req.setYarnTypeId(1L);
        req.setSpecName("40S 棉");
        req.setStandardName("40S-COTTON");

        YarnSpecDO current = new YarnSpecDO();
        current.setId(10L);
        current.setYarnTypeId(1L);
        when(yarnSpecMapper.selectById(10L)).thenReturn(current);

        YarnTypeDO yarnType = new YarnTypeDO();
        yarnType.setId(1L);
        when(yarnTypeMapper.selectById(1L)).thenReturn(yarnType);

        YarnSpecDO another = new YarnSpecDO();
        another.setId(20L);
        another.setYarnTypeId(1L);
        another.setStandardName("40S-COTTON");
        when(yarnSpecMapper.selectByYarnTypeIdAndStandardNameIncludingDeleted(1L, "40S-COTTON"))
                .thenReturn(another);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnSpecService.updateYarnSpec(req));
        assertEquals(1_030_008_003, ex.getCode());
        verify(yarnSpecMapper, never()).updateById(any(YarnSpecDO.class));
    }

    @Test
    void deleteYarnSpec_notExists_throws1030008002() {
        when(yarnSpecMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnSpecService.deleteYarnSpec(999L));
        assertEquals(1_030_008_002, ex.getCode());
        verify(yarnSpecMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteYarnSpec_success_setsInactiveThenLogicalDelete() {
        YarnSpecDO existing = new YarnSpecDO();
        existing.setId(10L);
        existing.setStatus("ACTIVE");
        when(yarnSpecMapper.selectById(10L)).thenReturn(existing);

        yarnSpecService.deleteYarnSpec(10L);

        ArgumentCaptor<YarnSpecDO> captor = ArgumentCaptor.forClass(YarnSpecDO.class);
        verify(yarnSpecMapper).updateById(captor.capture());
        YarnSpecDO updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertEquals("INACTIVE", updated.getStatus());

        verify(yarnSpecMapper).deleteById(10L);
    }

    @Test
    void getYarnSpecPage_success() {
        YarnSpecPageReqVO reqVO = new YarnSpecPageReqVO();
        reqVO.setYarnTypeId(1L);

        YarnSpecDO dobj = new YarnSpecDO();
        dobj.setId(10L);
        dobj.setYarnTypeId(1L);
        dobj.setSpecName("32S 棉");
        dobj.setStandardName("32S-COTTON");
        dobj.setStatus("ACTIVE");

        PageResult<YarnSpecDO> pageResult = new PageResult<>(Collections.singletonList(dobj), 1L);
        when(yarnSpecMapper.selectPage(reqVO)).thenReturn(pageResult);

        YarnTypeDO yarnType = new YarnTypeDO();
        yarnType.setId(1L);
        yarnType.setName("棉");
        when(yarnTypeMapper.selectByIds(any())).thenReturn(Collections.singletonList(yarnType));

        PageResult<YarnSpecRespVO> result = yarnSpecService.getYarnSpecPage(reqVO);
        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("32S 棉", result.getList().get(0).getSpecName());
        assertEquals("棉", result.getList().get(0).getYarnTypeName());
    }

    @Test
    void getCascadeOptions_twoLevelStructure_withKeywordFilter() {
        YarnTypeDO typeCotton = new YarnTypeDO();
        typeCotton.setId(1L);
        typeCotton.setName("棉");
        typeCotton.setSort(1);
        typeCotton.setStatus("ACTIVE");

        YarnTypeDO typePoly = new YarnTypeDO();
        typePoly.setId(2L);
        typePoly.setName("涤");
        typePoly.setSort(2);
        typePoly.setStatus("ACTIVE");

        when(yarnTypeMapper.selectList(any())).thenReturn(Arrays.asList(typeCotton, typePoly));

        YarnSpecDO specCotton32S = new YarnSpecDO();
        specCotton32S.setId(101L);
        specCotton32S.setYarnTypeId(1L);
        specCotton32S.setSpecName("32S 棉");
        specCotton32S.setStandardName("32S-COTTON");
        specCotton32S.setRefPrice(new BigDecimal("25.0000"));
        specCotton32S.setStatus("ACTIVE");

        YarnSpecDO specCotton40S = new YarnSpecDO();
        specCotton40S.setId(102L);
        specCotton40S.setYarnTypeId(1L);
        specCotton40S.setSpecName("40S 棉");
        specCotton40S.setStandardName("40S-COTTON");
        specCotton40S.setRefPrice(new BigDecimal("28.0000"));
        specCotton40S.setStatus("ACTIVE");

        YarnSpecDO specPoly50D = new YarnSpecDO();
        specPoly50D.setId(201L);
        specPoly50D.setYarnTypeId(2L);
        specPoly50D.setSpecName("50D 涤");
        specPoly50D.setStandardName("50D-POLY");
        specPoly50D.setRefPrice(new BigDecimal("12.0000"));
        specPoly50D.setStatus("ACTIVE");

        // 无 keyword：返回棉和涤两级
        when(yarnSpecMapper.selectList(any())).thenReturn(Arrays.asList(specCotton32S, specCotton40S, specPoly50D));
        List<YarnSpecCascadeOptionVO> optionsNoKeyword = yarnSpecService.getCascadeOptions(null);
        assertEquals(2, optionsNoKeyword.size());
        assertEquals("棉", optionsNoKeyword.get(0).getYarnTypeName());
        assertEquals(2, optionsNoKeyword.get(0).getSpecs().size());
        assertEquals("涤", optionsNoKeyword.get(1).getYarnTypeName());
        assertEquals(1, optionsNoKeyword.get(1).getSpecs().size());

        // 有 keyword "32S"：仅匹配 32S 棉，涤不含匹配项故不返回或 specs 为空
        when(yarnSpecMapper.selectList(any())).thenReturn(Collections.singletonList(specCotton32S));
        List<YarnSpecCascadeOptionVO> optionsWithKeyword = yarnSpecService.getCascadeOptions("32S");
        assertEquals(1, optionsWithKeyword.size());
        assertEquals("棉", optionsWithKeyword.get(0).getYarnTypeName());
        assertEquals(1, optionsWithKeyword.get(0).getSpecs().size());
        assertEquals("32S 棉", optionsWithKeyword.get(0).getSpecs().get(0).getSpecName());
    }
}
