package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

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
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.color.ColorInstructionColorMapper;
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
 * 打色指示单回收站业务处理器（ADR-0038 / W0 Spec / Issue 09）
 *
 * 仅 PENDING 状态打色指示单进入回收站（打色单无 DRAFT，PENDING 为"从未生效"等价物）。
 * 恢复为 PENDING 状态，并级联复活全部颜色行（含历史 REJECTED 轮次行，父 -> 子顺序）。
 * 主档阻断守卫：引用的客户/产品/承接供应商（若有）在回收站则阻断恢复（1_030_031_002）。
 * CONFIRMED/IN_PRODUCTION/VOID 不进回收站（作废留单是业务事实）。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ColorInstructionRecycleHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "COLOR_INSTRUCTION";
    private static final String DEFAULT_SYSTEM_OPERATOR = "1";

    @Resource
    private ColorInstructionMapper colorInstructionMapper;

    @Resource
    private ColorInstructionColorMapper colorInstructionColorMapper;

    @Resource
    private CustomerMapper customerMapper;

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
        colorInstructionMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ColorInstructionDO order = colorInstructionMapper.selectDeletedById(id, tenantId);
        if (order == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 1. 校验客户档案
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

        // 2. 校验产品档案
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

        // 3. 校验打色承接方供应商（若为本厂自打色则 labFactoryId 为空，跳过）
        Long labFactoryId = order.getLabFactoryId();
        if (labFactoryId != null) {
            SupplierDO deletedSupplier = supplierMapper.selectDeletedById(labFactoryId, tenantId);
            if (deletedSupplier != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedSupplier.getCompanyName());
            }
            SupplierDO activeSupplier = supplierMapper.selectById(labFactoryId);
            if (activeSupplier == null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "打色供应商");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复打色指示单【{{#id}}】")
    public void restore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 恢复前校验存在且在回收站中，且上游主数据未在回收站
        validateRestore(id);

        // 2. 并发条件更新主数据（deleted=0, status=PENDING）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String updater = loginUserId != null ? String.valueOf(loginUserId) : DEFAULT_SYSTEM_OPERATOR;
        LocalDateTime now = LocalDateTime.now();

        int affected = colorInstructionMapper.restoreColorInstruction(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 3. 父 -> 子顺序级联复活全部颜色明细（含历史 REJECTED 轮次行，不区分行状态批量复活）
        colorInstructionColorMapper.restoreByInstructionId(id, tenantId, updater, now);
        log.info("[ColorInstructionRecycleHandler] 打色指示单已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        if (id == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long count = colorInstructionMapper.selectActiveReferenceCount(id, tenantId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除打色指示单【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 检查是否存在于回收站
        ColorInstructionDO order = colorInstructionMapper.selectDeletedById(id, tenantId);
        if (order == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 2. 引用守卫：存在活跃引用时禁止彻底删除
        if (hasActiveReference(id)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }

        // 3. 子 -> 父顺序彻底删除：先物理删除颜色明细
        colorInstructionColorMapper.purgeByInstructionId(id, tenantId);

        // 4. 物理删除主表
        int affected = colorInstructionMapper.purgeColorInstruction(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[ColorInstructionRecycleHandler] 打色指示单已彻底删除: id={}, tenantId={}", id, tenantId);
    }

}
