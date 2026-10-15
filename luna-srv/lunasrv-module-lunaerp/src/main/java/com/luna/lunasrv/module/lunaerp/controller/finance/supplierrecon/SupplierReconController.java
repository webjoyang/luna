package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon;

import com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon.SupplierReconService;
import com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon.SupplierReconSourceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 财务管理 - 供应商对账 控制器
 *
 * @author luna
 */
@Tag(name = "财务管理 - 供应商对账")
@RestController
@RequestMapping("/lunaerp/finance/supplier-recon")
@Validated
public class SupplierReconController {

    @Resource
    private SupplierReconService supplierReconService;

    @Resource
    private SupplierReconSourceService supplierReconSourceService;

}
