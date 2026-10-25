package com.luna.lunasrv.module.lunaerp.service.finance.payroll;

import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.PayrollDefectPreviewRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.PayrollDefectRefSaveReqVO;

import java.util.Collection;

/**
 * 财务管理 - 工资单次品扣款导入 Service
 *
 * 只读质检确认库，严格遵循口径常量化与指纹防重防刷。
 *
 * @author luna
 */
public interface PayrollDefectService {

    /**
     * 获得指定期间的质检次品扣款预览数据（按员工聚合卷数+逐卷快照）
     *
     * @param period 工资期间（YYYY-MM）
     * @return 次品扣款预览视图
     */
    PayrollDefectPreviewRespVO getDefectPreview(String period);

    /**
     * 获得指定期间的质检次品扣款预览数据（排除指定工资单占用的指纹）
     *
     * @param period 工资期间（YYYY-MM）
     * @param currentPayrollId 当前工资单ID（可选，为编辑已有工资单时豁免自身已占用的指纹）
     * @return 次品扣款预览视图
     */
    PayrollDefectPreviewRespVO getDefectPreview(String period, Long currentPayrollId);

    /**
     * 校验次品引用指纹可用性（防并发被抢占、冲突排重）
     *
     * @param currentPayrollId 当前工资单ID（可空）
     * @param confirmationIds 待校验的质检确认记录ID列表
     */
    void validateDefectRefsAvailability(Long currentPayrollId, Collection<Long> confirmationIds);

    /**
     * 草稿保存时次品引用全量校验（防重、防抢占、质检状态仍有效重验、责任人匹配）
     *
     * @param currentPayrollId 当前工资单ID（可空）
     * @param defectRefs 待保存的次品引用明细列表
     */
    void validateDefectRefsForSave(Long currentPayrollId, Collection<PayrollDefectRefSaveReqVO> defectRefs);

    /**
     * 提交审批时重验工资单下属所有次品引用（防并发、防质检换链撤销）
     *
     * @param payrollId 工资单ID
     */
    void validateDefectRefsForSubmit(Long payrollId);

}
