package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.vo.*;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.WeavingOrderCascadeDeleteListener;
import jakarta.validation.Valid;

/**
 * 生产管理 - 领纱单 Service 接口
 *
 * @author luna
 */
public interface YarnRequisitionService extends WeavingOrderCascadeDeleteListener {

    /**
     * 创建领纱单（code 留空自动派生 YS-{织造单号去符号}-{2位流水}）
     *
     * @param createReqVO 创建信息
     * @return 领纱单ID
     */
    Long createYarnRequisition(@Valid YarnRequisitionSaveReqVO createReqVO);

    /**
     * 更新领纱单（仅草稿 DRAFT 态允许修改）
     *
     * @param updateReqVO 更新信息
     */
    void updateYarnRequisition(@Valid YarnRequisitionSaveReqVO updateReqVO);

    /**
     * 删除领纱单（仅草稿 DRAFT 态允许删除，级联逻辑删除明细）
     *
     * @param id 领纱单ID
     */
    void deleteYarnRequisition(Long id);

    /**
     * 获得领纱单详情（含明细列表）
     *
     * @param id 领纱单ID
     * @return 领纱单详情
     */
    YarnRequisitionRespVO getYarnRequisition(Long id);

    /**
     * 获得领纱单分页
     *
     * @param pageReqVO 分页查询条件
     * @return 领纱单分页
     */
    PageResult<YarnRequisitionRespVO> getYarnRequisitionPage(YarnRequisitionPageReqVO pageReqVO);

    /**
     * 预览下一个领纱单号（只读、不占号）
     *
     * @param weavingOrderId 织造单ID
     * @return 领纱单号
     */
    String getNextYarnRequisitionCode(Long weavingOrderId);

    /**
     * 提交领纱单（DRAFT → ACTIVE 守卫流转）
     *
     * @param id 领纱单ID
     */
    void submitYarnRequisition(Long id);

    /**
     * 关闭领纱单（转 CLOSED 手动终态）
     *
     * @param id 领纱单ID
     */
    void closeYarnRequisition(Long id);

    /**
     * 领纱用量 BOM 权威测算接口（ADR-0027 / ADR-0010）
     *
     * 测算公式：需求KG = 计划匹数 × 单匹计划重量(plannedWeight/plannedQty) × ratio / 100
     * 按 yarnSpecId 合并该产品配比 BOM，无 BOM 或缺计划重量时返回提示，结果可手工修改。
     *
     * @param calculateReqVO 测算请求
     * @return 测算结果与提示
     */
    YarnRequisitionCalculateRespVO calculate(@Valid YarnRequisitionCalculateReqVO calculateReqVO);

}
