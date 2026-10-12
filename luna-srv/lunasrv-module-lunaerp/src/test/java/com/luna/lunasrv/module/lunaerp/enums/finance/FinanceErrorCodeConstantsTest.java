package com.luna.lunasrv.module.lunaerp.enums.finance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 财务域错误码常量校验单元测试
 * 覆盖 Issue 01: 020, 021, 023 (100–107), 026 段
 */
public class FinanceErrorCodeConstantsTest {

    @Test
    @DisplayName("验证 020 域公共段错误码定义")
    void test020DomainCommonErrorCodes() {
        // 000 分录账
        assertNotNull(LEDGER_IMMUTABLE);
        assertEquals(1_030_020_000, LEDGER_IMMUTABLE.getCode());

        // 100-104 资金账户
        assertNotNull(ACCOUNT_NOT_EXISTS);
        assertEquals(1_030_020_100, ACCOUNT_NOT_EXISTS.getCode());
        assertNotNull(ACCOUNT_DISABLED);
        assertEquals(1_030_020_101, ACCOUNT_DISABLED.getCode());
        assertNotNull(ACCOUNT_NAME_DUPLICATE);
        assertEquals(1_030_020_102, ACCOUNT_NAME_DUPLICATE.getCode());
        assertNotNull(ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN);
        assertEquals(1_030_020_103, ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN.getCode());
        assertNotNull(ACCOUNT_OPENING_BALANCE_IMMUTABLE);
        assertEquals(1_030_020_104, ACCOUNT_OPENING_BALANCE_IMMUTABLE.getCode());

        // 200-202 汇率主档
        assertNotNull(EXCHANGE_RATE_NOT_FOUND);
        assertEquals(1_030_020_200, EXCHANGE_RATE_NOT_FOUND.getCode());
        assertNotNull(EXCHANGE_RATE_NO_DUPLICATE);
        assertEquals(1_030_020_201, EXCHANGE_RATE_NO_DUPLICATE.getCode());
        assertNotNull(EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN);
        assertEquals(1_030_020_202, EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("验证 021 客户对账模块错误码定义")
    void test021StatementErrorCodes() {
        assertNotNull(STATEMENT_NOT_EXISTS);
        assertEquals(1_030_021_000, STATEMENT_NOT_EXISTS.getCode());
        assertNotNull(STATEMENT_STATUS_NOT_ALLOW);
        assertEquals(1_030_021_001, STATEMENT_STATUS_NOT_ALLOW.getCode());
        assertNotNull(STATEMENT_NO_PERIOD_ENTRIES);
        assertEquals(1_030_021_002, STATEMENT_NO_PERIOD_ENTRIES.getCode());
        assertNotNull(STATEMENT_ENTRY_OCCUPIED);
        assertEquals(1_030_021_003, STATEMENT_ENTRY_OCCUPIED.getCode());
        assertNotNull(STATEMENT_CONFIRMED_NOT_VOID);
        assertEquals(1_030_021_004, STATEMENT_CONFIRMED_NOT_VOID.getCode());
        assertNotNull(STATEMENT_CURRENCY_MIXED);
        assertEquals(1_030_021_005, STATEMENT_CURRENCY_MIXED.getCode());
        assertNotNull(STATEMENT_CUSTOMER_MISMATCH);
        assertEquals(1_030_021_006, STATEMENT_CUSTOMER_MISMATCH.getCode());
        assertNotNull(STATEMENT_PERIOD_INVALID);
        assertEquals(1_030_021_007, STATEMENT_PERIOD_INVALID.getCode());
    }

    @Test
    @DisplayName("验证 023 IN 侧收款模块错误码定义")
    void test023ReceiptErrorCodes() {
        assertNotNull(RECEIPT_NOT_EXISTS);
        assertEquals(1_030_023_100, RECEIPT_NOT_EXISTS.getCode());
        assertNotNull(RECEIPT_STATUS_NOT_ALLOW);
        assertEquals(1_030_023_101, RECEIPT_STATUS_NOT_ALLOW.getCode());
        assertNotNull(RECEIPT_CUSTOMER_MISMATCH);
        assertEquals(1_030_023_102, RECEIPT_CUSTOMER_MISMATCH.getCode());
        assertNotNull(RECEIPT_CURRENCY_MISMATCH);
        assertEquals(1_030_023_103, RECEIPT_CURRENCY_MISMATCH.getCode());
        assertNotNull(RECEIPT_ALLOCATE_EXCEED);
        assertEquals(1_030_023_104, RECEIPT_ALLOCATE_EXCEED.getCode());
        assertNotNull(RECEIPT_POST_CONFLICT);
        assertEquals(1_030_023_105, RECEIPT_POST_CONFLICT.getCode());
        assertNotNull(RECEIPT_ADVANCE_BALANCE_NOT_ENOUGH);
        assertEquals(1_030_023_106, RECEIPT_ADVANCE_BALANCE_NOT_ENOUGH.getCode());
        assertNotNull(RECEIPT_REVERSAL_NOT_ALLOW);
        assertEquals(1_030_023_107, RECEIPT_REVERSAL_NOT_ALLOW.getCode());
    }

    @Test
    @DisplayName("验证 026 发票申请模块错误码定义")
    void test026InvoiceErrorCodes() {
        assertNotNull(INVOICE_NOT_EXISTS);
        assertEquals(1_030_026_000, INVOICE_NOT_EXISTS.getCode());
        assertNotNull(INVOICE_STATUS_NOT_ALLOW);
        assertEquals(1_030_026_001, INVOICE_STATUS_NOT_ALLOW.getCode());
        assertNotNull(INVOICE_EXCEED_SHIPMENT_AMOUNT);
        assertEquals(1_030_026_002, INVOICE_EXCEED_SHIPMENT_AMOUNT.getCode());
        assertNotNull(INVOICE_CURRENCY_MIXED);
        assertEquals(1_030_026_003, INVOICE_CURRENCY_MIXED.getCode());
        assertNotNull(INVOICE_CUSTOMER_MISMATCH);
        assertEquals(1_030_026_004, INVOICE_CUSTOMER_MISMATCH.getCode());
        assertNotNull(INVOICE_OFFICIAL_NO_REQUIRED);
        assertEquals(1_030_026_005, INVOICE_OFFICIAL_NO_REQUIRED.getCode());
        assertNotNull(INVOICE_OFFICIAL_NO_DUPLICATE);
        assertEquals(1_030_026_006, INVOICE_OFFICIAL_NO_DUPLICATE.getCode());
        assertNotNull(INVOICE_TAX_RATE_INVALID);
        assertEquals(1_030_026_007, INVOICE_TAX_RATE_INVALID.getCode());
    }
}
