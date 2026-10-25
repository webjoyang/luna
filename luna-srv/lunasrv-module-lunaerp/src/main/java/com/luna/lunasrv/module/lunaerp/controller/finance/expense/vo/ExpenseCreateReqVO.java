package com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 员工费用报销单创建 Request VO")
@Data
public class ExpenseCreateReqVO {

    @Schema(description = "报销人用户ID（缺省当前登录用户，可代他人填报）", example = "10")
    private Long applicantUserId;

    @Schema(description = "付款资金账户ID（必填，改账户联动币种）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "付款资金账户不能为空")
    private Long accountId;

    @Schema(description = "附件列表（有序文件URL数组，最多10个）")
    @Size(max = 10, message = "报销附件最多上传10个")
    private List<String> attachments;

    @Schema(description = "备注说明", example = "出差上海报销")
    private String remark;

    @Schema(description = "报销明细行列表（至少1行）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "报销明细行至少需要一行")
    @Valid
    private List<ExpenseItemSaveReqVO> items;

}
