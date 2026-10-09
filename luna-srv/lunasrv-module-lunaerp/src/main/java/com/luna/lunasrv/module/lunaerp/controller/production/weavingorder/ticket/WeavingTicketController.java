package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo.*;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket.WeavingTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - 布票")
@RestController
@RequestMapping({"/lunaerp/production/weaving-order-ticket", "/lunaerp/production/weaving-ticket"})
@Validated
public class WeavingTicketController {

    @Resource
    private WeavingTicketService weavingTicketService;

    @PostMapping({"/create", "/create-batch"})
    @Operation(summary = "按机台任务批量生成布票")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-ticket:create') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<List<WeavingTicketRespVO>> createTickets(@Valid @RequestBody WeavingTicketCreateReqVO createReqVO) {
        return success(weavingTicketService.createTickets(createReqVO));
    }

    @PutMapping("/start")
    @Operation(summary = "布票开工（单张/批量）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-ticket:update') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> startTicket(@Valid @RequestBody WeavingTicketStartReqVO startReqVO) {
        weavingTicketService.startTicket(startReqVO);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "布票完工（单张/批量）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-ticket:update') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> completeTicket(@Valid @RequestBody WeavingTicketCompleteReqVO completeReqVO) {
        weavingTicketService.completeTicket(completeReqVO);
        return success(true);
    }

    @PutMapping("/exception")
    @Operation(summary = "布票异常登记（单张/批量）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-ticket:update') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> exceptionTicket(@Valid @RequestBody WeavingTicketExceptionReqVO exceptionReqVO) {
        weavingTicketService.exceptionTicket(exceptionReqVO);
        return success(true);
    }

    @PutMapping("/void")
    @Operation(summary = "布票作废（单张/批量）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-ticket:update') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> voidTicket(@Valid @RequestBody WeavingTicketVoidReqVO voidReqVO) {
        weavingTicketService.voidTicket(voidReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得布票详情")
    @Parameter(name = "id", description = "布票ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-ticket:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<WeavingTicketRespVO> getTicket(@RequestParam("id") Long id) {
        return success(weavingTicketService.getTicket(id));
    }

    @GetMapping("/get-by-code")
    @Operation(summary = "按布票编号获得布票详情（扫码枪录入）")
    @Parameter(name = "code", description = "布票编号", required = true, example = "BT-MTZZPB09190010101-0001")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-ticket:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<WeavingTicketRespVO> getTicketByCode(@RequestParam("code") String code) {
        return success(weavingTicketService.getTicketByCode(code));
    }

    @GetMapping("/page")
    @Operation(summary = "获得布票分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-ticket:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<PageResult<WeavingTicketRespVO>> getTicketPage(@Validated WeavingTicketPageReqVO pageReqVO) {
        return success(weavingTicketService.getTicketPage(pageReqVO));
    }

    @GetMapping({"/{id}/print-data", "/print-data"})
    @Operation(summary = "获得布票打印数据")
    @Parameter(name = "id", description = "布票ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-ticket:print')")
    public CommonResult<WeavingTicketPrintRespVO> getWeavingTicketPrintData(
            @PathVariable(value = "id", required = false) Long pathId,
            @RequestParam(value = "id", required = false) Long queryId) {
        Long id = pathId != null ? pathId : queryId;
        return success(weavingTicketService.getWeavingTicketPrintData(id));
    }

}
