package com.luna.lunasrv.module.lunaerp.portal.core.filter;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.util.servlet.ServletUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalRateLimiter;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalTokenPayload;
import com.luna.lunasrv.module.lunaerp.portal.service.PortalTokenService;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.luna.lunasrv.module.lunaerp.enums.PortalErrorCodeConstants.*;

/**
 * 客户门户令牌校验与上下文注入过滤器
 *
 * 仅拦截 /app-api/lunaerp/portal/** 请求：
 * 1. 提取 header X-Portal-Token；未带或校验失败统一返回 401 + PORTAL_TOKEN_INVALID (1-030-038-000)
 * 2. 对有效 token 进行固定窗口限流（60 次/分钟）；超出返回 429 + PORTAL_REQUEST_TOO_FREQUENT (1-030-038-002)
 * 3. 校验通过后注入 TenantContextHolder 与 PortalContext (ThreadLocal)
 * 4. 校验客户存在且未删除 (deleted=0)；失效则返回 403 + PORTAL_DATA_ACCESS_DENIED (1-030-038-001)
 * 5. 请求完成在 finally 块清理 PortalContext 与 TenantContextHolder
 */
@Slf4j
@Component
public class PortalTokenFilter extends OncePerRequestFilter {

    public static final String PORTAL_API_PATTERN = "/app-api/lunaerp/portal/**";
    public static final String PORTAL_TOKEN_HEADER = "X-Portal-Token";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Resource
    private PortalTokenService portalTokenService;

    @Resource
    private PortalRateLimiter portalRateLimiter;

    @Resource
    private CustomerMapper customerMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        if (!pathMatcher.match(PORTAL_API_PATTERN, uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 放行 CORS 预检请求
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(PORTAL_TOKEN_HEADER);
        if (!StringUtils.hasText(token)) {
            rejectUnauthorized(response);
            return;
        }

        PortalTokenPayload payload;
        try {
            payload = portalTokenService.verifyAndRenewToken(token.trim());
        } catch (ServiceException ex) {
            log.warn("[doFilterInternal][门户令牌无效或已过期, uri={}]", uri);
            rejectUnauthorized(response);
            return;
        } catch (Throwable ex) {
            log.error("[doFilterInternal][校验门户令牌发生异常, uri={}]", uri, ex);
            rejectUnauthorized(response);
            return;
        }

        if (payload == null || payload.getCustomerId() == null) {
            rejectUnauthorized(response);
            return;
        }

        // 限流校验（仅对有效 token 进行固定窗口 60s 限流）
        if (!portalRateLimiter.tryAcquire(token.trim())) {
            log.warn("[doFilterInternal][门户请求过于频繁, token={}, uri={}]", maskToken(token), uri);
            response.setStatus(429);
            ServletUtils.writeJSON(response, CommonResult.error(PORTAL_REQUEST_TOO_FREQUENT.getCode(), PORTAL_REQUEST_TOO_FREQUENT.getMsg()));
            return;
        }

        // 注入租户与门户上下文
        TenantContextHolder.setTenantId(payload.getTenantId());
        PortalContext.set(payload);

        try {
            // 失效兜底：校验客户存在且未被删除（进回收站后门户不可见）
            CustomerDO customer = customerMapper.selectById(payload.getCustomerId());
            if (customer == null || Boolean.TRUE.equals(customer.getDeleted())) {
                log.warn("[doFilterInternal][客户不存在或已被删除, customerId={}, uri={}]", payload.getCustomerId(), uri);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                ServletUtils.writeJSON(response, CommonResult.error(PORTAL_DATA_ACCESS_DENIED.getCode(), PORTAL_DATA_ACCESS_DENIED.getMsg()));
                return;
            }

            filterChain.doFilter(request, response);
        } finally {
            PortalContext.clear();
            TenantContextHolder.clear();
        }
    }

    private void rejectUnauthorized(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ServletUtils.writeJSON(response, CommonResult.error(PORTAL_TOKEN_INVALID.getCode(), PORTAL_TOKEN_INVALID.getMsg()));
    }

    private String maskToken(String token) {
        if (!StringUtils.hasText(token) || token.length() <= 6) {
            return "******";
        }
        return token.substring(0, 6) + "...";
    }
}
