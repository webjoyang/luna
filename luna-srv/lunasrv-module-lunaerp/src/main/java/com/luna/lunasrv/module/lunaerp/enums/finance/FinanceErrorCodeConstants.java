package com.luna.lunasrv.module.lunaerp.enums.finance;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 财务域 - 供应商对账与收付款错误码常量
 *
 * 规范段位：
 * - 1-030-022-xxx：供应商对账（000–007）
 * - 1-030-023-xxx：收付款共享段（000–007，本 spec 先占）
 *
 * @author luna
 */
public interface FinanceErrorCodeConstants {

    // ========== 域公共模块（分录账 / 资金账户 / 汇率主档） 1-030-020-xxx ==========
    // --- 000 分录账 ---
    ErrorCode LEDGER_IMMUTABLE = new ErrorCode(1_030_020_000, "经营分录账禁止修改或删除");

    // --- 100-104 资金账户 ---
    ErrorCode ACCOUNT_NOT_EXISTS = new ErrorCode(1_030_020_100, "资金账户不存在");
    ErrorCode ACCOUNT_DISABLED = new ErrorCode(1_030_020_101, "资金账户已停用");
    ErrorCode ACCOUNT_NAME_DUPLICATE = new ErrorCode(1_030_020_102, "资金账户名称已存在");
    ErrorCode ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN = new ErrorCode(1_030_020_103, "资金账户已被分录引用，禁止删除");
    ErrorCode ACCOUNT_OPENING_BALANCE_IMMUTABLE = new ErrorCode(1_030_020_104, "期初余额仅创建时可录入，后续不可修改");

    // --- 200-202 汇率主档 ---
    ErrorCode EXCHANGE_RATE_NOT_FOUND = new ErrorCode(1_030_020_200, "未找到对应生效汇率");
    ErrorCode EXCHANGE_RATE_NO_DUPLICATE = new ErrorCode(1_030_020_201, "同币种同生效日汇率已存在");
    ErrorCode EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN = new ErrorCode(1_030_020_202, "本位币无需配置汇率");

    // ========== 客户对账模块 1-030-021-000 ~ 007 ==========
    ErrorCode STATEMENT_NOT_EXISTS = new ErrorCode(1_030_021_000, "对账单不存在");
    ErrorCode STATEMENT_STATUS_NOT_ALLOW = new ErrorCode(1_030_021_001, "当前状态不允许该操作");
    ErrorCode STATEMENT_NO_PERIOD_ENTRIES = new ErrorCode(1_030_021_002, "对账期间内无可对账分录");
    ErrorCode STATEMENT_ENTRY_OCCUPIED = new ErrorCode(1_030_021_003, "明细分录已被其他对账单占用");
    ErrorCode STATEMENT_CONFIRMED_NOT_VOID = new ErrorCode(1_030_021_004, "已确认对账单不可作废");
    ErrorCode STATEMENT_CURRENCY_MIXED = new ErrorCode(1_030_021_005, "多币种分录需分单生成对账单");
    ErrorCode STATEMENT_CUSTOMER_MISMATCH = new ErrorCode(1_030_021_006, "分录客户与对账单客户不一致");
    ErrorCode STATEMENT_PERIOD_INVALID = new ErrorCode(1_030_021_007, "对账起止日期非法");

    // ========== 供应商对账模块 1-030-022-000 ~ 007 ==========
    ErrorCode SUPPLIER_RECON_NOT_EXISTS = new ErrorCode(1_030_022_000, "对账单不存在");
    ErrorCode SUPPLIER_RECON_STATUS_NOT_ALLOWED = new ErrorCode(1_030_022_001, "当前状态不允许该操作");
    ErrorCode SUPPLIER_RECON_SUPPLIER_NOT_EXISTS = new ErrorCode(1_030_022_002, "供应商不存在");
    ErrorCode SUPPLIER_RECON_NO_SOURCE_ITEMS = new ErrorCode(1_030_022_003, "对账区间内无可对账明细");
    ErrorCode SUPPLIER_RECON_SOURCE_REFERENCED = new ErrorCode(1_030_022_004, "源明细已被对账单【{}】引用");
    ErrorCode SUPPLIER_RECON_ADJUST_LINE_INVALID = new ErrorCode(1_030_022_005, "调整项行不合法：金额为 0 或缺备注");
    ErrorCode SUPPLIER_RECON_HAS_PAYMENT_APPLY = new ErrorCode(1_030_022_006, "对账单存在付款申请，禁止作废");
    ErrorCode SUPPLIER_RECON_APPLY_AMOUNT_EXCEED = new ErrorCode(1_030_022_007, "累计申请金额【{}】超出对账确认金额");

