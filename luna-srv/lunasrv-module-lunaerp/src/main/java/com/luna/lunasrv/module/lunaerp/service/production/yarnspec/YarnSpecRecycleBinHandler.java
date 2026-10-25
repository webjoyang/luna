package com.luna.lunasrv.module.lunaerp.service.production.yarnspec;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.util.MyBatisUtils;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.RecycleBinHandler;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
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
 * 纱线规格回收站业务处理器（ADR-0038 / W0 Spec）
 *
 * 负责纱线规格的回收站分页查询、引用检查、复活（主数据恢复为 INACTIVE，先父后子校验材质）以及彻底删除。
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class YarnSpecRecycleBinHandler implements RecycleBinHandler {

    public static final String BIZ_TYPE = "YARN_SPEC";

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<RecycleItemRespVO> pageDeleted(RecyclePageReqVO req) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        IPage<RecycleItemRespVO> mpPage = MyBatisUtils.buildPage(req);
        yarnSpecMapper.selectRecyclePage(mpPage, tenantId);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @DataPermission(enable = false)
    public void validateRestore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        YarnSpecDO yarnSpec = yarnSpecMapper.selectDeletedById(id, tenantId);
        if (yarnSpec == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 校验所属纱线材质：若材质仍在回收站，报 1_030_031_002 阻断并提示先恢复材质
        Long yarnTypeId = yarnSpec.getYarnTypeId();
        if (yarnTypeId != null) {
            YarnTypeDO deletedParent = yarnTypeMapper.selectDeletedById(yarnTypeId, tenantId);
            if (deletedParent != null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, deletedParent.getName());
            }
            YarnTypeDO activeParent = yarnTypeMapper.selectById(yarnTypeId);
            if (activeParent == null) {
                throw exception(RECYCLE_BIN_REFERENCE_IN_RECYCLE, "纱线材质");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "RESTORE", bizNo = "{{#id}}", success = "恢复纱线规格【{{#id}}】")
    public void restore(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        // 1. 恢复前校验存在且材质不在回收站中
        validateRestore(id);

        // 2. 并发条件更新主数据（deleted=0, status=INACTIVE）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String updater = loginUserId != null ? String.valueOf(loginUserId) : "1";
        LocalDateTime now = LocalDateTime.now();

        int affected = yarnSpecMapper.restoreYarnSpec(id, tenantId, updater, now);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[YarnSpecRecycleBinHandler] 纱线规格已恢复: id={}, tenantId={}", id, tenantId);
    }

    @Override
    @DataPermission(enable = false)
    public boolean hasActiveReference(Long id) {
        if (id == null) {
            return false;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long count = yarnSpecMapper.selectActiveReferenceCount(id, tenantId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    @LogRecord(type = "RECYCLE_BIN", subType = "PURGE", bizNo = "{{#id}}", success = "彻底删除纱线规格【{{#id}}】")
    public void purge(Long id) {
        if (id == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // 1. 检查是否存在于回收站
        YarnSpecDO yarnSpec = yarnSpecMapper.selectDeletedById(id, tenantId);
        if (yarnSpec == null) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }

        // 2. 引用守卫：存在活跃引用时禁止彻底删除
        if (hasActiveReference(id)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }

        // 3. 物理删除纱线规格
        int affected = yarnSpecMapper.purgeYarnSpec(id, tenantId);
        if (affected == 0) {
            throw exception(RECYCLE_BIN_RECORD_NOT_EXISTS);
        }
        log.info("[YarnSpecRecycleBinHandler] 纱线规格已彻底删除: id={}, tenantId={}", id, tenantId);
    }

}
