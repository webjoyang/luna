package com.luna.lunasrv.module.lunaerp.controller.production.iot;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.realtime.MachineRealtimeSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.iot.realtime.MachineRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - IoT 设备实时快照")
@RestController
@RequestMapping("/lunaerp/production/iot/machine-realtime")
@Validated
public class MachineRealtimeController {

    @Resource
    private MachineRealtimeService machineRealtimeService;

    @PostMapping("/record")
    @Operation(summary = "手工录入/更新设备实时快照（更新覆盖最新值，不回写织机状态）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:machine-realtime:record')")
    public CommonResult<Long> recordRealtimeData(@Valid @RequestBody MachineRealtimeSaveReqVO saveReqVO) {
        return success(machineRealtimeService.recordRealtimeData(saveReqVO));
    }

    @GetMapping("/get-latest")
    @Operation(summary = "查询设备最新快照（支持 vendorDeviceId 或 loomId）")
    @Parameter(name = "vendorDeviceId", description = "厂商设备标识", example = "DEV-001")
    @Parameter(name = "loomId", description = "织机ID", example = "2001")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:machine-realtime:query') or @ss.hasPermission('lunaerp:production:loom:query')")
    public CommonResult<MachineRealtimeRespVO> getLatest(
            @RequestParam(value = "vendorDeviceId", required = false) String vendorDeviceId,
            @RequestParam(value = "loomId", required = false) Long loomId) {
        if (loomId != null) {
            return success(machineRealtimeService.getLatestByLoomId(loomId));
        }
        return success(machineRealtimeService.getLatestByVendorDeviceId(vendorDeviceId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备实时快照分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:machine-realtime:query')")
    public CommonResult<PageResult<MachineRealtimeRespVO>> getMachineRealtimePage(@Validated MachineRealtimePageReqVO pageReqVO) {
        return success(machineRealtimeService.getMachineRealtimePage(pageReqVO));
    }

    @PostMapping("/collect-mock")
    @Operation(summary = "触发模拟采集（按有效绑定生成模拟值并覆盖快照，不启用真实定时任务）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:machine-realtime:collect')")
    public CommonResult<Integer> triggerSimulatedCollection() {
        return success(machineRealtimeService.triggerSimulatedCollection());
    }

}
