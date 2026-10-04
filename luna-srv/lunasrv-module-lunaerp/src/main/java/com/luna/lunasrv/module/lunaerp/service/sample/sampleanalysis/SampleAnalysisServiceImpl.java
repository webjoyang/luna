package com.luna.lunasrv.module.lunaerp.service.sample.sampleanalysis;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis.SampleAnalysisMapper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
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

    @Resource
    private SampleAnalysisMapper sampleAnalysisMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSampleAnalysis(SampleAnalysisSaveReqVO createReqVO) {
        // 0. 必填项校验
        if (!StringUtils.hasText(createReqVO.getProductName())) {
            throw exception(SAMPLE_ANALYSIS_PRODUCT_NAME_EMPTY);
        }

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

        // 6. 落库
        sampleAnalysisMapper.insert(sampleDO);
        return sampleDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSampleAnalysis(SampleAnalysisSaveReqVO updateReqVO) {
        // 0. 必填项校验
        if (!StringUtils.hasText(updateReqVO.getProductName())) {
            throw exception(SAMPLE_ANALYSIS_PRODUCT_NAME_EMPTY);
        }

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
        // 明确不修改创建时记录的登记人快照与付款状态
        updateDO.setCreatorId(exist.getCreatorId());
        updateDO.setCreatorName(exist.getCreatorName());
        updateDO.setIsPaid(exist.getIsPaid());

        sampleAnalysisMapper.updateById(updateDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSampleAnalysis(Long id) {
        // 校验存在
        validateSampleAnalysisExists(id);
        // 逻辑删除主表（编号不释放、不重用）
        sampleAnalysisMapper.deleteById(id);
    }

    @Override
    public SampleAnalysisRespVO getSampleAnalysis(Long id) {
        SampleAnalysisDO sampleDO = validateSampleAnalysisExists(id);
        SampleAnalysisRespVO respVO = BeanUtils.toBean(sampleDO, SampleAnalysisRespVO.class);
        if (respVO.getPhotos() != null && !respVO.getPhotos().isEmpty()) {
            respVO.setFirstPhotoUrl(respVO.getPhotos().get(0));
        }
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
                if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                    item.setFirstPhotoUrl(item.getPhotos().get(0));
                }
            }
        }
        return respPage;
    }

    @Override
    public String getNextSampleAnalysisNo() {
        // 只读预览、不占号：扫描同租户存量（含软删行），返回下一个建议编号
        return generateSampleAnalysisNo();
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

}
