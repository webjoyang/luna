package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * LunaERP 错误码枚举类
 *
 * lunaerp 系统，使用 1-030-001-xxx 段（产品）
 *                  与 1-030-002-xxx 段（胚布品种，见 GreigeErrorCodeConstants）
 *                  与 1-030-004-xxx 段（客户，customer 与 customer-contact 共用）
 *                  与 1-030-003-xxx 段（销售合同）
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

    // ========== 客户模块 1-030-004-000（customer 与 customer-contact 共用） ==========
    // 注：1-030-002 段已由胚布品种先行占用（GreigeErrorCodeConstants，随产品模块迁移上线），
    // 客户作为后开发模块改用 004 段，避免整段重号。
    ErrorCode CUSTOMER_NOT_EXISTS = new ErrorCode(1_030_004_000, "客户不存在");
    ErrorCode CUSTOMER_CONTACT_NOT_EXISTS = new ErrorCode(1_030_004_001, "客户联系人不存在");

    // ========== 销售合同模块 1-030-003-000 ==========
    ErrorCode SALES_CONTRACT_NOT_EXISTS = new ErrorCode(1_030_003_000, "销售合同不存在");
    ErrorCode SALES_CONTRACT_NO_DUPLICATE = new ErrorCode(1_030_003_001, "合同编号已被占用，请修改编号或重新获取");

}
