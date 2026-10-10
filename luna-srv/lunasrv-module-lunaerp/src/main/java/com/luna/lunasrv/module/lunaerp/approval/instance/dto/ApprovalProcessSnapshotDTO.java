package com.luna.lunasrv.module.lunaerp.approval.instance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流程定义全量快照 DTO（序列化存入 lunaerp_approval_instance.process_snapshot）
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalProcessSnapshotDTO {

    /**
     * 流程配置ID
     */
    private Long configId;

    /**
     * 流程配置名称
     */
    private String configName;

    /**
     * 业务单据类型
     */
    private String bizType;

    /**
     * 节点定义与解析审批人快照列表
     */
    private List<ApprovalNodeSnapshotDTO> nodes;

}
