package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.*;
import jakarta.validation.Valid;

/**
 * 财务管理 - 供应商对账服务接口
 *
 * @author luna
 */
public interface SupplierReconService {

    /**
     * 获得供应商对账单分页
     *
     * @param pageReqVO 分页查询条件
     * @return 供应商对账单分页
     */
    PageResult<SupplierReconRespVO> getReconPage(SupplierReconPageReqVO pageReqVO);

    /**
     * 获得供应商对账单详情
     *
     * @param id 对账单ID
     * @return 供应商对账单详情
     */
    SupplierReconRespVO getRecon(Long id);

    /**
     * 取数预览候选明细
     *
     * @param previewReqVO 预览参数
     * @return 候选明细集合
     */
    SupplierReconCandidatesRespVO previewRecon(@Valid SupplierReconPreviewReqVO previewReqVO);

    /**
     * 创建供应商对账单
     *
     * @param createReqVO 创建参数
     * @return 对账单ID
     */
    Long createRecon(@Valid SupplierReconSaveReqVO createReqVO);

    /**
     * 更新草稿供应商对账单
     *
     * @param updateReqVO 更新参数
     */
    void updateRecon(@Valid SupplierReconSaveReqVO updateReqVO);

    /**
     * 重取草稿供应商对账单
     *
     * @param id 对账单ID
     */
    void reFetchRecon(Long id);

    /**
     * 删除草稿供应商对账单（软删除进回收站）
     *
     * @param id 对账单ID
     */
    void deleteRecon(Long id);

}
