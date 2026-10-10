package com.luna.lunasrv.module.lunaerp.controller.business.exportdoc;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportSellerConfigRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportSellerConfigSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.business.exportdoc.ExportDocService;
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
 * 管理后台 - 业务管理 - 出口单证
 *
 * @author luna
 */
@Tag(name = "管理后台 - 业务管理 - 出口单证")
@RestController
@RequestMapping("/lunaerp/business/export-doc")
@Validated
public class ExportDocController {

    @Resource
    private ExportDocService exportDocService;

    @PostMapping("/create")
    @Operation(summary = "创建出口单证套（聚合保存主表+明细）")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:create')")
    public CommonResult<Long> createExportDoc(@Valid @RequestBody ExportDocSaveReqVO createReqVO) {
        return success(exportDocService.createExportDoc(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新出口单证套（仅 DRAFT 态允许；聚合替换明细）")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:update')")
    public CommonResult<Boolean> updateExportDoc(@Valid @RequestBody ExportDocSaveReqVO updateReqVO) {
        exportDocService.updateExportDoc(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除出口单证套（仅 DRAFT 态允许；级联软删明细进回收站）")
    @Parameter(name = "id", description = "单证套编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:delete')")
    public CommonResult<Boolean> deleteExportDoc(@RequestParam("id") Long id) {
        exportDocService.deleteExportDoc(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得出口单证套详情（含明细列表）")
    @Parameter(name = "id", description = "单证套编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:query')")
    public CommonResult<ExportDocRespVO> getExportDoc(@RequestParam("id") Long id) {
        return success(exportDocService.getExportDoc(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得出口单证套分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:query')")
    public CommonResult<PageResult<ExportDocRespVO>> getExportDocPage(@Valid ExportDocPageReqVO pageVO) {
        return success(exportDocService.getExportDocPage(pageVO));
    }

    @GetMapping("/next-no")
    @Operation(summary = "获得下一个建议单证套号（只读预览不占号）")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:query')")
    public CommonResult<String> getNextDocNo() {
        return success(exportDocService.getNextDocNo());
    }

    @GetMapping("/seller-config")
    @Operation(summary = "获得当前租户的卖方出口配置")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:seller-config')")
    public CommonResult<ExportSellerConfigRespVO> getSellerConfig() {
        return success(exportDocService.getSellerConfig());
    }

    @PutMapping("/seller-config")
    @Operation(summary = "保存/更新当前租户的卖方出口配置（upsert 幂等）")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:seller-config')")
    public CommonResult<Boolean> saveSellerConfig(@Valid @RequestBody ExportSellerConfigSaveReqVO saveReqVO) {
        exportDocService.saveSellerConfig(saveReqVO);
        return success(true);
    }

    @PutMapping("/finalize")
    @Operation(summary = "定稿出口单证套（DRAFT -> FINALIZED，校验货物明细非空）")
    @Parameter(name = "id", description = "单证套编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:update')")
    public CommonResult<Boolean> finalizeExportDoc(@RequestParam("id") Long id) {
        exportDocService.finalizeExportDoc(id);
        return success(true);
    }

    @PutMapping("/revoke")
    @Operation(summary = "撤销定稿出口单证套（FINALIZED -> DRAFT）")
    @Parameter(name = "id", description = "单证套编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:update')")
    public CommonResult<Boolean> revokeExportDoc(@RequestParam("id") Long id) {
        exportDocService.revokeExportDoc(id);
        return success(true);
    }

    @GetMapping("/from-contract")
    @Operation(summary = "从销售合同带出草稿单证套数据（不落库）")
    @Parameter(name = "contractId", description = "销售合同ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:create')")
    public CommonResult<ExportDocRespVO> getDraftFromContract(@RequestParam("contractId") Long contractId) {
        return success(exportDocService.getDraftFromContract(contractId));
    }

    @GetMapping("/from-shipment")
    @Operation(summary = "从发货单带出草稿单证套数据（不落库）")
    @Parameter(name = "shipmentId", description = "发货单ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:create')")
    public CommonResult<ExportDocRespVO> getDraftFromShipment(@RequestParam("shipmentId") Long shipmentId) {
        return success(exportDocService.getDraftFromShipment(shipmentId));
    }

    @PutMapping("/resync-from-source")
    @Operation(summary = "重新同步源单据数据（仅 DRAFT 且非 MANUAL 来源；只补充空白字段、不覆盖已填字段）")
    @Parameter(name = "id", description = "单证套编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:export-doc:update')")
    public CommonResult<Boolean> resyncFromSource(@RequestParam("id") Long id) {
        exportDocService.resyncFromSource(id);
        return success(true);
    }

}
