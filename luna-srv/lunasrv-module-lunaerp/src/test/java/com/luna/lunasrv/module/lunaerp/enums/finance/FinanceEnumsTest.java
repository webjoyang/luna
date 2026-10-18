package com.luna.lunasrv.module.lunaerp.enums.finance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 财务域枚举单元测试
 */
class FinanceEnumsTest {

    @Test
    @DisplayName("测试 ReconStatus 枚举")
    void testReconStatus() {
        assertEquals("DRAFT", ReconStatus.DRAFT.getValue());
        assertEquals("草稿", ReconStatus.DRAFT.getLabel());
        assertEquals("DRAFT", ReconStatus.DRAFT.getCode());

        assertEquals("CONFIRMED", ReconStatus.CONFIRMED.getValue());
        assertEquals("已确认", ReconStatus.CONFIRMED.getLabel());

        assertEquals("CANCELLED", ReconStatus.CANCELLED.getValue());
        assertEquals("已作废", ReconStatus.CANCELLED.getLabel());
    }

    @Test
    @DisplayName("测试 LineType 枚举")
    void testLineType() {
        assertEquals("PROCESS", LineType.PROCESS.getValue());
        assertEquals("加工费", LineType.PROCESS.getLabel());
        assertEquals("PROCESS", LineType.PROCESS.getCode());

        assertEquals("PURCHASE", LineType.PURCHASE.getValue());
        assertEquals("采购货款", LineType.PURCHASE.getLabel());

        assertEquals("ADJUST", LineType.ADJUST.getValue());
        assertEquals("调整项", LineType.ADJUST.getLabel());
    }

    @Test
    @DisplayName("测试 ApplyType 枚举")
    void testApplyType() {
        assertEquals("RECON", ApplyType.RECON.getValue());
        assertEquals("对账付款", ApplyType.RECON.getLabel());
        assertEquals("RECON", ApplyType.RECON.getCode());

        assertEquals("PREPAY", ApplyType.PREPAY.getValue());
        assertEquals("预付付款", ApplyType.PREPAY.getLabel());
    }

    @Test
    @DisplayName("测试 Direction 枚举")
    void testDirection() {
        assertEquals("IN", Direction.IN.getValue());
        assertEquals("收款", Direction.IN.getLabel());
        assertEquals("IN", Direction.IN.getCode());

        assertEquals("OUT", Direction.OUT.getValue());
        assertEquals("付款", Direction.OUT.getLabel());
    }

    @Test
    @DisplayName("测试 PricingBasis 与 SourceRefType 枚举")
    void testPricingBasisAndSourceRefType() {
        assertEquals("METER", PricingBasis.METER.getValue());
        assertEquals("KG", PricingBasis.KG.getValue());

        assertEquals("INSPECTION", SourceRefType.INSPECTION.getValue());
        assertEquals("STOCK_IN_ITEM", SourceRefType.STOCK_IN_ITEM.getValue());
    }

    @Test
    @DisplayName("测试 CustomerStatementStatus 与 CustomerStatementConfirmType 枚举")
    void testCustomerStatementEnums() {
        assertEquals("DRAFT", CustomerStatementStatus.DRAFT.getValue());
        assertEquals("草稿", CustomerStatementStatus.DRAFT.getLabel());
        assertEquals("DRAFT", CustomerStatementStatus.DRAFT.getCode());
        assertEquals("SENT", CustomerStatementStatus.SENT.getValue());
        assertEquals("CONFIRMED", CustomerStatementStatus.CONFIRMED.getValue());
        assertEquals("REJECTED", CustomerStatementStatus.REJECTED.getValue());
        assertEquals("VOID", CustomerStatementStatus.VOID.getValue());

        assertEquals("ONLINE_PORTAL", CustomerStatementConfirmType.ONLINE_PORTAL.getValue());
        assertEquals("在线确认", CustomerStatementConfirmType.ONLINE_PORTAL.getLabel());
        assertEquals("ONLINE_PORTAL", CustomerStatementConfirmType.ONLINE_PORTAL.getCode());
        assertEquals("ON_BEHALF", CustomerStatementConfirmType.ON_BEHALF.getValue());
        assertEquals("代客确认", CustomerStatementConfirmType.ON_BEHALF.getLabel());
    }

}
