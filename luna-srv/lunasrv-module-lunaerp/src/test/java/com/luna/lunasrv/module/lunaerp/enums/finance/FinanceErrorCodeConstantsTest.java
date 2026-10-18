package com.luna.lunasrv.module.lunaerp.enums.finance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 财务域错误码常量测试（供应商对账 1-030-022 与收付款 1-030-023）
 */
public class FinanceErrorCodeConstantsTest {

    @Test
    @DisplayName("验证供应商对账错误码 1-030-022-000 ~ 007")
    void testSupplierReconErrorCodes() {
        assertEquals(1_030_022_000, FinanceErrorCodeConstants.SUPPLIER_RECON_NOT_EXISTS.getCode());
        assertEquals("对账单不存在", FinanceErrorCodeConstants.SUPPLIER_RECON_NOT_EXISTS.getMsg());

        assertEquals(1_030_022_001, FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode());
        assertEquals("当前状态不允许该操作", FinanceErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getMsg());

        assertEquals(1_030_022_002, FinanceErrorCodeConstants.SUPPLIER_RECON_SUPPLIER_NOT_EXISTS.getCode());
        assertEquals("供应商不存在", FinanceErrorCodeConstants.SUPPLIER_RECON_SUPPLIER_NOT_EXISTS.getMsg());

        assertEquals(1_030_022_003, FinanceErrorCodeConstants.SUPPLIER_RECON_NO_SOURCE_ITEMS.getCode());
        assertEquals("对账区间内无可对账明细", FinanceErrorCodeConstants.SUPPLIER_RECON_NO_SOURCE_ITEMS.getMsg());

        assertEquals(1_030_022_004, FinanceErrorCodeConstants.SUPPLIER_RECON_SOURCE_REFERENCED.getCode());
        assertEquals("源明细已被对账单【{}】引用", FinanceErrorCodeConstants.SUPPLIER_RECON_SOURCE_REFERENCED.getMsg());
        assertTrue(FinanceErrorCodeConstants.SUPPLIER_RECON_SOURCE_REFERENCED.getMsg().contains("【{}】"));

        assertEquals(1_030_022_005, FinanceErrorCodeConstants.SUPPLIER_RECON_ADJUST_LINE_INVALID.getCode());
        assertEquals("调整项行不合法：金额为 0 或缺备注", FinanceErrorCodeConstants.SUPPLIER_RECON_ADJUST_LINE_INVALID.getMsg());

        assertEquals(1_030_022_006, FinanceErrorCodeConstants.SUPPLIER_RECON_HAS_PAYMENT_APPLY.getCode());
        assertEquals("对账单存在付款申请，禁止作废", FinanceErrorCodeConstants.SUPPLIER_RECON_HAS_PAYMENT_APPLY.getMsg());

        assertEquals(1_030_022_007, FinanceErrorCodeConstants.SUPPLIER_RECON_APPLY_AMOUNT_EXCEED.getCode());
        assertEquals("累计申请金额【{}】超出对账确认金额", FinanceErrorCodeConstants.SUPPLIER_RECON_APPLY_AMOUNT_EXCEED.getMsg());
        assertTrue(FinanceErrorCodeConstants.SUPPLIER_RECON_APPLY_AMOUNT_EXCEED.getMsg().contains("【{}】"));
    }

