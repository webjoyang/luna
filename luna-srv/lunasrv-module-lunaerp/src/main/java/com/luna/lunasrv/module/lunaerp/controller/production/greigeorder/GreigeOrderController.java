package com.luna.lunasrv.module.lunaerp.controller.production.greigeorder;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.greigeorder.GreigeOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

// 注意：本模块包名是 controller.production（不含 .controller.admin. 段），框架不会自动追加
// /admin-api 前缀，因此类级 @RequestMapping 必须显式写全 /admin-api 路径（前端 axios baseURL 为 /admin-api）。
@Tag(name = "生产管理-胚布单")
@RestController
@RequestMapping("/admin-api/lunaerp/production/greige-order")
@Validated
public class GreigeOrderController {

    @Resource
    private GreigeOrderService greigeOrderService;

    @GetMapping("/page")
    @Operation(summary = "获得胚布单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:greige-order:query')")
    public CommonResult<PageResult<GreigeOrderRespVO>> getGreigeOrderPage(@Validated GreigeOrderPageReqVO pageReqVO) {
        return success(greigeOrderService.getGreigeOrderPage(pageReqVO));
    }

}
