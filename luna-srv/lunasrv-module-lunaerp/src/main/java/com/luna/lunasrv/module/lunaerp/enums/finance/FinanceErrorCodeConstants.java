package com.luna.lunasrv.module.lunaerp.enums.finance;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * LunaERP 财务域错误码常量
 *
 * 规范段位：
 * - 1-030-022-xxx：供应商对账（000–007 本 spec 独占）
 * - 1-030-023-xxx：收付款与核销（000–007 payment 共享段先占位，100–199 为 10 号收款侧避让段）
 *
 * @author luna
 */
public interface FinanceErrorCodeConstants {

    // ========== 域公共 1-030-020-000 ~ 299 ==========
    // 000 分录账
    ErrorCode LEDGER_IMMUTABLE = new ErrorCode(1_030_020_000, "分录账禁止修改或删除");
    // 100~104 资金账户
    ErrorCode ACCOUNT_NOT_EXISTS = new ErrorCode(1_030_020_100, "资金账户不存在");
    ErrorCode ACCOUNT_DISABLED = new ErrorCode(1_030_020_101, "资金账户已停用");
    ErrorCode ACCOUNT_NAME_DUPLICATE = new ErrorCode(1_030_020_102, "资金账户名称已存在");
    ErrorCode ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN = new ErrorCode(1_030_020_103, "资金账户存在分录引用，禁止删除");
    ErrorCode ACCOUNT_OPENING_BALANCE_IMMUTABLE = new ErrorCode(1_030_020_104, "资金账户期初余额仅创建可录入，禁止修改");
    // 200~202 汇率
    ErrorCode EXCHANGE_RATE_NOT_FOUND = new ErrorCode(1_030_020_200, "未找到生效汇率");
    ErrorCode EXCHANGE_RATE_NO_DUPLICATE = new ErrorCode(1_030_020_201, "同币种同生效日汇率已存在");
    ErrorCode EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN = new ErrorCode(1_030_020_202, "本位币无需配置汇率");

    // ========== 客户对账 1-030-021-000 ~ 099 ==========
    ErrorCode STATEMENT_NOT_EXISTS = new ErrorCode(1_030_021_000, "对账单不存在");
    ErrorCode STATEMENT_STATUS_NOT_ALLOW = new ErrorCode(1_030_021_001, "对账单当前状态不允许该操作");
    ErrorCode STATEMENT_NO_PERIOD_ENTRIES = new ErrorCode(1_030_021_002, "期间无可对账分录");
    ErrorCode STATEMENT_ENTRY_OCCUPIED = new ErrorCode(1_030_021_003, "明细已被其他对账单占用");
    ErrorCode STATEMENT_CONFIRMED_NOT_VOID = new ErrorCode(1_030_021_004, "已确认对账单不可作废");
    ErrorCode STATEMENT_CURRENCY_MIXED = new ErrorCode(1_030_021_005, "多币种需分单生成对账单");
    ErrorCode STATEMENT_CUSTOMER_MISMATCH = new ErrorCode(1_030_021_006, "客户信息不匹配");
    ErrorCode STATEMENT_PERIOD_INVALID = new ErrorCode(1_030_021_007, "对账起止日期非法");
    ErrorCode STATEMENT_NO_EXHAUSTED = new ErrorCode(1_030_021_008, "对账单号生成重试耗尽");

    // ========== 供应商对账 1-030-022-000 ~ 099 ==========
    ErrorCode SUPPLIER_RECON_NOT_EXISTS = new ErrorCode(1_030_022_000, "对账单不存在");
    ErrorCode SUPPLIER_RECON_STATUS_NOT_ALLOWED = new ErrorCode(1_030_022_001, "当前状态不允许该操作");
    ErrorCode SUPPLIER_RECON_SUPPLIER_NOT_EXISTS = new ErrorCode(1_030_022_002, "供应商不存在");
    ErrorCode SUPPLIER_RECON_NO_SOURCE_ITEMS = new ErrorCode(1_030_022_003, "对账区间内无可对账明细");
    ErrorCode SUPPLIER_RECON_SOURCE_REFERENCED = new ErrorCode(1_030_022_004, "源明细已被对账单【{}】引用");
    ErrorCode SUPPLIER_RECON_ADJUST_LINE_INVALID = new ErrorCode(1_030_022_005, "调整项行不合法：金额为 0 或缺备注");
    ErrorCode SUPPLIER_RECON_HAS_PAYMENT_APPLY = new ErrorCode(1_030_022_006, "对账单存在付款申请，禁止作废");
    ErrorCode SUPPLIER_RECON_APPLY_AMOUNT_EXCEED = new ErrorCode(1_030_022_007, "累计申请金额【{}】超出对账确认金额");

