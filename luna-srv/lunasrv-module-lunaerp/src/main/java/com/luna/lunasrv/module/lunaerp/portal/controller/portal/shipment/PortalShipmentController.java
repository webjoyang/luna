package com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentDetailRespVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentPageItemRespVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 客户门户 - 发货单只读控制器
 *
 * <p>端点统一挂在 /app-api/lunaerp/portal/shipment 下，标注 @PermitAll 豁免 Spring Security admin 鉴权，
 * 由自定义 PortalTokenFilter 校验令牌并注入 PortalContext 上下文。</p>
 *
 * @author luna
 */
@Tag(name = "客户门户 - 发货单")
@PermitAll
@RestController
@RequestMapping("/app-api/lunaerp/portal/shipment")
@Validated
public class PortalShipmentController {

    @Resource
    private PortalShipmentService portalShipmentService;

    @GetMapping("/page")
    @PermitAll
    @Operation(summary = "获取发货单分页列表")
    public CommonResult<PageResult<PortalShipmentPageItemRespVO>> getShipmentPage(@Valid PortalShipmentPageReqVO reqVO) {
        return success(portalShipmentService.getShipmentPage(reqVO));
    }

    @GetMapping("/get")
    @PermitAll
    @Operation(summary = "获取发货单详情")
    @Parameters({
            @Parameter(name = "id", description = "发货单ID", required = false),
            @Parameter(name = "no", description = "发货单号", required = false)
    })
    public CommonResult<PortalShipmentDetailRespVO> getShipmentDetail(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "no", required = false) String no) {
        return success(portalShipmentService.getShipmentDetail(id, no));
    }

}
