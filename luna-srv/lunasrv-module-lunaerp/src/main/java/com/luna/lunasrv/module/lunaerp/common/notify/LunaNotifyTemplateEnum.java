package com.luna.lunasrv.module.lunaerp.common.notify;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * LunaERP 站内信模板枚举
 *
 * 包含 W0 实装的 3 个审批模板以及后续波次冻结契约位（见 ADR-0038 与 shared GLOSSARY）
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum LunaNotifyTemplateEnum {

    // ========== W0 实装审批模板 ==========

    /**
     * 审批待办通知（节点到达，发送给该节点各审批人）
     */
    LUNA_APPROVAL_TODO(
            "LUNA_APPROVAL_TODO",
            "审批待办通知",
            "您有新的审批待办：{bizName} 单号 {bizNo}，提交人 {submitterName}"
    ),

    /**
     * 审批通过通知（审批实例终态通过，发送给提交人）
     */
    LUNA_APPROVAL_APPROVED(
            "LUNA_APPROVAL_APPROVED",
            "审批通过通知",
            "您的审批已通过：{bizName} 单号 {bizNo}"
    ),

    /**
     * 审批驳回通知（审批实例终态驳回，发送给提交人，内容带驳回意见）
     */
    LUNA_APPROVAL_REJECTED(
            "LUNA_APPROVAL_REJECTED",
            "审批驳回通知",
            "您的审批被驳回：{bizName} 单号 {bizNo}，驳回意见：{reason}"
    ),

    // ========== 冻结契约位（一期不接线，模板文案保留待二期） ==========

    /**
     * 销售合同生效通知（合同审批通过生效，发送给销售员/销售主管，随 W1 合同接入）
     */
    LUNA_SALES_CONTRACT_ACTIVE(
            "LUNA_SALES_CONTRACT_ACTIVE",
            "销售合同生效通知",
            "销售合同已生效：合同编号 {bizNo}"
    ),

    /**
     * 发货单签收通知（发货单签收，发送给销售员，随发货/门户接入）
     */
    LUNA_SHIPMENT_SIGNED(
            "LUNA_SHIPMENT_SIGNED",
            "发货单签收通知",
            "发货单已签收：发货单号 {bizNo}"
    ),

    /**
     * 应收账期逾期/即将到期定时预警（发送给财务/业务员，一期不接线，模板文案保留待二期）
     */
    LUNA_AR_PERIOD_WARNING(
            "LUNA_AR_PERIOD_WARNING",
            "应收账期预警通知",
            "客户账期预警：客户 {customerName} 单号 {bizNo} 即将到期或已逾期"
    );

    /**
     * 模板编码
     */
    private final String code;

    /**
     * 模板名称
     */
    private final String name;

    /**
     * 模板内容
     */
    private final String content;

    /**
     * 根据模板编码查找枚举
     */
    public static LunaNotifyTemplateEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LunaNotifyTemplateEnum item : values()) {
            if (item.getCode().equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }
}
