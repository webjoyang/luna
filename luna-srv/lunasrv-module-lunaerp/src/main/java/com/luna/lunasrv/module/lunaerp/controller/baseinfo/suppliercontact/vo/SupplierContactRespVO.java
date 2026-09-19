package com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 基本信息-供应商联系人 Response VO")
@Data
public class SupplierContactRespVO {

    @Schema(description = "联系人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long id;

    @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long supplierId;

    @Schema(description = "联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @Schema(description = "职位", example = "采购经理")
    private String position;

    @Schema(description = "联系电话", example = "13800000000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否主联系人（1 是 / 0 否）", example = "0")
    private Integer isPrimary;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
