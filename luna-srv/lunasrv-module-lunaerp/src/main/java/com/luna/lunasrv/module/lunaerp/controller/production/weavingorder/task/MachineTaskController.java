package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskHaltReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.task.vo.MachineTaskSaveRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.task.MachineTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - 机台任务")
@RestController
@RequestMapping("/lunaerp/production/weaving-order-task")
@Validated
public class MachineTaskController {

    @Resource
    private MachineTaskService machineTaskService;

    @PostMapping("/create")
    @Operation(summary = "创建机台任务（排产到织机）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:create') or @ss.hasPermission('lunaerp:production:weaving-order:update') or @ss.hasPermission('lunaerp:production:weaving-order:create')")
    public CommonResult<MachineTaskSaveRespVO> createMachineTask(@Valid @RequestBody MachineTaskSaveReqVO createReqVO) {
        return success(machineTaskService.createMachineTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改机台任务（排期/挡车工/匹数/织机）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:update') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<MachineTaskSaveRespVO> updateMachineTask(@Valid @RequestBody MachineTaskSaveReqVO updateReqVO) {
        return success(machineTaskService.updateMachineTask(updateReqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除机台任务（仅待开始 PENDING 态允许）")
    @Parameter(name = "id", description = "机台任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:delete') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> deleteMachineTask(@RequestParam("id") Long id) {
        machineTaskService.deleteMachineTask(id);
        return success(true);
    }

    @PutMapping("/halt")
    @Operation(summary = "机台任务停机（已完工禁停，停机原因必填，记录 haltAt）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:update') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> haltMachineTask(@Valid @RequestBody MachineTaskHaltReqVO haltReqVO) {
        machineTaskService.haltMachineTask(haltReqVO);
        return success(true);
    }

    @PutMapping("/resume")
    @Operation(summary = "机台任务恢复（解除停机态，恢复至 PENDING）")
    @Parameter(name = "id", description = "机台任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:update') or @ss.hasPermission('lunaerp:production:weaving-order:update')")
    public CommonResult<Boolean> resumeMachineTask(@RequestParam("id") Long id) {
        machineTaskService.resumeMachineTask(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得机台任务详情")
    @Parameter(name = "id", description = "机台任务ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<MachineTaskRespVO> getMachineTask(@RequestParam("id") Long id) {
        return success(machineTaskService.getMachineTask(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得机台任务分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<PageResult<MachineTaskRespVO>> getMachineTaskPage(@Validated MachineTaskPageReqVO pageReqVO) {
        return success(machineTaskService.getMachineTaskPage(pageReqVO));
    }

    @GetMapping("/get-next-code")
    @Operation(summary = "预览下一个机台任务编号（只读、不占号）")
    @Parameter(name = "weavingOrderId", description = "关联织造单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:weaving-order-task:query') or @ss.hasPermission('lunaerp:production:weaving-order:query')")
    public CommonResult<String> getNextMachineTaskCode(@RequestParam("weavingOrderId") Long weavingOrderId) {
        return success(machineTaskService.getNextMachineTaskCode(weavingOrderId));
    }

}
