package com.luna.lunasrv.module.lunaerp.portal.controller.portal.auth;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.auth.vo.PortalAuthInfoRespVO;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 客户门户 - 认证与身份信息控制器
 *
 * <p>端点统一挂在 /app-api/lunaerp/portal/** 下，标注 @PermitAll 豁免 Spring Security admin 鉴权，
 * 由自定义 PortalTokenFilter 校验令牌并注入 PortalContext 上下文。</p>
 *
 * @author luna
 */
@Tag(name = "客户门户 - 认证信息")
@RestController
@RequestMapping("/app-api/lunaerp/portal/auth")
@Validated
public class PortalAuthController {

    @GetMapping("/info")
    @PermitAll
    @Operation(summary = "获取当前客户门户身份信息")
    public CommonResult<PortalAuthInfoRespVO> getPortalAuthInfo() {
        return success(PortalAuthInfoRespVO.builder()
                .customerId(PortalContext.getCustomerId())
                .customerName(PortalContext.getCustomerName())
                .build());
    }

}
