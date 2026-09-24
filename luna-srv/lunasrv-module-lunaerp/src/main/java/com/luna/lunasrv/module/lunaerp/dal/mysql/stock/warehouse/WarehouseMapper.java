package com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.stock.warehouse.vo.WarehousePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 库存管理 - 仓库 Mapper
 *
 * 列表 search 智能词命中 warehouse_no / name / address 之一，
 * PostgreSQL 用 LOWER(...) LIKE LOWER({0}) 实现 OR ILIKE 语义。
 * 逻辑删除（deleted=0）与租户条件由框架自动追加。
 *
 * 编号取号与查重扫描【包含已逻辑删除的行】（ADR-0005, ADR-0018）：手写 SQL 绕过 MyBatis-Plus
 * 逻辑删除自动过滤，tenant_id 由租户拦截器自动追加。
 *
 * @author luna
 */
@Mapper
public interface WarehouseMapper extends BaseMapperX<WarehouseDO> {

    default PageResult<WarehouseDO> selectPage(WarehousePageReqVO reqVO) {
        LambdaQueryWrapperX<WarehouseDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(WarehouseDO::getStatus, reqVO.getStatus());
        String search = reqVO.getSearch() == null ? "" : reqVO.getSearch().trim();
        if (StringUtils.hasText(search)) {
            String like = "%" + search + "%";
            wrapper.and(w -> w
                    .apply("LOWER(warehouse_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(address) LIKE LOWER({0})", like));
        }
        wrapper.orderByDesc(WarehouseDO::getCreateTime).orderByDesc(WarehouseDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 按编号前缀查询仓库，【包含已逻辑删除的行】（用于按年取号扫描）。
     */
    @Select("SELECT id, warehouse_no, name, address, status, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_stock_warehouse WHERE warehouse_no LIKE CONCAT(#{prefix}, '%')")
    List<WarehouseDO> selectListByWarehouseNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按编号精确查询仓库，【包含已逻辑删除的行】（用于手填编号查重）。
     */
    @Select("SELECT id, warehouse_no, name, address, status, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_stock_warehouse WHERE warehouse_no = #{warehouseNo} LIMIT 1")
    WarehouseDO selectByWarehouseNoIncludingDeleted(@Param("warehouseNo") String warehouseNo);

}
