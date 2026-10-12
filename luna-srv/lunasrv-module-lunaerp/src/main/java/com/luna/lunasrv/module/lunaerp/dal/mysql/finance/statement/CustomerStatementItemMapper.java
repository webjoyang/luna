package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 客户对账单明细行表 Mapper
 *
 * @author luna
 */
@Mapper
public interface CustomerStatementItemMapper extends BaseMapperX<CustomerStatementItemDO> {

    /**
     * 按对账单ID查询行明细
     */
    default List<CustomerStatementItemDO> selectListByStatementId(Long statementId) {
        return selectList(new LambdaQueryWrapperX<CustomerStatementItemDO>()
                .eq(CustomerStatementItemDO::getStatementId, statementId)
                .orderByAsc(CustomerStatementItemDO::getLineNo)
                .orderByAsc(CustomerStatementItemDO::getId));
    }

    /**
     * 查询被非 VOID 对账单占用的所有分录 ID 集合
     */
    @org.apache.ibatis.annotations.Select("SELECT DISTINCT item.ledger_entry_id FROM lunaerp_finance_statement_item item "
            + "JOIN lunaerp_finance_statement s ON item.statement_id = s.id "
            + "WHERE item.deleted = 0 AND s.deleted = 0 AND s.status != 'VOID' AND item.ledger_entry_id IS NOT NULL")
    List<Long> selectActiveOccupiedLedgerEntryIds();

    /**
     * 查询被非 VOID 对账单占用的所有分录 ID 集合（排除指定对账单）
     */
    @org.apache.ibatis.annotations.Select("SELECT DISTINCT item.ledger_entry_id FROM lunaerp_finance_statement_item item "
            + "JOIN lunaerp_finance_statement s ON item.statement_id = s.id "
            + "WHERE item.deleted = 0 AND s.deleted = 0 AND s.status != 'VOID' AND item.ledger_entry_id IS NOT NULL "
            + "AND s.id != #{excludeStatementId}")
    List<Long> selectActiveOccupiedLedgerEntryIdsExcluding(@org.apache.ibatis.annotations.Param("excludeStatementId") Long excludeStatementId);

    /**
     * 查询被非 VOID 对账单占用的分录及所属单号/状态详情
     */
    @org.apache.ibatis.annotations.Select("SELECT item.ledger_entry_id AS ledgerEntryId, s.statement_no AS statementNo, s.status AS status "
            + "FROM lunaerp_finance_statement_item item "
            + "JOIN lunaerp_finance_statement s ON item.statement_id = s.id "
            + "WHERE item.deleted = 0 AND s.deleted = 0 AND s.status != 'VOID' AND item.ledger_entry_id IS NOT NULL")
    List<java.util.Map<String, Object>> selectActiveOccupiedMapList();

    /**
     * 查询已删除客户对账单下的明细
     */
    @org.apache.ibatis.annotations.Select("SELECT * FROM lunaerp_finance_statement_item "
            + "WHERE statement_id = #{statementId} AND deleted = 1 AND tenant_id = #{tenantId} "
            + "ORDER BY id ASC")
    List<CustomerStatementItemDO> selectDeletedListByStatementId(
            @org.apache.ibatis.annotations.Param("statementId") Long statementId,
            @org.apache.ibatis.annotations.Param("tenantId") Long tenantId);

    /**
     * 级联恢复客户对账单明细
     */
    @org.apache.ibatis.annotations.Update("UPDATE lunaerp_finance_statement_item "
            + "SET deleted = 0, updater = #{updater}, update_time = #{updateTime} "
            + "WHERE statement_id = #{statementId} AND deleted = 1 AND tenant_id = #{tenantId}")
    int restoreByStatementId(@org.apache.ibatis.annotations.Param("statementId") Long statementId,
                             @org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                             @org.apache.ibatis.annotations.Param("updater") String updater,
                             @org.apache.ibatis.annotations.Param("updateTime") java.time.LocalDateTime updateTime);

    /**
     * 彻底物理删除客户对账单明细
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM lunaerp_finance_statement_item "
            + "WHERE statement_id = #{statementId} AND deleted = 1 AND tenant_id = #{tenantId}")
    int purgeByStatementId(@org.apache.ibatis.annotations.Param("statementId") Long statementId,
                           @org.apache.ibatis.annotations.Param("tenantId") Long tenantId);
}
