package com.luna.lunasrv.module.lunaerp.approval.config;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 审批流程定义表 DO
 *
 * 表：lunaerp_approval_config
 *
 * @author luna
 */
@TableName("lunaerp_approval_config")
@KeySequence("lunaerp_approval_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalConfigDO extends TenantBaseDO {

    /**
     * 流程配置ID
     */
    @TableId
    private Long id;

    /**
     * 单据类型（字典 lunaerp_approval_biz_type：sales_contract/payment/expense/payroll/sales_return）
     */
    private String bizType;

    /**
     * 流程名称
     */
    private String name;

    /**
     * 状态（ENABLED 启用 / DISABLED 停用，同 biz_type 仅一条 ENABLED）
     */
    private String status;

    /**
     * 备注说明
     */
    private String remark;

}
