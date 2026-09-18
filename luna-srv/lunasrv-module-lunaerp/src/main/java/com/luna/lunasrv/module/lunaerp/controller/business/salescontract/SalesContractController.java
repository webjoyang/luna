package com.luna.lunasrv.module.lunaerp.controller.business.salescontract;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.business.salescontract.SalesContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

// 注意：本模块包名是 controller.business（不含 .controller.admin. 段），框架不会自动追加
// /admin-api 前缀，因此类级 @RequestMapping 必须显式写全 /admin-api 路径（前端 axios baseURL 为 /admin-api）。
@Tag(name = "业务管理-销售合同")
@RestController
@RequestMapping("/admin-api/lunaerp/business/sales-contract")
@Validated
public class SalesContractController {

    @Resource
    private SalesContractService salesContractService;

    @PostMapping("/create")
    @Operation(summary = "创建销售合同")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-contract:create')")
    public CommonResult<Long> createSalesContract(@Valid @RequestBody SalesContractSaveReqVO createReqVO) {
        return success(salesContractService.createSalesContract(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售合同分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-contract:query')")
    public CommonResult<PageResult<SalesContractRespVO>> getSalesContractPage(@Validated SalesContractPageReqVO pageReqVO) {
        return success(salesContractService.getSalesContractPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售合同")
    @Parameter(name = "id", description = "合同ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-contract:query')")
    public CommonResult<SalesContractRespVO> getSalesContract(@RequestParam("id") Long id) {
        return success(salesContractService.getSalesContract(id));
    }

    @PutMapping("/update")
    @Operation(summary = "修改销售合同")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-contract:update')")
    public CommonResult<Boolean> updateSalesContract(@Valid @RequestBody SalesContractSaveReqVO updateReqVO) {
        salesContractService.updateSalesContract(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除销售合同")
    @Parameter(name = "id", description = "合同ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:business:sales-contract:delete')")
    public CommonResult<Boolean> deleteSalesContract(@RequestParam("id") Long id) {
        salesContractService.deleteSalesContract(id);
        return success(true);
    }

}
