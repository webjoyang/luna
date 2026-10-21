package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
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
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopTicketScanRespVO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopInspectionItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.h5.workshop.WorkshopH5Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 车间移动 H5 - 接入层控制器
 *
 * 规范：
 * 1. 包路径 controller.admin.h5.workshop，由底层框架统一自动映射 /admin-api/lunaerp/h5/workshop 前缀；
 * 2. 鉴权：严格遵循车间 H5 权限位（报工 lunaerp:workshop:h5:report）；
 * 3. 强制在 Service 内施加数据范围过滤，不暴露任何内部敏感业务字段。
 *
 * @author luna
 */
@Tag(name = "车间移动 H5 - 接入层")
@RestController
@RequestMapping("/lunaerp/h5/workshop")
@Validated
public class WorkshopH5Controller {

    @Resource
    private WorkshopH5Service workshopH5Service;

    @GetMapping("/my-tasks")
    @Operation(summary = "查询我的机台任务列表（分页）")
    @PreAuthorize("@ss.hasPermission('lunaerp:workshop:h5:report')")
    public CommonResult<PageResult<WorkshopMyTaskRespVO>> getMyTasks(@Valid WorkshopMyTasksPageReqVO reqVO) {
        return success(workshopH5Service.getMyTasksPage(reqVO));
    }

    @GetMapping("/ticket/get-by-code")
    @Operation(summary = "按布票编码查询布票卡（扫码/手输定位）")
    @Parameter(name = "code", description = "布票编号", required = true, example = "BT-MTZZPB09190010101-0001")
    @PreAuthorize("@ss.hasPermission('lunaerp:workshop:h5:report')")
    public CommonResult<WorkshopTicketRespVO> getTicketByCode(@RequestParam("code") String code) {
        return success(workshopH5Service.getTicketByCode(code));
    }

    @PostMapping("/ticket/start")
    @Operation(summary = "布票开工（含自助认领）")
    @PreAuthorize("@ss.hasPermission('lunaerp:workshop:h5:report')")
    public CommonResult<Boolean> startTicket(@Valid @RequestBody WorkshopTicketStartReqVO reqVO) {
        workshopH5Service.startTicket(reqVO);
        return success(true);
    }

    @PostMapping("/ticket/complete")
    @Operation(summary = "布票完工（幂等友好）")
    @PreAuthorize("@ss.hasPermission('lunaerp:workshop:h5:report')")
    public CommonResult<WorkshopTicketCompleteRespVO> completeTicket(@Valid @RequestBody WorkshopTicketCompleteReqVO reqVO) {
        return success(workshopH5Service.completeTicket(reqVO));
    }

    @PostMapping("/ticket/exception")
    @Operation(summary = "布票异常登记")
    @PreAuthorize("@ss.hasPermission('lunaerp:workshop:h5:report')")
    public CommonResult<Boolean> exceptionTicket(@Valid @RequestBody WorkshopTicketExceptionReqVO reqVO) {
        workshopH5Service.exceptionTicket(reqVO);
        return success(true);
    }

    @GetMapping("/inspection/page")
    @Operation(summary = "查询移动质检单列表（分页）")
    @PreAuthorize("@ss.hasPermission('lunaerp:workshop:h5:inspection')")
    public CommonResult<PageResult<WorkshopInspectionRespVO>> getInspectionPage(@Valid WorkshopInspectionPageReqVO reqVO) {
        return success(workshopH5Service.getInspectionPage(reqVO));
    }

    @GetMapping("/inspection/get")
    @Operation(summary = "查询检验单详情（表头+逐行含照片）")
    @Parameter(name = "id", description = "检验单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:workshop:h5:inspection')")
    public CommonResult<WorkshopInspectionDetailRespVO> getInspection(@RequestParam("id") Long id) {
        return success(workshopH5Service.getInspection(id));
    }

    @GetMapping("/inspection/ticket-scan")
    @Operation(summary = "胚布扫码定位编排（自动交接检验单并定位录检行）")
    @Parameter(name = "code", description = "布票码", required = true, example = "BT-MTZZPB09190010101-0001")
    @PreAuthorize("@ss.hasPermission('lunaerp:workshop:h5:inspection')")
    public CommonResult<WorkshopTicketScanRespVO> ticketScan(@RequestParam("code") String code) {
        return success(workshopH5Service.ticketScan(code));
    }

    @PostMapping("/inspection/item/save")
    @Operation(summary = "保存检验单录检明细（新建/改录，支持胚布PIECE与成品ROLL）")
    @PreAuthorize("@ss.hasPermission('lunaerp:workshop:h5:inspection')")
    public CommonResult<Long> saveInspectionItem(@Valid @RequestBody WorkshopInspectionItemSaveReqVO reqVO) {
        return success(workshopH5Service.saveInspectionItem(reqVO));
    }

}
