package com.luna.lunasrv.module.lunaerp.service.production.yarntype;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypeSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link YarnTypeServiceImpl} 的单元测试。
 */
class YarnTypeServiceImplTest {

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @InjectMocks
    private YarnTypeServiceImpl yarnTypeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(yarnTypeMapper.insert(any(YarnTypeDO.class))).thenAnswer(invocation -> {
            YarnTypeDO dobj = invocation.getArgument(0);
            dobj.setId(100L);
            return 1;
        });
    }

    @Test
    void createYarnType_success() {
        YarnTypeSaveReqVO req = new YarnTypeSaveReqVO();
        req.setName("棉");
        req.setSort(1);
        req.setStatus("ACTIVE");

        when(yarnTypeMapper.selectByNameIncludingDeleted("棉")).thenReturn(null);

        Long id = yarnTypeService.createYarnType(req);

        assertEquals(100L, id);
        ArgumentCaptor<YarnTypeDO> captor = ArgumentCaptor.forClass(YarnTypeDO.class);
        verify(yarnTypeMapper).insert(captor.capture());
        YarnTypeDO saved = captor.getValue();
        assertEquals("棉", saved.getName());
        assertEquals(1, saved.getSort());
        assertEquals("ACTIVE", saved.getStatus());
    }

    @Test
    void createYarnType_nameDuplicate_throws1030008001() {
        YarnTypeSaveReqVO req = new YarnTypeSaveReqVO();
        req.setName("棉");

        YarnTypeDO existing = new YarnTypeDO();
        existing.setId(1L);
        existing.setName("棉");
        when(yarnTypeMapper.selectByNameIncludingDeleted("棉")).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnTypeService.createYarnType(req));
        assertEquals(1_030_008_001, ex.getCode());
        verify(yarnTypeMapper, never()).insert(any(YarnTypeDO.class));
    }

    @Test
    void updateYarnType_notExists_throws1030008000() {
        YarnTypeSaveReqVO req = new YarnTypeSaveReqVO();
        req.setId(999L);
        req.setName("棉");

        when(yarnTypeMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnTypeService.updateYarnType(req));
        assertEquals(1_030_008_000, ex.getCode());
        verify(yarnTypeMapper, never()).updateById(any(YarnTypeDO.class));
    }

    @Test
    void updateYarnType_nameDuplicate_throws1030008001() {
        YarnTypeSaveReqVO req = new YarnTypeSaveReqVO();
        req.setId(100L);
        req.setName("涤");

        YarnTypeDO current = new YarnTypeDO();
        current.setId(100L);
        current.setName("棉");
        when(yarnTypeMapper.selectById(100L)).thenReturn(current);

        YarnTypeDO another = new YarnTypeDO();
        another.setId(200L);
        another.setName("涤");
        when(yarnTypeMapper.selectByNameIncludingDeleted("涤")).thenReturn(another);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnTypeService.updateYarnType(req));
        assertEquals(1_030_008_001, ex.getCode());
        verify(yarnTypeMapper, never()).updateById(any(YarnTypeDO.class));
    }

    @Test
    void deleteYarnType_notExists_throws1030008000() {
        when(yarnTypeMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnTypeService.deleteYarnType(999L));
        assertEquals(1_030_008_000, ex.getCode());
        verify(yarnTypeMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteYarnType_success_setsInactiveThenLogicalDelete() {
        YarnTypeDO existing = new YarnTypeDO();
        existing.setId(100L);
        existing.setName("棉");
        existing.setStatus("ACTIVE");
        when(yarnTypeMapper.selectById(100L)).thenReturn(existing);

        yarnTypeService.deleteYarnType(100L);

        ArgumentCaptor<YarnTypeDO> captor = ArgumentCaptor.forClass(YarnTypeDO.class);
        verify(yarnTypeMapper).updateById(captor.capture());
        YarnTypeDO updated = captor.getValue();
        assertEquals(100L, updated.getId());
        assertEquals("INACTIVE", updated.getStatus());

        verify(yarnTypeMapper).deleteById(100L);
    }

    @Test
    void getYarnTypePage_success() {
        YarnTypePageReqVO reqVO = new YarnTypePageReqVO();
        reqVO.setName("棉");

        YarnTypeDO dobj = new YarnTypeDO();
        dobj.setId(1L);
        dobj.setName("棉");
        dobj.setSort(1);
        dobj.setStatus("ACTIVE");

        PageResult<YarnTypeDO> pageResult = new PageResult<>(Collections.singletonList(dobj), 1L);
        when(yarnTypeMapper.selectPage(reqVO)).thenReturn(pageResult);

        PageResult<YarnTypeRespVO> result = yarnTypeService.getYarnTypePage(reqVO);
        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("棉", result.getList().get(0).getName());
    }

    @Test
    void getYarnType_notExists_throws1030008000() {
        when(yarnTypeMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> yarnTypeService.getYarnType(999L));
        assertEquals(1_030_008_000, ex.getCode());
    }
}
