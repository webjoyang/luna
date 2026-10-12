package com.luna.lunasrv.module.lunaerp.service.finance.invoice;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.*;

import java.util.List;

/**
 * 财务管理 - 发票申请 Service
 *
 * @author luna
 */
public interface InvoiceService {

    /**
     * 创建发票申请（草稿）
     *
     * @param createReqVO 创建信息
     * @return 申请单ID
     */
    Long createInvoice(InvoiceSaveReqVO createReqVO);

    /**
     * 修改发票申请（草稿/已驳回状态允许修改）
     *
     * @param updateReqVO 修改信息
     */
    void updateInvoice(InvoiceSaveReqVO updateReqVO);

    /**
     * 删除发票申请（仅草稿状态允许删除）
     *
     * @param id 申请单ID
     */
    void deleteInvoice(Long id);

    /**
     * 提交发票申请（DRAFT/REJECTED -> SUBMITTED）
     *
     * @param id 申请单ID
     */
    void submitInvoice(Long id);

    /**
     * 财务登记发票回填（SUBMITTED -> ISSUED）
     *
     * @param issueReqVO 发票登记信息
     */
    void issueInvoice(InvoiceIssueReqVO issueReqVO);

    /**
     * 财务驳回发票申请（SUBMITTED -> REJECTED）
     *
     * @param id 申请单ID
     * @param reason 驳回原因（可空）
     */
    void rejectInvoice(Long id, String reason);

    /**
     * 获得发票申请详情
     *
     * @param id 申请单ID
     * @return 详情
     */
    InvoiceRespVO getInvoice(Long id);

    /**
     * 分页查询发票申请
     *
     * @param pageReqVO 查询参数
     * @return 分页结果
     */
    PageResult<InvoiceRespVO> getInvoicePage(InvoicePageReqVO pageReqVO);

    /**
     * 查询指定客户的可开票发货单候选列表（COMPLETED 状态且有未开票余额）
     *
     * @param customerId 客户ID
     * @return 候选发货单列表
     */
    List<InvoiceCandidateShipmentRespVO> getCandidateShipments(Long customerId);

}
