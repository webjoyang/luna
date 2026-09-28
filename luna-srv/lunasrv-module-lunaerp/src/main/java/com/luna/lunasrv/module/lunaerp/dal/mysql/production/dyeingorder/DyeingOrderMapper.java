package com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 生产管理-染色单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface DyeingOrderMapper extends BaseMapperX<DyeingOrderDO> {

    /**
     * 查询染色单列表，【包含已逻辑删除的行】（用于进度反馈"含已失效"视图）。
     *
     * 手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param dyeingOrderId   染色单ID
     * @param contractNo      合同号模糊匹配
     * @param dyeingFactoryId 承染方供应商ID
     * @return 染色单列表（含已删除行）
     */
    @Select("<script>"
            + "SELECT id, sales_contract_id, contract_no, customer_id, client_name, dyeing_factory_id, dyeing_factory_name, "
            + "order_date, delivery_date, product_code, product_name, composition, width, gsm, shrinkage, loss, ph, "
            + "fastness_wash, fastness_soap, fastness_dry_rub, fastness_wet_rub, fastness_light, fastness_perspiration, "
            + "fastness_dry_clean, fastness_six_fiber, other_req, remark, status, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_dyeing_order "
            + "<where>"
            + "  <if test='dyeingOrderId != null'> AND id = #{dyeingOrderId} </if>"
            + "  <if test='contractNo != null and contractNo != \"\"'> AND contract_no LIKE CONCAT('%', #{contractNo}, '%') </if>"
            + "  <if test='dyeingFactoryId != null'> AND dyeing_factory_id = #{dyeingFactoryId} </if>"
            + "  AND status IN ('ACTIVE', 'COMPLETED') "
            + "</where>"
            + "ORDER BY order_date DESC, id DESC"
            + "</script>")
    List<DyeingOrderDO> selectListIncludingDeleted(@Param("dyeingOrderId") Long dyeingOrderId,
                                                   @Param("contractNo") String contractNo,
                                                   @Param("dyeingFactoryId") Long dyeingFactoryId);

}

