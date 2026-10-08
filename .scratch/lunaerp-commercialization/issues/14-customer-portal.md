# 14-customer-portal: 客户门户 H5

Type: grilling
Status: resolved
Blocked by: 10

## Question

设计客户门户最小集并产出 spec（W4，Q13=A），grilling 时先调 Skill "grilling" 与 "domain-modeling"：

1. token 免登机制：签发（短信/微信发给客户的链接）、有效期、撤销、单客户维度隔离、防遍历（不可枚举）；
2. 页面集：发货单查询 + 签收确认（复用现有 shipment 签收接口，签收即回传确认人=客户侧）；对账单查看 + 确认（依赖 10 号对账单确认状态）；
3. 工程落点：luna-ui 内移动端路由（免登录白名单路由）vs 独立轻量 H5 工程——需决定并给理由；
4. 安全边界：租户隔离测试、越权用例、脱敏（客户只看自己的数据）；
5. 移动端适配规范（响应式 or 独立移动布局）；
6. 供应商门户二期（写进非目标）。

产出：`.scratch/customer-portal/spec.md`（按统一 DoD）。

## Answer

两轮 grilling（Q1–Q10 + 默认值确认）定稿，spec 落 `.scratch/customer-portal/spec.md`（ready-for-agent），ADR-0044 落 `docs/adr/`。要点：

- **免登模型（ADR-0044）**：token 即身份直绑 customerId，不建门户用户实体，签收/确认人手填姓名；单客户单 token（重发即吊销、可手动吊销），128bit 随机防遍历，Redis 双键（主键+反向索引）零新表，30 天滑动过期；URL token 即 API 凭证（`X-Portal-Token`，不换发）。签发=管理端客户档案「生成门户链接」+业务员线下发（不接短信网关）。
- **端点**：门户端点挂 `/app-api/lunaerp/portal/**`（@PermitAll + 自研 PortalTokenFilter 注入 tenantId/customerId + 60/min 限流）；10 号冻结的 `/portal/finance/statement/` 契约仅替换身份来源、语义零变更；签收新开 portal 端点复用 shipment 既有守卫（整单粒度、零新状态）。
- **页面集**：发货单列表/详情（仅 COMPLETED、金额透明、明细行）+ 整单签收 + 对账单列表/详情/确认/驳回；`src/views/portal/` 正式目录 + `/portal/*` 免登白名单路由（constantRoutes 静态注册）+ 独立 PortalLayout 移动布局（max-width 480px 居中，不引 vant）；04/10 号二维码冻结 URL（`?no=` 直达）不回改、同组件兼容。
- **安全**：独立 PortalVO 白名单透出（内部字段/手机号/成本零透出）、@LogRecord 五处留痕（token 只记指纹）、token 维度限流；越权测试覆盖 A↔B（id/no 双入口）与跨租户。
- 落点：错误码 1-030-038（000 无效/001 归属不符/002 限流）、按钮 10150（避让退货 10141–10149）、术语进 shared GLOSSARY（客户门户/门户令牌/链接失效页）、`_order.md` 序号 29；非目标：供应商门户、门户用户体系、短信自动发送、门户打印、出口单证查看页（20 号二期）。15 号车间 H5 引用本票工程模式（免登路由区+独立 axios+横切凭证 Filter）。
