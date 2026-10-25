package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.OtherTxnDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务管理 - 其他收支单 Mapper
 *
 * <p>遵循 coding-standards.md §4：保持 Mapper 纯粹性，不设 default 方法。</p>
 *
 * @author luna
 */
@Mapper
public interface OtherTxnMapper extends BaseMapperX<OtherTxnDO> {

    /**
     * 按单号查询（含已删除行，用于按天取号防撞）
     *
     * @param txnNo 单号
     * @return 其他收支单
     */
    @Select("SELECT id, txn_no, account_id, account_name, direction, category, "
            + "currency, original_amount, exchange_rate, base_amount, txn_date, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_other_txn WHERE txn_no = #{txnNo} LIMIT 1")
    OtherTxnDO selectByTxnNoIncludingDeleted(@Param("txnNo") String txnNo);

    /**
     * 按单号前缀查询列表（含已删除行，用于按天取号扫描）
     *
     * @param prefix 单号前缀（如 QT20261019）
     * @return 其他收支单列表
     */
    @Select("SELECT id, txn_no, account_id, account_name, direction, category, "
            + "currency, original_amount, exchange_rate, base_amount, txn_date, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_other_txn WHERE txn_no LIKE CONCAT(#{prefix}, '%')")
    List<OtherTxnDO> selectListByTxnNoPrefixIncludingDeleted(@Param("prefix") String prefix);

}
