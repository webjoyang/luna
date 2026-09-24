package com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouselocation;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouselocation.vo.WarehouseLocationPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouselocation.WarehouseLocationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

/**
 * 库存管理 - 库位 Mapper
 *
 * 同一仓库内 code 唯一（含软删行，ADR-0018）。
 * 手写 SQL 绕过逻辑删除自动过滤用于查重；tenant_id 由租户拦截器自动追加。
 *
 * @author luna
 */
@Mapper
public interface WarehouseLocationMapper extends BaseMapperX<WarehouseLocationDO> {

    default PageResult<WarehouseLocationDO> selectPage(WarehouseLocationPageReqVO reqVO) {
        LambdaQueryWrapperX<WarehouseLocationDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(WarehouseLocationDO::getWarehouseId, reqVO.getWarehouseId());
        wrapper.eqIfPresent(WarehouseLocationDO::getStatus, reqVO.getStatus());
        String search = reqVO.getSearch() == null ? "" : reqVO.getSearch().trim();
        if (StringUtils.hasText(search)) {
            String like = "%" + search + "%";
            wrapper.and(w -> w
                    .apply("LOWER(code) LIKE LOWER({0})", like)
                    .or().apply("LOWER(name) LIKE LOWER({0})", like));
        }
        wrapper.orderByAsc(WarehouseLocationDO::getSort)
                .orderByAsc(WarehouseLocationDO::getCode)
                .orderByDesc(WarehouseLocationDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 按仓库ID统计未删除的库位数量（用于仓库删除守卫）
     */
    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(WarehouseLocationDO::getWarehouseId, warehouseId);
    }

    /**
     * 按仓库ID和库位编码精确查询库位，【包含已逻辑删除的行】（用于仓内 code 唯一查重）。
     */
    @Select("SELECT id, warehouse_id, code, name, sort, status, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_stock_warehouse_location WHERE warehouse_id = #{warehouseId} AND code = #{code} LIMIT 1")
    WarehouseLocationDO selectByWarehouseIdAndCodeIncludingDeleted(@Param("warehouseId") Long warehouseId, @Param("code") String code);

}
