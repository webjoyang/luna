package com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 生产管理-染色单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface DyeingOrderMapper extends BaseMapperX<DyeingOrderDO> {

    /**
     * 回收站分页查询已软删草稿染色单（手写 XML：显式 WHERE deleted = 1 AND status = 'DRAFT' AND tenant_id = #{tenantId}）
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿染色单
     */
    DyeingOrderDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿染色单（并发条件更新带 deleted=1 AND status='DRAFT' AND tenant_id=?）
     */
    int restoreDyeingOrder(@Param("id") Long id, @Param("tenantId") Long tenantId,
                           @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除草稿染色单（物理删除带 deleted=1 AND status='DRAFT' AND tenant_id=?）
     */
    int purgeDyeingOrder(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查染色单是否存在活跃业务引用（deleted=0 且 tenant_id=?）
     */
    Long selectActiveReferenceCount(@Param("dyeingOrderId") Long dyeingOrderId, @Param("tenantId") Long tenantId);

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

    /**
     * 按销售合同ID集合批量查询染色单（生产进度聚合用，ADR-0034）。
     *
     * <p>返回未加工列表（含各状态），CANCELLED 排除由 Service 内存完成，便于单测。</p>
     *
     * @param contractIds 销售合同ID集合
     * @return 染色单列表
     */
    default List<DyeingOrderDO> selectListByContractIds(Collection<Long> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<DyeingOrderDO>()
                .in(DyeingOrderDO::getSalesContractId, contractIds));
    }

    /**
     * 按染色单ID集合批量查询染色单
     *
     * @param ids 染色单ID集合
     * @return 染色单列表
     */
    default List<DyeingOrderDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<DyeingOrderDO>()
                .in(DyeingOrderDO::getId, ids));
    }

}

