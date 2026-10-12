package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luna.lunasrv.framework.common.util.spring.SpringUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link CustomerAgingServiceImpl} 业务与脱敏单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerAgingServiceImpl 客户账龄服务集成测试")
public class CustomerAgingServiceImplTest {

    @InjectMocks
    private CustomerAgingServiceImpl customerAgingService;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock(name = "lunaerpShipmentMapper")
    private ShipmentMapper shipmentMapper;

    @Mock
    private AdminUserApi adminUserApi;

    private ObjectMapper objectMapper;
    private SecurityFrameworkService securityFrameworkService;
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        securityFrameworkService = mock(SecurityFrameworkService.class);
        applicationContext = mock(ApplicationContext.class);
        lenient().when(applicationContext.getBean(SecurityFrameworkService.class)).thenReturn(securityFrameworkService);
        lenient().when(applicationContext.getBean("ss")).thenReturn(securityFrameworkService);
        lenient().when(applicationContext.getBean("securityFrameworkService")).thenReturn(securityFrameworkService);
        new SpringUtils().setApplicationContext(applicationContext);
    }

    @AfterEach
    void tearDown() {
        new SpringUtils().setApplicationContext(null);
    }

    private CustomerDO createCustomer(Long id, String name, Integer creditDays, Long salesmanId) {
        CustomerDO c = new CustomerDO();
        c.setId(id);
        c.setCompanyName(name);
        c.setCreditDays(creditDays);
        c.setSalespersonUserId(salesmanId);
        return c;
    }

    private LedgerEntryDO createEntry(Long id, Long customerId, String category, BigDecimal amount, LocalDate bizDate) {
        return LedgerEntryDO.builder()
                .id(id)
                .partyType("CUSTOMER")
                .partyId(customerId)
                .category(category)
                .originalAmount(amount)
                .currency("CNY")
                .bizDate(bizDate)
                .remark("分录" + id)
                .build();
    }

    @Test
    @DisplayName("跨客户不互抵：客户A的收款核销绝不冲减客户B的应收分录")
    void testCrossCustomerIsolation() {
        CustomerDO customerA = createCustomer(1001L, "客户A", 30, null);
        CustomerDO customerB = createCustomer(1002L, "客户B", 30, null);

        when(customerMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(customerA, customerB));

        LocalDate agingDate = LocalDate.of(2026, 10, 31);

        // 客户A: 发货 5000，核销 5000 -> 剩余 0
        LedgerEntryDO arA = createEntry(1L, 1001L, "AR_INC", new BigDecimal("5000.00"), LocalDate.of(2026, 10, 1));
        LedgerEntryDO rA = createEntry(2L, 1001L, "RECEIPT_WRITE_OFF", new BigDecimal("5000.00"), LocalDate.of(2026, 10, 10));

        // 客户B: 发货 8000，无核销 -> 剩余 8000
        LedgerEntryDO arB = createEntry(3L, 1002L, "AR_INC", new BigDecimal("8000.00"), LocalDate.of(2026, 10, 1));

        when(ledgerEntryMapper.selectList(argThat(wrapper -> true)))
                .thenReturn(List.of(arA, rA))
                .thenReturn(List.of(arB));
        when(paymentMapper.selectList(any())).thenReturn(Collections.emptyList());

        CustomerAgingSummaryReqVO reqVO = CustomerAgingSummaryReqVO.builder()
                .agingDate(agingDate)
                .build();

        List<CustomerAgingSummaryRespVO> result = customerAgingService.getAgingSummary(reqVO);

        assertEquals(2, result.size());
        // 客户A 剩余 0
        assertEquals(new BigDecimal("0.00"), result.get(0).getTotalBalance());
        // 客户B 剩余 8000，绝不被客户A的 5000 核销互抵
        assertEquals(new BigDecimal("8000.00"), result.get(1).getTotalBalance());
    }

    @Test
    @DisplayName("汇总查询：成功调用 AdminUserApi 补充业务员姓名并返回汇总")
    void testGetAgingSummaryWithSalesman() {
        CustomerDO customer = createCustomer(1001L, "测试客户", 30, 2001L);
        when(customerMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(customer));

        AdminUserRespDTO userDTO = new AdminUserRespDTO();
        userDTO.setId(2001L);
        userDTO.setNickname("张销售");
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(2001L, userDTO));

        LedgerEntryDO ar = createEntry(1L, 1001L, "AR_INC", new BigDecimal("3000.00"), LocalDate.of(2026, 10, 5));
        when(ledgerEntryMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(ar));

        PaymentDO advance = new PaymentDO();
        advance.setId(10L);
        advance.setCustomerId(1001L);
        advance.setPayAmount(new BigDecimal("1000.00"));
        advance.setCurrency("CNY");
        when(paymentMapper.selectList(any())).thenReturn(List.of(advance));

        CustomerAgingSummaryReqVO reqVO = CustomerAgingSummaryReqVO.builder()
                .agingDate(LocalDate.of(2026, 10, 31))
                .build();

        List<CustomerAgingSummaryRespVO> summaryList = customerAgingService.getAgingSummary(reqVO);

        assertNotNull(summaryList);
        assertEquals(1, summaryList.size());
        CustomerAgingSummaryRespVO item = summaryList.get(0);
        assertEquals(1001L, item.getCustomerId());
        assertEquals("测试客户", item.getCustomerName());
        assertEquals("张销售", item.getSalesmanName());
        assertEquals(new BigDecimal("3000.00"), item.getTotalBalance());
        assertEquals(new BigDecimal("3000.00"), item.getBucket0To30());
        assertEquals(new BigDecimal("1000.00"), item.getAdvanceBalance());
    }

    @Test
    @DisplayName("明细下钻：成功解析发货单号并返回未核销明细列表")
    void testGetAgingDetailSuccess() {
        CustomerDO customer = createCustomer(1001L, "测试客户", 30, 2001L);
        when(customerMapper.selectById(1001L)).thenReturn(customer);

        LedgerEntryDO ar = createEntry(101L, 1001L, "AR_INC", new BigDecimal("6000.00"), LocalDate.of(2026, 9, 1));
        ar.setRefType("SHIPMENT");
        ar.setRefId(501L);

        LedgerEntryDO writeoff = createEntry(102L, 1001L, "RECEIPT_WRITE_OFF", new BigDecimal("2000.00"), LocalDate.of(2026, 9, 20));

        when(ledgerEntryMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(ar, writeoff));

        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(501L);
        shipment.setShipmentNo("SH-20260901-001");
        when(shipmentMapper.selectById(501L)).thenReturn(shipment);

        CustomerAgingDetailReqVO reqVO = CustomerAgingDetailReqVO.builder()
                .customerId(1001L)
                .agingDate(LocalDate.of(2026, 10, 31))
                .build();

        List<CustomerAgingDetailRespVO> details = customerAgingService.getAgingDetail(reqVO);

        assertNotNull(details);
        assertEquals(1, details.size());
        CustomerAgingDetailRespVO item = details.get(0);
        assertEquals(101L, item.getLedgerEntryId());
        assertEquals("SH-20260901-001", item.getRefNo());
        assertEquals(new BigDecimal("6000.00"), item.getOriginalAmount());
        assertEquals(new BigDecimal("2000.00"), item.getSettledAmount());
        assertEquals(new BigDecimal("4000.00"), item.getUnsettledAmount());
        assertTrue(item.getIsOverdue());
    }

    @Test
    @DisplayName("脱敏测试：无权限时金额字段脱敏为 ****，有权限时输出原值")
    void testAmountDesensitization() throws Exception {
        CustomerAgingSummaryRespVO summaryVO = CustomerAgingSummaryRespVO.builder()
                .customerId(1001L)
                .customerName("敏感客户")
                .totalBalance(new BigDecimal("10000.00"))
                .bucket0To30(new BigDecimal("2000.00"))
                .bucket31To60(new BigDecimal("3000.00"))
                .bucket61To90(new BigDecimal("1000.00"))
                .bucket91To180(new BigDecimal("1500.00"))
                .bucket181Plus(new BigDecimal("2500.00"))
                .overdueAmount(new BigDecimal("4000.00"))
                .advanceBalance(new BigDecimal("5000.00"))
                .build();

        // 1. 无金额权限
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);
        String maskedJson = objectMapper.writeValueAsString(summaryVO);
        assertTrue(maskedJson.contains("\"totalBalance\":\"****\""));
        assertTrue(maskedJson.contains("\"bucket0To30\":\"****\""));
        assertTrue(maskedJson.contains("\"overdueAmount\":\"****\""));
        assertTrue(maskedJson.contains("\"advanceBalance\":\"****\""));

        // 2. 有金额权限
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(true);
        String plainJson = objectMapper.writeValueAsString(summaryVO);
        assertTrue(plainJson.contains("\"totalBalance\":10000.00"));
        assertTrue(plainJson.contains("\"bucket0To30\":2000.00"));
        assertTrue(plainJson.contains("\"overdueAmount\":4000.00"));
        assertTrue(plainJson.contains("\"advanceBalance\":5000.00"));
    }

}
