package com.luna.lunasrv.module.lunaerp.service.finance.payment;

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
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment.PaymentRefMapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;

/**
 * 收付款单草稿回收站业务处理器
 *
 * 仅 approval_status=PENDING 状态单据进入回收站（ADR-0038 / spec.md#L163）。
 * 恢复为 PENDING 状态，并级联复活分配核销明细子表（恢复不补账——草稿收付款单本身不记账）。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class PaymentRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "PAYMENT";

    @Resource
    private PaymentMapper paymentMapper;

    @Resource
    private PaymentRefMapper paymentRefMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<RecycleItemRespVO> pageDeleted(RecyclePageReqVO req) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        IPage<RecycleItemRespVO> mpPage = MyBatisUtils.buildPage(req);
        paymentMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        PaymentDO payment = paymentMapper.selectDeletedById(id, tenantId);
        if (payment == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 校验关联客户（收款单方向）
        Long customerId = payment.getCustomerId();
        if (customerId != null) {
            CustomerDO deletedCustomer = customerMapper.selectDeletedById(customerId, tenantId);
            if (deletedCustomer != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedCustomer.getCompanyName());
            }
            CustomerDO activeCustomer = customerMapper.selectById(customerId);
            if (activeCustomer == null) {
                throw exception(CUSTOMER_NOT_EXISTS);
            }
        }

        // 校验关联供应商（付款单方向）
        Long supplierId = payment.getSupplierId();
        if (supplierId != null) {
            SupplierDO deletedSupplier = supplierMapper.selectDeletedById(supplierId, tenantId);
            if (deletedSupplier != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedSupplier.getCompanyName());
            }
            SupplierDO activeSupplier = supplierMapper.selectById(supplierId);
            if (activeSupplier == null) {
                throw exception(SUPPLIER_NOT_EXISTS);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复草稿收付款单【{{#id}}】")
    public void restore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 恢复前校验存在且在回收站中
        validateRestore(id);

        // 2. 并发条件更新主数据（deleted=0, approval_status=PENDING）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String updater = loginUserId != null ? String.valueOf(loginUserId) : "system";
        LocalDateTime now = LocalDateTime.now();

        int affected = paymentMapper.restorePayment(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 3. 级联复活收付款核销分配明细子表
        paymentRefMapper.restoreByPaymentId(id, tenantId, updater, now);
        log.info("[PaymentRecycleBinHandler] 草稿收付款单已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除草稿收付款单【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 彻底删除前校验引用关系
        validatePurge(id);

        // 2. 先物理删除子表
        paymentRefMapper.purgeByPaymentId(id, tenantId);

        // 3. 再物理删除主表
        int affected = paymentMapper.purgePayment(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[PaymentRecycleBinHandler] 草稿收付款单已彻底删除: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        if (id == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long count = paymentMapper.selectActiveReferenceCount(id, tenantId);
        return count != null && count > 0;
    }

    public void validatePurge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        PaymentDO payment = paymentMapper.selectDeletedById(id, tenantId);
        if (payment == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        if (hasActiveReference(id)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }
    }

}
