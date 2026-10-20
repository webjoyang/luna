package com.luna.lunasrv.module.lunaerp.portal.service;

import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPortalTokenRespVO;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalTokenPayload;

/**
 * 客户门户令牌服务接口
 *
 * 核心机制（ADR-0044）：
 * 1. Redis 双键：主键 luna_portal:token:{token} + 反向索引 luna_portal:token_of:{tenantId}:{customerId}
 * 2. 单客户单 token，重新生成即吊销旧链接
 * 3. 30 天滑动过期（每次有效请求续期）
 *
 * @author luna
 */
public interface PortalTokenService {

    /**
     * 生成或重新生成客户门户令牌
     *
     * 事务语义：
     * 1. 读反向索引，若存在旧 token 则 DEL 旧主键
     * 2. 生成 32 字节 Base64URL 无填充（43 字符）安全随机 token
     * 3. 写新主键（30 天 TTL）并更新反向索引（30 天 TTL）
     * 4. 拼装门户 URL（luna.portal.base-url 未配置时为空不报错）
     *
     * @param customerId 客户ID
     * @return 门户令牌及完整访问信息
     */
    CustomerPortalTokenRespVO generateToken(Long customerId);

    /**
     * 获取客户当前门户令牌状态
     *
     * @param customerId 客户ID
     * @return 门户令牌状态及剩余有效天数
     */
    CustomerPortalTokenRespVO getTokenState(Long customerId);

    /**
     * 吊销客户门户令牌
     *
     * 事务语义：DEL 主键与反向索引双键
     *
     * @param customerId 客户ID
     */
    void revokeToken(Long customerId);

    /**
     * 校验令牌有效性并执行滑动续期（主键与反向索引双键续期 30 天）
     *
     * @param token 门户令牌明文
     * @return 令牌载荷信息
     */
    PortalTokenPayload verifyAndRenewToken(String token);

}
