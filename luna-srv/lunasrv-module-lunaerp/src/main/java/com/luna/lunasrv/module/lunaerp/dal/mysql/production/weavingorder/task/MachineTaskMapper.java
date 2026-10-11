package com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产管理 - 机台任务 Mapper
 *
 * @author luna
 */
@Mapper
public interface MachineTaskMapper extends BaseMapperX<MachineTaskDO> {

    /**
     * 级联恢复机台任务（手写 XML：显式 SET deleted=0 WHERE weaving_order_id=? AND deleted=1 AND tenant_id=?）
     */
    int restoreByWeavingOrderId(@Param("weavingOrderId") Long weavingOrderId, @Param("tenantId") Long tenantId,
                                @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除机台任务（手写 XML：显式 DELETE WHERE weaving_order_id=? AND deleted=1 AND tenant_id=?）
     */
    int purgeByWeavingOrderId(@Param("weavingOrderId") Long weavingOrderId, @Param("tenantId") Long tenantId);

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
                .orderByAsc(MachineTaskDO::getPlannedStart)
                .orderByDesc(MachineTaskDO::getId));
    }

    /**
     * 分页查询机台任务（支持限定指定 ID 集合，如冲突任务过滤；排序按 planned_start 升、id 降）
     *
     * @param reqVO 分页查询条件
     * @param includeIds 限定包含的 ID 集合（为 null 则不限定）
     * @return 分页结果
     */
    default PageResult<MachineTaskDO> selectPageWithIds(MachineTaskPageReqVO reqVO, java.util.Collection<Long> includeIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MachineTaskDO>()
                .eqIfPresent(MachineTaskDO::getWeavingOrderId, reqVO.getWeavingOrderId())
                .eqIfPresent(MachineTaskDO::getLoomId, reqVO.getLoomId())
                .eqIfPresent(MachineTaskDO::getWorkerUserId, reqVO.getWorkerUserId())
                .eqIfPresent(MachineTaskDO::getStatus, reqVO.getStatus())
                .likeIfPresent(MachineTaskDO::getCode, reqVO.getCode())
                .inIfPresent(MachineTaskDO::getId, includeIds)
                .orderByAsc(MachineTaskDO::getPlannedStart)
                .orderByDesc(MachineTaskDO::getId));
    }

    /**
     * 查询指定织机下占用态（PENDING / IN_PROGRESS / HALTED）且已排日期的机台任务
     *
     * @param loomId 织机ID
     * @return 任务列表
     */
    default List<MachineTaskDO> selectOccupiedTasksByLoomId(Long loomId) {
        return selectOccupiedTasksByLoomIds(loomId != null ? List.of(loomId) : null);
    }

    /**
     * 查询指定织机列表（或全部织机）下占用态（PENDING / IN_PROGRESS / HALTED）且已排日期的机台任务
     *
     * @param loomIds 织机ID集合（为 null 时查询全部）
     * @return 任务列表
     */
    default List<MachineTaskDO> selectOccupiedTasksByLoomIds(java.util.Collection<Long> loomIds) {
        return selectList(new LambdaQueryWrapperX<MachineTaskDO>()
                .inIfPresent(MachineTaskDO::getLoomId, loomIds)
                .in(MachineTaskDO::getStatus, List.of("PENDING", "IN_PROGRESS", "HALTED"))
                .and(w -> w.isNotNull(MachineTaskDO::getPlannedStart).or().isNotNull(MachineTaskDO::getPlannedEnd))
                .orderByAsc(MachineTaskDO::getPlannedStart)
                .orderByAsc(MachineTaskDO::getId));
    }

    /**
     * 查询指定织机下指定状态列表的机台任务
     *
     * @param loomId 织机ID
     * @param statuses 状态列表
     * @return 任务列表
     */
    default List<MachineTaskDO> selectListByLoomIdAndStatuses(Long loomId, java.util.Collection<String> statuses) {
        return selectList(new LambdaQueryWrapperX<MachineTaskDO>()
                .eq(MachineTaskDO::getLoomId, loomId)
                .inIfPresent(MachineTaskDO::getStatus, statuses)
                .orderByAsc(MachineTaskDO::getPlannedStart)
                .orderByAsc(MachineTaskDO::getId));
    }

}
