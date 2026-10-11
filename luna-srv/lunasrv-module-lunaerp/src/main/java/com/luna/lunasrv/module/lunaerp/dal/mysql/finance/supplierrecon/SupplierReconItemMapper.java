package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 财务管理 - 供应商对账单行表 Mapper
 *
 * @author luna
 */
@Mapper
public interface SupplierReconItemMapper extends BaseMapperX<SupplierReconItemDO> {

    /**
     * 按对账单ID查询明细列表（按 id 升序）
     */
    default List<SupplierReconItemDO> selectListByReconId(Long reconId) {
        return selectList(new LambdaQueryWrapperX<SupplierReconItemDO>()
                .eq(SupplierReconItemDO::getReconId, reconId)
                .orderByAsc(SupplierReconItemDO::getId));
    }

    /**
     * 批量按对账单ID集合查询明细列表
     */
    default List<SupplierReconItemDO> selectListByReconIds(Collection<Long> reconIds) {
        if (CollUtil.isEmpty(reconIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<SupplierReconItemDO>()
                .in(SupplierReconItemDO::getReconId, reconIds)
                .orderByAsc(SupplierReconItemDO::getId));
    }

    /**
     * 按对账单ID删除全部明细
     */
    default int deleteByReconId(Long reconId) {
        return delete(new LambdaQueryWrapperX<SupplierReconItemDO>()
                .eq(SupplierReconItemDO::getReconId, reconId));
    }

    /**
     * 按源单据引用查询明细
     */
    default List<SupplierReconItemDO> selectListBySourceRef(String sourceRefType, Long sourceRefId, Long sourceRefItemId) {
        return selectList(new LambdaQueryWrapperX<SupplierReconItemDO>()
                .eq(SupplierReconItemDO::getSourceRefType, sourceRefType)
                .eq(SupplierReconItemDO::getSourceRefId, sourceRefId)
                .eqIfPresent(SupplierReconItemDO::getSourceRefItemId, sourceRefItemId));
    }

    /**
     * 查询已删除对账单下的明细
     */
    List<SupplierReconItemDO> selectDeletedListByReconId(@Param("reconId") Long reconId, @Param("tenantId") Long tenantId);

    /**
     * 级联恢复对账单明细
     */
    int restoreByReconId(@Param("reconId") Long reconId, @Param("tenantId") Long tenantId,
                         @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除对账单明细
     */
    int purgeByReconId(@Param("reconId") Long reconId, @Param("tenantId") Long tenantId);

}
