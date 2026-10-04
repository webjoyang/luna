package com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.sample.sampleanalysis.SampleAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "样品管理-样品分析")
@RestController
@RequestMapping("/lunaerp/sample/sample-analysis")
@Validated
public class SampleAnalysisController {

    @Resource
    private SampleAnalysisService sampleAnalysisService;

    @PostMapping("/create")
    @Operation(summary = "创建样品分析")
    @PreAuthorize("@ss.hasPermission('lunaerp:sample:sample-analysis:create')")
    public CommonResult<Long> createSampleAnalysis(@Valid @RequestBody SampleAnalysisSaveReqVO createReqVO) {
        return success(sampleAnalysisService.createSampleAnalysis(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新样品分析")
    @PreAuthorize("@ss.hasPermission('lunaerp:sample:sample-analysis:update')")
    public CommonResult<Boolean> updateSampleAnalysis(@Valid @RequestBody SampleAnalysisSaveReqVO updateReqVO) {
        sampleAnalysisService.updateSampleAnalysis(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除样品分析")
    @Parameter(name = "id", description = "样品分析ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:sample:sample-analysis:delete')")
    public CommonResult<Boolean> deleteSampleAnalysis(@RequestParam("id") Long id) {
        sampleAnalysisService.deleteSampleAnalysis(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得样品分析详情")
    @Parameter(name = "id", description = "样品分析ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:sample:sample-analysis:query')")
    public CommonResult<SampleAnalysisRespVO> getSampleAnalysis(@RequestParam("id") Long id) {
        return success(sampleAnalysisService.getSampleAnalysis(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得样品分析分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:sample:sample-analysis:query')")
    public CommonResult<PageResult<SampleAnalysisRespVO>> getSampleAnalysisPage(@Validated SampleAnalysisPageReqVO pageReqVO) {
        return success(sampleAnalysisService.getSampleAnalysisPage(pageReqVO));
    }

    @GetMapping("/next-no")
    @Operation(summary = "预览下一个样品编号（只读、不占号）")
    @PreAuthorize("@ss.hasPermission('lunaerp:sample:sample-analysis:query')")
    public CommonResult<String> getNextSampleAnalysisNo() {
        return success(sampleAnalysisService.getNextSampleAnalysisNo());
    }

}
