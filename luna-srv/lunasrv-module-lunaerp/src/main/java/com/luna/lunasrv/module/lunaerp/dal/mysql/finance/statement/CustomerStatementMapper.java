package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 客户对账单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface CustomerStatementMapper extends BaseMapperX<CustomerStatementDO> {

    /**
     * 按单号查询对账单
     */
    default CustomerStatementDO selectByStatementNo(String statementNo) {
        return selectOne(new LambdaQueryWrapperX<CustomerStatementDO>()
                .eq(CustomerStatementDO::getStatementNo, statementNo));
    }

    /**
     * 按客户ID和状态集合查询对账单列表
     */
    default List<CustomerStatementDO> selectListByCustomerIdAndStatuses(Long customerId, Collection<String> statuses) {
        if (CollUtil.isEmpty(statuses)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<CustomerStatementDO>()
                .eq(CustomerStatementDO::getCustomerId, customerId)
                .in(CustomerStatementDO::getStatus, statuses)
                .orderByDesc(CustomerStatementDO::getPeriodEnd)
                .orderByDesc(CustomerStatementDO::getId));
    }

    /**
     * 按单号精确查询对账单（含已删除行，用于手填查重与自动号防撞）
     */
    @Select("SELECT id, statement_no, customer_id, customer_name, currency, period_start, period_end, "
            + "opening_balance, period_debit, period_credit, closing_balance, status, confirm_type, "
            + "confirm_by_name, confirm_time, confirm_ip, confirm_method, reject_reason, reply_remark, "
            + "remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_statement WHERE statement_no = #{statementNo} LIMIT 1")
    CustomerStatementDO selectByStatementNoIncludingDeleted(@Param("statementNo") String statementNo);

    /**
     * 按单号前缀查询对账单列表（含已删除行，用于按年流水号扫描）
     */
    @Select("SELECT id, statement_no, customer_id, customer_name, currency, period_start, period_end, "
            + "opening_balance, period_debit, period_credit, closing_balance, status, confirm_type, "
            + "confirm_by_name, confirm_time, confirm_ip, confirm_method, reject_reason, reply_remark, "
            + "remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_statement WHERE statement_no LIKE CONCAT(#{prefix}, '%')")
    List<CustomerStatementDO> selectListByStatementNoPrefixIncludingDeleted(@Param("prefix") String prefix);

}
