package com.luna.lunasrv.module.lunaerp.controller.business.costcard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "成本卡试算 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CostCardTrialReqVO extends CostCardSaveReqVO {
}
