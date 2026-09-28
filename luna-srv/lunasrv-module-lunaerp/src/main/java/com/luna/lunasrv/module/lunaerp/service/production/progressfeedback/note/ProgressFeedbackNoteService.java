package com.luna.lunasrv.module.lunaerp.service.production.progressfeedback.note;

import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.note.vo.ProgressFeedbackNoteCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.note.vo.ProgressFeedbackNoteRespVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 生产管理 - 进度反馈记录流水 Service 接口
 *
 * @author luna
 */
public interface ProgressFeedbackNoteService {

    /**
     * 新增反馈记录
     *
     * @param reqVO 创建信息
     * @return 记录ID
     */
    Long createNote(@Valid ProgressFeedbackNoteCreateReqVO reqVO);

    /**
     * 按反馈缸ID获取反馈记录列表（按创建时间倒序）
     *
     * @param feedbackId 反馈缸ID
     * @return 记录列表
     */
    List<ProgressFeedbackNoteRespVO> getNoteListByFeedback(Long feedbackId);

    /**
     * 问题闭环
     *
     * @param id 记录ID
     */
    void resolveNote(Long id);

    /**
     * 删除反馈记录
     *
     * @param id 记录ID
     */
    void deleteNote(Long id);

}
