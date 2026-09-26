package com.luna.lunasrv.module.lunaerp.service.production.loom;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom.LoomDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.loom.LoomMapper;
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

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.LOOM_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.LOOM_NO_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.LOOM_REFERENCED_BY_MACHINE_TASK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * {@link LoomServiceImpl} 的纯 Mockito 单元测试
 *
 * 覆盖关键业务行为：
 * 1. 机台号自动取号（LM+年份+4位流水）、查重与重试
 * 2. 手填机台号查重（含已删除行，撞号报错）
 * 3. 详情与修改
 * 4. 删除守卫：被机台任务引用时禁止删除（1_030_015_202）
 * 5. 删除守卫：未被引用时置 INACTIVE 走逻辑删除（不物理删除）
 * 6. 下拉与预览下一个机台号
 *
 * @author luna
 */
class LoomServiceImplTest {

    @Mock
    private LoomMapper loomMapper;

    @Mock
    private LoomTaskReferenceProvider loomTaskReferenceProvider;

    @InjectMocks
    private LoomServiceImpl loomService;

    private static final String YEAR_PREFIX = "LM" + LocalDate.now().getYear();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), LoomDO.class);
        when(loomMapper.insert(any(LoomDO.class))).thenAnswer(invocation -> {
            LoomDO dobj = invocation.getArgument(0);
            dobj.setId(200L);
            return 1;
        });
    }

    private LoomSaveReqVO buildCreateReqVO() {
        LoomSaveReqVO req = new LoomSaveReqVO();
        req.setModel("大圆机 34″·28G");
        req.setRemark("1号车间");
        return req;
    }

    // ==================== 创建与取号 ====================

    @Test
    void createLoom_noLoomNo_generatesLMYearSeq() {
        LoomSaveReqVO req = buildCreateReqVO();
        req.setLoomNo(null);
        when(loomMapper.selectListByLoomNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        when(loomMapper.selectByLoomNoIncludingDeleted(anyString())).thenReturn(null);

        Long id = loomService.createLoom(req);

        assertEquals(200L, id);
        ArgumentCaptor<LoomDO> captor = ArgumentCaptor.forClass(LoomDO.class);
        verify(loomMapper).insert(captor.capture());
        LoomDO inserted = captor.getValue();
        assertEquals(YEAR_PREFIX + "0001", inserted.getLoomNo());
        assertEquals("ACTIVE", inserted.getStatus());
        assertEquals("大圆机 34″·28G", inserted.getModel());
    }

    @Test
    void createLoom_withManualLoomNo_success() {
        LoomSaveReqVO req = buildCreateReqVO();
        req.setLoomNo("A01");
        when(loomMapper.selectByLoomNoIncludingDeleted("A01")).thenReturn(null);

        Long id = loomService.createLoom(req);

        assertEquals(200L, id);
        ArgumentCaptor<LoomDO> captor = ArgumentCaptor.forClass(LoomDO.class);
        verify(loomMapper).insert(captor.capture());
        assertEquals("A01", captor.getValue().getLoomNo());
        assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    @Test
    void createLoom_duplicateLoomNo_throwsException() {
        LoomSaveReqVO req = buildCreateReqVO();
        req.setLoomNo("A01");
        LoomDO exist = new LoomDO();
        exist.setId(99L);
        exist.setLoomNo("A01");
        when(loomMapper.selectByLoomNoIncludingDeleted("A01")).thenReturn(exist);

        ServiceException ex = assertThrows(ServiceException.class, () -> loomService.createLoom(req));
        assertEquals(LOOM_NO_DUPLICATE.getCode(), ex.getCode());
        verify(loomMapper, never()).insert(any(LoomDO.class));
    }

    @Test
    void createLoom_autoGenClash_retriesAndThrowsIfExhausted() {
        LoomSaveReqVO req = buildCreateReqVO();
        req.setLoomNo(null);
        when(loomMapper.selectListByLoomNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());
        LoomDO clash = new LoomDO();
        clash.setId(88L);
        clash.setLoomNo(YEAR_PREFIX + "0001");
        when(loomMapper.selectByLoomNoIncludingDeleted(anyString())).thenReturn(clash);

        ServiceException ex = assertThrows(ServiceException.class, () -> loomService.createLoom(req));
        assertEquals(LOOM_NO_DUPLICATE.getCode(), ex.getCode());
        verify(loomMapper, never()).insert(any(LoomDO.class));
    }

    // ==================== 修改 ====================

    @Test
    void updateLoom_notExists_throwsException() {
        LoomSaveReqVO req = new LoomSaveReqVO();
        req.setId(999L);
        when(loomMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> loomService.updateLoom(req));
        assertEquals(LOOM_NOT_EXISTS.getCode(), ex.getCode());
        verify(loomMapper, never()).updateById(any(LoomDO.class));
    }

    @Test
    void updateLoom_duplicateLoomNo_throwsException() {
        LoomSaveReqVO req = new LoomSaveReqVO();
        req.setId(100L);
        req.setLoomNo("A02");

        LoomDO current = new LoomDO();
        current.setId(100L);
        current.setLoomNo("A01");
        when(loomMapper.selectById(100L)).thenReturn(current);

        LoomDO other = new LoomDO();
        other.setId(101L);
        other.setLoomNo("A02");
        when(loomMapper.selectByLoomNoIncludingDeleted("A02")).thenReturn(other);

        ServiceException ex = assertThrows(ServiceException.class, () -> loomService.updateLoom(req));
        assertEquals(LOOM_NO_DUPLICATE.getCode(), ex.getCode());
        verify(loomMapper, never()).updateById(any(LoomDO.class));
    }

    @Test
    void updateLoom_success() {
        LoomSaveReqVO req = new LoomSaveReqVO();
        req.setId(100L);
        req.setLoomNo("A01-MOD");
        req.setModel("大圆机 30″·24G");
        req.setStatus("INACTIVE");
        req.setRemark("已改型号并停用");

        LoomDO current = new LoomDO();
        current.setId(100L);
        current.setLoomNo("A01");
        when(loomMapper.selectById(100L)).thenReturn(current);
        when(loomMapper.selectByLoomNoIncludingDeleted("A01-MOD")).thenReturn(null);

        loomService.updateLoom(req);

        ArgumentCaptor<LoomDO> captor = ArgumentCaptor.forClass(LoomDO.class);
        verify(loomMapper).updateById(captor.capture());
        LoomDO updated = captor.getValue();
        assertEquals(100L, updated.getId());
        assertEquals("A01-MOD", updated.getLoomNo());
        assertEquals("大圆机 30″·24G", updated.getModel());
        assertEquals("INACTIVE", updated.getStatus());
        assertEquals("已改型号并停用", updated.getRemark());
    }

    // ==================== 删除守卫 ====================

    @Test
    void deleteLoom_notExists_throwsException() {
        when(loomMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> loomService.deleteLoom(999L));
        assertEquals(LOOM_NOT_EXISTS.getCode(), ex.getCode());
        verify(loomMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteLoom_referencedByMachineTask_throwsException() {
        LoomDO loom = new LoomDO();
        loom.setId(100L);
        loom.setLoomNo("A01");
        when(loomMapper.selectById(100L)).thenReturn(loom);
        when(loomTaskReferenceProvider.isLoomReferencedByTask(100L)).thenReturn(true);

        ServiceException ex = assertThrows(ServiceException.class, () -> loomService.deleteLoom(100L));
        assertEquals(LOOM_REFERENCED_BY_MACHINE_TASK.getCode(), ex.getCode());
        verify(loomMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteLoom_notReferenced_setsInactiveAndSoftDeletes() {
        LoomDO loom = new LoomDO();
        loom.setId(100L);
        loom.setLoomNo("A01");
        when(loomMapper.selectById(100L)).thenReturn(loom);
        when(loomTaskReferenceProvider.isLoomReferencedByTask(100L)).thenReturn(false);

        loomService.deleteLoom(100L);

        // 验证先将 status 置为 INACTIVE
        ArgumentCaptor<LoomDO> updateCaptor = ArgumentCaptor.forClass(LoomDO.class);
        verify(loomMapper).updateById(updateCaptor.capture());
        assertEquals(100L, updateCaptor.getValue().getId());
        assertEquals("INACTIVE", updateCaptor.getValue().getStatus());

        // 验证执行逻辑删除
        verify(loomMapper).deleteById(100L);
    }

    // ==================== 详情、分页与列表 ====================

    @Test
    void getLoom_success() {
        LoomDO loom = new LoomDO();
        loom.setId(100L);
        loom.setLoomNo("A01");
        loom.setModel("大圆机 34″·28G");
        loom.setStatus("ACTIVE");
        when(loomMapper.selectById(100L)).thenReturn(loom);

        LoomRespVO resp = loomService.getLoom(100L);

        assertNotNull(resp);
        assertEquals(100L, resp.getId());
        assertEquals("A01", resp.getLoomNo());
        assertEquals("ACTIVE", resp.getStatus());
    }

    @Test
    void getLoom_notExists_throwsException() {
        when(loomMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> loomService.getLoom(999L));
        assertEquals(LOOM_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void getLoomListAll_defaultActive_filtersActive() {
        LoomDO l1 = new LoomDO();
        l1.setId(1L);
        l1.setLoomNo("A01");
        l1.setStatus("ACTIVE");
        when(loomMapper.selectList(any())).thenReturn(List.of(l1));

        List<LoomRespVO> list = loomService.getLoomListAll(null);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("A01", list.get(0).getLoomNo());
    }

    @Test
    void getNextLoomNo_returnsNextSequence() {
        when(loomMapper.selectListByLoomNoPrefixIncludingDeleted(anyString()))
                .thenReturn(Collections.emptyList());

        String nextNo = loomService.getNextLoomNo();

        assertEquals(YEAR_PREFIX + "0001", nextNo);
    }

}
