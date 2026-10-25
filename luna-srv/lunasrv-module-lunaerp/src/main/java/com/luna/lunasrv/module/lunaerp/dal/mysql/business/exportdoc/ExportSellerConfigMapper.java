package com.luna.lunasrv.module.lunaerp.dal.mysql.business.exportdoc;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc.ExportSellerConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务管理-卖方出口配置表 Mapper
 *
 * @author luna
 */
@Mapper
public interface ExportSellerConfigMapper extends BaseMapperX<ExportSellerConfigDO> {

    /**
     * 查询当前租户的卖方出口配置（一租户一行）
     *
     * @return 卖方出口配置，未配置时返回 null
     */
    default ExportSellerConfigDO selectByTenant() {
        return selectOne(new LambdaQueryWrapperX<ExportSellerConfigDO>().last("LIMIT 1"));
    }

}
