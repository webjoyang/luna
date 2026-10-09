package com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.vo.YarnRequisitionPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.YarnRequisitionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产管理 - 领纱单 Mapper
 *
 * @author luna
 */
@Mapper
public interface YarnRequisitionMapper extends BaseMapperX<YarnRequisitionDO> {

    /**
     * 级联恢复领纱单（手写 XML：显式 SET deleted=0 WHERE weaving_order_id=? AND deleted=1 AND tenant_id=?）
     */
    int restoreByWeavingOrderId(@Param("weavingOrderId") Long weavingOrderId, @Param("tenantId") Long tenantId,
                                @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除领纱单（手写 XML：显式 DELETE WHERE weaving_order_id=? AND deleted=1 AND tenant_id=?）
     */
    int purgeByWeavingOrderId(@Param("weavingOrderId") Long weavingOrderId, @Param("tenantId") Long tenantId);

    /**
     * 按编号前缀查询领纱单，【包含已逻辑删除的行】。
     *
     * 派生编号 YS-{织造单号去符号}-{单内 2 位序号} 租户内全量唯一，软删不释放编号。
     *
     * @param prefix 编号前缀，如 YS-ZZPB091900101-
     * @return 领纱单列表（含已删除行）
     */
    @Select("SELECT id, code, weaving_order_id, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_yarn_requisition WHERE code LIKE CONCAT(#{prefix}, '%')")
    List<YarnRequisitionDO> selectListByCodePrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按领纱单号精确查询，【包含已逻辑删除的行】。
     *
     * @param code 领纱单号
     * @return 领纱单（含已删除行），不存在返回 null
     */
    @Select("SELECT id, code, weaving_order_id, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_yarn_requisition WHERE code = #{code} LIMIT 1")
    YarnRequisitionDO selectByCodeIncludingDeleted(@Param("code") String code);

    /**
     * 查询指定织造单下所有有效领纱单（未删除）
     *
     * @param weavingOrderId 织造单ID
     * @return 领纱单列表
     */
    default List<YarnRequisitionDO> selectListByWeavingOrderId(Long weavingOrderId) {
        return selectList(new LambdaQueryWrapperX<YarnRequisitionDO>()
                .eq(YarnRequisitionDO::getWeavingOrderId, weavingOrderId)
                .orderByAsc(YarnRequisitionDO::getId));
    }

    /**
     * 按织造单ID批量删除领纱单
     *
     * @param weavingOrderId 织造单ID
     * @return 删除行数
     */
    default int deleteByWeavingOrderId(Long weavingOrderId) {
        return delete(new LambdaQueryWrapperX<YarnRequisitionDO>()
                .eq(YarnRequisitionDO::getWeavingOrderId, weavingOrderId));
    }

    /**
     * 分页查询领纱单
     *
     * @param reqVO 分页查询条件
     * @return 分页结果
     */
    default PageResult<YarnRequisitionDO> selectPage(YarnRequisitionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<YarnRequisitionDO>()
                .eqIfPresent(YarnRequisitionDO::getWeavingOrderId, reqVO.getWeavingOrderId())
                .eqIfPresent(YarnRequisitionDO::getStatus, reqVO.getStatus())
                .likeIfPresent(YarnRequisitionDO::getCode, reqVO.getCode())
                .betweenIfPresent(YarnRequisitionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(YarnRequisitionDO::getId));
    }

}
