package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.*;

import java.util.List;

/**
 * 供应商对账单 Service 接口
 *
 * @author luna
 */
public interface SupplierReconService {

    /**
     * 取数预览（出参两类候选行不落库）
     *
     * @param reqVO 预览参数（供应商、区间）
     * @return 候选明细行列表
     */
    List<SupplierReconSourceItemRespVO> preview(SupplierReconPreviewReqVO reqVO);

    /**
     * 创建供应商对账单（草稿）
     *
     * @param createReqVO 对账单及行明细
     * @return 新建对账单ID
     */
    Long createSupplierRecon(SupplierReconCreateReqVO createReqVO);

    /**
     * 更新供应商对账单（仅 DRAFT 状态可编辑）
     *
     * @param updateReqVO 更新参数
     */
    void updateSupplierRecon(SupplierReconUpdateReqVO updateReqVO);

    /**
     * 重新取数（仅 DRAFT 状态可重取，替换全部 PROCESS/PURCHASE 行，保留 ADJUST 行）
     *
     * @param id 对账单ID
     */
    void reFetchSupplierRecon(Long id);

    /**
     * 删除供应商对账单（仅 DRAFT 状态可软删进回收站）
     *
     * @param id 对账单ID
     */
    void deleteSupplierRecon(Long id);

    /**
     * 获取供应商对账单详情（内嵌 items + 只读实时聚合已申请金额）
     *
     * @param id 对账单ID
     * @return 详情
     */
    SupplierReconRespVO getSupplierRecon(Long id);

    /**
     * 获得供应商对账单分页（实时聚合各单已申请金额）
     *
     * @param reqVO 分页参数
     * @return 分页结果
     */
    PageResult<SupplierReconRespVO> getSupplierReconPage(SupplierReconPageReqVO reqVO);

    /**
     * 确认供应商对账单（仅 DRAFT 状态，冻结快照）
     *
     * @param id 对账单ID
     */
    void confirmSupplierRecon(Long id);

    /**
     * 作废供应商对账单（仅 CONFIRMED 状态，释放指纹）
     *
     * @param id 对账单ID
     */
    void cancelSupplierRecon(Long id);

    /**
     * 获取供应商对账单打印数据（豁免金额脱敏）
     *
     * @param id 对账单ID
     * @return 打印数据
     */
    SupplierReconPrintRespVO getSupplierReconPrintData(Long id);

}
