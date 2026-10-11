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
     * 按单号精确查询付款申请（含已删除行）
     */
    @Select("SELECT id, apply_no, apply_type, supplier_id, supplier_name, recon_id, recon_no, "
            + "amount, purpose, bank_name, bank_account, approval_status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment_apply WHERE apply_no = #{applyNo} LIMIT 1")
    PaymentApplyDO selectByApplyNoIncludingDeleted(@Param("applyNo") String applyNo);

    /**
     * 按单号前缀查询付款申请列表（含已删除行，用于按天取号扫描）
     */
    @Select("SELECT id, apply_no, apply_type, supplier_id, supplier_name, recon_id, recon_no, "
            + "amount, purpose, bank_name, bank_account, approval_status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment_apply WHERE apply_no LIKE CONCAT(#{prefix}, '%')")
    List<PaymentApplyDO> selectListByApplyNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按对账单ID查询付款申请列表
     */
    default List<PaymentApplyDO> selectListByReconId(Long reconId) {
        return selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                .eq(PaymentApplyDO::getReconId, reconId));
    }

    /**
     * 按供应商ID与审批状态集合查询付款申请列表
     */
    default List<PaymentApplyDO> selectListBySupplierIdAndApprovalStatuses(Long supplierId, Collection<String> approvalStatuses) {
        if (CollUtil.isEmpty(approvalStatuses)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                .eq(PaymentApplyDO::getSupplierId, supplierId)
                .in(PaymentApplyDO::getApprovalStatus, approvalStatuses));
    }

}
