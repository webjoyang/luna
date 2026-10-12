package com.luna.lunasrv.module.lunaerp.service.portal;

import com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo.*;

import java.util.List;

/**
 * 客户门户 - 对账单服务接口
 *
 * @author luna
 */
public interface PortalStatementService {

    /**
     * 获取客户门户对账单列表
     *
     * @param customerId 显式客户ID（可选，内部代客场景）
     * @param status 状态过滤（可选，仅限 SENT, CONFIRMED, REJECTED）
     * @return 对账单列表（仅 SENT, CONFIRMED, REJECTED）
     */
    List<PortalStatementRespVO> getStatementList(Long customerId, String status);

    /**
     * 获取客户门户对账单详情（含明细行）
     *
     * @param id 对账单ID（可选）
     * @param no 对账单号（可选）
     * @param customerId 显式客户ID（可选，内部代客场景）
     * @return 对账单详情
     */
    PortalStatementDetailRespVO getStatementDetail(Long id, String no, Long customerId);

    /**
     * 门户客户确认对账单
     *
     * @param reqVO 确认请求
     */
    void confirmStatement(PortalStatementConfirmReqVO reqVO);

    /**
     * 门户客户驳回对账单
     *
     * @param reqVO 驳回请求
     */
    void rejectStatement(PortalStatementRejectReqVO reqVO);

}
