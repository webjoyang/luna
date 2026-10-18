package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 财务管理 - 发票申请明细行表 Mapper
 *
 * @author luna
 */
@Mapper
public interface InvoiceApplicationItemMapper extends BaseMapperX<InvoiceApplicationItemDO> {
}
