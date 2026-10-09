package com.luna.lunasrv.module.lunaerp.service.baseinfo.supplier;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SUPPLIER_NO_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;

/**
 * 基本信息-供应商信息 Service 实现类
 *
 * 编号生成沿用销售合同已验证的数字后缀扫描方式（不引 Redis）；
 * 取号扫描与查重范围包含已删除行，见 ADR-0005。
 *
 * @author luna
 */
@Service
public class SupplierServiceImpl implements SupplierService {

    /**
     * 供应商编号前缀
     */
    private static final String SUPPLIER_NO_PREFIX = "SUP";
    /**
     * 编号数字部分最小补零位数（超 9999 自然扩位）
     */
    private static final int NO_MIN_DIGITS = 4;
    /**
     * 自动取号撞唯一索引时的最大顺延重试次数
     */
    private static final int MAX_GENERATE_RETRY = 3;
    /**
     * 编号前缀（SUP+年份）后的数字部分
     */
    private static final Pattern NO_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");

    /**
     * 供应商默认状态
     */
    private static final String DEFAULT_STATUS = "ACTIVE";
    /**
     * 停用状态（删除时先置该状态，再逻辑删除）
     */
    private static final String STATUS_INACTIVE = "INACTIVE";

    @Resource
    private SupplierMapper supplierMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupplier(SupplierSaveReqVO createReqVO) {
        validateSupplierNotMasked(createReqVO);
        SupplierDO supplier = BeanUtils.toBean(createReqVO, SupplierDO.class);
        // 1. 编号：手填查重（含已删除行，撞号直接报错不顺延）；留空自动生成（撞号有限次顺延重试）
        if (StringUtils.hasText(supplier.getSupplierNo())) {
            validateSupplierNoUnique(null, supplier.getSupplierNo());
        } else {
            supplier.setSupplierNo(generateSupplierNoWithRetry());
        }
        // 2. shortName 为空时用 companyName 回填
        backfillShortName(supplier);
        // 3. status 缺省 ACTIVE
        if (!StringUtils.hasText(supplier.getStatus())) {
            supplier.setStatus(DEFAULT_STATUS);
        }
        supplierMapper.insert(supplier);
        return supplier.getId();
    }

    @Override
    public PageResult<SupplierRespVO> getSupplierPage(SupplierPageReqVO reqVO) {
        PageResult<SupplierDO> pageResult = supplierMapper.selectPage(reqVO);
        return BeanUtils.toBean(pageResult, SupplierRespVO.class);
    }

