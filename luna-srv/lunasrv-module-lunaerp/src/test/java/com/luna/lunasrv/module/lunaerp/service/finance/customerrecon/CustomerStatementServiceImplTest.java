package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerStatementServiceImplTest {

    @Mock
    private CustomerStatementMapper customerStatementMapper;

    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @Mock(name = "lunaerpShipmentMapper")
    private ShipmentMapper shipmentMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private CustomerStatementServiceImpl customerStatementService;

    private CustomerDO mockCustomer;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), LedgerEntryDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), CustomerStatementDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), CustomerStatementItemDO.class);
    }

    @BeforeEach
    void setUp() {
        mockCustomer = new CustomerDO();
        mockCustomer.setId(1001L);
        mockCustomer.setCompanyName("杭州明辉纺织有限公司");
    }

    @Test
    @DisplayName("取数预览：验证区间/客户/币种过滤、签收前可对账、期初期末试算")
    void testPrefetchLinesSuccess() {
        when(customerMapper.selectById(1001L)).thenReturn(mockCustomer);

        // 1. 期初分录：起始日 2026-09-01 前有一笔 10000 借方 AR_INC，一笔 3000 贷方 RECEIPT_WRITE_OFF
        LedgerEntryDO priorDebit = LedgerEntryDO.builder()
                .bizDate(LocalDate.of(2026, 8, 15))
                .partyType("CUSTOMER").partyId(1001L).currency("CNY")
                .category("AR_INC").originalAmount(new BigDecimal("10000.00")).build();
        LedgerEntryDO priorCredit = LedgerEntryDO.builder()
                .bizDate(LocalDate.of(2026, 8, 20))
                .partyType("CUSTOMER").partyId(1001L).currency("CNY")
                .category("RECEIPT_WRITE_OFF").originalAmount(new BigDecimal("3000.00")).build();
        when(ledgerEntryMapper.selectList(argThat(wrapper -> {
            // lt(bizDate, periodStart)
            return wrapper != null;
        }))).thenReturn(List.of(priorDebit, priorCredit));

        // 2. 区间内分录（9月）：
        // (1) 发货分录（未签收 PENDING，签收前可对账）：金额 5000
        LedgerEntryDO currentDebit = LedgerEntryDO.builder()
                .id(201L)
                .bizDate(LocalDate.of(2026, 9, 10))
                .partyType("CUSTOMER").partyId(1001L).currency("CNY")
                .category("AR_INC").originalAmount(new BigDecimal("5000.00"))
                .refType("SHIPMENT_ITEM").refId(301L).remark("SH20260910001 - 纯棉色织布").build();
        // (2) 核销分录：金额 2000
        LedgerEntryDO currentCredit = LedgerEntryDO.builder()
                .id(202L)
                .bizDate(LocalDate.of(2026, 9, 25))
                .partyType("CUSTOMER").partyId(1001L).currency("CNY")
                .category("RECEIPT_WRITE_OFF").originalAmount(new BigDecimal("2000.00"))
                .refType("PAYMENT").refId(401L).remark("收款核销").build();

        // 针对两次查询（期初与区间内）
        reset(ledgerEntryMapper);
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(priorDebit, priorCredit), List.of(currentDebit, currentCredit));
        when(ledgerEntryMapper.selectListByReversalOfId(201L)).thenReturn(Collections.emptyList());

        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(301L);
        shipment.setShipmentNo("SH20260910001");
        shipment.setSignStatus("PENDING");
        when(shipmentMapper.selectById(301L)).thenReturn(shipment);
        when(customerStatementItemMapper.selectActiveOccupiedMapList()).thenReturn(Collections.emptyList());

        CustomerStatementPrefetchReqVO reqVO = CustomerStatementPrefetchReqVO.builder()
                .customerId(1001L)
                .currency("CNY")
                .periodStart(LocalDate.of(2026, 9, 1))
                .periodEnd(LocalDate.of(2026, 9, 30))
                .build();

        CustomerStatementPrefetchRespVO result = customerStatementService.prefetchLines(reqVO);

        assertNotNull(result);
        assertEquals(1001L, result.getCustomerId());
        // 期初 = 10000 - 3000 = 7000.00
        assertEquals(0, new BigDecimal("7000.00").compareTo(result.getOpeningBalance()));
        // 候选借方 = 5000.00, 候选贷方 = 2000.00
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.getCandidateDebit()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(result.getCandidateCredit()));
        // 预计期末 = 7000 + 5000 - 2000 = 10000.00
        assertEquals(0, new BigDecimal("10000.00").compareTo(result.getEstimatedClosingBalance()));

        assertEquals(2, result.getItems().size());
        CustomerStatementCandidateItemVO debitItem = result.getItems().get(0);
        assertEquals("PENDING", debitItem.getSignStatus()); // 签收前不阻塞
        assertEquals("SH20260910001", debitItem.getRefNo());
        assertFalse(debitItem.getOccupied());
    }

    @Test
    @DisplayName("取数过滤：红冲分录净额呈现")
    void testPrefetchLinesRedHungNetPresentation() {
        when(customerMapper.selectById(1001L)).thenReturn(mockCustomer);

        // 原发货分录 6000.00，红冲了 2000.00，净额应为 4000.00
        LedgerEntryDO origEntry = LedgerEntryDO.builder()
                .id(201L)
                .bizDate(LocalDate.of(2026, 9, 10))
                .partyType("CUSTOMER").partyId(1001L).currency("CNY")
                .category("AR_INC").originalAmount(new BigDecimal("6000.00"))
                .refType("SHIPMENT_ITEM").refId(301L).remark("SH001 - 布料").build();

        LedgerEntryDO cancelEntry = LedgerEntryDO.builder()
                .id(209L)
                .originalAmount(new BigDecimal("2000.00"))
                .category("AR_INC_REVERSAL")
                .reversalOfId(201L).build();

        when(ledgerEntryMapper.selectList(any())).thenReturn(Collections.emptyList(), List.of(origEntry));
        when(ledgerEntryMapper.selectListByReversalOfId(201L)).thenReturn(List.of(cancelEntry));
        when(customerStatementItemMapper.selectActiveOccupiedMapList()).thenReturn(Collections.emptyList());

        CustomerStatementPrefetchReqVO reqVO = CustomerStatementPrefetchReqVO.builder()
                .customerId(1001L)
                .currency("CNY")
                .periodStart(LocalDate.of(2026, 9, 1))
                .periodEnd(LocalDate.of(2026, 9, 30))
                .build();

        CustomerStatementPrefetchRespVO result = customerStatementService.prefetchLines(reqVO);
        assertEquals(1, result.getItems().size());
        assertEquals(0, new BigDecimal("4000.00").compareTo(result.getItems().get(0).getDebitAmount()));
    }

    @Test
    @DisplayName("创建草稿对账单：自动取数、CS取号、重算期初期末并持久化")
    void testCreateStatementSuccess() {
        when(customerMapper.selectById(1001L)).thenReturn(mockCustomer);
        when(customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted(startsWith("CS")))
                .thenReturn(Collections.emptyList());
        when(customerStatementMapper.selectByStatementNoIncludingDeleted(any())).thenReturn(null);

        // 期初 = 1000
        LedgerEntryDO priorEntry = LedgerEntryDO.builder()
                .id(101L)
                .bizDate(LocalDate.of(2026, 8, 1))
                .partyType("CUSTOMER").partyId(1001L).currency("CNY")
                .category("AR_INC").originalAmount(new BigDecimal("1000.00")).build();

        // 本期 = 借方 3000
        LedgerEntryDO currentEntry = LedgerEntryDO.builder()
                .id(501L)
                .bizDate(LocalDate.of(2026, 9, 15))
                .partyType("CUSTOMER").partyId(1001L).currency("CNY")
                .category("AR_INC").originalAmount(new BigDecimal("3000.00"))
                .refType("SHIPMENT_ITEM").remark("SH999 - 色织").build();

        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(priorEntry), List.of(currentEntry));
        when(ledgerEntryMapper.selectListByReversalOfId(501L)).thenReturn(Collections.emptyList());
        when(customerStatementItemMapper.selectActiveOccupiedMapList()).thenReturn(Collections.emptyList());
        when(customerStatementItemMapper.selectActiveOccupiedLedgerEntryIds()).thenReturn(Collections.emptyList());

        doAnswer(invocation -> {
            CustomerStatementDO s = invocation.getArgument(0);
            s.setId(888L);
            return 1;
        }).when(customerStatementMapper).insert(any(CustomerStatementDO.class));

        CustomerStatementCreateReqVO reqVO = CustomerStatementCreateReqVO.builder()
                .customerId(1001L)
                .currency("CNY")
                .periodStart(LocalDate.of(2026, 9, 1))
                .periodEnd(LocalDate.of(2026, 9, 30))
                .remark("9月度对账单")
                .build();

        Long statementId = customerStatementService.createStatement(reqVO);
        assertEquals(888L, statementId);

        // 验证主表持久化字段
        ArgumentCaptor<CustomerStatementDO> captor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).insert(captor.capture());
        CustomerStatementDO saved = captor.getValue();
        assertTrue(saved.getStatementNo().startsWith("CS"));
        assertEquals("DRAFT", saved.getStatus());
        assertEquals(0, new BigDecimal("1000.00").compareTo(saved.getOpeningBalance()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(saved.getPeriodDebit()));
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getPeriodCredit()));
        assertEquals(0, new BigDecimal("4000.00").compareTo(saved.getClosingBalance()));

        // 验证行表持久化
        verify(customerStatementItemMapper).insert(any(CustomerStatementItemDO.class));
    }

    @Test
    @DisplayName("守卫：期间内无可对账分录抛 STATEMENT_NO_PERIOD_ENTRIES")
    void testCreateStatementNoPeriodEntries() {
        when(customerMapper.selectById(1001L)).thenReturn(mockCustomer);
        when(customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted(startsWith("CS")))
                .thenReturn(Collections.emptyList());
        when(customerStatementMapper.selectByStatementNoIncludingDeleted(any())).thenReturn(null);
        when(customerStatementItemMapper.selectActiveOccupiedLedgerEntryIds()).thenReturn(Collections.emptyList());
        when(ledgerEntryMapper.selectList(any())).thenReturn(Collections.emptyList());

        CustomerStatementCreateReqVO reqVO = CustomerStatementCreateReqVO.builder()
                .customerId(1001L)
                .currency("CNY")
                .periodStart(LocalDate.of(2026, 9, 1))
                .periodEnd(LocalDate.of(2026, 9, 30))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.createStatement(reqVO));
        assertEquals(STATEMENT_NO_PERIOD_ENTRIES.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("守卫：起止日期非法抛 STATEMENT_PERIOD_INVALID")
    void testCreateStatementPeriodInvalid() {
        CustomerStatementCreateReqVO reqVO = CustomerStatementCreateReqVO.builder()
                .customerId(1001L)
                .currency("CNY")
                .periodStart(LocalDate.of(2026, 9, 30))
                .periodEnd(LocalDate.of(2026, 9, 1)) // 倒置
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.createStatement(reqVO));
        assertEquals(STATEMENT_PERIOD_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("守卫：多币种分单拒绝抛 STATEMENT_CURRENCY_MIXED")
    void testCreateStatementCurrencyMixed() {
        when(customerMapper.selectById(1001L)).thenReturn(mockCustomer);
        when(customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted(startsWith("CS")))
                .thenReturn(Collections.emptyList());
        when(customerStatementMapper.selectByStatementNoIncludingDeleted(any())).thenReturn(null);
        when(customerStatementItemMapper.selectActiveOccupiedLedgerEntryIds()).thenReturn(Collections.emptyList());

        // 指定包含 USD 分录，但对账单币种为 CNY
        LedgerEntryDO usdEntry = LedgerEntryDO.builder()
                .id(601L)
                .partyType("CUSTOMER").partyId(1001L)
                .currency("USD") // 币种不一致
                .category("AR_INC").originalAmount(new BigDecimal("1000.00")).build();
        when(ledgerEntryMapper.selectById(601L)).thenReturn(usdEntry);

        CustomerStatementCreateReqVO reqVO = CustomerStatementCreateReqVO.builder()
                .customerId(1001L)
                .currency("CNY")
                .periodStart(LocalDate.of(2026, 9, 1))
                .periodEnd(LocalDate.of(2026, 9, 30))
                .selectedEntryIds(List.of(601L))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.createStatement(reqVO));
        assertEquals(STATEMENT_CURRENCY_MIXED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("行占用守卫：已被占用分录拒绝创建抛 STATEMENT_ENTRY_OCCUPIED")
    void testCreateStatementEntryOccupied() {
        when(customerMapper.selectById(1001L)).thenReturn(mockCustomer);
        when(customerStatementMapper.selectListByStatementNoPrefixIncludingDeleted(startsWith("CS")))
                .thenReturn(Collections.emptyList());
        when(customerStatementMapper.selectByStatementNoIncludingDeleted(any())).thenReturn(null);

        // 模拟 701L 已被占
        when(customerStatementItemMapper.selectActiveOccupiedLedgerEntryIds()).thenReturn(List.of(701L));

        LedgerEntryDO cnyEntry = LedgerEntryDO.builder()
                .id(701L)
                .partyType("CUSTOMER").partyId(1001L)
                .currency("CNY")
                .category("AR_INC").originalAmount(new BigDecimal("1000.00")).build();
        when(ledgerEntryMapper.selectById(701L)).thenReturn(cnyEntry);

        CustomerStatementCreateReqVO reqVO = CustomerStatementCreateReqVO.builder()
                .customerId(1001L)
                .currency("CNY")
                .periodStart(LocalDate.of(2026, 9, 1))
                .periodEnd(LocalDate.of(2026, 9, 30))
                .selectedEntryIds(List.of(701L))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.createStatement(reqVO));
        assertEquals(STATEMENT_ENTRY_OCCUPIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("草稿手工追加分录：未占用成功追加并重算汇总")
    void testAddEntrySuccess() {
        CustomerStatementDO draftStatement = CustomerStatementDO.builder()
                .id(100L)
                .status("DRAFT")
                .customerId(1001L)
                .currency("CNY")
                .openingBalance(new BigDecimal("1000.00"))
                .periodDebit(new BigDecimal("2000.00"))
                .periodCredit(BigDecimal.ZERO)
                .closingBalance(new BigDecimal("3000.00"))
                .build();
        when(customerStatementMapper.selectById(100L)).thenReturn(draftStatement);

        LedgerEntryDO entryToAdd = LedgerEntryDO.builder()
                .id(801L)
                .bizDate(LocalDate.of(2026, 8, 20)) // 补录历史漏选分录
                .partyType("CUSTOMER").partyId(1001L)
                .currency("CNY")
                .category("AR_INC")
                .originalAmount(new BigDecimal("1500.00"))
                .remark("补录发货")
                .build();
        when(ledgerEntryMapper.selectById(801L)).thenReturn(entryToAdd);
        when(ledgerEntryMapper.selectListByReversalOfId(801L)).thenReturn(Collections.emptyList());
        when(customerStatementItemMapper.selectActiveOccupiedLedgerEntryIdsExcluding(100L)).thenReturn(Collections.emptyList());
        when(customerStatementItemMapper.selectListByStatementId(100L)).thenReturn(Collections.emptyList());

        CustomerStatementAddEntryReqVO addReq = CustomerStatementAddEntryReqVO.builder()
                .statementId(100L)
                .ledgerEntryId(801L)
                .build();

        customerStatementService.addEntry(addReq);

        // 验证插入明细行
        ArgumentCaptor<CustomerStatementItemDO> itemCaptor = ArgumentCaptor.forClass(CustomerStatementItemDO.class);
        verify(customerStatementItemMapper).insert(itemCaptor.capture());
        CustomerStatementItemDO item = itemCaptor.getValue();
        assertEquals(801L, item.getLedgerEntryId());
        assertEquals(0, new BigDecimal("1500.00").compareTo(item.getDebitAmount()));

        // 验证主表发生额重算：2000 + 1500 = 3500, 期末 = 1000 + 3500 - 0 = 4500
        assertEquals(0, new BigDecimal("3500.00").compareTo(draftStatement.getPeriodDebit()));
        assertEquals(0, new BigDecimal("4500.00").compareTo(draftStatement.getClosingBalance()));
        verify(customerStatementMapper).updateById(draftStatement);
    }

    @Test
    @DisplayName("草稿手工追加分录守卫：非DRAFT状态拒绝追加抛 STATEMENT_STATUS_NOT_ALLOW")
    void testAddEntryNonDraftRejected() {
        CustomerStatementDO sentStatement = CustomerStatementDO.builder()
                .id(100L)
                .status("SENT") // 已发出不可改
                .build();
        when(customerStatementMapper.selectById(100L)).thenReturn(sentStatement);

        CustomerStatementAddEntryReqVO addReq = CustomerStatementAddEntryReqVO.builder()
                .statementId(100L)
                .ledgerEntryId(801L)
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> customerStatementService.addEntry(addReq));
        assertEquals(STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("草稿更新与删除：仅 DRAFT 允许修改与删除")
    void testUpdateAndDeleteDraft() {
        CustomerStatementDO draft = CustomerStatementDO.builder().id(100L).status("DRAFT").build();
        CustomerStatementDO confirmed = CustomerStatementDO.builder().id(200L).status("CONFIRMED").build();

        when(customerStatementMapper.selectById(100L)).thenReturn(draft);
        when(customerStatementMapper.selectById(200L)).thenReturn(confirmed);

        // 1. DRAFT 更新备注成功
        CustomerStatementUpdateReqVO updateReq = CustomerStatementUpdateReqVO.builder().id(100L).remark("新备注").build();
        customerStatementService.updateStatement(updateReq);
        assertEquals("新备注", draft.getRemark());
        verify(customerStatementMapper).updateById(draft);

        // 2. CONFIRMED 更新抛状态异常
        CustomerStatementUpdateReqVO invalidUpdate = CustomerStatementUpdateReqVO.builder().id(200L).remark("修改已确认单").build();
        ServiceException ex1 = assertThrows(ServiceException.class, () -> customerStatementService.updateStatement(invalidUpdate));
        assertEquals(STATEMENT_STATUS_NOT_ALLOW.getCode(), ex1.getCode());

        // 3. DRAFT 删除成功
        customerStatementService.deleteStatement(100L);
        verify(customerStatementMapper).deleteById(100L);
        verify(customerStatementItemMapper).delete(any());

        // 4. CONFIRMED 删除抛状态异常
        ServiceException ex2 = assertThrows(ServiceException.class, () -> customerStatementService.deleteStatement(200L));
        assertEquals(STATEMENT_STATUS_NOT_ALLOW.getCode(), ex2.getCode());
    }

}