    @Test
    @DisplayName("验证收付款与核销错误码 1-030-023-000 ~ 007")
    void testPaymentErrorCodes() {
        assertEquals(1_030_023_000, FinanceErrorCodeConstants.PAYMENT_APPLY_NOT_EXISTS.getCode());
        assertEquals("付款申请不存在", FinanceErrorCodeConstants.PAYMENT_APPLY_NOT_EXISTS.getMsg());

        assertEquals(1_030_023_001, FinanceErrorCodeConstants.PAYMENT_APPLY_STATUS_NOT_ALLOWED.getCode());
        assertEquals("付款申请当前状态不允许该操作", FinanceErrorCodeConstants.PAYMENT_APPLY_STATUS_NOT_ALLOWED.getMsg());

        assertEquals(1_030_023_002, FinanceErrorCodeConstants.PAYMENT_APPLY_RECON_NOT_CONFIRMED.getCode());
        assertEquals("对账单未确认，不可生成对账付款申请", FinanceErrorCodeConstants.PAYMENT_APPLY_RECON_NOT_CONFIRMED.getMsg());

        assertEquals(1_030_023_003, FinanceErrorCodeConstants.PAYMENT_ALLOCATE_EXCEED.getCode());
        assertEquals("核销分配金额超过付款申请剩余额", FinanceErrorCodeConstants.PAYMENT_ALLOCATE_EXCEED.getMsg());

        assertEquals(1_030_023_004, FinanceErrorCodeConstants.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH.getCode());
        assertEquals("资金账户余额不足", FinanceErrorCodeConstants.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH.getMsg());

        assertEquals(1_030_023_005, FinanceErrorCodeConstants.PAYMENT_PREPAY_BALANCE_NOT_ENOUGH.getCode());
        assertEquals("供应商预付余额不足", FinanceErrorCodeConstants.PAYMENT_PREPAY_BALANCE_NOT_ENOUGH.getMsg());

        assertEquals(1_030_023_006, FinanceErrorCodeConstants.PAYMENT_ACCOUNT_NOT_AVAILABLE.getCode());
        assertEquals("资金账户不存在或已停用", FinanceErrorCodeConstants.PAYMENT_ACCOUNT_NOT_AVAILABLE.getMsg());

        assertEquals(1_030_023_007, FinanceErrorCodeConstants.PAYMENT_AMOUNT_INVALID.getCode());
        assertEquals("付款金额不合法", FinanceErrorCodeConstants.PAYMENT_AMOUNT_INVALID.getMsg());

        assertEquals(1_030_023_008, FinanceErrorCodeConstants.PAYMENT_APPLY_NO_EXHAUSTED.getCode());
        assertEquals("付款申请单号生成重试耗尽", FinanceErrorCodeConstants.PAYMENT_APPLY_NO_EXHAUSTED.getMsg());

        assertEquals(1_030_023_009, FinanceErrorCodeConstants.PAYMENT_NOT_EXISTS.getCode());
        assertEquals("收付款单不存在", FinanceErrorCodeConstants.PAYMENT_NOT_EXISTS.getMsg());

        assertEquals(1_030_023_010, FinanceErrorCodeConstants.PAYMENT_NO_EXHAUSTED.getCode());
        assertEquals("收付款单号生成重试耗尽", FinanceErrorCodeConstants.PAYMENT_NO_EXHAUSTED.getMsg());
    }

