package com.luna.lunasrv.module.lunaerp.service.portal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户门户令牌响应 VO
 *
 * @author luna
 */
@Schema(description = "管理端 - 客户门户令牌响应 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalTokenRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门户令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "abc123xyz456")
    private String token;

    @Schema(description = "门户完整 URL（未配置基准地址时为 null）", example = "https://portal.luna.com/portal/shipments?t=abc123xyz456")
    private String url;

    @Schema(description = "生成时间")
    private LocalDateTime generateTime;

    @Schema(description = "剩余有效天数", example = "30")
    private Integer expireDays;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

}
