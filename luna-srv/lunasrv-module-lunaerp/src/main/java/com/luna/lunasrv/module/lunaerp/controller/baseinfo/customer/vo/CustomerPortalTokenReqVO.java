package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 客户门户令牌请求 VO
 *
 * @author luna
 */
@Schema(description = "管理端 - 客户门户令牌请求 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPortalTokenReqVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long customerId;

}
