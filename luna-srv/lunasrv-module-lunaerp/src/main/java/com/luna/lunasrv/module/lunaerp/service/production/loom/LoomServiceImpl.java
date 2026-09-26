package com.luna.lunasrv.module.lunaerp.service.production.loom;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom.LoomDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.loom.LoomMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理 - 织机档案 Service 实现类
 *
 * 编号生成沿用 LM+年份+4位序号格式，扫描与查重范围包含已删除行（ADR-0005, ADR-0018）。
 * 删除守卫：
 * 1. 被机台任务引用时禁止删除，引导改用停用（1_030_015_202）
 * 2. 停用织机不可被新机台任务选择，历史快照不变
 *
 * @author luna
 */
@Service
@Validated
public class LoomServiceImpl implements LoomService {

    /**
     * 织机编号前缀
     */
    private static final String LOOM_NO_PREFIX = "LM";
    /**
     * 编号数字部分最小补零位数（超 9999 自然扩位）
     */
    private static final int NO_MIN_DIGITS = 4;
    /**
     * 自动取号撞唯一索引时的最大顺延重试次数
     */
    private static final int MAX_GENERATE_RETRY = 3;
    /**
     * 编号前缀（LM+年份）后的数字部分正则
     */
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");

    /**
     * 织机默认状态
     */
    private static final String DEFAULT_STATUS = "ACTIVE";
    /**
     * 停用状态
     */
    private static final String STATUS_INACTIVE = "INACTIVE";

    @Resource
    private LoomMapper loomMapper;

    @Autowired(required = false)
    private LoomTaskReferenceProvider loomTaskReferenceProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLoom(LoomSaveReqVO createReqVO) {
        LoomDO loom = BeanUtils.toBean(createReqVO, LoomDO.class);
        // 1. 机台号：手填查重（含已删除行，撞号报错不顺延）；留空自动生成（撞号有限次顺延重试）
        if (StringUtils.hasText(loom.getLoomNo())) {
            validateLoomNoUnique(null, loom.getLoomNo());
        } else {
            loom.setLoomNo(generateLoomNoWithRetry());
        }
        // 2. 状态缺省 ACTIVE
        if (!StringUtils.hasText(loom.getStatus())) {
            loom.setStatus(DEFAULT_STATUS);
        }
        loomMapper.insert(loom);
        return loom.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLoom(LoomSaveReqVO updateReqVO) {
        validateLoomExists(updateReqVO.getId());
        LoomDO updateObj = BeanUtils.toBean(updateReqVO, LoomDO.class);
        // 机台号变更时查重（排除自身，含已删除行）
        if (StringUtils.hasText(updateObj.getLoomNo())) {
            validateLoomNoUnique(updateObj.getId(), updateObj.getLoomNo());
        } else {
            updateObj.setLoomNo(null);
        }
        loomMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLoom(Long id) {
        validateLoomExists(id);

        // 删除守卫：被机台任务引用时禁止删除，引导改用停用
        if (loomTaskReferenceProvider != null && loomTaskReferenceProvider.isLoomReferencedByTask(id)) {
            throw exception(LOOM_REFERENCED_BY_MACHINE_TASK);
        }

        // 先置 status='INACTIVE'，再逻辑删除
        LoomDO updateObj = new LoomDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_INACTIVE);
        loomMapper.updateById(updateObj);

        loomMapper.deleteById(id);
    }

    @Override
    public LoomRespVO getLoom(Long id) {
        LoomDO loom = loomMapper.selectById(id);
        if (loom == null) {
            throw exception(LOOM_NOT_EXISTS);
        }
        return BeanUtils.toBean(loom, LoomRespVO.class);
    }

    @Override
    public PageResult<LoomRespVO> getLoomPage(LoomPageReqVO pageReqVO) {
        PageResult<LoomDO> pageResult = loomMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, LoomRespVO.class);
    }

    @Override
    public List<LoomRespVO> getLoomListAll(String status) {
        String queryStatus = StringUtils.hasText(status) ? status : DEFAULT_STATUS;
        List<LoomDO> list = loomMapper.selectList(new LambdaQueryWrapperX<LoomDO>()
                .eq(LoomDO::getStatus, queryStatus)
                .orderByAsc(LoomDO::getLoomNo));
        return BeanUtils.toBean(list, LoomRespVO.class);
    }

    @Override
    public String getNextLoomNo() {
        return generateLoomNo();
    }

    // ==================== 私有方法 ====================

    private void validateLoomExists(Long id) {
        if (id == null || loomMapper.selectById(id) == null) {
            throw exception(LOOM_NOT_EXISTS);
        }
    }

    private void validateLoomNoUnique(Long excludeId, String loomNo) {
        if (!StringUtils.hasText(loomNo)) {
            return;
        }
        LoomDO exist = loomMapper.selectByLoomNoIncludingDeleted(loomNo.trim());
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw exception(LOOM_NO_DUPLICATE, loomNo);
        }
    }

    private String generateLoomNoWithRetry() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateLoomNo();
            if (loomMapper.selectByLoomNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        throw exception(LOOM_NO_DUPLICATE, generateLoomNo());
    }

    private String generateLoomNo() {
        String prefix = LOOM_NO_PREFIX + LocalDate.now().getYear();
        List<LoomDO> candidates = loomMapper.selectListByLoomNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        for (LoomDO candidate : candidates) {
            String no = candidate.getLoomNo();
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

}
