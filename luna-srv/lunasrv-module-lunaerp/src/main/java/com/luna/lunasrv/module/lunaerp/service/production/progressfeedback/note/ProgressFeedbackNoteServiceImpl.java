package com.luna.lunasrv.module.lunaerp.service.production.progressfeedback.note;

import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.note.vo.ProgressFeedbackNoteCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.note.vo.ProgressFeedbackNoteRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.ProgressFeedbackDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.note.ProgressFeedbackNoteDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.ProgressFeedbackMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.note.ProgressFeedbackNoteMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理 - 进度反馈记录流水 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ProgressFeedbackNoteServiceImpl implements ProgressFeedbackNoteService {

    @Resource
    private ProgressFeedbackNoteMapper progressFeedbackNoteMapper;

    @Resource
    private ProgressFeedbackMapper progressFeedbackMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNote(ProgressFeedbackNoteCreateReqVO reqVO) {
        // 1. 校验所属反馈缸是否存在且未被软删
        ProgressFeedbackDO vat = progressFeedbackMapper.selectById(reqVO.getFeedbackId());
        if (vat == null || Boolean.TRUE.equals(vat.getDeleted())) {
            throw exception(PROGRESS_FEEDBACK_VAT_NOT_EXISTS);
        }

        // 2. 构造并持久化反馈记录
        ProgressFeedbackNoteDO note = new ProgressFeedbackNoteDO();
        note.setFeedbackId(reqVO.getFeedbackId());
        note.setContent(reqVO.getContent().trim());
        note.setIssueFlag(Boolean.TRUE.equals(reqVO.getIssueFlag()));
        note.setResolvedAt(null);

        progressFeedbackNoteMapper.insert(note);
        return note.getId();
    }

    @Override
    public List<ProgressFeedbackNoteRespVO> getNoteListByFeedback(Long feedbackId) {
        List<ProgressFeedbackNoteDO> list = progressFeedbackNoteMapper.selectListByFeedbackId(feedbackId);
        return BeanUtils.toBean(list, ProgressFeedbackNoteRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveNote(Long id) {
        // 1. 校验记录存在性
        ProgressFeedbackNoteDO note = progressFeedbackNoteMapper.selectById(id);
        if (note == null || Boolean.TRUE.equals(note.getDeleted())) {
            throw exception(PROGRESS_FEEDBACK_NOTE_NOT_EXISTS);
        }

        // 2. 校验仅问题记录可闭环
        if (!Boolean.TRUE.equals(note.getIssueFlag())) {
            throw exception(PROGRESS_FEEDBACK_NOTE_NOT_AN_ISSUE);
        }

        // 3. 校验不可重复闭环
        if (note.getResolvedAt() != null) {
            throw exception(PROGRESS_FEEDBACK_NOTE_ALREADY_RESOLVED);
        }

        // 4. 写入闭环时间并更新
        note.setResolvedAt(LocalDateTime.now());
        progressFeedbackNoteMapper.updateById(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long id) {
        // 1. 校验记录存在性
        ProgressFeedbackNoteDO note = progressFeedbackNoteMapper.selectById(id);
        if (note == null || Boolean.TRUE.equals(note.getDeleted())) {
            throw exception(PROGRESS_FEEDBACK_NOTE_NOT_EXISTS);
        }

        // 2. 已闭环问题记录禁删（1-030-017-101）
        if (Boolean.TRUE.equals(note.getIssueFlag()) && note.getResolvedAt() != null) {
            throw exception(PROGRESS_FEEDBACK_NOTE_ALREADY_RESOLVED);
        }

        // 3. 删除记录
        progressFeedbackNoteMapper.deleteById(id);
    }

}
