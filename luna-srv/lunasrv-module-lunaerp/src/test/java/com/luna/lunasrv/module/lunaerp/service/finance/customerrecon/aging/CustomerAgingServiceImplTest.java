package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging;

import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.LedgerCategory;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 客户 FIFO 账龄 Service 单元测试（Seam B）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class CustomerAgingServiceImplTest {

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @InjectMocks
    private CustomerAgingServiceImpl customerAgingService;

    private CustomerDO customerA;
    private CustomerDO customerB;

    @BeforeEach
    public void setUp() {
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, ""), CustomerDO.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, ""), LedgerEntryDO.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, ""), ShipmentDO.class);

        customerA = new CustomerDO();
        customerA.setId(100L);
        customerA.setCompanyName("客户甲");
        customerA.setSalespersonUserId(2048L);
        customerA.setCreditDays(30);

        customerB = new CustomerDO();
        customerB.setId(200L);
        customerB.setCompanyName("客户乙");
        customerB.setSalespersonUserId(2049L);
        customerB.setCreditDays(null);
    }

    @Test
    @DisplayName("跨客户不互抵测试：客户甲应收1000与客户乙核销1000严格隔离，绝不跨客户互抵")
    public void testAgingSummary_CrossCustomerNonOffsetting() {
        LocalDate queryDate = LocalDate.of(2026, 7, 1);

        when(customerMapper.selectList(any())).thenReturn(List.of(customerA, customerB));

        // 客户甲：一笔发货 AR_INC 1000.00
        LedgerEntryDO arEntryA = new LedgerEntryDO();
        arEntryA.setId(1L);
        arEntryA.setPartyType("CUSTOMER");
        arEntryA.setPartyId(100L);
        arEntryA.setCategory(LedgerCategory.AR_INC.getValue());
        arEntryA.setDirection("DEBIT");
        arEntryA.setOriginalAmount(new BigDecimal("1000.00"));
        arEntryA.setBizDate(queryDate.minusDays(10));
        arEntryA.setCurrency("CNY");

        // 客户乙：一笔核销 RECEIPT_WRITE_OFF 1000.00
        LedgerEntryDO writeOffB = new LedgerEntryDO();
        writeOffB.setId(2L);
        writeOffB.setPartyType("CUSTOMER");
        writeOffB.setPartyId(200L);
        writeOffB.setCategory(LedgerCategory.RECEIPT_WRITE_OFF.getValue());
        writeOffB.setDirection("CREDIT");
        writeOffB.setOriginalAmount(new BigDecimal("1000.00"));
        writeOffB.setBizDate(queryDate.minusDays(5));
        writeOffB.setCurrency("CNY");

        // Mock ledgerEntryMapper
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(arEntryA, writeOffB));

        CustomerAgingSummaryReqVO reqVO = CustomerAgingSummaryReqVO.builder()
                .agingDate(queryDate)
                .build();

        List<CustomerAgingSummaryRespVO> result = customerAgingService.getAgingSummary(reqVO);

        // 验证：仅客户甲有未结清应收（1000.00），客户甲绝未被客户乙的核销冲抵
        assertNotNull(result);
        assertEquals(1, result.size());
        CustomerAgingSummaryRespVO voA = result.get(0);
        assertEquals(100L, voA.getCustomerId());
        assertEquals("客户甲", voA.getCustomerName());
        assertEquals(new BigDecimal("1000.00"), voA.getTotalArBalance());
        assertEquals(new BigDecimal("1000.00"), voA.getBucket0To30());
    }

    @Test
    @DisplayName("业务员与客户ID过滤测试：按 salesmanId 与 customerIds 精确过滤")
    public void testAgingSummary_SalesmanAndCustomerIdsFilter() {
        LocalDate queryDate = LocalDate.of(2026, 7, 1);

        when(customerMapper.selectList(any())).thenReturn(List.of(customerA));
        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of());

        CustomerAgingSummaryReqVO reqVO = CustomerAgingSummaryReqVO.builder()
                .agingDate(queryDate)
                .salesmanId(2048L)
                .customerIds(List.of(100L))
                .build();

        List<CustomerAgingSummaryRespVO> result = customerAgingService.getAgingSummary(reqVO);

        assertNotNull(result);
        // customerIds 显式指定时，即使余额为0也保留客户行
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getCustomerId());
        assertEquals(BigDecimal.ZERO.setScale(2), result.get(0).getTotalArBalance());
    }

    @Test
    @DisplayName("预收余额单列不进桶测试：预收余额独立展示，不从五桶及应收合计中扣减")
    public void testAgingSummary_AdvanceBalanceIsolated() {
        LocalDate queryDate = LocalDate.of(2026, 7, 1);

        when(customerMapper.selectList(any())).thenReturn(List.of(customerA));

        // 客户甲：发货 AR_INC 1500.00 (账龄 15 天)
        LedgerEntryDO ar = new LedgerEntryDO();
        ar.setId(1L);
        ar.setPartyType("CUSTOMER");
        ar.setPartyId(100L);
        ar.setCategory(LedgerCategory.AR_INC.getValue());
        ar.setDirection("DEBIT");
        ar.setOriginalAmount(new BigDecimal("1500.00"));
        ar.setBizDate(queryDate.minusDays(15));
        ar.setCurrency("CNY");

        // 预收入账 ADVANCE 5000.00
        LedgerEntryDO adv = new LedgerEntryDO();
        adv.setId(2L);
        adv.setPartyType("CUSTOMER");
        adv.setPartyId(100L);
        adv.setCategory(LedgerCategory.ADVANCE.getValue());
        adv.setDirection("CREDIT");
        adv.setOriginalAmount(new BigDecimal("5000.00"));
        adv.setBizDate(queryDate.minusDays(20));
        adv.setCurrency("CNY");

        // 预收转核销 ADVANCE_WRITE_OFF 2000.00
        LedgerEntryDO advWriteoff = new LedgerEntryDO();
        advWriteoff.setId(3L);
        advWriteoff.setPartyType("CUSTOMER");
        advWriteoff.setPartyId(100L);
        advWriteoff.setCategory(LedgerCategory.ADVANCE_WRITE_OFF.getValue());
        advWriteoff.setDirection("CREDIT");
        advWriteoff.setOriginalAmount(new BigDecimal("2000.00"));
        advWriteoff.setBizDate(queryDate.minusDays(10));
        advWriteoff.setCurrency("CNY");

        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(ar, adv, advWriteoff));

        CustomerAgingSummaryReqVO reqVO = CustomerAgingSummaryReqVO.builder()
                .agingDate(queryDate)
                .customerIds(List.of(100L))
                .build();

        List<CustomerAgingSummaryRespVO> result = customerAgingService.getAgingSummary(reqVO);

        assertEquals(1, result.size());
        CustomerAgingSummaryRespVO vo = result.get(0);
        // AR 1500 消耗 2000 (ADVANCE_WRITE_OFF 作为应收减少冲抵最老 AR_INC) -> remaining = 0
        // 预收余额 = 5000 - 2000 = 3000
        assertEquals(new BigDecimal("3000.00"), vo.getAdvanceBalance());
        assertEquals(BigDecimal.ZERO.setScale(2), vo.getTotalArBalance());
    }

    @Test
    @DisplayName("下钻明细测试：返回客户未核销明细分录并加载发货单号与合同号")
    public void testAgingDetail_DrillDownItems() {
        LocalDate queryDate = LocalDate.of(2026, 7, 1);

        when(customerMapper.selectById(100L)).thenReturn(customerA);

        // AR_INC 1: 发生额 2000，发货日 45 天前 (31-60 桶)
        LedgerEntryDO entry1 = new LedgerEntryDO();
        entry1.setId(10L);
        entry1.setPartyType("CUSTOMER");
        entry1.setPartyId(100L);
        entry1.setCategory(LedgerCategory.AR_INC.getValue());
        entry1.setDirection("DEBIT");
        entry1.setOriginalAmount(new BigDecimal("2000.00"));
        entry1.setBizDate(queryDate.minusDays(45));
        entry1.setRefType("SHIPMENT_ITEM");
        entry1.setRefId(501L);
        entry1.setRemark("纯棉纱 40S");
        entry1.setCurrency("CNY");

        // RECEIPT_WRITE_OFF 500.00 冲抵最老 AR_INC
        LedgerEntryDO writeOff = new LedgerEntryDO();
        writeOff.setId(11L);
        writeOff.setPartyType("CUSTOMER");
        writeOff.setPartyId(100L);
        writeOff.setCategory(LedgerCategory.RECEIPT_WRITE_OFF.getValue());
        writeOff.setDirection("CREDIT");
        writeOff.setOriginalAmount(new BigDecimal("500.00"));
        writeOff.setBizDate(queryDate.minusDays(10));
        writeOff.setCurrency("CNY");

        when(ledgerEntryMapper.selectList(any())).thenReturn(List.of(entry1, writeOff));

        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(501L);
        shipment.setShipmentNo("SH20260515001");
        shipment.setContractNo("HT-2026-001");
        when(shipmentMapper.selectList(any())).thenReturn(List.of(shipment));

        CustomerAgingDetailReqVO reqVO = CustomerAgingDetailReqVO.builder()
                .customerId(100L)
                .agingDate(queryDate)
                .build();

        List<CustomerAgingDetailRespVO> details = customerAgingService.getAgingDetail(reqVO);

        assertNotNull(details);
        assertEquals(1, details.size());
        CustomerAgingDetailRespVO item = details.get(0);
        assertEquals(10L, item.getLedgerEntryId());
        assertEquals(new BigDecimal("2000.00"), item.getOriginalAmount());
        assertEquals(new BigDecimal("500.00"), item.getWrittenOffAmount());
        assertEquals(new BigDecimal("1500.00"), item.getRemainingAmount());
        assertEquals(45, item.getAgingDays());
        assertEquals("31-60", item.getBucket());
        assertEquals("SH20260515001", item.getRefNo());
        assertEquals("HT-2026-001", item.getContractNo());
        assertEquals("纯棉纱 40S", item.getProductName());

        // 逾期判断：发货日 45 天前，账期 30 天，已逾期 15 天
        assertTrue(item.getIsOverdue());
        assertEquals(15, item.getOverdueDays());
    }
}
