package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

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
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
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

/**
 * 染色单草稿回收站业务处理器（ADR-0038 / W0 Spec）
 *
 * 仅 DRAFT 状态染色单进入回收站。
 * 恢复为 DRAFT 状态，并级联复活颜色明细子表（父 -> 子顺序）。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class DyeingOrderRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "DYEING_ORDER";

    @Resource
    private DyeingOrderMapper dyeingOrderMapper;

    @Resource
    private DyeingColorMapper dyeingColorMapper;

    @Resource
    private SupplierMapper supplierMapper;

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
        dyeingOrderMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DyeingOrderDO order = dyeingOrderMapper.selectDeletedById(id, tenantId);
        if (order == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 1. 校验承染方供应商
        Long dyeingFactoryId = order.getDyeingFactoryId();
        if (dyeingFactoryId != null) {
            SupplierDO deletedSupplier = supplierMapper.selectDeletedById(dyeingFactoryId, tenantId);
            if (deletedSupplier != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedSupplier.getCompanyName());
            }
            SupplierDO activeSupplier = supplierMapper.selectById(dyeingFactoryId);
            if (activeSupplier == null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "染厂供应商");
            }
        }

        // 2. 校验客户档案（若有）
        Long customerId = order.getCustomerId();
        if (customerId != null) {
            CustomerDO deletedCustomer = customerMapper.selectDeletedById(customerId, tenantId);
            if (deletedCustomer != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedCustomer.getCompanyName());
            }
            CustomerDO activeCustomer = customerMapper.selectById(customerId);
            if (activeCustomer == null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "客户档案");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复草稿染色单【{{#id}}】")
    public void restore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 恢复前校验存在且在回收站中
        validateRestore(id);

        // 2. 并发条件更新主数据（deleted=0, status=DRAFT）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String updater = loginUserId != null ? String.valueOf(loginUserId) : "1";
        LocalDateTime now = LocalDateTime.now();

        int affected = dyeingOrderMapper.restoreDyeingOrder(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 3. 父 -> 子顺序级联复活颜色明细子表
        dyeingColorMapper.restoreByDyeingOrderId(id, tenantId, updater, now);
        log.info("[DyeingOrderRecycleBinHandler] 草稿染色单已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        if (id == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long count = dyeingOrderMapper.selectActiveReferenceCount(id, tenantId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除草稿染色单【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 检查是否存在于回收站
        DyeingOrderDO order = dyeingOrderMapper.selectDeletedById(id, tenantId);
        if (order == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 2. 引用守卫：存在活跃引用时禁止彻底删除
        if (hasActiveReference(id)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }

        // 3. 子 -> 父顺序彻底删除：先删除颜色明细
        dyeingColorMapper.purgeByDyeingOrderId(id, tenantId);

        // 4. 物理删除主表
        int affected = dyeingOrderMapper.purgeDyeingOrder(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[DyeingOrderRecycleBinHandler] 草稿染色单已彻底删除: id={}, tenantId={}", id, tenantId);
    }

}
