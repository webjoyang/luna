package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 财务管理 - 付款申请单 Mapper
 *
 * @author luna
 */
@Mapper
public interface PaymentApplyMapper extends BaseMapperX<PaymentApplyDO> {

    /**
     * 按申请单号精确查询（含已删除行，用于按天取号防撞）
     *
     * @param applyNo 申请单号
     * @return 申请单（含已删除行）
     */
    @Select("SELECT id, apply_no, apply_type, supplier_id, supplier_name, recon_id, recon_no, amount, "
            + "purpose, bank_name, bank_account, approval_status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment_apply WHERE apply_no = #{applyNo} LIMIT 1")
    PaymentApplyDO selectByApplyNoIncludingDeleted(@Param("applyNo") String applyNo);

    /**
     * 按申请单号前缀查询列表（含已删除行，用于按天取号扫描）
     *
     * @param prefix 单号前缀（如 FK20261015）
     * @return 申请单列表（含已删除行）
     */
    @Select("SELECT id, apply_no, apply_type, supplier_id, supplier_name, recon_id, recon_no, amount, "
            + "purpose, bank_name, bank_account, approval_status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment_apply WHERE apply_no LIKE CONCAT(#{prefix}, '%')")
    List<PaymentApplyDO> selectListByApplyNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按对账单ID查询关联的付款申请列表
     *
     * @param reconId 对账单ID
     * @return 付款申请列表
     */
    default List<PaymentApplyDO> selectListByReconId(Long reconId) {
        if (reconId == null) {
            return Collections.emptyList();
        }
        return selectList(PaymentApplyDO::getReconId, reconId);
    }

    /**
     * 按对账单ID集合查询关联的付款申请列表
     *
     * @param reconIds 对账单ID集合
     * @return 付款申请列表
     */
    default List<PaymentApplyDO> selectListByReconIds(Collection<Long> reconIds) {
        if (CollUtil.isEmpty(reconIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                .in(PaymentApplyDO::getReconId, reconIds));
    }

}
