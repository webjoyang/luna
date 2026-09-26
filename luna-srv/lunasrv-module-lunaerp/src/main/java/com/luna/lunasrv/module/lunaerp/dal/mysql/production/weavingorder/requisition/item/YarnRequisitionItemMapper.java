package com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.item;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.YarnRequisitionItemPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.item.YarnRequisitionItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 生产管理 - 领纱明细 Mapper（独立子资源）
 *
 * @author luna
 */
@Mapper
public interface YarnRequisitionItemMapper extends BaseMapperX<YarnRequisitionItemDO> {

    /**
     * 查询指定领纱单下所有有效明细
     *
     * @param requisitionId 领纱单ID
     * @return 明细列表
     */
    default List<YarnRequisitionItemDO> selectListByRequisitionId(Long requisitionId) {
        return selectList(new LambdaQueryWrapperX<YarnRequisitionItemDO>()
                .eq(YarnRequisitionItemDO::getRequisitionId, requisitionId)
                .orderByAsc(YarnRequisitionItemDO::getId));
    }

    /**
     * 查询多个领纱单下的有效明细
     *
     * @param requisitionIds 领纱单ID集合
     * @return 明细列表
     */
    default List<YarnRequisitionItemDO> selectListByRequisitionIds(Collection<Long> requisitionIds) {
        return selectList(new LambdaQueryWrapperX<YarnRequisitionItemDO>()
                .in(YarnRequisitionItemDO::getRequisitionId, requisitionIds)
                .orderByAsc(YarnRequisitionItemDO::getId));
    }

    /**
     * 按领纱单ID批量逻辑删除明细
     *
     * @param requisitionId 领纱单ID
     * @return 删除行数
     */
    default int deleteByRequisitionId(Long requisitionId) {
        return delete(new LambdaQueryWrapperX<YarnRequisitionItemDO>()
                .eq(YarnRequisitionItemDO::getRequisitionId, requisitionId));
    }

    /**
     * 批量按领纱单ID列表删除明细
     *
     * @param requisitionIds 领纱单ID集合
     * @return 删除行数
     */
    default int deleteByRequisitionIds(Collection<Long> requisitionIds) {
        return delete(new LambdaQueryWrapperX<YarnRequisitionItemDO>()
                .in(YarnRequisitionItemDO::getRequisitionId, requisitionIds));
    }

    /**
     * 统计指定领纱单下明细行数
     *
     * @param requisitionId 领纱单ID
     * @return 行数
     */
    default Long selectCountByRequisitionId(Long requisitionId) {
        return selectCount(new LambdaQueryWrapperX<YarnRequisitionItemDO>()
                .eq(YarnRequisitionItemDO::getRequisitionId, requisitionId));
    }

    /**
     * 分页查询明细
     *
     * @param reqVO 分页参数
     * @return 分页结果
     */
    default PageResult<YarnRequisitionItemDO> selectPage(YarnRequisitionItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<YarnRequisitionItemDO>()
                .eqIfPresent(YarnRequisitionItemDO::getRequisitionId, reqVO.getRequisitionId())
                .eqIfPresent(YarnRequisitionItemDO::getYarnTypeId, reqVO.getYarnTypeId())
                .eqIfPresent(YarnRequisitionItemDO::getYarnSpecId, reqVO.getYarnSpecId())
                .orderByAsc(YarnRequisitionItemDO::getId));
    }

}
