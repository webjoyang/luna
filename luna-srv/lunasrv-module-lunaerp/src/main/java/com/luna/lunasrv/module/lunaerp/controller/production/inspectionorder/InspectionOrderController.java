package com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.InspectionOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理-检验单")
@RestController
@RequestMapping("/lunaerp/production/inspection-order")
@Validated
public class InspectionOrderController {

    @Resource
    private InspectionOrderService inspectionOrderService;

    @GetMapping("/page")
    @Operation(summary = "获得检验单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order:query')")
    public CommonResult<PageResult<InspectionOrderRespVO>> getInspectionOrderPage(@Validated InspectionOrderPageReqVO pageReqVO) {
        return success(inspectionOrderService.getInspectionOrderPage(pageReqVO));
    }

}
