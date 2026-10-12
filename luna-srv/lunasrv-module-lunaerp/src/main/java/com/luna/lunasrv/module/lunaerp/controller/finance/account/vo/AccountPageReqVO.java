package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 财务管理 - 资金账户分页请求 VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 资金账户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AccountPageReqVO extends PageParam {

    @Schema(description = "资金账户名称（模糊）", example = "招商")
    private String accountName;

    @Schema(description = "账户类型", example = "BANK")
    private String accountType;

    @Schema(description = "状态（ENABLE/DISABLE）", example = "ENABLE")
    private String status;

}
