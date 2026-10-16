package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 财务管理 - 经营分录账 Mapper（append-only 唯一账本）
 *
 * @author luna
 */
@Mapper
public interface LedgerEntryMapper extends BaseMapperX<LedgerEntryDO> {

    String COLUMNS = "id, entry_no, biz_date, category, direction, party_type, party_id, account_id, "
            + "currency, amount, exchange_rate, standard_amount, ref_type, ref_id, ref_item_id, reversal_of_id, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id";

    /**
     * 按业务回指主单查询分录列表
     *
     * @param refType 业务回指类型
     * @param refId 业务回指主单ID
     * @return 分录列表
     */
    @org.apache.ibatis.annotations.Select("SELECT " + COLUMNS + " FROM lunaerp_finance_ledger_entry "
            + "WHERE ref_type = #{refType} AND ref_id = #{refId} AND deleted = 0 ORDER BY id ASC")
    List<LedgerEntryDO> selectListByRef(@org.apache.ibatis.annotations.Param("refType") String refType,
                                        @org.apache.ibatis.annotations.Param("refId") Long refId);

    /**
     * 按业务回指主单与明细行查询分录列表
     *
     * @param refType 业务回指类型
     * @param refId 业务回指主单ID
     * @param refItemId 业务回指明细ID
     * @return 分录列表
     */
    @org.apache.ibatis.annotations.Select("SELECT " + COLUMNS + " FROM lunaerp_finance_ledger_entry "
            + "WHERE ref_type = #{refType} AND ref_id = #{refId} AND ref_item_id = #{refItemId} AND deleted = 0 ORDER BY id ASC")
    List<LedgerEntryDO> selectListByRefAndItemId(@org.apache.ibatis.annotations.Param("refType") String refType,
                                                @org.apache.ibatis.annotations.Param("refId") Long refId,
                                                @org.apache.ibatis.annotations.Param("refItemId") Long refItemId);

    /**
     * 查询指定原分录的红冲分录列表
     *
     * @param reversalOfId 原分录ID
     * @return 红冲分录列表
     */
    @org.apache.ibatis.annotations.Select("SELECT " + COLUMNS + " FROM lunaerp_finance_ledger_entry "
            + "WHERE reversal_of_id = #{reversalOfId} AND deleted = 0 ORDER BY id ASC")
    List<LedgerEntryDO> selectListByReversalOfId(@org.apache.ibatis.annotations.Param("reversalOfId") Long reversalOfId);

    /**
     * 批量查询指定原分录的红冲分录列表（防 N+1）
     *
     * @param reversalOfIds 原分录ID集合
     * @return 红冲分录列表
     */
    @org.apache.ibatis.annotations.Select("<script>"
            + "SELECT " + COLUMNS + " FROM lunaerp_finance_ledger_entry "
            + "WHERE reversal_of_id IN "
            + "<foreach collection='reversalOfIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "AND deleted = 0 ORDER BY id ASC"
            + "</script>")
    List<LedgerEntryDO> selectListByReversalOfIds(@org.apache.ibatis.annotations.Param("reversalOfIds") java.util.Collection<Long> reversalOfIds);

    /**
     * 统计指定资金账户的分录数量（用于账户删除保护）
     *
     * @param accountId 资金账户ID
     * @return 引用数量
     */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(1) FROM lunaerp_finance_ledger_entry WHERE account_id = #{accountId} AND deleted = 0")
    Long selectCountByAccountId(@org.apache.ibatis.annotations.Param("accountId") Long accountId);

}
