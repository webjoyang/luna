package com.luna.lunasrv.module.lunaerp.controller.production.productionprogress;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressPageItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressPageReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.productionprogress.ProductionProgressQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 生产管理-生产进度 Controller（只读聚合视图，ADR-0034）
 *
 * <p>进度是合同下各生产单据状态的投影，实时聚合不落库；page 跨合同列表、get 单合同全链路。</p>
 *
 * @author luna
 */
@Tag(name = "管理后台 - 生产管理-生产进度")
@RestController
@RequestMapping("/lunaerp/production/production-progress")
@Validated
public class ProductionProgressController {

    @Resource
    private ProductionProgressQueryService productionProgressQueryService;

    @GetMapping("/page")
    @Operation(summary = "获得生产进度分页（跨合同列表：合同头 + 各环节关键量 + 预警数）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production-progress:query')")
    public CommonResult<PageResult<ProductionProgressPageItemRespVO>> getProductionProgressPage(
            @Valid ProductionProgressPageReqVO reqVO) {
        return success(productionProgressQueryService.getProductionProgressPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得单合同全链路生产进度（六环节 + 发染记录/缸状态汇总下钻）")
    @Parameter(name = "id", description = "销售合同ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production-progress:query')")
    public CommonResult<ProductionProgressDetailRespVO> getProductionProgress(@RequestParam("id") Long id) {
        return success(productionProgressQueryService.getProductionProgress(id));
    }

}
