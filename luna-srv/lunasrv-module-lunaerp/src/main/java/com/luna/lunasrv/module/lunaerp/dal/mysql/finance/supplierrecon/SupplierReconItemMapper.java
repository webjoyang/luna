package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 财务管理 - 供应商对账单行表 Mapper
 *
 * <p>遵循 docs/agents/coding-standards.md 第 4 节规约：
 * 保持 Mapper 纯粹性，业务与集合空安全由 Service 层负责，不写冗余 default 包装方法。</p>
 *
 * @author luna
 */
@Mapper
public interface SupplierReconItemMapper extends BaseMapperX<SupplierReconItemDO> {
}
