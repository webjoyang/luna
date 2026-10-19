package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.TransferDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务管理 - 资金账户间转账单 Mapper
 *
 * <p>遵循 coding-standards.md §4：保持 Mapper 纯粹性，不设 default 方法。</p>
 *
 * @author luna
 */
@Mapper
public interface TransferMapper extends BaseMapperX<TransferDO> {

    /**
     * 按转账单号查询（含已删除行，用于按天取号防撞）
     *
     * @param transferNo 转账单号
     * @return 转账单
     */
    @Select("SELECT id, transfer_no, from_account_id, from_account_name, to_account_id, to_account_name, "
            + "currency, amount, transfer_date, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_transfer WHERE transfer_no = #{transferNo} LIMIT 1")
    TransferDO selectByTransferNoIncludingDeleted(@Param("transferNo") String transferNo);

    /**
     * 按转账单号前缀查询列表（含已删除行，用于按天取号扫描）
     *
     * @param prefix 单号前缀（如 ZZ20261019）
     * @return 转账单列表
     */
    @Select("SELECT id, transfer_no, from_account_id, from_account_name, to_account_id, to_account_name, "
            + "currency, amount, transfer_date, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_transfer WHERE transfer_no LIKE CONCAT(#{prefix}, '%')")
    List<TransferDO> selectListByTransferNoPrefixIncludingDeleted(@Param("prefix") String prefix);

}
