package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 财务管理 - 收付款核销明细 Mapper（共享表）
 *
 * <p>遵循 docs/agents/coding-standards.md 第 4 节规约：保持 Mapper 纯粹性，不设冗余 default 包装方法。</p>
 *
 * @author luna
 */
@Mapper
public interface PaymentRefMapper extends BaseMapperX<PaymentRefDO> {

    /**
     * 按付款申请ID统计已分配核销总额
     *
     * @param applyId 付款申请ID
     * @return 已分配核销总额
     */
    @Select("SELECT COALESCE(SUM(allocate_amount), 0) FROM lunaerp_finance_payment_ref "
            + "WHERE ref_type = 'PAY_APPLY' AND ref_id = #{applyId} AND deleted = 0")
    BigDecimal selectSumAllocatedAmountByApplyId(@Param("applyId") Long applyId);

    /**
     * 批量查询应收分录在已过账收款单中的已核销总额
     *
     * @param entryIds 分录ID集合
     * @return 分组统计结果列表（包含 entryId 和 totalAllocated）
     */
    @Select("<script>"
            + "SELECT r.ref_id AS entryId, COALESCE(SUM(r.allocate_amount), 0) AS totalAllocated "
            + "FROM lunaerp_finance_payment_ref r "
            + "JOIN lunaerp_finance_payment p ON r.payment_id = p.id AND p.deleted = 0 "
            + "WHERE r.ref_type = 'AR_ENTRY' AND r.deleted = 0 "
            + "AND p.status = 'COMPLETED' "
            + "AND r.ref_id IN "
            + "<foreach collection='entryIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY r.ref_id"
            + "</script>")
    java.util.List<com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.EntryAllocatedSumDTO> selectSumAllocatedAmountByEntryIds(@Param("entryIds") java.util.Collection<Long> entryIds);

    /**
     * 级联恢复收付款核销分配明细
     *
     * @param paymentId 收付款单ID
     * @param tenantId 租户编号
     * @param updater 更新人
     * @param now 更新时间
     * @return 影响行数
     */
    int restoreByPaymentId(@Param("paymentId") Long paymentId, @Param("tenantId") Long tenantId,
                          @Param("updater") String updater, @Param("now") java.time.LocalDateTime now);

    /**
     * 彻底物理删除收付款核销分配明细
     *
     * @param paymentId 收付款单ID
     * @param tenantId 租户编号
     * @return 影响行数
     */
    int purgeByPaymentId(@Param("paymentId") Long paymentId, @Param("tenantId") Long tenantId);

}

