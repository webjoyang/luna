package com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 业务管理 - 卖方出口配置保存 Request VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 业务管理 - 卖方出口配置保存 Request VO")
@Data
public class ExportSellerConfigSaveReqVO {

    @Schema(description = "卖方英文名称（抬头/签章）", requiredMode = Schema.RequiredMode.REQUIRED, example = "LUNA TEXTILE CO., LTD.")
    @NotEmpty(message = "卖方英文名称不能为空")
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

}
