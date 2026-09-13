package com.luna.lunasrv.module.lunaerp.service.baseinfo.greigetype;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo.GreigeTypePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo.GreigeTypeSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.greigetype.GreigeTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.greigetype.GreigeTypeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.GreigeErrorCodeConstants.GREIGE_TYPE_CODE_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.GreigeErrorCodeConstants.GREIGE_TYPE_MATCH_KEY_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.GreigeErrorCodeConstants.GREIGE_TYPE_NOT_EXISTS;

/**
 * 胚布品种 Service 实现类
 *
 * 识别口径移植自 oldserver/src/lib/greige-type.util.ts：
 * 一个坯由「布种、成分、纱支、坯克重、坯门幅」五个属性唯一确定，
 * matchKey / displayName 的生成规则全仓唯一，查重与建档都复用本类的同一套方法。
 *
 * @author luna
 */
@Service
public class GreigeTypeServiceImpl implements GreigeTypeService {

    /**
     * 品种编号前缀
     */
    private static final String CODE_PREFIX = "GT-";
    /**
     * 品种流水号位宽（只补不截，超过四位时自然增长）
     */
    private static final int CODE_SEQUENCE_MIN_DIGITS = 4;

    @Resource
    private GreigeTypeMapper greigeTypeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGreigeType(GreigeTypeSaveReqVO createReqVO) {
        // 1. 编号：显式传入则校验唯一；不传则按租户内流水号生成 GT-XXXX
        String code = StrUtil.trim(createReqVO.getCode());
        if (StrUtil.isBlank(code)) {
            code = nextGreigeCode();
        }
        validateCodeUnique(code, null);

        // 2. 归一化五个识别属性并生成 matchKey（服务端生成，不信任前端传值），同租户查重
        String fabricType = StrUtil.trim(createReqVO.getFabricType());
        String composition = normalizeUpper(createReqVO.getComposition());
        String yarnSpec = normalizeUpper(createReqVO.getYarnSpec());
        String matchKey = buildGreigeMatchKey(fabricType, composition, yarnSpec,
                createReqVO.getWeightGsm(), createReqVO.getWidthCm());
        validateMatchKeyUnique(matchKey, null);

        // 3. 插入：名称未传时按源规则生成，传了则用传入值覆盖；状态默认 ACTIVE
        GreigeTypeDO greigeType = BeanUtils.toBean(createReqVO, GreigeTypeDO.class);
        greigeType.setCode(code);
        greigeType.setFabricType(fabricType);
        greigeType.setComposition(composition);
        greigeType.setYarnSpec(yarnSpec);
        greigeType.setMatchKey(matchKey);
        greigeType.setName(resolveName(createReqVO.getName(), fabricType, composition, yarnSpec,
                createReqVO.getWeightGsm(), createReqVO.getWidthCm()));
        greigeType.setStatus(resolveStatus(createReqVO.getStatus(), GreigeTypeDO.STATUS_ACTIVE));
        greigeTypeMapper.insert(greigeType);
        return greigeType.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGreigeType(GreigeTypeSaveReqVO updateReqVO) {
        // 1. 校验存在
        GreigeTypeDO existing = validateGreigeTypeExists(updateReqVO.getId());

        // 2. 编号：传了就改并校验唯一；不传则保持原编号（编号不随属性变化）
        String code = StrUtil.trim(updateReqVO.getCode());
        if (StrUtil.isBlank(code)) {
            code = existing.getCode();
        }
        validateCodeUnique(code, updateReqVO.getId());

        // 3. 五个识别属性任一变化则重新计算 matchKey 并查重
        String fabricType = StrUtil.trim(updateReqVO.getFabricType());
        String composition = normalizeUpper(updateReqVO.getComposition());
        String yarnSpec = normalizeUpper(updateReqVO.getYarnSpec());
        String matchKey = buildGreigeMatchKey(fabricType, composition, yarnSpec,
                updateReqVO.getWeightGsm(), updateReqVO.getWidthCm());
        if (!matchKey.equals(existing.getMatchKey())) {
            validateMatchKeyUnique(matchKey, updateReqVO.getId());
        }

        // 4. 更新：名称未传时重新生成；状态未传时保持原值
        GreigeTypeDO updateObj = BeanUtils.toBean(updateReqVO, GreigeTypeDO.class);
        updateObj.setCode(code);
        updateObj.setFabricType(fabricType);
        updateObj.setComposition(composition);
        updateObj.setYarnSpec(yarnSpec);
        updateObj.setMatchKey(matchKey);
        updateObj.setName(resolveName(updateReqVO.getName(), fabricType, composition, yarnSpec,
                updateReqVO.getWeightGsm(), updateReqVO.getWidthCm()));
        updateObj.setStatus(resolveStatus(updateReqVO.getStatus(), existing.getStatus()));
        greigeTypeMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGreigeType(Long id) {
        // 校验存在
        validateGreigeTypeExists(id);
        // 逻辑删除（deleted = 0 的部分唯一索引随之释放）
        greigeTypeMapper.deleteById(id);
    }

    @Override
    public GreigeTypeDO getGreigeType(Long id) {
        return greigeTypeMapper.selectById(id);
    }

    @Override
    public PageResult<GreigeTypeDO> getGreigeTypePage(GreigeTypePageReqVO pageReqVO) {
        return greigeTypeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<GreigeTypeDO> getActiveGreigeTypeList() {
        return greigeTypeMapper.selectListByStatus(GreigeTypeDO.STATUS_ACTIVE);
    }

    // ========== 校验 ==========

    private GreigeTypeDO validateGreigeTypeExists(Long id) {
        GreigeTypeDO greigeType = greigeTypeMapper.selectById(id);
        if (greigeType == null) {
            throw exception(GREIGE_TYPE_NOT_EXISTS);
        }
        return greigeType;
    }

    /**
     * 校验编号在当前租户内唯一。多租户与 deleted = 0 条件由插件自动追加。
     *
     * @param code 品种编号
     * @param excludeId 修改时排除自身ID，创建时传 null
     */
    private void validateCodeUnique(String code, Long excludeId) {
        GreigeTypeDO greigeType = greigeTypeMapper.selectByCode(code);
        if (greigeType != null && !greigeType.getId().equals(excludeId)) {
            throw exception(GREIGE_TYPE_CODE_DUPLICATE, code);
        }
    }

    /**
     * 校验识别键在当前租户内唯一。
     *
     * @param matchKey 识别键
     * @param excludeId 修改时排除自身ID，创建时传 null
     */
    private void validateMatchKeyUnique(String matchKey, Long excludeId) {
        GreigeTypeDO greigeType = greigeTypeMapper.selectByMatchKey(matchKey);
        if (greigeType != null && !greigeType.getId().equals(excludeId)) {
            throw exception(GREIGE_TYPE_MATCH_KEY_DUPLICATE);
        }
    }

    // ========== 胚布品种识别口径（移植自 oldserver/src/lib/greige-type.util.ts） ==========

    /**
     * 取当前租户下一个品种流水号，形如 GT-0001。
     *
     * 移植自 oldserver nextGreigeCode：不能用 orderBy code desc 取最大——code 是字符串，
     * 'GT-10000' 在字符串序里小于 'GT-9999'（首字符 '1' &lt; '9'），破四位后会取回旧号、
     * 再生成已存在的号撞唯一索引。改为取全量 code 在内存里求数值最大。
     * 品种表量级几百到几千，且只 select code 一列，开销可以忽略。
     */
    private String nextGreigeCode() {
        List<GreigeTypeDO> rows = greigeTypeMapper.selectListCode();
        long max = 0L;
        for (GreigeTypeDO row : rows) {
            // 等价 TS：parseInt(code.replace(/\D/g, ''), 10)，去掉所有非数字字符后解析
            String digits = StrUtil.blankToDefault(row.getCode(), "").replaceAll("\\D+", "");
            if (StrUtil.isBlank(digits)) {
                continue;
            }
            try {
                long n = Long.parseLong(digits);
                if (n > max) {
                    max = n;
                }
            } catch (NumberFormatException ignore) {
                // 编号超出 long 范围属异常数据，忽略不计入最大值
            }
        }
        // 等价 TS：String(max + 1).padStart(4, '0')，只补不截，超过四位位宽自然增长
        return CODE_PREFIX + String.format(Locale.ROOT, "%0" + CODE_SEQUENCE_MIN_DIGITS + "d", max + 1);
    }

    /**
     * 成分 / 纱支统一大写并去空白，避免 nr/NR、30s/30S 建出两个品种。
     */
    private String normalizeUpper(String text) {
        return StrUtil.trim(text).toUpperCase(Locale.ROOT);
    }

    /**
     * 数量归一化入键：先四舍五入保留两位小数，再去掉尾随的零。
     *
     * 等价 TS keyNum：String(Math.round(n * 100) / 100)。
     * 例如 260 → "260"、260.10 → "260.1"、260.126 → "260.13"，
     * 避免 260.10 与 260.1 被当成两个品种。BigDecimal 比较一律使用 compareTo。
     */
    private String keyNum(BigDecimal n) {
        BigDecimal rounded = n.setScale(2, RoundingMode.HALF_UP);
        if (rounded.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return rounded.stripTrailingZeros().toPlainString();
    }

    /**
     * 匹配键：布种|成分|纱支|坯克重|坯门幅，查重与自动匹配的全仓唯一口径。
     * 等价 TS buildGreigeMatchKey。
     */
    private String buildGreigeMatchKey(String fabricType, String composition, String yarnSpec,
                                       BigDecimal weightGsm, BigDecimal widthCm) {
        return String.join("|", fabricType, composition, yarnSpec,
                keyNum(weightGsm), keyNum(widthCm));
    }

    /**
     * 显示名：人读的，形如「30S NR罗马布 260g×160cm」。
     * 等价 TS buildGreigeDisplayName：
     * {@code `${yarnSpec} ${composition}${fabricType} ${weight}g×${width}cm`}
     */
    private String buildGreigeDisplayName(String fabricType, String composition, String yarnSpec,
                                          BigDecimal weightGsm, BigDecimal widthCm) {
        return yarnSpec + " " + composition + fabricType + " "
                + keyNum(weightGsm) + "g\u00D7" + keyNum(widthCm) + "cm";
    }

    /**
     * 名称解析：前端传了非空白 name 时以传入值为准（trim），否则按源规则生成。
     */
    private String resolveName(String name, String fabricType, String composition, String yarnSpec,
                               BigDecimal weightGsm, BigDecimal widthCm) {
        String trimmed = StrUtil.trim(name);
        if (StrUtil.isNotBlank(trimmed)) {
            return trimmed;
        }
        return buildGreigeDisplayName(fabricType, composition, yarnSpec, weightGsm, widthCm);
    }

    /**
     * 状态解析：传了非空白值则 trim + 大写，否则回退到默认值（创建默认 ACTIVE，修改保持原值）。
     */
    private String resolveStatus(String status, String defaultStatus) {
        String normalized = StrUtil.trimToNull(status);
        return normalized != null ? normalized.toUpperCase(Locale.ROOT) : defaultStatus;
    }

}
