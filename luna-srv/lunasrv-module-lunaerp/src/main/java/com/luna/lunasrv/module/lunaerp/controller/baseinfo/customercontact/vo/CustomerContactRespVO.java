package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo;

import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 基本信息-客户联系人 Response VO")
@Data
public class CustomerContactRespVO {

    @Schema(description = "联系人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long id;

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long customerId;

    @Schema(description = "联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @Sensitive(seePermission = "lunaerp:sensitive:contact", type = SensitiveType.CHINESE_NAME)
    private String name;

    @Schema(description = "职位", example = "采购经理")
    private String position;

    @Schema(description = "联系电话", example = "13800000000")
    @Sensitive(seePermission = "lunaerp:sensitive:contact", type = SensitiveType.MOBILE)
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Sensitive(seePermission = "lunaerp:sensitive:contact", type = SensitiveType.EMAIL)
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
