package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 生产管理 - 织造布票 Service 接口
 *
 * @author luna
 */
public interface WeavingTicketService {

    /**
     * 按机台任务批量生成布票
     *
     * 事务内锁任务行 + 余量校验（超计划匹数拒绝） + 撞键重试 + 状态推导。
     *
     * @param createReqVO 生成信息
     * @return 已生成的布票列表
     */
    List<WeavingTicketRespVO> createTickets(@Valid WeavingTicketCreateReqVO createReqVO);

    /**
     * 布票开工（扫码/人工，允许来源态：GENERATED/STARTED/EXCEPTION，写 actualMachine/工人/startedAt，同事务推导状态）
     *
     * @param startReqVO 开工信息
     */
    void startTicket(@Valid WeavingTicketStartReqVO startReqVO);

    /**
     * 布票完工（扫码/人工，允许来源态：STARTED，写 completedAt，同事务推导状态与 actualQty）
     *
     * @param completeReqVO 完工信息
     */
    void completeTicket(@Valid WeavingTicketCompleteReqVO completeReqVO);

    /**
     * 布票异常登记（扫码/人工，允许来源态：GENERATED/STARTED，校验异常类型合法性，同事务推导状态）
     *
     * @param exceptionReqVO 异常信息
     */
    void exceptionTicket(@Valid WeavingTicketExceptionReqVO exceptionReqVO);

    /**
     * 布票作废（人工终态，允许来源态：GENERATED/STARTED/EXCEPTION，作废时间与原因必填，同事务推导状态）
     *
     * @param voidReqVO 作废信息
     */
    void voidTicket(@Valid WeavingTicketVoidReqVO voidReqVO);

    /**
     * 获得布票详情（按ID）
     *
     * @param id 布票ID
     * @return 布票详情
     */
    WeavingTicketRespVO getTicket(Long id);

    /**
     * 获得布票详情（按布票编号/扫码枪输入）
     *
     * @param code 布票编号
     * @return 布票详情
     */
    WeavingTicketRespVO getTicketByCode(String code);

    /**
     * 获得布票分页
     *
     * @param pageReqVO 分页查询
     * @return 布票分页
     */
    PageResult<WeavingTicketRespVO> getTicketPage(WeavingTicketPageReqVO pageReqVO);

    /**
     * 按织造单ID批量删除布票（供织造单删除监听器级联调用）
     *
     * @param weavingOrderId 织造单ID
     */
    void deleteByWeavingOrderId(Long weavingOrderId);

    /**
     * 按机台任务ID批量删除布票（供机台任务删除调用）
     *
     * @param machineTaskId 机台任务ID
     */
    void deleteByMachineTaskId(Long machineTaskId);

    /**
     * 自动推导机台任务与织造单状态（同事务调用）
     *
     * 规则（ADR-0024）：
     * - 任务 actualQty = 下属完工口径票数（COMPLETED/INSPECTED/STOCKED）
     * - 任务状态：全部有效票达完工口径→COMPLETED，任一有效票在产→IN_PROGRESS，无在产票→PENDING
     *   * 特例：任务处于 HALTED 期间，只更新 actualQty，不覆盖 HALTED 状态
     * - 织造单状态：全部有效票达完工口径→COMPLETED，首票开工/任一在产→ACTIVE，无在产票→DRAFT
     *   * 特例：已 CANCELLED 织造单不覆盖
     *
     * @param machineTaskId 机台任务ID
     * @param weavingOrderId 织造单ID
     */
    void deriveState(Long machineTaskId, Long weavingOrderId);

    /**
     * 回写布票状态为已检验（INSPECTED）并同事务推导状态（供检验明细保存调用，ADR-0024、ADR-0025）
     *
     * @param ticketId 布票ID
     */
    void markInspected(Long ticketId);

    /**
     * 检验明细删除时回退布票状态（若当前为 INSPECTED 则回退为 COMPLETED）并同事务推导状态
     *
     * @param ticketId 布票ID
     */
    void rollbackInspected(Long ticketId);

    /**
     * 回写布票状态为已入库（STOCKED）并同事务推导状态（供入库审核过账调用，ADR-0024、ADR-0025）
     *
     * @param ticketIds 布票ID集合
     */
    void markStocked(java.util.Collection<Long> ticketIds);

    /**
     * 获得布票打印数据（独立 VO，豁免脱敏，含拼好的 qrUrl）
     *
     * @param id 布票ID
     * @return 打印数据
     */
    WeavingTicketPrintRespVO getWeavingTicketPrintData(Long id);

}
