package com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产管理-胚布单主表 Mapper
 *
 * 取号/查重的"包含已删除行"查询随 02 票编号规则（ADR-0006）一起接入。
 *
 * @author luna
 */
@Mapper
public interface GreigeOrderMapper extends BaseMapperX<GreigeOrderDO> {

}
