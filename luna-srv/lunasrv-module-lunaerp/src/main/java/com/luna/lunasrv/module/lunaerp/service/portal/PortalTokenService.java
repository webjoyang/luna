package com.luna.lunasrv.module.lunaerp.service.portal;

/**
 * 客户门户令牌服务接口
 *
 * 遵循 ADR-0044：
 * 1. 令牌即身份：绑定 {tenantId, customerId}
 * 2. 单令牌轮换：每客户唯一有效令牌，重新生成即吊销旧令牌
 * 3. 30 天滑动过期
 *
 * @author luna
 */
public interface PortalTokenService {

    /**
     * 生成/重新生成客户门户令牌
     *
     * <p>事务语义：读反向索引 → 若存在旧令牌则立即删除旧主键 → 写入新主键与反向索引，TTL 均为 30 天。</p>
     *
     * @param customerId 客户ID
     * @return 令牌响应 VO（含 token、url、生成时间、剩余天数、过期时间）
     */
    PortalTokenRespVO generateToken(Long customerId);

    /**
     * 查询客户门户令牌状态
     *
     * @param customerId 客户ID
     * @return 令牌响应 VO；若未开通或已过期则返回 null
     */
    PortalTokenRespVO getTokenStatus(Long customerId);

    /**
     * 吊销客户门户令牌
     *
     * <p>读反向索引并删除主键与反向索引双键。</p>
     *
     * @param customerId 客户ID
     * @return 被吊销的令牌明文（若未开通或已过期则返回 null）
     */
    String revokeToken(Long customerId);

    /**
     * 滑动续期客户门户令牌（TTL 重置为 30 天）
     *
     * @param token 门户令牌
     * @return 是否成功续期
     */
    boolean renewToken(String token);

    /**
     * 获取并校验客户门户令牌
     *
     * @param token 门户令牌
     * @return 令牌元数据 DTO；若不存在或已过期则返回 null
     */
    PortalTokenDTO getPortalToken(String token);

    /**
     * 校验客户门户令牌（getPortalToken 别名）
     *
     * @param token 门户令牌
     * @return 令牌元数据 DTO；若不存在或已过期则返回 null
     */
    default PortalTokenDTO validateToken(String token) {
        return getPortalToken(token);
    }

}
