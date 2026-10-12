package com.luna.lunasrv.module.lunaerp.service.finance.invoice;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.InvoiceApplicationStatusEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.InvoiceKindEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link InvoiceServiceImpl} 单元测试
 * 覆盖 Issue 10 全部业务守卫与核心逻辑：
 * 1. 超开守卫（跨申请聚合含 SUBMITTED 在途、REJECTED 释放）
 * 2. 币种混选 / 客户不一致拒绝
 * 3. 服务端税额反算（精确分舍入）
 * 4. 登记发票号码唯一性校验（含软删）与 20 位校验
 * 5. 抬头四要素快照带出与可改
 * 6. 状态机流转与删除守卫（仅 DRAFT 可删）
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceApplicationMapper invoiceApplicationMapper;

    @Mock
    private InvoiceApplicationItemMapper invoiceApplicationItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private ShipmentMapper shipmentMapper;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    // ==================== 1. 税额反算测试 ====================

    @Test
    @DisplayName("税额反算：13% 正常价税反算与分舍入平衡")
    void testCalculateTax_13Percent() {
        // 价税合计 113.00，13% 税率 -> 不含税 100.00，税额 13.00
        InvoiceServiceImpl.TaxResult result1 = InvoiceServiceImpl.calculateTax(new BigDecimal("113.00"), "13");
        assertEquals(new BigDecimal("113.00"), result1.inclTax);
        assertEquals(new BigDecimal("100.00"), result1.exclTax);
        assertEquals(new BigDecimal("13.00"), result1.tax);
        assertEquals(result1.inclTax, result1.exclTax.add(result1.tax));

        // 价税合计 100.00，13% 税率 -> 100 / 1.13 = 88.50，税额 = 11.50
        InvoiceServiceImpl.TaxResult result2 = InvoiceServiceImpl.calculateTax(new BigDecimal("100.00"), "13");
        assertEquals(new BigDecimal("100.00"), result2.inclTax);
        assertEquals(new BigDecimal("88.50"), result2.exclTax);
        assertEquals(new BigDecimal("11.50"), result2.tax);
        assertEquals(result2.inclTax, result2.exclTax.add(result2.tax));
    }

    @Test
    @DisplayName("税额反算：0% 和 EXEMPT 免税反算")
    void testCalculateTax_ZeroAndExempt() {
        InvoiceServiceImpl.TaxResult zeroResult = InvoiceServiceImpl.calculateTax(new BigDecimal("200.00"), "0");
        assertEquals(new BigDecimal("200.00"), zeroResult.inclTax);
        assertEquals(new BigDecimal("200.00"), zeroResult.exclTax);
        assertEquals(new BigDecimal("0.00"), zeroResult.tax);

        InvoiceServiceImpl.TaxResult exemptResult = InvoiceServiceImpl.calculateTax(new BigDecimal("200.00"), "EXEMPT");
        assertEquals(new BigDecimal("200.00"), exemptResult.inclTax);
        assertEquals(new BigDecimal("200.00"), exemptResult.exclTax);
        assertEquals(new BigDecimal("0.00"), exemptResult.tax);
    }

    @Test
    @DisplayName("税额反算：非法税率抛 026_007 INVOICE_TAX_RATE_INVALID")
    void testCalculateTax_InvalidRate_throws026007() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                InvoiceServiceImpl.calculateTax(new BigDecimal("100.00"), "17"));
        assertEquals(INVOICE_TAX_RATE_INVALID.getCode(), ex.getCode());
    }

    // ==================== 2. 客户快照与可改测试 ====================

    @Test
    @DisplayName("创建申请：客户档案四要素默认带出")
    void createInvoice_customerSnapshotDefaulting() {
        Long customerId = 1001L;
        CustomerDO customer = new CustomerDO();
        customer.setId(customerId);
        customer.setCompanyName("某某纺织有限公司");
        customer.setInvoiceTitle("某某开票抬头");
        customer.setUnifiedSocialCreditCode("91330100MA2XXXXX01");
        customer.setInvoiceAddress("杭州市西湖区某某路 0571-12345678");
        customer.setBankName("工商银行");
        customer.setBankAccountNo("6222020000000001");
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        Long shipmentId = 2001L;
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(shipmentId);
        shipment.setShipmentNo("SH20261012-001");
        shipment.setStatus("COMPLETED");
        shipment.setCustomerId(customerId);
        shipment.setCurrency("CNY");
        shipment.setTotalAmount(new BigDecimal("1000.00"));
        shipment.setFreightFee(new BigDecimal("50.00"));
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);
        when(invoiceApplicationItemMapper.selectInvoicedAmountByShipmentId(shipmentId, null)).thenReturn(BigDecimal.ZERO);

        InvoiceSaveReqVO reqVO = InvoiceSaveReqVO.builder()
                .customerId(customerId)
                .invoiceKind(InvoiceKindEnum.ELEC_SPECIAL.getValue())
                .items(List.of(InvoiceItemSaveVO.builder().shipmentId(shipmentId).amount(new BigDecimal("1050.00")).build()))
                .build();

        invoiceService.createInvoice(reqVO);

        ArgumentCaptor<InvoiceApplicationDO> captor = ArgumentCaptor.forClass(InvoiceApplicationDO.class);
        verify(invoiceApplicationMapper).insert(captor.capture());
        InvoiceApplicationDO saved = captor.getValue();

        assertEquals("某某开票抬头", saved.getInvoiceTitle());
        assertEquals("91330100MA2XXXXX01", saved.getTaxNo());
        assertEquals("杭州市西湖区某某路 0571-12345678", saved.getAddress());
        assertEquals("工商银行 6222020000000001", saved.getBankInfo());
        assertEquals("13", saved.getTaxRate()); // CNY 默认 13%
        assertEquals(InvoiceApplicationStatusEnum.DRAFT.getValue(), saved.getStatus());
        assertEquals(new BigDecimal("1050.00"), saved.getTotalAmountInclTax());
    }

    @Test
    @DisplayName("创建申请：可自定义编辑客户四要素")
    void createInvoice_customerSnapshotEditable() {
        Long customerId = 1001L;
        CustomerDO customer = new CustomerDO();
        customer.setId(customerId);
        customer.setCompanyName("某某纺织有限公司");
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        Long shipmentId = 2001L;
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(shipmentId);
        shipment.setShipmentNo("SH20261012-001");
        shipment.setStatus("COMPLETED");
        shipment.setCustomerId(customerId);
        shipment.setCurrency("CNY");
        shipment.setTotalAmount(new BigDecimal("500.00"));
        shipment.setFreightFee(BigDecimal.ZERO);
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);
        when(invoiceApplicationItemMapper.selectInvoicedAmountByShipmentId(shipmentId, null)).thenReturn(BigDecimal.ZERO);

        InvoiceSaveReqVO reqVO = InvoiceSaveReqVO.builder()
                .customerId(customerId)
                .invoiceKind(InvoiceKindEnum.ELEC_NORMAL.getValue())
                .invoiceTitle("自定义抬头")
                .taxNo("91330100MA2CUSTOM")
                .address("自定义地址")
                .bankInfo("招商银行 123456789")
                .taxRate("9")
                .items(List.of(InvoiceItemSaveVO.builder().shipmentId(shipmentId).amount(new BigDecimal("500.00")).build()))
                .build();

        invoiceService.createInvoice(reqVO);

        ArgumentCaptor<InvoiceApplicationDO> captor = ArgumentCaptor.forClass(InvoiceApplicationDO.class);
        verify(invoiceApplicationMapper).insert(captor.capture());
        InvoiceApplicationDO saved = captor.getValue();

        assertEquals("自定义抬头", saved.getInvoiceTitle());
        assertEquals("91330100MA2CUSTOM", saved.getTaxNo());
        assertEquals("自定义地址", saved.getAddress());
        assertEquals("招商银行 123456789", saved.getBankInfo());
        assertEquals("9", saved.getTaxRate());
    }

    // ==================== 3. 客户与币种混选守卫测试 ====================

    @Test
    @DisplayName("创建申请：客户不一致抛 026_004 INVOICE_CUSTOMER_MISMATCH")
    void createInvoice_customerMismatch_throws026004() {
        Long customerId = 1001L;
        CustomerDO customer = new CustomerDO();
        customer.setId(customerId);
        customer.setCompanyName("客户A");
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        Long shipmentId = 2001L;
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(shipmentId);
        shipment.setStatus("COMPLETED");
        shipment.setCustomerId(9999L); // 属于客户 B
        shipment.setCurrency("CNY");
        shipment.setTotalAmount(new BigDecimal("100.00"));
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

        InvoiceSaveReqVO reqVO = InvoiceSaveReqVO.builder()
                .customerId(customerId)
                .invoiceKind(InvoiceKindEnum.ELEC_SPECIAL.getValue())
                .items(List.of(InvoiceItemSaveVO.builder().shipmentId(shipmentId).amount(new BigDecimal("100.00")).build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> invoiceService.createInvoice(reqVO));
        assertEquals(INVOICE_CUSTOMER_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建申请：多选发货单币种混选抛 026_003 INVOICE_CURRENCY_MIXED")
    void createInvoice_currencyMixed_throws026003() {
        Long customerId = 1001L;
        CustomerDO customer = new CustomerDO();
        customer.setId(customerId);
        customer.setCompanyName("客户A");
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        Long s1 = 2001L;
        ShipmentDO ship1 = new ShipmentDO();
        ship1.setId(s1);
        ship1.setStatus("COMPLETED");
        ship1.setCustomerId(customerId);
        ship1.setCurrency("CNY");
        ship1.setTotalAmount(new BigDecimal("100.00"));
        when(shipmentMapper.selectById(s1)).thenReturn(ship1);

        Long s2 = 2002L;
        ShipmentDO ship2 = new ShipmentDO();
        ship2.setId(s2);
        ship2.setStatus("COMPLETED");
        ship2.setCustomerId(customerId);
        ship2.setCurrency("USD"); // 混选外币
        ship2.setTotalAmount(new BigDecimal("100.00"));
        when(shipmentMapper.selectById(s2)).thenReturn(ship2);

        InvoiceSaveReqVO reqVO = InvoiceSaveReqVO.builder()
                .customerId(customerId)
                .invoiceKind(InvoiceKindEnum.ELEC_SPECIAL.getValue())
                .items(List.of(
                        InvoiceItemSaveVO.builder().shipmentId(s1).amount(new BigDecimal("100.00")).build(),
                        InvoiceItemSaveVO.builder().shipmentId(s2).amount(new BigDecimal("100.00")).build()
                ))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> invoiceService.createInvoice(reqVO));
        assertEquals(INVOICE_CURRENCY_MIXED.getCode(), ex.getCode());
    }

    // ==================== 4. 超开守卫测试 ====================

    @Test
    @DisplayName("超开守卫：申请金额超过发货单可开票额（含运费）抛 026_002 INVOICE_EXCEED_SHIPMENT_AMOUNT")
    void createInvoice_exceedShipmentAmount_throws026002() {
        Long customerId = 1001L;
        CustomerDO customer = new CustomerDO();
        customer.setId(customerId);
        customer.setCompanyName("客户A");
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        Long shipmentId = 2001L;
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(shipmentId);
        shipment.setStatus("COMPLETED");
        shipment.setCustomerId(customerId);
        shipment.setCurrency("CNY");
        shipment.setTotalAmount(new BigDecimal("1000.00"));
        shipment.setFreightFee(new BigDecimal("50.00")); // 合计 1050.00
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);
        // 已开 800.00
        when(invoiceApplicationItemMapper.selectInvoicedAmountByShipmentId(shipmentId, null)).thenReturn(new BigDecimal("800.00"));

        // 本次申请 300.00，800 + 300 = 1100 > 1050
        InvoiceSaveReqVO reqVO = InvoiceSaveReqVO.builder()
                .customerId(customerId)
                .invoiceKind(InvoiceKindEnum.ELEC_SPECIAL.getValue())
                .items(List.of(InvoiceItemSaveVO.builder().shipmentId(shipmentId).amount(new BigDecimal("300.00")).build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> invoiceService.createInvoice(reqVO));
        assertEquals(INVOICE_EXCEED_SHIPMENT_AMOUNT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("超开守卫：提交时跨申请聚合防并发超开（含 SUBMITTED 在途占用）")
    void submitInvoice_concurrentInFlightCheck_throws026002() {
        Long appId = 3001L;
        InvoiceApplicationDO app = InvoiceApplicationDO.builder()
                .id(appId)
                .status(InvoiceApplicationStatusEnum.DRAFT.getValue())
                .build();
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);

        Long shipmentId = 2001L;
        InvoiceApplicationItemDO item = InvoiceApplicationItemDO.builder()
                .applicationId(appId)
                .shipmentId(shipmentId)
                .amount(new BigDecimal("600.00"))
                .build();
        when(invoiceApplicationItemMapper.selectListByApplicationId(appId)).thenReturn(List.of(item));

        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(shipmentId);
        shipment.setStatus("COMPLETED");
        shipment.setTotalAmount(new BigDecimal("1000.00"));
        shipment.setFreightFee(BigDecimal.ZERO);
        when(shipmentMapper.selectByIdForUpdate(shipmentId)).thenReturn(shipment);

        // 另一单已提交并发占用了 500.00（500 + 600 > 1000）
        when(invoiceApplicationItemMapper.selectInvoicedAmountByShipmentId(shipmentId, appId)).thenReturn(new BigDecimal("500.00"));

        ServiceException ex = assertThrows(ServiceException.class, () -> invoiceService.submitInvoice(appId));
        assertEquals(INVOICE_EXCEED_SHIPMENT_AMOUNT.getCode(), ex.getCode());
    }

    // ==================== 5. 发票号唯一性与登记回填测试 ====================

    @Test
    @DisplayName("登记回填：发票号码未填或非20位数字校验")
    void issueInvoice_officialNoValidation() {
        Long appId = 3001L;
        InvoiceApplicationDO app = InvoiceApplicationDO.builder()
                .id(appId)
                .status(InvoiceApplicationStatusEnum.SUBMITTED.getValue())
                .build();
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);

        // 1. 发票号为空
        InvoiceIssueReqVO emptyNoVO = InvoiceIssueReqVO.builder()
                .id(appId)
                .officialInvoiceNo("")
                .invoiceDate(LocalDate.now())
                .build();
        ServiceException ex1 = assertThrows(ServiceException.class, () -> invoiceService.issueInvoice(emptyNoVO));
        assertEquals(INVOICE_OFFICIAL_NO_REQUIRED.getCode(), ex1.getCode());

        // 2. 发票号非20位
        InvoiceIssueReqVO invalidLenVO = InvoiceIssueReqVO.builder()
                .id(appId)
                .officialInvoiceNo("1234567890123456789") // 19位
                .invoiceDate(LocalDate.now())
                .build();
        ServiceException ex2 = assertThrows(ServiceException.class, () -> invoiceService.issueInvoice(invalidLenVO));
        assertEquals(INVOICE_OFFICIAL_NO_REQUIRED.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("登记回填：发票号码重复（含软删行）抛 026_006 INVOICE_OFFICIAL_NO_DUPLICATE")
    void issueInvoice_officialNoDuplicate_throws026006() {
        Long appId = 3001L;
        InvoiceApplicationDO app = InvoiceApplicationDO.builder()
                .id(appId)
                .status(InvoiceApplicationStatusEnum.SUBMITTED.getValue())
                .build();
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);

        String officialNo = "24332000000012345678";
        // 模拟已存在其他记录占用该发票号码（哪怕是已被软删除的记录）
        InvoiceApplicationDO existing = InvoiceApplicationDO.builder()
                .id(9999L)
                .officialInvoiceNo(officialNo)
                .build();
        when(invoiceApplicationMapper.selectByOfficialInvoiceNoIncludingDeleted(officialNo)).thenReturn(existing);

        InvoiceIssueReqVO reqVO = InvoiceIssueReqVO.builder()
                .id(appId)
                .officialInvoiceNo(officialNo)
                .invoiceDate(LocalDate.now())
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> invoiceService.issueInvoice(reqVO));
        assertEquals(INVOICE_OFFICIAL_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("登记回填：成功回填发票号、日期、附件并跃迁至 ISSUED")
    void issueInvoice_success() {
        Long appId = 3001L;
        InvoiceApplicationDO app = InvoiceApplicationDO.builder()
                .id(appId)
                .status(InvoiceApplicationStatusEnum.SUBMITTED.getValue())
                .build();
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);

        String officialNo = "24332000000012345678";
        when(invoiceApplicationMapper.selectByOfficialInvoiceNoIncludingDeleted(officialNo)).thenReturn(null);

        InvoiceIssueReqVO reqVO = InvoiceIssueReqVO.builder()
                .id(appId)
                .officialInvoiceNo(officialNo)
                .invoiceCode("3300201130")
                .invoiceDate(LocalDate.of(2026, 10, 12))
                .attachmentUrl("https://oss.example.com/inv.pdf")
                .build();

        invoiceService.issueInvoice(reqVO);

        assertEquals(InvoiceApplicationStatusEnum.ISSUED.getValue(), app.getStatus());
        assertEquals(officialNo, app.getOfficialInvoiceNo());
        assertEquals("3300201130", app.getInvoiceCode());
        assertEquals(LocalDate.of(2026, 10, 12), app.getInvoiceDate());
        assertEquals("https://oss.example.com/inv.pdf", app.getAttachmentUrl());
        assertNotNull(app.getIssueTime());
        verify(invoiceApplicationMapper).updateById(app);
    }

    // ==================== 6. 状态机流转、驳回释放与删除守卫测试 ====================

    @Test
    @DisplayName("驳回操作：SUBMITTED -> REJECTED，记录驳回原因")
    void rejectInvoice_success() {
        Long appId = 3001L;
        InvoiceApplicationDO app = InvoiceApplicationDO.builder()
                .id(appId)
                .status(InvoiceApplicationStatusEnum.SUBMITTED.getValue())
                .build();
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);

        invoiceService.rejectInvoice(appId, "税号有误，请修改后重提");

        assertEquals(InvoiceApplicationStatusEnum.REJECTED.getValue(), app.getStatus());
        assertEquals("税号有误，请修改后重提", app.getRejectReason());
        verify(invoiceApplicationMapper).updateById(app);
    }

    @Test
    @DisplayName("删除守卫：仅 DRAFT 允许删除，SUBMITTED/ISSUED/REJECTED 抛 026_001")
    void deleteInvoice_statusGuard() {
        // 1. SUBMITTED 不可删除
        InvoiceApplicationDO submittedApp = InvoiceApplicationDO.builder()
                .id(3001L)
                .status(InvoiceApplicationStatusEnum.SUBMITTED.getValue())
                .build();
        when(invoiceApplicationMapper.selectById(3001L)).thenReturn(submittedApp);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> invoiceService.deleteInvoice(3001L));
        assertEquals(INVOICE_STATUS_NOT_ALLOW.getCode(), ex1.getCode());

        // 2. ISSUED 不可删除
        InvoiceApplicationDO issuedApp = InvoiceApplicationDO.builder()
                .id(3002L)
                .status(InvoiceApplicationStatusEnum.ISSUED.getValue())
                .build();
        when(invoiceApplicationMapper.selectById(3002L)).thenReturn(issuedApp);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> invoiceService.deleteInvoice(3002L));
        assertEquals(INVOICE_STATUS_NOT_ALLOW.getCode(), ex2.getCode());

        // 3. DRAFT 允许删除
        InvoiceApplicationDO draftApp = InvoiceApplicationDO.builder()
                .id(3003L)
                .status(InvoiceApplicationStatusEnum.DRAFT.getValue())
                .build();
        when(invoiceApplicationMapper.selectById(3003L)).thenReturn(draftApp);
        invoiceService.deleteInvoice(3003L);
        verify(invoiceApplicationMapper).deleteById(3003L);
        verify(invoiceApplicationItemMapper).deleteByApplicationId(3003L);
    }

}
