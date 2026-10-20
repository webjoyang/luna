package com.luna.lunasrv.module.lunaerp.controller.portal;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentPageRespVO;
import com.luna.lunasrv.module.lunaerp.portal.service.PortalShipmentService;
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
 * 客户门户 - 发货单免登控制器
 *
 * @author luna
 */
@Tag(name = "客户门户 - 发货单")
@RestController
@RequestMapping("/app-api/lunaerp/portal/shipment")
@Validated
@PermitAll
public class PortalShipmentController {

    @Resource
    private PortalShipmentService portalShipmentService;

    @GetMapping("/page")
    @Operation(summary = "获得客户门户发货单分页")
    public CommonResult<PageResult<PortalShipmentPageRespVO>> getPortalShipmentPage(
            @Valid PortalShipmentPageReqVO pageReqVO) {
        return success(portalShipmentService.getPortalShipmentPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户门户发货单详情")
    @Parameters({
            @Parameter(name = "id", description = "发货单ID", example = "1024"),
            @Parameter(name = "no", description = "发货单号（二维码冻结契约）", example = "SH-20260927-001"),
            @Parameter(name = "shipmentNo", description = "发货单号别名", example = "SH-20260927-001")
    })
    public CommonResult<PortalShipmentDetailRespVO> getPortalShipment(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "no", required = false) String no,
            @RequestParam(value = "shipmentNo", required = false) String shipmentNo) {
        String finalNo = no != null && !no.trim().isEmpty() ? no : shipmentNo;
        return success(portalShipmentService.getPortalShipment(id, finalNo));
    }

}
