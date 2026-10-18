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

    /**
     * 级联恢复对账单明细
     *
     * @param statementId 对账单ID
     * @param tenantId 租户编号
     * @param updater 更新人
     * @param now 更新时间
     * @return 影响行数
     */
    int restoreByStatementId(@org.apache.ibatis.annotations.Param("statementId") Long statementId,
                            @org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
                            @org.apache.ibatis.annotations.Param("updater") String updater,
                            @org.apache.ibatis.annotations.Param("now") java.time.LocalDateTime now);

    /**
     * 彻底物理删除对账单明细
     *
     * @param statementId 对账单ID
     * @param tenantId 租户编号
     * @return 影响行数
     */
    int purgeByStatementId(@org.apache.ibatis.annotations.Param("statementId") Long statementId,
                          @org.apache.ibatis.annotations.Param("tenantId") Long tenantId);

}