    // ========== 收付款模块（共享段先占） 1-030-023-000 ~ 007 ==========
    ErrorCode PAYMENT_APPLY_NOT_EXISTS = new ErrorCode(1_030_023_000, "付款申请不存在");
    ErrorCode PAYMENT_APPLY_STATUS_NOT_ALLOWED = new ErrorCode(1_030_023_001, "付款申请当前状态不允许该操作");
    ErrorCode PAYMENT_APPLY_RECON_NOT_CONFIRMED = new ErrorCode(1_030_023_002, "对账单未确认，不可生成对账付款申请");
    ErrorCode PAYMENT_ALLOCATE_EXCEED = new ErrorCode(1_030_023_003, "核销分配金额超过付款申请剩余额");
    ErrorCode PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH = new ErrorCode(1_030_023_004, "资金账户余额不足");
    ErrorCode PAYMENT_PREPAY_BALANCE_NOT_ENOUGH = new ErrorCode(1_030_023_005, "供应商预付余额不足");
    ErrorCode PAYMENT_ACCOUNT_NOT_AVAILABLE = new ErrorCode(1_030_023_006, "资金账户不存在或已停用");
    ErrorCode PAYMENT_AMOUNT_INVALID = new ErrorCode(1_030_023_007, "付款金额不合法");
    ErrorCode PAYMENT_NOT_EXISTS = new ErrorCode(1_030_023_008, "收付款单不存在");

    // ========== 收付款模块（IN 侧收款段） 1-030-023-100 ~ 107 ==========
    ErrorCode RECEIPT_NOT_EXISTS = new ErrorCode(1_030_023_100, "收款单不存在");
    ErrorCode RECEIPT_STATUS_NOT_ALLOW = new ErrorCode(1_030_023_101, "收款单当前状态不允许该操作");
    ErrorCode RECEIPT_CUSTOMER_MISMATCH = new ErrorCode(1_030_023_102, "收款客户与核销分录客户不一致");
    ErrorCode RECEIPT_CURRENCY_MISMATCH = new ErrorCode(1_030_023_103, "收款币种与分录币种不一致，核销严格同币种");
    ErrorCode RECEIPT_ALLOCATE_EXCEED = new ErrorCode(1_030_023_104, "核销分配合计超出收款金额");
    ErrorCode RECEIPT_POST_CONFLICT = new ErrorCode(1_030_023_105, "收款过账并发冲突，请重试");
    ErrorCode RECEIPT_ADVANCE_BALANCE_NOT_ENOUGH = new ErrorCode(1_030_023_106, "预收余额不足以支持本次核销");
    ErrorCode RECEIPT_REVERSAL_NOT_ALLOW = new ErrorCode(1_030_023_107, "当前收款单状态不允许红冲");

    // ========== 发票申请模块 1-030-026-000 ~ 007 ==========
    ErrorCode INVOICE_NOT_EXISTS = new ErrorCode(1_030_026_000, "发票申请不存在");
    ErrorCode INVOICE_STATUS_NOT_ALLOW = new ErrorCode(1_030_026_001, "发票申请当前状态不允许该操作");
    ErrorCode INVOICE_EXCEED_SHIPMENT_AMOUNT = new ErrorCode(1_030_026_002, "累计申请开票金额超出发货单可开票金额");
    ErrorCode INVOICE_CURRENCY_MIXED = new ErrorCode(1_030_026_003, "发货单跨币种混选，开票严格同币种");
    ErrorCode INVOICE_CUSTOMER_MISMATCH = new ErrorCode(1_030_026_004, "发货单客户与发票申请客户不匹配");
    ErrorCode INVOICE_OFFICIAL_NO_REQUIRED = new ErrorCode(1_030_026_005, "登记发票必须填写发票号码");
    ErrorCode INVOICE_OFFICIAL_NO_DUPLICATE = new ErrorCode(1_030_026_006, "发票号码已存在");
    ErrorCode INVOICE_TAX_RATE_INVALID = new ErrorCode(1_030_026_007, "发票税率不合法");

}
