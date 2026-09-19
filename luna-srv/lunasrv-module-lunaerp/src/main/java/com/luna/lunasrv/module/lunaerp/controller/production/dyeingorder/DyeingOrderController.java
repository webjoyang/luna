package com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.production.dyeingorder.DyeingOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

// 注意：本模块包名是 controller.production（不含 .controller.admin. 段），框架不会自动追加
// /admin-api 前缀，因此类级 @RequestMapping 必须显式写全 /admin-api 路径（前端 axios baseURL 为 /admin-api）。
@Tag(name = "生产管理-染色单")
@RestController
@RequestMapping("/admin-api/lunaerp/production/dyeing-order")
@Validated
public class DyeingOrderController {

    @Resource
    private DyeingOrderService dyeingOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建染色单")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:dyeing-order:create')")
    public CommonResult<Long> createDyeingOrder(@Valid @RequestBody DyeingOrderSaveReqVO createReqVO) {
        return success(dyeingOrderService.createDyeingOrder(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得染色单分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:dyeing-order:query')")
    public CommonResult<PageResult<DyeingOrderRespVO>> getDyeingOrderPage(@Validated DyeingOrderPageReqVO pageReqVO) {
        return success(dyeingOrderService.getDyeingOrderPage(pageReqVO));
    }

}
