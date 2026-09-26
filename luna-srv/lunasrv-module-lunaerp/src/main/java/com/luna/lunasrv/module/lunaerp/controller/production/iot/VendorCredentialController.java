package com.luna.lunasrv.module.lunaerp.controller.production.iot;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.iot.credential.VendorCredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - IoT 厂商凭证")
@RestController
@RequestMapping("/lunaerp/production/iot/vendor-credential")
@Validated
public class VendorCredentialController {

    @Resource
    private VendorCredentialService vendorCredentialService;

    @PostMapping("/create")
    @Operation(summary = "创建厂商凭证")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:vendor-credential:create')")
    public CommonResult<Long> createVendorCredential(@Valid @RequestBody VendorCredentialSaveReqVO createReqVO) {
        return success(vendorCredentialService.createVendorCredential(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改厂商凭证")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:vendor-credential:update')")
    public CommonResult<Boolean> updateVendorCredential(@Valid @RequestBody VendorCredentialSaveReqVO updateReqVO) {
        vendorCredentialService.updateVendorCredential(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除厂商凭证")
    @Parameter(name = "id", description = "凭证ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:vendor-credential:delete')")
    public CommonResult<Boolean> deleteVendorCredential(@RequestParam("id") Long id) {
        vendorCredentialService.deleteVendorCredential(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得厂商凭证详情（不回显明文密码）")
    @Parameter(name = "id", description = "凭证ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:vendor-credential:query')")
    public CommonResult<VendorCredentialRespVO> getVendorCredential(@RequestParam("id") Long id) {
        return success(vendorCredentialService.getVendorCredential(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得厂商凭证分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:vendor-credential:query')")
    public CommonResult<PageResult<VendorCredentialRespVO>> getVendorCredentialPage(@Validated VendorCredentialPageReqVO pageReqVO) {
        return success(vendorCredentialService.getVendorCredentialPage(pageReqVO));
    }

}
