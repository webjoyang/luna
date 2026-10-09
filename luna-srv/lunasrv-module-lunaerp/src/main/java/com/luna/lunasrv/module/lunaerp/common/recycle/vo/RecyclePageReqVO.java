package com.luna.lunasrv.module.lunaerp.common.recycle.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 回收站分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RecyclePageReqVO extends PageParam {

    @Schema(description = "业务对象类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "CUSTOMER")
    @NotBlank(message = "业务对象类型不能为空")
    private String bizType;

}
