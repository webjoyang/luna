package com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo.*;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 客户门户 - 对账单控制器
 *
 * <p>端点统一挂在 /app-api/lunaerp/portal/finance/statement 下，标注 @PermitAll 豁免 Spring Security admin 鉴权，
 * 满足 10 号契约 /portal/finance/statement/* 模块内相对前缀规范；由 PortalTokenFilter 校验令牌并注入 PortalContext 上下文。</p>
 *
 * @author luna
 */
@Tag(name = "客户门户 - 对账单")
@PermitAll
@RestController
@RequestMapping({"/app-api/lunaerp/portal/finance/statement", "/portal/finance/statement"})
@Validated
public class PortalStatementController {

    @Resource
    private PortalStatementService portalStatementService;

    @GetMapping("/list")
    @PermitAll
    @Operation(summary = "获取对账单列表")
    @Parameters({
            @Parameter(name = "customerId", description = "显式客户ID（可选）", required = false),
            @Parameter(name = "status", description = "状态过滤（可选，仅 SENT, CONFIRMED, REJECTED）", required = false)
    })
    public CommonResult<List<PortalStatementRespVO>> getStatementList(
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "status", required = false) String status) {
        return success(portalStatementService.getStatementList(customerId, status));
    }

    @GetMapping(value = {"/{id}", "", "/get"})
    @PermitAll
    @Operation(summary = "获取对账单详情")
    @Parameters({
            @Parameter(name = "id", description = "对账单ID（可选）", required = false),
            @Parameter(name = "no", description = "对账单号（可选）", required = false),
            @Parameter(name = "customerId", description = "显式客户ID（可选）", required = false)
    })
    public CommonResult<PortalStatementDetailRespVO> getStatementDetail(
            @PathVariable(value = "id", required = false) Long id,
            @RequestParam(value = "no", required = false) String no,
            @RequestParam(value = "customerId", required = false) Long customerId) {
        return success(portalStatementService.getStatementDetail(id, no, customerId));
    }

    @PutMapping("/confirm")
    @PermitAll
    @Operation(summary = "门户客户确认对账单")
    public CommonResult<Boolean> confirmStatement(@Valid @RequestBody PortalStatementConfirmReqVO reqVO) {
        portalStatementService.confirmStatement(reqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @PermitAll
    @Operation(summary = "门户客户驳回对账单")
    public CommonResult<Boolean> rejectStatement(@Valid @RequestBody PortalStatementRejectReqVO reqVO) {
        portalStatementService.rejectStatement(reqVO);
        return success(true);
    }

}
