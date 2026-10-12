package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务管理 - 收付款单主表 Mapper（共享表）
 *
 * @author luna
 */
@Mapper
public interface PaymentMapper extends BaseMapperX<PaymentDO> {

    /**
     * 按单号精确查询付款单（含已删除行）
     */
    @Select("SELECT id, payment_no, direction, supplier_id, customer_id, account_id, "
            + "pay_amount, prepay_used_amount, status, receipt_type, currency, exchange_rate, receipt_date, "
            + "bank_ref_no, handler_name, approval_status, remark, creator, create_time, "
            + "updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment WHERE payment_no = #{paymentNo} LIMIT 1")
    PaymentDO selectByPaymentNoIncludingDeleted(@Param("paymentNo") String paymentNo);

    /**
     * 按单号前缀查询付款单列表（含已删除行，用于按天取号扫描）
     */
    @Select("SELECT id, payment_no, direction, supplier_id, customer_id, account_id, "
            + "pay_amount, prepay_used_amount, status, receipt_type, currency, exchange_rate, receipt_date, "
            + "bank_ref_no, handler_name, approval_status, remark, creator, create_time, "
            + "updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment WHERE payment_no LIKE CONCAT(#{prefix}, '%')")
    List<PaymentDO> selectListByPaymentNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按供应商ID与方向查询付款单列表
     */
    default List<PaymentDO> selectListBySupplierIdAndDirection(Long supplierId, String direction) {
        return selectList(new LambdaQueryWrapperX<PaymentDO>()
                .eq(PaymentDO::getSupplierId, supplierId)
                .eq(PaymentDO::getDirection, direction));
    }

    /**
     * 按方向与过滤条件分页查询付款单
     */
    default PageResult<PaymentDO> selectPageByDirection(PaymentPageReqVO reqVO, String direction) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PaymentDO>()
                .eq(PaymentDO::getDirection, direction)
                .eqIfPresent(PaymentDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(PaymentDO::getAccountId, reqVO.getAccountId())
                .geIfPresent(PaymentDO::getCreateTime, reqVO.getCreateTimeStart())
                .leIfPresent(PaymentDO::getCreateTime, reqVO.getCreateTimeEnd())
                .orderByDesc(PaymentDO::getId));
    }

    /**
     * 回收站分页查询已软删客户收款单（仅 IN 方向且 PENDING 状态）
     */
    @Select("SELECT 'PAYMENT' AS biz_type, id AS biz_id, payment_no AS display_name, "
            + "updater AS deleted_by, update_time AS deleted_time "
            + "FROM lunaerp_finance_payment "
            + "WHERE deleted = 1 AND direction = 'IN' AND approval_status = 'PENDING' AND tenant_id = #{tenantId} "
            + "ORDER BY update_time DESC")
    com.baomidou.mybatisplus.core.metadata.IPage<com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO> selectRecyclePage(
            com.baomidou.mybatisplus.core.metadata.IPage<?> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿客户收款单
     */
    @Select("SELECT * FROM lunaerp_finance_payment "
            + "WHERE id = #{id} AND deleted = 1 AND direction = 'IN' AND approval_status = 'PENDING' AND tenant_id = #{tenantId} LIMIT 1")
    PaymentDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿客户收款单
     */
    @org.apache.ibatis.annotations.Update("UPDATE lunaerp_finance_payment "
            + "SET deleted = 0, approval_status = 'PENDING', updater = #{updater}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND deleted = 1 AND direction = 'IN' AND approval_status = 'PENDING' AND tenant_id = #{tenantId}")
    int restorePayment(@Param("id") Long id, @Param("tenantId") Long tenantId,
                       @Param("updater") String updater, @Param("updateTime") java.time.LocalDateTime updateTime);

    /**
     * 彻底物理删除草稿客户收款单
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM lunaerp_finance_payment "
            + "WHERE id = #{id} AND deleted = 1 AND direction = 'IN' AND approval_status = 'PENDING' AND tenant_id = #{tenantId}")
    int purgePayment(@Param("id") Long id, @Param("tenantId") Long tenantId);

}
