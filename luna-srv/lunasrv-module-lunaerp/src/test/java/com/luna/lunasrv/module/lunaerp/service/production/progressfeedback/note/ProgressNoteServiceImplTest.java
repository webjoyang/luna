package com.luna.lunasrv.module.lunaerp.service.production.progressfeedback.note;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.note.vo.ProgressFeedbackNoteCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.note.vo.ProgressFeedbackNoteRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.ProgressFeedbackDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.note.ProgressFeedbackNoteDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.ProgressFeedbackMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.note.ProgressFeedbackNoteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link ProgressFeedbackNoteServiceImpl} 纯 Mockito 单元测试。
 *
 * 覆盖 05 票要求：
 * 1. 新增普通/问题两型反馈记录
 * 2. 缸不存在抛 1-030-017-000
 * 3. 按缸列表查询与字段转换
 * 4. 问题闭环成功写 resolvedAt
 * 5. 重复闭环拒（1-030-017-101）
 * 6. 非问题记录闭环拦截（1-030-017-102）
 * 7. 闭环记录不存在抛 1-030-017-100
 * 8. 普通记录与未闭环问题记录可删
 * 9. 已闭环问题记录禁删（1-030-017-101）
 * 10. 删除不存在记录抛 1-030-017-100
 *
 * @author luna
 */
public class ProgressNoteServiceImplTest {

    @Mock
    private ProgressFeedbackNoteMapper progressFeedbackNoteMapper;

    @Mock
    private ProgressFeedbackMapper progressFeedbackMapper;

    @InjectMocks
    private ProgressFeedbackNoteServiceImpl progressFeedbackNoteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ==================== 1. 新增反馈记录 ====================

    @Test
    @DisplayName("新增普通反馈记录成功：issueFlag=false，resolvedAt=null")
    void testCreateNote_normal_success() {
        Long feedbackId = 1001L;
        ProgressFeedbackDO vat = new ProgressFeedbackDO();
        vat.setId(feedbackId);
        vat.setDeleted(false);
        when(progressFeedbackMapper.selectById(feedbackId)).thenReturn(vat);

        doAnswer(invocation -> {
            ProgressFeedbackNoteDO entity = invocation.getArgument(0);
            entity.setId(2001L);
            return 1;
        }).when(progressFeedbackNoteMapper).insert(any(ProgressFeedbackNoteDO.class));

        ProgressFeedbackNoteCreateReqVO reqVO = new ProgressFeedbackNoteCreateReqVO();
        reqVO.setFeedbackId(feedbackId);
        reqVO.setContent("日常跟单进度询问，染厂反馈预计明日出缸");
        reqVO.setIssueFlag(false);

        Long noteId = progressFeedbackNoteService.createNote(reqVO);
        assertEquals(2001L, noteId);

        ArgumentCaptor<ProgressFeedbackNoteDO> captor = ArgumentCaptor.forClass(ProgressFeedbackNoteDO.class);
        verify(progressFeedbackNoteMapper).insert(captor.capture());
        ProgressFeedbackNoteDO saved = captor.getValue();
        assertEquals(feedbackId, saved.getFeedbackId());
        assertEquals("日常跟单进度询问，染厂反馈预计明日出缸", saved.getContent());
        assertFalse(saved.getIssueFlag());
        assertNull(saved.getResolvedAt());
    }

    @Test
    @DisplayName("新增问题反馈记录成功：issueFlag=true，resolvedAt=null")
    void testCreateNote_issue_success() {
        Long feedbackId = 1002L;
        ProgressFeedbackDO vat = new ProgressFeedbackDO();
        vat.setId(feedbackId);
        vat.setDeleted(false);
        when(progressFeedbackMapper.selectById(feedbackId)).thenReturn(vat);

        doAnswer(invocation -> {
            ProgressFeedbackNoteDO entity = invocation.getArgument(0);
            entity.setId(2002L);
            return 1;
        }).when(progressFeedbackNoteMapper).insert(any(ProgressFeedbackNoteDO.class));

        ProgressFeedbackNoteCreateReqVO reqVO = new ProgressFeedbackNoteCreateReqVO();
        reqVO.setFeedbackId(feedbackId);
        reqVO.setContent("色光偏暗，已通知实验室打小样复核");
        reqVO.setIssueFlag(true);

        Long noteId = progressFeedbackNoteService.createNote(reqVO);
        assertEquals(2002L, noteId);

        ArgumentCaptor<ProgressFeedbackNoteDO> captor = ArgumentCaptor.forClass(ProgressFeedbackNoteDO.class);
        verify(progressFeedbackNoteMapper).insert(captor.capture());
        ProgressFeedbackNoteDO saved = captor.getValue();
        assertEquals(feedbackId, saved.getFeedbackId());
        assertEquals("色光偏暗，已通知实验室打小样复核", saved.getContent());
        assertTrue(saved.getIssueFlag());
        assertNull(saved.getResolvedAt());
    }

