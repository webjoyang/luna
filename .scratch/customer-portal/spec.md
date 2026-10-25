# 客户门户 H5 - 免登发货查询/签收 + 对账单确认（W4 触点）

Status: ready-for-agent（14 号 grilling 两轮 Q1–Q10 + 默认值确认定稿，可按 session 施工）

## Problem Statement

商业化 W4「触点与验收」的客户侧触点：外部客户需要一个免登录入口，查看自己名下的发货单并登记签收，查看对账单并确认/驳回。前置契约已冻结：

- **10 号**：门户 4 端点契约 `/portal/finance/statement/`（list/{id}/confirm/reject），对账单确认身份字段 `confirm_by_name/time/ip` 由门户写入；10 号实现按显式 customerId 并完成隔离专测，本票接入时**仅替换身份来源**。
- **04 号**：发货单打印二维码冻结为 `{luna.portal.base-url}/portal/shipment?no={发货单号}`；infra_config 键 `luna.portal.base-url` 已冻结（未配置不阻断打印）。**零新表**基线沿用。
- **20 号**：出口单证门户查看页 `/portal/export-doc?no=` 一期不含（二期门户扩展时接页）。

本票新增：门户令牌签发/吊销/校验基础设施、门户 shipment 只读+签收端点、门户前端路由区、管理端链接管理。

## Grilling 定稿决策（14 号 ticket，Q1–Q10）

1. **Q1 工程落点 = luna-ui 单工程内门户路由区**：`/portal/*` 免登白名单路由 + 独立 PortalLayout + 独立 axios 实例，页面懒加载。理由：私有化买断单产物交付、一期仅 4–6 页、复用构建链与 CI；MPA/独立工程收益体现不出来。15 号车间 H5 沿用同一模式互相引用。
2. **Q2 签发 = 管理端生成 + 线下发**：业务员在客户档案点「生成门户链接」，复制后经微信/短信**人工**发送；一期不接短信网关、不做任何自动外发。
3. **Q3 身份模型 = token 直绑 customerId**：不建门户用户实体（`portal_user` 否决），签收人/确认人由客户在页面**手填姓名**回传（复用 shipment `signedByName`、10 号 `confirm_by_name` 字段语义）。审计粒度=客户+手填姓名，一期可接受。
4. **Q4 页面集**：发货单仅显示本客户 **COMPLETED** 单据，**金额透明展示**（含运费；04 号 `@Sensitive(AMOUNT)` 是内部权限位，门户客户看自己的单据属对账场景不适用）；签收**整单粒度**（复用现有 signStatus/signedByName/signedAt/signRemark 四字段，不改状态机）；后端新开 portal 端点，服务层复用既有签收逻辑，不暴露内部 `@PreAuthorize` 接口。
5. **Q5 适配 = 独立门户移动布局**：PortalLayout 移动优先（max-width 480px 居中兜底 PC 扫码场景），ElementPlus 现有组件 + 自定义 CSS；**不引 vant、不做 px-to-viewport**；主站不做响应式。
6. **Q6 token 生命周期**：**单客户单 token**（重新生成即吊销旧链接，语义=链接即客户门户钥匙）；**URL token 即 API 凭证**（前端存 sessionStorage、header `X-Portal-Token`、地址栏清理 `?t=`，一期不换发短会话）；**30 天滑动过期**（每次有效请求续期）+ 管理端显示剩余有效期 + 手动吊销。
7. **Q7 端点落点 = `/app-api` 前缀**：门户端点统一 `/app-api/lunaerp/portal/**`，`@PermitAll` 豁免 admin 鉴权 + 自研 `PortalTokenFilter` 校验注入上下文。10 号契约的 `/portal/finance/statement/` 作为模块内相对前缀不变，完整路径映射为 `/app-api/lunaerp/portal/finance/statement/*`（契约语义零变更，仅补全映射注记）。
8. **Q8 安全**：**限流做**（token 维度 Redis 固定窗口 60 次/分钟，超出 429）；**留痕做**（生成/吊销/签收/确认/驳回五处 `@LogRecord`，日志不含完整 token 仅记指纹）；**透出原则**：独立 PortalVO 白名单——金额/数量/品名/物流可看，客户手机号、内部备注、成本相关字段一律不透出。
9. **Q9 页面细节**：门户不提供打印（9a）；发货单详情展示逐行明细（品名/规格/色号/数量/卷数），签收按钮仅 PENDING 显示、二次确认弹窗手填签收人必填（9b）；列表仅时间倒序分页 + 状态 Tab，不做搜索框（9c）；对账单确认前手填姓名、驳回必填原因（9d，10 号契约）。
10. **Q10 前端落点**：`src/views/portal/`（正式目录，非 elmui 临时区）+ `src/api/portal/`；路由清单与 401 处理见下文；401 → 「链接已失效」页引导联系业务员重发。

