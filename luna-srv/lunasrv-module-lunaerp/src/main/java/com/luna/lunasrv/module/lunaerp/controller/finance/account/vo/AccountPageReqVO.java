package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 财务管理 - 资金账户分页 Request VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 资金账户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AccountPageReqVO extends PageParam {

    @Schema(description = "账户名称（模糊匹配）", example = "基本户")
    private String accountName;

    @Schema(description = "账户类型（CASH/BANK/WECHAT/ALIPAY）", example = "BANK")
    private String accountType;

    @Schema(description = "账户性质（PUBLIC/PRIVATE）", example = "PUBLIC")
    private String accountNature;

    @Schema(description = "账户币种", example = "RMB")
    private String currency;

    @Schema(description = "状态（ENABLE/DISABLE）", example = "ENABLE")
    private String status;

}
