package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition.item;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.*;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

/**
 * 生产管理 - 领纱明细 Service 接口（独立子资源）
 *
 * @author luna
 */
public interface YarnRequisitionItemService {

    /**
     * 创建领纱明细
     *
     * @param createReqVO 创建信息
     * @return 明细编号
     */
    Long createYarnRequisitionItem(@Valid YarnRequisitionItemSaveReqVO createReqVO);

    /**
     * 更新领纱明细
     *
     * @param updateReqVO 更新信息
     */
    void updateYarnRequisitionItem(@Valid YarnRequisitionItemSaveReqVO updateReqVO);

    /**
     * 删除领纱明细
     *
     * @param id 明细编号
     */
    void deleteYarnRequisitionItem(Long id);

    /**
     * 获得领纱明细详情
     *
     * @param id 明细编号
     * @return 明细详情
     */
    YarnRequisitionItemRespVO getYarnRequisitionItem(Long id);

    /**
     * 获得领纱明细分页
     *
     * @param pageReqVO 分页查询条件
     * @return 明细分页
     */
    PageResult<YarnRequisitionItemRespVO> getYarnRequisitionItemPage(YarnRequisitionItemPageReqVO pageReqVO);

    /**
     * 查询指定领纱单下所有有效明细
     *
     * @param requisitionId 领纱单ID
     * @return 明细列表
     */
    List<YarnRequisitionItemRespVO> getItemListByRequisitionId(Long requisitionId);

    /**
     * 领纱明细发纱（同事务 YARN_OUT + dispatchedQty + 状态推导）
     *
     * @param dispatchReqVO 发纱请求信息
     */
    void dispatchYarn(@Valid YarnRequisitionDispatchReqVO dispatchReqVO);

    /**
     * 撤销发纱（按发纱记录红冲 YARN_OUT_REVERSAL + 回减 dispatchedQty + 状态回退）
     *
     * @param reversalReqVO 撤销红冲请求
     */
    void reverseDispatch(@Valid YarnRequisitionDispatchReversalReqVO reversalReqVO);

    /**
     * 获取指定领纱明细的发纱记录列表
     *
     * @param itemId 领纱明细ID
     * @return 发纱记录列表
     */
    List<YarnDispatchRecordRespVO> getDispatchRecordList(Long itemId);

    /**
     * 查询某仓库下某纱线规格的可用库存余额
     *
     * @param warehouseId 仓库ID
     * @param yarnSpecId 纱线规格ID
     * @return 可用库存数量
     */
    BigDecimal getYarnBalance(Long warehouseId, Long yarnSpecId);

}
