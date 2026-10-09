package com.luna.lunasrv.module.lunaerp.approval.instance;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 审批实例表 DO
 *
 * 表：lunaerp_approval_instance
 *
 * @author luna
 */
@TableName("lunaerp_approval_instance")
@KeySequence("lunaerp_approval_instance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalInstanceDO extends TenantBaseDO {

    /**
     * 审批实例ID
     */
    @TableId
    private Long id;

    /**
     * 业务单据类型（sales_contract/payment/expense/payroll/sales_return）
     */
    private String bizType;

    /**
     * 业务单据ID
     */
    private Long bizId;

    /**
     * 业务单号快照（展示用）
     */
    private String bizNo;

    /**
     * 流程定义全量快照JSON（含每节点审批人ID列表，发起时解析冻结）
     */
    private String processSnapshot;

    /**
     * 实例状态（APPROVING 审批中 / APPROVED 已通过 / REJECTED 已驳回 / CANCELLED 已撤回）
     */
    private String status;

    /**
     * 发起人用户ID
     */
    private Long submitterId;

    /**
     * 当前节点序号（-1表示流程已结束）
     */
    private Integer currentNodeSort;

    /**
     * 预留 Flowable 流程实例ID
     */
    private String flowableProcInstId;

    /**
     * 发起时间
     */
    private LocalDateTime startTime;

    /**
     * 审批结束时间（终态时回填）
     */
    private LocalDateTime endTime;

}
