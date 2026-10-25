package com.luna.lunasrv.module.lunaerp.approval.instance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 审批节点快照 DTO（含解析后冻结的审批人ID列表）
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalNodeSnapshotDTO {

    /**
     * 节点配置ID
     */
    private Long nodeConfigId;

    /**
     * 节点序号
     */
    private Integer sort;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点类型（SEQUENTIAL / OR_SIGN / AND_SIGN）
     */
    private String nodeType;

    /**
     * 审批人来源（USER / POST / ROLE / DEPT_LEADER / SUBMITTER_LEADER）
     */
    private String approverType;

    /**
     * 解析后冻结的审批人用户ID列表
     */
    private List<Long> approverUserIds;

}
