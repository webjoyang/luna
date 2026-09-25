package com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.suppliercontact.SupplierContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "基本信息-供应商联系人")
@RestController
@RequestMapping("/lunaerp/baseinfo/supplier-contact")
@Validated
public class SupplierContactController {

    @Resource
    private SupplierContactService supplierContactService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商联系人")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier-contact:create')")
    public CommonResult<Long> createSupplierContact(@Valid @RequestBody SupplierContactSaveReqVO createReqVO) {
        return success(supplierContactService.createSupplierContact(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商联系人分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier-contact:query')")
    public CommonResult<PageResult<SupplierContactRespVO>> getSupplierContactPage(
            @Validated SupplierContactPageReqVO pageReqVO) {
        return success(supplierContactService.getSupplierContactPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商联系人")
    @Parameter(name = "id", description = "联系人ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier-contact:query')")
    public CommonResult<SupplierContactRespVO> getSupplierContact(@RequestParam("id") Long id) {
        return success(supplierContactService.getSupplierContact(id));
    }

    @PutMapping("/update")
    @Operation(summary = "修改供应商联系人")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier-contact:update')")
    public CommonResult<Boolean> updateSupplierContact(@Valid @RequestBody SupplierContactSaveReqVO updateReqVO) {
        supplierContactService.updateSupplierContact(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商联系人")
    @Parameter(name = "id", description = "联系人ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:supplier-contact:delete')")
    public CommonResult<Boolean> deleteSupplierContact(@RequestParam("id") Long id) {
        supplierContactService.deleteSupplierContact(id);
        return success(true);
    }

}
