package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * LunaERP 错误码枚举类
 *
 * lunaerp 系统，使用 1-030-001-xxx 段（产品）
 *                  与 1-030-002-xxx 段（客户）
 */
public interface ErrorCodeConstants {

    // ========== 产品模块 1-030-001-000 ==========
    ErrorCode PRODUCT_NOT_EXISTS = new ErrorCode(1_030_001_000, "产品不存在");
    ErrorCode PRODUCT_CODE_DUPLICATE = new ErrorCode(1_030_001_001, "编号已被占用，请重新获取编号");
    ErrorCode PRODUCT_TAG_NOT_EXISTS = new ErrorCode(1_030_001_002, "产品标签不存在");
    ErrorCode PRODUCT_TAG_NAME_DUPLICATE = new ErrorCode(1_030_001_003, "已经存在名为【{}】的产品标签");
    ErrorCode PRODUCT_TAG_COLOR_INVALID = new ErrorCode(1_030_001_004, "标签颜色只能从预设色里选");
    ErrorCode PRODUCT_TAG_NAME_TOO_LONG = new ErrorCode(1_030_001_005, "标签最多 8 个字，请再精简一下");
    ErrorCode PRODUCT_TAG_LIMIT_EXCEEDED = new ErrorCode(1_030_001_006, "一个产品最多 20 个标签，当前 {} 个");
    ErrorCode PRODUCT_TAG_NAME_EMPTY = new ErrorCode(1_030_001_007, "标签内容不能为空");

    // ========== 客户模块 1-030-002-000 ==========
    ErrorCode CUSTOMER_NOT_EXISTS = new ErrorCode(1_030_002_000, "客户不存在");

}
