package com.luna.lunasrv.module.lunaerp.service.business.costcard;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.costcard.vo.*;
import jakarta.validation.Valid;

/**
 * 业务管理 - 成本卡 Service 接口
 *
 * @author luna
 */
public interface CostCardService {

    /**
     * 创建成本卡
     *
     * @param createReqVO 创建信息
     * @return 成本卡ID
     */
    Long createCostCard(@Valid CostCardSaveReqVO createReqVO);

    /**
     * 更新成本卡
     *
     * @param updateReqVO 更新信息
     */
    void updateCostCard(@Valid CostCardSaveReqVO updateReqVO);

    /**
     * 删除成本卡（仅草稿可删，级联软删明细）
     *
     * @param id 成本卡ID
     */
    void deleteCostCard(Long id);

    /**
     * 查询成本卡详情（含纱线行和费用行）
     *
     * @param id 成本卡ID
     * @return 成本卡详情
     */
    CostCardRespVO getCostCard(Long id);

    /**
     * 查询指定产品的有效生效成本卡（无生效卡返回 null）
     *
     * @param productId 产品ID
     * @return 成本卡详情或 null
     */
    CostCardRespVO getEffectiveCostCardByProductId(Long productId);

    /**
     * 分页查询成本卡
     *
     * @param pageReqVO 分页条件
     * @return 分页结果
     */
    PageResult<CostCardRespVO> getCostCardPage(CostCardPageReqVO pageReqVO);

    /**
     * 生效成本卡（DRAFT -> EFFECTIVE，旧卡失效）
     *
     * @param id 成本卡ID
     */
    void effectCostCard(Long id);

    /**
     * 失效成本卡（EFFECTIVE -> EXPIRED）
     *
     * @param id 成本卡ID
     */
    void expireCostCard(Long id);

    /**
     * 复制成本卡生成新草稿卡
     *
     * @param id 源成本卡ID
     * @return 新成本卡ID
     */
    Long copyCostCard(Long id);

    /**
     * 重新按价类取当日价并重算
     *
     * @param reqVO 试算入参
     * @return 重算结果
     */
    CostCardTrialRespVO refreshPrices(@Valid CostCardTrialReqVO reqVO);

    /**
     * 试算成本卡（纯内存不落库）
     *
     * @param trialReqVO 试算入参
     * @return 测算结果
     */
    CostCardTrialRespVO trialCostCard(@Valid CostCardTrialReqVO trialReqVO);

    /**
     * 预览下一个成本卡号
     *
     * @return 下一个卡号
     */
    String getNextCardNo();

}
