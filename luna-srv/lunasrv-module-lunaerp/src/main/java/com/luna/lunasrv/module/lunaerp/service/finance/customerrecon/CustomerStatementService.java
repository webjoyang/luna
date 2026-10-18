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

}
