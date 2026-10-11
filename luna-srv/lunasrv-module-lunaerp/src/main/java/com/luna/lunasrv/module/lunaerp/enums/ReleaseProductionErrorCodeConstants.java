package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 生产管理 - 下达生产编排错误码常量
 *
 * 规范段位：1-030-035-xxx
 *
 * @author luna
 */
public interface ReleaseProductionErrorCodeConstants {

    ErrorCode SALES_CONTRACT_NOT_EXISTS = new ErrorCode(1_030_035_000, "销售合同不存在");
    ErrorCode SALES_CONTRACT_CANCELLED = new ErrorCode(1_030_035_001, "合同已取消，不能下达生产");
    ErrorCode CONTRACT_LINE_OVER_RELEASED = new ErrorCode(1_030_035_002, "合同行【{}】已下达量超出合同数量（超下达或并发冲突）");
    ErrorCode COLOR_INSTRUCTION_STATUS_NOT_ALLOWED = new ErrorCode(1_030_035_003, "打色单【{}】状态不允许被引用（须 CONFIRMED/IN_PRODUCTION）");
    ErrorCode COLOR_INSTRUCTION_LIGHT_PRIMARY_REQUIRED = new ErrorCode(1_030_035_004, "打色单颜色行主灯源不能为空");
    ErrorCode FINISHED_INSPECTION_ORDER_ALREADY_EXISTS = new ErrorCode(1_030_035_005, "存在相同染色单的成品检验单，请勿重复创建");

    // 语义别名
    ErrorCode CONTRACT_NOT_EXISTS = SALES_CONTRACT_NOT_EXISTS;
    ErrorCode CONTRACT_CANCELLED = SALES_CONTRACT_CANCELLED;
    ErrorCode COLOR_INSTRUCTION_STATUS_NOT_ALLOWED_FOR_REFERENCE = COLOR_INSTRUCTION_STATUS_NOT_ALLOWED;
    ErrorCode COLOR_INSTRUCTION_LIGHT_PRIMARY_EMPTY = COLOR_INSTRUCTION_LIGHT_PRIMARY_REQUIRED;
    ErrorCode FINISHED_INSPECTION_ALREADY_EXISTS = FINISHED_INSPECTION_ORDER_ALREADY_EXISTS;

}
