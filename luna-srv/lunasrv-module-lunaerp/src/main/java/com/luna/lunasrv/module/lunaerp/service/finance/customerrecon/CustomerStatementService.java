package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;

/**
 * 财务管理 - 客户对账单服务接口
 *
 * @author luna
 */
public interface CustomerStatementService {

    /**
     * 分页查询客户对账单列表
     *
     * @param pageReqVO 分页查询条件
     * @return 对账单分页结果
     */
    PageResult<CustomerStatementRespVO> getStatementPage(CustomerStatementPageReqVO pageReqVO);

    /**
     * 查询客户对账单详情（含明细行）
     *
     * @param id 对账单ID
     * @return 对账单详情
     */
    CustomerStatementDetailRespVO getStatement(Long id);

    /**
     * 对账期间候选分录取数预览（支持生成前预览与草稿追加选择器）
     *
     * @param reqVO 取数请求条件
     * @return 候选分录及期初/发生额试算结果
     */
    CustomerStatementPrefetchRespVO prefetchLines(CustomerStatementPrefetchReqVO reqVO);

    /**
     * 创建草稿客户对账单
     *
     * @param reqVO 创建请求
     * @return 生成的对账单ID
     */
    Long createStatement(CustomerStatementCreateReqVO reqVO);

    /**
     * 更新草稿客户对账单
     *
     * @param reqVO 更新请求
     */
    void updateStatement(CustomerStatementUpdateReqVO reqVO);

    /**
     * 删除草稿客户对账单（软删除，释放行占用）
     *
     * @param id 对账单ID
     */
    void deleteStatement(Long id);

    /**
     * 草稿对账单手工追加未占用分录
     *
     * @param reqVO 追加请求
     */
    void addEntry(CustomerStatementAddEntryReqVO reqVO);

}
