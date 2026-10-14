package com.luna.lunasrv.module.lunaerp.controller.production.scheduling;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.PendingPoolRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.scheduling.SchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - 排产中心")
@RestController
@RequestMapping("/lunaerp/production/weaving-order-task")
@Validated
public class SchedulingController {

    @Resource
    private SchedulingService schedulingService;

    @GetMapping("/gantt")
    @Operation(summary = "获得甘特图数据（织机行 + 任务条带 + 冲突标记）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<GanttRespVO> getGantt(@Validated GanttReqVO reqVO) {
        return success(schedulingService.getGantt(reqVO));
    }

    @GetMapping("/pending-pool")
    @Operation(summary = "获得待排池数据（未排期任务 + 未排满织造单）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<PendingPoolRespVO> getPendingPool() {
        return success(schedulingService.getPendingPool());
    }

}
