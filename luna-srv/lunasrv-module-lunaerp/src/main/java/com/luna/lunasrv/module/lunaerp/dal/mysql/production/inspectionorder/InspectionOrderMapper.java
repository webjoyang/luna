package com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 生产管理-检验单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface InspectionOrderMapper extends BaseMapperX<InspectionOrderDO> {

    /**
     * 按单号前缀查询检验单，【包含已逻辑删除的行】。
     *
     * ADR-0015：检验单号 (tenant_id, code) 全量唯一、软删不释放编号，
     * 取号扫描必须覆盖已删除行，否则软删单据的编号会被重新分配。
     * 手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param prefix 单号前缀，如 INS-20260921-
     * @return 检验单列表（含已删除行）
     */
    @Select("SELECT id, code, type, status, product_id, product_code, product_name, "
            + "greige_order_id, dyeing_order_id, inspection_factory_id, inspection_factory_name, "
            + "sales_contract_id, contract_no, target_weight, remark, order_date, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_inspection_order WHERE code LIKE CONCAT(#{prefix}, '%')")
    List<InspectionOrderDO> selectListByCodePrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按单号精确查询检验单，【包含已逻辑删除的行】。
     *
     * @param code 检验单号
     * @return 检验单（含已删除行），不存在返回 null
     */
    @Select("SELECT id, code, type, status, product_id, product_code, product_name, "
            + "greige_order_id, dyeing_order_id, inspection_factory_id, inspection_factory_name, "
            + "sales_contract_id, contract_no, target_weight, remark, order_date, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_inspection_order WHERE code = #{code} LIMIT 1")
    InspectionOrderDO selectByCodeIncludingDeleted(@Param("code") String code);

}
