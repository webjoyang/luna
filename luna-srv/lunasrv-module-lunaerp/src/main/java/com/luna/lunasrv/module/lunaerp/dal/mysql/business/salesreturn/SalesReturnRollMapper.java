package com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnRollDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 业务管理 - 销售退货卷明细 Mapper
 *
 * @author luna
 */
@Mapper
public interface SalesReturnRollMapper extends BaseMapperX<SalesReturnRollDO> {

    /**
     * 级联恢复销售退货卷清单（显式 SET deleted=0 WHERE return_id=? AND deleted=1 AND tenant_id=?）
     */
    int restoreByReturnId(@Param("returnId") Long returnId, @Param("tenantId") Long tenantId,
                          @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除销售退货卷清单（显式 DELETE WHERE return_id=? AND deleted=1 AND tenant_id=?）
     */
    int purgeByReturnId(@Param("returnId") Long returnId, @Param("tenantId") Long tenantId);

}
