package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务管理 - 供应商对账单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface SupplierReconMapper extends BaseMapperX<SupplierReconDO> {

    /**
     * 按单号精确查询对账单（含已删除行，用于按天取号防撞）
     *
     * @param reconNo 对账单号
     * @return 对账单（含已删除行）
     */
    @Select("SELECT id, recon_no, supplier_id, supplier_name, period_start, period_end, status, total_amount, "
            + "confirmed_by, confirmed_at, cancelled_by, cancelled_at, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_supplier_recon WHERE recon_no = #{reconNo} LIMIT 1")
    SupplierReconDO selectByReconNoIncludingDeleted(@Param("reconNo") String reconNo);

    /**
     * 按单号前缀查询对账单列表（含已删除行，用于按天取号扫描）
     *
     * @param prefix 单号前缀（如 SR20261015）
     * @return 对账单列表（含已删除行）
     */
    @Select("SELECT id, recon_no, supplier_id, supplier_name, period_start, period_end, status, total_amount, "
            + "confirmed_by, confirmed_at, cancelled_by, cancelled_at, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_supplier_recon WHERE recon_no LIKE CONCAT(#{prefix}, '%')")
    List<SupplierReconDO> selectListByReconNoPrefixIncludingDeleted(@Param("prefix") String prefix);

}
