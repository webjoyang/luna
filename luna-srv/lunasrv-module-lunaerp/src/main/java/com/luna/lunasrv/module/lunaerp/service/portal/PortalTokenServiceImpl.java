package com.luna.lunasrv.module.lunaerp.service.portal;

import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.infra.api.config.ConfigApi;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;

/**
 * 客户门户令牌服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class PortalTokenServiceImpl implements PortalTokenService {

    private static final String KEY_TOKEN = "luna_portal:token:%s";
    private static final String KEY_TOKEN_OF = "luna_portal:token_of:%s:%s";
    private static final long TOKEN_TTL_DAYS = 30L;

    private final SecureRandom secureRandom = new SecureRandom();

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private ConfigApi configApi;

    @Override
    public PortalTokenRespVO generateToken(Long customerId) {
        CustomerDO customer = validateCustomer(customerId);
        Long tenantId = getTenantId(customer);

        String reverseKey = String.format(KEY_TOKEN_OF, tenantId, customerId);
        String oldToken = stringRedisTemplate.opsForValue().get(reverseKey);
        if (StringUtils.hasText(oldToken)) {
            stringRedisTemplate.delete(String.format(KEY_TOKEN, oldToken));
        }

        // SecureRandom 32 字节 -> Base64URL 无填充（43 字符）
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String newToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();

        PortalTokenDTO dto = PortalTokenDTO.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .customerName(customer.getCompanyName())
                .generateTime(now)
                .build();

        String mainKey = String.format(KEY_TOKEN, newToken);
        stringRedisTemplate.opsForValue().set(mainKey, JsonUtils.toJsonString(dto), TOKEN_TTL_DAYS, TimeUnit.DAYS);
        stringRedisTemplate.opsForValue().set(reverseKey, newToken, TOKEN_TTL_DAYS, TimeUnit.DAYS);

        String url = buildPortalUrl(newToken);

        return PortalTokenRespVO.builder()
                .token(newToken)
                .url(url)
                .generateTime(now)
                .expireDays((int) TOKEN_TTL_DAYS)
                .expireTime(now.plusDays(TOKEN_TTL_DAYS))
                .build();
    }

    @Override
    public PortalTokenRespVO getTokenStatus(Long customerId) {
        CustomerDO customer = validateCustomer(customerId);
        Long tenantId = getTenantId(customer);

        String reverseKey = String.format(KEY_TOKEN_OF, tenantId, customerId);
        String token = stringRedisTemplate.opsForValue().get(reverseKey);
        if (!StringUtils.hasText(token)) {
            return null;
        }

        String mainKey = String.format(KEY_TOKEN, token);
        String json = stringRedisTemplate.opsForValue().get(mainKey);
        if (!StringUtils.hasText(json)) {
            stringRedisTemplate.delete(reverseKey);
            return null;
        }

        Long ttlSeconds = stringRedisTemplate.getExpire(mainKey, TimeUnit.SECONDS);
        if (ttlSeconds == null || ttlSeconds <= 0) {
            stringRedisTemplate.delete(mainKey);
            stringRedisTemplate.delete(reverseKey);
            return null;
        }

        PortalTokenDTO dto = JsonUtils.parseObject(json, PortalTokenDTO.class);
        String url = buildPortalUrl(token);
        int expireDays = (int) Math.max(1, (ttlSeconds + 86399) / 86400);
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(ttlSeconds);

        return PortalTokenRespVO.builder()
                .token(token)
                .url(url)
                .generateTime(dto != null ? dto.getGenerateTime() : null)
                .expireDays(expireDays)
                .expireTime(expireTime)
                .build();
    }

    @Override
    public String revokeToken(Long customerId) {
        CustomerDO customer = validateCustomer(customerId);
        Long tenantId = getTenantId(customer);

        String reverseKey = String.format(KEY_TOKEN_OF, tenantId, customerId);
        String token = stringRedisTemplate.opsForValue().get(reverseKey);
        if (StringUtils.hasText(token)) {
            stringRedisTemplate.delete(String.format(KEY_TOKEN, token));
        }
        stringRedisTemplate.delete(reverseKey);
        return token;
    }

    @Override
    public boolean renewToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        String mainKey = String.format(KEY_TOKEN, token);
        String json = stringRedisTemplate.opsForValue().get(mainKey);
        if (!StringUtils.hasText(json)) {
            return false;
        }

        PortalTokenDTO dto = JsonUtils.parseObject(json, PortalTokenDTO.class);
        if (dto == null) {
            return false;
        }

        stringRedisTemplate.expire(mainKey, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        String reverseKey = String.format(KEY_TOKEN_OF, dto.getTenantId(), dto.getCustomerId());
        stringRedisTemplate.expire(reverseKey, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        return true;
    }

    @Override
    public PortalTokenDTO getPortalToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }

        String mainKey = String.format(KEY_TOKEN, token);
        String json = stringRedisTemplate.opsForValue().get(mainKey);
        if (!StringUtils.hasText(json)) {
            return null;
        }

        return JsonUtils.parseObject(json, PortalTokenDTO.class);
    }

    private CustomerDO validateCustomer(Long customerId) {
        if (customerId == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        CustomerDO customer = customerMapper.selectById(customerId);
        if (customer == null || Boolean.TRUE.equals(customer.getDeleted())) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        return customer;
    }

    private Long getTenantId(CustomerDO customer) {
        if (customer.getTenantId() != null) {
            return customer.getTenantId();
        }
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId != null ? tenantId : 0L;
    }

    private String buildPortalUrl(String token) {
        String baseUrl = configApi != null ? configApi.getConfigValueByKey("luna.portal.base-url") : null;
        if (StringUtils.hasText(baseUrl)) {
            String cleanBaseUrl = baseUrl.trim().replaceAll("/+$", "");
            return cleanBaseUrl + "/portal/shipments?t=" + token;
        }
        return null;
    }

}