默认值（按仓库惯例直接取，用户已确认）：错误码 **1-030-038** 段；按钮权限码 **10150**（避让退货 10141–10149）；**ADR-0044**（门户免登模型）；术语进 shared GLOSSARY；`_order.md` 序号 29。

## Solution

**零新表**。门户令牌全部存 Redis（主键 + 反向索引双键），无任何 DDL（infra_config 键 `luna.portal.base-url` 复用 04 号已冻结键）。架构决策见 **ADR-0044**（token 即身份、单 token 轮换、app-api 前缀隔离、无门户用户实体）。

### 门户令牌机制（ADR-0044 核心）

**存储（Redis 双键）**：

| 键 | 值 | TTL |
|---|---|---|
| `luna_portal:token:{token}` | JSON `{tenantId, customerId, customerName, generateTime}` | 30d，滑动续期 |
| `luna_portal:token_of:{tenantId}:{customerId}` | token 明文（反向索引，用于重发时吊销旧键与状态查询） | 30d，与主键同步续期 |

**生成**：`SecureRandom` 32 字节 → Base64URL 无填充（43 字符，≥128bit 熵，不可枚举）。事务语义：读反向索引 → 存在旧 token 则 DEL 旧主键 → 写新主键 + 更新反向索引。返回 `{token, url}`，URL = `{luna.portal.base-url}/portal/shipments?t={token}`；基址未配置时 url 为空、前端仅复制 token 并提示先配置基址。

**校验（PortalTokenFilter）**：仅拦截 `/app-api/lunaerp/portal/**`。解析 header `X-Portal-Token` → GET 主键，未命中统一返回 **401 + 1-030-038-000**（无效/过期/吊销/伪造不区分文案，防探测）；命中则双键 EXPIRE 续期 → 设置 `TenantContextHolder`（租户插件生效）+ `PortalContext`（ThreadLocal：tenantId/customerId）→ 请求结束清理 ThreadLocal。**限流**：`luna_portal:rl:{token}` 固定窗口 60s 计数，>60 返回 429 + 1-030-038-002（仅对有效 token 限流；无效 token 攻击面为 128bit 熵爆破不可行，不做 IP 限流）。

**吊销**：管理端按钮 DEL 双键。**失效兜底**：PortalContext 校验客户存在且未删除（deleted=0），否则按 1-030-038-001 拒绝（客户进回收站后门户不可见，不新增 RecycleBinHandler）。

### 管理端（admin-api，签发入口）

| 端点 | 说明 |
|---|---|
| `GET /admin-api/lunaerp/baseinfo/customer/portal-token?customerId=` | 状态：是否存在/生成时间/剩余天数 |
| `POST /admin-api/lunaerp/baseinfo/customer/portal-token/generate` | 生成/重新生成（重新生成=吊销旧链接，前端二次确认弹窗） |
| `DELETE /admin-api/lunaerp/baseinfo/customer/portal-token?customerId=` | 吊销 |

权限码 `lunaerp:baseinfo:customer:portal-token`（按钮 10150，挂客户管理菜单）；`@LogRecord` 生成/吊销各一条，**日志记 token 指纹（前 6 位），不记明文**。前端：客户详情页（elmui 临时区现有客户页面）新增「客户门户」卡片——状态（未开通/有效期至 xx）、生成/重新生成、复制链接、吊销。

### 门户端点（/app-api，@PermitAll + PortalTokenFilter）

| 端点 | 说明 |
|---|---|
| `GET /app-api/lunaerp/portal/shipment/page` | 本客户 COMPLETED 发货单分页，时间倒序；返回单号/日期/物流/签收状态/金额合计/运费 |
| `GET /app-api/lunaerp/portal/shipment/get?id=` | 详情：头 + 逐行明细（品名/规格/色号/数量/卷数）；**归属校验** customerId 不符 → 038-001 |
| `PUT /app-api/lunaerp/portal/shipment/sign` | body `{id, signStatus(SIGNED\|REFUSED), signedByName(必填), signRemark?}`；signedAt=服务器时间；服务层复用现有签收守卫（仅 COMPLETED、signStatus ∈ PENDING/SIGNED/REFUSED 条件更新），**零新状态** |
| `GET /app-api/lunaerp/portal/finance/statement/list` | 10 号契约：仅 SENT/CONFIRMED |
| `GET /app-api/lunaerp/portal/finance/statement/{id}` | 10 号契约：含全部行 |
| `PUT /app-api/lunaerp/portal/finance/statement/confirm` | 10 号契约：SENT→CONFIRMED，confirmByName 必填 |
| `PUT /app-api/lunaerp/portal/finance/statement/reject` | 10 号契约：必填 reject_reason，SENT→REJECTED |

