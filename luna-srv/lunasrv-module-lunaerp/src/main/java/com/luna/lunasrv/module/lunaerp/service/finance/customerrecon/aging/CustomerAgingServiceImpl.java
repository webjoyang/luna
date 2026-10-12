package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
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
import com.luna.lunasrv.module.lunaerp.enums.finance.FinancePartyTypeEnum;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 客户 FIFO 账龄分析服务实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class CustomerAgingServiceImpl implements CustomerAgingService {

    private static final String DEFAULT_CURRENCY = "CNY";
    private static final String RECEIPT_TYPE_ADVANCE = "ADVANCE";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String DIRECTION_IN = "IN";

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private LedgerEntryMapper ledgerEntryMapper;

    @Resource
    private PaymentMapper paymentMapper;

    @Resource(name = "lunaerpShipmentMapper")
    private ShipmentMapper shipmentMapper;

    @Autowired(required = false)
    private AdminUserApi adminUserApi;

    @Override
    public List<CustomerAgingSummaryRespVO> getAgingSummary(CustomerAgingSummaryReqVO reqVO) {
        LocalDate agingDate = reqVO.getAgingDate() != null ? reqVO.getAgingDate() : LocalDate.now();
        String currency = StrUtil.isNotBlank(reqVO.getCurrency()) ? reqVO.getCurrency().trim().toUpperCase() : DEFAULT_CURRENCY;

        // 1. 查询目标客户列表
        LambdaQueryWrapper<CustomerDO> customerWrapper = new LambdaQueryWrapperX<CustomerDO>()
                .inIfPresent(CustomerDO::getId, reqVO.getCustomerIds())
                .eqIfPresent(CustomerDO::getSalespersonUserId, reqVO.getSalesmanId())
                .orderByAsc(CustomerDO::getId);
        List<CustomerDO> customers = customerMapper.selectList(customerWrapper);
        if (CollUtil.isEmpty(customers)) {
            return Collections.emptyList();
        }

        // 2. 批量解析业务员姓名
        Map<Long, String> salesmanNameMap = resolveSalesmanNames(customers);

        // 3. 逐客户纯函数计算（按客户独立计算，不跨客户互抵）
        List<CustomerAgingSummaryRespVO> resultList = new ArrayList<>();
        for (CustomerDO customer : customers) {
            // (1) 查询该客户截至 agingDate 的所有分录
            LambdaQueryWrapper<LedgerEntryDO> entryWrapper = new LambdaQueryWrapperX<LedgerEntryDO>()
                    .eq(LedgerEntryDO::getPartyType, FinancePartyTypeEnum.CUSTOMER.getValue())
                    .eq(LedgerEntryDO::getPartyId, customer.getId())
                    .le(LedgerEntryDO::getBizDate, agingDate)
                    .orderByAsc(LedgerEntryDO::getBizDate)
                    .orderByAsc(LedgerEntryDO::getId);
            List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(entryWrapper);

            // (2) 计算预收余额（ADVANCE 入账 − ADVANCE_WRITE_OFF）
            BigDecimal advanceBalance = calculateCustomerAdvanceBalance(customer.getId(), currency, agingDate, entries);

            // (3) 纯函数实时计算账龄与五桶
            CustomerAgingCalculator.AgingCalculationResult calcResult = CustomerAgingCalculator.calculate(
                    customer,
                    currency,
                    agingDate,
                    entries,
                    advanceBalance,
                    null
            );

            CustomerAgingSummaryRespVO summaryVO = calcResult.getSummary();
            if (summaryVO != null) {
                if (customer.getSalespersonUserId() != null) {
                    summaryVO.setSalesmanName(salesmanNameMap.get(customer.getSalespersonUserId()));
                }
                resultList.add(summaryVO);
            }
        }

        return resultList;
    }

    @Override
    public List<CustomerAgingDetailRespVO> getAgingDetail(CustomerAgingDetailReqVO reqVO) {
        if (reqVO == null || reqVO.getCustomerId() == null) {
            throw exception(BAD_REQUEST, "客户ID不能为空");
        }

        CustomerDO customer = customerMapper.selectById(reqVO.getCustomerId());
        if (customer == null) {
            throw exception(BAD_REQUEST, "客户不存在: id=" + reqVO.getCustomerId());
        }

        LocalDate agingDate = reqVO.getAgingDate() != null ? reqVO.getAgingDate() : LocalDate.now();
        String currency = StrUtil.isNotBlank(reqVO.getCurrency()) ? reqVO.getCurrency().trim().toUpperCase() : DEFAULT_CURRENCY;

        // 查询该客户截至 agingDate 的所有分录
        LambdaQueryWrapper<LedgerEntryDO> entryWrapper = new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getPartyType, FinancePartyTypeEnum.CUSTOMER.getValue())
                .eq(LedgerEntryDO::getPartyId, customer.getId())
                .le(LedgerEntryDO::getBizDate, agingDate)
                .orderByAsc(LedgerEntryDO::getBizDate)
                .orderByAsc(LedgerEntryDO::getId);
        List<LedgerEntryDO> entries = ledgerEntryMapper.selectList(entryWrapper);

        // 单号解析器
        Function<LedgerEntryDO, String> refNoResolver = entry -> {
            if ("SHIPMENT".equalsIgnoreCase(entry.getRefType()) && entry.getRefId() != null) {
                try {
                    ShipmentDO shipment = shipmentMapper.selectById(entry.getRefId());
                    if (shipment != null && StrUtil.isNotBlank(shipment.getShipmentNo())) {
                        return shipment.getShipmentNo();
                    }
                } catch (Exception ignored) {
                }
            }
            return StrUtil.isNotBlank(entry.getRemark()) ? entry.getRemark() : String.valueOf(entry.getId());
        };

        CustomerAgingCalculator.AgingCalculationResult calcResult = CustomerAgingCalculator.calculate(
                customer,
                currency,
                agingDate,
                entries,
                BigDecimal.ZERO,
                refNoResolver
        );

        return calcResult.getDetails();
    }

    /**
     * 计算客户可用预收余额：ADVANCE 入账 − ADVANCE_WRITE_OFF
     */
    private BigDecimal calculateCustomerAdvanceBalance(Long customerId, String currency, LocalDate agingDate, List<LedgerEntryDO> entries) {
        BigDecimal advanceInTotal = BigDecimal.ZERO;
        BigDecimal advanceWriteoffTotal = BigDecimal.ZERO;

        // 1. 尝试从 Payment 表累计已过账的 ADVANCE 收款单
        try {
            List<PaymentDO> advancePayments = paymentMapper.selectList(new LambdaQueryWrapper<PaymentDO>()
                    .eq(PaymentDO::getCustomerId, customerId)
                    .eq(PaymentDO::getDirection, DIRECTION_IN)
                    .eq(PaymentDO::getReceiptType, RECEIPT_TYPE_ADVANCE)
                    .eq(PaymentDO::getStatus, STATUS_COMPLETED)
                    .le(PaymentDO::getReceiptDate, agingDate));
            if (CollUtil.isNotEmpty(advancePayments)) {
                for (PaymentDO p : advancePayments) {
                    if (StrUtil.isBlank(currency) || currency.equalsIgnoreCase(p.getCurrency())) {
                        BigDecimal amt = p.getPayAmount() != null ? p.getPayAmount() : BigDecimal.ZERO;
                        advanceInTotal = advanceInTotal.add(amt);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[calculateCustomerAdvanceBalance] 从 Payment 表检索预收单异常: customerId={}, err={}", customerId, e.getMessage());
        }

        // 2. 检查分录中的 ADVANCE 入账（如果测试直接通过分录传入）
        if (CollUtil.isNotEmpty(entries)) {
            for (LedgerEntryDO e : entries) {
                String cat = e.getCategory();
                if ("ADVANCE".equalsIgnoreCase(cat)) {
                    BigDecimal amt = e.getOriginalAmount() != null ? e.getOriginalAmount() : BigDecimal.ZERO;
                    advanceInTotal = advanceInTotal.add(amt);
                } else if (FinanceLedgerCategoryEnum.ADVANCE_WRITE_OFF.getValue().equalsIgnoreCase(cat)) {
                    BigDecimal amt = e.getOriginalAmount() != null ? e.getOriginalAmount() : BigDecimal.ZERO;
                    advanceWriteoffTotal = advanceWriteoffTotal.add(amt);
                }
            }
        }

        BigDecimal balance = advanceInTotal.subtract(advanceWriteoffTotal);
        return balance.compareTo(BigDecimal.ZERO) > 0 ? balance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, String> resolveSalesmanNames(List<CustomerDO> customers) {
        if (adminUserApi == null || CollUtil.isEmpty(customers)) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = customers.stream()
                .map(CustomerDO::getSalespersonUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        try {
            Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
            if (CollUtil.isEmpty(userMap)) {
                return Collections.emptyMap();
            }
            Map<Long, String> result = new HashMap<>();
            userMap.forEach((id, dto) -> {
                if (dto != null) {
                    result.put(id, dto.getNickname());
                }
            });
            return result;
        } catch (Exception e) {
            log.warn("[resolveSalesmanNames] 调用 AdminUserApi 查询业务员信息异常: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

}
