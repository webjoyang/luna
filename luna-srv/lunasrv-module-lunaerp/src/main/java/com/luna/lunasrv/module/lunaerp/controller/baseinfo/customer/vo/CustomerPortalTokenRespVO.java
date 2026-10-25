package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 客户门户令牌信息 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPortalTokenRespVO {

    @Schema(description = "是否存在有效令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean exists;

    @Schema(description = "完整访问 URL（基址未配置时为空）", example = "https://portal.lunaerp.com/portal/shipments?t=xxx")
    private String url;

    @Schema(description = "令牌明文（基址未配置时用于仅复制令牌降级）", example = "D4g8x...")
    private String token;

    @Schema(description = "令牌指纹（前 6 位，用于日志与脱敏展示）", example = "D4g8xK")
    private String tokenFingerprint;

    @Schema(description = "生成时间", example = "2026-10-20T12:00:00")
    private LocalDateTime generateTime;

    @Schema(description = "剩余有效天数", example = "30")
    private Long remainingDays;

}
