package com.luna.lunasrv.module.lunaerp.service.finance.payroll;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.util.MyBatisUtils;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.RecycleBinHandler;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollDefectRefDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollDefectRefMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollEmployeeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollLineItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll.PayrollMapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;

/**
 * 员工工资单草稿回收站业务处理器
 *
 * 仅 PENDING/REJECTED 状态软删工资单进入回收站。
 * 恢复为 PENDING 状态（不补账——草稿工资单未过账）。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class PayrollRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "PAYROLL";

    @Resource
    private PayrollMapper payrollMapper;

    @Resource
    private PayrollEmployeeMapper payrollEmployeeMapper;

    @Resource
    private PayrollLineItemMapper payrollLineItemMapper;

    @Resource
    private PayrollDefectRefMapper payrollDefectRefMapper;

    @Resource
    private PayrollDefectService payrollDefectService;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<RecycleItemRespVO> pageDeleted(RecyclePageReqVO req) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        IPage<RecycleItemRespVO> mpPage = MyBatisUtils.buildPage(req);
        payrollMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        PayrollDO payroll = payrollMapper.selectDeletedById(id, tenantId);
        if (payroll == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 恢复随之恢复次品指纹占用——07 票语义：校验次品指纹是否已被其它有效单据抢占
        List<PayrollDefectRefDO> deletedRefs = payrollDefectRefMapper.selectAllByPayrollIdIncludingDeleted(id, tenantId);
        if (CollUtil.isNotEmpty(deletedRefs)) {
            Set<Long> confirmationIds = deletedRefs.stream()
                    .map(PayrollDefectRefDO::getConfirmationId)
                    .collect(Collectors.toSet());
            payrollDefectService.validateDefectRefsAvailability(id, confirmationIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复草稿工资单【{{#id}}】")
    public void restore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 恢复前校验
        validateRestore(id);

        // 2. 恢复主从表
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String updater = loginUserId != null ? String.valueOf(loginUserId) : "1";
        LocalDateTime now = LocalDateTime.now();

        int affected = payrollMapper.restorePayroll(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        payrollEmployeeMapper.restorePayrollEmployees(id, tenantId, updater, now);
        payrollLineItemMapper.restorePayrollLineItems(id, tenantId, updater, now);
        payrollDefectRefMapper.restoreDefectRefsByPayrollId(id, tenantId, updater, now);

        log.info("[PayrollRecycleBinHandler] 草稿工资单已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除草稿工资单【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 物理删除前校验
        validatePurge(id);

        // 2. 物理删除明细与主表
        payrollDefectRefMapper.purgeDefectRefsByPayrollId(id, tenantId);
        payrollLineItemMapper.purgePayrollLineItems(id, tenantId);
        payrollEmployeeMapper.purgePayrollEmployees(id, tenantId);
        int affected = payrollMapper.purgePayroll(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        log.info("[PayrollRecycleBinHandler] 草稿工资单已彻底物理删除: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        // 草稿状态工资单无下游引用单据
        return false;
    }

    public void validatePurge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        PayrollDO payroll = payrollMapper.selectDeletedById(id, tenantId);
        if (payroll == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
    }

}
