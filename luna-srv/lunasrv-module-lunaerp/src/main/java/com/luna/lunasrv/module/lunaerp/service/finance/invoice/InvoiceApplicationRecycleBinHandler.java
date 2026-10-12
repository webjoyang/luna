package com.luna.lunasrv.module.lunaerp.service.finance.invoice;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.util.MyBatisUtils;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.RecycleBinHandler;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice.InvoiceApplicationMapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;

/**
 * 发票申请草稿回收站业务处理器（ADR-0038 / W0 Spec）
 *
 * 仅 DRAFT 状态发票申请进入回收站。
 * 恢复为 DRAFT 状态，恢复不补账。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class InvoiceApplicationRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "INVOICE_APPLICATION";

    @Resource
    private InvoiceApplicationMapper invoiceApplicationMapper;

    @Resource
    private InvoiceApplicationItemMapper invoiceApplicationItemMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<RecycleItemRespVO> pageDeleted(RecyclePageReqVO req) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        IPage<RecycleItemRespVO> mpPage = MyBatisUtils.buildPage(req);
        invoiceApplicationMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        InvoiceApplicationDO app = invoiceApplicationMapper.selectDeletedById(id, tenantId);
        if (app == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 客户校验
        Long customerId = app.getCustomerId();
        if (customerId != null) {
            CustomerDO deletedCustomer = customerMapper.selectDeletedById(customerId, tenantId);
            if (deletedCustomer != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedCustomer.getCompanyName());
            }
            CustomerDO activeCustomer = customerMapper.selectById(customerId);
            if (activeCustomer == null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "客户");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复草稿发票申请【{{#id}}】")
    public void restore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 恢复前校验存在且在回收站中
        validateRestore(id);

        // 2. 并发条件更新主数据（deleted=0）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String updater = loginUserId != null ? String.valueOf(loginUserId) : "1";
        LocalDateTime now = LocalDateTime.now();

        int affected = invoiceApplicationMapper.restoreInvoiceApplication(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[InvoiceApplicationRecycleBinHandler] 草稿发票申请已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        // 草稿状态发票申请无其他单据强引用
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除草稿发票申请【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 检查是否存在于回收站
        InvoiceApplicationDO app = invoiceApplicationMapper.selectDeletedById(id, tenantId);
        if (app == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 2. 物理删除主表
        int affected = invoiceApplicationMapper.purgeInvoiceApplication(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[InvoiceApplicationRecycleBinHandler] 草稿发票申请已彻底删除: id={}, tenantId={}", id, tenantId);
    }

}
