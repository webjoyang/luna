package com.luna.lunasrv.module.lunaerp.controller.production.greigeorder;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.greigeorder.GreigeOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理-胚布单")
@RestController
@RequestMapping("/lunaerp/production/greige-order")
@Validated
public class GreigeOrderController {

    @Resource
    private GreigeOrderService greigeOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建胚布单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:greige-order:create')")
    public CommonResult<Long> createGreigeOrder(@Valid @RequestBody GreigeOrderSaveReqVO createReqVO) {
        return success(greigeOrderService.createGreigeOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新胚布单（整体保存）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:greige-order:update')")
    public CommonResult<Boolean> updateGreigeOrder(@Valid @RequestBody GreigeOrderSaveReqVO updateReqVO) {
        greigeOrderService.updateGreigeOrder(updateReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得胚布单详情")
    @Parameter(name = "id", description = "胚布单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:greige-order:query')")
    public CommonResult<GreigeOrderRespVO> getGreigeOrder(@RequestParam("id") Long id) {
        return success(greigeOrderService.getGreigeOrder(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得胚布单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:greige-order:query')")
    public CommonResult<PageResult<GreigeOrderRespVO>> getGreigeOrderPage(@Validated GreigeOrderPageReqVO pageReqVO) {
        return success(greigeOrderService.getGreigeOrderPage(pageReqVO));
    }

    @GetMapping("/get-next-no")
    @Operation(summary = "预览下一个胚布单号（只读、不占号）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:greige-order:query')")
    public CommonResult<String> getNextGreigeOrderNo() {
        return success(greigeOrderService.getNextGreigeOrderNo());
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除胚布单")
    @Parameter(name = "id", description = "胚布单ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:greige-order:delete')")
    public CommonResult<Boolean> deleteGreigeOrder(@RequestParam("id") Long id) {
        greigeOrderService.deleteGreigeOrder(id);
        return success(true);
    }

}
