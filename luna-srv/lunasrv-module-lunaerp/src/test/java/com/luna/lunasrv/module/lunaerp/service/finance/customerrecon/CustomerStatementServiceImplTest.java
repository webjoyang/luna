package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.CustomerStatementStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.finance.LedgerCategory;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link CustomerStatementServiceImpl} 客户对账单 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class CustomerStatementServiceImplTest {

    @InjectMocks
    private CustomerStatementServiceImpl customerStatementService;

    @Mock
    private CustomerStatementMapper customerStatementMapper;

    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private com.luna.lunasrv.module.infra.api.config.ConfigApi configApi;

    private MockedStatic<TenantContextHolder> tenantContextMock;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContextHolder.class);
        tenantContextMock.when(TenantContextHolder::getTenantId).thenReturn(1L);
        tenantContextMock.when(TenantContextHolder::getRequiredTenantId).thenReturn(1L);

        // 预热 MyBatis-Plus TableInfo 缓存，防止纯 Mockito 单测找不到 Lambda 缓存
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerStatementDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerStatementItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), LedgerEntryDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ShipmentDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), PaymentDO.class);
    }

    @AfterEach
    void tearDown() {
        if (tenantContextMock != null) {
            tenantContextMock.close();
        }
    }

    private CustomerDO mockCustomer(Long id, String name) {
        CustomerDO customer = new CustomerDO();
        customer.setId(id);
        customer.setCompanyName(name);
        customer.setTenantId(1L);
        return customer;
    }

    private LedgerEntryDO mockLedgerEntry(Long id, Long partyId, String category, String direction,
                                         String currency, BigDecimal amount, LocalDate bizDate,
                                         String refType, Long refId, Long refItemId, Long reversalOfId) {
        LedgerEntryDO entry = new LedgerEntryDO();
        entry.setId(id);
        entry.setPartyType("CUSTOMER");
        entry.setPartyId(partyId);
        entry.setCategory(category);
        entry.setDirection(direction);
        entry.setCurrency(currency);
        entry.setOriginalAmount(amount);
        entry.setBaseAmount(amount);
        entry.setExchangeRate(BigDecimal.ONE);
        entry.setBizDate(bizDate);
        entry.setRefType(refType);
        entry.setRefId(refId);
        entry.setRefItemId(refItemId);
        entry.setReversalOfId(reversalOfId);
        entry.setRemark("品名测试" + id);
        entry.setTenantId(1L);
        return entry;
    }

    private ShipmentDO mockShipment(Long id, String shipmentNo, String signStatus) {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(id);
        shipment.setShipmentNo(shipmentNo);
        shipment.setSignStatus(signStatus);
        shipment.setTenantId(1L);
        return shipment;
    }

    private CustomerStatementDO mockStatement(Long id, String statementNo, Long customerId, String currency,
                                             CustomerStatementStatus status, LocalDate periodStart, LocalDate periodEnd) {
        CustomerStatementDO statement = new CustomerStatementDO();
        statement.setId(id);
        statement.setStatementNo(statementNo);
        statement.setCustomerId(customerId);
        statement.setCustomerName("测试客户" + customerId);
        statement.setCurrency(currency);
        statement.setStatus(status.getValue());
        statement.setPeriodStart(periodStart);
        statement.setPeriodEnd(periodEnd);
        statement.setOpeningBalance(BigDecimal.ZERO);
        statement.setPeriodDebit(BigDecimal.ZERO);
        statement.setPeriodCredit(BigDecimal.ZERO);
        statement.setClosingBalance(BigDecimal.ZERO);
        statement.setTenantId(1L);
        return statement;
    }

    // ==================== 1. prefetchLines 预览与取数测试 ====================

    @Test
    @DisplayName("prefetchLines 异常：起止日期非法 (periodStart > periodEnd) 抛出 STATEMENT_PERIOD_INVALID")
    void testPrefetchLines_periodInvalid_throwsException() {
        CustomerStatementPrefetchReqVO req = new CustomerStatementPrefetchReqVO();
        req.setCustomerId(1001L);
        req.setCurrency("RMB");
        req.setPeriodStart(LocalDate.of(2026, 10, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 1));

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.prefetchLines(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_PERIOD_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("prefetchLines 异常：客户不存在抛出 STATEMENT_CUSTOMER_MISMATCH")
    void testPrefetchLines_customerNotFound_throwsException() {
        CustomerStatementPrefetchReqVO req = new CustomerStatementPrefetchReqVO();
        req.setCustomerId(9999L);
        req.setCurrency("RMB");
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));

        when(customerMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.prefetchLines(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_CUSTOMER_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("prefetchLines 成功：过滤区间内客户与币种分录、未签收放行、红冲净额与期初期末闭环重算")
    void testPrefetchLines_filterByPeriodCustomerCurrency_success() {
        Long customerId = 1001L;
        String currency = "RMB";
        LocalDate periodStart = LocalDate.of(2026, 9, 1);
        LocalDate periodEnd = LocalDate.of(2026, 9, 30);

        CustomerStatementPrefetchReqVO req = new CustomerStatementPrefetchReqVO();
        req.setCustomerId(customerId);
        req.setCurrency(currency);
        req.setPeriodStart(periodStart);
        req.setPeriodEnd(periodEnd);

        when(customerMapper.selectById(customerId)).thenReturn(mockCustomer(customerId, "盛泽纺织"));

        // 构造分录：
        // 1. 期前发货分录：2026-08-10, 1000.00
        LedgerEntryDO priorEntry = mockLedgerEntry(1L, customerId, LedgerCategory.AR_INC.getValue(), "DEBIT",
                currency, new BigDecimal("1000.00"), LocalDate.of(2026, 8, 10), "SHIPMENT_ITEM", 101L, 1001L, null);
        // 2. 期前核销分录：2026-08-20, 400.00 -> 期初欠款余额应为 1000 - 400 = 600.00
        LedgerEntryDO priorWriteOff = mockLedgerEntry(2L, customerId, LedgerCategory.RECEIPT_WRITE_OFF.getValue(), "CREDIT",
                currency, new BigDecimal("400.00"), LocalDate.of(2026, 8, 20), "RECEIPT", 201L, 1L, null);

        // 3. 区间内发货分录 1：2026-09-05, 1200.00 (关联发货单未签收 PENDING)
        LedgerEntryDO curShipment1 = mockLedgerEntry(3L, customerId, LedgerCategory.AR_INC.getValue(), "DEBIT",
                currency, new BigDecimal("1200.00"), LocalDate.of(2026, 9, 5), "SHIPMENT_ITEM", 102L, 1002L, null);
        // 4. 区间内发货分录 2：2026-09-10, 800.00，但有部分红冲 200.00 -> 净额 600.00
        LedgerEntryDO curShipment2 = mockLedgerEntry(4L, customerId, LedgerCategory.AR_INC.getValue(), "DEBIT",
                currency, new BigDecimal("800.00"), LocalDate.of(2026, 9, 10), "SHIPMENT_ITEM", 103L, 1003L, null);
        LedgerEntryDO curReversal2 = mockLedgerEntry(5L, customerId, LedgerCategory.AR_INC_REVERSAL.getValue(), "CREDIT",
                currency, new BigDecimal("200.00"), LocalDate.of(2026, 9, 12), "SHIPMENT_ITEM", 103L, 1003L, 4L);

        // 5. 区间内发货分录 3：2026-09-15, 300.00，完全红冲 300.00 -> 净额 0，应被排除
        LedgerEntryDO curShipment3 = mockLedgerEntry(6L, customerId, LedgerCategory.AR_INC.getValue(), "DEBIT",
                currency, new BigDecimal("300.00"), LocalDate.of(2026, 9, 15), "SHIPMENT_ITEM", 104L, 1004L, null);
        LedgerEntryDO curReversal3 = mockLedgerEntry(7L, customerId, LedgerCategory.AR_INC_REVERSAL.getValue(), "CREDIT",
                currency, new BigDecimal("300.00"), LocalDate.of(2026, 9, 16), "SHIPMENT_ITEM", 104L, 1004L, 6L);

        // 6. 区间内核销分录：2026-09-20, 500.00
        LedgerEntryDO curWriteOff = mockLedgerEntry(8L, customerId, LedgerCategory.RECEIPT_WRITE_OFF.getValue(), "CREDIT",
                currency, new BigDecimal("500.00"), LocalDate.of(2026, 9, 20), "RECEIPT", 202L, 3L, null);

        // 打桩分录查询
        when(ledgerEntryMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            Wrapper<?> w = inv.getArgument(0);
            w.getSqlSegment(); // 触发求值
            if (w.getSqlSegment().contains("reversal_of_id")) {
                return List.of(curReversal2, curReversal3);
            }
            if (w.getSqlSegment().contains(">=")) {
                return List.of(curShipment1, curShipment2, curShipment3, curWriteOff);
            }
            // 期初前查询 (biz_date < periodStart)
            return List.of(priorEntry, priorWriteOff);
        });

        // 占用检查无占用
        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        // 发货单快照打桩（未签收 PENDING）
        when(shipmentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                mockShipment(102L, "SH2026090001", "PENDING"),
                mockShipment(103L, "SH2026090002", "SIGNED")
        ));

        CustomerStatementPrefetchRespVO result = customerStatementService.prefetchLines(req);

        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        assertEquals("盛泽纺织", result.getCustomerName());
        assertEquals("RMB", result.getCurrency());
        // 期初：1000 - 400 = 600.00
        assertEquals(new BigDecimal("600.00"), result.getOpeningBalance());
        // 本期借方：1200 + 600(800-200) = 1800.00
        assertEquals(new BigDecimal("1800.00"), result.getPeriodDebit());
        // 本期贷方：500.00
        assertEquals(new BigDecimal("500.00"), result.getPeriodCredit());
        // 期末：600 + 1800 - 500 = 1900.00
        assertEquals(new BigDecimal("1900.00"), result.getClosingBalance());

        // 验证候选行列表：3 行（curShipment1, curShipment2净额行, curWriteOff）
        assertEquals(3, result.getLines().size());
        CustomerStatementCandidateItemRespVO line1 = result.getLines().stream()
                .filter(l -> l.getLedgerEntryId().equals(3L)).findFirst().orElse(null);
        assertNotNull(line1);
        assertEquals("PENDING", line1.getSignStatus()); // 签收前可对账，标记保留
        assertEquals(new BigDecimal("1200.00"), line1.getDebitAmount());

        CustomerStatementCandidateItemRespVO line2 = result.getLines().stream()
                .filter(l -> l.getLedgerEntryId().equals(4L)).findFirst().orElse(null);
        assertNotNull(line2);
        assertEquals(new BigDecimal("600.00"), line2.getDebitAmount()); // 红冲净额呈现
    }

    @Test
    @DisplayName("prefetchLines 过滤：已在非 VOID 对账单中的分录自动排除候选")
    void testPrefetchLines_excludesOccupiedEntries() {
        Long customerId = 1001L;
        String currency = "RMB";
        LocalDate periodStart = LocalDate.of(2026, 9, 1);
        LocalDate periodEnd = LocalDate.of(2026, 9, 30);

        CustomerStatementPrefetchReqVO req = new CustomerStatementPrefetchReqVO();
        req.setCustomerId(customerId);
        req.setCurrency(currency);
        req.setPeriodStart(periodStart);
        req.setPeriodEnd(periodEnd);

        when(customerMapper.selectById(customerId)).thenReturn(mockCustomer(customerId, "盛泽纺织"));

        LedgerEntryDO entry1 = mockLedgerEntry(101L, customerId, LedgerCategory.AR_INC.getValue(), "DEBIT",
                currency, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 5), "SHIPMENT_ITEM", 11L, 111L, null);
        LedgerEntryDO entry2 = mockLedgerEntry(102L, customerId, LedgerCategory.AR_INC.getValue(), "DEBIT",
                currency, new BigDecimal("2000.00"), LocalDate.of(2026, 9, 10), "SHIPMENT_ITEM", 12L, 112L, null);

        when(ledgerEntryMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            Wrapper<?> w = inv.getArgument(0);
            w.getSqlSegment();
            if (w.getSqlSegment().contains(">=")) {
                return List.of(entry1, entry2);
            }
            return Collections.emptyList();
        });

        // 模拟 entry1 已被一张 CONFIRMED 对账单占用
        CustomerStatementItemDO occupiedItem = new CustomerStatementItemDO();
        occupiedItem.setLedgerEntryId(101L);
        occupiedItem.setStatementId(555L);
        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(occupiedItem));

        CustomerStatementDO activeStatement = mockStatement(555L, "CS20260001", customerId, "RMB",
                CustomerStatementStatus.CONFIRMED, periodStart, periodEnd);
        when(customerStatementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeStatement));
        when(shipmentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(mockShipment(12L, "SH12", "SIGNED")));

        CustomerStatementPrefetchRespVO result = customerStatementService.prefetchLines(req);

        // entry1 已被排除，仅剩下 entry2
        assertEquals(1, result.getLines().size());
        assertEquals(102L, result.getLines().get(0).getLedgerEntryId());
    }

    // ==================== 2. createCustomerStatement 生成对账单测试 ====================

    @Test
    @DisplayName("createCustomerStatement 异常：期间无可对账分录抛出 STATEMENT_NO_PERIOD_ENTRIES")
    void testCreateCustomerStatement_noEntries_throwsException() {
        CustomerStatementCreateReqVO req = new CustomerStatementCreateReqVO();
        req.setCustomerId(1001L);
        req.setCurrency("RMB");
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));

        when(customerMapper.selectById(1001L)).thenReturn(mockCustomer(1001L, "盛泽纺织"));
        when(ledgerEntryMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.createCustomerStatement(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_NO_PERIOD_ENTRIES.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createCustomerStatement 异常：多币种分录混入抛出 STATEMENT_CURRENCY_MIXED")
    void testCreateCustomerStatement_currencyMismatch_throwsException() {
        CustomerStatementCreateReqVO req = new CustomerStatementCreateReqVO();
        req.setCustomerId(1001L);
        req.setCurrency("RMB");
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));
        req.setManualEntryIds(List.of(999L));

        when(customerMapper.selectById(1001L)).thenReturn(mockCustomer(1001L, "盛泽纺织"));

        LedgerEntryDO usdEntry = mockLedgerEntry(999L, 1001L, LedgerCategory.AR_INC.getValue(), "DEBIT",
                "USD", new BigDecimal("500.00"), LocalDate.of(2026, 9, 10), "SHIPMENT_ITEM", 10L, 1L, null);
        when(ledgerEntryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(usdEntry));

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.createCustomerStatement(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_CURRENCY_MIXED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createCustomerStatement 异常：分录已被 CONFIRMED 对账单占用抛出 STATEMENT_ENTRY_OCCUPIED")
    void testCreateCustomerStatement_confirmedOccupied_throwsException() {
        CustomerStatementCreateReqVO req = new CustomerStatementCreateReqVO();
        req.setCustomerId(1001L);
        req.setCurrency("RMB");
        req.setPeriodStart(LocalDate.of(2026, 9, 1));
        req.setPeriodEnd(LocalDate.of(2026, 9, 30));
        req.setManualEntryIds(List.of(101L));

        when(customerMapper.selectById(1001L)).thenReturn(mockCustomer(1001L, "盛泽纺织"));

        LedgerEntryDO entry = mockLedgerEntry(101L, 1001L, LedgerCategory.AR_INC.getValue(), "DEBIT",
                "RMB", new BigDecimal("1000.00"), LocalDate.of(2026, 9, 10), "SHIPMENT_ITEM", 10L, 1L, null);
        when(ledgerEntryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(entry));

        // 模拟已存在 CONFIRMED 对账单占用
        CustomerStatementItemDO item = new CustomerStatementItemDO();
        item.setLedgerEntryId(101L);
        item.setStatementId(888L);
        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(item));

        CustomerStatementDO confirmedStmt = mockStatement(888L, "CS20260002", 1001L, "RMB",
                CustomerStatementStatus.CONFIRMED, req.getPeriodStart(), req.getPeriodEnd());
        when(customerStatementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(confirmedStmt));

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.createCustomerStatement(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_ENTRY_OCCUPIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createCustomerStatement 成功：自动生成 CS 取号、落库快照冻结、服务端重算期初期末")
    void testCreateCustomerStatement_success_withSerialNoAndSnapshots() {
        Long customerId = 1001L;
        String currency = "RMB";
        LocalDate periodStart = LocalDate.of(2026, 9, 1);
        LocalDate periodEnd = LocalDate.of(2026, 9, 30);

        CustomerStatementCreateReqVO req = new CustomerStatementCreateReqVO();
        req.setCustomerId(customerId);
        req.setCurrency(currency);
        req.setPeriodStart(periodStart);
        req.setPeriodEnd(periodEnd);
        req.setRemark("2026年9月对账单");

        when(customerMapper.selectById(customerId)).thenReturn(mockCustomer(customerId, "盛泽纺织"));

        // 分录数据
        LedgerEntryDO priorEntry = mockLedgerEntry(1L, customerId, LedgerCategory.AR_INC.getValue(), "DEBIT",
                currency, new BigDecimal("500.00"), LocalDate.of(2026, 8, 10), "SHIPMENT_ITEM", 101L, 1001L, null);
        LedgerEntryDO curEntry = mockLedgerEntry(2L, customerId, LedgerCategory.AR_INC.getValue(), "DEBIT",
                currency, new BigDecimal("1500.00"), LocalDate.of(2026, 9, 5), "SHIPMENT_ITEM", 102L, 1002L, null);

        when(ledgerEntryMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            Wrapper<?> w = inv.getArgument(0);
            w.getSqlSegment();
            if (w.getSqlSegment().contains(">=")) {
                return List.of(curEntry);
            }
            return List.of(priorEntry);
        });

        // 无占用
        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(shipmentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(mockShipment(102L, "SH2026090001", "SIGNED")));

        // 取号防撞打桩：CS20260001
        when(customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted("CS2026")).thenReturn(Collections.emptyList());
        when(customerStatementMapper.selectByStatementNoIncludingDeleted("CS20260001")).thenReturn(null);

        // 插入主表模拟生成 ID
        doAnswer(inv -> {
            CustomerStatementDO stmt = inv.getArgument(0);
            stmt.setId(777L);
            return 1;
        }).when(customerStatementMapper).insert(any(CustomerStatementDO.class));

        Long statementId = customerStatementService.createCustomerStatement(req);

        assertEquals(777L, statementId);

        // 验证主表快照
        ArgumentCaptor<CustomerStatementDO> stmtCaptor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).insert(stmtCaptor.capture());
        CustomerStatementDO savedStmt = stmtCaptor.getValue();
        assertEquals("CS20260001", savedStmt.getStatementNo());
        assertEquals("盛泽纺织", savedStmt.getCustomerName());
        assertEquals(CustomerStatementStatus.DRAFT.getValue(), savedStmt.getStatus());
        assertEquals(new BigDecimal("500.00"), savedStmt.getOpeningBalance());
        assertEquals(new BigDecimal("1500.00"), savedStmt.getPeriodDebit());
        assertEquals(new BigDecimal("0.00"), savedStmt.getPeriodCredit());
        assertEquals(new BigDecimal("2000.00"), savedStmt.getClosingBalance()); // 500 + 1500 - 0 = 2000

        // 验证行表快照批量插入
        verify(customerStatementItemMapper, atLeastOnce()).insert(any(CustomerStatementItemDO.class));
    }

    // ==================== 3. getCustomerStatement 详情测试 ====================

    @Test
    @DisplayName("getCustomerStatement 异常：对账单不存在抛出 STATEMENT_NOT_EXISTS")
    void testGetCustomerStatement_notExists_throwsException() {
        when(customerStatementMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.getCustomerStatement(999L));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getCustomerStatement 成功：返回主表与明细行快照列表")
    void testGetCustomerStatement_success() {
        CustomerStatementDO stmt = mockStatement(100L, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(100L)).thenReturn(stmt);

        CustomerStatementItemDO item = new CustomerStatementItemDO();
        item.setId(1L);
        item.setStatementId(100L);
        item.setLineNo(1);
        item.setLedgerEntryId(2001L);
        item.setDirection("DEBIT");
        item.setDebitAmount(new BigDecimal("1000.00"));
        item.setCreditAmount(BigDecimal.ZERO);
        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(item));

        CustomerStatementDetailRespVO detail = customerStatementService.getCustomerStatement(100L);

        assertNotNull(detail);
        assertEquals("CS20260001", detail.getStatementNo());
        assertEquals(1, detail.getItems().size());
        assertEquals(new BigDecimal("1000.00"), detail.getItems().get(0).getDebitAmount());
    }

    // ==================== 4. update / delete (DRAFT) 测试 ====================

    @Test
    @DisplayName("updateCustomerStatement 异常：非草稿状态抛出 STATEMENT_STATUS_NOT_ALLOW")
    void testUpdateCustomerStatement_notDraft_throwsException() {
        CustomerStatementDO stmt = mockStatement(100L, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.SENT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(100L)).thenReturn(stmt);

        CustomerStatementUpdateReqVO req = new CustomerStatementUpdateReqVO();
        req.setId(100L);
        req.setRemark("新备注");

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.updateCustomerStatement(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("updateCustomerStatement 成功：删除指定明细行并自动重算本期发生额与期末余额")
    void testUpdateCustomerStatement_deleteItems_recalculatesTotals_success() {
        CustomerStatementDO stmt = mockStatement(100L, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        stmt.setOpeningBalance(new BigDecimal("200.00"));
        stmt.setPeriodDebit(new BigDecimal("1500.00"));
        stmt.setPeriodCredit(BigDecimal.ZERO);
        stmt.setClosingBalance(new BigDecimal("1700.00"));
        when(customerStatementMapper.selectById(100L)).thenReturn(stmt);

        CustomerStatementItemDO item1 = new CustomerStatementItemDO();
        item1.setId(10L);
        item1.setStatementId(100L);
        item1.setDirection("DEBIT");
        item1.setDebitAmount(new BigDecimal("1000.00"));
        item1.setCreditAmount(BigDecimal.ZERO);

        CustomerStatementItemDO item2 = new CustomerStatementItemDO();
        item2.setId(20L);
        item2.setStatementId(100L);
        item2.setDirection("DEBIT");
        item2.setDebitAmount(new BigDecimal("500.00"));
        item2.setCreditAmount(BigDecimal.ZERO);

        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(item1, item2));

        CustomerStatementUpdateReqVO req = new CustomerStatementUpdateReqVO();
        req.setId(100L);
        req.setDeletedItemIds(List.of(20L)); // 删掉 item2 (500元)
        req.setRemark("已删减一项");

        customerStatementService.updateCustomerStatement(req);

        // 验证 item2 被删除
        verify(customerStatementItemMapper).deleteBatchIds(List.of(20L));
        // 验证主表重新计算：借方变为 1000，期末变为 200 + 1000 - 0 = 1200
        assertEquals(new BigDecimal("1000.00"), stmt.getPeriodDebit());
        assertEquals(new BigDecimal("1200.00"), stmt.getClosingBalance());
        verify(customerStatementMapper).updateById(stmt);
    }

    @Test
    @DisplayName("deleteCustomerStatement 异常：非草稿状态删除抛出 STATEMENT_STATUS_NOT_ALLOW")
    void testDeleteCustomerStatement_notDraft_throwsException() {
        CustomerStatementDO stmt = mockStatement(100L, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.CONFIRMED, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(100L)).thenReturn(stmt);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.deleteCustomerStatement(100L));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("deleteCustomerStatement 成功：草稿对账单软删除主表并级联删除明细行")
    void testDeleteCustomerStatement_success() {
        CustomerStatementDO stmt = mockStatement(100L, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(100L)).thenReturn(stmt);

        customerStatementService.deleteCustomerStatement(100L);

        verify(customerStatementMapper).deleteById(100L);
        verify(customerStatementItemMapper).delete(any(Wrapper.class));
    }

    // ==================== 5. addStatementEntry 手工追加分录测试 ====================

    @Test
    @DisplayName("addStatementEntry 异常：分录客户与对账单客户不匹配抛出 STATEMENT_CUSTOMER_MISMATCH")
    void testAddStatementEntry_customerMismatch_throwsException() {
        CustomerStatementDO stmt = mockStatement(100L, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(100L)).thenReturn(stmt);

        LedgerEntryDO otherCustomerEntry = mockLedgerEntry(2001L, 9999L, LedgerCategory.AR_INC.getValue(), "DEBIT",
                "RMB", new BigDecimal("500.00"), LocalDate.of(2026, 8, 15), "SHIPMENT_ITEM", 10L, 1L, null);
        when(ledgerEntryMapper.selectById(2001L)).thenReturn(otherCustomerEntry);

        CustomerStatementAddEntryReqVO req = new CustomerStatementAddEntryReqVO();
        req.setStatementId(100L);
        req.setLedgerEntryId(2001L);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.addStatementEntry(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_CUSTOMER_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("addStatementEntry 异常：分录币种不匹配抛出 STATEMENT_CURRENCY_MIXED")
    void testAddStatementEntry_currencyMismatch_throwsException() {
        CustomerStatementDO stmt = mockStatement(100L, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(100L)).thenReturn(stmt);

        LedgerEntryDO usdEntry = mockLedgerEntry(2001L, 1001L, LedgerCategory.AR_INC.getValue(), "DEBIT",
                "USD", new BigDecimal("500.00"), LocalDate.of(2026, 8, 15), "SHIPMENT_ITEM", 10L, 1L, null);
        when(ledgerEntryMapper.selectById(2001L)).thenReturn(usdEntry);

        CustomerStatementAddEntryReqVO req = new CustomerStatementAddEntryReqVO();
        req.setStatementId(100L);
        req.setLedgerEntryId(2001L);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.addStatementEntry(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_CURRENCY_MIXED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("addStatementEntry 异常：分录已被 CONFIRMED 对账单占用抛出 STATEMENT_ENTRY_OCCUPIED")
    void testAddStatementEntry_confirmedOccupied_throwsException() {
        CustomerStatementDO stmt = mockStatement(100L, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(100L)).thenReturn(stmt);

        LedgerEntryDO entry = mockLedgerEntry(2001L, 1001L, LedgerCategory.AR_INC.getValue(), "DEBIT",
                "RMB", new BigDecimal("500.00"), LocalDate.of(2026, 8, 15), "SHIPMENT_ITEM", 10L, 1L, null);
        when(ledgerEntryMapper.selectById(2001L)).thenReturn(entry);

        CustomerStatementItemDO itemInConfirmed = new CustomerStatementItemDO();
        itemInConfirmed.setLedgerEntryId(2001L);
        itemInConfirmed.setStatementId(888L);
        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(itemInConfirmed));

        CustomerStatementDO confirmedStmt = mockStatement(888L, "CS20260002", 1001L, "RMB",
                CustomerStatementStatus.CONFIRMED, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        when(customerStatementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(confirmedStmt));

        CustomerStatementAddEntryReqVO req = new CustomerStatementAddEntryReqVO();
        req.setStatementId(100L);
        req.setLedgerEntryId(2001L);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.addStatementEntry(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_ENTRY_OCCUPIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("addStatementEntry 成功：追加期间内分录到草稿对账单并自动更新汇总")
    void testAddStatementEntry_success() {
        CustomerStatementDO stmt = mockStatement(100L, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        stmt.setOpeningBalance(new BigDecimal("100.00"));
        stmt.setPeriodDebit(new BigDecimal("1000.00"));
        stmt.setPeriodCredit(BigDecimal.ZERO);
        stmt.setClosingBalance(new BigDecimal("1100.00"));
        when(customerStatementMapper.selectById(100L)).thenReturn(stmt);

        LedgerEntryDO entry = mockLedgerEntry(2001L, 1001L, LedgerCategory.AR_INC.getValue(), "DEBIT",
                "RMB", new BigDecimal("500.00"), LocalDate.of(2026, 9, 15), "SHIPMENT_ITEM", 10L, 1L, null);
        when(ledgerEntryMapper.selectById(2001L)).thenReturn(entry);

        // 未被 CONFIRMED 占用
        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(shipmentMapper.selectById(10L)).thenReturn(mockShipment(10L, "SH10", "SIGNED"));

        doAnswer(inv -> {
            CustomerStatementItemDO it = inv.getArgument(0);
            it.setId(999L);
            return 1;
        }).when(customerStatementItemMapper).insert(any(CustomerStatementItemDO.class));

        CustomerStatementAddEntryReqVO req = new CustomerStatementAddEntryReqVO();
        req.setStatementId(100L);
        req.setLedgerEntryId(2001L);

        Long itemId = customerStatementService.addStatementEntry(req);

        assertEquals(999L, itemId);
        assertEquals(new BigDecimal("1500.00"), stmt.getPeriodDebit());
        assertEquals(new BigDecimal("1600.00"), stmt.getClosingBalance());
        verify(customerStatementMapper).updateById(stmt);
    }

    @Test
    @DisplayName("addStatementEntry 成功：追加起始日前历史分录，从期初余额剥离防止期末双计")
    void testAddStatementEntry_historicalPriorEntry_adjustsOpeningBalance() {
        CustomerStatementDO stmt = mockStatement(100L, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        stmt.setOpeningBalance(new BigDecimal("1000.00"));
        stmt.setPeriodDebit(new BigDecimal("1000.00"));
        stmt.setPeriodCredit(BigDecimal.ZERO);
        stmt.setClosingBalance(new BigDecimal("2000.00"));
        when(customerStatementMapper.selectById(100L)).thenReturn(stmt);

        // 历史分录：2026-08-15（早于 periodStart 2026-09-01）
        LedgerEntryDO entry = mockLedgerEntry(2002L, 1001L, LedgerCategory.AR_INC.getValue(), "DEBIT",
                "RMB", new BigDecimal("500.00"), LocalDate.of(2026, 8, 15), "SHIPMENT_ITEM", 10L, 1L, null);
        when(ledgerEntryMapper.selectById(2002L)).thenReturn(entry);

        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(shipmentMapper.selectById(10L)).thenReturn(mockShipment(10L, "SH10", "SIGNED"));

        doAnswer(inv -> {
            CustomerStatementItemDO it = inv.getArgument(0);
            it.setId(998L);
            return 1;
        }).when(customerStatementItemMapper).insert(any(CustomerStatementItemDO.class));

        CustomerStatementAddEntryReqVO req = new CustomerStatementAddEntryReqVO();
        req.setStatementId(100L);
        req.setLedgerEntryId(2002L);

        Long itemId = customerStatementService.addStatementEntry(req);

        assertEquals(998L, itemId);
        // 期初扣除 500 变为 500
        assertEquals(new BigDecimal("500.00"), stmt.getOpeningBalance());
        // 本期发生增加 500 变为 1500
        assertEquals(new BigDecimal("1500.00"), stmt.getPeriodDebit());
        // 期末保持 500 + 1500 - 0 = 2000，消除双计
        assertEquals(new BigDecimal("2000.00"), stmt.getClosingBalance());
        verify(customerStatementMapper).updateById(stmt);
    }

    @Test
    @DisplayName("updateCustomerStatement 规范§15：支持将 remark 更新清空为 null 并使用 ArgumentCaptor 捕获校验")
    void testUpdateCustomerStatement_clearRemarkToNull_success() {
        CustomerStatementDO stmt = mockStatement(100L, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        stmt.setRemark("原本有备注");
        when(customerStatementMapper.selectById(100L)).thenReturn(stmt);
        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        CustomerStatementUpdateReqVO req = new CustomerStatementUpdateReqVO();
        req.setId(100L);
        req.setRemark(null); // 清空为 null

        customerStatementService.updateCustomerStatement(req);

        ArgumentCaptor<CustomerStatementDO> captor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).updateById(captor.capture());
        assertNull(captor.getValue().getRemark());
    }

    @Test
    @DisplayName("prefetchLines 弱占用：已被 DRAFT/SENT 占用的分录包含在候选列表中并带有占用标记")
    void testPrefetchLines_weakOccupied_includedInLinesWithWarning() {
        Long customerId = 1001L;
        String currency = "RMB";
        LocalDate periodStart = LocalDate.of(2026, 9, 1);
        LocalDate periodEnd = LocalDate.of(2026, 9, 30);

        CustomerStatementPrefetchReqVO req = new CustomerStatementPrefetchReqVO();
        req.setCustomerId(customerId);
        req.setCurrency(currency);
        req.setPeriodStart(periodStart);
        req.setPeriodEnd(periodEnd);

        when(customerMapper.selectById(customerId)).thenReturn(mockCustomer(customerId, "盛泽纺织"));

        LedgerEntryDO entry1 = mockLedgerEntry(101L, customerId, LedgerCategory.AR_INC.getValue(), "DEBIT",
                currency, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 5), "SHIPMENT_ITEM", 11L, 111L, null);
        LedgerEntryDO entry2 = mockLedgerEntry(102L, customerId, LedgerCategory.AR_INC.getValue(), "DEBIT",
                currency, new BigDecimal("2000.00"), LocalDate.of(2026, 9, 10), "SHIPMENT_ITEM", 12L, 112L, null);

        when(ledgerEntryMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            Wrapper<?> w = inv.getArgument(0);
            w.getSqlSegment();
            if (w.getSqlSegment().contains(">=")) {
                return List.of(entry1, entry2);
            }
            return Collections.emptyList();
        });

        // 模拟 entry1 已被一张 DRAFT 对账单占用
        CustomerStatementItemDO occupiedItem = new CustomerStatementItemDO();
        occupiedItem.setLedgerEntryId(101L);
        occupiedItem.setStatementId(666L);
        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(occupiedItem));

        CustomerStatementDO draftStatement = mockStatement(666L, "CS20260066", customerId, "RMB",
                CustomerStatementStatus.DRAFT, periodStart, periodEnd);
        when(customerStatementMapper.selectList(any(Wrapper.class))).thenReturn(List.of(draftStatement));
        when(shipmentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                mockShipment(11L, "SH11", "SIGNED"),
                mockShipment(12L, "SH12", "SIGNED")
        ));

        CustomerStatementPrefetchRespVO result = customerStatementService.prefetchLines(req);

        // 两条分录均返回（entry1 为弱占用，entry2 为未占用）
        assertEquals(2, result.getLines().size());
        CustomerStatementCandidateItemRespVO line1 = result.getLines().stream()
                .filter(l -> l.getLedgerEntryId().equals(101L)).findFirst().orElse(null);
        assertNotNull(line1);
        assertTrue(line1.getIsOccupied());
        assertEquals("CS20260066", line1.getOccupiedStatementNo());
        assertEquals("DRAFT", line1.getOccupiedStatus());

        // 默认汇总只统计未被占用的 entry2 (2000.00)
        assertEquals(new BigDecimal("2000.00"), result.getPeriodDebit());
    }

    // ==================== 6. 状态流转与白名单守卫测试 (Seam 1 & 2) ====================

    @Test
    @DisplayName("sendCustomerStatement 成功：DRAFT 态通过条件更新转换为 SENT")
    void testSendCustomerStatement_success() {
        Long id = 100L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);
        when(customerStatementMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        customerStatementService.sendCustomerStatement(id);

        verify(customerStatementMapper).update(any(), any(Wrapper.class));
    }

    @Test
    @DisplayName("sendCustomerStatement 异常：非 DRAFT 状态发送抛出 STATEMENT_STATUS_NOT_ALLOW")
    void testSendCustomerStatement_notDraft_throwsException() {
        Long id = 100L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.SENT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.sendCustomerStatement(id));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("sendCustomerStatement 异常：条件更新因并发冲突受影响行数为0抛出 STATEMENT_STATUS_NOT_ALLOW")
    void testSendCustomerStatement_concurrencyConflict_throwsException() {
        Long id = 100L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);
        when(customerStatementMapper.update(any(), any(Wrapper.class))).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.sendCustomerStatement(id));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("voidCustomerStatement 成功：DRAFT/SENT/REJECTED 非终态可作废")
    void testVoidCustomerStatement_success_nonTerminal() {
        // 1. DRAFT 可作废
        Long draftId = 101L;
        CustomerStatementDO draftStmt = mockStatement(draftId, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(draftId)).thenReturn(draftStmt);
        when(customerStatementMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        customerStatementService.voidCustomerStatement(draftId);

        // 2. SENT 可作废
        Long sentId = 102L;
        CustomerStatementDO sentStmt = mockStatement(sentId, "CS20260002", 1001L, "RMB",
                CustomerStatementStatus.SENT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(sentId)).thenReturn(sentStmt);
        customerStatementService.voidCustomerStatement(sentId);

        // 3. REJECTED 可作废
        Long rejectedId = 103L;
        CustomerStatementDO rejectedStmt = mockStatement(rejectedId, "CS20260003", 1001L, "RMB",
                CustomerStatementStatus.REJECTED, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(rejectedId)).thenReturn(rejectedStmt);
        customerStatementService.voidCustomerStatement(rejectedId);

        verify(customerStatementMapper, times(3)).update(any(), any(Wrapper.class));
    }

    @Test
    @DisplayName("voidCustomerStatement 异常：CONFIRMED 终态作废抛出 STATEMENT_CONFIRMED_NOT_VOID")
    void testVoidCustomerStatement_confirmed_throwsException() {
        Long id = 100L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.CONFIRMED, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.voidCustomerStatement(id));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_CONFIRMED_NOT_VOID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("voidCustomerStatement 异常：已作废对账单再次作废抛出 STATEMENT_STATUS_NOT_ALLOW")
    void testVoidCustomerStatement_alreadyVoid_throwsException() {
        Long id = 100L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.VOID, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.voidCustomerStatement(id));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("confirmOnBehalf 成功：SENT 态代客确认落库 ON_BEHALF 及经办字段")
    void testConfirmOnBehalf_success() {
        Long id = 100L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.SENT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);
        when(customerStatementMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        CustomerStatementConfirmOnBehalfReqVO req = new CustomerStatementConfirmOnBehalfReqVO();
        req.setId(id);
        req.setConfirmMethod("微信");
        req.setRemark("客户微信确认核对无误");

        customerStatementService.confirmOnBehalf(req);

        verify(customerStatementMapper).update(any(), any(Wrapper.class));
    }

    @Test
    @DisplayName("confirmOnBehalf 异常：非 SENT 状态代客确认抛出 STATEMENT_STATUS_NOT_ALLOW")
    void testConfirmOnBehalf_notSent_throwsException() {
        Long id = 100L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);

        CustomerStatementConfirmOnBehalfReqVO req = new CustomerStatementConfirmOnBehalfReqVO();
        req.setId(id);
        req.setConfirmMethod("微信");

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.confirmOnBehalf(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("rejectCustomerStatement 成功：SENT 态驳回记录 rejectReason 并置为 REJECTED")
    void testRejectCustomerStatement_success() {
        Long id = 100L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.SENT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);
        when(customerStatementMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        CustomerStatementRejectReqVO req = new CustomerStatementRejectReqVO();
        req.setId(id);
        req.setRejectReason("运费分摊计算有异议");

        customerStatementService.rejectCustomerStatement(req);

        verify(customerStatementMapper).update(any(), any(Wrapper.class));
    }

    @Test
    @DisplayName("rejectCustomerStatement 异常：非 SENT 状态驳回抛出 STATEMENT_STATUS_NOT_ALLOW")
    void testRejectCustomerStatement_notSent_throwsException() {
        Long id = 100L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.CONFIRMED, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);

        CustomerStatementRejectReqVO req = new CustomerStatementRejectReqVO();
        req.setId(id);
        req.setRejectReason("异议原因");

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.rejectCustomerStatement(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("rejectReplyCustomerStatement 成功：REJECTED 态回复异议备注并重置为 DRAFT")
    void testRejectReplyCustomerStatement_success() {
        Long id = 100L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.REJECTED, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);
        when(customerStatementMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        CustomerStatementRejectReplyReqVO req = new CustomerStatementRejectReplyReqVO();
        req.setId(id);
        req.setReplyRemark("已重新核算运费，请复核");

        customerStatementService.rejectReplyCustomerStatement(req);

        verify(customerStatementMapper).update(any(), any(Wrapper.class));
    }

    @Test
    @DisplayName("rejectReplyCustomerStatement 异常：非 REJECTED 状态回复异议抛出 STATEMENT_STATUS_NOT_ALLOW")
    void testRejectReplyCustomerStatement_notRejected_throwsException() {
        Long id = 100L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", 1001L, "RMB",
                CustomerStatementStatus.SENT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);

        CustomerStatementRejectReplyReqVO req = new CustomerStatementRejectReplyReqVO();
        req.setId(id);
        req.setReplyRemark("回复说明");

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.rejectReplyCustomerStatement(req));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    // ==================== 7. 套打出参契约与二维码基址降级测试 (Seam 3) ====================

    @Test
    @DisplayName("getCustomerStatementPrintData 成功：包含客户四要素、全量行快照且正确组装二维码 URL")
    void testGetCustomerStatementPrintData_withPortalBaseUrl_success() {
        Long id = 100L;
        Long customerId = 1001L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", customerId, "RMB",
                CustomerStatementStatus.CONFIRMED, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        stmt.setOpeningBalance(new BigDecimal("1000.00"));
        stmt.setPeriodDebit(new BigDecimal("5000.00"));
        stmt.setPeriodCredit(new BigDecimal("2000.00"));
        stmt.setClosingBalance(new BigDecimal("4000.00"));
        stmt.setConfirmType("ON_BEHALF");
        stmt.setConfirmByName("王财务");
        stmt.setConfirmMethod("微信");
        stmt.setRemark("按月对账");
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);

        CustomerDO customer = mockCustomer(customerId, "盛泽纺织实业有限公司");
        customer.setInvoiceTitle("盛泽纺织实业有限公司");
        customer.setUnifiedSocialCreditCode("91330100MA2XXXXX1");
        customer.setInvoiceAddress("江苏省苏州市吴江区盛泽镇西环路100号");
        customer.setBankName("中国工商银行吴江盛泽支行");
        customer.setBankAccountNo("1055020109200000000");
        customer.setPhone("0512-63000000");
        when(customerMapper.selectById(customerId)).thenReturn(customer);

        CustomerStatementItemDO item1 = new CustomerStatementItemDO();
        item1.setId(10L);
        item1.setStatementId(id);
        item1.setLineNo(1);
        item1.setBizDate(LocalDate.of(2026, 9, 10));
        item1.setDirection("DEBIT");
        item1.setRefType("SHIPMENT_ITEM");
        item1.setRefNo("SH2026090001");
        item1.setSummary("高弹涤纶布");
        item1.setDebitAmount(new BigDecimal("5000.00"));
        item1.setCreditAmount(BigDecimal.ZERO);
        item1.setSignStatus("SIGNED");
        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(item1));

        // 模拟已配置门户基准地址
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn("https://portal.lunaerp.com///");

        CustomerStatementPrintRespVO printVO = customerStatementService.getCustomerStatementPrintData(id);

        assertNotNull(printVO);
        assertEquals("FINANCE_STATEMENT", printVO.getBizType());
        assertEquals("CS20260001", printVO.getStatementNo());
        assertEquals("盛泽纺织实业有限公司", printVO.getCustomerName());
        assertEquals("91330100MA2XXXXX1", printVO.getTaxNo());
        assertEquals("江苏省苏州市吴江区盛泽镇西环路100号", printVO.getAddress());
        assertEquals("中国工商银行吴江盛泽支行", printVO.getBankName());
        assertEquals("1055020109200000000", printVO.getBankAccountNo());
        assertEquals("0512-63000000", printVO.getContactPhone());
        assertEquals(new BigDecimal("1000.00"), printVO.getOpeningBalance());
        assertEquals(new BigDecimal("5000.00"), printVO.getPeriodDebit());
        assertEquals(new BigDecimal("2000.00"), printVO.getPeriodCredit());
        assertEquals(new BigDecimal("4000.00"), printVO.getClosingBalance());
        assertEquals("https://portal.lunaerp.com/portal/statement?no=CS20260001", printVO.getQrUrl());
        assertEquals(1, printVO.getItems().size());
        assertEquals("SH2026090001", printVO.getItems().get(0).getRefNo());
        assertEquals(new BigDecimal("5000.00"), printVO.getItems().get(0).getDebitAmount());
    }

    @Test
    @DisplayName("getCustomerStatementPrintData 成功：luna.portal.base-url 未配置或为空时二维码降级为 null 不阻断打印")
    void testGetCustomerStatementPrintData_withoutPortalBaseUrl_degradesGracefully() {
        Long id = 100L;
        Long customerId = 1001L;
        CustomerStatementDO stmt = mockStatement(id, "CS20260001", customerId, "RMB",
                CustomerStatementStatus.DRAFT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        when(customerStatementMapper.selectById(id)).thenReturn(stmt);
        when(customerMapper.selectById(customerId)).thenReturn(mockCustomer(customerId, "盛泽纺织"));
        when(customerStatementItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        // 模拟基址未配置
        when(configApi.getConfigValueByKey("luna.portal.base-url")).thenReturn(null);

        CustomerStatementPrintRespVO printVO = customerStatementService.getCustomerStatementPrintData(id);

        assertNotNull(printVO);
        assertNull(printVO.getQrUrl());
        assertEquals("CS20260001", printVO.getStatementNo());
    }

    @Test
    @DisplayName("getCustomerStatementPrintData 异常：对账单不存在抛出 STATEMENT_NOT_EXISTS")
    void testGetCustomerStatementPrintData_notExists_throwsException() {
        when(customerStatementMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.getCustomerStatementPrintData(999L));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getCustomerStatementPrintData 审计审查：方法必须标注 @LogRecord 留痕")
    void testGetCustomerStatementPrintData_hasLogRecordAnnotation() throws NoSuchMethodException {
        java.lang.reflect.Method method = CustomerStatementServiceImpl.class.getMethod("getCustomerStatementPrintData", Long.class);
        com.mzt.logapi.starter.annotation.LogRecord annotation = method.getAnnotation(com.mzt.logapi.starter.annotation.LogRecord.class);
        assertNotNull(annotation, "getCustomerStatementPrintData 必须标注 @LogRecord");
        assertEquals("FINANCE_STATEMENT", annotation.type());
        assertEquals("PRINT", annotation.subType());
    }

}
