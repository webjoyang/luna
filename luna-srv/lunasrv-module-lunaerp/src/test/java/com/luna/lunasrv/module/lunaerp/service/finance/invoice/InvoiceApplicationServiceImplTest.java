package com.luna.lunasrv.module.lunaerp.service.finance.invoice;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.finance.InvoiceApplicationStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link InvoiceApplicationServiceImpl} 纯 Mockito 单元测试 (TDD Seam 2)
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class InvoiceApplicationServiceImplTest {

    @InjectMocks
    private InvoiceApplicationServiceImpl invoiceApplicationService;

    @Mock
    private InvoiceApplicationMapper invoiceApplicationMapper;

    @Mock
    private InvoiceApplicationItemMapper invoiceApplicationItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private ShipmentMapper shipmentMapper;

    private MockedStatic<TenantContextHolder> tenantContextMock;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContextHolder.class);
        tenantContextMock.when(TenantContextHolder::getTenantId).thenReturn(1L);
        tenantContextMock.when(TenantContextHolder::getRequiredTenantId).thenReturn(1L);

        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), InvoiceApplicationDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), InvoiceApplicationItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ShipmentDO.class);
    }

    @AfterEach
    void tearDown() {
        if (tenantContextMock != null) {
            tenantContextMock.close();
        }
    }

    private CustomerDO mockCustomer(Long id, String name, String invoiceTitle, String taxNo, String address, String bankName, String bankAccountNo) {
        CustomerDO customer = new CustomerDO();
        customer.setId(id);
        customer.setCompanyName(name);
        customer.setInvoiceTitle(invoiceTitle);
        customer.setUnifiedSocialCreditCode(taxNo);
        customer.setInvoiceAddress(address);
        customer.setBankName(bankName);
        customer.setBankAccountNo(bankAccountNo);
        customer.setTenantId(1L);
        return customer;
    }

    private ShipmentDO mockShipment(Long id, String shipmentNo, Long customerId, String currency, String status, BigDecimal totalAmount, BigDecimal freightFee) {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(id);
        shipment.setShipmentNo(shipmentNo);
        shipment.setCustomerId(customerId);
        shipment.setCustomerName("测试客户");
        shipment.setCurrency(currency);
        shipment.setStatus(status);
        shipment.setTotalAmount(totalAmount);
        shipment.setFreightFee(freightFee);
        shipment.setShippingDate(LocalDate.of(2026, 10, 10));
        shipment.setTenantId(1L);
        return shipment;
    }

    @Test
    @DisplayName("创建成功：从客户档案自动带出四要素，反算税额，初始状态为 DRAFT")
    public void testCreate_success() {
        Long customerId = 1001L;
        CustomerDO customer = mockCustomer(customerId, "盛泽纺织", "盛泽纺织开票专用抬头", "91320500123456789X",
                "苏州市吴江区盛泽镇", "中国银行盛泽支行", "6222020011223344");
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        ShipmentDO shipment = mockShipment(2001L, "SH-20261010-001", customerId, "RMB", "COMPLETED",
                new BigDecimal("9000.00"), new BigDecimal("1000.00")); // shipmentAmount = 10000.00
        when(shipmentMapper.selectBatchIds(any())).thenReturn(List.of(shipment));

        // 模拟当前该发货单无其他已占用开票额
        when(invoiceApplicationItemMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(invoiceApplicationMapper.selectListByInvoiceNoPrefixIncludingDeleted(any())).thenReturn(Collections.emptyList());
        when(invoiceApplicationMapper.selectByInvoiceNoIncludingDeleted(any())).thenReturn(null);

        InvoiceApplicationCreateReqVO reqVO = InvoiceApplicationCreateReqVO.builder()
                .customerId(customerId)
                .currency("RMB")
                .taxRate(new BigDecimal("13.00"))
                .invoiceKind("DIGITAL_SPECIAL")
                .electronic(1)
                .items(List.of(
                        InvoiceApplicationItemSaveReqVO.builder()
                                .shipmentId(2001L)
                                .amount(new BigDecimal("1130.00"))
                                .remark("首笔开票")
                                .build()
                ))
                .build();

        Long id = invoiceApplicationService.createInvoiceApplication(reqVO);

        ArgumentCaptor<InvoiceApplicationDO> appCaptor = ArgumentCaptor.forClass(InvoiceApplicationDO.class);
        verify(invoiceApplicationMapper).insert(appCaptor.capture());
        InvoiceApplicationDO saved = appCaptor.getValue();

        assertNotNull(saved);
        assertEquals(customerId, saved.getCustomerId());
        assertEquals("盛泽纺织开票专用抬头", saved.getInvoiceTitle());
        assertEquals("91320500123456789X", saved.getTaxNo());
        assertEquals(InvoiceApplicationStatus.DRAFT.getValue(), saved.getStatus());
        assertEquals(new BigDecimal("1130.00"), saved.getTotalAmountInclTax());
        assertEquals(new BigDecimal("1000.00"), saved.getTotalAmountExclTax());
        assertEquals(new BigDecimal("130.00"), saved.getTotalTax());
        assertTrue(saved.getInvoiceNo().startsWith("IA"));

        verify(invoiceApplicationItemMapper).insert(any(InvoiceApplicationItemDO.class));
    }

    @Test
    @DisplayName("创建守卫：客户不匹配抛出 INVOICE_CUSTOMER_MISMATCH")
    public void testCreate_customerMismatch_throwException() {
        Long customerId = 1001L;
        CustomerDO customer = mockCustomer(customerId, "盛泽纺织", null, null, null, null, null);
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        // 发货单所属客户为 1002L
        ShipmentDO shipment = mockShipment(2001L, "SH-20261010-001", 1002L, "RMB", "COMPLETED",
                new BigDecimal("9000.00"), new BigDecimal("1000.00"));
        when(shipmentMapper.selectBatchIds(any())).thenReturn(List.of(shipment));

        InvoiceApplicationCreateReqVO reqVO = InvoiceApplicationCreateReqVO.builder()
                .customerId(customerId)
                .currency("RMB")
                .taxRate(new BigDecimal("13.00"))
                .invoiceKind("DIGITAL_SPECIAL")
                .items(List.of(InvoiceApplicationItemSaveReqVO.builder().shipmentId(2001L).amount(new BigDecimal("100.00")).build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                invoiceApplicationService.createInvoiceApplication(reqVO));
        assertEquals(FinanceErrorCodeConstants.INVOICE_CUSTOMER_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建守卫：币种混选抛出 INVOICE_CURRENCY_MIXED")
    public void testCreate_currencyMixed_throwException() {
        Long customerId = 1001L;
        CustomerDO customer = mockCustomer(customerId, "盛泽纺织", null, null, null, null, null);
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        // 发货单为 USD，请求为 RMB
        ShipmentDO shipment = mockShipment(2001L, "SH-20261010-001", customerId, "USD", "COMPLETED",
                new BigDecimal("9000.00"), BigDecimal.ZERO);
        when(shipmentMapper.selectBatchIds(any())).thenReturn(List.of(shipment));

        InvoiceApplicationCreateReqVO reqVO = InvoiceApplicationCreateReqVO.builder()
                .customerId(customerId)
                .currency("RMB")
                .taxRate(new BigDecimal("13.00"))
                .invoiceKind("DIGITAL_SPECIAL")
                .items(List.of(InvoiceApplicationItemSaveReqVO.builder().shipmentId(2001L).amount(new BigDecimal("100.00")).build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                invoiceApplicationService.createInvoiceApplication(reqVO));
        assertEquals(FinanceErrorCodeConstants.INVOICE_CURRENCY_MIXED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建超开守卫：申请金额超过发货单总金额抛出 INVOICE_EXCEED_SHIPMENT_AMOUNT")
    public void testCreate_exceedShipmentAmount_singleShipment() {
        Long customerId = 1001L;
        CustomerDO customer = mockCustomer(customerId, "盛泽纺织", null, null, null, null, null);
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        ShipmentDO shipment = mockShipment(2001L, "SH-20261010-001", customerId, "RMB", "COMPLETED",
                new BigDecimal("900.00"), new BigDecimal("100.00")); // 合计 1000.00
        when(shipmentMapper.selectBatchIds(any())).thenReturn(List.of(shipment));
        when(invoiceApplicationItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        // 申请 1000.01
        InvoiceApplicationCreateReqVO reqVO = InvoiceApplicationCreateReqVO.builder()
                .customerId(customerId)
                .currency("RMB")
                .taxRate(new BigDecimal("13.00"))
                .invoiceKind("DIGITAL_SPECIAL")
                .items(List.of(InvoiceApplicationItemSaveReqVO.builder().shipmentId(2001L).amount(new BigDecimal("1000.01")).build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                invoiceApplicationService.createInvoiceApplication(reqVO));
        assertEquals(FinanceErrorCodeConstants.INVOICE_EXCEED_SHIPMENT_AMOUNT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("超开守卫：跨申请聚合 SUBMITTED 在途与 ISSUED 已开，REJECTED 释放")
    public void testCreate_exceedShipmentAmount_aggregatedWithSubmittedAndIssued() {
        Long customerId = 1001L;
        CustomerDO customer = mockCustomer(customerId, "盛泽纺织", null, null, null, null, null);
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        ShipmentDO shipment = mockShipment(2001L, "SH-20261010-001", customerId, "RMB", "COMPLETED",
                new BigDecimal("1000.00"), BigDecimal.ZERO);
        when(shipmentMapper.selectBatchIds(any())).thenReturn(List.of(shipment));

        // 模拟其他申请行：
        // 申请 1 (SUBMITTED 在途): 600.00
        // 申请 2 (ISSUED 已开): 300.00
        // 申请 3 (REJECTED 已驳回): 500.00（应释放，不计入）
        // 申请 4 (DRAFT 草稿): 400.00（草稿不锁定其他申请配额）
        InvoiceApplicationItemDO item1 = new InvoiceApplicationItemDO();
        item1.setApplicationId(11L);
        item1.setShipmentId(2001L);
        item1.setAmount(new BigDecimal("600.00"));

        InvoiceApplicationItemDO item2 = new InvoiceApplicationItemDO();
        item2.setApplicationId(12L);
        item2.setShipmentId(2001L);
        item2.setAmount(new BigDecimal("300.00"));

        InvoiceApplicationItemDO item3 = new InvoiceApplicationItemDO();
        item3.setApplicationId(13L);
        item3.setShipmentId(2001L);
        item3.setAmount(new BigDecimal("500.00"));

        when(invoiceApplicationItemMapper.selectList(any())).thenReturn(List.of(item1, item2, item3));

        InvoiceApplicationDO app1 = new InvoiceApplicationDO();
        app1.setId(11L);
        app1.setStatus(InvoiceApplicationStatus.SUBMITTED.getValue());

        InvoiceApplicationDO app2 = new InvoiceApplicationDO();
        app2.setId(12L);
        app2.setStatus(InvoiceApplicationStatus.ISSUED.getValue());

        InvoiceApplicationDO app3 = new InvoiceApplicationDO();
        app3.setId(13L);
        app3.setStatus(InvoiceApplicationStatus.REJECTED.getValue());

        when(invoiceApplicationMapper.selectBatchIds(any())).thenReturn(List.of(app1, app2, app3));

        // 当前申请 150.00：已占用 600 + 300 = 900.00，剩余 100.00，申请 150.00 必超
        InvoiceApplicationCreateReqVO reqVO = InvoiceApplicationCreateReqVO.builder()
                .customerId(customerId)
                .currency("RMB")
                .taxRate(new BigDecimal("13.00"))
                .invoiceKind("DIGITAL_SPECIAL")
                .items(List.of(InvoiceApplicationItemSaveReqVO.builder().shipmentId(2001L).amount(new BigDecimal("150.00")).build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                invoiceApplicationService.createInvoiceApplication(reqVO));
        assertEquals(FinanceErrorCodeConstants.INVOICE_EXCEED_SHIPMENT_AMOUNT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交申请：DRAFT -> SUBMITTED，记录 submitBy 与 submitTime")
    public void testSubmit_success() {
        Long appId = 10001L;
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(appId);
        app.setStatus(InvoiceApplicationStatus.DRAFT.getValue());
        app.setCustomerId(1001L);
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);

        InvoiceApplicationItemDO item = new InvoiceApplicationItemDO();
        item.setApplicationId(appId);
        item.setShipmentId(2001L);
        item.setAmount(new BigDecimal("500.00"));
        when(invoiceApplicationItemMapper.selectList(any())).thenReturn(List.of(item));

        ShipmentDO shipment = mockShipment(2001L, "SH-001", 1001L, "RMB", "COMPLETED", new BigDecimal("1000.00"), BigDecimal.ZERO);
        when(shipmentMapper.selectBatchIds(any())).thenReturn(List.of(shipment));

        invoiceApplicationService.submitInvoiceApplication(appId);

        assertEquals(InvoiceApplicationStatus.SUBMITTED.getValue(), app.getStatus());
        assertNotNull(app.getSubmitTime());
        verify(invoiceApplicationMapper).updateById(app);
    }

    @Test
    @DisplayName("提交守卫：非 DRAFT 状态提交抛出 INVOICE_STATUS_NOT_ALLOW")
    public void testSubmit_invalidStatus_throwsException() {
        Long appId = 10001L;
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(appId);
        app.setStatus(InvoiceApplicationStatus.SUBMITTED.getValue());
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                invoiceApplicationService.submitInvoiceApplication(appId));
        assertEquals(FinanceErrorCodeConstants.INVOICE_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("登记开票：SUBMITTED -> ISSUED，回填 20 位发票号、开票日期，记录 issueBy 与 issueTime")
    public void testIssue_success() {
        Long appId = 10001L;
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(appId);
        app.setStatus(InvoiceApplicationStatus.SUBMITTED.getValue());
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);
        when(invoiceApplicationMapper.selectByOfficialInvoiceNoIncludingDeleted("24322000000000000001")).thenReturn(null);

        InvoiceApplicationIssueReqVO reqVO = InvoiceApplicationIssueReqVO.builder()
                .id(appId)
                .officialInvoiceNo("24322000000000000001")
                .invoiceDate(LocalDate.of(2026, 10, 18))
                .invoiceCode("3200201130")
                .attachmentUrl("https://oss.lunaerp.com/inv.pdf")
                .build();

        invoiceApplicationService.issueInvoiceApplication(reqVO);

        assertEquals(InvoiceApplicationStatus.ISSUED.getValue(), app.getStatus());
        assertEquals("24322000000000000001", app.getOfficialInvoiceNo());
        assertEquals(LocalDate.of(2026, 10, 18), app.getInvoiceDate());
        assertNotNull(app.getIssueTime());
        verify(invoiceApplicationMapper).updateById(app);
    }

    @Test
    @DisplayName("登记开票守卫：发票号已存在抛出 INVOICE_OFFICIAL_NO_DUPLICATE")
    public void testIssue_duplicateOfficialNo_throwException() {
        Long appId = 10001L;
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(appId);
        app.setStatus(InvoiceApplicationStatus.SUBMITTED.getValue());
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);

        InvoiceApplicationDO existing = new InvoiceApplicationDO();
        existing.setId(9999L);
        existing.setOfficialInvoiceNo("24322000000000000001");
        when(invoiceApplicationMapper.selectByOfficialInvoiceNoIncludingDeleted("24322000000000000001")).thenReturn(existing);

        InvoiceApplicationIssueReqVO reqVO = InvoiceApplicationIssueReqVO.builder()
                .id(appId)
                .officialInvoiceNo("24322000000000000001")
                .invoiceDate(LocalDate.of(2026, 10, 18))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                invoiceApplicationService.issueInvoiceApplication(reqVO));
        assertEquals(FinanceErrorCodeConstants.INVOICE_OFFICIAL_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("驳回申请：SUBMITTED -> REJECTED，释放开票占用")
    public void testReject_success() {
        Long appId = 10001L;
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(appId);
        app.setStatus(InvoiceApplicationStatus.SUBMITTED.getValue());
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);

        InvoiceApplicationRejectReqVO reqVO = InvoiceApplicationRejectReqVO.builder()
                .id(appId)
                .rejectReason("税号错误")
                .build();

        invoiceApplicationService.rejectInvoiceApplication(reqVO);

        assertEquals(InvoiceApplicationStatus.REJECTED.getValue(), app.getStatus());
        assertTrue(app.getRemark().contains("税号错误"));
        verify(invoiceApplicationMapper).updateById(app);
    }

    @Test
    @DisplayName("修改申请：仅 DRAFT 态允许修改")
    public void testUpdate_onlyDraftAllowed() {
        Long appId = 10001L;
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(appId);
        app.setStatus(InvoiceApplicationStatus.SUBMITTED.getValue());
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);

        InvoiceApplicationUpdateReqVO updateReqVO = InvoiceApplicationUpdateReqVO.builder()
                .id(appId)
                .taxRate(new BigDecimal("13.00"))
                .items(List.of(InvoiceApplicationItemSaveReqVO.builder().shipmentId(2001L).amount(new BigDecimal("100.00")).build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                invoiceApplicationService.updateInvoiceApplication(updateReqVO));
        assertEquals(FinanceErrorCodeConstants.INVOICE_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除申请：仅 DRAFT 态允许删除")
    public void testDelete_onlyDraftAllowed() {
        Long appId = 10001L;
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setId(appId);
        app.setStatus(InvoiceApplicationStatus.ISSUED.getValue());
        when(invoiceApplicationMapper.selectById(appId)).thenReturn(app);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                invoiceApplicationService.deleteInvoiceApplication(appId));
        assertEquals(FinanceErrorCodeConstants.INVOICE_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("候选发货单查询：自动反算已开票额与可开票余额，过滤出大于0的 COMPLETED 单")
    public void testGetCandidateShipments_success() {
        Long customerId = 1001L;
        String currency = "RMB";

        ShipmentDO shipment1 = mockShipment(2001L, "SH-001", customerId, currency, "COMPLETED", new BigDecimal("1000.00"), BigDecimal.ZERO);
        ShipmentDO shipment2 = mockShipment(2002L, "SH-002", customerId, currency, "COMPLETED", new BigDecimal("2000.00"), new BigDecimal("200.00")); // 2200.00
        when(shipmentMapper.selectList(any())).thenReturn(List.of(shipment1, shipment2));

        // shipment1 已经被占用 1000.00（全额开完），shipment2 被占用 500.00
        InvoiceApplicationItemDO item1 = new InvoiceApplicationItemDO();
        item1.setApplicationId(101L);
        item1.setShipmentId(2001L);
        item1.setAmount(new BigDecimal("1000.00"));

        InvoiceApplicationItemDO item2 = new InvoiceApplicationItemDO();
        item2.setApplicationId(102L);
        item2.setShipmentId(2002L);
        item2.setAmount(new BigDecimal("500.00"));

        when(invoiceApplicationItemMapper.selectList(any()))
                .thenReturn(List.of(item1), List.of(item2));

        InvoiceApplicationDO app1 = new InvoiceApplicationDO();
        app1.setId(101L);
        app1.setStatus(InvoiceApplicationStatus.ISSUED.getValue());

        InvoiceApplicationDO app2 = new InvoiceApplicationDO();
        app2.setId(102L);
        app2.setStatus(InvoiceApplicationStatus.SUBMITTED.getValue());

        when(invoiceApplicationMapper.selectBatchIds(any())).thenReturn(List.of(app1, app2));

        List<InvoiceCandidateShipmentRespVO> result = invoiceApplicationService.getCandidateShipments(customerId, currency);

        // shipment1 剩余为 0，应被过滤；shipment2 剩余 1700.00
        assertEquals(1, result.size());
        InvoiceCandidateShipmentRespVO resp = result.get(0);
        assertEquals(2002L, resp.getShipmentId());
        assertEquals(new BigDecimal("2200.00"), resp.getShipmentAmount());
        assertEquals(new BigDecimal("500.00"), resp.getInvoicedAmount());
        assertEquals(new BigDecimal("1700.00"), resp.getAvailableAmount());
    }
}