    @Override
    public SupplierRespVO getSupplier(Long id) {
        SupplierDO supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
        return BeanUtils.toBean(supplier, SupplierRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupplier(SupplierSaveReqVO updateReqVO) {
        // 校验存在
        validateSupplierExists(updateReqVO.getId());
        validateSupplierNotMasked(updateReqVO);
        SupplierDO updateObj = BeanUtils.toBean(updateReqVO, SupplierDO.class);
        // 编号变更时查重（排除自身，含已删除行）
        if (StringUtils.hasText(updateObj.getSupplierNo())) {
            validateSupplierNoUnique(updateObj.getId(), updateObj.getSupplierNo());
        } else {
            // 编号留空视为"不修改"：置 null 使 MyBatis-Plus 跳过该列，
            // 避免把 supplier_no 写成空串绕过编号全量唯一语义（ADR-0005）
            updateObj.setSupplierNo(null);
        }
        // shortName 为空时用 companyName 回填（与创建一致）
        backfillShortName(updateObj);
        supplierMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplier(Long id) {
        // 校验存在
        validateSupplierExists(id);
        doDelete(Collections.singletonList(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplierList(List<Long> ids) {
        doDelete(ids);
    }

    @Override
    public String getNextSupplierNo() {
        // 只读预览、不占号：直接复用自动取号的扫描结果（含已删除行，ADR-0005），
        // 不做撞号查重、不写库；无新建时多次调用返回相同值，保存后再次调用自然顺延
        return generateSupplierNo();
    }

    // ==================== 私有方法 ====================

    /**
     * shortName 为空时用 companyName 回填（create/update 共用）
     */
    private void backfillShortName(SupplierDO supplier) {
        if (!StringUtils.hasText(supplier.getShortName())) {
            supplier.setShortName(supplier.getCompanyName());
        }
    }

    /**
     * 校验供应商存在，不存在抛 1_030_005_000
     */
    private void validateSupplierExists(Long id) {
        if (id == null || supplierMapper.selectById(id) == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    /**
     * 批量逻辑删除：先置 status='INACTIVE'，再走 mapper.deleteByIds（deleted=1，与客户模块模式一致）。
     * 仅处理当前租户下存在的供应商。
     */
    private void doDelete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<SupplierDO> suppliers = supplierMapper.selectByIds(ids);
        if (CollUtil.isEmpty(suppliers)) {
            return;
        }
        // 1. 先置 status='INACTIVE'
        for (SupplierDO supplier : suppliers) {
            SupplierDO updateObj = new SupplierDO();
            updateObj.setId(supplier.getId());
            updateObj.setStatus(STATUS_INACTIVE);
            supplierMapper.updateById(updateObj);
        }
        // 2. 逻辑删除（deleted=1）
        supplierMapper.deleteByIds(ids);
    }

    /**
     * 自动取号（撞号有限次顺延重试）。
     *
     * @return 生成的供应商编号
     */
    private String generateSupplierNoWithRetry() {
        for (int i = 0; i < MAX_GENERATE_RETRY; i++) {
            String candidate = generateSupplierNo();
            // 查重含已删除行（ADR-0005）；命中说明并发取号或历史数据占用，顺延重试
            if (supplierMapper.selectBySupplierNoIncludingDeleted(candidate) == null) {
                return candidate;
            }
        }
        // 连续撞号放弃取号，提示用户手填，避免无限循环
        throw exception(SUPPLIER_NO_DUPLICATE);
    }

    /**
     * 生成供应商编号：SUP + 当前年份 + 4 位顺序号（超 9999 自然扩位）。
     * 扫描当前租户同前缀的全部供应商（含已删除行，ADR-0005），按前缀后的数字部分取 max + 1，
     * 字符串排序会取错最大值（如 SUP9 > SUP10）。
     */
    private String generateSupplierNo() {
        String prefix = SUPPLIER_NO_PREFIX + LocalDate.now().getYear();
        List<SupplierDO> candidates = supplierMapper.selectListBySupplierNoPrefixIncludingDeleted(prefix);
        long maxNum = 0L;
        int maxDigits = NO_MIN_DIGITS;
        for (SupplierDO candidate : candidates) {
            String no = candidate.getSupplierNo();
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
                // 手填的超长/异常数字后缀不参与取号比较，避免取号被单条脏数据打断
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
     * 校验供应商编号未被占用（排除 excludeId 自身），扫描范围含已删除行（ADR-0005）：
     * 手填撞号（无论目标是否已删除）一律报 SUPPLIER_NO_DUPLICATE，不顺延。
     */
    private void validateSupplierNoUnique(Long excludeId, String supplierNo) {
        if (!StringUtils.hasText(supplierNo)) {
            return;
        }
        SupplierDO exist = supplierMapper.selectBySupplierNoIncludingDeleted(supplierNo);
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw exception(SUPPLIER_NO_DUPLICATE);
        }
    }

    /**
     * 校验联系电话不含脱敏字符，写接口不接受脱敏值入库
     */
    private void validateSupplierNotMasked(SupplierSaveReqVO reqVO) {
        if (reqVO != null && reqVO.getPhone() != null && reqVO.getPhone().contains("*")) {
            throw exception(BAD_REQUEST, "联系电话不能包含脱敏字符");
        }
    }

}
