package com.luna.lunasrv.module.lunaerp.service.production.yarnorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo.*;
import jakarta.validation.Valid;

/**
 * 生产管理-纱线采购单 Service 接口
 *
 * @author luna
 */
public interface YarnOrderService {

    /**
     * 创建纱线采购单
     *
     * @param createReqVO 创建信息
     * @return 采购单ID
     */
    Long createYarnOrder(@Valid YarnOrderSaveReqVO createReqVO);

    /**
     * 更新纱线采购单（仅草稿态可更新，明细整体替换）
     *
     * @param updateReqVO 更新信息
     */
    void updateYarnOrder(@Valid YarnOrderSaveReqVO updateReqVO);

    /**
     * 删除纱线采购单（仅草稿态可删除，级联逻辑删除明细）
     *
     * @param id 采购单ID
     */
    void deleteYarnOrder(Long id);

    /**
     * 获得纱线采购单详情（内嵌明细及胚布单快照）
     *
     * @param id 采购单ID
     * @return 采购单详情
     */
    YarnOrderRespVO getYarnOrder(Long id);

    /**
     * 获得纱线采购单分页
     *
     * @param pageReqVO 分页查询条件
     * @return 采购单分页
     */
    PageResult<YarnOrderRespVO> getYarnOrderPage(YarnOrderPageReqVO pageReqVO);

    /**
     * 下达采购单（DRAFT -> ACTIVE）
     *
     * @param id 采购单ID
     */
    void effectYarnOrder(Long id);

    /**
     * 完成采购单（ACTIVE -> COMPLETED）
     *
     * @param id 采购单ID
     */
    void completeYarnOrder(Long id);

    /**
     * 取消采购单（ACTIVE -> CANCELLED）
     *
     * @param id 采购单ID
     */
    void cancelYarnOrder(Long id);

    /**
     * 核心测算：根据勾选的胚布单及损耗率测算纱线采购明细
     *
     * @param calculateReqVO 测算入参
     * @return 测算结果明细及胚布单快照
     */
    YarnOrderCalculateRespVO calculateYarnOrder(@Valid YarnOrderCalculateReqVO calculateReqVO);

    /**
     * 预览下一个采购单号（只读、不占号）
     *
     * @return 下一个单号，形如 YP20260920001
     */
    String getNextYarnOrderNo();

}
