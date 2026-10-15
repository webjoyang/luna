package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 财务管理 - 供应商对账单行表 Mapper
 *
 * <p>遵循 docs/agents/coding-standards.md 第 4 节规约：
 * 保持 Mapper 纯粹性，业务与集合空安全由 Service 层负责，不写冗余 default 包装方法。</p>
 *
 * @author luna
 */
@Mapper
public interface SupplierReconItemMapper extends BaseMapperX<SupplierReconItemDO> {

    /**
     * 级联恢复对账单明细
     *
     * @param reconId 对账单ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param now 更新时间
     * @return 影响行数
     */
    int restoreByReconId(@Param("reconId") Long reconId, @Param("tenantId") Long tenantId,
                         @Param("updater") String updater, @Param("now") LocalDateTime now);

    /**
     * 级联物理删除对账单明细
     *
     * @param reconId 对账单ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    int purgeByReconId(@Param("reconId") Long reconId, @Param("tenantId") Long tenantId);

}
