package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 财务管理 - 收付款核销子表 Mapper（共享表）
 *
 * @author luna
 */
@Mapper
public interface PaymentRefMapper extends BaseMapperX<PaymentRefDO> {

    /**
     * 按付款单ID查询核销明细列表
     */
    default List<PaymentRefDO> selectListByPaymentId(Long paymentId) {
        return selectList(new LambdaQueryWrapperX<PaymentRefDO>()
                .eq(PaymentRefDO::getPaymentId, paymentId)
                .orderByAsc(PaymentRefDO::getId));
    }

    /**
     * 按引用类型和引用ID查询核销明细列表
     */
    default List<PaymentRefDO> selectListByRefTypeAndRefId(String refType, Long refId) {
        return selectList(new LambdaQueryWrapperX<PaymentRefDO>()
                .eq(PaymentRefDO::getRefType, refType)
                .eq(PaymentRefDO::getRefId, refId));
    }

    /**
     * 批量按引用类型和引用ID集合查询核销明细列表
     */
    default List<PaymentRefDO> selectListByRefTypeAndRefIds(String refType, Collection<Long> refIds) {
        if (CollUtil.isEmpty(refIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<PaymentRefDO>()
                .eq(PaymentRefDO::getRefType, refType)
                .in(PaymentRefDO::getRefId, refIds));
    }

    /**
     * 查询已删除收款单下的核销明细
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM lunaerp_finance_payment_ref "
            + "WHERE payment_id = #{paymentId} AND deleted = 1 AND tenant_id = #{tenantId} "
            + "ORDER BY id ASC")
    List<PaymentRefDO> selectDeletedListByPaymentId(
            @org.apache.ibatis.annotations.Param("paymentId") Long paymentId,
            @org.apache.ibatis.annotations.Param("tenantId") Long tenantId);

    /**
     * 级联恢复收款单核销明细
     */
    @org.apache.ibatis.annotations.Update("UPDATE lunaerp_finance_payment_ref "
            + "SET deleted = 0, updater = #{updater}, update_time = #{updateTime} "
            + "WHERE payment_id = #{paymentId} AND deleted = 1 AND tenant_id = #{tenantId}")
    int restoreByPaymentId(@org.apache.ibatis.annotations.Param("paymentId") Long paymentId,
                           @org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                           @org.apache.ibatis.annotations.Param("updater") String updater,
                           @org.apache.ibatis.annotations.Param("updateTime") java.time.LocalDateTime updateTime);

    /**
     * 彻底物理删除收款单核销明细
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM lunaerp_finance_payment_ref "
            + "WHERE payment_id = #{paymentId} AND deleted = 1 AND tenant_id = #{tenantId}")
    int purgeByPaymentId(@org.apache.ibatis.annotations.Param("paymentId") Long paymentId,
                         @org.apache.ibatis.annotations.Param("tenantId") Long tenantId);

}
