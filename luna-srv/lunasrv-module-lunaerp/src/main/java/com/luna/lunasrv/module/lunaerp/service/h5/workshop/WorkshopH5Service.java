package com.luna.lunasrv.module.lunaerp.service.h5.workshop;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTaskRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopMyTasksPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketStartReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketCompleteReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketCompleteRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketExceptionReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketScanRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopRequisitionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopRequisitionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopRequisitionDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopWarehouseSimpleRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopRequisitionDispatchReqVO;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/**
 * 车间移动 H5 接入层编排服务
 *
 * @author luna
 */
public interface WorkshopH5Service {

    /**
     * 获取当前登录挡车工的机台任务分页列表
     *
     * @param reqVO 分页查询条件
     * @return 任务列表分页
     */
    PageResult<WorkshopMyTaskRespVO> getMyTasksPage(WorkshopMyTasksPageReqVO reqVO);

    /**
     * 按布票编码查询布票详情卡片（扫码/手输定位）
     *
     * @param code 布票编号
     * @return 布票卡精简出参
     */
    WorkshopTicketRespVO getTicketByCode(String code);

    /**
     * 布票开工（含未派工自助认领）
     *
     * @param reqVO 开工请求入参
     */
    void startTicket(WorkshopTicketStartReqVO reqVO);

    /**
     * 布票完工（幂等友好）
     *
     * @param reqVO 完工请求入参
     * @return 完工响应（含 alreadyCompleted 标志）
     */
    WorkshopTicketCompleteRespVO completeTicket(WorkshopTicketCompleteReqVO reqVO);

    /**
     * 布票异常登记（含现场照片）
     *
     * @param reqVO 异常登记请求入参
     */
    void exceptionTicket(WorkshopTicketExceptionReqVO reqVO);

    /**
     * 获取移动质检单分页列表
     *
     * @param reqVO 分页查询条件
     * @return 检验单列表分页
     */
    PageResult<WorkshopInspectionRespVO> getInspectionPage(WorkshopInspectionPageReqVO reqVO);

    /**
     * 查询检验单详情（表头+逐行含照片）
     *
     * @param id 检验单ID
     * @return 检验单详情
     */
    WorkshopInspectionDetailRespVO getInspection(Long id);

    /**
     * 胚布扫码定位编排（自动交接检验单并定位录检行）
     *
     * @param code 布票码
     * @return 扫码定位编排响应（检验单ID、布票ID、建议匹号、已录检明细行）
     */
    WorkshopTicketScanRespVO ticketScan(String code);

    /**
     * 保存检验单录检明细（新建/改录，支持胚布 PIECE 与成品 ROLL）
     *
     * @param reqVO 录检行保存入参
     * @return 录检明细ID
     */
    Long saveInspectionItem(WorkshopInspectionItemSaveReqVO reqVO);

    /**
     * 查询我的待领纱单列表（分页）
     *
     * @param reqVO 分页查询条件
     * @return 领纱单分页
     */
    PageResult<WorkshopRequisitionRespVO> getRequisitionPage(WorkshopRequisitionPageReqVO reqVO);

    /**
     * 查询领纱单详情（含明细及默认仓库）
     *
     * @param id 领纱单ID
     * @return 领纱单详情
     */
    WorkshopRequisitionDetailRespVO getRequisition(Long id);

    /**
     * 获取启用中仓库精简列表（供移动端手选仓库）
     *
     * @return 仓库精简列表
     */
    List<WorkshopWarehouseSimpleRespVO> getWarehouseSimpleList();

    /**
     * 查询指定仓库下某纱线规格的可用库存余额
     *
     * @param warehouseId 仓库ID
     * @param yarnSpecId  纱线规格ID
     * @return 库存余额
     */
    BigDecimal getYarnBalance(Long warehouseId, Long yarnSpecId);

    /**
     * 移动端领纱明细发纱
     *
     * @param reqVO 发纱请求参数
     */
    void dispatchYarn(@Valid WorkshopRequisitionDispatchReqVO reqVO);

}
