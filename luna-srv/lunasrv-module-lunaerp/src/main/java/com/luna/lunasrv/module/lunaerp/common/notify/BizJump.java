package com.luna.lunasrv.module.lunaerp.common.notify;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 站内消息点击跳转上下文对象
 *
 * 零 DDL 设计：跳转信息随 templateParams JSON 携带，固定包含 bizType / bizId / bizNo。
 * 前端铃铛点击时根据前端维护的 bizType 路由表跳转至对应工作台或单据。
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BizJump implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务对象类型（如 APPROVAL、SALES_CONTRACT、SHIPMENT 等）
     */
    private String bizType;

    /**
     * 业务主键 ID（如审批实例 ID、合同 ID 等）
     */
    private Long bizId;

    /**
     * 业务单号（如合同号、发货单号等）
     */
    private String bizNo;

    /**
     * 构造跳转上下文
     *
     * @param bizType 业务类型
     * @param bizId   业务主键 ID
     * @param bizNo   业务单号
     * @return BizJump 实例
     */
    public static BizJump of(String bizType, Long bizId, String bizNo) {
        return BizJump.builder()
                .bizType(bizType)
                .bizId(bizId)
                .bizNo(bizNo)
                .build();
    }

    /**
     * 构造审批类跳转上下文（bizType="APPROVAL"）
     *
     * @param instanceId 审批实例 ID
     * @param bizNo      单据编号
     * @return BizJump 实例
     */
    public static BizJump ofApproval(Long instanceId, String bizNo) {
        return of("APPROVAL", instanceId, bizNo);
    }
}