10 号 statement 端点由 10 号按显式 customerId 实现；本票第 0 前置 session 将其身份来源替换为 `PortalContext`（保留 customerId 参数路径做内部代客场景），隔离专测不放松。

### 包结构（横切子包）

`com.luna.lunasrv.module.lunaerp.portal`（与 common 同级横切思路）：`controller/portal`（PortalShipmentController、PortalStatementController）、`service/portal`、`PortalTokenFilter`、`PortalContext`、`PortalRateLimiter`。shipment/statement 业务逻辑一律复用 business/finance 各域既有 Service，portal 层只做身份注入、归属校验、VO 裁剪。

### 前端（luna-ui）

**目录**：`src/views/portal/`（`shipment/list.vue`、`shipment/detail.vue`、`statement/list.vue`、`statement/detail.vue`、`invalid.vue`）+ `src/api/portal/` + `PortalLayout.vue`。正式目录，非 elmui 临时区。

**路由（constantRoutes 静态注册，不挂菜单）**：`/portal/shipments`（Tab：待签收/已签收/已拒收/全部）、`/portal/shipment/:id`、`/portal/statements`（Tab：待确认/已确认/已驳回）、`/portal/statement/:id`、`/portal/invalid`。**同时支持二维码冻结直达形态** `/portal/shipment?no=`、`/portal/statement?no=`（同一组件：有 `:id` 用 id，有 `?no=` 按 no 查询）——04/10 号冻结的二维码 URL 契约**不回改**。`permission.ts` whiteList 增加 `'/portal'` 一条（path 前缀 startsWith 规则覆盖全部子路径）。

**token 引导**：`?t=` 进入页（挂在各列表路由组件内）→ 存 sessionStorage → `history.replaceState` 清地址栏 → 进列表。**axios**：`src/config/axios/portal.ts` 独立实例（baseURL 同 `/app-api`，header `X-Portal-Token`，不引 user store、不带 Authorization）；响应 401 → 跳 `/portal/invalid`（文案：链接已失效，请联系业务员重新获取）；429 → toast 稍后再试。**扫码无会话场景**：直接打开 `?no=` 链接而无 token → invalid 页引导先从业务员链接进入门户（二维码印在纸上无法带可轮换凭证，此体验一期接受）。

**适配**：PortalLayout 移动优先，页面容器 max-width 480px 居中；顶部标题栏（门户名+客户名）+ 底部 Tab（发货单/对账单）。零新依赖。

### 错误码（1-030-038 段，`PortalErrorCodeConstants`）

| 码 | 含义 |
|---|---|
| `1-030-038-000` | 门户令牌无效或已过期（含伪造/吊销，统一文案防探测） |
| `1-030-038-001` | 门户数据归属不符（横向越权/客户已删除） |
| `1-030-038-002` | 门户请求过于频繁（限流） |
| `003~009` | 段内备用 |

无新字典。SQL 仅：按钮 10150 权限码幂等脚本（零 DDL、零数据迁移）。

## Testing Decisions

1. **令牌生命周期**：生成→旧 token 失效（重发即吊销）；吊销→401；30 天 TTL 滑动续期断言（请求前后 Redis TTL 变化）；伪造/未带 header/过期统一 401-000；反向索引与主键一致性（重发/吊销双键同步）。
2. **横向/纵向越权必测**：A 客户 token 访问 B 客户发货单（id 与 `?no=` 双入口）、对账单（{id} 与 confirm/reject）→ 038-001；跨租户 token（租户 X token 访问租户 Y 数据）→ 038-001；列表天然 customerId 过滤断言；`?no=` 形态与 `:id` 形态守卫等价。
3. **租户隔离**：PortalTokenFilter 设置 TenantContextHolder 后所有查询强制 tenant_id；ThreadLocal 请求结束清理（并发请求串数据专测）。
4. **限流**：61 次/分钟 → 429-002；窗口重置恢复。
5. **签收**：仅 COMPLETED 可签；signedByName 必填；signedAt=服务器时间；SIGNED/REFUSED 两态；与内部管理端签收并发条件更新（复用现有守卫语义，不出现双写竞争）；REFUSED 不产生任何库存/账务副作用（与现有 update-sign 一致）。
6. **对账单（10 号契约复核）**：list 仅 SENT/CONFIRMED；confirm SENT→CONFIRMED 记 confirm_by_name/time；reject 必填原因；CONFIRMED 再操作被 10 号既有守卫拒绝；门户确认与内部代客确认落同一状态字段。
7. **VO 白名单断言**：PortalShipmentRespVO / PortalStatementRespVO 序列化输出不含手机号、内部备注、成本类字段；金额字段存在且正确。
8. **留痕**：@LogRecord 五处（生成/吊销/签收/确认/驳回）断言，日志不含完整 token 明文。
9. **性能**：门户列表 P95<1s（万级）；Redis 校验路径 O(1)。

