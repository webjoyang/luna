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

}
