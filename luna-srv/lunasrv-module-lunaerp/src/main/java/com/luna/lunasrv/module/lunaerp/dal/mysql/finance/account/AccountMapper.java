package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 财务管理 - 资金账户 Mapper
 *
 * @author luna
 */
@Mapper
public interface AccountMapper extends BaseMapperX<AccountDO> {

    /**
     * 按账户名称精确查询
     *
     * @param accountName 账户名称
     * @return 账户DO
     */
    @org.apache.ibatis.annotations.Select("SELECT id, account_name, account_type, currency, opening_balance, current_balance, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_account WHERE account_name = #{accountName} AND deleted = 0 LIMIT 1")
    AccountDO selectByAccountName(@Param("accountName") String accountName);

    /**
     * 条件增加账户余额（流入：收款、转入、红冲回滚等）
     * 影响行数为 0 表示账户不存在、已停用或已删除
     *
     * @param id 账户ID
     * @param amountDelta 增加金额（正数）
     * @return 影响行数
     */
    @Update("UPDATE lunaerp_finance_account SET current_balance = current_balance + #{amountDelta}, "
            + "update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND status = 'ENABLE' AND deleted = 0")
    int updateBalanceIncrease(@Param("id") Long id, @Param("amountDelta") BigDecimal amountDelta);

    /**
     * 条件扣减账户余额（流出：付款、转出等）
     * 影响行数为 0 表示账户不存在、已停用、已删除或余额不足
     *
     * @param id 账户ID
     * @param amountDelta 变更金额（负数）
     * @param minBalance 最低余额要求（通常为 abs(amountDelta)）
     * @return 影响行数
     */
    @Update("UPDATE lunaerp_finance_account SET current_balance = current_balance + #{amountDelta}, "
            + "update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND status = 'ENABLE' AND deleted = 0 "
            + "AND current_balance >= #{minBalance}")
    int updateBalanceDecrease(@Param("id") Long id, @Param("amountDelta") BigDecimal amountDelta, @Param("minBalance") BigDecimal minBalance);

}
