package com.luna.lunasrv.module.lunaerp.controller.production.iot;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.binding.DeviceBindingSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.iot.binding.DeviceBindingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - IoT 设备绑定")
@RestController
@RequestMapping("/lunaerp/production/iot/device-binding")
@Validated
public class DeviceBindingController {

    @Resource
    private DeviceBindingService deviceBindingService;

    @PostMapping("/create")
    @Operation(summary = "创建设备绑定")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:device-binding:create')")
    public CommonResult<Long> createDeviceBinding(@Valid @RequestBody DeviceBindingSaveReqVO createReqVO) {
        return success(deviceBindingService.createDeviceBinding(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改设备绑定")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:device-binding:update')")
    public CommonResult<Boolean> updateDeviceBinding(@Valid @RequestBody DeviceBindingSaveReqVO updateReqVO) {
        deviceBindingService.updateDeviceBinding(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备绑定")
    @Parameter(name = "id", description = "绑定ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:device-binding:delete')")
    public CommonResult<Boolean> deleteDeviceBinding(@RequestParam("id") Long id) {
        deviceBindingService.deleteDeviceBinding(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备绑定详情")
    @Parameter(name = "id", description = "绑定ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:device-binding:query')")
    public CommonResult<DeviceBindingRespVO> getDeviceBinding(@RequestParam("id") Long id) {
        return success(deviceBindingService.getDeviceBinding(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备绑定分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:device-binding:query')")
    public CommonResult<PageResult<DeviceBindingRespVO>> getDeviceBindingPage(@Validated DeviceBindingPageReqVO pageReqVO) {
        return success(deviceBindingService.getDeviceBindingPage(pageReqVO));
    }

    @GetMapping("/get-by-loom")
    @Operation(summary = "根据织机ID获得设备绑定")
    @Parameter(name = "loomId", description = "织机ID", required = true, example = "2001")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:iot:device-binding:query') or @ss.hasPermission('lunaerp:production:loom:query')")
    public CommonResult<DeviceBindingRespVO> getByLoomId(@RequestParam("loomId") Long loomId) {
        return success(deviceBindingService.getByLoomId(loomId));
    }

}