## Out of Scope（一期不做）

- **供应商门户**（Q13=A，二期）。
- 门户用户实体 / 多联系人账号 / 客户侧登录注册（Q3=A；二期按 portal_user 独立立项）。
- 短信/微信/邮件自动发送与模板消息（链接一律线下人工发送）。
- 门户打印、PDF 导出（9a）。
- 出口单证门户查看页 `/portal/export-doc`（20 号已冻结 URL，二期接页）。
- 扫码无会话直达单据详情（invalid 引导页兜底；二期可做"链接+会话"组合）。
- 签收/确认后站内通知业务员（@LogRecord 可查，推送二期）。
- 微信小程序原生端（Q6=A，移动端一律 H5）。
- IP 维度限流、验证码人机校验（token 128bit 熵下无必要）。

## 验收标准（统一 DoD 映射）

- [ ] 零新表：Redis 双键令牌 + 反向索引；SQL 仅按钮 10150 幂等脚本；`luna.portal.base-url` 复用 04 号键
- [ ] 门户端点收敛 `/app-api/lunaerp/portal/**`：@PermitAll + PortalTokenFilter（续期/上下文注入/清理）+ 60/min 限流
- [ ] 状态机零新增：签收复用 shipment 既有守卫（COMPLETED + PENDING/SIGNED/REFUSED 条件更新）、对账单走 10 号五态；门户不引入任何新状态
- [ ] 安全测试全绿：A↔B 横向越权（id/no 双入口）、跨租户、token 伪造/过期/吊销/未带 header、限流 429、ThreadLocal 并发隔离
- [ ] PortalVO 白名单断言（金额透明、内部字段零透出）；@LogRecord 五处留痕且不含完整 token
- [ ] 10 号契约身份来源替换（显式 customerId → PortalContext）+ 隔离专测复核；代客确认与门户确认同字段
- [ ] 二维码 URL 契约不回改：`/portal/shipment?no=`、`/portal/statement?no=` 直达路由兼容，无会话引导 invalid 页
- [ ] 管理端：生成/重新生成（吊销旧链接+二次确认）/复制/吊销/剩余有效期可用；基址未配置时降级提示
- [ ] 错误码 038 段登记 shared GLOSSARY 总表；术语（客户门户/门户令牌）进 shared GLOSSARY；ADR-0044 落 `docs/adr/`
- [ ] 前端验收清单（移动视口 375px）：生成链接→客户打开（?t= 清址）→发货单列表/详情→签收（手填姓名）→对账单列表/详情→确认→驳回路径→吊销后 401 invalid 页 全流程冒烟
- [ ] 常规列表 P95<1s（万级）；PortalTokenFilter 校验路径 O(1)

## Further Notes

### 跨票契约

- **15 号（车间移动 H5）**：工程落点（luna-ui 内免登路由区 + 独立 axios 实例 + constantRoutes 静态注册）、PortalLayout 适配策略、`@PermitAll`+自定义凭证 Filter 的横切包模式（portal 包）均由本票先行定型，15 号照抄引用（凭证类型改为员工账号体系，复用模式不复用 token 机制）。
- **20 号（出口单证）**：`/portal/export-doc?no=` 二期接页时直接复用本票 PortalContext 与路由模式。
- **10 号**：statement 4 端点身份来源替换为契约级变更点，在 10 号 spec 的门户注记处补映射说明（不改其余语义）。

### 参考

- ADR-0044（门户免登模型）；ADR-0037（域/横切包结构）；`.scratch/common-foundation/spec.md`（luna.portal.base-url 键、@LogRecord、@Sensitive）；`.scratch/customer-reconciliation/spec.md`（Q9 门户契约、对账单状态机）
