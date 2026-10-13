package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 生产管理 - 打色指示单错误码常量
 *
 * 规范段位：1-030-032-xxx（主表 000–099 / 颜色行 100–199）
 *
 * @author luna
 */
public interface ColorInstructionErrorCodeConstants {

    // ========== 打色指示单主表 1-030-032-000 ~ 099 ==========
    ErrorCode COLOR_INSTRUCTION_NOT_EXISTS = new ErrorCode(1_030_032_000, "打色单不存在");
    ErrorCode COLOR_INSTRUCTION_NO_DUPLICATE = new ErrorCode(1_030_032_001, "打色单号【{}】已存在");
    ErrorCode COLOR_INSTRUCTION_STATUS_NOT_ALLOWED = new ErrorCode(1_030_032_002, "打色单当前状态不允许该操作");
    ErrorCode COLOR_INSTRUCTION_REFERENCE_NOT_FOUND = new ErrorCode(1_030_032_003, "关联引用不存在（客户/产品/供应商）");
    ErrorCode COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET = new ErrorCode(1_030_032_004, "存在未决颜色行或无通过行，不能确认");
    ErrorCode COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE = new ErrorCode(1_030_032_005, "已被下游有效单据引用，不能作废或删除");
    ErrorCode COLOR_INSTRUCTION_LINE_FROZEN = new ErrorCode(1_030_032_006, "颜色行当前状态不允许修改或删除");
    ErrorCode COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT = new ErrorCode(1_030_032_007, "灯源或确认类型非法");

    // ========== 颜色行 1-030-032-100 ~ 199 ==========
    ErrorCode COLOR_INSTRUCTION_COLOR_NOT_EXISTS = new ErrorCode(1_030_032_100, "颜色行不存在");

}
