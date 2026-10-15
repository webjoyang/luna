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

    // ========== 供应商对账 1-030-022-000 ~ 099 ==========
    ErrorCode SUPPLIER_RECON_NOT_EXISTS = new ErrorCode(1_030_022_000, "对账单不存在");
    ErrorCode SUPPLIER_RECON_STATUS_NOT_ALLOWED = new ErrorCode(1_030_022_001, "当前状态不允许该操作");
    ErrorCode SUPPLIER_RECON_SUPPLIER_NOT_EXISTS = new ErrorCode(1_030_022_002, "供应商不存在");
    ErrorCode SUPPLIER_RECON_NO_SOURCE_ITEMS = new ErrorCode(1_030_022_003, "对账区间内无可对账明细");
    ErrorCode SUPPLIER_RECON_SOURCE_REFERENCED = new ErrorCode(1_030_022_004, "源明细已被对账单【{}】引用");
    ErrorCode SUPPLIER_RECON_ADJUST_LINE_INVALID = new ErrorCode(1_030_022_005, "调整项行不合法：金额为 0 或缺备注");
    ErrorCode SUPPLIER_RECON_HAS_PAYMENT_APPLY = new ErrorCode(1_030_022_006, "对账单存在付款申请，禁止作废");
    ErrorCode SUPPLIER_RECON_APPLY_AMOUNT_EXCEED = new ErrorCode(1_030_022_007, "累计申请金额【{}】超出对账确认金额");

    // ========== 收付款与核销 1-030-023-000 ~ 099 ==========
    ErrorCode PAYMENT_APPLY_NOT_EXISTS = new ErrorCode(1_030_023_000, "付款申请不存在");
    ErrorCode PAYMENT_APPLY_STATUS_NOT_ALLOWED = new ErrorCode(1_030_023_001, "付款申请当前状态不允许该操作");
    ErrorCode PAYMENT_APPLY_RECON_NOT_CONFIRMED = new ErrorCode(1_030_023_002, "对账单未确认，不可生成对账付款申请");
    ErrorCode PAYMENT_ALLOCATE_EXCEED = new ErrorCode(1_030_023_003, "核销分配金额超过付款申请剩余额");
    ErrorCode PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH = new ErrorCode(1_030_023_004, "资金账户余额不足");
    ErrorCode PAYMENT_PREPAY_BALANCE_NOT_ENOUGH = new ErrorCode(1_030_023_005, "供应商预付余额不足");
    ErrorCode PAYMENT_ACCOUNT_NOT_AVAILABLE = new ErrorCode(1_030_023_006, "资金账户不存在或已停用");
    ErrorCode PAYMENT_AMOUNT_INVALID = new ErrorCode(1_030_023_007, "付款金额不合法");

}