    // ========== 收付款与核销 1-030-023-000 ~ 099 (OUT侧与共享) ==========
    ErrorCode PAYMENT_APPLY_NOT_EXISTS = new ErrorCode(1_030_023_000, "付款申请不存在");
    ErrorCode PAYMENT_APPLY_STATUS_NOT_ALLOWED = new ErrorCode(1_030_023_001, "付款申请当前状态不允许该操作");
    ErrorCode PAYMENT_APPLY_RECON_NOT_CONFIRMED = new ErrorCode(1_030_023_002, "对账单未确认，不可生成对账付款申请");
    ErrorCode PAYMENT_ALLOCATE_EXCEED = new ErrorCode(1_030_023_003, "核销分配金额超过付款申请剩余额");
    ErrorCode PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH = new ErrorCode(1_030_023_004, "资金账户余额不足");
    ErrorCode PAYMENT_PREPAY_BALANCE_NOT_ENOUGH = new ErrorCode(1_030_023_005, "供应商预付余额不足");
    ErrorCode PAYMENT_ACCOUNT_NOT_AVAILABLE = new ErrorCode(1_030_023_006, "资金账户不存在或已停用");
    ErrorCode PAYMENT_AMOUNT_INVALID = new ErrorCode(1_030_023_007, "付款金额不合法");
    ErrorCode PAYMENT_APPLY_NO_EXHAUSTED = new ErrorCode(1_030_023_008, "付款申请单号生成重试耗尽");
    ErrorCode PAYMENT_NOT_EXISTS = new ErrorCode(1_030_023_009, "收付款单不存在");
    ErrorCode PAYMENT_NO_EXHAUSTED = new ErrorCode(1_030_023_010, "收付款单号生成重试耗尽");

    // ========== 收款单（IN侧）1-030-023-100 ~ 199 ==========
    ErrorCode RECEIPT_NOT_EXISTS = new ErrorCode(1_030_023_100, "收款单不存在");
    ErrorCode RECEIPT_STATUS_NOT_ALLOW = new ErrorCode(1_030_023_101, "收款单当前状态不允许该操作");
    ErrorCode RECEIPT_CUSTOMER_MISMATCH = new ErrorCode(1_030_023_102, "收款客户不匹配");
    ErrorCode RECEIPT_CURRENCY_MISMATCH = new ErrorCode(1_030_023_103, "核销严格同币种");
    ErrorCode RECEIPT_ALLOCATE_EXCEED = new ErrorCode(1_030_023_104, "分配合计超收款额");
    ErrorCode RECEIPT_POST_CONFLICT = new ErrorCode(1_030_023_105, "过账并发冲突");
    ErrorCode RECEIPT_ADVANCE_BALANCE_NOT_ENOUGH = new ErrorCode(1_030_023_106, "预收余额不足");
    ErrorCode RECEIPT_REVERSAL_NOT_ALLOW = new ErrorCode(1_030_023_107, "收款单不允许红冲");

    // ========== 发票申请 1-030-026-000 ~ 099 ==========
    ErrorCode INVOICE_NOT_EXISTS = new ErrorCode(1_030_026_000, "发票申请不存在");
    ErrorCode INVOICE_STATUS_NOT_ALLOW = new ErrorCode(1_030_026_001, "发票申请当前状态不允许该操作");
    ErrorCode INVOICE_EXCEED_SHIPMENT_AMOUNT = new ErrorCode(1_030_026_002, "累计开票超发货可开票额");
    ErrorCode INVOICE_CURRENCY_MIXED = new ErrorCode(1_030_026_003, "发货单跨币种混选");
    ErrorCode INVOICE_CUSTOMER_MISMATCH = new ErrorCode(1_030_026_004, "开票客户不匹配");
    ErrorCode INVOICE_OFFICIAL_NO_REQUIRED = new ErrorCode(1_030_026_005, "登记必填发票号");
    ErrorCode INVOICE_OFFICIAL_NO_DUPLICATE = new ErrorCode(1_030_026_006, "发票号码已存在");
    ErrorCode INVOICE_TAX_RATE_INVALID = new ErrorCode(1_030_026_007, "开票税率不合法");
    ErrorCode INVOICE_NO_EXHAUSTED = new ErrorCode(1_030_026_008, "发票申请单号生成重试耗尽");
}
