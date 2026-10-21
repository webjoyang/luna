package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产管理 - 织造布票 DO
 *
 * 织造执行最底层的每匹实体——"每匹一码"，票号即二维码内容。
 * 派生编号规则：BT-{机台任务号去符号}-{4 位流水}，单内流水计数，全量唯一含软删行（ADR-0023、ADR-0024）。
 *
 * @author luna
 */
@TableName(value = "lunaerp_production_weaving_ticket", autoResultMap = true)
@KeySequence("lunaerp_production_weaving_ticket_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeavingTicketDO extends TenantBaseDO {

    /**
     * 布票ID
     */
    @TableId
    private Long id;

    /**
     * 布票编号（BT-{机台任务号去符号}-{4 位流水}）
     */
    private String code;

    /**
     * 关联织造单ID
     */
    private Long weavingOrderId;

    /**
     * 关联机台任务ID
     */
    private Long machineTaskId;

    /**
     * 计划上机机台号快照（任务的 loomNo）
     */
    private String plannedMachine;

    /**
     * 实际上机机台号（开工时录入/确认）
     */
    private String actualMachine;

    /**
     * 单匹计划重量 KG（生成时按 织造单总重÷计划匹数 计算带入）
     */
    private BigDecimal plannedWeight;

    /**
     * 挡车工用户ID（关联 system User）
     */
    private Long workerUserId;

    /**
     * 挡车工姓名快照
     */
    private String workerName;

    /**
     * 原料批号
     */
    private String materialBatchNo;

    /**
     * 状态（字典 lunaerp_weaving_ticket_status：GENERATED/STARTED/COMPLETED/INSPECTED/STOCKED/EXCEPTION/VOID，缺省 GENERATED）
     */
    private String status;

    /**
     * 异常类型（字典 lunaerp_weaving_exception_type：YARN_BREAK/CLOTH_DAMAGE/YARN_SHORTAGE/MACHINE_CHANGE/WORKER_CHANGE）
     */
    private String exceptionType;

    /**
     * 异常说明
     */
    private String exceptionNote;

    /**
     * 异常现场照片 URL JSON 数组（车间 H5 异常登记上传，≤6 张，infra 文件服务）
     */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> exceptionPhotos;

    /**
     * 开工时间
     */
    private LocalDateTime startedAt;

    /**
     * 完工时间
     */
    private LocalDateTime completedAt;

    /**
     * 作废原因
     */
    private String voidReason;

    /**
     * 作废时间
     */
    private LocalDateTime voidedAt;

}
