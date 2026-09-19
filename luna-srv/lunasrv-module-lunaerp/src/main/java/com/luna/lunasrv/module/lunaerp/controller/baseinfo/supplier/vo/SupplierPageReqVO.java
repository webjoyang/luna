package com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 基本信息-供应商分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierPageReqVO extends PageParam {

    @Schema(description = "智能搜索词（命中 supplierNo/companyName/shortName/phone 之一）", example = "绍兴")
    private String search;

    @Schema(description = "供应商类型（字典 lunaerp_supplier_type 等值过滤）", example = "YARN_MILL")
    private String supplierType;

    @Schema(description = "状态：ACTIVE/INACTIVE", example = "ACTIVE")
    private String status;

}
