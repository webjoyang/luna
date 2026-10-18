package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 财务管理 - 发票申请明细行表 Mapper
 *
 * @author luna
 */
@Mapper
public interface InvoiceApplicationItemMapper extends BaseMapperX<InvoiceApplicationItemDO> {

    /**
     * 级联恢复发票申请明细
     *
     * @param applicationId 发票申请ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param now 更新时间
     * @return 影响行数
     */
    int restoreByApplicationId(@Param("applicationId") Long applicationId, @Param("tenantId") Long tenantId,
                               @Param("updater") String updater, @Param("now") LocalDateTime now);

    /**
     * 彻底物理删除发票申请明细
     *
     * @param applicationId 发票申请ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    int purgeByApplicationId(@Param("applicationId") Long applicationId, @Param("tenantId") Long tenantId);

}
