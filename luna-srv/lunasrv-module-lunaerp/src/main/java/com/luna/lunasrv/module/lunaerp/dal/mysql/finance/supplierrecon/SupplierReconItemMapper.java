package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconItemDO;
import org.apache.ibatis.annotations.Mapper;

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
     * 按对账单ID查询行明细列表
     *
     * @param reconId 对账单ID
     * @return 行明细列表
     */
    default List<SupplierReconItemDO> selectListByReconId(Long reconId) {
        if (reconId == null) {
            return Collections.emptyList();
        }
        return selectList(SupplierReconItemDO::getReconId, reconId);
    }

    /**
     * 按对账单ID集合批量查询行明细
     *
     * @param reconIds 对账单ID集合
     * @return 行明细列表
     */
    default List<SupplierReconItemDO> selectListByReconIds(Collection<Long> reconIds) {
        if (CollUtil.isEmpty(reconIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<SupplierReconItemDO>()
                .in(SupplierReconItemDO::getReconId, reconIds));
    }

    /**
     * 按对账单ID逻辑删除明细
     *
     * @param reconId 对账单ID
     */
    default void deleteByReconId(Long reconId) {
        if (reconId != null) {
            delete(SupplierReconItemDO::getReconId, reconId);
        }
    }

    /**
     * 根据来源类型与来源主表ID查询已引用的行
     *
     * @param sourceRefType 来源类型
     * @param sourceRefId 来源ID
     * @return 行列表
     */
    default List<SupplierReconItemDO> selectListBySourceRef(String sourceRefType, Long sourceRefId) {
        return selectList(new LambdaQueryWrapperX<SupplierReconItemDO>()
                .eq(SupplierReconItemDO::getSourceRefType, sourceRefType)
                .eq(SupplierReconItemDO::getSourceRefId, sourceRefId));
    }

}
