package com.luna.lunasrv.module.lunaerp.service.finance.expense;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.util.MyBatisUtils;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.RecycleBinHandler;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense.ExpenseItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense.ExpenseMapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;

/**
 * 员工费用报销草稿回收站业务处理器
 *
 * 仅 PENDING/REJECTED 状态软删报销单进入回收站。
 * 恢复为 PENDING 状态（不补账——草稿报销未过账）。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ExpenseRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "EXPENSE";

    @Resource
    private ExpenseMapper expenseMapper;

    @Resource
    private ExpenseItemMapper expenseItemMapper;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<RecycleItemRespVO> pageDeleted(RecyclePageReqVO req) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        IPage<RecycleItemRespVO> mpPage = MyBatisUtils.buildPage(req);
        expenseMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ExpenseDO expense = expenseMapper.selectDeletedById(id, tenantId);
        if (expense == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复草稿报销单【{{#id}}】")
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

        int affected = expenseMapper.restoreExpense(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        expenseItemMapper.restoreExpenseItems(id, tenantId, updater, now);

        log.info("[ExpenseRecycleBinHandler] 草稿报销单已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除草稿报销单【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 物理删除前校验
        validatePurge(id);

        // 2. 物理删除明细与主表
        expenseItemMapper.purgeExpenseItems(id, tenantId);
        int affected = expenseMapper.purgeExpense(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        log.info("[ExpenseRecycleBinHandler] 草稿报销单已彻底物理删除: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        // 草稿状态报销单无下游引用单据
        return false;
    }

    public void validatePurge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ExpenseDO expense = expenseMapper.selectDeletedById(id, tenantId);
        if (expense == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
    }

}
