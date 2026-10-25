package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollPaymentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务管理 - 工资发放批次 Mapper
 *
 * <p>遵循 coding-standards.md §4：保持 Mapper 纯粹性，不设 default 方法。</p>
 *
 * @author luna
 */
@Mapper
public interface PayrollPaymentMapper extends BaseMapperX<PayrollPaymentDO> {

    /**
     * 按批次流水号查询（含已删除行，用于按天取号防撞）
     *
     * @param paymentNo 批次流水号
     * @return 发放批次
     */
    @Select("SELECT * FROM lunaerp_finance_payroll_payment WHERE payment_no = #{paymentNo} LIMIT 1")
    PayrollPaymentDO selectByPaymentNoIncludingDeleted(@Param("paymentNo") String paymentNo);

    /**
     * 按前缀查询发放批次列表（含已删除行，用于取号）
     *
     * @param prefix 单号前缀
     * @return 列表
     */
    @Select("SELECT * FROM lunaerp_finance_payroll_payment WHERE payment_no LIKE CONCAT(#{prefix}, '%')")
    List<PayrollPaymentDO> selectListByPaymentNoPrefixIncludingDeleted(@Param("prefix") String prefix);

}
