package com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 库存管理-仓库创建/修改 Request VO")
@Data
public class WarehouseSaveReqVO {

    @Schema(description = "仓库ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "仓库编号（留空自动生成 WH+年份+4位序号；手填按原值保存）", example = "WH20260001")
    private String warehouseNo;

    @Schema(description = "仓库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "成品一号仓")
    @NotBlank(message = "仓库名称不能为空")
    private String name;

    @Schema(description = "仓库地址", example = "浙江省绍兴市柯桥区仓库路1号")
    private String address;

    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE", example = "ACTIVE")
    private String status;

    @Schema(description = "备注")
    private String remark;

}
