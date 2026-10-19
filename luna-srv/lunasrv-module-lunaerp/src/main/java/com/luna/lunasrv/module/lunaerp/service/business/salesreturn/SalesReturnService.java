package com.luna.lunasrv.module.lunaerp.service.business.salesreturn;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 业务管理 - 销售退货 Service 接口（ADR-0042）
 *
 * @author luna
 */
public interface SalesReturnService {

    /**
     * 查询可选发货单候选列表（仅 COMPLETED 且含实时可退量>0 行）
     *
     * @param customerId 客户ID（可选，远程搜索）
     * @param shipmentNo 发货单号（可选模糊）
     * @return 候选发货单列表
     */
    List<SalesReturnCandidateRespVO> getShipmentCandidates(Long customerId, String shipmentNo);

    /**
     * 发起退货取数：获得发货单头快照、明细行（含实时可退量）及卷清单
     *
     * @param shipmentId 发货单ID
     * @return 退货制单详情
     */
    SalesReturnShipmentDetailRespVO getShipmentDetail(Long shipmentId);

    /**
     * 创建销售退货单（草稿保存）
     *
     * @param createReqVO 创建信息
     * @return 退货单ID
     */
    Long createSalesReturn(@Valid SalesReturnCreateReqVO createReqVO);

    /**
     * 修改销售退货单（仅草稿态可编辑，行/卷整单差异替换 ADR-0032）
     *
     * @param updateReqVO 修改信息
     */
    void updateSalesReturn(@Valid SalesReturnUpdateReqVO updateReqVO);

    /**
     * 删除销售退货单（仅草稿态可删除，级联软删行/卷）
     *
     * @param id 退货单ID
     */
    void deleteSalesReturn(Long id);

    /**
     * 获得销售退货单详情（包含头快照、明细行、卷清单）
     *
     * @param id 退货单ID
     * @return 退货单详情
     */
    SalesReturnRespVO getSalesReturn(Long id);

    /**
     * 获得销售退货单分页
     *
     * @param reqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<SalesReturnRespVO> getSalesReturnPage(SalesReturnPageReqVO reqVO);

    /**
     * 提交销售退货单审批
     *
     * @param id 退货单ID
     */
    void submitSalesReturn(Long id);

    /**
     * 撤回销售退货单审批
     *
     * @param id 退货单ID
     */
    void withdrawSalesReturn(Long id);

    /**
     * 作废销售退货单（草稿分支）
     *
     * @param id 退货单ID
     * @param cancelReason 作废原因
     */
    void cancelSalesReturn(Long id, String cancelReason);

    /**
     * 到货质检事务（逐卷判定 RESTOCK/SCRAP，重写入库单并审核或全废取消）
     *
     * @param reqVO 到货质检请求信息
     */
    void receiveSalesReturn(@Valid SalesReturnReceiveReqVO reqVO);

}
