package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.customer.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "基础信息-客户档案")
@RestController
@RequestMapping("/lunaerp/baseinfo/customer")
@Validated
public class CustomerController {

    @Resource
    private CustomerService customerService;

    @PostMapping("/create")
    @Operation(summary = "创建客户")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:create')")
    public CommonResult<Long> createCustomer(@Valid @RequestBody CustomerSaveReqVO createReqVO) {
        return success(customerService.createCustomer(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得客户分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:query')")
    public CommonResult<PageResult<CustomerRespVO>> getCustomerPage(@Validated CustomerPageReqVO pageReqVO) {
        return success(customerService.getCustomerPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得客户")
    @Parameter(name = "id", description = "客户ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:query')")
    public CommonResult<CustomerRespVO> getCustomer(@RequestParam("id") Long id) {
        return success(customerService.getCustomer(id));
    }

    @PutMapping("/update")
    @Operation(summary = "修改客户")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:update')")
    public CommonResult<Boolean> updateCustomer(@Valid @RequestBody CustomerSaveReqVO updateReqVO) {
        customerService.updateCustomer(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除客户")
    @Parameter(name = "id", description = "客户ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:delete')")
    public CommonResult<Boolean> deleteCustomer(@RequestParam("id") Long id) {
        customerService.deleteCustomer(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除客户")
    @Parameter(name = "ids", description = "客户ID列表", required = true, example = "1,2,3")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:delete')")
    public CommonResult<Boolean> deleteCustomerList(@RequestParam("ids") List<Long> ids) {
        customerService.deleteCustomerList(ids);
        return success(true);
    }

    @GetMapping("/import-template")
    @Operation(summary = "获得客户导入模板")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:import')")
    public void importCustomerTemplate(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        List<com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerImportVO> list = customerService.getImportTemplate();
        com.luna.lunasrv.framework.excel.core.util.ExcelUtils.write(response, "客户导入模板.xlsx", "客户列表",
                com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerImportVO.class, list);
    }

    @PostMapping(value = "/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入客户档案")
    @io.swagger.v3.oas.annotations.Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true)
    })
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:import')")
    public CommonResult<Integer> importCustomer(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                                jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult result = customerService.importCustomer(file);
        if (result.hasError()) {
            result.writeResponse(response, "客户导入错误明细.xlsx");
            return null;
        }
        return success(result.getSuccessCount());
    }

}
