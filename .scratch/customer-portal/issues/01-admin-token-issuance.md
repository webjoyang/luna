# 01: 管理端门户链接签发闭环（令牌机制 + 三端点 + 客户档案门户卡片）

**What to build:** 业务员在客户档案点「生成门户链接」，复制后经微信/短信人工发给客户（一期不接任何自动外发）。后端落地门户令牌机制（ADR-0044：Redis 双键、单客户单 token、重发即吊销、30 天滑动过期）与管理端签发三端点；前端客户详情页（elmui 临时区现有客户页面）新增「客户门户」卡片。可 demo：生成→复制链接→重新生成（旧链接失效，二次确认）→手动吊销→卡片显示未开通/剩余有效期。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Redis 双键落地：主键 `luna_portal:token:{token}`（JSON：tenantId/customerId/customerName/generateTime，30d TTL 滑动续期）+ 反向索引 `luna_portal:token_of:{tenantId}:{customerId}`（token 明文，与主键同步续期）
- [ ] token 生成：SecureRandom 32 字节 → Base64URL 无填充（43 字符，≥128bit 熵）；事务语义=读反向索引→存在旧 token 则 DEL 旧主键→写新主键+更新反向索引；返回 `{token, url}`，url=`{luna.portal.base-url}/portal/shipments?t={token}`，基址未配置时 url 为空不报错
- [ ] 管理端三端点（admin-api，挂客户管理）：`GET /lunaerp/baseinfo/customer/portal-token?customerId=`（是否存在/生成时间/剩余天数）、`POST .../portal-token/generate`（重新生成=吊销旧链接）、`DELETE .../portal-token?customerId=`（吊销，DEL 双键）
- [ ] 权限码 `lunaerp:baseinfo:customer:portal-token`（按钮 10150，避让退货 10141–10149）幂等 SQL 脚本；零 DDL、零数据迁移
- [ ] `@LogRecord` 生成/吊销各一条，日志只记 token 指纹（前 6 位），不记明文
- [ ] `PortalErrorCodeConstants`：038 段 000（令牌无效或过期，统一文案防探测）/001（归属不符）/002（限流），003–009 备用
- [ ] 前端客户详情页「客户门户」卡片：状态（未开通/有效期至 xx）、生成/重新生成（二次确认弹窗提示旧链接失效）/复制链接/吊销；基址未配置时降级提示（仅复制 token，提示先配置基址）
- [ ] 专测：生成→旧 token 立即失效（重发即吊销）；吊销→主键反向索引双删一致；有效请求前后 Redis TTL 变化（滑动续期）；重发/吊销后双键一致性
