package com.luna.lunasrv.module.lunaerp.approval.record;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 审批操作记录表 DO
 *
 * 表：lunaerp_approval_record
 *
 * @author luna
 */
@TableName("lunaerp_approval_record")
@KeySequence("lunaerp_approval_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRecordDO extends TenantBaseDO {

    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_TRANSFER = "TRANSFER";
    public static final String ACTION_WITHDRAW = "WITHDRAW";

    /**
     * 操作记录ID
     */
    @TableId
    private Long id;

    /**
     * 关联审批实例ID（lunaerp_approval_instance.id）
     */
    private Long instanceId;

    /**
     * 关联审批任务ID（可空，如撤回/提交操作）
     */
    private Long taskId;

    /**
     * 操作人用户ID
     */
    private Long operatorId;

    /**
     * 操作动作（SUBMIT / APPROVE / REJECT / TRANSFER / WITHDRAW）
     */
    private String action;

    /**
     * 审批意见/处理说明
     */
    private String comment;

    /**
     * 转交接收人用户ID（action=TRANSFER 时必填）
     */
    private Long transferToId;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

}
