package com.luna.lunasrv.module.lunaerp.approval.config;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 审批流程节点定义表 DO
 *
 * 表：lunaerp_approval_config_node
 *
 * @author luna
 */
@TableName("lunaerp_approval_config_node")
@KeySequence("lunaerp_approval_config_node_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalConfigNodeDO extends TenantBaseDO {

    /**
     * 节点定义ID
     */
    @TableId
    private Long id;

    /**
     * 关联流程配置ID（lunaerp_approval_config.id）
     */
    private Long configId;

    /**
     * 节点顺序（从1开始）
     */
    private Integer sort;

    /**
     * 节点名称（如部门主管审批）
     */
    private String name;

    /**
     * 节点类型（SEQUENTIAL 逐级 / OR_SIGN 或签 / AND_SIGN 会签）
     */
    private String nodeType;

    /**
     * 审批人来源（USER 指定用户 / POST 指定岗位 / ROLE 指定角色 / DEPT_LEADER 部门负责人 / SUBMITTER_LEADER 提交人直属上级）
     */
    private String approverType;

    /**
     * 审批人ID标识（USER时存用户ID列表JSON；POST/ROLE时存岗位或角色ID）
     */
    private String approverIds;

}
