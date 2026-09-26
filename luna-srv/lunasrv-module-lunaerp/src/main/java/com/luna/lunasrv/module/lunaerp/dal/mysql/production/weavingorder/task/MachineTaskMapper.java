package com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 生产管理 - 机台任务 Mapper
 *
 * @author luna
 */
@Mapper
public interface MachineTaskMapper extends BaseMapperX<MachineTaskDO> {

    /**
     * 按编号前缀查询机台任务，【包含已逻辑删除的行】。
     *
     * 派生编号 MT-{织造单号去符号}-{单内 2 位序号} 租户内全量唯一，软删不释放编号。
     *
     * @param prefix 编号前缀，如 MT-ZZPB091900101-
     * @return 任务列表（含已删除行）
     */
    @Select("SELECT id, code, weaving_order_id, loom_id, loom_no, worker_user_id, worker_name, "
            + "planned_qty, actual_qty, planned_start, planned_end, status, halt_reason, halt_at, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_machine_task WHERE code LIKE CONCAT(#{prefix}, '%')")
    List<MachineTaskDO> selectListByCodePrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按任务编号精确查询，【包含已逻辑删除的行】。
     *
     * @param code 任务编号
     * @return 任务（含已删除行），不存在返回 null
     */
    @Select("SELECT id, code, weaving_order_id, loom_id, loom_no, worker_user_id, worker_name, "
            + "planned_qty, actual_qty, planned_start, planned_end, status, halt_reason, halt_at, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_machine_task WHERE code = #{code} LIMIT 1")
    MachineTaskDO selectByCodeIncludingDeleted(@Param("code") String code);

    /**
     * 查询指定织造单下所有机台任务
     *
     * @param weavingOrderId 织造单ID
     * @return 任务列表
     */
    default List<MachineTaskDO> selectListByWeavingOrderId(Long weavingOrderId) {
        return selectList(new LambdaQueryWrapperX<MachineTaskDO>()
                .eq(MachineTaskDO::getWeavingOrderId, weavingOrderId)
                .orderByAsc(MachineTaskDO::getId));
    }

    /**
     * 按织造单ID批量删除机台任务
     *
     * @param weavingOrderId 织造单ID
     * @return 删除行数
     */
    default int deleteByWeavingOrderId(Long weavingOrderId) {
        return delete(new LambdaQueryWrapperX<MachineTaskDO>()
                .eq(MachineTaskDO::getWeavingOrderId, weavingOrderId));
    }

    /**
     * 查询指定织造单下所有有效机台任务（未删除）
     *
     * @param weavingOrderId 织造单ID
     * @return 任务列表
     */
    default List<MachineTaskDO> selectActiveListByWeavingOrderId(Long weavingOrderId) {
        return selectList(new LambdaQueryWrapperX<MachineTaskDO>()
                .eq(MachineTaskDO::getWeavingOrderId, weavingOrderId));
    }

    /**
     * 查询指定织机被引用的机台任务数量（未删除）
     *
     * @param loomId 织机ID
     * @return 引用数量
     */
    default Long selectCountByLoomId(Long loomId) {
        return selectCount(new LambdaQueryWrapperX<MachineTaskDO>()
                .eq(MachineTaskDO::getLoomId, loomId));
    }

    /**
     * 分页查询机台任务
     *
     * @param reqVO 分页查询条件
     * @return 分页结果
     */
    default PageResult<MachineTaskDO> selectPage(MachineTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MachineTaskDO>()
                .eqIfPresent(MachineTaskDO::getWeavingOrderId, reqVO.getWeavingOrderId())
                .eqIfPresent(MachineTaskDO::getLoomId, reqVO.getLoomId())
                .eqIfPresent(MachineTaskDO::getWorkerUserId, reqVO.getWorkerUserId())
                .eqIfPresent(MachineTaskDO::getStatus, reqVO.getStatus())
                .likeIfPresent(MachineTaskDO::getCode, reqVO.getCode())
                .orderByDesc(MachineTaskDO::getId));
    }

}
