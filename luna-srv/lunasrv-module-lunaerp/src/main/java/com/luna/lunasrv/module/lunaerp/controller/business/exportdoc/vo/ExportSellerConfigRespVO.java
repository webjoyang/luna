package com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务管理 - 卖方出口配置 Response VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 业务管理 - 卖方出口配置 Response VO")
@Data
public class ExportSellerConfigRespVO {

    @Schema(description = "配置ID", example = "1")
    private Long id;

    @Schema(description = "卖方英文名称（抬头/签章）", example = "LUNA TEXTILE CO., LTD.")
    private String sellerName;

    @Schema(description = "卖方英文地址", example = "No. 88 Luna Industrial Park, Hangzhou, Zhejiang, China")
    private String sellerAddress;

    @Schema(description = "收款银行名称", example = "BANK OF CHINA HANGZHOU BRANCH")
    private String bankName;

    @Schema(description = "收款银行账号 A/C", example = "123456789012")
    private String bankAccount;

    @Schema(description = "收款银行 SWIFT 码", example = "BKCHCNBJ999")
    private String bankSwift;

    @Schema(description = "收款银行地址", example = "No. 1 Qiushi Rd, Hangzhou, China")
    private String bankAddress;

    @Schema(description = "备注", example = "默认出口收汇账户")
    private String remark;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