    @Test
    @DisplayName("验证域公共错误码 1-030-020-xxx（分录/账户/汇率）")
    void testCommonFinanceErrorCodes() {
        assertEquals(1_030_020_000, FinanceErrorCodeConstants.LEDGER_IMMUTABLE.getCode());
        assertEquals("分录账禁止修改或删除", FinanceErrorCodeConstants.LEDGER_IMMUTABLE.getMsg());

        assertEquals(1_030_020_100, FinanceErrorCodeConstants.ACCOUNT_NOT_EXISTS.getCode());
        assertEquals(1_030_020_101, FinanceErrorCodeConstants.ACCOUNT_DISABLED.getCode());
        assertEquals(1_030_020_102, FinanceErrorCodeConstants.ACCOUNT_NAME_DUPLICATE.getCode());
        assertEquals(1_030_020_103, FinanceErrorCodeConstants.ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN.getCode());
        assertEquals(1_030_020_104, FinanceErrorCodeConstants.ACCOUNT_OPENING_BALANCE_IMMUTABLE.getCode());

        assertEquals(1_030_020_200, FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND.getCode());
        assertEquals(1_030_020_201, FinanceErrorCodeConstants.EXCHANGE_RATE_NO_DUPLICATE.getCode());
        assertEquals(1_030_020_202, FinanceErrorCodeConstants.EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("验证客户对账错误码 1-030-021-000 ~ 007")
    void testCustomerReconErrorCodes() {
        assertEquals(1_030_021_000, FinanceErrorCodeConstants.STATEMENT_NOT_EXISTS.getCode());
        assertEquals(1_030_021_001, FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode());
        assertEquals(1_030_021_002, FinanceErrorCodeConstants.STATEMENT_NO_PERIOD_ENTRIES.getCode());
        assertEquals(1_030_021_003, FinanceErrorCodeConstants.STATEMENT_ENTRY_OCCUPIED.getCode());
        assertEquals(1_030_021_004, FinanceErrorCodeConstants.STATEMENT_CONFIRMED_NOT_VOID.getCode());
        assertEquals(1_030_021_005, FinanceErrorCodeConstants.STATEMENT_CURRENCY_MIXED.getCode());
        assertEquals(1_030_021_006, FinanceErrorCodeConstants.STATEMENT_CUSTOMER_MISMATCH.getCode());
        assertEquals(1_030_021_007, FinanceErrorCodeConstants.STATEMENT_PERIOD_INVALID.getCode());
        assertEquals(1_030_021_008, FinanceErrorCodeConstants.STATEMENT_NO_EXHAUSTED.getCode());
    }

    @Test
    @DisplayName("验证收款单（IN侧）错误码 1-030-023-100 ~ 107")
    void testReceiptErrorCodes() {
        assertEquals(1_030_023_100, FinanceErrorCodeConstants.RECEIPT_NOT_EXISTS.getCode());
        assertEquals(1_030_023_101, FinanceErrorCodeConstants.RECEIPT_STATUS_NOT_ALLOW.getCode());
        assertEquals(1_030_023_102, FinanceErrorCodeConstants.RECEIPT_CUSTOMER_MISMATCH.getCode());
        assertEquals(1_030_023_103, FinanceErrorCodeConstants.RECEIPT_CURRENCY_MISMATCH.getCode());
        assertEquals(1_030_023_104, FinanceErrorCodeConstants.RECEIPT_ALLOCATE_EXCEED.getCode());
        assertEquals(1_030_023_105, FinanceErrorCodeConstants.RECEIPT_POST_CONFLICT.getCode());
        assertEquals(1_030_023_106, FinanceErrorCodeConstants.RECEIPT_ADVANCE_BALANCE_NOT_ENOUGH.getCode());
        assertEquals(1_030_023_107, FinanceErrorCodeConstants.RECEIPT_REVERSAL_NOT_ALLOW.getCode());
    }

    @Test
    @DisplayName("验证发票申请错误码 1-030-026-000 ~ 007")
    void testInvoiceErrorCodes() {
        assertEquals(1_030_026_000, FinanceErrorCodeConstants.INVOICE_NOT_EXISTS.getCode());
        assertEquals(1_030_026_001, FinanceErrorCodeConstants.INVOICE_STATUS_NOT_ALLOW.getCode());
        assertEquals(1_030_026_002, FinanceErrorCodeConstants.INVOICE_EXCEED_SHIPMENT_AMOUNT.getCode());
        assertEquals(1_030_026_003, FinanceErrorCodeConstants.INVOICE_CURRENCY_MIXED.getCode());
        assertEquals(1_030_026_004, FinanceErrorCodeConstants.INVOICE_CUSTOMER_MISMATCH.getCode());
        assertEquals(1_030_026_005, FinanceErrorCodeConstants.INVOICE_OFFICIAL_NO_REQUIRED.getCode());
        assertEquals(1_030_026_006, FinanceErrorCodeConstants.INVOICE_OFFICIAL_NO_DUPLICATE.getCode());
        assertEquals(1_030_026_007, FinanceErrorCodeConstants.INVOICE_TAX_RATE_INVALID.getCode());
    }
}
