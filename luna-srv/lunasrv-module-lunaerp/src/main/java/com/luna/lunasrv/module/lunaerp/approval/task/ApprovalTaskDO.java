package com.luna.lunasrv.module.lunaerp.approval.task;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 审批任务表 DO
 *
 * 表：lunaerp_approval_task
 *
 * @author luna
 */
@TableName("lunaerp_approval_task")
@KeySequence("lunaerp_approval_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTaskDO extends TenantBaseDO {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DONE = "DONE";

    public static final String ACTION_DONE = "DONE";
    public static final String ACTION_REJECTED = "REJECTED";
    public static final String ACTION_TRANSFERRED = "TRANSFERRED";

    /**
     * 审批任务ID
     */
    @TableId
    private Long id;

    /**
     * 关联审批实例ID（lunaerp_approval_instance.id）
     */
    private Long instanceId;

    /**
     * 节点序号
     */
    private Integer nodeSort;

    /**
     * 节点名称快照
     */
    private String nodeName;

    /**
     * 节点类型快照（SEQUENTIAL / OR_SIGN / AND_SIGN）
     */
    private String nodeType;

    /**
     * 当前审批人用户ID
     */
    private Long approverId;

    /**
     * 任务状态（PENDING 待处理 / DONE 已处理）
     */
    private String status;

    /**
     * 处理动作（空 / DONE 通过 / REJECTED 驳回 / TRANSFERRED 转交）
     */
    private String action;

    /**
     * 任务到达时间
     */
    private LocalDateTime startTime;

    /**
     * 任务完成时间
     */
    private LocalDateTime endTime;

    /**
     * 预留 Flowable 任务ID（未来回迁BPM时回填）
     */
    private String flowableTaskId;

    /**
     * 乐观锁版本号（会签并发控制）
     */
    @Version
    private Integer version;

}
