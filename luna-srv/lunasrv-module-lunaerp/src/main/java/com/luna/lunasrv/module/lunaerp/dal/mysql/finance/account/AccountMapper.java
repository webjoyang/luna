package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务管理 - 资金账户 Mapper
 *
 * @author luna
 */
@Mapper
public interface AccountMapper extends BaseMapperX<AccountDO> {

    /**
     * 按账户名称精确查询（未删除行）
     *
     * @param accountName 账户名称
     * @return 账户DO
     */
    @Select("SELECT id, account_name, account_type, account_no, account_nature, currency, opening_balance, current_balance, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_account WHERE account_name = #{accountName} AND deleted = 0 LIMIT 1")
    AccountDO selectByAccountName(@Param("accountName") String accountName);

    /**
     * 按账户名称精确查询（含已删除行，租户内全量唯一校验）
     *
     * @param accountName 账户名称
     * @return 账户DO（含已删除行）
     */
    @Select("SELECT id, account_name, account_type, account_no, account_nature, currency, opening_balance, current_balance, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_account WHERE account_name = #{accountName} LIMIT 1")
    AccountDO selectByAccountNameIncludingDeleted(@Param("accountName") String accountName);

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

    /**
     * 回收站分页查询已软删资金账户（手写 XML：显式 WHERE deleted = 1 AND tenant_id = #{tenantId}）
     *
     * @param page 分页参数
     * @param tenantId 租户编号
     * @return 分页结果
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的资金账户记录
     *
     * @param id 主键
     * @param tenantId 租户编号
     * @return 账户 DO
     */
    AccountDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复资金账户（并发条件更新带 deleted=1 AND tenant_id=?，恢复回原状态）
     *
     * @param id 主键
     * @param tenantId 租户编号
     * @param updater 更新人
     * @param now 更新时间
     * @return 影响行数
     */
    int restoreAccount(@Param("id") Long id, @Param("tenantId") Long tenantId,
                       @Param("updater") String updater, @Param("now") LocalDateTime now);

    /**
     * 彻底删除资金账户（物理删除带 deleted=1 AND tenant_id=?）
     *
     * @param id 主键
     * @param tenantId 租户编号
     * @return 影响行数
     */
    int purgeAccount(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查资金账户是否存在活跃分录引用（deleted=0 且 tenant_id=?）
     *
     * @param id 账户ID
     * @param tenantId 租户编号
     * @return 活跃引用数
     */
    Long selectActiveReferenceCount(@Param("id") Long id, @Param("tenantId") Long tenantId);

}
