package com.luna.lunasrv.module.lunaerp.service.production.yarnprice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportHelper;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ImportRow;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnprice.YarnPriceDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnprice.YarnPriceMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.YARN_PRICE_DUPLICATE_DATE;
import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.YARN_PRICE_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.YARN_SPEC_NOT_EXISTS;

/**
 * 生产管理 - 纱价主档 Service 实现
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class YarnPriceServiceImpl implements YarnPriceService {

    @Resource
    private YarnPriceMapper yarnPriceMapper;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createYarnPrice(YarnPriceSaveReqVO createReqVO) {
        // 1. 校验规格存在
        YarnSpecDO spec = validateYarnSpecExists(createReqVO.getYarnSpecId());

        // 2. 规范化价类
        String priceType = normalizePriceType(createReqVO.getPriceType());

        // 3. 校验同规格同价类同生效日期唯一
        validateDuplicateDate(null, createReqVO.getYarnSpecId(), priceType, createReqVO.getEffectiveDate(), spec.getSpecName());

        // 4. 构建 DO 并持久化
        YarnPriceDO yarnPrice = BeanUtils.toBean(createReqVO, YarnPriceDO.class);
        yarnPrice.setPriceType(priceType);
        yarnPrice.setSpecName(spec.getSpecName());
        yarnPrice.setYarnTypeId(spec.getYarnTypeId());
        if (!StringUtils.hasText(yarnPrice.getSource())) {
            yarnPrice.setSource("HAND");
        }
        yarnPriceMapper.insert(yarnPrice);
        return yarnPrice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateYarnPrice(YarnPriceSaveReqVO updateReqVO) {
        // 1. 校验存在
        YarnPriceDO existing = validateYarnPriceExists(updateReqVO.getId());

        // 2. 校验规格存在
        YarnSpecDO spec = validateYarnSpecExists(updateReqVO.getYarnSpecId());

        // 3. 规范化价类
        String priceType = normalizePriceType(updateReqVO.getPriceType());

        // 4. 校验同日唯一
        validateDuplicateDate(updateReqVO.getId(), updateReqVO.getYarnSpecId(), priceType, updateReqVO.getEffectiveDate(), spec.getSpecName());

        // 5. 更新
        YarnPriceDO updateDO = BeanUtils.toBean(updateReqVO, YarnPriceDO.class);
        updateDO.setPriceType(priceType);
        updateDO.setSpecName(spec.getSpecName());
        updateDO.setYarnTypeId(spec.getYarnTypeId());
        if (!StringUtils.hasText(updateDO.getSource())) {
            updateDO.setSource(existing.getSource());
        }
        yarnPriceMapper.updateById(updateDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteYarnPrice(Long id) {
        validateYarnPriceExists(id);
        yarnPriceMapper.deleteById(id);
    }

    @Override
    public YarnPriceRespVO getYarnPrice(Long id) {
        YarnPriceDO yarnPrice = validateYarnPriceExists(id);
        return BeanUtils.toBean(yarnPrice, YarnPriceRespVO.class);
    }

    @Override
    public PageResult<YarnPriceRespVO> getYarnPricePage(YarnPricePageReqVO pageReqVO) {
        PageResult<YarnPriceDO> page = yarnPriceMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(page, YarnPriceRespVO.class);
    }

    @Override
    public List<YarnPriceRespVO> listBySpec(Long yarnSpecId, String priceType) {
        if (yarnSpecId == null) {
            return Collections.emptyList();
        }
        String normalizedType = StringUtils.hasText(priceType) ? normalizePriceType(priceType) : null;
        List<YarnPriceDO> list = yarnPriceMapper.selectListBySpec(yarnSpecId, normalizedType);
        return BeanUtils.toBean(list, YarnPriceRespVO.class);
    }

    @Override
    public BigDecimal getLatestPurchasedPrice(Long yarnSpecId) {
        if (yarnSpecId == null) {
            return null;
        }
        return yarnPriceMapper.selectLatestPurchasedUnitPrice(yarnSpecId);
    }

    @Override
    public YarnPriceEffectiveRespVO getEffectivePrice(Long yarnSpecId, String priceType, LocalDate date) {
        if (yarnSpecId == null) {
            return YarnPriceEffectiveRespVO.builder()
                    .yarnSpecId(null)
                    .price(BigDecimal.ZERO)
                    .priceSource("MISSING")
                    .isMissing(true)
                    .build();
        }
        String type = StringUtils.hasText(priceType) ? normalizePriceType(priceType) : "PURCHASE";
        LocalDate queryDate = date != null ? date : LocalDate.now();

        // 1. 查纱价主档：取 effective_date <= queryDate 的最近一条未删记录
        YarnPriceDO latestPrice = yarnPriceMapper.selectLatestEffectivePrice(yarnSpecId, type, queryDate);
        if (latestPrice != null && latestPrice.getPrice() != null && latestPrice.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return YarnPriceEffectiveRespVO.builder()
                    .yarnSpecId(yarnSpecId)
                    .priceType(type)
                    .price(latestPrice.getPrice())
                    .effectiveDate(latestPrice.getEffectiveDate())
                    .priceSource("PRICE_TABLE")
                    .isMissing(false)
                    .build();
        }

        // 2. 回退：查规格档案 ref_price
        YarnSpecDO spec = yarnSpecMapper.selectById(yarnSpecId);
        if (spec != null && spec.getRefPrice() != null && spec.getRefPrice().compareTo(BigDecimal.ZERO) > 0) {
            return YarnPriceEffectiveRespVO.builder()
                    .yarnSpecId(yarnSpecId)
                    .priceType(type)
                    .price(spec.getRefPrice())
                    .effectiveDate(null)
                    .priceSource("SPEC_REF_FALLBACK")
                    .isMissing(false)
                    .build();
        }

        // 3. 连 ref_price 都没有，计缺价
        return YarnPriceEffectiveRespVO.builder()
                .yarnSpecId(yarnSpecId)
                .priceType(type)
                .price(BigDecimal.ZERO)
                .effectiveDate(null)
                .priceSource("MISSING")
                .isMissing(true)
                .build();
    }

    @Override
    public List<YarnPriceImportVO> getImportTemplate() {
        return Collections.singletonList(
                YarnPriceImportVO.builder()
                        .standardName("32S-COTTON")
                        .yarnTypeName("棉")
                        .specName("32S 棉")
                        .priceType("采购参考价")
                        .effectiveDate("2026-10-10")
                        .price(new BigDecimal("35.5000"))
                        .remark("月初集中调价")
                        .build()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExcelImportResult importYarnPrice(MultipartFile file) throws IOException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return ExcelImportHelper.importExcel(
                file,
                YarnPriceImportVO.class,
                rows -> validateImportRows(rows, dateFormatter),
                validList -> {
                    List<YarnPriceDO> doList = new ArrayList<>(validList.size());
                    for (YarnPriceImportVO vo : validList) {
                        YarnSpecDO spec = findSpecByImportRow(vo);
                        String priceType = parsePriceTypeFromImport(vo.getPriceType());
                        LocalDate effectiveDate = LocalDate.parse(vo.getEffectiveDate().trim(), dateFormatter);

                        YarnPriceDO priceDO = YarnPriceDO.builder()
                                .yarnSpecId(spec.getId())
                                .yarnTypeId(spec.getYarnTypeId())
                                .specName(spec.getSpecName())
                                .priceType(priceType)
                                .price(vo.getPrice())
                                .effectiveDate(effectiveDate)
                                .source("EXCEL")
                                .remark(vo.getRemark())
                                .build();
                        doList.add(priceDO);
                    }
                    for (YarnPriceDO priceDO : doList) {
                        yarnPriceMapper.insert(priceDO);
                    }
                }
        );
    }

    // ==================== 私有辅助方法 ====================

    private YarnPriceDO validateYarnPriceExists(Long id) {
        if (id == null) {
            throw exception(YARN_PRICE_NOT_EXISTS);
        }
        YarnPriceDO existing = yarnPriceMapper.selectById(id);
        if (existing == null) {
            throw exception(YARN_PRICE_NOT_EXISTS);
        }
        return existing;
    }

    private YarnSpecDO validateYarnSpecExists(Long yarnSpecId) {
        if (yarnSpecId == null) {
            throw exception(YARN_SPEC_NOT_EXISTS);
        }
        YarnSpecDO spec = yarnSpecMapper.selectById(yarnSpecId);
        if (spec == null) {
            throw exception(YARN_SPEC_NOT_EXISTS);
        }
        return spec;
    }

    private void validateDuplicateDate(Long currentId, Long yarnSpecId, String priceType, LocalDate effectiveDate, String specName) {
        if (yarnSpecId == null || effectiveDate == null || !StringUtils.hasText(priceType)) {
            return;
        }
        YarnPriceDO existing = yarnPriceMapper.selectBySpecTypeAndDate(yarnSpecId, priceType, effectiveDate);
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            String typeLabel = getPriceTypeLabel(priceType);
            throw exception(YARN_PRICE_DUPLICATE_DATE, specName, typeLabel, effectiveDate);
        }
    }

    private String normalizePriceType(String priceType) {
        if (!StringUtils.hasText(priceType)) {
            return "PURCHASE";
        }
        String trimmed = priceType.trim();
        if ("MARKET".equalsIgnoreCase(trimmed) || "市场参考价".equals(trimmed)) {
            return "MARKET";
        }
        return "PURCHASE";
    }

    private String getPriceTypeLabel(String priceType) {
        if ("MARKET".equalsIgnoreCase(priceType)) {
            return "市场参考价";
        }
        return "采购参考价";
    }

    private YarnSpecDO findSpecByImportRow(YarnPriceImportVO vo) {
        if (StringUtils.hasText(vo.getStandardName())) {
            YarnSpecDO spec = yarnSpecMapper.selectOne(new LambdaQueryWrapper<YarnSpecDO>()
                    .eq(YarnSpecDO::getStandardName, vo.getStandardName().trim())
                    .last("LIMIT 1"));
            if (spec != null) {
                return spec;
            }
        }
        if (StringUtils.hasText(vo.getYarnTypeName()) && StringUtils.hasText(vo.getSpecName())) {
            YarnTypeDO yarnType = yarnTypeMapper.selectOne(new LambdaQueryWrapper<YarnTypeDO>()
                    .eq(YarnTypeDO::getName, vo.getYarnTypeName().trim())
                    .last("LIMIT 1"));
            if (yarnType != null) {
                return yarnSpecMapper.selectOne(new LambdaQueryWrapper<YarnSpecDO>()
                        .eq(YarnSpecDO::getYarnTypeId, yarnType.getId())
                        .eq(YarnSpecDO::getSpecName, vo.getSpecName().trim())
                        .last("LIMIT 1"));
            }
        }
        return null;
    }

    private String parsePriceTypeFromImport(String priceType) {
        if (!StringUtils.hasText(priceType)) {
            return "PURCHASE";
        }
        String trimmed = priceType.trim();
        if ("MARKET".equalsIgnoreCase(trimmed) || "市场参考价".equals(trimmed)) {
            return "MARKET";
        }
        return "PURCHASE";
    }

    private void validateImportRows(List<ImportRow<YarnPriceImportVO>> rows, DateTimeFormatter dateFormatter) {
        Set<String> fileUniqueKeys = new HashSet<>();

        for (ImportRow<YarnPriceImportVO> row : rows) {
            YarnPriceImportVO vo = row.getData();

            // 1. 查找规格
            YarnSpecDO spec = findSpecByImportRow(vo);
            if (spec == null) {
                row.addError("未找到匹配的纱线规格（请填写正确的标准名称或材质+规格名）");
            }

            // 2. 价格校验
            if (vo.getPrice() == null) {
                row.addError("单价不能为空");
            } else if (vo.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                row.addError("单价必须大于 0");
            }

            // 3. 生效日期校验
            LocalDate effectiveDate = null;
            if (!StringUtils.hasText(vo.getEffectiveDate())) {
                row.addError("生效日期不能为空");
            } else {
                try {
                    effectiveDate = LocalDate.parse(vo.getEffectiveDate().trim(), dateFormatter);
                } catch (Exception e) {
                    row.addError("生效日期格式不正确，应为 yyyy-MM-dd");
                }
            }

            // 4. 重复校验
            if (spec != null && effectiveDate != null) {
                String priceType = parsePriceTypeFromImport(vo.getPriceType());
                String uniqueKey = spec.getId() + "_" + priceType + "_" + effectiveDate;

                if (!fileUniqueKeys.add(uniqueKey)) {
                    row.addError("导入文件中包含重复的同日价格记录");
                } else {
                    YarnPriceDO existing = yarnPriceMapper.selectBySpecTypeAndDate(spec.getId(), priceType, effectiveDate);
                    if (existing != null) {
                        row.addError("该规格在 " + effectiveDate + " 已存在" + getPriceTypeLabel(priceType) + "记录");
                    }
                }
            }
        }
    }

}
