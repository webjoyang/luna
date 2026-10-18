package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import jakarta.validation.Valid;

/**
 * 财务管理 - 客户对账单 Service 接口
 *
 * @author luna
 */
public interface CustomerStatementService {

    /**
     * 期间对账分录取数预览与候选计算
     *
     * @param reqVO 预览参数
     * @return 预览数据（含期初/期末试算与候选分录行）
     */
    CustomerStatementPrefetchRespVO prefetchLines(@Valid CustomerStatementPrefetchReqVO reqVO);

    /**
     * 生成客户对账单（DRAFT）
     *
     * @param createReqVO 创建参数
     * @return 对账单ID
     */
    Long createCustomerStatement(@Valid CustomerStatementCreateReqVO createReqVO);

    /**
     * 获取客户对账单详情
     *
     * @param id 对账单ID
     * @return 对账单详情（含明细行）
     */
    CustomerStatementDetailRespVO getCustomerStatement(Long id);

    /**
     * 分页查询客户对账单
     *
     * @param pageReqVO 分页查询参数
     * @return 分页结果
     */
    PageResult<CustomerStatementRespVO> getCustomerStatementPage(CustomerStatementPageReqVO pageReqVO);

    /**
     * 更新客户对账单（仅 DRAFT 态允许，支持修改备注与删减行）
     *
     * @param updateReqVO 更新参数
     */
    void updateCustomerStatement(@Valid CustomerStatementUpdateReqVO updateReqVO);

    /**
     * 删除客户对账单（仅 DRAFT 态允许软删除）
     *
     * @param id 对账单ID
     */
    void deleteCustomerStatement(Long id);

    /**
     * 草稿对账单手工追加历史未占用分录
     *
     * @param addEntryReqVO 追加参数
     * @return 新生成的对账单明细行ID
     */
    Long addStatementEntry(@Valid CustomerStatementAddEntryReqVO addEntryReqVO);

    /**
     * 发送对账单（DRAFT -> SENT，条件更新防并发）
     *
     * @param id 对账单ID
     */
    void sendCustomerStatement(Long id);

    /**
     * 作废对账单（非终态 DRAFT/SENT/REJECTED -> VOID，释放行占用，不删分录；CONFIRMED 不可作废）
     *
     * @param id 对账单ID
     */
    void voidCustomerStatement(Long id);

    /**
     * 代客确认对账单（SENT -> CONFIRMED，落库 confirm_type=ON_BEHALF、方式、时间、操作人）
     *
     * @param reqVO 代客确认参数
     */
    void confirmOnBehalf(@Valid CustomerStatementConfirmOnBehalfReqVO reqVO);

    /**
     * 驳回对账单（SENT -> REJECTED，必填 reject_reason）
     *
     * @param reqVO 驳回参数
     */
    void rejectCustomerStatement(@Valid CustomerStatementRejectReqVO reqVO);

    /**
     * 回复异议并重置为草稿（REJECTED -> DRAFT，改后可重新发送）
     *
     * @param reqVO 异议回复参数
     */
    void rejectReplyCustomerStatement(@Valid CustomerStatementRejectReplyReqVO reqVO);

    /**
     * 获得客户对账单打印数据（豁免脱敏，含客户四要素、全量行快照与二维码）
     *
     * @param id 对账单ID
     * @return 打印数据
     */
    CustomerStatementPrintRespVO getCustomerStatementPrintData(Long id);

}
