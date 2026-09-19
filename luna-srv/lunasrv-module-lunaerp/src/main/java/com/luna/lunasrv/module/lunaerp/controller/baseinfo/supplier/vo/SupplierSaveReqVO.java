package com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 基本信息-供应商创建/修改 Request VO")
@Data
public class SupplierSaveReqVO {

    @Schema(description = "供应商ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "供应商编号（留空自动生成 SUP+年份+4位序号；手填按原值保存）", example = "SUP20260001")
    private String supplierNo;

    @Schema(description = "公司/档口名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "绍兴纱线厂")
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @Schema(description = "简称（为空时后端用 companyName 回填）", example = "绍兴纱线")
    private String shortName;

    @Schema(description = "联系电话", example = "0575-12345678")
    private String phone;

    @Schema(description = "公司地址", example = "浙江省绍兴市柯桥区")
    private String address;

    @Schema(description = "供应商类型（字典 lunaerp_supplier_type，可空）", example = "YARN_MILL")
    private String supplierType;

    @Schema(description = "主营机型/设备说明（自由文本）", example = "34寸28针")
    private String machineType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE", example = "ACTIVE")
    private String status;

}
