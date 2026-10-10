package com.luna.lunasrv.module.lunaerp.service.business.costcard;

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
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard.CostCardDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardFeeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardYarnMapper;
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
 * 成本卡草稿回收站业务处理器（ADR-0038 / W0 Spec）
 *
 * 仅 DRAFT 状态成本卡进入回收站。
 * 恢复为 DRAFT 状态，并级联复活纱线行与费用行两子表。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class CostCardRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "COST_CARD";

    @Resource
    private CostCardMapper costCardMapper;

    @Resource
    private CostCardYarnMapper costCardYarnMapper;

    @Resource
    private CostCardFeeMapper costCardFeeMapper;

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
        costCardMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        CostCardDO card = costCardMapper.selectDeletedById(id, tenantId);
        if (card == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 校验所属产品：若产品在回收站，阻断并提示先恢复产品档案
        Long productId = card.getProductId();
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
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复草稿成本卡【{{#id}}】")
    public void restore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 恢复前校验存在且在回收站中（主数据引用健全）
        validateRestore(id);

        // 2. 并发条件更新主数据（deleted=0, status=DRAFT）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String updater = loginUserId != null ? String.valueOf(loginUserId) : "1";
        LocalDateTime now = LocalDateTime.now();

        int affected = costCardMapper.restoreCostCard(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 3. 级联复活两子表（父 -> 子顺序）
        costCardYarnMapper.restoreByCardId(id, tenantId, updater, now);
        costCardFeeMapper.restoreByCardId(id, tenantId, updater, now);
        log.info("[CostCardRecycleBinHandler] 草稿成本卡已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除草稿成本卡【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 彻底删除前校验引用关系
        validatePurge(id);

        // 2. 先物理删除子表
        costCardFeeMapper.purgeByCardId(id, tenantId);
        costCardYarnMapper.purgeByCardId(id, tenantId);

        // 3. 再物理删除主表
        int affected = costCardMapper.purgeCostCard(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[CostCardRecycleBinHandler] 草稿成本卡已彻底删除: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        if (id == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long count = costCardMapper.selectActiveReferenceCount(id, tenantId);
        return count != null && count > 0;
    }

    public void validatePurge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        CostCardDO card = costCardMapper.selectDeletedById(id, tenantId);
        if (card == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        if (hasActiveReference(id)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }
    }

}
