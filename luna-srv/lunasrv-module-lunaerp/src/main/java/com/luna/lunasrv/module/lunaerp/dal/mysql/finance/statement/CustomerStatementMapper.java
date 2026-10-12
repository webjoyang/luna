package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementDO;
import org.apache.ibatis.annotations.Mapper;

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

}
