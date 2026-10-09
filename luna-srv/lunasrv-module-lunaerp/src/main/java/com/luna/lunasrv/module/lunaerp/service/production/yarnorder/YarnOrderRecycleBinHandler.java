package com.luna.lunasrv.module.lunaerp.service.production.yarnorder;

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
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;

/**
 * 纱线采购单草稿回收站业务处理器（ADR-0038 / W0 Spec）
 *
 * 仅 DRAFT 状态纱线采购单进入回收站。
 * 恢复为 DRAFT 状态，并级联复活采购明细子表（父 -> 子顺序）。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class YarnOrderRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "YARN_PURCHASE_ORDER";

    @Resource
    private YarnOrderMapper yarnOrderMapper;

    @Resource
    private YarnOrderItemMapper yarnOrderItemMapper;

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<RecycleItemRespVO> pageDeleted(RecyclePageReqVO req) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        IPage<RecycleItemRespVO> mpPage = MyBatisUtils.buildPage(req);
        yarnOrderMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        YarnOrderDO order = yarnOrderMapper.selectDeletedById(id, tenantId);
        if (order == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 校验明细中的上游主数据引用
        List<YarnOrderItemDO> items = yarnOrderItemMapper.selectDeletedListByOrderId(id, tenantId);
        if (CollUtil.isNotEmpty(items)) {
            for (YarnOrderItemDO item : items) {
                // 1. 供应商校验
                Long supplierId = item.getSupplierId();
                if (supplierId != null) {
                    SupplierDO deletedSupplier = supplierMapper.selectDeletedById(supplierId, tenantId);
                    if (deletedSupplier != null) {
                        throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedSupplier.getCompanyName());
                    }
                    SupplierDO activeSupplier = supplierMapper.selectById(supplierId);
                    if (activeSupplier == null) {
                        throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "纱线供应商");
                    }
                }

                // 2. 纱线材质校验
                Long yarnTypeId = item.getYarnTypeId();
                if (yarnTypeId != null) {
                    YarnTypeDO deletedType = yarnTypeMapper.selectDeletedById(yarnTypeId, tenantId);
                    if (deletedType != null) {
                        throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedType.getName());
                    }
                    YarnTypeDO activeType = yarnTypeMapper.selectById(yarnTypeId);
                    if (activeType == null) {
                        throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "纱线材质");
                    }
                }

                // 3. 纱线规格校验
                Long yarnSpecId = item.getYarnSpecId();
                if (yarnSpecId != null) {
                    YarnSpecDO deletedSpec = yarnSpecMapper.selectDeletedById(yarnSpecId, tenantId);
                    if (deletedSpec != null) {
                        throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedSpec.getSpecName());
                    }
                    YarnSpecDO activeSpec = yarnSpecMapper.selectById(yarnSpecId);
                    if (activeSpec == null) {
                        throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "纱线规格");
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复草稿纱线采购单【{{#id}}】")
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

        int affected = yarnOrderMapper.restoreYarnOrder(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 3. 父 -> 子顺序级联复活采购明细子表
        yarnOrderItemMapper.restoreByOrderId(id, tenantId, updater, now);
        log.info("[YarnOrderRecycleBinHandler] 草稿纱线采购单已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        if (id == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long count = yarnOrderMapper.selectActiveReferenceCount(id, tenantId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除草稿纱线采购单【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 检查是否存在于回收站
        YarnOrderDO order = yarnOrderMapper.selectDeletedById(id, tenantId);
        if (order == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 2. 引用守卫：存在活跃引用时禁止彻底删除
        if (hasActiveReference(id)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }

        // 3. 子 -> 父顺序彻底删除：先删除明细
        yarnOrderItemMapper.purgeByOrderId(id, tenantId);

        // 4. 物理删除主表
        int affected = yarnOrderMapper.purgeYarnOrder(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[YarnOrderRecycleBinHandler] 草稿纱线采购单已彻底删除: id={}, tenantId={}", id, tenantId);
    }

}
