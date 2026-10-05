package com.luna.lunasrv.module.lunaerp.service.sample.sampleanalysis;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPaymentReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPaymentStatsRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisSupplierQuoteRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisSupplierQuoteSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisYarnArrangeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisYarnArrangeSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisYarnSpecRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisYarnSpecSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisSupplierQuoteDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisYarnArrangeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisYarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis.SampleAnalysisMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis.SampleAnalysisSupplierQuoteMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis.SampleAnalysisYarnArrangeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis.SampleAnalysisYarnSpecMapper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisStitchArrangeVO;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.luna.lunasrv.module.lunaerp.enums.SampleErrorCodeConstants.*;

/**
 * 样品管理-样品分析 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class SampleAnalysisServiceImpl implements SampleAnalysisService {

    public static final String SAMPLE_ANALYSIS_NO_PREFIX = "SA";
    public static final int NO_MIN_DIGITS = 4;
    private static final int MAX_GENERATE_RETRY = 5;
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");
    private static final BigDecimal DEFAULT_UNIT_PRICE = new BigDecimal("30");
    private static final BigDecimal HUNDRED_THOUSAND = new BigDecimal("100000");
    public static final BigDecimal HUNDRED = new BigDecimal("100");
    public static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final int MAX_PHOTOS_COUNT = 9;
    private static final Set<String> ALLOWED_STITCH_SYMBOLS = Set.of("", "v", "-", "+", "=");

    public static final String PERMISSION_PAY = "lunaerp:sample:sample-analysis:pay";

    @Resource
    private SampleAnalysisMapper sampleAnalysisMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private SampleAnalysisSupplierQuoteMapper sampleAnalysisSupplierQuoteMapper;

    @Resource
    private SampleAnalysisYarnArrangeMapper sampleAnalysisYarnArrangeMapper;

    @Resource
    private SampleAnalysisYarnSpecMapper sampleAnalysisYarnSpecMapper;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private SecurityFrameworkService securityFrameworkService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSampleAnalysis(SampleAnalysisSaveReqVO createReqVO) {
        // 0. 必填项校验
        if (!StringUtils.hasText(createReqVO.getProductName())) {
            throw exception(SAMPLE_ANALYSIS_PRODUCT_NAME_EMPTY);
        }

        // 照片上限校验（最多 9 张）
        validatePhotos(createReqVO.getPhotos());

        // 三角排列画板校验（rows=4，columns 与 data 维数匹配，符号五态限制）
        validateStitchArrange(createReqVO.getStitchArrange());

        // 1. 编号处理：未传则自动取号；传了则校验唯一性（含已删除行，ADR-0036）
        String sampleAnalysisNo = createReqVO.getSampleAnalysisNo();
        if (StringUtils.hasText(sampleAnalysisNo)) {
            validateSampleAnalysisNoUnique(null, sampleAnalysisNo.trim());
            sampleAnalysisNo = sampleAnalysisNo.trim();
        } else {
            sampleAnalysisNo = generateSampleAnalysisNoWithRetry();
        }

        // 2. 客户关联校验与快照
        String clientName = validateCustomerAndResolveClientName(createReqVO.getCustomerId(), createReqVO.getClientName());

        // 3. 服务端计算米长：100000 / (width * weight) 四舍五入保留两位
        BigDecimal meterLength = calculateMeterLength(createReqVO.getWidth(), createReqVO.getWeight());

        // 4. 登记人快照（当前用户 ID + 姓名快照）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String creatorName = getUserNickname(loginUserId);

        // 5. 组装 DO
        SampleAnalysisDO sampleDO = BeanUtils.toBean(createReqVO, SampleAnalysisDO.class);
        sampleDO.setSampleAnalysisNo(sampleAnalysisNo);
        sampleDO.setClientName(clientName);
        sampleDO.setMeterLength(meterLength);
        sampleDO.setCreatorId(loginUserId);
        sampleDO.setCreatorName(creatorName);
        if (sampleDO.getIsPaid() == null) {
            sampleDO.setIsPaid(false);
        }
        if (sampleDO.getUnitPrice() == null) {
            sampleDO.setUnitPrice(DEFAULT_UNIT_PRICE);
        }
        // 服务端权威重算总成本（两类报价绝不参与总成本）
        BigDecimal yarnCost = calculateYarnCost(createReqVO.getYarnSpecs());
        BigDecimal totalCost = calculateTotalCost(yarnCost, createReqVO.getProductionCost(), createReqVO.getDyeingCost(), createReqVO.getOtherCost(), createReqVO.getLossRate());
        sampleDO.setTotalCost(totalCost);

        // 6. 落库
        sampleAnalysisMapper.insert(sampleDO);

        // 7. 保存供应商报价子表
        saveSupplierQuotesOnCreate(sampleDO.getId(), createReqVO.getSupplierQuotes());
        // 8. 保存绕纱行子表（ADR-0036 票04）
        saveYarnArrangesOnCreate(sampleDO.getId(), createReqVO.getYarnArranges());
        // 9. 保存纱线规格明细子表（ADR-0036 票03）
        saveYarnSpecsOnCreate(sampleDO.getId(), createReqVO.getYarnSpecs());
        return sampleDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSampleAnalysis(SampleAnalysisSaveReqVO updateReqVO) {
        // 0. 必填项校验
        if (!StringUtils.hasText(updateReqVO.getProductName())) {
            throw exception(SAMPLE_ANALYSIS_PRODUCT_NAME_EMPTY);
        }

        // 照片上限校验（最多 9 张）
        validatePhotos(updateReqVO.getPhotos());

        // 三角排列画板校验（rows=4，columns 与 data 维数匹配，符号五态限制）
        validateStitchArrange(updateReqVO.getStitchArrange());

        // 1. 校验存在
        SampleAnalysisDO exist = validateSampleAnalysisExists(updateReqVO.getId());

        // 2. 编号处理：若修改编号，校验唯一性（含已删除行）
        String sampleAnalysisNo = updateReqVO.getSampleAnalysisNo();
        if (StringUtils.hasText(sampleAnalysisNo)) {
            validateSampleAnalysisNoUnique(updateReqVO.getId(), sampleAnalysisNo.trim());
            sampleAnalysisNo = sampleAnalysisNo.trim();
        } else {
            sampleAnalysisNo = exist.getSampleAnalysisNo();
        }

        // 3. 客户关联校验与快照
        String clientName = validateCustomerAndResolveClientName(updateReqVO.getCustomerId(), updateReqVO.getClientName());

        // 4. 服务端重算米长
        BigDecimal meterLength = calculateMeterLength(updateReqVO.getWidth(), updateReqVO.getWeight());

        // 5. 组装更新（登记人保持快照不变，不随更新变更）
        SampleAnalysisDO updateDO = BeanUtils.toBean(updateReqVO, SampleAnalysisDO.class);
        updateDO.setSampleAnalysisNo(sampleAnalysisNo);
        updateDO.setClientName(clientName);
        updateDO.setMeterLength(meterLength);
        updateDO.setPhotos(updateReqVO.getPhotos());
        // 明确不修改创建时记录的登记人快照与付款状态
        updateDO.setCreatorId(exist.getCreatorId());
        updateDO.setCreatorName(exist.getCreatorName());
        updateDO.setIsPaid(exist.getIsPaid());
        // 服务端权威重算总成本（两类报价绝不参与总成本）
        BigDecimal yarnCost = calculateYarnCost(updateReqVO.getYarnSpecs());
        BigDecimal totalCost = calculateTotalCost(yarnCost, updateReqVO.getProductionCost(), updateReqVO.getDyeingCost(), updateReqVO.getOtherCost(), updateReqVO.getLossRate());
        updateDO.setTotalCost(totalCost);

        sampleAnalysisMapper.updateById(updateDO);

        // 6. 差异保存供应商报价子表
        diffSaveSupplierQuotes(updateReqVO.getId(), updateReqVO.getSupplierQuotes());
        // 7. 差异保存绕纱行子表（ADR-0032 / ADR-0036 票04）
        diffSaveYarnArranges(updateReqVO.getId(), updateReqVO.getYarnArranges());
        // 8. 差异保存纱线规格明细子表（ADR-0032 / ADR-0036 票03）
        diffSaveYarnSpecs(updateReqVO.getId(), updateReqVO.getYarnSpecs());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSampleAnalysis(Long id) {
        // 校验存在
        validateSampleAnalysisExists(id);
        // 级联逻辑删除供应商报价子表
        sampleAnalysisSupplierQuoteMapper.deleteBySampleAnalysisId(id);
        // 级联逻辑删除绕纱行子表（ADR-0036 票04）
        sampleAnalysisYarnArrangeMapper.deleteBySampleAnalysisId(id);
        // 级联逻辑删除纱线规格明细子表（ADR-0036 票03）
        sampleAnalysisYarnSpecMapper.deleteBySampleAnalysisId(id);
        // 逻辑删除主表（编号不释放、不重用）
        sampleAnalysisMapper.deleteById(id);
    }

    @Override
    public SampleAnalysisRespVO getSampleAnalysis(Long id) {
        SampleAnalysisDO sampleDO = validateSampleAnalysisExists(id);
        SampleAnalysisRespVO respVO = BeanUtils.toBean(sampleDO, SampleAnalysisRespVO.class);
        populateFirstPhotoUrl(respVO);
        // 还原供应商报价子表
        List<SampleAnalysisSupplierQuoteDO> quotes = sampleAnalysisSupplierQuoteMapper.selectListBySampleAnalysisId(id);
        respVO.setSupplierQuotes(BeanUtils.toBean(quotes, SampleAnalysisSupplierQuoteRespVO.class));
        // 还原绕纱行子表（保序，ADR-0036 票04）
        List<SampleAnalysisYarnArrangeDO> arranges = sampleAnalysisYarnArrangeMapper.selectListBySampleAnalysisId(id);
        respVO.setYarnArranges(BeanUtils.toBean(arranges, SampleAnalysisYarnArrangeRespVO.class));
        // 还原纱线规格明细子表（保序，ADR-0036 票03）
        List<SampleAnalysisYarnSpecDO> yarnSpecs = sampleAnalysisYarnSpecMapper.selectListBySampleAnalysisId(id);
        respVO.setYarnSpecs(BeanUtils.toBean(yarnSpecs, SampleAnalysisYarnSpecRespVO.class));
        return respVO;
    }

    @Override
    public PageResult<SampleAnalysisRespVO> getSampleAnalysisPage(SampleAnalysisPageReqVO pageReqVO) {
        LambdaQueryWrapperX<SampleAnalysisDO> wrapper = buildPageWrapper(pageReqVO);
        wrapper.orderByDesc(SampleAnalysisDO::getCreateTime);
        PageResult<SampleAnalysisDO> pageResult = sampleAnalysisMapper.selectPage(pageReqVO, wrapper);
        PageResult<SampleAnalysisRespVO> respPage = BeanUtils.toBean(pageResult, SampleAnalysisRespVO.class);
        if (respPage.getList() != null) {
            for (SampleAnalysisRespVO item : respPage.getList()) {
                populateFirstPhotoUrl(item);
                // 列表只请求第一张，详情才返回全部 URL（避免列表接口载荷因照片数量明显膨胀）
                item.setPhotos(null);
            }
        }
        return respPage;
    }

    @Override
    public String getNextSampleAnalysisNo() {
        // 只读预览、不占号：扫描同租户存量（含软删行），返回下一个建议编号
        return generateSampleAnalysisNo();
    }

    @Override
    public SampleAnalysisPaymentStatsRespVO getSampleAnalysisPaymentStats(SampleAnalysisPageReqVO reqVO) {
        LambdaQueryWrapperX<SampleAnalysisDO> wrapper = buildPageWrapper(reqVO);
        List<SampleAnalysisDO> list = sampleAnalysisMapper.selectList(wrapper);
        BigDecimal unpaidAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        long totalCount = 0L;
        if (list != null) {
            for (SampleAnalysisDO item : list) {
                totalCount++;
                BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                if (Boolean.TRUE.equals(item.getIsPaid())) {
                    paidAmount = paidAmount.add(price);
                } else {
                    unpaidAmount = unpaidAmount.add(price);
                }
            }
        }
        return new SampleAnalysisPaymentStatsRespVO(unpaidAmount, paidAmount, totalCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSampleAnalysisPayment(SampleAnalysisPaymentReqVO reqVO) {
        // 0. 权限校验（独立 pay 权限守卫，未授权调用抛出 AccessDeniedException）
        if (securityFrameworkService != null && !securityFrameworkService.hasPermission(PERMISSION_PAY)) {
            throw new AccessDeniedException("没有样品分析收费权限");
        }

        if (reqVO.getIds() == null || reqVO.getIds().isEmpty()) {
            return;
        }

        // 1. 校验目标记录存在
        Set<Long> uniqueIds = new HashSet<>(reqVO.getIds());
        List<SampleAnalysisDO> list = sampleAnalysisMapper.selectBatchIds(uniqueIds);
        if (list == null || list.size() != uniqueIds.size()) {
            throw exception(SAMPLE_ANALYSIS_NOT_EXISTS);
        }

        // 2. 批量更新付款状态（仅影响所选 id）
        LambdaUpdateWrapper<SampleAnalysisDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(SampleAnalysisDO::getId, uniqueIds)
                .set(SampleAnalysisDO::getIsPaid, reqVO.getIsPaid());
        sampleAnalysisMapper.update(null, updateWrapper);
    }


    // ==================== 辅助计算与私有方法 ====================

    /**
     * 计算米长：100000 / (width * weight)，四舍五入保留两位小数。
     * 门幅/克重为空、0 或负数时返回 null。
     */
    public BigDecimal calculateMeterLength(BigDecimal width, BigDecimal weight) {
        if (width == null || weight == null) {
            return null;
        }
        if (width.compareTo(BigDecimal.ZERO) <= 0 || weight.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal denominator = width.multiply(weight);
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return HUNDRED_THOUSAND.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * 校验客户并解析客户名称快照：
     * customerId 存在时档案必须存在（抛 1_030_018_002），若未传 clientName 则以档案公司名兜底快照；
     * customerId 为空时直接返回手填的 clientName。
     */
    private String validateCustomerAndResolveClientName(Long customerId, String clientName) {
        if (customerId != null) {
            CustomerDO customer = customerMapper.selectById(customerId);
            if (customer == null) {
                throw exception(SAMPLE_ANALYSIS_REF_NOT_EXISTS);
            }
            if (!StringUtils.hasText(clientName)) {
                return customer.getCompanyName();
            }
        }
        return clientName;
    }

    /**
     * 校验样品编号唯一（包含已逻辑删除的行，ADR-0036/ADR-0004）
     */
    private void validateSampleAnalysisNoUnique(Long excludeId, String sampleAnalysisNo) {
        if (!StringUtils.hasText(sampleAnalysisNo)) {
            return;
        }
        SampleAnalysisDO exist = sampleAnalysisMapper.selectByNoIncludingDeleted(sampleAnalysisNo);
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw exception(SAMPLE_ANALYSIS_NO_DUPLICATE, sampleAnalysisNo);
        }
    }

    /**
     * 自动取号（带撞号重试）
     */
    private String generateSampleAnalysisNoWithRetry() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateSampleAnalysisNo();
            if (sampleAnalysisMapper.selectByNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(SAMPLE_ANALYSIS_NO_DUPLICATE, "自动取号冲突，请手填编号重试");
    }

    /**
     * 生成下一个样品编号：SA + 公历年 + 4 位流水号（超 9999 自然扩位）。
     * 扫描同租户（含软删行）获取最大后缀数字 + 1。
     */
    private String generateSampleAnalysisNo() {
        String prefix = SAMPLE_ANALYSIS_NO_PREFIX + LocalDate.now().getYear();
        List<SampleAnalysisDO> candidates = sampleAnalysisMapper.selectListByNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        for (SampleAnalysisDO candidate : candidates) {
            String no = candidate.getSampleAnalysisNo();
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
    }

    /**
     * 校验样品分析记录存在
     */
    private SampleAnalysisDO validateSampleAnalysisExists(Long id) {
        if (id == null) {
            throw exception(SAMPLE_ANALYSIS_NOT_EXISTS);
        }
        SampleAnalysisDO sampleDO = sampleAnalysisMapper.selectById(id);
        if (sampleDO == null) {
            throw exception(SAMPLE_ANALYSIS_NOT_EXISTS);
        }
        return sampleDO;
    }

    /**
     * 获取用户昵称快照
     */
    private String getUserNickname(Long userId) {
        if (userId == null) {
            return SecurityFrameworkUtils.getLoginUserNickname();
        }
        try {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            if (user != null && StringUtils.hasText(user.getNickname())) {
                return user.getNickname();
            }
        } catch (Exception ignore) {
            // 系统间或单元测试环境中如果 adminUserApi 调用失败，降级为 SecurityFrameworkUtils
        }
        return SecurityFrameworkUtils.getLoginUserNickname();
    }

    /**
     * 构造分页查询条件：
     * isPaid 等值过滤；
     * keyword 模糊匹配：sample_analysis_no, client_name, product_name, client_style_no, composition
     */
    private LambdaQueryWrapperX<SampleAnalysisDO> buildPageWrapper(SampleAnalysisPageReqVO reqVO) {
        LambdaQueryWrapperX<SampleAnalysisDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(SampleAnalysisDO::getIsPaid, reqVO.getIsPaid());

        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w
                    .apply("LOWER(sample_analysis_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(client_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(client_style_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(composition) LIKE LOWER({0})", like));
        }
        return wrapper;
    }

    /**
     * 校验样品照片数量（不得超过 9 张）
     */
    private void validatePhotos(List<String> photos) {
        if (photos != null && photos.size() > MAX_PHOTOS_COUNT) {
            throw exception(SAMPLE_ANALYSIS_PHOTO_COUNT_EXCEED);
        }
    }

    /**
     * 填充第一张照片 URL（用于列表展示与封面图）
     */
    private void populateFirstPhotoUrl(SampleAnalysisRespVO respVO) {
        if (respVO != null && respVO.getPhotos() != null && !respVO.getPhotos().isEmpty()) {
            respVO.setFirstPhotoUrl(respVO.getPhotos().get(0));
        }
    }

    /**
     * 校验三角排列画板数据合法性（契约：rows=4, data 4×columns, 符号仅允许空串/v/-/+/=）
     */
    private void validateStitchArrange(SampleAnalysisStitchArrangeVO stitchArrange) {
        if (stitchArrange == null) {
            return;
        }
        // 1. rows 必须为 4
        if (stitchArrange.getRows() == null || stitchArrange.getRows() != 4) {
            throw exception(SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID, "画板行数必须为 4");
        }
        // 2. columns 必须 >= 0
        if (stitchArrange.getColumns() == null || stitchArrange.getColumns() < 0) {
            throw exception(SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID, "画板列数非法");
        }
        // 3. data 维数不符
        List<List<String>> data = stitchArrange.getData();
        if (data == null || data.size() != 4) {
            throw exception(SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID, "画板数据行数必须为 4");
        }
        int columns = stitchArrange.getColumns();
        for (int r = 0; r < 4; r++) {
            List<String> row = data.get(r);
            if (row == null || row.size() != columns) {
                throw exception(SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID, "第 " + (r + 1) + " 行列数与 columns 不一致");
            }
        }
        // 4. 符号只允许五态：空串/v/-/+/=
        for (List<String> row : data) {
            for (String cell : row) {
                if (cell == null || !ALLOWED_STITCH_SYMBOLS.contains(cell)) {
                    throw exception(SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID, "包含非法符号：" + cell);
                }
            }
        }
    }

    /**
     * 计算总成本（服务端权威计算）：
     * (纱线成本 + 生产工费 + 染整费 + 其他费用) ÷ (1 − 损耗率/100)，四舍五入保留2位小数。
     * 费用项留空按 0 计算；损耗率必须 0 ≤ lossRate < 100，否则抛出 SAMPLE_ANALYSIS_LOSS_RATE_INVALID (003)。
     * 供应商报价与对外报价永远不参与总成本计算。
     */
    public BigDecimal calculateTotalCost(BigDecimal yarnCost, BigDecimal productionCost, BigDecimal dyeingCost, BigDecimal otherCost, BigDecimal lossRate) {
        if (lossRate != null) {
            if (lossRate.compareTo(BigDecimal.ZERO) < 0 || lossRate.compareTo(new BigDecimal("100")) >= 0) {
                throw exception(SAMPLE_ANALYSIS_LOSS_RATE_INVALID);
            }
        }
        if (yarnCost == null && productionCost == null && dyeingCost == null && otherCost == null && lossRate == null) {
            return null;
        }
        BigDecimal yCost = yarnCost != null ? yarnCost : BigDecimal.ZERO;
        BigDecimal pCost = productionCost != null ? productionCost : BigDecimal.ZERO;
        BigDecimal dCost = dyeingCost != null ? dyeingCost : BigDecimal.ZERO;
        BigDecimal oCost = otherCost != null ? otherCost : BigDecimal.ZERO;
        BigDecimal sumCost = yCost.add(pCost).add(dCost).add(oCost);

        if (lossRate == null || lossRate.compareTo(BigDecimal.ZERO) == 0) {
            return sumCost.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.subtract(lossRate.divide(HUNDRED, 8, RoundingMode.HALF_UP));
        return sumCost.divide(factor, 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算纱线成本（服务端权威计算）：
     * Σ（明细单价 × 比例 ÷ 100），明细单价 >1000 时自动 ÷1000（防吨价误填）。
     * 若未提供规格明细，返回 null。
     */
    public BigDecimal calculateYarnCost(List<SampleAnalysisYarnSpecSaveReqVO> yarnSpecs) {
        if (yarnSpecs == null || yarnSpecs.isEmpty()) {
            return null;
        }
        BigDecimal yarnCost = BigDecimal.ZERO;
        for (SampleAnalysisYarnSpecSaveReqVO item : yarnSpecs) {
            if (item == null) {
                continue;
            }
            if (item.getYarnSpecId() == null) {
                throw exception(SAMPLE_ANALYSIS_YARN_SPEC_NOT_EXISTS);
            }
            BigDecimal price = item.getUnitPrice();
            BigDecimal ratio = item.getRatio();
            if (price != null && ratio != null) {
                // 明细单价 >1000 时自动 ÷1000（防吨价误填）
                if (price.compareTo(THOUSAND) > 0) {
                    price = price.divide(THOUSAND, 4, RoundingMode.HALF_UP);
                }
                BigDecimal rowCost = price.multiply(ratio).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                yarnCost = yarnCost.add(rowCost);
            }
        }
        return yarnCost;
    }

    /**
     * 新建时保存纱线规格明细子表（ADR-0036 票03）
     */
    private void saveYarnSpecsOnCreate(Long sampleAnalysisId, List<SampleAnalysisYarnSpecSaveReqVO> yarnSpecs) {
        if (yarnSpecs == null || yarnSpecs.isEmpty()) {
            return;
        }
        for (SampleAnalysisYarnSpecSaveReqVO vo : yarnSpecs) {
            if (vo == null) {
                continue;
            }
            if (vo.getYarnSpecId() == null) {
                throw exception(SAMPLE_ANALYSIS_YARN_SPEC_NOT_EXISTS);
            }
            YarnSpecDO specDO = yarnSpecMapper.selectById(vo.getYarnSpecId());
            if (specDO == null) {
                throw exception(SAMPLE_ANALYSIS_YARN_SPEC_NOT_EXISTS);
            }
            // 停用规格不拦截（specDO.getStatus() 是 ACTIVE 还是 INACTIVE 均可保存）
            SampleAnalysisYarnSpecDO itemDO = BeanUtils.toBean(vo, SampleAnalysisYarnSpecDO.class);
            itemDO.setId(null);
            itemDO.setSampleAnalysisId(sampleAnalysisId);
            populateYarnSpecSnapshots(itemDO, specDO, vo);
            sampleAnalysisYarnSpecMapper.insert(itemDO);
        }
    }

    /**
     * 更新时按行 ID 差异保存纱线规格明细子表（ADR-0032、ADR-0036 票03）
     */
    private void diffSaveYarnSpecs(Long sampleAnalysisId, List<SampleAnalysisYarnSpecSaveReqVO> specVOs) {
        List<SampleAnalysisYarnSpecDO> existingList = sampleAnalysisYarnSpecMapper.selectListBySampleAnalysisId(sampleAnalysisId);
        Map<Long, SampleAnalysisYarnSpecDO> existingMap = existingList.stream()
                .collect(Collectors.toMap(SampleAnalysisYarnSpecDO::getId, Function.identity()));
        Set<Long> existingIds = existingMap.keySet();

        if (specVOs == null || specVOs.isEmpty()) {
            if (!existingIds.isEmpty()) {
                sampleAnalysisYarnSpecMapper.deleteByIds(existingIds);
            }
            return;
        }

        List<SampleAnalysisYarnSpecDO> toInsert = new ArrayList<>();
        List<SampleAnalysisYarnSpecDO> toUpdate = new ArrayList<>();
        Set<Long> submittedIds = new HashSet<>();

        for (SampleAnalysisYarnSpecSaveReqVO vo : specVOs) {
            if (vo == null) {
                continue;
            }
            if (vo.getYarnSpecId() == null) {
                throw exception(SAMPLE_ANALYSIS_YARN_SPEC_NOT_EXISTS);
            }
            YarnSpecDO specDO = yarnSpecMapper.selectById(vo.getYarnSpecId());
            boolean isExistingRow = vo.getId() != null && existingMap.containsKey(vo.getId());
            // 如果规格不存在且不是已有历史行，报错
            if (specDO == null && !isExistingRow) {
                throw exception(SAMPLE_ANALYSIS_YARN_SPEC_NOT_EXISTS);
            }

            SampleAnalysisYarnSpecDO itemDO = BeanUtils.toBean(vo, SampleAnalysisYarnSpecDO.class);
            itemDO.setSampleAnalysisId(sampleAnalysisId);

            if (specDO != null) {
                populateYarnSpecSnapshots(itemDO, specDO, vo);
            } else {
                // 历史规格已被软删，继承存量快照
                SampleAnalysisYarnSpecDO oldItem = existingMap.get(vo.getId());
                if (oldItem != null) {
                    if (!StringUtils.hasText(itemDO.getSpecName())) itemDO.setSpecName(oldItem.getSpecName());
                    if (!StringUtils.hasText(itemDO.getSpinMethod())) itemDO.setSpinMethod(oldItem.getSpinMethod());
                    if (itemDO.getYarnTypeId() == null) itemDO.setYarnTypeId(oldItem.getYarnTypeId());
                    if (!StringUtils.hasText(itemDO.getYarnTypeName())) itemDO.setYarnTypeName(oldItem.getYarnTypeName());
                }
            }

            if (isExistingRow) {
                toUpdate.add(itemDO);
                submittedIds.add(vo.getId());
            } else {
                itemDO.setId(null);
                toInsert.add(itemDO);
            }
        }

        // 逻辑删除未提交的已有行
        Set<Long> toDelete = new HashSet<>(existingIds);
        toDelete.removeAll(submittedIds);
        if (!toDelete.isEmpty()) {
            sampleAnalysisYarnSpecMapper.deleteByIds(toDelete);
        }

        // 更新已有行
        for (SampleAnalysisYarnSpecDO item : toUpdate) {
            sampleAnalysisYarnSpecMapper.updateById(item);
        }

        // 批量插入新行
        for (SampleAnalysisYarnSpecDO item : toInsert) {
            sampleAnalysisYarnSpecMapper.insert(item);
        }
    }

    /**
     * 填充纱线规格及材质快照
     */
    private void populateYarnSpecSnapshots(SampleAnalysisYarnSpecDO itemDO, YarnSpecDO specDO, SampleAnalysisYarnSpecSaveReqVO vo) {
        if (specDO != null) {
            itemDO.setSpecName(specDO.getSpecName());
            itemDO.setSpinMethod(specDO.getSpinMethod());
            itemDO.setYarnTypeId(specDO.getYarnTypeId());
            if (specDO.getYarnTypeId() != null) {
                YarnTypeDO yarnType = yarnTypeMapper.selectById(specDO.getYarnTypeId());
                if (yarnType != null) {
                    itemDO.setYarnTypeName(yarnType.getName());
                }
            }
        }
    }

    /**
     * 新建时保存供应商报价子表
     */
    private void saveSupplierQuotesOnCreate(Long sampleAnalysisId, List<SampleAnalysisSupplierQuoteSaveReqVO> supplierQuotes) {
        if (supplierQuotes == null || supplierQuotes.isEmpty()) {
            return;
        }
        for (SampleAnalysisSupplierQuoteSaveReqVO item : supplierQuotes) {
            if (item == null) {
                continue;
            }
            if (item.getSupplierId() != null) {
                SupplierDO supplier = supplierMapper.selectById(item.getSupplierId());
                if (supplier == null) {
                    throw exception(SAMPLE_ANALYSIS_REF_NOT_EXISTS);
                }
                if (!StringUtils.hasText(item.getSupplierName())) {
                    item.setSupplierName(supplier.getCompanyName());
                }
            }
            SampleAnalysisSupplierQuoteDO quoteDO = BeanUtils.toBean(item, SampleAnalysisSupplierQuoteDO.class);
            quoteDO.setSampleAnalysisId(sampleAnalysisId);
            quoteDO.setId(null);
            sampleAnalysisSupplierQuoteMapper.insert(quoteDO);
        }
    }

    /**
     * 差异保存供应商报价子表（ADR-0032）：
     * - 带 id 且属于本记录存量 -> update
     * - 无 id -> insert
     * - 库内存量未提交 -> delete
     * - supplierId 校验：历史行未修改 supplierId 放行（即使已停用/已软删不拦截）；新增或修改 supplierId 且非空则查档案，不存在抛 002
     */
    private void diffSaveSupplierQuotes(Long sampleAnalysisId, List<SampleAnalysisSupplierQuoteSaveReqVO> reqQuotes) {
        List<SampleAnalysisSupplierQuoteDO> dbQuotes = sampleAnalysisSupplierQuoteMapper.selectListBySampleAnalysisId(sampleAnalysisId);
        Map<Long, SampleAnalysisSupplierQuoteDO> dbQuoteMap = dbQuotes.stream()
                .collect(Collectors.toMap(SampleAnalysisSupplierQuoteDO::getId, Function.identity()));
        Set<Long> remainingDbIds = new HashSet<>(dbQuoteMap.keySet());

        List<SampleAnalysisSupplierQuoteDO> toInsert = new ArrayList<>();
        List<SampleAnalysisSupplierQuoteDO> toUpdate = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        if (reqQuotes != null) {
            for (SampleAnalysisSupplierQuoteSaveReqVO item : reqQuotes) {
                if (item == null) {
                    continue;
                }
                if (item.getId() != null) {
                    Long quoteId = item.getId();
                    if (!seenIds.add(quoteId)) {
                        throw invalidParamException("供应商报价明细行 ID【{}】重复提交", quoteId);
                    }
                    if (!remainingDbIds.remove(quoteId)) {
                        throw invalidParamException("供应商报价明细行 ID【{}】不属于当前样品分析", quoteId);
                    }
                    SampleAnalysisSupplierQuoteDO dbItem = dbQuoteMap.get(quoteId);
                    if (item.getSupplierId() != null && !Objects.equals(item.getSupplierId(), dbItem.getSupplierId())) {
                        SupplierDO supplier = supplierMapper.selectById(item.getSupplierId());
                        if (supplier == null) {
                            throw exception(SAMPLE_ANALYSIS_REF_NOT_EXISTS);
                        }
                        if (!StringUtils.hasText(item.getSupplierName())) {
                            item.setSupplierName(supplier.getCompanyName());
                        }
                    } else if (item.getSupplierId() != null && Objects.equals(item.getSupplierId(), dbItem.getSupplierId())) {
                        // 历史行相同 supplierId，即使供应商档案停用/删除也不拦截
                        if (!StringUtils.hasText(item.getSupplierName())) {
                            item.setSupplierName(dbItem.getSupplierName());
                        }
                    }
                    SampleAnalysisSupplierQuoteDO updateDO = BeanUtils.toBean(item, SampleAnalysisSupplierQuoteDO.class);
                    updateDO.setSampleAnalysisId(sampleAnalysisId);
                    toUpdate.add(updateDO);
                } else {
                    // 新增行
                    if (item.getSupplierId() != null) {
                        SupplierDO supplier = supplierMapper.selectById(item.getSupplierId());
                        if (supplier == null) {
                            throw exception(SAMPLE_ANALYSIS_REF_NOT_EXISTS);
                        }
                        if (!StringUtils.hasText(item.getSupplierName())) {
                            item.setSupplierName(supplier.getCompanyName());
                        }
                    }
                    SampleAnalysisSupplierQuoteDO insertDO = BeanUtils.toBean(item, SampleAnalysisSupplierQuoteDO.class);
                    insertDO.setSampleAnalysisId(sampleAnalysisId);
                    insertDO.setId(null);
                    toInsert.add(insertDO);
                }
            }
        }

        for (SampleAnalysisSupplierQuoteDO insertDO : toInsert) {
            sampleAnalysisSupplierQuoteMapper.insert(insertDO);
        }
        for (SampleAnalysisSupplierQuoteDO updateDO : toUpdate) {
            sampleAnalysisSupplierQuoteMapper.updateById(updateDO);
        }
        if (!remainingDbIds.isEmpty()) {
            for (Long deleteId : remainingDbIds) {
                sampleAnalysisSupplierQuoteMapper.deleteById(deleteId);
            }
        }
    }

    /**
     * 创建时保存绕纱行子表（自由文本行，全部留空也允许保存，按插入顺序展示）
     */
    private void saveYarnArrangesOnCreate(Long sampleAnalysisId, List<SampleAnalysisYarnArrangeSaveReqVO> reqArranges) {
        if (reqArranges == null || reqArranges.isEmpty()) {
            return;
        }
        for (SampleAnalysisYarnArrangeSaveReqVO item : reqArranges) {
            if (item == null) {
                continue;
            }
            SampleAnalysisYarnArrangeDO arrangeDO = BeanUtils.toBean(item, SampleAnalysisYarnArrangeDO.class);
            arrangeDO.setId(null);
            arrangeDO.setSampleAnalysisId(sampleAnalysisId);
            sampleAnalysisYarnArrangeMapper.insert(arrangeDO);
        }
    }

    /**
     * 差异保存绕纱行子表（ADR-0032 / ADR-0036）：
     * - 带 id 且属于本记录存量 -> update
     * - 无 id -> insert
     * - 库内存量未提交 -> deleteByIds
     */
    private void diffSaveYarnArranges(Long sampleAnalysisId, List<SampleAnalysisYarnArrangeSaveReqVO> reqArranges) {
        if (reqArranges == null) {
            return;
        }
        List<SampleAnalysisYarnArrangeDO> dbArranges = sampleAnalysisYarnArrangeMapper.selectListBySampleAnalysisId(sampleAnalysisId);
        Map<Long, SampleAnalysisYarnArrangeDO> dbArrangeMap = dbArranges.stream()
                .collect(Collectors.toMap(SampleAnalysisYarnArrangeDO::getId, Function.identity()));
        Set<Long> remainingDbIds = new HashSet<>(dbArrangeMap.keySet());

        List<SampleAnalysisYarnArrangeDO> toInsert = new ArrayList<>();
        List<SampleAnalysisYarnArrangeDO> toUpdate = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (SampleAnalysisYarnArrangeSaveReqVO item : reqArranges) {
                if (item == null) {
                    continue;
                }
                if (item.getId() != null) {
                    Long arrangeId = item.getId();
                    if (!seenIds.add(arrangeId)) {
                        throw invalidParamException("绕纱行 ID【{}】重复提交", arrangeId);
                    }
                    if (!remainingDbIds.remove(arrangeId)) {
                        throw invalidParamException("绕纱行 ID【{}】不属于当前样品分析", arrangeId);
                    }
                    SampleAnalysisYarnArrangeDO updateDO = BeanUtils.toBean(item, SampleAnalysisYarnArrangeDO.class);
                    updateDO.setSampleAnalysisId(sampleAnalysisId);
                    toUpdate.add(updateDO);
                } else {
                    SampleAnalysisYarnArrangeDO insertDO = BeanUtils.toBean(item, SampleAnalysisYarnArrangeDO.class);
                    insertDO.setSampleAnalysisId(sampleAnalysisId);
                    insertDO.setId(null);
                    toInsert.add(insertDO);
                }
            }

        // 3. 执行删除（未被本次提交命中的存量 ID）
        if (!remainingDbIds.isEmpty()) {
            sampleAnalysisYarnArrangeMapper.deleteByIds(remainingDbIds);
        }

        // 4. 执行更新
        for (SampleAnalysisYarnArrangeDO updateDO : toUpdate) {
            sampleAnalysisYarnArrangeMapper.updateById(updateDO);
        }

        // 5. 执行插入
        for (SampleAnalysisYarnArrangeDO insertDO : toInsert) {
            sampleAnalysisYarnArrangeMapper.insert(insertDO);
        }
    }

}
