package com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item.InspectionOrderItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产管理-检验单录检明细表 Mapper
 *
 * 录检明细作为独立子资源，分页按 inspectionOrderId 过滤，按 id 倒序。
 * 逻辑删除（deleted=0）与租户条件由框架自动追加。
 *
 * @author luna
 */
@Mapper
public interface InspectionOrderItemMapper extends BaseMapperX<InspectionOrderItemDO> {

    /**
     * 录检明细分页查询：按 inspectionOrderId 等值过滤，按 id 倒序
     *
     * @param reqVO 分页与查询条件（inspectionOrderId 必填）
     * @return 录检明细分页
     */
    default PageResult<InspectionOrderItemDO> selectPage(InspectionOrderItemPageReqVO reqVO) {
        LambdaQueryWrapperX<InspectionOrderItemDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(InspectionOrderItemDO::getInspectionOrderId, reqVO.getInspectionOrderId());
        wrapper.orderByDesc(InspectionOrderItemDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 根据检验单ID、缸号、卷号查询明细
     *
     * @param inspectionOrderId 检验单ID
     * @param vatNo 缸号
     * @param rollNo 卷号
     * @return 录检明细
     */
    default InspectionOrderItemDO selectByInspectionOrderIdAndVatNoAndRollNo(Long inspectionOrderId, String vatNo, String rollNo) {
        return selectOne(new LambdaQueryWrapperX<InspectionOrderItemDO>()
                .eq(InspectionOrderItemDO::getInspectionOrderId, inspectionOrderId)
                .eq(InspectionOrderItemDO::getVatNo, vatNo)
                .eq(InspectionOrderItemDO::getRollNo, rollNo));
    }

    /**
     * 根据检验单ID、缸号、匹号查询明细（PIECE 模式查重，ADR-0025）
     *
     * @param inspectionOrderId 检验单ID
     * @param vatNo 缸号
     * @param pieceNo 匹号
     * @return 录检明细
     */
    default InspectionOrderItemDO selectByInspectionOrderIdAndVatNoAndPieceNo(Long inspectionOrderId, String vatNo, String pieceNo) {
        return selectOne(new LambdaQueryWrapperX<InspectionOrderItemDO>()
                .eq(InspectionOrderItemDO::getInspectionOrderId, inspectionOrderId)
                .eq(InspectionOrderItemDO::getVatNo, vatNo)
                .eq(InspectionOrderItemDO::getPieceNo, pieceNo));
    }

    /**
     * 查询检验单下的明细数量
     *
     * @param inspectionOrderId 检验单ID
     * @return 明细数量
     */
    default Long selectCountByInspectionOrderId(Long inspectionOrderId) {
        return selectCount(InspectionOrderItemDO::getInspectionOrderId, inspectionOrderId);
    }

    /**
     * 批量查询多个检验单下的明细数量（分组统计，避免 N+1 查询）
     *
     * @param inspectionOrderIds 检验单ID集合
     * @return 检验单ID -> 明细数量 Map
     */
    default java.util.Map<Long, Integer> selectCountMapByInspectionOrderIds(java.util.Collection<Long> inspectionOrderIds) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(inspectionOrderIds)) {
            return java.util.Collections.emptyMap();
        }
        java.util.List<InspectionOrderItemDO> list = selectList(new LambdaQueryWrapperX<InspectionOrderItemDO>()
                .in(InspectionOrderItemDO::getInspectionOrderId, inspectionOrderIds)
                .select(InspectionOrderItemDO::getInspectionOrderId, InspectionOrderItemDO::getId));
        java.util.Map<Long, Integer> countMap = new java.util.HashMap<>();
        for (InspectionOrderItemDO item : list) {
            countMap.merge(item.getInspectionOrderId(), 1, Integer::sum);
        }
        return countMap;
    }

    /**
     * 根据检验单ID批量逻辑删除明细（级联删除用）
     *
     * @param inspectionOrderId 检验单ID
     * @return 删除行数
     */
    default int deleteByInspectionOrderId(Long inspectionOrderId) {
        return delete(new LambdaQueryWrapperX<InspectionOrderItemDO>()
                .eq(InspectionOrderItemDO::getInspectionOrderId, inspectionOrderId));
    }


}