    @Test
    @DisplayName("新增反馈记录：框架审计列记录人与创建时间正常承载")
    void testCreateNote_auditFields_success() {
        Long feedbackId = 1003L;
        ProgressFeedbackDO vat = new ProgressFeedbackDO();
        vat.setId(feedbackId);
        vat.setDeleted(false);
        when(progressFeedbackMapper.selectById(feedbackId)).thenReturn(vat);

        LocalDateTime now = LocalDateTime.now();
        doAnswer(invocation -> {
            ProgressFeedbackNoteDO entity = invocation.getArgument(0);
            entity.setId(2003L);
            entity.setCreator("dispatcher");
            entity.setCreateTime(now);
            return 1;
        }).when(progressFeedbackNoteMapper).insert(any(ProgressFeedbackNoteDO.class));

        ProgressFeedbackNoteCreateReqVO reqVO = new ProgressFeedbackNoteCreateReqVO();
        reqVO.setFeedbackId(feedbackId);
        reqVO.setContent("打样合格，同意投产");
        reqVO.setIssueFlag(false);

        Long noteId = progressFeedbackNoteService.createNote(reqVO);
        assertEquals(2003L, noteId);

        ArgumentCaptor<ProgressFeedbackNoteDO> captor = ArgumentCaptor.forClass(ProgressFeedbackNoteDO.class);
        verify(progressFeedbackNoteMapper, atLeastOnce()).insert(captor.capture());
        ProgressFeedbackNoteDO saved = captor.getValue();
        assertEquals(feedbackId, saved.getFeedbackId());
        assertEquals("打样合格，同意投产", saved.getContent());
    }

