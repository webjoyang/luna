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

}
