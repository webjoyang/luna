package com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 基本信息-供应商联系人分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierContactPageReqVO extends PageParam {

    @Schema(description = "供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;

}
