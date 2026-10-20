package com.luna.lunasrv.module.lunaerp.portal.service;

import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.infra.api.config.ConfigApi;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPortalTokenRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalTokenPayload;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
import static com.luna.lunasrv.module.lunaerp.enums.PortalErrorCodeConstants.PORTAL_TOKEN_INVALID;

/**
 * 客户门户令牌服务实现
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class PortalTokenServiceImpl implements PortalTokenService {

    public static final String PORTAL_BASE_URL_KEY = "luna.portal.base-url";
    private static final String TOKEN_MAIN_KEY_PREFIX = "luna_portal:token:";
    private static final String TOKEN_REVERSE_KEY_PREFIX = "luna_portal:token_of:";
    private static final long TOKEN_TTL_DAYS = 30L;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Resource
    private CustomerMapper customerMapper;

    @Resource
    private ConfigApi configApi;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @LogRecord(type = "CUSTOMER_PORTAL", subType = "GENERATE_TOKEN", bizNo = "{{#customerId}}",
            success = "生成客户【{{#customerId}}】门户令牌，指纹【{{#tokenFingerprint}}】")
    public CustomerPortalTokenRespVO generateToken(Long customerId) {
        CustomerDO customer = validateCustomerExists(customerId);
        Long tenantId = resolveTenantId(customer);

        String reverseKey = formatReverseKey(tenantId, customerId);
        // 1. 读反向索引，若存在旧 token 则 DEL 旧主键（单客户单 token，重发即吊销）
        String oldToken = stringRedisTemplate.opsForValue().get(reverseKey);
        if (StringUtils.hasText(oldToken)) {
            stringRedisTemplate.delete(formatMainKey(oldToken));
            log.info("[generateToken] 客户【{}】重新生成令牌，已吊销旧主键【{}】", customerId, oldToken.substring(0, Math.min(6, oldToken.length())));
        }

        // 2. 生成安全随机 32 字节 Base64URL 无填充（43 字符，≥128bit 熵）
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String newToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        LocalDateTime now = LocalDateTime.now();
        PortalTokenPayload payload = PortalTokenPayload.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .customerName(customer.getCompanyName())
                .generateTime(now)
                .build();

        // 3. 写入新主键与反向索引（30 天 TTL 滑动过期）
        String mainKey = formatMainKey(newToken);
        stringRedisTemplate.opsForValue().set(mainKey, JsonUtils.toJsonString(payload), TOKEN_TTL_DAYS, TimeUnit.DAYS);
        stringRedisTemplate.opsForValue().set(reverseKey, newToken, TOKEN_TTL_DAYS, TimeUnit.DAYS);

        // 4. 拼装门户 URL（luna.portal.base-url 未配置时为空不报错）
        String url = buildPortalUrl(newToken);

        String fingerprint = newToken.substring(0, Math.min(6, newToken.length()));
        LogRecordContext.putVariable("tokenFingerprint", fingerprint);

        return CustomerPortalTokenRespVO.builder()
                .exists(true)
                .token(newToken)
                .tokenFingerprint(fingerprint)
                .url(url)
                .generateTime(now)
                .remainingDays(TOKEN_TTL_DAYS)
                .build();
    }

    @Override
    public CustomerPortalTokenRespVO getTokenState(Long customerId) {
        CustomerDO customer = validateCustomerExists(customerId);
        Long tenantId = resolveTenantId(customer);

        String reverseKey = formatReverseKey(tenantId, customerId);
        String token = stringRedisTemplate.opsForValue().get(reverseKey);
        if (!StringUtils.hasText(token)) {
            return CustomerPortalTokenRespVO.builder().exists(false).build();
        }

        String mainKey = formatMainKey(token);
        String json = stringRedisTemplate.opsForValue().get(mainKey);
        if (!StringUtils.hasText(json)) {
            // 反向索引存在但主键已过期，清理残留反向索引保持一致性
            stringRedisTemplate.delete(reverseKey);
            return CustomerPortalTokenRespVO.builder().exists(false).build();
        }

        PortalTokenPayload payload = JsonUtils.parseObject(json, PortalTokenPayload.class);
        Long expireSeconds = stringRedisTemplate.getExpire(mainKey, TimeUnit.SECONDS);
        long remainingDays = (expireSeconds != null && expireSeconds > 0) ? (expireSeconds + 86399) / 86400 : 0;
        String url = buildPortalUrl(token);
        String fingerprint = token.substring(0, Math.min(6, token.length()));

        return CustomerPortalTokenRespVO.builder()
                .exists(true)
                .token(token)
                .tokenFingerprint(fingerprint)
                .url(url)
                .generateTime(payload != null ? payload.getGenerateTime() : null)
                .remainingDays(remainingDays)
                .build();
    }

    @Override
    @LogRecord(type = "CUSTOMER_PORTAL", subType = "REVOKE_TOKEN", bizNo = "{{#customerId}}",
            success = "吊销客户【{{#customerId}}】门户令牌，指纹【{{#tokenFingerprint}}】")
    public void revokeToken(Long customerId) {
        CustomerDO customer = validateCustomerExists(customerId);
        Long tenantId = resolveTenantId(customer);

        String reverseKey = formatReverseKey(tenantId, customerId);
        String token = stringRedisTemplate.opsForValue().get(reverseKey);
        if (StringUtils.hasText(token)) {
            String fingerprint = token.substring(0, Math.min(6, token.length()));
            LogRecordContext.putVariable("tokenFingerprint", fingerprint);
            stringRedisTemplate.delete(formatMainKey(token));
        } else {
            LogRecordContext.putVariable("tokenFingerprint", "NONE");
        }
        stringRedisTemplate.delete(reverseKey);
    }

    @Override
    public PortalTokenPayload verifyAndRenewToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw exception(PORTAL_TOKEN_INVALID);
        }
        String mainKey = formatMainKey(token);
        String json = stringRedisTemplate.opsForValue().get(mainKey);
        if (!StringUtils.hasText(json)) {
            throw exception(PORTAL_TOKEN_INVALID);
        }
        PortalTokenPayload payload = JsonUtils.parseObject(json, PortalTokenPayload.class);
        if (payload == null || payload.getCustomerId() == null) {
            throw exception(PORTAL_TOKEN_INVALID);
        }

        // 双键滑动续期 30 天
        stringRedisTemplate.expire(mainKey, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        stringRedisTemplate.expire(formatReverseKey(payload.getTenantId(), payload.getCustomerId()), TOKEN_TTL_DAYS, TimeUnit.DAYS);

        return payload;
    }

    private CustomerDO validateCustomerExists(Long customerId) {
        if (customerId == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        CustomerDO customer = customerMapper.selectById(customerId);
        if (customer == null || Boolean.TRUE.equals(customer.getDeleted())) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        return customer;
    }

    private Long resolveTenantId(CustomerDO customer) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        if (customer != null && customer.getTenantId() != null) {
            return customer.getTenantId();
        }
        return 0L;
    }

    private String buildPortalUrl(String token) {
        String baseUrl = configApi != null ? configApi.getConfigValueByKey(PORTAL_BASE_URL_KEY) : null;
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }
        String cleanBaseUrl = baseUrl.trim().replaceAll("/+$", "");
        return cleanBaseUrl + "/portal/shipments?t=" + token;
    }

    private String formatMainKey(String token) {
        return TOKEN_MAIN_KEY_PREFIX + token;
    }

    private String formatReverseKey(Long tenantId, Long customerId) {
        return TOKEN_REVERSE_KEY_PREFIX + (tenantId != null ? tenantId : 0L) + ":" + customerId;
    }

}
