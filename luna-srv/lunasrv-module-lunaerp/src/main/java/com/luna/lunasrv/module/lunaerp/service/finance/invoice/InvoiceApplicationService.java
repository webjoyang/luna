package com.luna.lunasrv.module.lunaerp.service.finance.invoice;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.*;

import java.util.List;

/**
 * 财务管理 - 发票申请 Service 接口
 *
 * @author luna
 */
public interface InvoiceApplicationService {

    /**
     * 获得发票申请分页
     *
     * @param pageReqVO 分页查询条件
     * @return 发票申请分页
     */
    PageResult<InvoiceApplicationRespVO> getInvoiceApplicationPage(InvoiceApplicationPageReqVO pageReqVO);

    /**
     * 获得发票申请详情（含明细行）
     *
     * @param id 发票申请ID
     * @return 发票申请详情
     */
    InvoiceApplicationDetailRespVO getInvoiceApplication(Long id);

    /**
     * 创建发票申请（草稿）
     *
     * @param createReqVO 创建信息
     * @return 发票申请ID
     */
    Long createInvoiceApplication(InvoiceApplicationCreateReqVO createReqVO);

    /**
     * 修改发票申请（仅草稿态可改）
     *
     * @param updateReqVO 修改信息
     */
    void updateInvoiceApplication(InvoiceApplicationUpdateReqVO updateReqVO);

    /**
     * 删除发票申请（仅草稿态可删）
     *
     * @param id 发票申请ID
     */
    void deleteInvoiceApplication(Long id);

    /**
     * 提交发票申请（DRAFT -> SUBMITTED）
     *
     * @param id 发票申请ID
     */
    void submitInvoiceApplication(Long id);

    /**
     * 登记开票回填（SUBMITTED -> ISSUED，回填 20 位官方发票号、开票日期、附件等）
     *
     * @param reqVO 登记回填信息
     */
    void issueInvoiceApplication(InvoiceApplicationIssueReqVO reqVO);

    /**
     * 驳回发票申请（SUBMITTED -> REJECTED，释放开票占用）
     *
     * @param reqVO 驳回信息
     */
    void rejectInvoiceApplication(InvoiceApplicationRejectReqVO reqVO);

    /**
     * 查询指定客户和币种的可开票候选发货单列表
     *
     * @param customerId 客户ID
     * @param currency   币种
     * @return 候选发货单列表（含已开票金额、可开票余额）
     */
    List<InvoiceCandidateShipmentRespVO> getCandidateShipments(Long customerId, String currency);

}
