package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.note;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.note.vo.ProgressFeedbackNoteCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.note.vo.ProgressFeedbackNoteRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.progressfeedback.note.ProgressFeedbackNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 生产管理-进度反馈记录流水 Controller（ADR-0014 独立子资源）
 *
 * @author luna
 */
@Tag(name = "管理后台 - 生产管理-进度反馈记录")
@RestController
@RequestMapping("/lunaerp/production/progress-feedback/note")
@Validated
public class ProgressFeedbackNoteController {

    @Resource
    private ProgressFeedbackNoteService progressFeedbackNoteService;

    @GetMapping("/list-by-feedback")
    @Operation(summary = "获得指定缸的反馈记录流水列表（按创建时间倒序）")
    @Parameter(name = "feedbackId", description = "反馈缸ID", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:note')")
    public CommonResult<List<ProgressFeedbackNoteRespVO>> getNoteListByFeedback(
            @RequestParam("feedbackId") @NotNull(message = "反馈缸ID不能为空") Long feedbackId) {
        return success(progressFeedbackNoteService.getNoteListByFeedback(feedbackId));
    }

    @PostMapping("/create")
    @Operation(summary = "新建反馈记录（文字必填，可勾选问题标记）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:note')")
    public CommonResult<Long> createNote(@Valid @RequestBody ProgressFeedbackNoteCreateReqVO reqVO) {
        return success(progressFeedbackNoteService.createNote(reqVO));
    }

    @PostMapping("/resolve")
    @Operation(summary = "问题记录闭环（仅问题记录可闭环，写入闭环时间）")
    @Parameter(name = "id", description = "记录ID", required = true, example = "2001")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:note')")
    public CommonResult<Boolean> resolveNote(
            @RequestParam("id") @NotNull(message = "记录ID不能为空") Long id) {
        progressFeedbackNoteService.resolveNote(id);
        return success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除反馈记录（普通记录可删，已闭环问题禁删）")
    @Parameter(name = "id", description = "记录ID", required = true, example = "2001")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:progress-feedback:note')")
    public CommonResult<Boolean> deleteNote(@PathVariable("id") Long id) {
        progressFeedbackNoteService.deleteNote(id);
        return success(true);
    }

}
