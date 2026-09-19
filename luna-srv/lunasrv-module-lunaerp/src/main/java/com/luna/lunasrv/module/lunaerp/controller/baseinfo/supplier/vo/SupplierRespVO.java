package com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 基本信息-供应商 Response VO")
@Data
public class SupplierRespVO {

    @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUP20260001")
    private String supplierNo;

    @Schema(description = "公司/档口名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "绍兴纱线厂")
    private String companyName;

    @Schema(description = "简称", example = "绍兴纱线")
    private String shortName;

    @Schema(description = "联系电话", example = "0575-12345678")
    private String phone;

    @Schema(description = "公司地址", example = "浙江省绍兴市柯桥区")
    private String address;

    @Schema(description = "供应商类型（字典 lunaerp_supplier_type）", example = "YARN_MILL")
    private String supplierType;

    @Schema(description = "主营机型/设备说明", example = "34寸28针")
    private String machineType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用）")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
