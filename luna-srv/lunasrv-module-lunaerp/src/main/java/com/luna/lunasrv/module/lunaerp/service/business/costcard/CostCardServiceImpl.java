package com.luna.lunasrv.module.lunaerp.service.business.costcard;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.datapermission.core.util.DataPermissionUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.mybatis.core.util.MyBatisUtils;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.controller.business.costcard.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo.YarnPriceEffectiveRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard.CostCardDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard.CostCardFeeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard.CostCardYarnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardFeeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardYarnMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing.CostCardPricingCalculator;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing.CostCardPricingParam;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing.CostCardPricingResult;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing.FeePricingRow;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing.FeeRowPricingResult;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing.YarnPricingRow;
import com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing.YarnRowPricingResult;
import com.luna.lunasrv.module.lunaerp.service.production.yarnprice.YarnPriceService;
import com.luna.lunasrv.module.lunaerp.service.production.yarnrecipebom.YarnRecipeBomService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.*;

/**
 * 业务管理 - 成本卡 Service 实现
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class CostCardServiceImpl implements CostCardService {

    public static final String CARD_NO_PREFIX = "CC";
    public static final int NO_MIN_DIGITS = 4;
    public static final int MAX_GENERATE_RETRY = 5;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("\\d+");

    @Resource
    private CostCardMapper costCardMapper;

    @Resource
    private CostCardYarnMapper costCardYarnMapper;

    @Resource
    private CostCardFeeMapper costCardFeeMapper;

    @Resource
    private YarnPriceService yarnPriceService;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private YarnRecipeBomService yarnRecipeBomService;

    @Resource
    private ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCostCard(CostCardSaveReqVO reqVO) {
        // 1. 编号处理与查重（扫描含已删除行，ADR-0004）
        String cardNo = reqVO.getCardNo();
        if (!StringUtils.hasText(cardNo)) {
            cardNo = generateCardNoWithRetry();
        } else {
            cardNo = cardNo.trim();
            validateCardNoUnique(cardNo, null);
        }

        // 2. 校验产品与配比 BOM（产品无有效配比 BOM 或 BOM 无明细时报 1_030_034_004 拦截）
        validateProductAndRecipeBom(reqVO.getProductId(), reqVO.getProductName());

        // 3. 构建成本卡主表 DO
        CostCardDO card = BeanUtils.toBean(reqVO, CostCardDO.class);
        card.setCardNo(cardNo);
        card.setStatus("DRAFT");
        card.setEffectiveDate(null);

        // 4. 服务端取价 + 纯计算重算
        List<CostCardYarnItemVO> yarnVOs = reqVO.getYarnItems() != null ? reqVO.getYarnItems() : new ArrayList<>();
        List<CostCardFeeItemVO> feeVOs = reqVO.getFeeItems() != null ? reqVO.getFeeItems() : new ArrayList<>();

        fetchServerYarnPrices(yarnVOs, card.getYarnPriceType());
        CostCardPricingResult pricingResult = executePricing(card, yarnVOs, feeVOs);
        applyPricingResultToCard(card, pricingResult);

        // 5. 落库主表
        costCardMapper.insert(card);
        Long cardId = card.getId();

        // 6. 落库明细子表（重排 line_no）
        saveChildItems(cardId, yarnVOs, feeVOs, pricingResult);

        log.info("[createCostCard] 成本卡创建成功: id={}, cardNo={}", cardId, cardNo);
        return cardId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCostCard(CostCardSaveReqVO reqVO) {
        // 1. 校验存在与状态守卫（仅 DRAFT 允许修改，API 层报 1_030_034_002）
        CostCardDO existing = costCardMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw exception(COST_CARD_NOT_EXISTS);
        }
        if (!"DRAFT".equals(existing.getStatus())) {
            throw exception(COST_CARD_STATUS_NOT_ALLOWED);
        }

        // 2. 编号查重（若改动卡号，查重含已删除行，ADR-0004）
        String cardNo = reqVO.getCardNo();
        if (StringUtils.hasText(cardNo)) {
            cardNo = cardNo.trim();
            if (!cardNo.equals(existing.getCardNo())) {
                validateCardNoUnique(cardNo, existing.getId());
            }
        } else {
            cardNo = existing.getCardNo();
        }

        // 3. 校验产品与配比 BOM
        validateProductAndRecipeBom(reqVO.getProductId(), reqVO.getProductName());

        // 4. 服务端取价 + 纯计算重算
        CostCardDO updateCard = BeanUtils.toBean(reqVO, CostCardDO.class);
        updateCard.setCardNo(cardNo);
        updateCard.setStatus("DRAFT");
        updateCard.setEffectiveDate(null);

        List<CostCardYarnItemVO> yarnVOs = reqVO.getYarnItems() != null ? reqVO.getYarnItems() : new ArrayList<>();
        List<CostCardFeeItemVO> feeVOs = reqVO.getFeeItems() != null ? reqVO.getFeeItems() : new ArrayList<>();

        fetchServerYarnPrices(yarnVOs, updateCard.getYarnPriceType());
        CostCardPricingResult pricingResult = executePricing(updateCard, yarnVOs, feeVOs);
        applyPricingResultToCard(updateCard, pricingResult);

        // 5. 更新主表
        costCardMapper.updateById(updateCard);

        // 6. 替换明细子表（按 card_id 模式逻辑删旧行 + 插新行，line_no 重排）
        costCardYarnMapper.deleteByCardId(existing.getId());
        costCardFeeMapper.deleteByCardId(existing.getId());
        saveChildItems(existing.getId(), yarnVOs, feeVOs, pricingResult);

        log.info("[updateCostCard] 成本卡更新成功: id={}", existing.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCostCard(Long id) {
        // 1. 校验存在与状态守卫（仅 DRAFT 进回收站，生效/失效卡报 1_030_034_002）
        CostCardDO card = costCardMapper.selectById(id);
        if (card == null) {
            throw exception(COST_CARD_NOT_EXISTS);
        }
        if (!"DRAFT".equals(card.getStatus())) {
            throw exception(COST_CARD_STATUS_NOT_ALLOWED);
        }

        // 2. 逻辑删除主表与级联软删两子表
        costCardMapper.deleteById(id);
        costCardYarnMapper.deleteByCardId(id);
        costCardFeeMapper.deleteByCardId(id);
        log.info("[deleteCostCard] 草稿成本卡已软删: id={}", id);
    }

    @Override
    public CostCardRespVO getCostCard(Long id) {
        CostCardDO card = costCardMapper.selectById(id);
        if (card == null) {
            throw exception(COST_CARD_NOT_EXISTS);
        }
        return buildRespVO(card);
    }

    @Override
    public CostCardRespVO getEffectiveCostCardByProductId(Long productId) {
        if (productId == null) {
            return null;
        }
        CostCardDO card = costCardMapper.selectEffectiveByProductId(productId);
        if (card == null) {
            return null;
        }
        return buildRespVO(card);
    }

    @Override
    public PageResult<CostCardRespVO> getCostCardPage(CostCardPageReqVO pageReqVO) {
        LambdaQueryWrapperX<CostCardDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(CostCardDO::getStatus, pageReqVO.getStatus());
        wrapper.eqIfPresent(CostCardDO::getProductId, pageReqVO.getProductId());
        wrapper.eqIfPresent(CostCardDO::getFabricConstruction, pageReqVO.getFabricConstruction());

        String search = pageReqVO.getSearch();
        if (StringUtils.hasText(search)) {
            String kw = "%" + search.trim() + "%";
            wrapper.and(w -> w
                    .apply("LOWER(card_no) LIKE LOWER({0})", kw)
                    .or().apply("LOWER(product_code) LIKE LOWER({0})", kw)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", kw));
        }

        wrapper.orderByDesc(CostCardDO::getEffectiveDate)
                .orderByDesc(CostCardDO::getUpdateTime);

        PageResult<CostCardDO> pageResult = costCardMapper.selectPage(pageReqVO, wrapper);
        return BeanUtils.toBean(pageResult, CostCardRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void effectCostCard(Long id) {
        CostCardDO card = costCardMapper.selectById(id);
        if (card == null) {
            throw exception(COST_CARD_NOT_EXISTS);
        }
        // 1. 守卫：非草稿报 1_030_034_002
        if (!"DRAFT".equals(card.getStatus())) {
            throw exception(COST_CARD_STATUS_NOT_ALLOWED);
        }
        // 2. 守卫：缺价报 1_030_034_003
        if (card.getMissingPriceCount() != null && card.getMissingPriceCount() > 0) {
            throw exception(COST_CARD_MISSING_PRICE, card.getMissingPriceCount());
        }
        // 3. 守卫：无 BOM 报 1_030_034_004
        validateProductAndRecipeBom(card.getProductId(), card.getProductName());

        String updater = getLoginUserIdStr();
        LocalDateTime now = LocalDateTime.now();

        // 4. 同事务把同产品旧 EFFECTIVE 卡条件更新为 EXPIRED
        costCardMapper.expireEffectiveCardsByProductId(card.getProductId(), updater, now);

        // 5. 条件更新本卡为 EFFECTIVE，生效日期=今天；影响行 0 报 1_030_034_002
        try {
            int affected = costCardMapper.effectCardById(id, LocalDate.now(), updater, now);
            if (affected == 0) {
                throw exception(COST_CARD_STATUS_NOT_ALLOWED);
            }
        } catch (DuplicateKeyException ex) {
            // 部分唯一索引冲突转业务错
            log.warn("[effectCostCard] 唯一索引冲突: id={}, ex={}", id, ex.getMessage());
            throw exception(COST_CARD_STATUS_NOT_ALLOWED);
        }
        log.info("[effectCostCard] 成本卡生效成功: id={}, productId={}", id, card.getProductId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireCostCard(Long id) {
        CostCardDO card = costCardMapper.selectById(id);
        if (card == null) {
            throw exception(COST_CARD_NOT_EXISTS);
        }
        // 仅 EFFECTIVE -> EXPIRED
        if (!"EFFECTIVE".equals(card.getStatus())) {
            throw exception(COST_CARD_STATUS_NOT_ALLOWED);
        }
        int affected = costCardMapper.expireCardById(id, getLoginUserIdStr(), LocalDateTime.now());
        if (affected == 0) {
            throw exception(COST_CARD_STATUS_NOT_ALLOWED);
        }
        log.info("[expireCostCard] 成本卡手工失效成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyCostCard(Long id) {
        CostCardDO source = costCardMapper.selectById(id);
        if (source == null) {
            throw exception(COST_CARD_NOT_EXISTS);
        }

        List<CostCardYarnDO> sourceYarns = costCardYarnMapper.selectListByCardId(id);
        List<CostCardFeeDO> sourceFees = costCardFeeMapper.selectListByCardId(id);

        // 生成新卡号
        String newCardNo = generateCardNoWithRetry();

        // 创建新草稿成本卡
        CostCardDO newCard = BeanUtils.toBean(source, CostCardDO.class);
        newCard.setId(null);
        newCard.setCardNo(newCardNo);
        newCard.setStatus("DRAFT");
        newCard.setEffectiveDate(null);
        costCardMapper.insert(newCard);
        Long newCardId = newCard.getId();

        // 复制行数据，价格标记为复制时点快照
        if (CollUtil.isNotEmpty(sourceYarns)) {
            List<CostCardYarnDO> newYarns = new ArrayList<>(sourceYarns.size());
            for (int i = 0; i < sourceYarns.size(); i++) {
                CostCardYarnDO y = BeanUtils.toBean(sourceYarns.get(i), CostCardYarnDO.class);
                y.setId(null);
                y.setCardId(newCardId);
                y.setLineNo(i + 1);
                y.setPriceSource("COPY_SNAPSHOT");
                newYarns.add(y);
            }
            costCardYarnMapper.insertBatch(newYarns);
        }

        if (CollUtil.isNotEmpty(sourceFees)) {
            List<CostCardFeeDO> newFees = new ArrayList<>(sourceFees.size());
            for (int i = 0; i < sourceFees.size(); i++) {
                CostCardFeeDO f = BeanUtils.toBean(sourceFees.get(i), CostCardFeeDO.class);
                f.setId(null);
                f.setCardId(newCardId);
                f.setLineNo(i + 1);
                newFees.add(f);
            }
            costCardFeeMapper.insertBatch(newFees);
        }

        log.info("[copyCostCard] 复制新建成本卡成功: sourceId={}, newId={}, newCardNo={}", id, newCardId, newCardNo);
        return newCardId;
    }

    @Override
    public CostCardTrialRespVO refreshPrices(CostCardTrialReqVO reqVO) {
        List<CostCardYarnItemVO> yarnVOs = reqVO.getYarnItems() != null ? reqVO.getYarnItems() : new ArrayList<>();
        // 强制按价类重新拉取当日价格
        fetchServerYarnPrices(yarnVOs, reqVO.getYarnPriceType());
        reqVO.setYarnItems(yarnVOs);
        return trialCostCard(reqVO);
    }

    @Override
    public CostCardTrialRespVO trialCostCard(CostCardTrialReqVO trialReqVO) {
        CostCardDO tempCard = BeanUtils.toBean(trialReqVO, CostCardDO.class);
        List<CostCardYarnItemVO> yarnVOs = trialReqVO.getYarnItems() != null ? trialReqVO.getYarnItems() : new ArrayList<>();
        List<CostCardFeeItemVO> feeVOs = trialReqVO.getFeeItems() != null ? trialReqVO.getFeeItems() : new ArrayList<>();

        // 若纱线行未给价格或<=0，尝试根据当日价与规格回退兜底
        for (CostCardYarnItemVO y : yarnVOs) {
            if (y.getPrice() == null || y.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                if (y.getYarnSpecId() != null) {
                    YarnPriceEffectiveRespVO p = yarnPriceService.getEffectivePrice(y.getYarnSpecId(), trialReqVO.getYarnPriceType(), LocalDate.now());
                    if (p != null && p.getPrice() != null && p.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                        y.setPrice(p.getPrice());
                        y.setPriceType(p.getPriceType());
                        y.setPriceEffectiveDate(p.getEffectiveDate());
                        y.setPriceSource("PRICE_TABLE");
                    } else {
                        YarnSpecDO spec = yarnSpecMapper.selectById(y.getYarnSpecId());
                        if (spec != null && spec.getRefPrice() != null && spec.getRefPrice().compareTo(BigDecimal.ZERO) > 0) {
                            y.setPrice(spec.getRefPrice());
                            y.setPriceType(trialReqVO.getYarnPriceType());
                            y.setPriceEffectiveDate(null);
                            y.setPriceSource("SPEC_REF_FALLBACK");
                        }
                    }
                }
            }
        }

        CostCardPricingResult res = executePricing(tempCard, yarnVOs, feeVOs);

        // 回填纱线行与费用行试算贡献
        applyPricingResultToVOs(yarnVOs, feeVOs, res);

        return CostCardTrialRespVO.builder()
                .basis(res.getBasis())
                .kgPerMeter(res.getKgPerMeter())
                .yarnCostPerKg(res.getYarnCostPerKg())
                .yarnCostPerMeter(res.getYarnCostPerMeter())
                .totalFeePerKg(res.getFeeTotalPerKg())
                .totalFeePerMeter(res.getFeeTotalPerMeter())
                .unitCostPerKg(res.getUnitCostExclTaxPerKg())
                .unitCostPerKgWithTax(res.getUnitCostInclTaxPerKg())
                .unitCostPerMeter(res.getUnitCostExclTaxPerMeter())
                .unitCostPerMeterWithTax(res.getUnitCostInclTaxPerMeter())
                .missingPriceCount(res.getMissingPriceCount())
                .yarnItems(yarnVOs)
                .feeItems(feeVOs)
                .build();
    }

    @Override
    @DataPermission(enable = false)
    public String getNextCardNo() {
        return DataPermissionUtils.executeIgnore(this::generateCardNo);
    }

    // ==================== 私有辅助方法 ====================

    private CostCardRespVO buildRespVO(CostCardDO card) {
        CostCardRespVO vo = BeanUtils.toBean(card, CostCardRespVO.class);
        List<CostCardYarnDO> yarns = costCardYarnMapper.selectListByCardId(card.getId());
        List<CostCardFeeDO> fees = costCardFeeMapper.selectListByCardId(card.getId());
        vo.setYarnItems(BeanUtils.toBean(yarns, CostCardYarnItemVO.class));
        vo.setFeeItems(BeanUtils.toBean(fees, CostCardFeeItemVO.class));
        return vo;
    }

    private void validateProductAndRecipeBom(Long productId, String productName) {
        if (productId == null) {
            throw exception(COST_CARD_RECIPE_BOM_MISSING, productName != null ? productName : "");
        }
        YarnRecipeBomRespVO bom = yarnRecipeBomService.getYarnRecipeBomByProductId(productId);
        if (bom == null || CollUtil.isEmpty(bom.getItems())) {
            String name = StringUtils.hasText(productName) ? productName : String.valueOf(productId);
            throw exception(COST_CARD_RECIPE_BOM_MISSING, name);
        }
    }

    private void validateCardNoUnique(String cardNo, Long excludeId) {
        CostCardDO existing = costCardMapper.selectByCardNoIncludingDeleted(cardNo);
        if (existing != null && (excludeId == null || !existing.getId().equals(excludeId))) {
            throw exception(COST_CARD_NO_DUPLICATE, cardNo);
        }
    }

    private void fetchServerYarnPrices(List<CostCardYarnItemVO> yarnVOs, String priceType) {
        if (CollUtil.isEmpty(yarnVOs)) {
            return;
        }
        for (CostCardYarnItemVO item : yarnVOs) {
            if (item.getYarnSpecId() == null) {
                continue;
            }
            YarnPriceEffectiveRespVO priceRecord = yarnPriceService.getEffectivePrice(
                    item.getYarnSpecId(),
                    StringUtils.hasText(priceType) ? priceType : "PURCHASE",
                    LocalDate.now()
            );
            if (priceRecord != null && priceRecord.getPrice() != null && priceRecord.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                item.setPrice(priceRecord.getPrice());
                item.setPriceType(priceRecord.getPriceType());
                item.setPriceEffectiveDate(priceRecord.getEffectiveDate());
                item.setPriceSource("PRICE_TABLE");
            } else {
                YarnSpecDO spec = yarnSpecMapper.selectById(item.getYarnSpecId());
                if (spec != null && spec.getRefPrice() != null && spec.getRefPrice().compareTo(BigDecimal.ZERO) > 0) {
                    item.setPrice(spec.getRefPrice());
                    item.setPriceType(priceType);
                    item.setPriceEffectiveDate(null);
                    item.setPriceSource("SPEC_REF_FALLBACK");
                } else {
                    item.setPrice(BigDecimal.ZERO);
                    item.setPriceType(priceType);
                    item.setPriceEffectiveDate(null);
                    item.setPriceSource("PRICE_TABLE");
                }
            }
        }
    }

    private CostCardPricingResult executePricing(CostCardDO card, List<CostCardYarnItemVO> yarnVOs, List<CostCardFeeItemVO> feeVOs) {
        List<YarnPricingRow> yarnRows = new ArrayList<>();
        if (CollUtil.isNotEmpty(yarnVOs)) {
            for (CostCardYarnItemVO y : yarnVOs) {
                yarnRows.add(YarnPricingRow.builder()
                        .yarnSpecId(y.getYarnSpecId())
                        .yarnSpecName(y.getYarnSpecName())
                        .price(y.getPrice())
                        .ratio(y.getRatio())
                        .direction(y.getDirection())
                        .density(y.getDensity())
                        .crimpRate(y.getCrimpRate())
                        .wasteRate(y.getWasteRate())
                        .consumptionKgPerMeter(y.getConsumptionKgPerMeter())
                        .build());
            }
        }

        List<FeePricingRow> feeRows = new ArrayList<>();
        if (CollUtil.isNotEmpty(feeVOs)) {
            for (CostCardFeeItemVO f : feeVOs) {
                feeRows.add(FeePricingRow.builder()
                        .feeType(f.getFeeType())
                        .feeUnit(f.getFeeUnit())
                        .price(f.getPrice())
                        .lossRate(f.getLossRate())
                        .build());
            }
        }

        CostCardPricingParam param = CostCardPricingParam.builder()
                .fabricConstruction(card.getFabricConstruction())
                .wovenCalculationMode(card.getWovenCalculationMode())
                .greigeWeightGsm(card.getGreigeWeightGsm())
                .greigeWidthCm(card.getGreigeWidthCm())
                .taxRate(card.getTaxRate())
                .yarnRows(yarnRows)
                .feeRows(feeRows)
                .build();

        return CostCardPricingCalculator.calculateCostCard(param);
    }

    private void applyPricingResultToCard(CostCardDO card, CostCardPricingResult res) {
        card.setBasis(res.getBasis());
        card.setKgPerMeter(res.getKgPerMeter());
        card.setYarnCostPerKg(res.getYarnCostPerKg());
        card.setYarnCostPerMeter(res.getYarnCostPerMeter());
        card.setTotalFeePerKg(res.getFeeTotalPerKg());
        card.setTotalFeePerMeter(res.getFeeTotalPerMeter());
        card.setUnitCostPerKg(res.getUnitCostExclTaxPerKg());
        card.setUnitCostPerKgWithTax(res.getUnitCostInclTaxPerKg());
        card.setUnitCostPerMeter(res.getUnitCostExclTaxPerMeter());
        card.setUnitCostPerMeterWithTax(res.getUnitCostInclTaxPerMeter());
        card.setMissingPriceCount(res.getMissingPriceCount());
    }

    private void applyPricingResultToVOs(List<CostCardYarnItemVO> yarnVOs, List<CostCardFeeItemVO> feeVOs, CostCardPricingResult res) {
        List<YarnRowPricingResult> yResults = res.getYarnRowResults() != null ? res.getYarnRowResults() : Collections.emptyList();
        for (int i = 0; i < yarnVOs.size(); i++) {
            CostCardYarnItemVO y = yarnVOs.get(i);
            if (i < yResults.size()) {
                YarnRowPricingResult yr = yResults.get(i);
                y.setContributionPerKg(yr.getContributionPerKg());
                y.setContributionPerMeter(yr.getContributionPerMeter());
            }
        }

        List<FeeRowPricingResult> fResults = res.getFeeRowResults() != null ? res.getFeeRowResults() : Collections.emptyList();
        for (int i = 0; i < feeVOs.size(); i++) {
            CostCardFeeItemVO f = feeVOs.get(i);
            if (i < fResults.size()) {
                FeeRowPricingResult fr = fResults.get(i);
                f.setContributionPerKg(fr.getContributionPerKg());
                f.setContributionPerMeter(fr.getContributionPerMeter());
            }
        }
    }

    private void saveChildItems(Long cardId, List<CostCardYarnItemVO> yarnVOs, List<CostCardFeeItemVO> feeVOs, CostCardPricingResult res) {
        applyPricingResultToVOs(yarnVOs, feeVOs, res);

        if (CollUtil.isNotEmpty(yarnVOs)) {
            List<CostCardYarnDO> yarnDOs = new ArrayList<>(yarnVOs.size());
            for (int i = 0; i < yarnVOs.size(); i++) {
                CostCardYarnItemVO vo = yarnVOs.get(i);
                CostCardYarnDO doObj = BeanUtils.toBean(vo, CostCardYarnDO.class);
                doObj.setId(null);
                doObj.setCardId(cardId);
                doObj.setLineNo(i + 1);
                yarnDOs.add(doObj);
            }
            costCardYarnMapper.insertBatch(yarnDOs);
        }

        if (CollUtil.isNotEmpty(feeVOs)) {
            List<CostCardFeeDO> feeDOs = new ArrayList<>(feeVOs.size());
            for (int i = 0; i < feeVOs.size(); i++) {
                CostCardFeeItemVO vo = feeVOs.get(i);
                CostCardFeeDO doObj = BeanUtils.toBean(vo, CostCardFeeDO.class);
                doObj.setId(null);
                doObj.setCardId(cardId);
                doObj.setLineNo(i + 1);
                feeDOs.add(doObj);
            }
            costCardFeeMapper.insertBatch(feeDOs);
        }
    }

    @DataPermission(enable = false)
    private String generateCardNoWithRetry() {
        return DataPermissionUtils.executeIgnore(() -> {
            for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
                String candidate = generateCardNo();
                if (costCardMapper.selectByCardNoIncludingDeleted(candidate) == null) {
                    return candidate;
                }
            }
            throw exception(COST_CARD_NO_DUPLICATE, "生成冲突");
        });
    }

    @DataPermission(enable = false)
    private String generateCardNo() {
        return DataPermissionUtils.executeIgnore(() -> {
            String prefix = CARD_NO_PREFIX + LocalDate.now().getYear();
            List<CostCardDO> candidates = costCardMapper.selectListByCardNoPrefixIncludingDeleted(prefix);
            long maxNum = 0L;
            int maxDigits = NO_MIN_DIGITS;
            for (CostCardDO candidate : candidates) {
                String no = candidate.getCardNo();
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
            if (maxNum == 0L) {
                return prefix + "0001";
            }
            return prefix + String.format("%0" + maxDigits + "d", maxNum + 1);
        });
    }

    private String getLoginUserIdStr() {
        Long uid = SecurityFrameworkUtils.getLoginUserId();
        return uid != null ? String.valueOf(uid) : "1";
    }

}
