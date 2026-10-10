package com.luna.lunasrv.module.lunaerp.service.business.quotation;

import com.luna.lunasrv.framework.common.enums.CommonStatusEnum;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.datapermission.core.util.DataPermissionUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.infra.api.config.ConfigApi;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.quotation.QuotationDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.quotation.QuotationItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.quotation.QuotationItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.quotation.QuotationMapper;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing.CostCardPricingCalculator;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing.QuotationItemPricingParam;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing.QuotationItemPricingResult;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing.QuotationSummaryPricingResult;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.*;

/**
 * 业务管理 - 报价单 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class QuotationServiceImpl implements QuotationService {

    private static final String QUOTATION_NO_PREFIX = "QT";
    private static final int NO_MIN_DIGITS = 4;
    private static final int MAX_GENERATE_RETRY = 3;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");

    private static final String DEFAULT_STATUS = "DRAFT";
    private static final String DEFAULT_CURRENCY = "RMB";
    private static final String DEFAULT_UNIT = "KG";

    @Resource
    private QuotationMapper quotationMapper;

    @Resource
    private QuotationItemMapper quotationItemMapper;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private ConfigApi configApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQuotation(QuotationSaveReqVO createReqVO) {
        // 1. 编号处理与并发撞号重试（ADR-0004）
        String quotationNo = createReqVO.getQuotationNo();
        boolean manualNo = StringUtils.hasText(quotationNo);

        QuotationDO quotationDO = BeanUtils.toBean(createReqVO, QuotationDO.class);
        quotationDO.setStatus(DEFAULT_STATUS);
        quotationDO.setVersionNo(1);

        // 2. 销售归属人与部门推导（ADR-0046）
        if (quotationDO.getSalespersonUserId() == null) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId != null) {
                quotationDO.setSalespersonUserId(loginUserId);
            }
        }
        if (quotationDO.getSalespersonUserId() != null) {
            AdminUserRespDTO user = adminUserApi.getUser(quotationDO.getSalespersonUserId());
            if (user != null) {
                quotationDO.setDeptId(user.getDeptId());
            }
        }

        // 3. 币种与汇率校验
        String currency = StringUtils.hasText(createReqVO.getCurrency())
                ? createReqVO.getCurrency().trim().toUpperCase()
                : DEFAULT_CURRENCY;
        quotationDO.setCurrency(currency);

        BigDecimal exchangeRate = createReqVO.getExchangeRate();
        if (DEFAULT_CURRENCY.equalsIgnoreCase(currency)) {
            exchangeRate = BigDecimal.ONE;
        } else {
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(QUOTATION_FX_RATE_REQUIRED);
            }
        }
        quotationDO.setExchangeRate(exchangeRate);

        // 4. 重算行明细与单头汇总
        boolean hasAmountPerm = hasAmountPermission();
        BigDecimal minMarginRate = getMinMarginRate();
        List<QuotationItemDO> itemDOList = calculateAndBuildItems(createReqVO.getItems(), exchangeRate, hasAmountPerm, minMarginRate, quotationDO);

        // 5. 保存主表与重试
        if (manualNo) {
            quotationNo = quotationNo.trim();
            // 手填编号：查重覆盖软删行（ADR-0004）
            QuotationDO existing = quotationMapper.selectByNoAndVersionIncludingDeleted(quotationNo, 1);
            if (existing != null) {
                throw exception(QUOTATION_NO_DUPLICATE, quotationNo);
            }
            QuotationDO activeDraft = quotationMapper.selectDraftByQuotationNo(quotationNo);
            if (activeDraft != null) {
                throw exception(QUOTATION_DRAFT_EXISTS);
            }
            quotationDO.setQuotationNo(quotationNo);
            quotationMapper.insert(quotationDO);
        } else {
            // 自动取号
            boolean saved = false;
            for (int retry = 0; retry < MAX_GENERATE_RETRY; retry++) {
                String candidateNo = generateQuotationNo();
                QuotationDO draft = quotationMapper.selectDraftByQuotationNo(candidateNo);
                if (draft != null) {
                    continue;
                }
                quotationDO.setQuotationNo(candidateNo);
                try {
                    quotationMapper.insert(quotationDO);
                    saved = true;
                    break;
                } catch (Exception ex) {
                    log.warn("[createQuotation] 自动取号撞唯一索引重试: no={}, retry={}", candidateNo, retry, ex);
                }
            }
            if (!saved) {
                throw exception(QUOTATION_NO_DUPLICATE, "自动取号失败，请尝试手填");
            }
        }

        // 6. 保存明细行
        for (QuotationItemDO itemDO : itemDOList) {
            itemDO.setQuotationId(quotationDO.getId());
            quotationItemMapper.insert(itemDO);
        }

        return quotationDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuotation(QuotationSaveReqVO updateReqVO) {
        // 1. 校验存在
        QuotationDO existing = quotationMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw exception(QUOTATION_NOT_EXISTS);
        }
        if (!DEFAULT_STATUS.equals(existing.getStatus())) {
            throw exception(QUOTATION_STATUS_NOT_ALLOWED);
        }

        // 2. 销售归属人与部门推导
        Long salespersonUserId = updateReqVO.getSalespersonUserId() != null
                ? updateReqVO.getSalespersonUserId()
                : existing.getSalespersonUserId();
        Long deptId = existing.getDeptId();
        if (salespersonUserId != null && !salespersonUserId.equals(existing.getSalespersonUserId())) {
            AdminUserRespDTO user = adminUserApi.getUser(salespersonUserId);
            if (user != null) {
                deptId = user.getDeptId();
            }
        }

        // 3. 币种与汇率校验
        String currency = StringUtils.hasText(updateReqVO.getCurrency())
                ? updateReqVO.getCurrency().trim().toUpperCase()
                : DEFAULT_CURRENCY;
        BigDecimal exchangeRate = updateReqVO.getExchangeRate();
        if (DEFAULT_CURRENCY.equalsIgnoreCase(currency)) {
            exchangeRate = BigDecimal.ONE;
        } else {
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(QUOTATION_FX_RATE_REQUIRED);
            }
        }

        // 4. 构建主表更新对象
        QuotationDO updateDO = BeanUtils.toBean(updateReqVO, QuotationDO.class);
        updateDO.setQuotationNo(existing.getQuotationNo());
        updateDO.setVersionNo(existing.getVersionNo());
        updateDO.setStatus(existing.getStatus());
        updateDO.setSalespersonUserId(salespersonUserId);
        updateDO.setDeptId(deptId);
        updateDO.setCurrency(currency);
        updateDO.setExchangeRate(exchangeRate);

        // 5. 重算行明细与单头汇总
        boolean hasAmountPerm = hasAmountPermission();
        BigDecimal minMarginRate = getMinMarginRate();
        List<QuotationItemDO> itemDOList = calculateAndBuildItems(updateReqVO.getItems(), exchangeRate, hasAmountPerm, minMarginRate, updateDO);

        quotationMapper.updateById(updateDO);

        // 6. 聚合替换明细（先删后插）
        quotationItemMapper.deleteByQuotationId(existing.getId());
        for (QuotationItemDO itemDO : itemDOList) {
            itemDO.setQuotationId(existing.getId());
            quotationItemMapper.insert(itemDO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuotation(Long id) {
        QuotationDO existing = quotationMapper.selectById(id);
        if (existing == null) {
            throw exception(QUOTATION_NOT_EXISTS);
        }
        if (!DEFAULT_STATUS.equals(existing.getStatus())) {
            throw exception(QUOTATION_STATUS_NOT_ALLOWED);
        }
        quotationMapper.deleteById(id);
        quotationItemMapper.deleteByQuotationId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmQuotation(Long id) {
        QuotationDO existing = quotationMapper.selectById(id);
        if (existing == null) {
            throw exception(QUOTATION_NOT_EXISTS);
        }
        if (!DEFAULT_STATUS.equals(existing.getStatus())) {
            throw exception(QUOTATION_STATUS_NOT_ALLOWED);
        }
        String updater = getUpdater();
        int affected = quotationMapper.confirmQuotation(id, updater, LocalDateTime.now());
        if (affected == 0) {
            throw exception(QUOTATION_STATUS_NOT_ALLOWED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelQuotation(Long id) {
        QuotationDO existing = quotationMapper.selectById(id);
        if (existing == null) {
            throw exception(QUOTATION_NOT_EXISTS);
        }
        if (!"CONFIRMED".equals(existing.getStatus())) {
            throw exception(QUOTATION_STATUS_NOT_ALLOWED);
        }
        String updater = getUpdater();
        int affected = quotationMapper.cancelQuotation(id, updater, LocalDateTime.now());
        if (affected == 0) {
            throw exception(QUOTATION_STATUS_NOT_ALLOWED);
        }
    }

    @Override
    public QuotationRespVO getQuotation(Long id) {
        QuotationDO quotation = quotationMapper.selectById(id);
        if (quotation == null) {
            throw exception(QUOTATION_NOT_EXISTS);
        }
        QuotationRespVO respVO = BeanUtils.toBean(quotation, QuotationRespVO.class);
        respVO.setExpired(isExpired(quotation));

        List<QuotationItemDO> items = quotationItemMapper.selectListByQuotationId(id);
        List<QuotationItemRespVO> itemVOs = BeanUtils.toBean(items, QuotationItemRespVO.class);
        respVO.setItems(itemVOs);

        BigDecimal minMarginRate = getMinMarginRate();
        boolean hasLowWarning = (quotation.getAvgMarginRate() != null && quotation.getAvgMarginRate().compareTo(minMarginRate) < 0)
                || items.stream().anyMatch(i -> Boolean.TRUE.equals(i.getLowMarginWarning()));
        respVO.setLowMarginWarning(hasLowWarning);

        // 数据脱敏（若调用者无金额权限，抹除金额及毛利敏感字段）
        if (!hasAmountPermission()) {
            maskSensitiveFields(respVO);
        }

        return respVO;
    }

    @Override
    public PageResult<QuotationRespVO> getQuotationPage(QuotationPageReqVO pageReqVO) {
        LambdaQueryWrapperX<QuotationDO> wrapper = new LambdaQueryWrapperX<>();

        // 1. 关键词查询（单号 / 客户名 / 备注）
        String keyword = pageReqVO.getKeyword() == null ? "" : pageReqVO.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w
                    .apply("LOWER(quotation_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(client_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(remarks) LIKE LOWER({0})", like));
        }

        // 2. 客户筛选
        wrapper.eqIfPresent(QuotationDO::getCustomerId, pageReqVO.getCustomerId());

        // 3. 状态筛选（含派生 EXPIRED）
        if ("EXPIRED".equalsIgnoreCase(pageReqVO.getStatus())) {
            wrapper.eq(QuotationDO::getStatus, "CONFIRMED")
                    .isNotNull(QuotationDO::getValidTo)
                    .lt(QuotationDO::getValidTo, LocalDate.now());
        } else if ("CONFIRMED".equalsIgnoreCase(pageReqVO.getStatus())) {
            wrapper.eq(QuotationDO::getStatus, "CONFIRMED")
                    .and(w -> w.isNull(QuotationDO::getValidTo).or().ge(QuotationDO::getValidTo, LocalDate.now()));
        } else if (StringUtils.hasText(pageReqVO.getStatus())) {
            wrapper.eq(QuotationDO::getStatus, pageReqVO.getStatus().trim());
        }

        // 4. 仅看最新版本（默认 true）
        boolean latestOnly = pageReqVO.getLatestOnly() == null || pageReqVO.getLatestOnly();
        if (latestOnly) {
            wrapper.apply("version_no = (SELECT MAX(v.version_no) FROM lunaerp_business_quotation v WHERE v.quotation_no = lunaerp_business_quotation.quotation_no AND v.deleted = 0)");
        }

        // 5. 排序：报价日期倒序，ID 倒序
        wrapper.orderByDesc(QuotationDO::getQuotationDate).orderByDesc(QuotationDO::getId);

        PageResult<QuotationDO> pageResult = quotationMapper.selectPage(pageReqVO, wrapper);
        PageResult<QuotationRespVO> respResult = BeanUtils.toBean(pageResult, QuotationRespVO.class);

        BigDecimal minMarginRate = getMinMarginRate();
        boolean hasAmountPerm = hasAmountPermission();

        for (QuotationRespVO item : respResult.getList()) {
            item.setExpired("CONFIRMED".equals(item.getStatus())
                    && item.getValidTo() != null
                    && item.getValidTo().isBefore(LocalDate.now()));
            item.setLowMarginWarning(item.getAvgMarginRate() != null && item.getAvgMarginRate().compareTo(minMarginRate) < 0);
            if (!hasAmountPerm) {
                maskSensitiveFields(item);
            }
        }

        return respResult;
    }

    @Override
    @DataPermission(enable = false)
    public String getNextQuotationNo() {
        return DataPermissionUtils.executeIgnore(this::generateQuotationNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reviseQuotation(Long id) {
        // 1. 校验源报价单存在
        QuotationDO existing = quotationMapper.selectById(id);
        if (existing == null) {
            throw exception(QUOTATION_NOT_EXISTS);
        }
        // 必须为 CONFIRMED
        if (!"CONFIRMED".equals(existing.getStatus())) {
            throw exception(QUOTATION_STATUS_NOT_ALLOWED);
        }

        // 2. 必须为该单号的最新已确认版本
        Integer maxVersion = quotationMapper.selectMaxVersionNoByQuotationNo(existing.getQuotationNo());
        if (maxVersion != null && !maxVersion.equals(existing.getVersionNo())) {
            throw exception(QUOTATION_NOT_LATEST_VERSION);
        }

        // 3. 校验该单号下不得已有草稿（同号至多一草稿）
        QuotationDO draft = quotationMapper.selectDraftByQuotationNo(existing.getQuotationNo());
        if (draft != null) {
            throw exception(QUOTATION_DRAFT_EXISTS);
        }

        // 4. 将旧版本置为 SUPERSEDED
        String updater = getUpdater();
        int affected = quotationMapper.supersedeQuotation(id, updater, LocalDateTime.now());
        if (affected == 0) {
            throw exception(QUOTATION_STATUS_NOT_ALLOWED);
        }

        // 5. 复制为新草稿，version_no + 1，有效期重置为今天起/至空，保留成本快照
        QuotationDO newDraft = BeanUtils.toBean(existing, QuotationDO.class);
        newDraft.setId(null);
        newDraft.setVersionNo(existing.getVersionNo() + 1);
        newDraft.setStatus(DEFAULT_STATUS);
        newDraft.setQuotationDate(LocalDate.now());
        newDraft.setValidFrom(LocalDate.now());
        newDraft.setValidTo(null);
        newDraft.setRemarks("修订自 v" + existing.getVersionNo() + (StringUtils.hasText(existing.getRemarks()) ? " | " + existing.getRemarks() : ""));

        try {
            quotationMapper.insert(newDraft);
        } catch (Exception ex) {
            log.warn("[reviseQuotation] 插入新版本草稿冲突: no={}, version={}", newDraft.getQuotationNo(), newDraft.getVersionNo(), ex);
            throw exception(QUOTATION_DRAFT_EXISTS);
        }

        // 6. 复制明细行（保留成本快照并标记复制自 vN，可重新取价）
        List<QuotationItemDO> oldItems = quotationItemMapper.selectListByQuotationId(existing.getId());
        for (QuotationItemDO oldItem : oldItems) {
            QuotationItemDO newItem = BeanUtils.toBean(oldItem, QuotationItemDO.class);
            newItem.setId(null);
            newItem.setQuotationId(newDraft.getId());
            String copyNote = "复制自 v" + existing.getVersionNo() + "，可重新取价";
            newItem.setRemark(StringUtils.hasText(oldItem.getRemark()) ? oldItem.getRemark() + " (" + copyNote + ")" : copyNote);
            quotationItemMapper.insert(newItem);
        }

        return newDraft.getId();
    }

    @Override
    public List<QuotationRespVO> getQuotationVersions(String quotationNo) {
        if (!StringUtils.hasText(quotationNo)) {
            return new ArrayList<>();
        }
        List<QuotationDO> list = quotationMapper.selectListByQuotationNo(quotationNo.trim());
        List<QuotationRespVO> respList = BeanUtils.toBean(list, QuotationRespVO.class);

        BigDecimal minMarginRate = getMinMarginRate();
        boolean hasAmountPerm = hasAmountPermission();

        for (QuotationRespVO item : respList) {
            item.setExpired("CONFIRMED".equals(item.getStatus())
                    && item.getValidTo() != null
                    && item.getValidTo().isBefore(LocalDate.now()));
            item.setLowMarginWarning(item.getAvgMarginRate() != null && item.getAvgMarginRate().compareTo(minMarginRate) < 0);
            if (!hasAmountPerm) {
                maskSensitiveFields(item);
            }
        }
        return respList;
    }

    @Override
    public QuotationContractPrefillRespVO getContractPrefill(Long id) {
        QuotationDO quotation = quotationMapper.selectById(id);
        if (quotation == null) {
            throw exception(QUOTATION_NOT_EXISTS);
        }
        if (!"CONFIRMED".equalsIgnoreCase(quotation.getStatus())) {
            throw exception(QUOTATION_STATUS_NOT_ALLOWED);
        }
        Integer maxVersion = quotationMapper.selectMaxVersionNoByQuotationNo(quotation.getQuotationNo());
        if (maxVersion != null && !maxVersion.equals(quotation.getVersionNo())) {
            throw exception(QUOTATION_NOT_LATEST_VERSION);
        }

        boolean expiredWarning = quotation.getValidTo() != null && quotation.getValidTo().isBefore(LocalDate.now());

        // 业务员继承：业务员已停用则回退登录用户，部门按其业务员重新推导
        Long salespersonUserId = null;
        Long deptId = null;
        if (quotation.getSalespersonUserId() != null && adminUserApi != null) {
            AdminUserRespDTO qUser = adminUserApi.getUser(quotation.getSalespersonUserId());
            if (qUser != null && CommonStatusEnum.ENABLE.getStatus().equals(qUser.getStatus())) {
                salespersonUserId = quotation.getSalespersonUserId();
                deptId = qUser.getDeptId();
            }
        }
        if (salespersonUserId == null) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId != null) {
                salespersonUserId = loginUserId;
                if (adminUserApi != null) {
                    AdminUserRespDTO loginUser = adminUserApi.getUser(loginUserId);
                    deptId = loginUser != null ? loginUser.getDeptId() : null;
                }
            }
        }

        QuotationContractPrefillRespVO resp = new QuotationContractPrefillRespVO();
        resp.setSourceQuotationId(quotation.getId());
        resp.setSourceQuotationNo(quotation.getQuotationNo() + "-v" + quotation.getVersionNo());
        resp.setCustomerId(quotation.getCustomerId());
        resp.setClientName(quotation.getClientName());
        resp.setClientContact(quotation.getClientContact());
        resp.setPhone(quotation.getPhone());
        resp.setCurrency(quotation.getCurrency());
        resp.setTradeType("DOMESTIC");
        resp.setSalespersonUserId(salespersonUserId);
        resp.setDeptId(deptId);
        resp.setExpiredWarning(expiredWarning);

        List<QuotationItemDO> items = quotationItemMapper.selectListByQuotationId(quotation.getId());
        List<QuotationContractPrefillItemRespVO> itemRespList = new ArrayList<>();
        if (items != null) {
            for (QuotationItemDO item : items) {
                QuotationContractPrefillItemRespVO itemResp = new QuotationContractPrefillItemRespVO();
                itemResp.setProductCode(item.getProductCode());
                itemResp.setProductName(item.getProductName());
                itemResp.setCustomerColor(item.getCustomerColor());
                itemResp.setComposition(item.getComposition());
                itemResp.setWidth(item.getWidth());
                itemResp.setWeight(item.getWeight());
                itemResp.setQuantity(item.getQuantity());
                itemResp.setUnitPrice(item.getUnitPrice());
                itemResp.setOtherFees(BigDecimal.ZERO);
                itemResp.setAmount(item.getAmount());
                itemResp.setRemark(item.getRemark());
                itemRespList.add(itemResp);
            }
        }
        resp.setItems(itemRespList);

        return resp;
    }

    @Override
    @LogRecord(type = "QUOTATION", subType = "PRINT", bizNo = "{{#id}}", success = "打印报价单【{{#id}}】")
    public QuotationPrintRespVO getPrintData(Long id) {
        QuotationDO quotation = quotationMapper.selectById(id);
        if (quotation == null) {
            throw exception(QUOTATION_NOT_EXISTS);
        }
        LogRecordContext.putVariable("id", quotation.getId());
        LogRecordContext.putVariable("no", quotation.getQuotationNo() + "-v" + quotation.getVersionNo());

        QuotationPrintRespVO resp = new QuotationPrintRespVO();
        resp.setId(quotation.getId());
        resp.setQuotationNo(quotation.getQuotationNo());
        resp.setVersionNo(quotation.getVersionNo());
        resp.setDisplayNo(quotation.getQuotationNo() + "-v" + quotation.getVersionNo());
        resp.setCustomerId(quotation.getCustomerId());
        resp.setClientName(quotation.getClientName());
        resp.setClientContact(quotation.getClientContact());
        resp.setPhone(quotation.getPhone());
        resp.setSalespersonUserId(quotation.getSalespersonUserId());
        if (quotation.getSalespersonUserId() != null && adminUserApi != null) {
            AdminUserRespDTO user = adminUserApi.getUser(quotation.getSalespersonUserId());
            resp.setSalespersonName(user != null ? user.getNickname() : null);
        }
        resp.setQuotationDate(quotation.getQuotationDate());
        resp.setValidFrom(quotation.getValidFrom());
        resp.setValidTo(quotation.getValidTo());
        resp.setCurrency(quotation.getCurrency());
        resp.setExchangeRate(quotation.getExchangeRate());
        resp.setTotalQuantity(quotation.getTotalQuantity());
        resp.setTotalAmount(quotation.getTotalAmount());
        resp.setRemarks(quotation.getRemarks());

        List<QuotationItemDO> items = quotationItemMapper.selectListByQuotationId(quotation.getId());
        List<QuotationPrintItemRespVO> itemRespList = new ArrayList<>();
        if (items != null) {
            for (QuotationItemDO item : items) {
                QuotationPrintItemRespVO itemResp = new QuotationPrintItemRespVO();
                itemResp.setId(item.getId());
                itemResp.setLineNo(item.getLineNo());
                itemResp.setProductCode(item.getProductCode());
                itemResp.setProductName(item.getProductName());
                itemResp.setCustomerColor(item.getCustomerColor());
                itemResp.setComposition(item.getComposition());
                itemResp.setWidth(item.getWidth());
                itemResp.setWeight(item.getWeight());
                itemResp.setUnit(item.getUnit());
                itemResp.setQuantity(item.getQuantity());
                itemResp.setUnitPrice(item.getUnitPrice());
                itemResp.setAmount(item.getAmount());
                itemResp.setRemark(item.getRemark());
                itemRespList.add(itemResp);
            }
        }
        resp.setItems(itemRespList);

        return resp;
    }

    // ==================== 私有辅助方法 ====================

    private boolean isExpired(QuotationDO quotation) {
        return "CONFIRMED".equals(quotation.getStatus())
                && quotation.getValidTo() != null
                && quotation.getValidTo().isBefore(LocalDate.now());
    }

    private boolean hasAmountPermission() {
        return securityFrameworkService != null && securityFrameworkService.hasPermission(SensitiveConstants.PERMISSION_AMOUNT);
    }

    private BigDecimal getMinMarginRate() {
        if (configApi != null) {
            String val = configApi.getConfigValueByKey("quotation.min-margin-rate");
            if (StringUtils.hasText(val)) {
                try {
                    return new BigDecimal(val.trim());
                } catch (Exception ignore) {
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private String getUpdater() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId != null ? String.valueOf(loginUserId) : "";
    }

    private void maskSensitiveFields(QuotationRespVO respVO) {
        respVO.setTotalCostAmount(null);
        respVO.setAvgMarginRate(null);
        if (respVO.getItems() != null) {
            for (QuotationItemRespVO item : respVO.getItems()) {
                item.setCostSnapshot(null);
                item.setCostUnitPriceRmb(null);
                item.setCostUnitPrice(null);
                item.setTargetMarginRate(null);
                item.setCostAmount(null);
                item.setMarginRate(null);
            }
        }
    }

    private List<QuotationItemDO> calculateAndBuildItems(List<QuotationItemSaveReqVO> itemReqList,
                                                         BigDecimal exchangeRate,
                                                         boolean hasAmountPerm,
                                                         BigDecimal minMarginRate,
                                                         QuotationDO quotationDO) {
        List<QuotationItemDO> itemDOList = new ArrayList<>(itemReqList.size());
        List<QuotationItemPricingResult> itemResults = new ArrayList<>(itemReqList.size());

        int lineNo = 1;
        for (QuotationItemSaveReqVO reqItem : itemReqList) {
            QuotationItemDO itemDO = BeanUtils.toBean(reqItem, QuotationItemDO.class);
            itemDO.setLineNo(lineNo++);
            itemDO.setUnit(StringUtils.hasText(reqItem.getUnit()) ? reqItem.getUnit().trim() : DEFAULT_UNIT);
            itemDO.setExchangeRateSnapshot(exchangeRate);

            // MANUAL 行手填成本受脱敏位约束：无权限者提交值被清零
            if ("MANUAL".equalsIgnoreCase(itemDO.getCostSource()) && !hasAmountPerm) {
                itemDO.setCostUnitPriceRmb(BigDecimal.ZERO);
            }
            if (itemDO.getCostUnitPriceRmb() == null) {
                itemDO.setCostUnitPriceRmb(BigDecimal.ZERO);
            }

            // 毛利率输入范围校验 (-100%, 100%)，等于 100% 拦截报 1_030_034_106
            if (reqItem.getTargetMarginRate() != null) {
                BigDecimal margin = reqItem.getTargetMarginRate();
                if (margin.compareTo(BigDecimal.ONE) >= 0 || margin.compareTo(new BigDecimal("-1")) <= 0) {
                    throw exception(QUOTATION_MARGIN_RATE_INVALID);
                }
            }

            // 调用纯函数计算行成本、单价、金额、毛利率
            QuotationItemPricingParam param = QuotationItemPricingParam.builder()
                    .quantity(reqItem.getQuantity())
                    .costUnitPriceRmb(itemDO.getCostUnitPriceRmb())
                    .exchangeRate(exchangeRate)
                    .targetMarginRate(reqItem.getTargetMarginRate())
                    .unitPrice(reqItem.getUnitPrice())
                    .build();

            QuotationItemPricingResult result = CostCardPricingCalculator.calculateQuotationItem(param);

            itemDO.setCostUnitPrice(result.getCostUnitPrice());
            itemDO.setUnitPrice(result.getUnitPrice());
            itemDO.setCostAmount(result.getCostAmount());
            itemDO.setAmount(result.getAmount());
            itemDO.setMarginRate(result.getMarginRate());

            // 低毛利预警判定
            boolean lowWarning = result.getMarginRate() != null && result.getMarginRate().compareTo(minMarginRate) < 0;
            itemDO.setLowMarginWarning(lowWarning);

            itemDOList.add(itemDO);
            itemResults.add(result);
        }

        // 调用纯函数汇总单头总数量、总金额、总成本额、加权毛利率
        QuotationSummaryPricingResult summary = CostCardPricingCalculator.calculateQuotationSummary(itemResults);
        quotationDO.setTotalQuantity(summary.getTotalQuantity());
        quotationDO.setTotalAmount(summary.getTotalAmount());
        quotationDO.setTotalCostAmount(summary.getTotalCostAmount());
        quotationDO.setAvgMarginRate(summary.getAvgMarginRate());

        return itemDOList;
    }

    @DataPermission(enable = false)
    private String generateQuotationNo() {
        return DataPermissionUtils.executeIgnore(() -> {
            String prefix = QUOTATION_NO_PREFIX + LocalDate.now().getYear();
            List<QuotationDO> candidates = quotationMapper.selectListByQuotationNoPrefixIncludingDeleted(prefix);
            long maxNum = 0L;
            int maxDigits = NO_MIN_DIGITS;

            for (QuotationDO candidate : candidates) {
                String no = candidate.getQuotationNo();
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
                } catch (NumberFormatException ignore) {
                }
            }

            if (maxNum == 0L) {
                return prefix + "0001";
            }
            long next = maxNum + 1L;
            return prefix + String.format("%0" + maxDigits + "d", next);
        });
    }

}
