package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产管理 - 机台任务 DO
 *
 * 织造单排产到某台织机的派工任务——"哪台机织多少匹"的载体。
 * 派生编号规则：MT-{织造单号去符号}-{单内 2 位序号}，全量唯一含软删行（ADR-0023）。
 *
 * @author luna
 */
@TableName("lunaerp_production_machine_task")
@KeySequence("lunaerp_production_machine_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineTaskDO extends TenantBaseDO {

    /**
     * 机台任务ID
     */
    @TableId
    private Long id;

    /**
     * 任务编号（MT-{织造单号去符号}-{单内 2 位序号}）
     */
    private String code;

    /**
     * 关联织造单ID
     */
    private Long weavingOrderId;

    /**
     * 关联织机ID（仅 ACTIVE 织机可选）
     */
    private Long loomId;

    /**
     * 机台号快照
     */
    private String loomNo;

    /**
     * 挡车工用户ID（关联 system User，可空，ADR-0021）
     */
    private Long workerUserId;

    /**
     * 挡车工姓名快照
     */
    private String workerName;

    /**
     * 计划匹数（正整数）
     */
    private Integer plannedQty;

    /**
     * 完工/实织匹数（缺省 0，布票聚合只读）
     */
    private Integer actualQty;

    /**
     * 计划开始日期（普通日期可空）
     */
    private LocalDate plannedStart;

    /**
     * 计划结束日期（普通日期可空，end ≥ start）
     */
    private LocalDate plannedEnd;

    /**
     * 任务状态（字典 lunaerp_weaving_task_status：PENDING/IN_PROGRESS/HALTED/COMPLETED，缺省 PENDING）
     */
    private String status;

    /**
     * 停机原因
     */
    private String haltReason;

    /**
     * 停机时间
     */
    private LocalDateTime haltAt;

    /**
     * 备注
     */
    private String remark;

}
