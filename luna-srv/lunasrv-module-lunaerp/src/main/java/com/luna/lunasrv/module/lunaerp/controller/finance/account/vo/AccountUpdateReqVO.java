package com.luna.lunasrv.module.lunaerp.controller.finance.account.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 财务管理 - 资金账户修改 Request VO（仅备注/状态/名称可改，期初与当前余额只读）
 *
 * @author luna
 */
@Data
public class AccountUpdateReqVO {

    /**
     * 资金账户ID
     */
    @NotNull(message = "资金账户ID不能为空")
    private Long id;

    /**
     * 账户名称
     */
    @NotBlank(message = "账户名称不能为空")
    private String accountName;

    /**
     * 状态（ENABLE 启用 / DISABLE 停用）
     */
    @NotBlank(message = "状态不能为空")
    private String status;

    /**
     * 备注说明
     */
    private String remark;

}
