package com.luna.lunasrv.module.lunaerp.service.production.greigeorder;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.util.MyBatisUtils;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.RecycleBinHandler;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.color.GreigeOrderColorMapper;
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
 * 胚布单草稿回收站业务处理器（ADR-0038 / W0 Spec）
 *
 * 仅 DRAFT 状态胚布单进入回收站。
 * 恢复为 DRAFT 状态，并级联复活颜色明细子表（父 -> 子顺序）。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class GreigeOrderRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "GREIGE_ORDER";

    @Resource
    private GreigeOrderMapper greigeOrderMapper;

    @Resource
    private GreigeOrderColorMapper greigeOrderColorMapper;

    @Resource
    private ProductMapper productMapper;

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
        greigeOrderMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        GreigeOrderDO order = greigeOrderMapper.selectDeletedById(id, tenantId);
        if (order == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 1. 校验关联产品
        Long productId = order.getProductId();
        if (productId != null) {
            ProductDO deletedProduct = productMapper.selectDeletedById(productId, tenantId);
            if (deletedProduct != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedProduct.getName());
            }
            ProductDO activeProduct = productMapper.selectById(productId);
            if (activeProduct == null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "产品档案");
            }
        }

        // 2. 校验承织坯布厂供应商
        Long greigeFactoryId = order.getGreigeFactoryId();
        if (greigeFactoryId != null) {
            SupplierDO deletedSupplier = supplierMapper.selectDeletedById(greigeFactoryId, tenantId);
            if (deletedSupplier != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedSupplier.getCompanyName());
            }
            SupplierDO activeSupplier = supplierMapper.selectById(greigeFactoryId);
            if (activeSupplier == null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "坯布厂供应商");
            }
        }

        // 3. 校验指定染厂供应商
        Long dyeingFactoryId = order.getDyeingFactoryId();
        if (dyeingFactoryId != null) {
            SupplierDO deletedDyeing = supplierMapper.selectDeletedById(dyeingFactoryId, tenantId);
            if (deletedDyeing != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedDyeing.getCompanyName());
            }
            SupplierDO activeDyeing = supplierMapper.selectById(dyeingFactoryId);
            if (activeDyeing == null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "染厂供应商");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复草稿胚布单【{{#id}}】")
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

        int affected = greigeOrderMapper.restoreGreigeOrder(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 3. 父 -> 子顺序级联复活颜色明细子表
        greigeOrderColorMapper.restoreByOrderId(id, tenantId, updater, now);
        log.info("[GreigeOrderRecycleBinHandler] 草稿胚布单已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        if (id == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long count = greigeOrderMapper.selectActiveReferenceCount(id, tenantId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除草稿胚布单【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 检查是否存在于回收站
        GreigeOrderDO order = greigeOrderMapper.selectDeletedById(id, tenantId);
        if (order == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 2. 引用守卫：存在活跃引用时禁止彻底删除
        if (hasActiveReference(id)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }

        // 3. 子 -> 父顺序彻底删除：先删除颜色明细
        greigeOrderColorMapper.purgeByOrderId(id, tenantId);

        // 4. 物理删除主表
        int affected = greigeOrderMapper.purgeGreigeOrder(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[GreigeOrderRecycleBinHandler] 草稿胚布单已彻底删除: id={}, tenantId={}", id, tenantId);
    }

}
