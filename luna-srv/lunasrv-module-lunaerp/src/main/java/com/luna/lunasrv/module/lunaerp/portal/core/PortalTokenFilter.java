package com.luna.lunasrv.module.lunaerp.portal.core;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.util.servlet.ServletUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalTokenDTO;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.luna.lunasrv.module.lunaerp.enums.portal.PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED;
import static com.luna.lunasrv.module.lunaerp.enums.portal.PortalErrorCodeConstants.PORTAL_RATE_LIMIT_EXCEEDED;
import static com.luna.lunasrv.module.lunaerp.enums.portal.PortalErrorCodeConstants.PORTAL_TOKEN_INVALID;

/**
 * 客户门户令牌校验过滤器
 *
 * <p>拦截 /app-api/lunaerp/portal/** 请求：</p>
 * <ol>
 *   <li>解析 Header 中的 X-Portal-Token</li>
 *   <li>校验 Token 有效性（未命中统一 401 + PORTAL_TOKEN_INVALID）</li>
 *   <li>限流校验（超限 429 + PORTAL_RATE_LIMIT_EXCEEDED）</li>
 *   <li>校验客户存在且未删除（否则 403 + PORTAL_DATA_ACCESS_DENIED）</li>
 *   <li>滑动续期 Token</li>
 *   <li>注入 TenantContextHolder 与 PortalContext</li>
 *   <li>请求结束后在 finally 块中清理 ThreadLocal</li>
 * </ol>
 *
 * @author luna
 */
@Slf4j
@RequiredArgsConstructor
public class PortalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_PORTAL_TOKEN = "X-Portal-Token";
    public static final String PORTAL_API_PREFIX = "/app-api/lunaerp/portal/";

    private final PortalTokenService portalTokenService;
    private final PortalRateLimiter portalRateLimiter;
    private final CustomerMapper customerMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = (contextPath != null && !contextPath.isEmpty())
                ? uri.substring(contextPath.length())
                : uri;
        return !path.startsWith(PORTAL_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getHeader(HEADER_PORTAL_TOKEN);
        if (!StringUtils.hasText(token)) {
            log.warn("[doFilterInternal][请求 URL({}) 缺失 {} 请求头]", request.getRequestURI(), HEADER_PORTAL_TOKEN);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ServletUtils.writeJSON(response, CommonResult.error(PORTAL_TOKEN_INVALID.getCode(), PORTAL_TOKEN_INVALID.getMsg()));
            return;
        }

        // 1. 校验 Token 命中主键
        PortalTokenDTO tokenDTO = portalTokenService.getPortalToken(token);
        if (tokenDTO == null) {
            log.warn("[doFilterInternal][请求 URL({}) 携带无效或已过期的门户令牌]", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ServletUtils.writeJSON(response, CommonResult.error(PORTAL_TOKEN_INVALID.getCode(), PORTAL_TOKEN_INVALID.getMsg()));
            return;
        }

        // 2. 限流检查（60s 固定窗口 <= 60 次）
        if (!portalRateLimiter.incrementAndCheck(token)) {
            log.warn("[doFilterInternal][请求 URL({}) 门户令牌请求频率超限]", request.getRequestURI());
            response.setStatus(429);
            ServletUtils.writeJSON(response, CommonResult.error(PORTAL_RATE_LIMIT_EXCEEDED.getCode(), PORTAL_RATE_LIMIT_EXCEEDED.getMsg()));
            return;
        }

        // 3. 注入租户上下文并校验客户实体合法性
        TenantContextHolder.setTenantId(tokenDTO.getTenantId());
        try {
            CustomerDO customer = customerMapper.selectById(tokenDTO.getCustomerId());
            if (customer == null || Boolean.TRUE.equals(customer.getDeleted())) {
                log.warn("[doFilterInternal][请求 URL({}) 客户已删除或不存在: customerId={}]", request.getRequestURI(), tokenDTO.getCustomerId());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                ServletUtils.writeJSON(response, CommonResult.error(PORTAL_DATA_ACCESS_DENIED.getCode(), PORTAL_DATA_ACCESS_DENIED.getMsg()));
                return;
            }

            // 4. 滑动续期
            portalTokenService.renewToken(token);

            // 5. 注入门户客户上下文
            String customerName = StringUtils.hasText(customer.getCompanyName())
                    ? customer.getCompanyName()
                    : tokenDTO.getCustomerName();
            PortalContext.set(tokenDTO.getTenantId(), tokenDTO.getCustomerId(), customerName);

            // 继续执行后续链路
            filterChain.doFilter(request, response);
        } finally {
            // 6. 彻底清理 ThreadLocal 上下文
            TenantContextHolder.clear();
            PortalContext.clear();
        }
    }

}
