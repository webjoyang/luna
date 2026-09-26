package com.luna.lunasrv.module.lunaerp.service.production.weavingorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderSaveRespVO;
import jakarta.validation.Valid;

/**
 * 生产管理-织造执行-织造单 Service 接口
 *
 * @author luna
 */
public interface WeavingOrderService {

    /**
     * 创建织造单
     *
     * 校验关联胚布单存在且为自织态（productionMode=SELF）且未取消；
     * 自动带入胚布单/产品档案快照；派生生成单号 ZZ-{胚布单号去符号}-{2位序号}；
     * 计划匹数软校验：超胚布单匹数时提示警告并允许保存。
     *
     * @param createReqVO 创建信息
     * @return 保存结果（包含单据ID与可能的软警告信息）
     */
    WeavingOrderSaveRespVO createWeavingOrder(@Valid WeavingOrderSaveReqVO createReqVO);

    /**
     * 修改织造单
     *
     * 仅待排产（DRAFT）状态允许修改；
     * 单号手填查重（全量唯一含软删行，排除自身）。
     *
     * @param updateReqVO 修改信息
     * @return 保存结果（包含单据ID与可能的软警告信息）
     */
    WeavingOrderSaveRespVO updateWeavingOrder(@Valid WeavingOrderSaveReqVO updateReqVO);

    /**
     * 删除织造单
     *
     * 仅待排产（DRAFT）状态允许删除；
     * 级联逻辑删除下属机台任务与布票。
     *
     * @param id 织造单ID
     */
    void deleteWeavingOrder(Long id);

    /**
     * 取消织造单
     *
     * 仅非 COMPLETED / CANCELLED 状态允许手动取消。
     *
     * @param id 织造单ID
     */
    void cancelWeavingOrder(Long id);

    /**
     * 获取织造单详情
     *
     * @param id 织造单ID
     * @return 织造单详情
     */
    WeavingOrderRespVO getWeavingOrder(Long id);

    /**
     * 获取织造单分页
     *
     * @param pageReqVO 分页查询条件
     * @return 织造单分页结果
     */
    PageResult<WeavingOrderRespVO> getWeavingOrderPage(WeavingOrderPageReqVO pageReqVO);

    /**
     * 预览下一个织造单号（只读、不占号）
     *
     * @param greigeOrderId 关联胚布单ID
     * @return 派生单号
     */
    String getNextWeavingOrderCode(Long greigeOrderId);

    /**
     * 获得织造单统计数据（计划匹数、有效票数、作废数、完工数、完成率、各状态计数）
     *
     * @param id 织造单ID
     * @return 统计数据
     */
    com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.vo.WeavingOrderStatsRespVO getWeavingOrderStats(Long id);

}