    @Test
    @DisplayName("新增反馈记录时缸不存在：抛 1-030-017-000")
    void testCreateNote_vatNotExists_throws000() {
        Long feedbackId = 9999L;
        when(progressFeedbackMapper.selectById(feedbackId)).thenReturn(null);

        ProgressFeedbackNoteCreateReqVO reqVO = new ProgressFeedbackNoteCreateReqVO();
        reqVO.setFeedbackId(feedbackId);
        reqVO.setContent("测试沟通");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> progressFeedbackNoteService.createNote(reqVO));
        assertEquals(PROGRESS_FEEDBACK_VAT_NOT_EXISTS.getCode(), ex.getCode());
        verify(progressFeedbackNoteMapper, never()).insert(any(ProgressFeedbackNoteDO.class));
    }

    // ==================== 2. 按缸查询记录列表 ====================

    @Test
    @DisplayName("按缸获取记录列表：字段正确透出并按创建倒序")
    void testGetNoteListByFeedback_success() {
        Long feedbackId = 1001L;

        ProgressFeedbackNoteDO n1 = new ProgressFeedbackNoteDO();
        n1.setId(101L);
        n1.setFeedbackId(feedbackId);
        n1.setContent("记录1");
        n1.setIssueFlag(false);
        n1.setCreateTime(LocalDateTime.now().minusHours(1));
        n1.setCreator("admin");

        ProgressFeedbackNoteDO n2 = new ProgressFeedbackNoteDO();
        n2.setId(102L);
        n2.setFeedbackId(feedbackId);
        n2.setContent("问题记录2");
        n2.setIssueFlag(true);
        n2.setResolvedAt(LocalDateTime.now());
        n2.setCreateTime(LocalDateTime.now().minusHours(2));
        n2.setCreator("user1");

        when(progressFeedbackNoteMapper.selectListByFeedbackId(feedbackId)).thenReturn(Arrays.asList(n1, n2));

        List<ProgressFeedbackNoteRespVO> list = progressFeedbackNoteService.getNoteListByFeedback(feedbackId);
        assertEquals(2, list.size());

        ProgressFeedbackNoteRespVO r1 = list.get(0);
        assertEquals(101L, r1.getId());
        assertEquals("记录1", r1.getContent());
        assertFalse(r1.getIssueFlag());
        assertNull(r1.getResolvedAt());
        assertEquals("admin", r1.getCreator());
        assertEquals(n1.getCreateTime(), r1.getCreateTime());

        ProgressFeedbackNoteRespVO r2 = list.get(1);
        assertEquals(102L, r2.getId());
        assertEquals("问题记录2", r2.getContent());
        assertTrue(r2.getIssueFlag());
        assertNotNull(r2.getResolvedAt());
        assertEquals("user1", r2.getCreator());
        assertEquals(n2.getCreateTime(), r2.getCreateTime());
    }

    // ==================== 3. 问题闭环 ====================

    @Test
    @DisplayName("问题记录闭环成功：写入 resolvedAt")
    void testResolveNote_success() {
        Long noteId = 2001L;
        ProgressFeedbackNoteDO note = new ProgressFeedbackNoteDO();
        note.setId(noteId);
        note.setFeedbackId(1001L);
        note.setContent("排缸机台故障");
        note.setIssueFlag(true);
        note.setResolvedAt(null);
        note.setDeleted(false);

        when(progressFeedbackNoteMapper.selectById(noteId)).thenReturn(note);

        progressFeedbackNoteService.resolveNote(noteId);

        ArgumentCaptor<ProgressFeedbackNoteDO> captor = ArgumentCaptor.forClass(ProgressFeedbackNoteDO.class);
        verify(progressFeedbackNoteMapper).updateById(captor.capture());
        ProgressFeedbackNoteDO updated = captor.getValue();
        assertEquals(noteId, updated.getId());
        assertNotNull(updated.getResolvedAt());
    }

    @Test
    @DisplayName("问题记录重复闭环：抛 1-030-017-101 (PROGRESS_FEEDBACK_NOTE_ALREADY_RESOLVED)")
    void testResolveNote_alreadyResolved_throws101() {
        Long noteId = 2002L;
        ProgressFeedbackNoteDO note = new ProgressFeedbackNoteDO();
        note.setId(noteId);
        note.setIssueFlag(true);
        note.setResolvedAt(LocalDateTime.now().minusHours(3));
        note.setDeleted(false);

        when(progressFeedbackNoteMapper.selectById(noteId)).thenReturn(note);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> progressFeedbackNoteService.resolveNote(noteId));
        assertEquals(PROGRESS_FEEDBACK_NOTE_ALREADY_RESOLVED.getCode(), ex.getCode());
        verify(progressFeedbackNoteMapper, never()).updateById(any(ProgressFeedbackNoteDO.class));
    }

    @Test
    @DisplayName("非问题记录尝试闭环：抛 1-030-017-102 (PROGRESS_FEEDBACK_NOTE_NOT_AN_ISSUE)")
    void testResolveNote_notAnIssue_throws102() {
        Long noteId = 2003L;
        ProgressFeedbackNoteDO note = new ProgressFeedbackNoteDO();
        note.setId(noteId);
        note.setIssueFlag(false);
        note.setResolvedAt(null);
        note.setDeleted(false);

        when(progressFeedbackNoteMapper.selectById(noteId)).thenReturn(note);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> progressFeedbackNoteService.resolveNote(noteId));
        assertEquals(PROGRESS_FEEDBACK_NOTE_NOT_AN_ISSUE.getCode(), ex.getCode());
        verify(progressFeedbackNoteMapper, never()).updateById(any(ProgressFeedbackNoteDO.class));
    }

    @Test
    @DisplayName("闭环不存在记录：抛 1-030-017-100 (PROGRESS_FEEDBACK_NOTE_NOT_EXISTS)")
    void testResolveNote_notExists_throws100() {
        Long noteId = 9999L;
        when(progressFeedbackNoteMapper.selectById(noteId)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> progressFeedbackNoteService.resolveNote(noteId));
        assertEquals(PROGRESS_FEEDBACK_NOTE_NOT_EXISTS.getCode(), ex.getCode());
        verify(progressFeedbackNoteMapper, never()).updateById(any(ProgressFeedbackNoteDO.class));
    }

    // ==================== 4. 删除反馈记录 ====================

    @Test
    @DisplayName("删除普通记录成功：正常删除")
    void testDeleteNote_normalSuccess() {
        Long noteId = 3001L;
        ProgressFeedbackNoteDO note = new ProgressFeedbackNoteDO();
        note.setId(noteId);
        note.setIssueFlag(false);
        note.setResolvedAt(null);
        note.setDeleted(false);

        when(progressFeedbackNoteMapper.selectById(noteId)).thenReturn(note);

        progressFeedbackNoteService.deleteNote(noteId);

        verify(progressFeedbackNoteMapper).deleteById(noteId);
    }

    @Test
    @DisplayName("删除未闭环问题记录成功：正常删除")
    void testDeleteNote_unresolvedIssueSuccess() {
        Long noteId = 3002L;
        ProgressFeedbackNoteDO note = new ProgressFeedbackNoteDO();
        note.setId(noteId);
        note.setIssueFlag(true);
        note.setResolvedAt(null);
        note.setDeleted(false);

        when(progressFeedbackNoteMapper.selectById(noteId)).thenReturn(note);

        progressFeedbackNoteService.deleteNote(noteId);

        verify(progressFeedbackNoteMapper).deleteById(noteId);
    }

    @Test
    @DisplayName("删除已闭环问题记录被禁：抛 1-030-017-101")
    void testDeleteNote_resolvedIssue_throws101() {
        Long noteId = 3003L;
        ProgressFeedbackNoteDO note = new ProgressFeedbackNoteDO();
        note.setId(noteId);
        note.setIssueFlag(true);
        note.setResolvedAt(LocalDateTime.now().minusDays(1));
        note.setDeleted(false);

        when(progressFeedbackNoteMapper.selectById(noteId)).thenReturn(note);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> progressFeedbackNoteService.deleteNote(noteId));
        assertEquals(PROGRESS_FEEDBACK_NOTE_ALREADY_RESOLVED.getCode(), ex.getCode());
        verify(progressFeedbackNoteMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除不存在记录：抛 1-030-017-100")
    void testDeleteNote_notExists_throws100() {
        Long noteId = 9999L;
        when(progressFeedbackNoteMapper.selectById(noteId)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> progressFeedbackNoteService.deleteNote(noteId));
        assertEquals(PROGRESS_FEEDBACK_NOTE_NOT_EXISTS.getCode(), ex.getCode());
        verify(progressFeedbackNoteMapper, never()).deleteById(anyLong());
    }

}
