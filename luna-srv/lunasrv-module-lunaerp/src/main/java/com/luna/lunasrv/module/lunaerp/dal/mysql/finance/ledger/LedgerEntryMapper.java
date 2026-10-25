package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.AccountDirectionAmountDTO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.CategoryAmountDTO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 财务管理 - 经营分录账 Mapper（append-only 唯一账本）
 *
 * @author luna
 */
@Mapper
public interface LedgerEntryMapper extends BaseMapperX<LedgerEntryDO> {

    String COLUMNS = "id, biz_date, party_type, party_id, party_name, account_id, category, direction, "
            + "currency, exchange_rate, original_amount, base_amount, ref_type, ref_id, ref_item_id, ref_no, reversal_of_id, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id";

    /**
     * 按业务回指主单查询分录列表
     *
     * @param refType 业务回指类型
     * @param refId 业务回指主单ID
     * @return 分录列表
     */
    @Select("SELECT " + COLUMNS + " FROM lunaerp_finance_ledger_entry "
            + "WHERE ref_type = #{refType} AND ref_id = #{refId} AND deleted = 0 ORDER BY id ASC")
    List<LedgerEntryDO> selectListByRef(@Param("refType") String refType,
                                        @Param("refId") Long refId);

    /**
     * 按业务回指主单与明细行查询分录列表
     *
     * @param refType 业务回指类型
     * @param refId 业务回指主单ID
     * @param refItemId 业务回指明细ID
     * @return 分录列表
     */
    @Select("SELECT " + COLUMNS + " FROM lunaerp_finance_ledger_entry "
            + "WHERE ref_type = #{refType} AND ref_id = #{refId} AND ref_item_id = #{refItemId} AND deleted = 0 ORDER BY id ASC")
    List<LedgerEntryDO> selectListByRefAndItemId(@Param("refType") String refType,
                                                @Param("refId") Long refId,
                                                @Param("refItemId") Long refItemId);

    /**
     * 查询指定原分录的红冲分录列表
     *
     * @param reversalOfId 原分录ID
     * @return 红冲分录列表
     */
    @Select("SELECT " + COLUMNS + " FROM lunaerp_finance_ledger_entry "
            + "WHERE reversal_of_id = #{reversalOfId} AND deleted = 0 ORDER BY id ASC")
    List<LedgerEntryDO> selectListByReversalOfId(@Param("reversalOfId") Long reversalOfId);

    /**
     * 批量查询指定原分录的红冲分录列表（防 N+1）
     *
     * @param reversalOfIds 原分录ID集合
     * @return 红冲分录列表
     */
    @Select("<script>"
            + "SELECT " + COLUMNS + " FROM lunaerp_finance_ledger_entry "
            + "WHERE reversal_of_id IN "
            + "<foreach collection='reversalOfIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "AND deleted = 0 ORDER BY id ASC"
            + "</script>")
    List<LedgerEntryDO> selectListByReversalOfIds(@Param("reversalOfIds") Collection<Long> reversalOfIds);

    /**
     * 统计指定资金账户的分录数量（用于账户删除保护）
     *
     * @param accountId 资金账户ID
     * @return 引用数量
     */
    @Select("SELECT COUNT(1) FROM lunaerp_finance_ledger_entry WHERE account_id = #{accountId} AND deleted = 0")
    Long selectCountByAccountId(@Param("accountId") Long accountId);

    /**
     * 按账户与收支方向聚合区间发生额
     *
     * @param accountId 可选资金账户ID
     * @param startDate 区间开始日期
     * @param endDate 区间结束日期
     * @return 账户各方向发生额
     */
    @Select("<script>"
            + "SELECT account_id AS accountId, direction, COALESCE(SUM(original_amount), 0) AS totalAmount "
            + "FROM lunaerp_finance_ledger_entry "
            + "WHERE deleted = 0 AND account_id IS NOT NULL "
            + "<if test='accountId != null'> AND account_id = #{accountId} </if>"
            + "<if test='startDate != null'> AND biz_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND biz_date &lt;= #{endDate} </if>"
            + "GROUP BY account_id, direction"
            + "</script>")
    List<AccountDirectionAmountDTO> selectAccountDirectionSummary(@Param("accountId") Long accountId,
                                                                 @Param("startDate") LocalDate startDate,
                                                                 @Param("endDate") LocalDate endDate);

    /**
     * 按账户与收支方向聚合指定日期之前的历史发生额（用于期初推算）
     *
     * @param accountId 可选资金账户ID
     * @param beforeDate 截止日期（不含）
     * @return 账户各方向历史发生额
     */
    @Select("<script>"
            + "SELECT account_id AS accountId, direction, COALESCE(SUM(original_amount), 0) AS totalAmount "
            + "FROM lunaerp_finance_ledger_entry "
            + "WHERE deleted = 0 AND account_id IS NOT NULL "
            + "<if test='accountId != null'> AND account_id = #{accountId} </if>"
            + "AND biz_date &lt; #{beforeDate} "
            + "GROUP BY account_id, direction"
            + "</script>")
    List<AccountDirectionAmountDTO> selectAccountDirectionSummaryBeforeDate(@Param("accountId") Long accountId,
                                                                           @Param("beforeDate") LocalDate beforeDate);

    /**
     * 按类别与方向聚合区间发生额
     *
     * @param accountId 可选资金账户ID
     * @param startDate 区间开始日期
     * @param endDate 区间结束日期
     * @return 类别各方向发生额
     */
    @Select("<script>"
            + "SELECT category, direction, COALESCE(SUM(original_amount), 0) AS originalAmount, "
            + "COALESCE(SUM(base_amount), 0) AS baseAmount, COUNT(1) AS count "
            + "FROM lunaerp_finance_ledger_entry "
            + "WHERE deleted = 0 AND account_id IS NOT NULL "
            + "<if test='accountId != null'> AND account_id = #{accountId} </if>"
            + "<if test='startDate != null'> AND biz_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND biz_date &lt;= #{endDate} </if>"
            + "GROUP BY category, direction "
            + "ORDER BY baseAmount DESC"
            + "</script>")
    List<CategoryAmountDTO> selectCategorySummary(@Param("accountId") Long accountId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * 按类别与业务日期聚合本位币金额（如 AR_INC, AR_INC_REVERSAL）
     *
     * @param category 分录类别
     * @param startDate 区间开始日期
     * @param endDate 区间结束日期
     * @return 本位币累计金额
     */
    @Select("<script>"
            + "SELECT COALESCE(SUM(base_amount), 0) FROM lunaerp_finance_ledger_entry "
            + "WHERE category = #{category} AND deleted = 0 "
            + "<if test='startDate != null'> AND biz_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND biz_date &lt;= #{endDate} </if>"
            + "</script>")
    BigDecimal selectSumBaseAmountByCategoryAndBizDateRange(@Param("category") String category,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);

    /**
     * 统计指定业务日期区间内应收分录数量
     *
     * @param startDate 区间开始日期
     * @param endDate 区间结束日期
     * @return 应收分录数量
     */
    @Select("<script>"
            + "SELECT COUNT(1) FROM lunaerp_finance_ledger_entry "
            + "WHERE category IN ('AR_INC', 'AR_INC_REVERSAL') AND deleted = 0 "
            + "<if test='startDate != null'> AND biz_date &gt;= #{startDate} </if>"
            + "<if test='endDate != null'> AND biz_date &lt;= #{endDate} </if>"
            + "</script>")
    Long selectCountArEntriesByBizDateRange(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

}
