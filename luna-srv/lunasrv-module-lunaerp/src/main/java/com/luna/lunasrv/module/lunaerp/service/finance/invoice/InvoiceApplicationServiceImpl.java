package com.luna.lunasrv.module.lunaerp.service.finance.invoice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
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
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 财务管理 - 发票申请 Service 实现类
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class InvoiceApplicationServiceImpl implements InvoiceApplicationService {

    private static final String INVOICE_NO_PREFIX = "IA";
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final int NO_MIN_DIGITS = 4;
    private static final int MAX_GENERATE_RETRY = 50;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("\\d+");

    private static final Set<String> OCCUPY_STATUSES = Set.of(
            InvoiceApplicationStatus.SUBMITTED.getValue(),
            InvoiceApplicationStatus.ISSUED.getValue()
    );

    @Resource
    private InvoiceApplicationMapper invoiceApplicationMapper;

    @Resource
    private InvoiceApplicationItemMapper invoiceApplicationItemMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private ShipmentMapper shipmentMapper;

    @Override
    public PageResult<InvoiceApplicationRespVO> getInvoiceApplicationPage(InvoiceApplicationPageReqVO pageReqVO) {
        LambdaQueryWrapperX<InvoiceApplicationDO> wrapper = new LambdaQueryWrapperX<InvoiceApplicationDO>()
                .likeIfPresent(InvoiceApplicationDO::getInvoiceNo, pageReqVO.getInvoiceNo())
                .eqIfPresent(InvoiceApplicationDO::getCustomerId, pageReqVO.getCustomerId())
                .likeIfPresent(InvoiceApplicationDO::getCustomerName, pageReqVO.getCustomerName())
                .eqIfPresent(InvoiceApplicationDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(InvoiceApplicationDO::getInvoiceKind, pageReqVO.getInvoiceKind())
                .likeIfPresent(InvoiceApplicationDO::getOfficialInvoiceNo, pageReqVO.getOfficialInvoiceNo())
                .orderByDesc(InvoiceApplicationDO::getId);

        PageResult<InvoiceApplicationDO> pageResult = invoiceApplicationMapper.selectPage(pageReqVO, wrapper);
        List<InvoiceApplicationRespVO> list = pageResult.getList().stream()
                .map(this::convertDOToRespVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public InvoiceApplicationDetailRespVO getInvoiceApplication(Long id) {
        InvoiceApplicationDO app = validateInvoiceApplicationExists(id);
        List<InvoiceApplicationItemDO> items = invoiceApplicationItemMapper.selectList(
                new LambdaQueryWrapperX<InvoiceApplicationItemDO>()
                        .eq(InvoiceApplicationItemDO::getApplicationId, id)
                        .orderByAsc(InvoiceApplicationItemDO::getLineNo));

        InvoiceApplicationDetailRespVO detail = new InvoiceApplicationDetailRespVO();
        copyProperties(app, detail);
        detail.setItems(items.stream().map(this::convertItemDOToRespVO).collect(Collectors.toList()));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInvoiceApplication(InvoiceApplicationCreateReqVO createReqVO) {
        // 1. 客户校验与四要素带出
        CustomerDO customer = customerMapper.selectById(createReqVO.getCustomerId());
        if (customer == null) {
            throw exception(INVOICE_CUSTOMER_MISMATCH);
        }

        String invoiceTitle = StrUtil.isNotBlank(createReqVO.getInvoiceTitle()) ? createReqVO.getInvoiceTitle().trim()
                : (StrUtil.isNotBlank(customer.getInvoiceTitle()) ? customer.getInvoiceTitle().trim() : customer.getCompanyName());
        String taxNo = StrUtil.isNotBlank(createReqVO.getTaxNo()) ? createReqVO.getTaxNo().trim()
                : customer.getUnifiedSocialCreditCode();
        String address = StrUtil.isNotBlank(createReqVO.getAddress()) ? createReqVO.getAddress().trim()
                : (StrUtil.isNotBlank(customer.getInvoiceAddress()) ? customer.getInvoiceAddress().trim() : customer.getAddress());
        String bankInfo = StrUtil.isNotBlank(createReqVO.getBankInfo()) ? createReqVO.getBankInfo().trim()
                : buildCustomerBankInfo(customer);

        // 2. 发货单校验（必须存在、COMPLETED、客户一致、币种一致）
        List<Long> shipmentIds = createReqVO.getItems().stream()
                .map(InvoiceApplicationItemSaveReqVO::getShipmentId)
                .distinct()
                .collect(Collectors.toList());
        List<ShipmentDO> shipments = shipmentMapper.selectBatchIds(shipmentIds);
        if (shipments.size() != shipmentIds.size()) {
            throw exception(INVOICE_EXCEED_SHIPMENT_AMOUNT);
        }
        Map<Long, ShipmentDO> shipmentMap = shipments.stream().collect(Collectors.toMap(ShipmentDO::getId, s -> s));

        for (ShipmentDO shipment : shipments) {
            if (!"COMPLETED".equalsIgnoreCase(shipment.getStatus())) {
                throw exception(INVOICE_STATUS_NOT_ALLOW);
            }
            if (!Objects.equals(shipment.getCustomerId(), createReqVO.getCustomerId())) {
                throw exception(INVOICE_CUSTOMER_MISMATCH);
            }
            if (!StrUtil.equalsIgnoreCase(shipment.getCurrency(), createReqVO.getCurrency())) {
                throw exception(INVOICE_CURRENCY_MIXED);
            }
        }

        // 3. 超开守卫（逐发货单聚合已占用额度与本次申请额度）
        Map<Long, BigDecimal> currentReqShipmentAmounts = new HashMap<>();
        for (InvoiceApplicationItemSaveReqVO itemReq : createReqVO.getItems()) {
            BigDecimal amt = itemReq.getAmount() != null ? itemReq.getAmount() : BigDecimal.ZERO;
            currentReqShipmentAmounts.merge(itemReq.getShipmentId(), amt, BigDecimal::add);
        }

        for (Map.Entry<Long, BigDecimal> entry : currentReqShipmentAmounts.entrySet()) {
            Long shipmentId = entry.getKey();
            BigDecimal applyAmount = entry.getValue();
            ShipmentDO shipment = shipmentMap.get(shipmentId);
            BigDecimal shipmentTotal = (shipment.getTotalAmount() != null ? shipment.getTotalAmount() : BigDecimal.ZERO)
                    .add(shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO);

            BigDecimal occupiedAmount = calculateOccupiedInvoiceAmount(shipmentId, null);
            if (occupiedAmount.add(applyAmount).compareTo(shipmentTotal) > 0) {
                log.warn("[createInvoiceApplication] 超开拦截: shipmentId={}, shipmentTotal={}, occupied={}, apply={}",
                        shipmentId, shipmentTotal, occupiedAmount, applyAmount);
                throw exception(INVOICE_EXCEED_SHIPMENT_AMOUNT);
            }
        }

        // 4. 税额服务端反算
        BigDecimal totalAmountInclTax = createReqVO.getItems().stream()
                .map(InvoiceApplicationItemSaveReqVO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        InvoiceTaxCalculator.TaxCalculationResult taxCalc = InvoiceTaxCalculator.calculate(totalAmountInclTax, createReqVO.getTaxRate());

        // 5. IA 取号
        String invoiceNo = generateInvoiceNo(LocalDate.now());

        // 6. 落库主表
        InvoiceApplicationDO app = new InvoiceApplicationDO();
        app.setInvoiceNo(invoiceNo);
        app.setCustomerId(customer.getId());
        app.setCustomerName(customer.getCompanyName());
        app.setCurrency(createReqVO.getCurrency());
        app.setTaxRate(taxCalc.getTaxRate());
        app.setInvoiceKind(createReqVO.getInvoiceKind());
        app.setElectronic(createReqVO.getElectronic() != null ? createReqVO.getElectronic() : 1);
        app.setInvoiceTitle(invoiceTitle);
        app.setTaxNo(taxNo);
        app.setAddress(address);
        app.setBankInfo(bankInfo);
        app.setStatus(InvoiceApplicationStatus.DRAFT.getValue());
        app.setTotalAmountExclTax(taxCalc.getTotalAmountExclTax());
        app.setTotalTax(taxCalc.getTotalTax());
        app.setTotalAmountInclTax(taxCalc.getTotalAmountInclTax());
        app.setRemark(createReqVO.getRemark());

        invoiceApplicationMapper.insert(app);

        // 7. 落库明细行
        int lineNo = 1;
        for (InvoiceApplicationItemSaveReqVO itemReq : createReqVO.getItems()) {
            ShipmentDO shipment = shipmentMap.get(itemReq.getShipmentId());
            BigDecimal shipmentTotal = (shipment.getTotalAmount() != null ? shipment.getTotalAmount() : BigDecimal.ZERO)
                    .add(shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO);

            InvoiceApplicationItemDO itemDO = new InvoiceApplicationItemDO();
            itemDO.setApplicationId(app.getId());
            itemDO.setLineNo(lineNo++);
            itemDO.setShipmentId(shipment.getId());
            itemDO.setShipmentNo(shipment.getShipmentNo());
            itemDO.setShipmentAmount(shipmentTotal);
            itemDO.setAmount(itemReq.getAmount().setScale(2, RoundingMode.HALF_UP));
            itemDO.setRemark(itemReq.getRemark());
            invoiceApplicationItemMapper.insert(itemDO);
        }

        return app.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInvoiceApplication(InvoiceApplicationUpdateReqVO updateReqVO) {
        InvoiceApplicationDO app = validateInvoiceApplicationExists(updateReqVO.getId());
        if (!InvoiceApplicationStatus.DRAFT.getValue().equals(app.getStatus())) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        if (CollUtil.isNotEmpty(updateReqVO.getItems())) {
            List<Long> shipmentIds = updateReqVO.getItems().stream()
                    .map(InvoiceApplicationItemSaveReqVO::getShipmentId)
                    .distinct()
                    .collect(Collectors.toList());
            List<ShipmentDO> shipments = shipmentMapper.selectBatchIds(shipmentIds);
            if (shipments.size() != shipmentIds.size()) {
                throw exception(INVOICE_EXCEED_SHIPMENT_AMOUNT);
            }
            Map<Long, ShipmentDO> shipmentMap = shipments.stream().collect(Collectors.toMap(ShipmentDO::getId, s -> s));

            for (ShipmentDO shipment : shipments) {
                if (!"COMPLETED".equalsIgnoreCase(shipment.getStatus())) {
                    throw exception(INVOICE_STATUS_NOT_ALLOW);
                }
                if (!Objects.equals(shipment.getCustomerId(), app.getCustomerId())) {
                    throw exception(INVOICE_CUSTOMER_MISMATCH);
                }
                if (!StrUtil.equalsIgnoreCase(shipment.getCurrency(), app.getCurrency())) {
                    throw exception(INVOICE_CURRENCY_MIXED);
                }
            }

            // 超开校验（排除当前申请自身的已占额）
            Map<Long, BigDecimal> currentReqShipmentAmounts = new HashMap<>();
            for (InvoiceApplicationItemSaveReqVO itemReq : updateReqVO.getItems()) {
                BigDecimal amt = itemReq.getAmount() != null ? itemReq.getAmount() : BigDecimal.ZERO;
                currentReqShipmentAmounts.merge(itemReq.getShipmentId(), amt, BigDecimal::add);
            }

            for (Map.Entry<Long, BigDecimal> entry : currentReqShipmentAmounts.entrySet()) {
                Long shipmentId = entry.getKey();
                BigDecimal applyAmount = entry.getValue();
                ShipmentDO shipment = shipmentMap.get(shipmentId);
                BigDecimal shipmentTotal = (shipment.getTotalAmount() != null ? shipment.getTotalAmount() : BigDecimal.ZERO)
                        .add(shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO);

                BigDecimal occupiedAmount = calculateOccupiedInvoiceAmount(shipmentId, app.getId());
                if (occupiedAmount.add(applyAmount).compareTo(shipmentTotal) > 0) {
                    throw exception(INVOICE_EXCEED_SHIPMENT_AMOUNT);
                }
            }

            // 重算税额
            BigDecimal totalAmountInclTax = updateReqVO.getItems().stream()
                    .map(InvoiceApplicationItemSaveReqVO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxRate = updateReqVO.getTaxRate() != null ? updateReqVO.getTaxRate() : app.getTaxRate();
            InvoiceTaxCalculator.TaxCalculationResult taxCalc = InvoiceTaxCalculator.calculate(totalAmountInclTax, taxRate);

            app.setTotalAmountInclTax(taxCalc.getTotalAmountInclTax());
            app.setTotalAmountExclTax(taxCalc.getTotalAmountExclTax());
            app.setTotalTax(taxCalc.getTotalTax());
            app.setTaxRate(taxCalc.getTaxRate());

            // 替换行表
            invoiceApplicationItemMapper.delete(new LambdaQueryWrapperX<InvoiceApplicationItemDO>()
                    .eq(InvoiceApplicationItemDO::getApplicationId, app.getId()));

            int lineNo = 1;
            for (InvoiceApplicationItemSaveReqVO itemReq : updateReqVO.getItems()) {
                ShipmentDO shipment = shipmentMap.get(itemReq.getShipmentId());
                BigDecimal shipmentTotal = (shipment.getTotalAmount() != null ? shipment.getTotalAmount() : BigDecimal.ZERO)
                        .add(shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO);

                InvoiceApplicationItemDO itemDO = new InvoiceApplicationItemDO();
                itemDO.setApplicationId(app.getId());
                itemDO.setLineNo(lineNo++);
                itemDO.setShipmentId(shipment.getId());
                itemDO.setShipmentNo(shipment.getShipmentNo());
                itemDO.setShipmentAmount(shipmentTotal);
                itemDO.setAmount(itemReq.getAmount().setScale(2, RoundingMode.HALF_UP));
                itemDO.setRemark(itemReq.getRemark());
                invoiceApplicationItemMapper.insert(itemDO);
            }
        }

        if (updateReqVO.getInvoiceKind() != null) {
            app.setInvoiceKind(updateReqVO.getInvoiceKind());
        }
        if (updateReqVO.getElectronic() != null) {
            app.setElectronic(updateReqVO.getElectronic());
        }
        if (updateReqVO.getInvoiceTitle() != null) {
            app.setInvoiceTitle(updateReqVO.getInvoiceTitle());
        }
        if (updateReqVO.getTaxNo() != null) {
            app.setTaxNo(updateReqVO.getTaxNo());
        }
        if (updateReqVO.getAddress() != null) {
            app.setAddress(updateReqVO.getAddress());
        }
        if (updateReqVO.getBankInfo() != null) {
            app.setBankInfo(updateReqVO.getBankInfo());
        }
        if (updateReqVO.getRemark() != null) {
            app.setRemark(updateReqVO.getRemark());
        }

        invoiceApplicationMapper.updateById(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInvoiceApplication(Long id) {
        InvoiceApplicationDO app = validateInvoiceApplicationExists(id);
        if (!InvoiceApplicationStatus.DRAFT.getValue().equals(app.getStatus())) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }
        invoiceApplicationItemMapper.delete(new LambdaQueryWrapperX<InvoiceApplicationItemDO>()
                .eq(InvoiceApplicationItemDO::getApplicationId, id));
        invoiceApplicationMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInvoiceApplication(Long id) {
        InvoiceApplicationDO app = validateInvoiceApplicationExists(id);
        if (!InvoiceApplicationStatus.DRAFT.getValue().equals(app.getStatus())
                && !InvoiceApplicationStatus.REJECTED.getValue().equals(app.getStatus())) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        // 提交前重新守卫超开（防并发超开）
        List<InvoiceApplicationItemDO> items = invoiceApplicationItemMapper.selectList(
                new LambdaQueryWrapperX<InvoiceApplicationItemDO>()
                        .eq(InvoiceApplicationItemDO::getApplicationId, id));
        if (items.isEmpty()) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        List<Long> shipmentIds = items.stream().map(InvoiceApplicationItemDO::getShipmentId).distinct().collect(Collectors.toList());
        List<ShipmentDO> shipments = shipmentMapper.selectBatchIds(shipmentIds);
        Map<Long, ShipmentDO> shipmentMap = shipments.stream().collect(Collectors.toMap(ShipmentDO::getId, s -> s));

        Map<Long, BigDecimal> currentReqShipmentAmounts = new HashMap<>();
        for (InvoiceApplicationItemDO item : items) {
            currentReqShipmentAmounts.merge(item.getShipmentId(), item.getAmount(), BigDecimal::add);
        }

        for (Map.Entry<Long, BigDecimal> entry : currentReqShipmentAmounts.entrySet()) {
            Long shipmentId = entry.getKey();
            BigDecimal applyAmount = entry.getValue();
            ShipmentDO shipment = shipmentMap.get(shipmentId);
            if (shipment == null) {
                throw exception(INVOICE_EXCEED_SHIPMENT_AMOUNT);
            }
            BigDecimal shipmentTotal = (shipment.getTotalAmount() != null ? shipment.getTotalAmount() : BigDecimal.ZERO)
                    .add(shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO);

            BigDecimal occupiedAmount = calculateOccupiedInvoiceAmount(shipmentId, app.getId());
            if (occupiedAmount.add(applyAmount).compareTo(shipmentTotal) > 0) {
                log.warn("[submitInvoiceApplication] 提交并发超开拦截: shipmentId={}, shipmentTotal={}, occupied={}, apply={}",
                        shipmentId, shipmentTotal, occupiedAmount, applyAmount);
                throw exception(INVOICE_EXCEED_SHIPMENT_AMOUNT);
            }
        }

        String username = SecurityFrameworkUtils.getLoginUserNickname();
        if (StrUtil.isBlank(username)) {
            username = "admin";
        }
        app.setStatus(InvoiceApplicationStatus.SUBMITTED.getValue());
        app.setSubmitBy(username);
        app.setSubmitTime(LocalDateTime.now());

        invoiceApplicationMapper.updateById(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_INVOICE", subType = "ISSUE", bizNo = "{{#reqVO.id}}", success = "发票申请【{{#reqVO.id}}】登记开票成功，发票号【{{#reqVO.officialInvoiceNo}}】")
    public void issueInvoiceApplication(InvoiceApplicationIssueReqVO reqVO) {
        InvoiceApplicationDO app = validateInvoiceApplicationExists(reqVO.getId());
        if (!InvoiceApplicationStatus.SUBMITTED.getValue().equals(app.getStatus())) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        String officialNo = reqVO.getOfficialInvoiceNo() != null ? reqVO.getOfficialInvoiceNo().trim() : "";
        if (officialNo.length() != 20) {
            throw exception(INVOICE_OFFICIAL_NO_REQUIRED);
        }

        // 唯一性校验（含软删除记录）
        InvoiceApplicationDO duplicate = invoiceApplicationMapper.selectByOfficialInvoiceNoIncludingDeleted(officialNo);
        if (duplicate != null && !duplicate.getId().equals(app.getId())) {
            throw exception(INVOICE_OFFICIAL_NO_DUPLICATE);
        }

        String username = SecurityFrameworkUtils.getLoginUserNickname();
        if (StrUtil.isBlank(username)) {
            username = "admin";
        }

        app.setStatus(InvoiceApplicationStatus.ISSUED.getValue());
        app.setOfficialInvoiceNo(officialNo);
        app.setInvoiceDate(reqVO.getInvoiceDate());
        app.setInvoiceCode(reqVO.getInvoiceCode());
        app.setAttachmentUrl(reqVO.getAttachmentUrl());
        app.setIssueBy(username);
        app.setIssueTime(LocalDateTime.now());

        invoiceApplicationMapper.updateById(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "FINANCE_INVOICE", subType = "REJECT", bizNo = "{{#reqVO.id}}", success = "发票申请【{{#reqVO.id}}】驳回成功，原因【{{#reqVO.rejectReason}}】")
    public void rejectInvoiceApplication(InvoiceApplicationRejectReqVO reqVO) {
        InvoiceApplicationDO app = validateInvoiceApplicationExists(reqVO.getId());
        if (!InvoiceApplicationStatus.SUBMITTED.getValue().equals(app.getStatus())) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        app.setStatus(InvoiceApplicationStatus.REJECTED.getValue());
        if (StrUtil.isNotBlank(reqVO.getRejectReason())) {
            String remark = StrUtil.isNotBlank(app.getRemark())
                    ? app.getRemark() + "；[驳回原因]: " + reqVO.getRejectReason().trim()
                    : "[驳回原因]: " + reqVO.getRejectReason().trim();
            app.setRemark(remark);
        }

        invoiceApplicationMapper.updateById(app);
    }

    @Override
    public List<InvoiceCandidateShipmentRespVO> getCandidateShipments(Long customerId, String currency) {
        if (customerId == null || StrUtil.isBlank(currency)) {
            return Collections.emptyList();
        }

        List<ShipmentDO> shipments = shipmentMapper.selectList(new LambdaQueryWrapperX<ShipmentDO>()
                .eq(ShipmentDO::getCustomerId, customerId)
                .eq(ShipmentDO::getCurrency, currency)
                .eq(ShipmentDO::getStatus, "COMPLETED")
                .orderByDesc(ShipmentDO::getId));

        if (CollUtil.isEmpty(shipments)) {
            return Collections.emptyList();
        }

        List<InvoiceCandidateShipmentRespVO> result = new ArrayList<>();
        for (ShipmentDO shipment : shipments) {
            BigDecimal shipmentTotal = (shipment.getTotalAmount() != null ? shipment.getTotalAmount() : BigDecimal.ZERO)
                    .add(shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO);

            BigDecimal invoicedAmount = calculateOccupiedInvoiceAmount(shipment.getId(), null);
            BigDecimal availableAmount = shipmentTotal.subtract(invoicedAmount);

            if (availableAmount.compareTo(BigDecimal.ZERO) > 0) {
                result.add(InvoiceCandidateShipmentRespVO.builder()
                        .shipmentId(shipment.getId())
                        .shipmentNo(shipment.getShipmentNo())
                        .customerId(shipment.getCustomerId())
                        .customerName(shipment.getCustomerName())
                        .currency(shipment.getCurrency())
                        .shippingDate(shipment.getShippingDate())
                        .salesContractId(shipment.getSalesContractId())
                        .contractNo(shipment.getContractNo())
                        .totalAmount(shipment.getTotalAmount() != null ? shipment.getTotalAmount() : BigDecimal.ZERO)
                        .freightFee(shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO)
                        .shipmentAmount(shipmentTotal)
                        .invoicedAmount(invoicedAmount)
                        .availableAmount(availableAmount)
                        .build());
            }
        }

        return result;
    }

    /**
     * 计算指定发货单在其他申请中的已占用开票额（仅统计 SUBMITTED 与 ISSUED 状态）
     *
     * @param shipmentId 发货单ID
     * @param excludeAppId 排除的申请单ID（可为 null）
     * @return 累计占用金额
     */
    private BigDecimal calculateOccupiedInvoiceAmount(Long shipmentId, Long excludeAppId) {
        List<InvoiceApplicationItemDO> items = invoiceApplicationItemMapper.selectList(
                new LambdaQueryWrapperX<InvoiceApplicationItemDO>()
                        .eq(InvoiceApplicationItemDO::getShipmentId, shipmentId));
        if (CollUtil.isEmpty(items)) {
            return BigDecimal.ZERO;
        }

        Set<Long> appIds = items.stream()
                .map(InvoiceApplicationItemDO::getApplicationId)
                .filter(id -> excludeAppId == null || !Objects.equals(id, excludeAppId))
                .collect(Collectors.toSet());
        if (appIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<Long, InvoiceApplicationDO> appMap = batchGetApplications(appIds);
        BigDecimal occupied = BigDecimal.ZERO;
        for (InvoiceApplicationItemDO item : items) {
            if (excludeAppId != null && Objects.equals(item.getApplicationId(), excludeAppId)) {
                continue;
            }
            InvoiceApplicationDO parentApp = appMap.get(item.getApplicationId());
            if (parentApp != null && OCCUPY_STATUSES.contains(parentApp.getStatus())) {
                occupied = occupied.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
            }
        }
        return occupied;
    }

    /**
     * 遵循 coding-standards.md §2 规范的批量快照辅助方法 (以 batch* 前缀命名)
     */
    private Map<Long, InvoiceApplicationDO> batchGetApplications(Collection<Long> appIds) {
        if (CollUtil.isEmpty(appIds)) {
            return Collections.emptyMap();
        }
        List<InvoiceApplicationDO> list = invoiceApplicationMapper.selectBatchIds(appIds);
        return list.stream().collect(Collectors.toMap(InvoiceApplicationDO::getId, a -> a));
    }

    private InvoiceApplicationDO validateInvoiceApplicationExists(Long id) {
        if (id == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        InvoiceApplicationDO app = invoiceApplicationMapper.selectById(id);
        if (app == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        return app;
    }

    private String buildCustomerBankInfo(CustomerDO customer) {
        String bank = StrUtil.isNotBlank(customer.getBankName()) ? customer.getBankName().trim() : "";
        String acc = StrUtil.isNotBlank(customer.getBankAccountNo()) ? customer.getBankAccountNo().trim() : "";
        if (StrUtil.isNotBlank(bank) && StrUtil.isNotBlank(acc)) {
            return bank + " " + acc;
        }
        return StrUtil.isNotBlank(bank) ? bank : acc;
    }

    private String generateInvoiceNo(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String prefix = INVOICE_NO_PREFIX + YEAR_FORMATTER.format(targetDate);
        List<InvoiceApplicationDO> candidates = invoiceApplicationMapper.selectListByInvoiceNoPrefixIncludingDeleted(prefix);

        long maxNum = 0;
        int maxDigits = NO_MIN_DIGITS;
        if (CollUtil.isNotEmpty(candidates)) {
            for (InvoiceApplicationDO candidate : candidates) {
                String no = candidate.getInvoiceNo();
                if (no == null || no.length() <= prefix.length()) {
                    continue;
                }
                String suffix = no.substring(prefix.length());
                Matcher matcher = NO_SUFFIX_NUMBER_PATTERN.matcher(suffix);
                if (!matcher.find()) {
                    continue;
                }
                String digits = matcher.group();
                try {
                    long num = Long.parseLong(digits);
                    if (num > maxNum) {
                        maxNum = num;
                        maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            long nextNum = maxNum + 1 + i;
            String candidateNo = String.format("%s%0" + maxDigits + "d", prefix, nextNum);
            InvoiceApplicationDO existing = invoiceApplicationMapper.selectByInvoiceNoIncludingDeleted(candidateNo);
            if (existing == null) {
                return candidateNo;
            }
        }
        log.error("[generateInvoiceNo] 发票申请单号生成重试耗尽: prefix={}, maxNum={}", prefix, maxNum);
        throw exception(STATEMENT_NO_EXHAUSTED);
    }

    private InvoiceApplicationRespVO convertDOToRespVO(InvoiceApplicationDO app) {
        InvoiceApplicationRespVO resp = new InvoiceApplicationRespVO();
        copyProperties(app, resp);
        return resp;
    }

    private InvoiceApplicationItemRespVO convertItemDOToRespVO(InvoiceApplicationItemDO item) {
        return InvoiceApplicationItemRespVO.builder()
                .id(item.getId())
                .applicationId(item.getApplicationId())
                .lineNo(item.getLineNo())
                .shipmentId(item.getShipmentId())
                .shipmentNo(item.getShipmentNo())
                .shipmentAmount(item.getShipmentAmount())
                .amount(item.getAmount())
                .remark(item.getRemark())
                .build();
    }

    private void copyProperties(InvoiceApplicationDO app, InvoiceApplicationRespVO resp) {
        resp.setId(app.getId());
        resp.setInvoiceNo(app.getInvoiceNo());
        resp.setCustomerId(app.getCustomerId());
        resp.setCustomerName(app.getCustomerName());
        resp.setCurrency(app.getCurrency());
        resp.setTaxRate(app.getTaxRate());
        resp.setInvoiceKind(app.getInvoiceKind());
        resp.setElectronic(app.getElectronic());
        resp.setInvoiceTitle(app.getInvoiceTitle());
        resp.setTaxNo(app.getTaxNo());
        resp.setAddress(app.getAddress());
        resp.setBankInfo(app.getBankInfo());
        resp.setStatus(app.getStatus());
        resp.setOfficialInvoiceNo(app.getOfficialInvoiceNo());
        resp.setInvoiceCode(app.getInvoiceCode());
        resp.setInvoiceDate(app.getInvoiceDate());
        resp.setAttachmentUrl(app.getAttachmentUrl());
        resp.setTotalAmountExclTax(app.getTotalAmountExclTax());
        resp.setTotalTax(app.getTotalTax());
        resp.setTotalAmountInclTax(app.getTotalAmountInclTax());
        resp.setIssueBy(app.getIssueBy());
        resp.setIssueTime(app.getIssueTime());
        resp.setSubmitBy(app.getSubmitBy());
        resp.setSubmitTime(app.getSubmitTime());
        resp.setRemark(app.getRemark());
        resp.setCreateTime(app.getCreateTime());
    }
}
