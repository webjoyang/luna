package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPortalTokenReqVO;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalTokenRespVO;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalTokenService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;

/**
 * 管理端 - 客户门户链接 Controller
 *
 * @author luna
 */
@Tag(name = "管理端 - 客户门户链接")
@RestController
@RequestMapping("/lunaerp/baseinfo/customer/portal-token")
@Validated
public class CustomerPortalTokenController {

    @Resource
    private PortalTokenService portalTokenService;

    @GetMapping
    @Operation(summary = "获得客户门户令牌状态")
    @Parameter(name = "customerId", description = "客户ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:portal-token')")
    public CommonResult<PortalTokenRespVO> getPortalToken(@RequestParam("customerId") Long customerId) {
        return success(portalTokenService.getTokenStatus(customerId));
    }

    @PostMapping("/generate")
    @Operation(summary = "生成/重新生成客户门户令牌")
    @Parameter(name = "customerId", description = "客户ID", required = false, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:portal-token')")
    @LogRecord(type = "CUSTOMER_PORTAL", subType = "GENERATE_TOKEN", bizNo = "{{#targetCustomerId}}",
            success = "生成客户【{{#targetCustomerId}}】门户链接，令牌指纹【{{#tokenFingerprint}}】")
    public CommonResult<PortalTokenRespVO> generatePortalToken(
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestBody(required = false) CustomerPortalTokenReqVO reqVO) {
        Long targetCustomerId = customerId != null ? customerId : (reqVO != null ? reqVO.getCustomerId() : null);
        if (targetCustomerId == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        LogRecordContext.putVariable("targetCustomerId", targetCustomerId);

        PortalTokenRespVO respVO = portalTokenService.generateToken(targetCustomerId);
        String fingerprint = respVO != null && respVO.getToken() != null
                ? respVO.getToken().substring(0, Math.min(respVO.getToken().length(), 6)) + "..."
                : "";
        LogRecordContext.putVariable("tokenFingerprint", fingerprint);

        return success(respVO);
    }

    @DeleteMapping
    @Operation(summary = "吊销客户门户令牌")
    @Parameter(name = "customerId", description = "客户ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:portal-token')")
    @LogRecord(type = "CUSTOMER_PORTAL", subType = "REVOKE_TOKEN", bizNo = "{{#customerId}}",
            success = "吊销客户【{{#customerId}}】门户链接，令牌指纹【{{#tokenFingerprint}}】")
    public CommonResult<Boolean> revokePortalToken(@RequestParam("customerId") Long customerId) {
        LogRecordContext.putVariable("customerId", customerId);
        String revokedToken = portalTokenService.revokeToken(customerId);
        String fingerprint = StringUtils.hasText(revokedToken)
                ? revokedToken.substring(0, Math.min(revokedToken.length(), 6)) + "..."
                : "";
        LogRecordContext.putVariable("tokenFingerprint", fingerprint);
        return success(true);
    }

}
