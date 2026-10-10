package com.luna.lunasrv.module.lunaerp.service.production.yarnprice;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.util.MyBatisUtils;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.RecycleBinHandler;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnprice.YarnPriceDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnprice.YarnPriceMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
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
import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.YARN_PRICE_DUPLICATE_DATE;

/**
 * 纱价主档回收站业务处理器（ADR-0038 / W0 Spec）
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class YarnPriceRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "YARN_PRICE";

    @Resource
    private YarnPriceMapper yarnPriceMapper;

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
        yarnPriceMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        YarnPriceDO yarnPrice = yarnPriceMapper.selectDeletedById(id, tenantId);
        if (yarnPrice == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 校验所属纱线规格：若规格仍在回收站，报阻断并提示先恢复规格
        Long yarnSpecId = yarnPrice.getYarnSpecId();
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

        // 校验恢复后是否与当前未删记录发生同一规格同一价类同一生效日期冲突
        YarnPriceDO conflict = yarnPriceMapper.selectBySpecTypeAndDate(yarnPrice.getYarnSpecId(), yarnPrice.getPriceType(), yarnPrice.getEffectiveDate());
        if (conflict != null && !conflict.getId().equals(id)) {
            throw exception(YARN_PRICE_DUPLICATE_DATE);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复纱价记录【{{#id}}】")
    public void restore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        // 1. 恢复前校验
        validateRestore(id);

        // 2. 并发条件更新主数据（deleted=0）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String updater = loginUserId != null ? String.valueOf(loginUserId) : "1";
        LocalDateTime now = LocalDateTime.now();

        int affected = yarnPriceMapper.restoreYarnPrice(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[YarnPriceRecycleBinHandler] 纱价记录已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        if (id == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long count = yarnPriceMapper.selectActiveReferenceCount(id, tenantId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除纱价记录【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 检查是否存在于回收站
        YarnPriceDO yarnPrice = yarnPriceMapper.selectDeletedById(id, tenantId);
        if (yarnPrice == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 2. 引用守卫：存在活跃引用时禁止彻底删除
        if (hasActiveReference(id)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }

        // 3. 物理删除
        int affected = yarnPriceMapper.purgeYarnPrice(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[YarnPriceRecycleBinHandler] 纱价记录已彻底删除: id={}, tenantId={}", id, tenantId);
    }

}
