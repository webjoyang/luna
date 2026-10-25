package com.luna.lunasrv.module.lunaerp.service.production.weavingorder;

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
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.YarnRequisitionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.item.YarnRequisitionItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper;
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
 * 织造单草稿回收站业务处理器（ADR-0038 / W0 Spec）
 *
 * 仅 DRAFT 状态织造单进入回收站。
 * 恢复为 DRAFT 状态，并逆向复活机台任务、织造布票、领纱单及领纱明细（父 -> 子顺序）。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class WeavingOrderRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "WEAVING_ORDER";

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Resource
    private MachineTaskMapper machineTaskMapper;

    @Resource
    private WeavingTicketMapper weavingTicketMapper;

    @Resource
    private YarnRequisitionMapper yarnRequisitionMapper;

    @Resource
    private YarnRequisitionItemMapper yarnRequisitionItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<RecycleItemRespVO> pageDeleted(RecyclePageReqVO req) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        IPage<RecycleItemRespVO> mpPage = MyBatisUtils.buildPage(req);
        weavingOrderMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        WeavingOrderDO order = weavingOrderMapper.selectDeletedById(id, tenantId);
        if (order == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 校验关联产品
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复草稿织造单【{{#id}}】")
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

        int affected = weavingOrderMapper.restoreWeavingOrder(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 3. 逆向复活级联子资源：父 -> 子顺序
        machineTaskMapper.restoreByWeavingOrderId(id, tenantId, updater, now);
        weavingTicketMapper.restoreByWeavingOrderId(id, tenantId, updater, now);
        yarnRequisitionMapper.restoreByWeavingOrderId(id, tenantId, updater, now);
        yarnRequisitionItemMapper.restoreByWeavingOrderId(id, tenantId, updater, now);
        log.info("[WeavingOrderRecycleBinHandler] 草稿织造单已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        if (id == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long count = weavingOrderMapper.selectActiveReferenceCount(id, tenantId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除草稿织造单【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 检查是否存在于回收站
        WeavingOrderDO order = weavingOrderMapper.selectDeletedById(id, tenantId);
        if (order == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 2. 引用守卫：存在活跃引用时禁止彻底删除
        if (hasActiveReference(id)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }

        // 3. 子 -> 父顺序彻底删除：先删除子资源，再删除主单
        yarnRequisitionItemMapper.purgeByWeavingOrderId(id, tenantId);
        yarnRequisitionMapper.purgeByWeavingOrderId(id, tenantId);
        weavingTicketMapper.purgeByWeavingOrderId(id, tenantId);
        machineTaskMapper.purgeByWeavingOrderId(id, tenantId);

        // 4. 物理删除主表
        int affected = weavingOrderMapper.purgeWeavingOrder(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[WeavingOrderRecycleBinHandler] 草稿织造单已彻底删除: id={}, tenantId={}", id, tenantId);
    }

}
