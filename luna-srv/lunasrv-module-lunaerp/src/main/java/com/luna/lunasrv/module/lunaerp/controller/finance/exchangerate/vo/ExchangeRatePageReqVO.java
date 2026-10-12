package com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 财务管理 - 汇率分页请求 VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 汇率主档分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExchangeRatePageReqVO extends PageParam {

    @Schema(description = "币种", example = "USD")
    private String currency;

    @Schema(description = "状态（ENABLE/DISABLE）", example = "ENABLE")
    private String status;

}
