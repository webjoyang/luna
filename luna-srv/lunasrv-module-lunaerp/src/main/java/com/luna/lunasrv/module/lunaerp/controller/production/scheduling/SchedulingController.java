package com.luna.lunasrv.module.lunaerp.controller.production.scheduling;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttQueryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.GanttRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.scheduling.vo.PendingPoolRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.scheduling.SchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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
    @Operation(summary = "获得排产甘特图数据（织机行与任务条带）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<GanttRespVO> getGanttData(@Valid GanttQueryReqVO reqVO) {
        return success(schedulingService.getGanttData(reqVO));
    }

    @GetMapping("/pending-pool")
    @Operation(summary = "获得排产待排池数据（无日期任务与未排满织造单）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<PendingPoolRespVO> getPendingPool() {
        return success(schedulingService.getPendingPool());
    }

}
