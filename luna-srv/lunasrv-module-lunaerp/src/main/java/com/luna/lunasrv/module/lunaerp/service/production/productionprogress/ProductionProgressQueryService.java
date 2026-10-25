package com.luna.lunasrv.module.lunaerp.service.production.productionprogress;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressPageItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressPageReqVO;

import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;

/**
 * 生产管理-生产进度查询 Service 接口（只读聚合视图，ADR-0034）
 *
 * <p>进度 = 合同下各生产单据状态的投影：以 SalesContract 为唯一锚点，实时批量聚合
 * （IN + GROUP BY，无 N+1），DTO 不落库、合同侧不加进度字段。染色环节数据来自进度反馈
 * 覆盖层（ADR-0031），入库/发货量按 related_doc join 源单反查合同（不给 ledger 加列）。</p>
 *
 * @author luna
 */
public interface ProductionProgressQueryService {

    /**
     * 获得生产进度分页（跨合同列表：合同头 + 各环节关键量 + 预警数）。
     *
     * <p>默认按交期升序、无交期排最后；支持关键字/交期范围/状态/仅看预警筛选，默认排除 CANCELLED。</p>
     * 显式关闭数据权限（ADR-0034 / ADR-0046 §5.2 第 4 项），系统级跨单聚合视图。
     *
     * @param reqVO 分页与筛选参数
     * @return 生产进度分页
     */
    @DataPermission(enable = false)
    PageResult<ProductionProgressPageItemRespVO> getProductionProgressPage(ProductionProgressPageReqVO reqVO);

    /**
     * 获得单合同全链路生产进度视图（六环节 + 发染记录/缸状态汇总下钻）。
     *
     * @param contractId 销售合同ID
     * @return 单合同全链路进度视图
     */
    ProductionProgressDetailRespVO getProductionProgress(Long contractId);

}
