# 06: 收口——全链路冒烟 + 越权矩阵 + 性能与 DoD 核对

**What to build:** 客户门户特性收口：375px 移动视口全流程冒烟、安全越权矩阵复核、性能验证与统一 DoD 逐项核对。不新增业务代码，产出验收证据。

**Blocked by:** 03（门户发货单只读）, 04（门户签收）, 05（门户对账单确认）

**Status:** closed

- [x] 前端验收清单全流程冒烟（移动视口 375px）：生成链接→客户打开（`?t=` 清址）→发货单列表/详情→签收（手填姓名）→对账单列表/详情→确认→驳回路径→吊销后 401 invalid 页
- [x] 越权矩阵复核全绿：横向越权（发货单 id/`?no=` 双入口、对账单 {id}/confirm/reject 全入口）、跨租户、token 伪造/过期/吊销/未带 header 统一 401-000、限流 429、ThreadLocal 并发隔离
- [x] VO 白名单断言汇总复核：PortalShipmentRespVO / PortalStatementRespVO 金额透明、手机号/内部备注/成本类字段零透出
- [x] `@LogRecord` 五处（生成/吊销/签收/确认/驳回）断言齐全且日志不含完整 token 明文（仅指纹）
- [x] 性能：门户列表 P95<1s（万级）；PortalTokenFilter 校验路径 O(1)（Redis 直查）
- [x] 统一 DoD 逐项核对：零新表（Redis 双键令牌 + 反向索引，仅 10150 按钮幂等 SQL）、`luna.portal.base-url` 复用 04 号冻结键、状态机零新增（签收复用既有守卫、对账走 10 号五态）、错误码 038 段与术语在 shared GLOSSARY 核对（grilling 已落，施工如有偏移回写）、ADR-0044 在 docs/adr/ 已 Accepted
