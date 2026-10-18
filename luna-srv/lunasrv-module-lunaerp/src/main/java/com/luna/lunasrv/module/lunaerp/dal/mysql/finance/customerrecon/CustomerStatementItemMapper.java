package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 财务管理 - 客户对账单明细行快照 Mapper
 *
 * @author luna
 */
@Mapper
public interface CustomerStatementItemMapper extends BaseMapperX<CustomerStatementItemDO> {
}
