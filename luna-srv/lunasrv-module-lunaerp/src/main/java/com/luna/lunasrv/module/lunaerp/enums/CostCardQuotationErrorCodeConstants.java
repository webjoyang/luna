package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 售前链 - 成本卡与智能报价错误码常量
 *
 * 规范段位：1-030-034-xxx（成本卡 000–099 / 报价单 100–199）
 * 纱价追加段位：1-030-008-008 ~ 009（生产域既有纱线档案段追加）
 *
 * @author luna
 */
public interface CostCardQuotationErrorCodeConstants {

    // ========== 成本卡模块 1-030-034-000 ~ 099 ==========
    ErrorCode COST_CARD_NOT_EXISTS = new ErrorCode(1_030_034_000, "成本卡不存在");
    ErrorCode COST_CARD_NO_DUPLICATE = new ErrorCode(1_030_034_001, "成本卡号【{}】已存在");
    ErrorCode COST_CARD_STATUS_NOT_ALLOWED = new ErrorCode(1_030_034_002, "成本卡当前状态不允许该操作");
    ErrorCode COST_CARD_MISSING_PRICE = new ErrorCode(1_030_034_003, "成本卡存在 {} 个未取到价的纱线行，不能生效");
    ErrorCode COST_CARD_RECIPE_BOM_MISSING = new ErrorCode(1_030_034_004, "产品【{}】无有效配比 BOM 或 BOM 无明细");
    ErrorCode COST_CARD_KG_METER_CONVERSION_MISSING = new ErrorCode(1_030_034_005, "缺少克重/门幅，无法进行千克/米换算");

    // ========== 报价单模块 1-030-034-100 ~ 199 ==========
    ErrorCode QUOTATION_NOT_EXISTS = new ErrorCode(1_030_034_100, "报价单不存在");
    ErrorCode QUOTATION_NO_DUPLICATE = new ErrorCode(1_030_034_101, "报价单号【{}】已存在");
    ErrorCode QUOTATION_STATUS_NOT_ALLOWED = new ErrorCode(1_030_034_102, "报价单当前状态不允许该操作");
    ErrorCode QUOTATION_NOT_LATEST_VERSION = new ErrorCode(1_030_034_103, "仅最新已确认版本可修订/转合同");
    ErrorCode QUOTATION_DRAFT_EXISTS = new ErrorCode(1_030_034_104, "该报价号已存在草稿版本，请在草稿上继续编辑");
    ErrorCode QUOTATION_FX_RATE_REQUIRED = new ErrorCode(1_030_034_105, "外币报价必须填写汇率");
    ErrorCode QUOTATION_MARGIN_RATE_INVALID = new ErrorCode(1_030_034_106, "毛利率必须介于 -100% 与 100% 之间且不能等于 100%");
    ErrorCode QUOTATION_CURRENCY_MISMATCH = new ErrorCode(1_030_034_107, "报价币种【{}】与合同币种不一致，不能转合同");

    // ========== 纱价追加码（生产域 1-030-008-008 ~ 009） ==========
    ErrorCode YARN_PRICE_NOT_EXISTS = new ErrorCode(1_030_008_008, "纱价记录不存在");
    ErrorCode YARN_PRICE_DUPLICATE_DATE = new ErrorCode(1_030_008_009, "纱线规格【{}】的{}价在 {} 已存在记录");

}
