package com.luna.lunasrv.module.lunaerp.service.finance.invoice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
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
import com.luna.lunasrv.module.lunaerp.enums.finance.InvoiceApplicationStatusEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.InvoiceKindEnum;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

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
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SHIPMENT_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 财务管理 - 发票申请 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private static final String INVOICE_NO_PREFIX = "IA";
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final int NO_MIN_DIGITS = 4;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");
    private static final int MAX_GENERATE_RETRY = 5;

    private static final String DEFAULT_TAX_RATE_DOMESTIC = "13";
    private static final String DEFAULT_TAX_RATE_EXPORT = "0";
    private static final String SHIPMENT_STATUS_COMPLETED = "COMPLETED";

    @Resource
    private InvoiceApplicationMapper invoiceApplicationMapper;

    @Resource
    private InvoiceApplicationItemMapper invoiceApplicationItemMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private ShipmentMapper shipmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInvoice(InvoiceSaveReqVO reqVO) {
        // 1. 客户校验
        CustomerDO customer = validateCustomer(reqVO.getCustomerId());

        // 2. 发货单及明细校验（同客户、同币种、超开守卫）
        ValidationContext ctx = validateItemsAndShipments(reqVO.getCustomerId(), reqVO.getCurrency(), reqVO.getItems(), null);

        // 3. 税率处理与反算
        String taxRate = resolveTaxRate(reqVO.getTaxRate(), ctx.currency);
        TaxResult taxResult = calculateTax(ctx.totalInclTax, taxRate);

        // 4. 客户开票四要素快照（优先前端入参，其次客户档案带出）
        String invoiceTitle = StringUtils.hasText(reqVO.getInvoiceTitle())
                ? reqVO.getInvoiceTitle().trim()
                : (StringUtils.hasText(customer.getInvoiceTitle()) ? customer.getInvoiceTitle() : customer.getCompanyName());
        String taxNo = StringUtils.hasText(reqVO.getTaxNo())
                ? reqVO.getTaxNo().trim()
                : customer.getUnifiedSocialCreditCode();
        String address = StringUtils.hasText(reqVO.getAddress())
                ? reqVO.getAddress().trim()
                : customer.getInvoiceAddress();
        String bankInfo = StringUtils.hasText(reqVO.getBankInfo())
                ? reqVO.getBankInfo().trim()
                : formatBankInfo(customer.getBankName(), customer.getBankAccountNo());

        // 5. 生成或校验单号（IA{yyyy}{4位流水}）
        String invoiceNo;
        if (StringUtils.hasText(reqVO.getInvoiceNo())) {
            invoiceNo = reqVO.getInvoiceNo().trim();
            InvoiceApplicationDO existing = invoiceApplicationMapper.selectByInvoiceNoIncludingDeleted(invoiceNo);
            if (existing != null) {
                throw exception(INVOICE_STATUS_NOT_ALLOW, "发票申请单号【" + invoiceNo + "】已存在");
            }
        } else {
            invoiceNo = generateInvoiceNoWithRetry();
        }

        // 6. 保存主表（DRAFT）
        InvoiceApplicationDO app = InvoiceApplicationDO.builder()
                .invoiceNo(invoiceNo)
                .customerId(customer.getId())
                .customerName(customer.getCompanyName())
                .currency(ctx.currency)
                .taxRate(taxRate)
                .invoiceKind(reqVO.getInvoiceKind())
                .electronic(reqVO.getElectronic() != null ? reqVO.getElectronic() : 1)
                .invoiceTitle(invoiceTitle)
                .taxNo(taxNo)
                .address(address)
                .bankInfo(bankInfo)
                .status(InvoiceApplicationStatusEnum.DRAFT.getValue())
                .totalAmountInclTax(taxResult.inclTax)
                .totalAmountExclTax(taxResult.exclTax)
                .totalTax(taxResult.tax)
                .remark(reqVO.getRemark())
                .build();
        invoiceApplicationMapper.insert(app);

        // 7. 保存明细行
        saveApplicationItems(app.getId(), reqVO.getItems(), ctx.shipmentMap);

        return app.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInvoice(InvoiceSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        InvoiceApplicationDO app = invoiceApplicationMapper.selectById(reqVO.getId());
        if (app == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        // 状态守卫：仅 DRAFT 或 REJECTED 允许修改
        if (!InvoiceApplicationStatusEnum.DRAFT.getValue().equals(app.getStatus())
                && !InvoiceApplicationStatusEnum.REJECTED.getValue().equals(app.getStatus())) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        // 客户校验
        CustomerDO customer = validateCustomer(reqVO.getCustomerId());

        // 发货单及明细校验（排除当前单已占）
        ValidationContext ctx = validateItemsAndShipments(reqVO.getCustomerId(), reqVO.getCurrency(), reqVO.getItems(), app.getId());

        // 税率处理与反算
        String taxRate = resolveTaxRate(reqVO.getTaxRate(), ctx.currency);
        TaxResult taxResult = calculateTax(ctx.totalInclTax, taxRate);

        // 四要素
        String invoiceTitle = StringUtils.hasText(reqVO.getInvoiceTitle())
                ? reqVO.getInvoiceTitle().trim()
                : (StringUtils.hasText(customer.getInvoiceTitle()) ? customer.getInvoiceTitle() : customer.getCompanyName());
        String taxNo = StringUtils.hasText(reqVO.getTaxNo())
                ? reqVO.getTaxNo().trim()
                : customer.getUnifiedSocialCreditCode();
        String address = StringUtils.hasText(reqVO.getAddress())
                ? reqVO.getAddress().trim()
                : customer.getInvoiceAddress();
        String bankInfo = StringUtils.hasText(reqVO.getBankInfo())
                ? reqVO.getBankInfo().trim()
                : formatBankInfo(customer.getBankName(), customer.getBankAccountNo());

        // 更新主表
        app.setCustomerId(customer.getId());
        app.setCustomerName(customer.getCompanyName());
        app.setCurrency(ctx.currency);
        app.setTaxRate(taxRate);
        app.setInvoiceKind(reqVO.getInvoiceKind());
        app.setElectronic(reqVO.getElectronic() != null ? reqVO.getElectronic() : 1);
        app.setInvoiceTitle(invoiceTitle);
        app.setTaxNo(taxNo);
        app.setAddress(address);
        app.setBankInfo(bankInfo);
        app.setTotalAmountInclTax(taxResult.inclTax);
        app.setTotalAmountExclTax(taxResult.exclTax);
        app.setTotalTax(taxResult.tax);
        app.setRemark(reqVO.getRemark());
        invoiceApplicationMapper.updateById(app);

        // 删除旧明细，重新插入新明细
        invoiceApplicationItemMapper.deleteByApplicationId(app.getId());
        saveApplicationItems(app.getId(), reqVO.getItems(), ctx.shipmentMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInvoice(Long id) {
        if (id == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        InvoiceApplicationDO app = invoiceApplicationMapper.selectById(id);
        if (app == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        // 状态守卫：仅 DRAFT 允许删除
        if (!InvoiceApplicationStatusEnum.DRAFT.getValue().equals(app.getStatus())) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        invoiceApplicationMapper.deleteById(id);
        invoiceApplicationItemMapper.deleteByApplicationId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInvoice(Long id) {
        if (id == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        InvoiceApplicationDO app = invoiceApplicationMapper.selectById(id);
        if (app == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        // 状态守卫：DRAFT 或 REJECTED 可提交
        if (!InvoiceApplicationStatusEnum.DRAFT.getValue().equals(app.getStatus())
                && !InvoiceApplicationStatusEnum.REJECTED.getValue().equals(app.getStatus())) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        List<InvoiceApplicationItemDO> items = invoiceApplicationItemMapper.selectListByApplicationId(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        // 聚合各发货单本次申请开票金额
        Map<Long, BigDecimal> appShipmentAmounts = new HashMap<>();
        for (InvoiceApplicationItemDO item : items) {
            appShipmentAmounts.merge(item.getShipmentId(), item.getAmount(), BigDecimal::add);
        }

        // 悲观锁锁定各发货单，防并发超开
        for (Map.Entry<Long, BigDecimal> entry : appShipmentAmounts.entrySet()) {
            Long shipmentId = entry.getKey();
            BigDecimal thisShipmentAppAmount = entry.getValue();

            ShipmentDO shipment = shipmentMapper.selectByIdForUpdate(shipmentId);
            if (shipment == null) {
                throw exception(SHIPMENT_NOT_EXISTS);
            }
            if (!SHIPMENT_STATUS_COMPLETED.equals(shipment.getStatus())) {
                throw exception(INVOICE_STATUS_NOT_ALLOW);
            }

            BigDecimal shipmentTotal = shipment.getTotalAmount().add(shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO);
            BigDecimal alreadyInvoiced = invoiceApplicationItemMapper.selectInvoicedAmountByShipmentId(shipmentId, id);

            long maxCents = toCents(shipmentTotal);
            long alreadyCents = toCents(alreadyInvoiced);
            long thisCents = toCents(thisShipmentAppAmount);

            if (alreadyCents + thisCents > maxCents) {
                throw exception(INVOICE_EXCEED_SHIPMENT_AMOUNT);
            }
        }

        app.setStatus(InvoiceApplicationStatusEnum.SUBMITTED.getValue());
        app.setSubmitBy(getLoginUserName());
        app.setSubmitTime(LocalDateTime.now());
        app.setRejectReason(null);
        invoiceApplicationMapper.updateById(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "财务", subType = "发票申请", bizNo = "{{#issueReqVO.id}}", success = "登记发票成功，发票号：{{#issueReqVO.officialInvoiceNo}}")
    public void issueInvoice(InvoiceIssueReqVO issueReqVO) {
        if (issueReqVO == null || issueReqVO.getId() == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        InvoiceApplicationDO app = invoiceApplicationMapper.selectById(issueReqVO.getId());
        if (app == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        // 状态守卫：仅 SUBMITTED 允许开票登记
        if (!InvoiceApplicationStatusEnum.SUBMITTED.getValue().equals(app.getStatus())) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        String officialNo = issueReqVO.getOfficialInvoiceNo();
        if (!StringUtils.hasText(officialNo)) {
            throw exception(INVOICE_OFFICIAL_NO_REQUIRED);
        }
        officialNo = officialNo.trim();
        if (!officialNo.matches("^\\d{20}$")) {
            throw exception(INVOICE_OFFICIAL_NO_REQUIRED, "发票号码必须为20位数字");
        }

        // 唯一性校验（含软删行）
        InvoiceApplicationDO duplicate = invoiceApplicationMapper.selectByOfficialInvoiceNoIncludingDeleted(officialNo);
        if (duplicate != null && !duplicate.getId().equals(app.getId())) {
            throw exception(INVOICE_OFFICIAL_NO_DUPLICATE);
        }

        app.setStatus(InvoiceApplicationStatusEnum.ISSUED.getValue());
        app.setOfficialInvoiceNo(officialNo);
        app.setInvoiceCode(issueReqVO.getInvoiceCode());
        app.setInvoiceDate(issueReqVO.getInvoiceDate());
        app.setAttachmentUrl(issueReqVO.getAttachmentUrl());
        app.setIssueBy(getLoginUserName());
        app.setIssueTime(LocalDateTime.now());
        invoiceApplicationMapper.updateById(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectInvoice(Long id, String reason) {
        if (id == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        InvoiceApplicationDO app = invoiceApplicationMapper.selectById(id);
        if (app == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        // 状态守卫：仅 SUBMITTED 允许驳回
        if (!InvoiceApplicationStatusEnum.SUBMITTED.getValue().equals(app.getStatus())) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        app.setStatus(InvoiceApplicationStatusEnum.REJECTED.getValue());
        app.setRejectReason(StringUtils.hasText(reason) ? reason.trim() : null);
        invoiceApplicationMapper.updateById(app);
        // 状态转为 REJECTED 后，selectInvoicedAmountByShipmentId 自然不包含该单，开票占用自动释放
    }

    @Override
    public InvoiceRespVO getInvoice(Long id) {
        if (id == null) {
            return null;
        }
        InvoiceApplicationDO app = invoiceApplicationMapper.selectById(id);
        if (app == null) {
            return null;
        }
        List<InvoiceApplicationItemDO> items = invoiceApplicationItemMapper.selectListByApplicationId(id);
        return convertToRespVO(app, items);
    }

    @Override
    public PageResult<InvoiceRespVO> getInvoicePage(InvoicePageReqVO pageReqVO) {
        PageResult<InvoiceApplicationDO> pageResult = invoiceApplicationMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        List<InvoiceRespVO> respList = pageResult.getList().stream()
                .map(app -> convertToRespVO(app, null))
                .toList();
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public List<InvoiceCandidateShipmentRespVO> getCandidateShipments(Long customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }
        List<ShipmentDO> shipments = shipmentMapper.selectList(new LambdaQueryWrapperX<ShipmentDO>()
                .eq(ShipmentDO::getCustomerId, customerId)
                .eq(ShipmentDO::getStatus, SHIPMENT_STATUS_COMPLETED)
                .orderByDesc(ShipmentDO::getShippingDate)
                .orderByDesc(ShipmentDO::getId));

        if (CollUtil.isEmpty(shipments)) {
            return Collections.emptyList();
        }

        List<Long> shipmentIds = shipments.stream().map(ShipmentDO::getId).filter(Objects::nonNull).toList();
        List<Map<String, Object>> invoicedList = invoiceApplicationItemMapper.selectInvoicedAmountsByShipmentIds(shipmentIds);
        Map<Long, BigDecimal> invoicedMap = new HashMap<>();
        if (CollUtil.isNotEmpty(invoicedList)) {
            for (Map<String, Object> map : invoicedList) {
                Object sId = map.get("shipmentId");
                Object amt = map.get("invoicedAmount");
                if (sId != null && amt != null) {
                    Long shipId = Long.valueOf(sId.toString());
                    BigDecimal amount = new BigDecimal(amt.toString());
                    invoicedMap.put(shipId, amount);
                }
            }
        }

        List<InvoiceCandidateShipmentRespVO> result = new ArrayList<>();
        for (ShipmentDO shipment : shipments) {
            BigDecimal freight = shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO;
            BigDecimal totalGoods = shipment.getTotalAmount() != null ? shipment.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal shipmentAmount = totalGoods.add(freight);
            BigDecimal invoicedAmount = invoicedMap.getOrDefault(shipment.getId(), BigDecimal.ZERO);
            BigDecimal uninvoicedBalance = shipmentAmount.subtract(invoicedAmount);
            if (uninvoicedBalance.compareTo(BigDecimal.ZERO) < 0) {
                uninvoicedBalance = BigDecimal.ZERO;
            }

            result.add(InvoiceCandidateShipmentRespVO.builder()
                    .id(shipment.getId())
                    .shipmentNo(shipment.getShipmentNo())
                    .customerId(shipment.getCustomerId())
                    .currency(shipment.getCurrency())
                    .shippingDate(shipment.getShippingDate())
                    .totalAmount(totalGoods)
                    .freightFee(freight)
                    .shipmentAmount(shipmentAmount)
                    .invoicedAmount(invoicedAmount)
                    .uninvoicedBalance(uninvoicedBalance)
                    .build());
        }
        return result;
    }

    // ==================== 私有辅助方法 ====================

    private CustomerDO validateCustomer(Long customerId) {
        if (customerId == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        CustomerDO customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        return customer;
    }

    private static class ValidationContext {
        String currency;
        BigDecimal totalInclTax;
        Map<Long, ShipmentDO> shipmentMap;
    }

    private ValidationContext validateItemsAndShipments(Long customerId, String reqCurrency,
                                                        List<InvoiceItemSaveVO> items, Long excludeAppId) {
        if (CollUtil.isEmpty(items)) {
            throw exception(INVOICE_STATUS_NOT_ALLOW);
        }

        Map<Long, BigDecimal> shipmentAppAmounts = new HashMap<>();
        for (InvoiceItemSaveVO item : items) {
            if (item.getShipmentId() == null || item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(INVOICE_STATUS_NOT_ALLOW);
            }
            shipmentAppAmounts.merge(item.getShipmentId(), item.getAmount(), BigDecimal::add);
        }

        Map<Long, ShipmentDO> shipmentMap = new HashMap<>();
        String commonCurrency = null;
        BigDecimal totalInclTax = BigDecimal.ZERO;

        for (Map.Entry<Long, BigDecimal> entry : shipmentAppAmounts.entrySet()) {
            Long shipmentId = entry.getKey();
            BigDecimal thisShipmentAppAmount = entry.getValue();

            ShipmentDO shipment = shipmentMapper.selectById(shipmentId);
            if (shipment == null) {
                throw exception(SHIPMENT_NOT_EXISTS);
            }
            if (!SHIPMENT_STATUS_COMPLETED.equals(shipment.getStatus())) {
                throw exception(INVOICE_STATUS_NOT_ALLOW);
            }
            // 校验客户一致性
            if (!customerId.equals(shipment.getCustomerId())) {
                throw exception(INVOICE_CUSTOMER_MISMATCH);
            }
            // 校验币种一致性（发货单之间、以及与请求币种）
            if (commonCurrency == null) {
                commonCurrency = shipment.getCurrency();
            } else if (!commonCurrency.equals(shipment.getCurrency())) {
                throw exception(INVOICE_CURRENCY_MIXED);
            }
            if (StringUtils.hasText(reqCurrency) && !reqCurrency.equals(shipment.getCurrency())) {
                throw exception(INVOICE_CURRENCY_MIXED);
            }

            // 超开守卫校验
            BigDecimal shipmentTotal = shipment.getTotalAmount().add(shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO);
            BigDecimal alreadyInvoiced = invoiceApplicationItemMapper.selectInvoicedAmountByShipmentId(shipmentId, excludeAppId);

            long maxCents = toCents(shipmentTotal);
            long alreadyCents = toCents(alreadyInvoiced);
            long thisCents = toCents(thisShipmentAppAmount);

            if (alreadyCents + thisCents > maxCents) {
                throw exception(INVOICE_EXCEED_SHIPMENT_AMOUNT);
            }

            shipmentMap.put(shipmentId, shipment);
            totalInclTax = totalInclTax.add(thisShipmentAppAmount);
        }

        ValidationContext ctx = new ValidationContext();
        ctx.currency = commonCurrency != null ? commonCurrency : (StringUtils.hasText(reqCurrency) ? reqCurrency : "CNY");
        ctx.totalInclTax = totalInclTax.setScale(2, RoundingMode.HALF_UP);
        ctx.shipmentMap = shipmentMap;
        return ctx;
    }

    private void saveApplicationItems(Long applicationId, List<InvoiceItemSaveVO> itemVOs, Map<Long, ShipmentDO> shipmentMap) {
        int lineNo = 1;
        for (InvoiceItemSaveVO itemVO : itemVOs) {
            ShipmentDO shipment = shipmentMap.get(itemVO.getShipmentId());
            BigDecimal freight = shipment != null && shipment.getFreightFee() != null ? shipment.getFreightFee() : BigDecimal.ZERO;
            BigDecimal totalGoods = shipment != null && shipment.getTotalAmount() != null ? shipment.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal shipmentAmount = totalGoods.add(freight);

            InvoiceApplicationItemDO itemDO = InvoiceApplicationItemDO.builder()
                    .applicationId(applicationId)
                    .lineNo(lineNo++)
                    .shipmentId(itemVO.getShipmentId())
                    .shipmentNo(shipment != null ? shipment.getShipmentNo() : "")
                    .shipmentAmount(shipmentAmount)
                    .amount(itemVO.getAmount().setScale(2, RoundingMode.HALF_UP))
                    .remark(itemVO.getRemark())
                    .build();
            invoiceApplicationItemMapper.insert(itemDO);
        }
    }

    /**
     * 服务端税额反算纯函数逻辑
     */
    public static class TaxResult {
        public BigDecimal inclTax;
        public BigDecimal exclTax;
        public BigDecimal tax;
    }

    public static TaxResult calculateTax(BigDecimal inclTax, String taxRate) {
        if (inclTax == null) {
            inclTax = BigDecimal.ZERO;
        }
        inclTax = inclTax.setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate;
        if ("13".equals(taxRate)) {
            rate = new BigDecimal("0.13");
        } else if ("9".equals(taxRate)) {
            rate = new BigDecimal("0.09");
        } else if ("6".equals(taxRate)) {
            rate = new BigDecimal("0.06");
        } else if ("3".equals(taxRate)) {
            rate = new BigDecimal("0.03");
        } else if ("1".equals(taxRate)) {
            rate = new BigDecimal("0.01");
        } else if ("0".equals(taxRate) || "EXEMPT".equalsIgnoreCase(taxRate)) {
            rate = BigDecimal.ZERO;
        } else {
            throw exception(INVOICE_TAX_RATE_INVALID);
        }

        BigDecimal exclTax;
        BigDecimal tax;
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            exclTax = inclTax;
            tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal divisor = BigDecimal.ONE.add(rate);
            exclTax = inclTax.divide(divisor, 2, RoundingMode.HALF_UP);
            tax = inclTax.subtract(exclTax).setScale(2, RoundingMode.HALF_UP);
        }

        TaxResult result = new TaxResult();
        result.inclTax = inclTax;
        result.exclTax = exclTax;
        result.tax = tax;
        return result;
    }

    private String resolveTaxRate(String taxRate, String currency) {
        if (StringUtils.hasText(taxRate)) {
            return taxRate.trim();
        }
        // 纺织默认 13%，外贸默认 0%
        if ("CNY".equalsIgnoreCase(currency) || "RMB".equalsIgnoreCase(currency)) {
            return DEFAULT_TAX_RATE_DOMESTIC;
        }
        return DEFAULT_TAX_RATE_EXPORT;
    }

    private String formatBankInfo(String bankName, String bankAccountNo) {
        if (!StringUtils.hasText(bankName) && !StringUtils.hasText(bankAccountNo)) {
            return null;
        }
        if (StringUtils.hasText(bankName) && StringUtils.hasText(bankAccountNo)) {
            return bankName.trim() + " " + bankAccountNo.trim();
        }
        return StringUtils.hasText(bankName) ? bankName.trim() : bankAccountNo.trim();
    }

    private long toCents(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private String generateInvoiceNo() {
        LocalDate today = LocalDate.now();
        String prefix = INVOICE_NO_PREFIX + today.format(YEAR_FORMATTER);
        List<InvoiceApplicationDO> candidates = invoiceApplicationMapper.selectListByInvoiceNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
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
                long num;
                try {
                    num = Long.parseLong(digits);
                } catch (NumberFormatException ignore) {
                    continue;
                }
                if (num > maxNum) {
                    maxNum = num;
                    maxDigits = Math.max(NO_MIN_DIGITS, digits.length());
                }
            }
        }
        if (maxNum == 0L) {
            return prefix + String.format("%0" + NO_MIN_DIGITS + "d", 1);
        }
        return prefix + String.format("%0" + maxDigits + "d", maxNum + 1);
    }

    private String generateInvoiceNoWithRetry() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateInvoiceNo();
            if (invoiceApplicationMapper.selectByInvoiceNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(INVOICE_STATUS_NOT_ALLOW);
    }

    private String getLoginUserName() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId != null) {
            return String.valueOf(userId);
        }
        return "admin";
    }

    private InvoiceRespVO convertToRespVO(InvoiceApplicationDO app, List<InvoiceApplicationItemDO> items) {
        List<InvoiceItemRespVO> itemRespVOs = null;
        if (items != null) {
            itemRespVOs = items.stream().map(item -> InvoiceItemRespVO.builder()
                    .id(item.getId())
                    .lineNo(item.getLineNo())
                    .shipmentId(item.getShipmentId())
                    .shipmentNo(item.getShipmentNo())
                    .shipmentAmount(item.getShipmentAmount())
                    .amount(item.getAmount())
                    .remark(item.getRemark())
                    .build()).collect(Collectors.toList());
        }

        return InvoiceRespVO.builder()
                .id(app.getId())
                .invoiceNo(app.getInvoiceNo())
                .customerId(app.getCustomerId())
                .customerName(app.getCustomerName())
                .currency(app.getCurrency())
                .taxRate(app.getTaxRate())
                .invoiceKind(app.getInvoiceKind())
                .electronic(app.getElectronic())
                .invoiceTitle(app.getInvoiceTitle())
                .taxNo(app.getTaxNo())
                .address(app.getAddress())
                .bankInfo(app.getBankInfo())
                .status(app.getStatus())
                .officialInvoiceNo(app.getOfficialInvoiceNo())
                .invoiceCode(app.getInvoiceCode())
                .invoiceDate(app.getInvoiceDate())
                .attachmentUrl(app.getAttachmentUrl())
                .totalAmountExclTax(app.getTotalAmountExclTax())
                .totalTax(app.getTotalTax())
                .totalAmountInclTax(app.getTotalAmountInclTax())
                .issueBy(app.getIssueBy())
                .issueTime(app.getIssueTime())
                .submitBy(app.getSubmitBy())
                .submitTime(app.getSubmitTime())
                .rejectReason(app.getRejectReason())
                .remark(app.getRemark())
                .createTime(app.getCreateTime())
                .items(itemRespVOs)
                .build();
    }

}
