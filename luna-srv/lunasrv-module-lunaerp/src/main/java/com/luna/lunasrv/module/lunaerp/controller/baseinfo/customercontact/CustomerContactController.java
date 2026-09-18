package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.customercontact.CustomerContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

// 注意：本模块包名是 controller.baseinfo（不含 .controller.admin. 段），框架不会自动追加
// /admin-api 前缀，因此类级 @RequestMapping 必须显式写全 /admin-api 路径（前端 axios baseURL 为 /admin-api）。
@Tag(name = "基本信息-客户联系人")
@RestController
@RequestMapping("/admin-api/lunaerp/baseinfo/customer-contact")
@Validated
public class CustomerContactController {

    @Resource
    private CustomerContactService customerContactService;

    @PostMapping("/create")
    @Operation(summary = "创建客户联系人")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer-contact:create')")
    public CommonResult<Long> createCustomerContact(@Valid @RequestBody CustomerContactSaveReqVO createReqVO) {
        return success(customerContactService.createCustomerContact(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户联系人分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer-contact:query')")
    public CommonResult<PageResult<CustomerContactRespVO>> getCustomerContactPage(
            @Validated CustomerContactPageReqVO pageReqVO) {
        return success(customerContactService.getCustomerContactPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户联系人")
    @Parameter(name = "id", description = "联系人ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer-contact:query')")
    public CommonResult<CustomerContactRespVO> getCustomerContact(@RequestParam("id") Long id) {
        return success(customerContactService.getCustomerContact(id));
    }

    @PutMapping("/update")
    @Operation(summary = "修改客户联系人")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer-contact:update')")
    public CommonResult<Boolean> updateCustomerContact(@Valid @RequestBody CustomerContactSaveReqVO updateReqVO) {
        customerContactService.updateCustomerContact(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户联系人")
    @Parameter(name = "id", description = "联系人ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer-contact:delete')")
    public CommonResult<Boolean> deleteCustomerContact(@RequestParam("id") Long id) {
        customerContactService.deleteCustomerContact(id);
        return success(true);
    }

}
