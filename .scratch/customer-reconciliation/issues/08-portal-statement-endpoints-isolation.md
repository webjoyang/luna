# 08: 门户对账 4 端点 + 客户隔离专测

**What to build:** 门户前缀 `/portal/finance/statement/` 四端点：`GET /list`（仅 SENT/CONFIRMED）、`GET /{id}`（含全部行）、`PUT /confirm`（SENT→CONFIRMED，记录 confirm_by_name/time/ip）、`PUT /reject`（必填 reject_reason）。客户身份按显式 customerId 实现（14 号接入时仅替换身份来源），完成隔离专测：A 客户不可见/不可操作 B 客户单、不可遍历 id。代客确认（07 号）与门户确认落同一状态字段与 confirm_type 字典。

**Blocked by:** 07 客户对账单 — 发送/确认/驳回/作废 + 打印

**Status:** completed

- [x] `GET /portal/finance/statement/list`：按 customerId 过滤，仅返回 SENT/CONFIRMED 状态
- [x] `GET /portal/finance/statement/{id}`：含全部行，校验该单 customerId 与当前客户一致
- [x] `PUT /portal/finance/statement/confirm`：SENT→CONFIRMED，记录 confirm_type=ONLINE_PORTAL / confirm_by_name / confirm_time / confirm_ip
- [x] `PUT /portal/finance/statement/reject`：必填 reject_reason，SENT→REJECTED
- [x] 客户身份来源：本票按显式 customerId 参数实现并完成隔离专测；14 号接入时替换为 token 注入
- [x] confirm_type 字典 2278：ONLINE_PORTAL / ON_BEHALT（与 07 号代客确认同字段落库）
- [x] 测试：**客户隔离专测**——A 客户 list 不可见 B 客户单、GET {id} 越权拒绝、confirm/reject 越权拒绝、id 遍历不可越权；门户确认字段记录；门户与代客确认同字段落库
