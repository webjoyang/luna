package com.luna.lunasrv.module.lunaerp.portal.controller.portal.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 客户门户当前认证身份信息 Response VO
 *
 * @author luna
 */
@Schema(description = "客户门户 - 当前认证身份信息 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalAuthInfoRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long customerId;

    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试客户有限公司")
    private String customerName;

}
