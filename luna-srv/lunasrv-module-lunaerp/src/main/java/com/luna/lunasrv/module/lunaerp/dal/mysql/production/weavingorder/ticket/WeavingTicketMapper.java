package com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.WeavingTicketPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产管理 - 织造布票 Mapper
 *
 * @author luna
 */
@Mapper
public interface WeavingTicketMapper extends BaseMapperX<WeavingTicketDO> {

    /**
     * 级联恢复布票（手写 XML：显式 SET deleted=0 WHERE weaving_order_id=? AND deleted=1 AND tenant_id=?）
     */
    int restoreByWeavingOrderId(@Param("weavingOrderId") Long weavingOrderId, @Param("tenantId") Long tenantId,
                                @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除布票（手写 XML：显式 DELETE WHERE weaving_order_id=? AND deleted=1 AND tenant_id=?）
     */
    int purgeByWeavingOrderId(@Param("weavingOrderId") Long weavingOrderId, @Param("tenantId") Long tenantId);

    /**
     * 按织造单ID查询所有布票编号，【包含已逻辑删除和作废的行】。
     *
     * 派生编号 BT-{机台任务号去符号}-{4 位流水} 按织造单维度计数、作废占号不重用。
     *
     * @param weavingOrderId 织造单ID
     * @return 该织造单下全量布票编号列表
     */
    @Select("SELECT code FROM lunaerp_production_weaving_ticket WHERE weaving_order_id = #{weavingOrderId}")
    List<String> selectCodesByWeavingOrderIdIncludingDeleted(@Param("weavingOrderId") Long weavingOrderId);

    /**
     * 按布票编号精确查询，【包含已逻辑删除的行】。
     *
     * @param code 布票编号
     * @return 布票（含已删除行），不存在返回 null
     */
    @Select("SELECT id, code, weaving_order_id, machine_task_id, planned_machine, actual_machine, "
            + "planned_weight, worker_user_id, worker_name, material_batch_no, status, "
            + "exception_type, exception_note, started_at, completed_at, void_reason, voided_at, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_weaving_ticket WHERE code = #{code} LIMIT 1")
    WeavingTicketDO selectByCodeIncludingDeleted(@Param("code") String code);

    /**
     * 按布票编号查询有效布票（未删除）
     *
     * @param code 布票编号
     * @return 布票，不存在返回 null
     */
    default WeavingTicketDO selectByCode(String code) {
        return selectOne(WeavingTicketDO::getCode, code);
    }

    /**
     * 查询指定机台任务下所有有效布票（未删除）
     *
     * @param machineTaskId 机台任务ID
     * @return 布票列表
     */
    default List<WeavingTicketDO> selectListByMachineTaskId(Long machineTaskId) {
        return selectList(new LambdaQueryWrapperX<WeavingTicketDO>()
                .eq(WeavingTicketDO::getMachineTaskId, machineTaskId)
                .orderByAsc(WeavingTicketDO::getId));
    }

    /**
     * 查询指定织造单下所有有效布票（未删除）
     *
     * @param weavingOrderId 织造单ID
     * @return 布票列表
     */
    default List<WeavingTicketDO> selectListByWeavingOrderId(Long weavingOrderId) {
        return selectList(new LambdaQueryWrapperX<WeavingTicketDO>()
                .eq(WeavingTicketDO::getWeavingOrderId, weavingOrderId)
                .orderByAsc(WeavingTicketDO::getId));
    }

    /**
     * 按织造单ID批量删除布票
     *
     * @param weavingOrderId 织造单ID
     * @return 删除行数
     */
    default int deleteByWeavingOrderId(Long weavingOrderId) {
        return delete(new LambdaQueryWrapperX<WeavingTicketDO>()
                .eq(WeavingTicketDO::getWeavingOrderId, weavingOrderId));
    }

    /**
     * 按机台任务ID批量删除布票
     *
     * @param machineTaskId 机台任务ID
     * @return 删除行数
     */
    default int deleteByMachineTaskId(Long machineTaskId) {
        return delete(new LambdaQueryWrapperX<WeavingTicketDO>()
                .eq(WeavingTicketDO::getMachineTaskId, machineTaskId));
    }

    /**
     * 分页查询布票
     *
     * @param reqVO 分页查询条件
     * @return 分页结果
     */
    default PageResult<WeavingTicketDO> selectPage(WeavingTicketPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WeavingTicketDO>()
                .eqIfPresent(WeavingTicketDO::getWeavingOrderId, reqVO.getWeavingOrderId())
                .eqIfPresent(WeavingTicketDO::getMachineTaskId, reqVO.getMachineTaskId())
                .eqIfPresent(WeavingTicketDO::getStatus, reqVO.getStatus())
                .likeIfPresent(WeavingTicketDO::getCode, reqVO.getCode())
                .likeIfPresent(WeavingTicketDO::getPlannedMachine, reqVO.getPlannedMachine())
                .likeIfPresent(WeavingTicketDO::getActualMachine, reqVO.getActualMachine())
                .likeIfPresent(WeavingTicketDO::getWorkerName, reqVO.getWorkerName())
                .orderByDesc(WeavingTicketDO::getId));
    }

}
