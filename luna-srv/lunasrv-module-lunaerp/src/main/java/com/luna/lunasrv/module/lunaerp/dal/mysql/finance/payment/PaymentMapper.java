package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务管理 - 收付款单 Mapper（共享表）
 *
 * @author luna
 */
@Mapper
public interface PaymentMapper extends BaseMapperX<PaymentDO> {

    /**
     * 按单号精确查询（含已删除行，用于按天取号防撞）
     *
     * @param paymentNo 收付款单号
     * @return 收付款单（含已删除行）
     */
    @Select("SELECT id, payment_no, direction, supplier_id, customer_id, account_id, pay_amount, prepay_used_amount, "
            + "status, remark, receipt_type, currency, exchange_rate, receipt_date, bank_ref_no, handler_name, approval_status, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment WHERE payment_no = #{paymentNo} LIMIT 1")
    PaymentDO selectByPaymentNoIncludingDeleted(@Param("paymentNo") String paymentNo);

    /**
     * 按单号前缀查询列表（含已删除行，用于按天取号扫描）
     *
     * @param prefix 单号前缀
     * @return 收付款单列表（含已删除行）
     */
    @Select("SELECT id, payment_no, direction, supplier_id, customer_id, account_id, pay_amount, prepay_used_amount, "
            + "status, remark, receipt_type, currency, exchange_rate, receipt_date, bank_ref_no, handler_name, approval_status, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment WHERE payment_no LIKE CONCAT(#{prefix}, '%')")
    List<PaymentDO> selectListByPaymentNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按供应商统计已核销使用的预付总额（prepay_used_amount 累计）
     *
     * @param supplierId 供应商ID
     * @param tenantId 租户编号
     * @return 预付使用总额
     */
    @Select("SELECT COALESCE(SUM(prepay_used_amount), 0) FROM lunaerp_finance_payment "
            + "WHERE supplier_id = #{supplierId} AND direction = 'OUT' AND deleted = 0 AND tenant_id = #{tenantId}")
    BigDecimal selectSumPrepayUsedAmountBySupplier(@Param("supplierId") Long supplierId, @Param("tenantId") Long tenantId);

}
