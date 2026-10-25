package com.luna.lunasrv.module.lunaerp.service.finance.payroll;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.payroll.vo.*;

/**
 * 财务管理 - 员工工资单业务服务接口
 *
 * @author luna
 */
public interface PayrollService {

    /**
     * 创建工资单（按月期间新建草稿）
     *
     * @param createReqVO 创建信息
     * @return 工资单ID
     */
    Long createPayroll(PayrollCreateReqVO createReqVO);

    /**
     * 更新工资单（仅 PENDING/REJECTED 可整体替换员工行与款项行）
     *
     * @param updateReqVO 更新信息
     */
    void updatePayroll(PayrollUpdateReqVO updateReqVO);

    /**
     * 软删除工资单草稿（仅 PENDING/REJECTED，删除后进回收站）
     *
     * @param id 工资单ID
     */
    void deletePayroll(Long id);

    /**
     * 提交工资单审批（PENDING/REJECTED -> APPROVING，biz_type=payroll）
     *
     * @param id 工资单ID
     */
    void submitPayroll(Long id);

    /**
     * 撤回工资单审批（APPROVING -> PENDING）
     *
     * @param id 工资单ID
     */
    void cancelApprove(Long id);

    /**
     * 作废已审批工资单（仅 APPROVED 且发放批次=0，置 CANCELLED）
     *
     * @param id 工资单ID
     * @param cancelReason 作废原因（必填）
     */
    void cancelPayroll(Long id, String cancelReason);

    /**
     * 获取工资单详情（含员工汇总行、款项行、次品引用、发放批次与审批进度）
     *
     * @param id 工资单ID
     * @return 工资单详情
     */
    PayrollRespVO getPayroll(Long id);

    /**
     * 分页查询工资单（支持期间、审批状态、派生发放态筛选）
     *
     * @param pageReqVO 分页查询条件
     * @return 工资单分页列表
     */
    PageResult<PayrollRespVO> getPayrollPage(PayrollPageReqVO pageReqVO);

    /**
     * 获取已通过工资单 A4 套打数据（仅 APPROVED 终态可取）
     *
     * @param id 工资单ID
     * @return 套打数据
     */
    PayrollPrintRespVO getPayrollPrintData(Long id);

    /**
     * 登记工资单发放批次（出纳执行·扣款并过账）
     *
     * @param reqVO 发放登记请求
     * @return 发放批次ID
     */
    Long registerPayrollPayment(PayrollPaymentRegisterReqVO reqVO);

    /**
     * 分页查询工资单发放批次列表
     *
     * @param pageReqVO 批次分页查询请求
     * @return 发放批次分页
     */
    PageResult<PayrollPaymentRespVO> getPayrollPaymentPage(PayrollPaymentPageReqVO pageReqVO);

}
