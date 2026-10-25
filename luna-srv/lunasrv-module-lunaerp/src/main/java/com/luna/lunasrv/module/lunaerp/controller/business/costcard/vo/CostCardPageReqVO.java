package com.luna.lunasrv.module.lunaerp.controller.business.costcard.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "成本卡分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CostCardPageReqVO extends PageParam {

    @Schema(description = "搜索关键词（卡号 / 产品编号 / 品名）", example = "CC2026")
    private String search;

    @Schema(description = "状态（DRAFT / EFFECTIVE / EXPIRED）", example = "EFFECTIVE")
    private String status;

    @Schema(description = "产品ID", example = "1")
    private Long productId;

    @Schema(description = "织物结构（KNIT / WOVEN）", example = "KNIT")
    private String fabricConstruction;

}
