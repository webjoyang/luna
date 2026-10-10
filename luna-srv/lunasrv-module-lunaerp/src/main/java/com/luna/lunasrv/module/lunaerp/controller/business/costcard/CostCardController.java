package com.luna.lunasrv.module.lunaerp.controller.business.costcard;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.costcard.vo.*;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.CostCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 业务管理 - 成本卡 Controller
 *
 * @author luna
 */
@Tag(name = "业务管理 - 成本卡")
@RestController
@RequestMapping("/lunaerp/business/cost-card")
@Validated
public class CostCardController {

    @Resource
    private CostCardService costCardService;

    @PostMapping("/create")
    @Operation(summary = "创建成本卡")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:create')")
    public CommonResult<Long> createCostCard(@Valid @RequestBody CostCardSaveReqVO createReqVO) {
        return success(costCardService.createCostCard(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新成本卡")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:update')")
    public CommonResult<Boolean> updateCostCard(@Valid @RequestBody CostCardSaveReqVO updateReqVO) {
        costCardService.updateCostCard(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除成本卡（仅草稿）")
    @Parameter(name = "id", description = "成本卡编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:delete')")
    public CommonResult<Boolean> deleteCostCard(@RequestParam("id") Long id) {
        costCardService.deleteCostCard(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得成本卡详情")
    @Parameter(name = "id", description = "成本卡编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:query')")
    public CommonResult<CostCardRespVO> getCostCard(@RequestParam("id") Long id) {
        return success(costCardService.getCostCard(id));
    }

    @GetMapping("/effective-by-product")
    @Operation(summary = "获得指定产品的当前生效成本卡")
    @Parameter(name = "productId", description = "产品编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:query')")
    public CommonResult<CostCardRespVO> getEffectiveCostCardByProductId(@RequestParam("productId") Long productId) {
        return success(costCardService.getEffectiveCostCardByProductId(productId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得成本卡分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:query')")
    public CommonResult<PageResult<CostCardRespVO>> getCostCardPage(@Valid CostCardPageReqVO pageReqVO) {
        return success(costCardService.getCostCardPage(pageReqVO));
    }

    @PutMapping("/effect")
    @Operation(summary = "生效成本卡")
    @Parameter(name = "id", description = "成本卡编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:update')")
    public CommonResult<Boolean> effectCostCard(@RequestParam("id") Long id) {
        costCardService.effectCostCard(id);
        return success(true);
    }

    @PutMapping("/expire")
    @Operation(summary = "失效成本卡")
    @Parameter(name = "id", description = "成本卡编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:update')")
    public CommonResult<Boolean> expireCostCard(@RequestParam("id") Long id) {
        costCardService.expireCostCard(id);
        return success(true);
    }

    @PostMapping("/copy")
    @Operation(summary = "复制成本卡生成新草稿")
    @Parameter(name = "id", description = "源成本卡编号", required = true)
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:update')")
    public CommonResult<Long> copyCostCard(@RequestParam("id") Long id) {
        return success(costCardService.copyCostCard(id));
    }

    @PostMapping("/refresh-prices")
    @Operation(summary = "重新拉取当日价格并重算")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:update')")
    public CommonResult<CostCardTrialRespVO> refreshPrices(@Valid @RequestBody CostCardTrialReqVO reqVO) {
        return success(costCardService.refreshPrices(reqVO));
    }

    @PostMapping("/trial")
    @Operation(summary = "成本卡试算（纯内存不落库）")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:query')")
    public CommonResult<CostCardTrialRespVO> trialCostCard(@Valid @RequestBody CostCardTrialReqVO trialReqVO) {
        return success(costCardService.trialCostCard(trialReqVO));
    }

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个成本卡号")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:cost-card:query')")
    public CommonResult<String> getNextCardNo() {
        return success(costCardService.getNextCardNo());
    }

}
