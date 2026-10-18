package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务管理 - 客户对账单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface CustomerStatementMapper extends BaseMapperX<CustomerStatementDO> {

    String COLUMNS = "id, statement_no, customer_id, customer_name, currency, "
            + "period_start, period_end, opening_balance, period_debit, period_credit, closing_balance, "
            + "status, confirm_type, confirm_by_name, confirm_time, confirm_ip, confirm_method, "
            + "reject_reason, reply_remark, remark, creator, create_time, updater, update_time, deleted, tenant_id";

    /**
     * 按对账单号精确查询（含已软删除记录，用于取号防撞）
     *
     * @param statementNo 对账单号
     * @return 对账单（含已删除记录）
     */
    @Select("SELECT " + COLUMNS + " FROM lunaerp_finance_statement WHERE statement_no = #{statementNo} LIMIT 1")
    CustomerStatementDO selectByStatementNoIncludingDeleted(@Param("statementNo") String statementNo);

    /**
     * 按对账单号前缀查询列表（含已软删除记录，用于发号扫描）
     *
     * @param prefix 单号前缀
     * @return 对账单列表（含已删除记录）
     */
    @Select("SELECT " + COLUMNS + " FROM lunaerp_finance_statement WHERE statement_no LIKE CONCAT(#{prefix}, '%')")
    List<CustomerStatementDO> selectListByStatementNoPrefixIncludingDeleted(@Param("prefix") String prefix);

}
