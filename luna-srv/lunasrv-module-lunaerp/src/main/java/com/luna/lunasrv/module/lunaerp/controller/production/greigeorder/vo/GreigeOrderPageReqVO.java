package com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 生产管理-胚布单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class GreigeOrderPageReqVO extends PageParam {

    @Schema(description = "关键词（命中 orderNo/contractNo/productName/productCode 之一）", example = "PB0919")
    private String keyword;

    @Schema(description = "胚布单状态：ACTIVE/COMPLETED/CANCELLED", example = "ACTIVE")
    private String status;

}
