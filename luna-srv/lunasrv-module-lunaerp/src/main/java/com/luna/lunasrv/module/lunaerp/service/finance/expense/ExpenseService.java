package com.luna.lunasrv.module.lunaerp.service.finance.expense;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.expense.vo.*;
import jakarta.validation.Valid;

/**
 * 财务管理 - 员工费用报销服务接口
 *
 * @author luna
 */
public interface ExpenseService {

    /**
     * 新建报销单草稿
     *
     * @param createReqVO 创建请求
     * @return 报销单ID
     */
    Long createExpense(@Valid ExpenseCreateReqVO createReqVO);

    /**
     * 修改报销单草稿（仅 PENDING/REJECTED 可改）
     *
     * @param updateReqVO 修改请求
     */
    void updateExpense(@Valid ExpenseUpdateReqVO updateReqVO);

    /**
     * 删除报销单草稿（仅 PENDING/REJECTED 软删进回收站）
     *
     * @param id 报销单ID
     */
    void deleteExpense(Long id);

    /**
     * 提交报销审批（校验账户、行、附件、日期守卫，发起 02 号审批流）
     *
     * @param id 报销单ID
     */
    void submitExpense(Long id);

    /**
     * 撤回报销审批（仅 APPROVING 态，回置为 PENDING）
     *
     * @param id 报销单ID
     */
    void cancelApprove(Long id);

    /**
     * 获得报销单详情（主从 + 附件 + 审批进度摘要）
     *
     * @param id 报销单ID
     * @return 报销单详情
     */
    ExpenseRespVO getExpense(Long id);

    /**
     * 获得报销单分页
     *
     * @param pageReqVO 分页查询条件
     * @return 报销单分页
     */
    PageResult<ExpenseRespVO> getExpensePage(ExpensePageReqVO pageReqVO);

    /**
     * 获得 A4 套打数据（仅 APPROVED 可取，豁免金额脱敏）
     *
     * @param id 报销单ID
     * @return 套打数据
     */
    ExpensePrintRespVO getExpensePrintData(Long id);

    /**
     * 审批通过后同事务过账（供 ExpenseApprovalHandler 调用）
     *
     * @param id 报销单ID
     */
    void approveExpenseAndPost(Long id);

}
