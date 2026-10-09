package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 审批引擎错误码枚举类
 *
 * 规范段位：1-030-030-xxx
 *
 * @author luna
 */
public interface ApprovalErrorCodeConstants {

    // ========== 审批流程配置与实例任务 1-030-030-000 ~ 009 ==========
    ErrorCode APPROVAL_CONFIG_NOT_EXISTS = new ErrorCode(1_030_030_000, "审批流程不存在");
    ErrorCode APPROVAL_CONFIG_NOT_ENABLED = new ErrorCode(1_030_030_001, "该单据类型无启用的审批流程");
    ErrorCode APPROVAL_INSTANCE_NOT_EXISTS = new ErrorCode(1_030_030_002, "审批实例不存在");
    ErrorCode APPROVAL_TASK_NOT_EXISTS = new ErrorCode(1_030_030_003, "审批任务不存在");
    ErrorCode APPROVAL_INSTANCE_STATUS_ERROR = new ErrorCode(1_030_030_004, "当前状态不允许该操作");
    ErrorCode APPROVAL_NO_PERMISSION = new ErrorCode(1_030_030_005, "非当前审批人无权操作");
    ErrorCode APPROVAL_REJECT_COMMENT_EMPTY = new ErrorCode(1_030_030_006, "驳回意见不能为空");
    ErrorCode APPROVAL_CONFIG_BIZ_TYPE_ALREADY_ENABLED = new ErrorCode(1_030_030_007, "该单据类型已有启用流程，请先停用");
    ErrorCode APPROVAL_CONFIG_HAS_ACTIVE_INSTANCE = new ErrorCode(1_030_030_008, "流程已被引用，无法删除");
    ErrorCode APPROVAL_TRANSFER_TO_SELF_NOT_ALLOWED = new ErrorCode(1_030_030_009, "转交接收人不能是自己");

}
