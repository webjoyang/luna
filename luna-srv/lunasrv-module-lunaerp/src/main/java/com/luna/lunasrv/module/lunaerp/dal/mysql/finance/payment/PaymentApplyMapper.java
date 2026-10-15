package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务管理 - 付款申请单 Mapper
 *
 * <p>遵循 docs/agents/coding-standards.md 第 4 节规约：保持 Mapper 纯粹性，不设冗余 default 包装方法。</p>
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

}
