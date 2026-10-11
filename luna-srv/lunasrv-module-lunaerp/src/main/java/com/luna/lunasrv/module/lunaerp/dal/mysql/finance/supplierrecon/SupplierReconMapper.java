package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 财务管理 - 供应商对账单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface SupplierReconMapper extends BaseMapperX<SupplierReconDO> {

    /**
     * 按单号精确查询对账单（含已删除行，用于防撞与查重）
     */
    @Select("SELECT id, recon_no, supplier_id, supplier_name, period_start, period_end, "
            + "status, total_amount, confirmed_by, confirmed_at, cancelled_by, cancelled_at, "
            + "remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_supplier_recon WHERE recon_no = #{reconNo} LIMIT 1")
    SupplierReconDO selectByReconNoIncludingDeleted(@Param("reconNo") String reconNo);

    /**
     * 按单号前缀查询对账单列表（含已删除行，用于按天取号扫描）
     */
    @Select("SELECT id, recon_no, supplier_id, supplier_name, period_start, period_end, "
            + "status, total_amount, confirmed_by, confirmed_at, cancelled_by, cancelled_at, "
            + "remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_supplier_recon WHERE recon_no LIKE CONCAT(#{prefix}, '%')")
    List<SupplierReconDO> selectListByReconNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按供应商ID与状态集合查询对账单列表
     */
    default List<SupplierReconDO> selectListBySupplierIdAndStatuses(Long supplierId, Collection<String> statuses) {
        if (CollUtil.isEmpty(statuses)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<SupplierReconDO>()
                .eqIfPresent(SupplierReconDO::getSupplierId, supplierId)
                .in(SupplierReconDO::getStatus, statuses));
    }

    /**
     * 按状态集合查询对账单列表
     */
    default List<SupplierReconDO> selectListByStatuses(Collection<String> statuses) {
        if (CollUtil.isEmpty(statuses)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<SupplierReconDO>()
                .in(SupplierReconDO::getStatus, statuses));
    }

}
