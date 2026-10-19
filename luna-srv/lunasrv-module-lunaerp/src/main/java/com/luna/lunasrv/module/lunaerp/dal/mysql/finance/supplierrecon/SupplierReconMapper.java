package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /**
     * 回收站分页查询已软删草稿对账单
     *
     * @param page 分页参数
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿对账单记录
     *
     * @param id 对账单ID
     * @param tenantId 租户ID
     * @return 对账单
     */
    SupplierReconDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿对账单
     *
     * @param id 对账单ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param now 更新时间
     * @return 影响行数
     */
    int restoreSupplierRecon(@Param("id") Long id, @Param("tenantId") Long tenantId,
                             @Param("updater") String updater, @Param("now") LocalDateTime now);

    /**
     * 彻底物理删除草稿对账单
     *
     * @param id 对账单ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    int purgeSupplierRecon(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查对账单是否存在未删除的活跃业务引用（如有效付款申请）
     *
     * @param reconId 对账单ID
     * @param tenantId 租户ID
     * @return 活跃引用数量
     */
    Long selectActiveReferenceCount(@Param("reconId") Long reconId, @Param("tenantId") Long tenantId);

    /**
     * 统计指定确认时间区间内 CONFIRMED 供应商对账单总金额（用于毛利成本计算）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 确认对账单总金额
     */
    @Select("<script>"
            + "SELECT COALESCE(SUM(total_amount), 0) FROM lunaerp_finance_supplier_recon "
            + "WHERE status = 'CONFIRMED' AND deleted = 0 "
            + "<if test='startTime != null'> AND confirmed_at &gt;= #{startTime} </if>"
            + "<if test='endTime != null'> AND confirmed_at &lt;= #{endTime} </if>"
            + "</script>")
    BigDecimal selectSumConfirmedTotalAmount(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

}
