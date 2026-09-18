package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 基本信息-客户联系人创建/修改 Request VO")
@Data
public class CustomerContactSaveReqVO {

    @Schema(description = "联系人ID（修改时必填）", example = "2048")
    private Long id;

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @Schema(description = "联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "联系人姓名不能为空")
    private String name;

    @Schema(description = "职位", example = "采购经理")
    private String position;

    @Schema(description = "联系电话", example = "13800000000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否主联系人（1 是 / 0 否）", example = "0")
    private Integer isPrimary;

}
