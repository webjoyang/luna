package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.util.collection.CollectionUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.LedgerCategory;
import com.luna.lunasrv.module.lunaerp.enums.finance.PaymentStatus;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.STATEMENT_CUSTOMER_MISMATCH;

/**
 * 客户 FIFO 账龄 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class CustomerAgingServiceImpl implements CustomerAgingService {

    private static final String DEFAULT_CURRENCY = "CNY";

    private static final Set<String> CREDIT_REDUCTION_CATEGORIES = Set.of(
            LedgerCategory.RECEIPT_WRITE_OFF.getValue(),
            LedgerCategory.ADVANCE_WRITE_OFF.getValue(),
            LedgerCategory.AR_RETURN_REVERSAL.getValue()
    );

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private LedgerEntryMapper ledgerEntryMapper;

    @Resource
    private ShipmentMapper shipmentMapper;

    @Resource
    private PaymentMapper paymentMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public List<CustomerAgingSummaryRespVO> getAgingSummary(CustomerAgingSummaryReqVO reqVO) {
        LocalDate queryDate = reqVO != null && reqVO.getAgingDate() != null ? reqVO.getAgingDate() : LocalDate.now();

        // 1. 查询匹配的客户列表
        LambdaQueryWrapper<CustomerDO> customerQuery = new LambdaQueryWrapperX<CustomerDO>()
                .inIfPresent(CustomerDO::getId, reqVO != null ? reqVO.getCustomerIds() : null)
                .eqIfPresent(CustomerDO::getSalespersonUserId, reqVO != null ? reqVO.getEffectiveSalespersonUserId() : null)
                .orderByAsc(CustomerDO::getId);

        List<CustomerDO> customers = customerMapper.selectList(customerQuery);
        if (CollUtil.isEmpty(customers)) {
            return Collections.emptyList();
        }

        List<Long> customerIds = CollectionUtils.convertList(customers, CustomerDO::getId);

        // 2. 批量查询所有客户在基准日及之前的经营分录
        List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getPartyType, "CUSTOMER")
                .inOrEmpty(LedgerEntryDO::getPartyId, customerIds)
                .le(LedgerEntryDO::getBizDate, queryDate)
                .orderByAsc(LedgerEntryDO::getBizDate)
                .orderByAsc(LedgerEntryDO::getId));

        // 批量识别已红冲的收款单ID，其关联核销分录不应冲减应收
        Set<Long> reversedPaymentIds = getReversedPaymentIds(entries);

        // 按 customerId 分组
        Map<Long, List<LedgerEntryDO>> customerEntriesMap = entries.stream()
                .filter(e -> e.getPartyId() != null)
                .collect(Collectors.groupingBy(LedgerEntryDO::getPartyId));

        // 3. 批量查询业务员用户信息
        Set<Long> userIds = customers.stream()
                .map(CustomerDO::getSalespersonUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = Collections.emptyMap();
        if (adminUserApi != null && CollUtil.isNotEmpty(userIds)) {
            try {
                userMap = adminUserApi.getUserMap(userIds);
            } catch (Exception e) {
                log.warn("[getAgingSummary] 加载业务员用户信息异常: userIds={}", userIds, e);
            }
        }

        List<CustomerAgingSummaryRespVO> result = new ArrayList<>();

        for (CustomerDO customer : customers) {
            List<LedgerEntryDO> custEntries = customerEntriesMap.getOrDefault(customer.getId(), Collections.emptyList());
            CustomerParsedEntries parsed = parseCustomerEntries(custEntries, reversedPaymentIds);

            // 调用 FIFO 纯函数计算
            CustomerAgingCalculator.AgingResult agingResult = CustomerAgingCalculator.calculate(
                    customer.getId(),
                    customer.getCompanyName(),
                    customer.getCreditDays(),
                    queryDate,
                    parsed.getArEntries(),
                    parsed.getReductions(),
                    parsed.getAdvanceBalance()
            );

            // 获取业务员姓名
            String salesmanName = null;
            if (customer.getSalespersonUserId() != null && userMap.containsKey(customer.getSalespersonUserId())) {
                salesmanName = userMap.get(customer.getSalespersonUserId()).getNickname();
            }

            result.add(CustomerAgingSummaryRespVO.builder()
                    .customerId(customer.getId())
                    .customerName(customer.getCompanyName())
                    .creditDays(customer.getCreditDays())
                    .salespersonUserId(customer.getSalespersonUserId())
                    .salespersonName(salesmanName)
                    .currency(DEFAULT_CURRENCY)
                    .totalArBalance(agingResult.getTotalArBalance())
                    .bucket0To30(agingResult.getBucket0To30())
                    .bucket31To60(agingResult.getBucket31To60())
                    .bucket61To90(agingResult.getBucket61To90())
                    .bucket91To180(agingResult.getBucket91To180())
                    .bucket181Plus(agingResult.getBucket181Plus())
                    .advanceBalance(agingResult.getAdvanceBalance())
                    .overdueAmount(agingResult.getOverdueAmount())
                    .agingDate(queryDate)
                    .build());
        }

        return result;
    }

    @Override
    public List<CustomerAgingDetailRespVO> getAgingDetail(CustomerAgingDetailReqVO reqVO) {
        if (reqVO == null || reqVO.getCustomerId() == null) {
            throw exception(STATEMENT_CUSTOMER_MISMATCH);
        }
        CustomerDO customer = customerMapper.selectById(reqVO.getCustomerId());
        if (customer == null) {
            throw exception(STATEMENT_CUSTOMER_MISMATCH);
        }
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && customer.getTenantId() != null && !currentTenantId.equals(customer.getTenantId())) {
            throw exception(STATEMENT_CUSTOMER_MISMATCH);
        }

        LocalDate queryDate = reqVO.getAgingDate() != null ? reqVO.getAgingDate() : LocalDate.now();

        // 1. 查询该客户截至基准日的分录
        List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getPartyType, "CUSTOMER")
                .eq(LedgerEntryDO::getPartyId, customer.getId())
                .le(LedgerEntryDO::getBizDate, queryDate)
                .orderByAsc(LedgerEntryDO::getBizDate)
                .orderByAsc(LedgerEntryDO::getId));

        // 批量识别已红冲的收款单ID
        Set<Long> reversedPaymentIds = getReversedPaymentIds(entries);
        CustomerParsedEntries parsed = parseCustomerEntries(entries, reversedPaymentIds);

        // 批量加载发货单信息
        Map<Long, ShipmentDO> shipmentMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(parsed.getShipmentIds())) {
            List<ShipmentDO> shipments = shipmentMapper.selectList(new LambdaQueryWrapperX<ShipmentDO>()
                    .inOrEmpty(ShipmentDO::getId, parsed.getShipmentIds()));
            shipmentMap = CollectionUtils.convertMap(shipments, ShipmentDO::getId);
        }

        // 2. 调用 FIFO 纯函数计算
        CustomerAgingCalculator.AgingResult agingResult = CustomerAgingCalculator.calculate(
                customer.getId(),
                customer.getCompanyName(),
                customer.getCreditDays(),
                queryDate,
                parsed.getArEntries(),
                parsed.getReductions(),
                BigDecimal.ZERO
        );

        // 3. 组装返回下钻明细
        List<CustomerAgingDetailRespVO> result = new ArrayList<>();
        for (CustomerAgingCalculator.AgingDetailItem item : agingResult.getDetailItems()) {
            ShipmentDO shipment = item.getRefId() != null ? shipmentMap.get(item.getRefId()) : null;
            String shipmentNo = shipment != null ? shipment.getShipmentNo() : "";
            String contractNo = shipment != null ? shipment.getContractNo() : "";

            result.add(CustomerAgingDetailRespVO.builder()
                    .ledgerEntryId(item.getLedgerEntryId())
                    .bizDate(item.getBizDate())
                    .refType(item.getRefType())
                    .refId(item.getRefId())
                    .refNo(shipmentNo)
                    .contractNo(contractNo)
                    .productName(item.getSummary())
                    .currency(DEFAULT_CURRENCY)
                    .originalAmount(item.getOriginalAmount())
                    .writtenOffAmount(item.getWrittenOffAmount())
                    .remainingAmount(item.getRemainingAmount())
                    .agingDays(item.getAgingDays())
                    .bucket(item.getBucket())
                    .creditDays(item.getCreditDays())
                    .dueDate(item.getDueDate())
                    .isOverdue(item.getIsOverdue())
                    .overdueDays(item.getOverdueDays())
                    .build());
        }

        return result;
    }

    /**
     * 识别与分录关联但已被红冲的收款单ID集合
     */
    private Set<Long> getReversedPaymentIds(List<LedgerEntryDO> entries) {
        if (CollUtil.isEmpty(entries) || paymentMapper == null) {
            return Collections.emptySet();
        }
        Set<Long> paymentIds = entries.stream()
                .filter(e -> "RECEIPT".equalsIgnoreCase(e.getRefType()) && e.getRefId() != null)
                .map(LedgerEntryDO::getRefId)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(paymentIds)) {
            return Collections.emptySet();
        }
        List<PaymentDO> payments = paymentMapper.selectBatchIds(paymentIds);
        if (CollUtil.isEmpty(payments)) {
            return Collections.emptySet();
        }
        return payments.stream()
                .filter(p -> PaymentStatus.REVERSED.getValue().equalsIgnoreCase(p.getStatus()))
                .map(PaymentDO::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 解析客户分录：过滤已红冲收款核销、归集应收增加净额与减少分录、核算预收余额
     */
    private CustomerParsedEntries parseCustomerEntries(List<LedgerEntryDO> custEntries, Set<Long> reversedPaymentIds) {
        List<LedgerEntryDO> arIncEntries = new ArrayList<>();
        List<LedgerEntryDO> arIncReversals = new ArrayList<>();
        List<CustomerAgingCalculator.CreditReduction> reductions = new ArrayList<>();
        List<LedgerEntryDO> advanceEntries = new ArrayList<>();
        List<LedgerEntryDO> advanceReversals = new ArrayList<>();
        List<LedgerEntryDO> advanceWriteOffEntries = new ArrayList<>();

        for (LedgerEntryDO e : custEntries) {
            String cat = e.getCategory();
            boolean isReversedPayment = "RECEIPT".equalsIgnoreCase(e.getRefType())
                    && e.getRefId() != null
                    && reversedPaymentIds.contains(e.getRefId());

            if (LedgerCategory.AR_INC.getValue().equalsIgnoreCase(cat)) {
                arIncEntries.add(e);
            } else if (LedgerCategory.AR_INC_REVERSAL.getValue().equalsIgnoreCase(cat)) {
                arIncReversals.add(e);
            } else if (CREDIT_REDUCTION_CATEGORIES.contains(cat)) {
                if (!isReversedPayment) {
                    reductions.add(new CustomerAgingCalculator.CreditReduction(
                            e.getId(), e.getBizDate(), cat, e.getOriginalAmount()));
                }
            } else if (LedgerCategory.ADVANCE.getValue().equalsIgnoreCase(cat)) {
                if (!isReversedPayment) {
                    advanceEntries.add(e);
                }
            } else if (LedgerCategory.RECEIPT_REVERSAL.getValue().equalsIgnoreCase(cat)) {
                if (!isReversedPayment) {
                    advanceReversals.add(e);
                }
            }
            if (LedgerCategory.ADVANCE_WRITE_OFF.getValue().equalsIgnoreCase(cat) && !isReversedPayment) {
                advanceWriteOffEntries.add(e);
            }
        }

        // 统计 AR_INC 的红冲
        Map<Long, BigDecimal> arReversalMap = new HashMap<>();
        for (LedgerEntryDO rev : arIncReversals) {
            if (rev.getReversalOfId() != null && rev.getOriginalAmount() != null) {
                arReversalMap.merge(rev.getReversalOfId(), rev.getOriginalAmount(), BigDecimal::add);
            }
        }

        List<CustomerAgingCalculator.ArEntry> calcArEntries = new ArrayList<>();
        Set<Long> shipmentIds = new HashSet<>();
        for (LedgerEntryDO e : arIncEntries) {
            BigDecimal revAmount = arReversalMap.getOrDefault(e.getId(), BigDecimal.ZERO);
            calcArEntries.add(new CustomerAgingCalculator.ArEntry(
                    e.getId(), e.getBizDate(), e.getOriginalAmount(), e.getRefType(),
                    e.getRefId(), "", e.getRemark(), revAmount));
            if (e.getRefId() != null && ("SHIPMENT_ITEM".equalsIgnoreCase(e.getRefType()) || "SHIPMENT_FREIGHT".equalsIgnoreCase(e.getRefType()))) {
                shipmentIds.add(e.getRefId());
            }
        }

        // 统计预收余额 = (ADVANCE 入账 - 其红冲) - ADVANCE_WRITE_OFF
        Map<Long, BigDecimal> advReversalMap = new HashMap<>();
        for (LedgerEntryDO rev : advanceReversals) {
            if (rev.getReversalOfId() != null && rev.getOriginalAmount() != null) {
                advReversalMap.merge(rev.getReversalOfId(), rev.getOriginalAmount(), BigDecimal::add);
            }
        }

        BigDecimal totalAdvance = BigDecimal.ZERO;
        for (LedgerEntryDO adv : advanceEntries) {
            BigDecimal orig = adv.getOriginalAmount() != null ? adv.getOriginalAmount() : BigDecimal.ZERO;
            BigDecimal rev = advReversalMap.getOrDefault(adv.getId(), BigDecimal.ZERO);
            BigDecimal net = orig.subtract(rev);
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                totalAdvance = totalAdvance.add(net);
            }
        }

        BigDecimal totalAdvanceWriteOff = BigDecimal.ZERO;
        for (LedgerEntryDO awo : advanceWriteOffEntries) {
            if (awo.getOriginalAmount() != null) {
                totalAdvanceWriteOff = totalAdvanceWriteOff.add(awo.getOriginalAmount());
            }
        }

        BigDecimal advanceBalance = totalAdvance.subtract(totalAdvanceWriteOff);
        if (advanceBalance.compareTo(BigDecimal.ZERO) < 0) {
            advanceBalance = BigDecimal.ZERO;
        }

        return new CustomerParsedEntries(calcArEntries, reductions, advanceBalance, shipmentIds);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class CustomerParsedEntries {
        private final List<CustomerAgingCalculator.ArEntry> arEntries;
        private final List<CustomerAgingCalculator.CreditReduction> reductions;
        private final BigDecimal advanceBalance;
        private final Set<Long> shipmentIds;
    }
}
