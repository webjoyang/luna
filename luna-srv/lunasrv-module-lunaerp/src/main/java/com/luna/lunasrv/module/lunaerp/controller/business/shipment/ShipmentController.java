package com.luna.lunasrv.module.lunaerp.controller.business.shipment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;
import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Tag(name = "业务管理 - 发货单")
@RestController
@RequestMapping("/lunaerp/business/shipment")
@Validated
public class ShipmentController {

    @Resource
    private ShipmentService shipmentService;

    @PostMapping("/create")
    @Operation(summary = "创建发货单")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:shipment:create')")
    public CommonResult<Long> createShipment(@Valid @RequestBody ShipmentSaveReqVO createReqVO) {
        return success(shipmentService.createShipment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改发货单")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:shipment:update')")
    public CommonResult<Boolean> updateShipment(@Valid @RequestBody ShipmentSaveReqVO updateReqVO) {
        shipmentService.updateShipment(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除发货单")
    @Parameter(name = "id", description = "发货单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:shipment:delete')")
    public CommonResult<Boolean> deleteShipment(@RequestParam("id") Long id) {
        shipmentService.deleteShipment(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得发货单详情")
    @Parameter(name = "id", description = "发货单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:shipment:query')")
    public CommonResult<ShipmentRespVO> getShipment(@RequestParam("id") Long id) {
        return success(shipmentService.getShipment(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得发货单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:shipment:query')")
    public CommonResult<PageResult<ShipmentRespVO>> getShipmentPage(@Validated ShipmentPageReqVO pageReqVO) {
        return success(shipmentService.getShipmentPage(pageReqVO));
    }

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个发货单号（只读、不占号）")
    @Parameter(name = "shippingDate", description = "发货日期，可空（默认今天）", example = "2026-09-27")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:shipment:query') or @ss.hasPermission('lunaerp:business:shipment:create')")
    public CommonResult<String> getNextShipmentNo(
            @RequestParam(value = "shippingDate", required = false)
            @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY) LocalDate shippingDate) {
        return success(shipmentService.getNextShipmentNo(shippingDate));
    }

    @GetMapping("/from-contract")
    @Operation(summary = "从合同明细带入发货明细（含实时未发量计算）")
    @Parameter(name = "contractId", description = "销售合同ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:shipment:query') or @ss.hasPermission('lunaerp:business:shipment:create')")
    public CommonResult<List<ShipmentItemRespVO>> getShipmentItemsFromContract(@RequestParam("contractId") Long contractId) {
        return success(shipmentService.getShipmentItemsFromContract(contractId));
    }

    @PutMapping("/audit")
    @Operation(summary = "审核发货单（DRAFT -> COMPLETED，同事务写负数出库流水）")
    @Parameter(name = "id", description = "发货单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:shipment:audit')")
    public CommonResult<Boolean> auditShipment(@RequestParam("id") Long id) {
        shipmentService.auditShipment(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消发货单（草稿态作废 / 已发货态红冲）")
    @Parameter(name = "id", description = "发货单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasAnyPermissions('lunaerp:business:shipment:update', 'lunaerp:business:shipment:audit')")
    public CommonResult<Boolean> cancelShipment(@RequestParam("id") Long id) {
        shipmentService.cancelShipment(id);
        return success(true);
    }

}
