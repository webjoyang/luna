package com.luna.lunasrv.module.lunaerp.approval.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 审批通过完成事件（对齐 ADR-0017 与 Issue 07）
 *
 * 审批实例所有节点通过（或无下一节点）时，在审批事务内发布此事件，
 * 触发各业务单据自身生效/过账动作。
 *
 * @author luna
 */
@Getter
public class ApprovalCompletedEvent extends ApplicationEvent {

    /**
     * 单据类型（如 sales_contract 等）
     */
    private final String bizType;

    /**
     * 业务单据ID
     */
    private final Long bizId;

    public ApprovalCompletedEvent(Object source, String bizType, Long bizId) {
        super(source);
        this.bizType = bizType;
        this.bizId = bizId;
    }

}
