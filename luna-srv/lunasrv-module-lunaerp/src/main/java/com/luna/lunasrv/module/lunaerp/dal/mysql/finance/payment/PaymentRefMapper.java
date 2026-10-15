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

}
