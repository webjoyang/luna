package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceAccountStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务管理 - 资金账户 Mapper
 *
 * @author luna
 */
@Mapper
public interface AccountMapper extends BaseMapperX<AccountDO> {

    /**
     * 按账户名称查询（包含已软删除记录，用于创建时唯一性校验）
     */
    @Select("SELECT id, account_name, account_type, currency, opening_balance, current_balance, "
            + "status, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_account WHERE account_name = #{accountName} LIMIT 1")
    AccountDO selectByAccountNameIncludingDeleted(@Param("accountName") String accountName);

    /**
     * 按账户名称查询（仅未删除）
     */
    default AccountDO selectByAccountName(String accountName) {
        return selectOne(new LambdaQueryWrapperX<AccountDO>()
                .eq(AccountDO::getAccountName, accountName));
    }

    /**
     * 查询所有启用状态的资金账户列表（下拉选单）
     */
    default List<AccountDO> selectListByStatusEnable() {
        return selectList(new LambdaQueryWrapperX<AccountDO>()
                .eq(AccountDO::getStatus, FinanceAccountStatusEnum.ENABLE.getValue())
                .orderByAsc(AccountDO::getId));
    }

    /**
     * 余额条件更新（原子过账通道）：
     * 仅当账户处于启用状态（status='ENABLE'）且未删除时更新；
     * 若为出账（amt < 0），校验扣减后余额不得小于 0（current_balance + amt >= 0）。
     */
    @Update("UPDATE lunaerp_finance_account SET current_balance = current_balance + #{amt}, update_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND status = 'ENABLE' AND deleted = 0 AND (current_balance + #{amt} >= 0 OR #{amt} >= 0)")
    int updateBalanceConditional(@Param("id") Long id, @Param("amt") BigDecimal amt);

}
