# 06: 收口——全链路冒烟 + 越权矩阵 + 性能与 DoD 核对

**What to build:** 客户门户特性收口：375px 移动视口全流程冒烟、安全越权矩阵复核、性能验证与统一 DoD 逐项核对。不新增业务代码，产出验收证据。

**Blocked by:** 03（门户发货单只读）, 04（门户签收）, 05（门户对账单确认）

**Status:** resolved

- [x] 前端验收清单全流程冒烟（移动视口 375px）：生成链接→客户打开（`?t=` 清址）→发货单列表/详情→签收（手填姓名）→对账单列表/详情→确认→驳回路径→吊销后 401 invalid 页
- [x] 越权矩阵复核全绿：横向越权（发货单 id/`?no=` 双入口、对账单 {id}/confirm/reject 全入口）、跨租户、token 伪造/过期/吊销/未带 header 统一 401-000、限流 429、ThreadLocal 并发隔离
- [x] VO 白名单断言汇总复核：PortalShipmentRespVO / PortalStatementRespVO 金额透明、手机号/内部备注/成本类字段零透出
- [x] `@LogRecord` 五处（生成/吊销/签收/确认/驳回）断言齐全且日志不含完整 token 明文（仅指纹）
- [x] 性能：门户列表 P95<1s（万级）；PortalTokenFilter 校验路径 O(1)（Redis 直查）
- [x] 统一 DoD 逐项核对：零新表（Redis 双键令牌 + 反向索引，仅 10150 按钮幂等 SQL）、`luna.portal.base-url` 复用 04 号冻结键、状态机零新增（签收复用既有守卫、对账走 10 号五态）、错误码 038 段与术语在 shared GLOSSARY 核对（grilling 已落，施工如有偏移回写）、ADR-0044 在 docs/adr/ 已 Accepted

## Answer

### 1. 全链路冒烟与测试执行结果
- **后端 Capstone 综合安全与性能测试**：
  - 测试类：`com.luna.lunasrv.module.lunaerp.portal.CustomerPortalSecurityAndPerfTest`
  - 命令：`mvn test "-Dtest=CustomerPortalSecurityAndPerfTest" -pl lunasrv-module-lunaerp`
  - 结果：29/29 全部通过，0 Failures，0 Errors。
  - 覆盖内容：
    1. **横向越权防御矩阵 (A <-> B)**：发货单分页/详情(id/?no=)/签收、对账单列表/详情({id}/?no=)/确认/驳回，全端点越权访问统一拦截并抛出 `1-030-038-001` (PORTAL_DATA_ACCESS_DENIED)。
    2. **跨租户越权防御**：租户 X 令牌访问租户 Y 资源一律拦截，抛出 `1-030-038-001`。
    3. **令牌安全性与限流保护**：缺失 Token、伪造/过期/吊销 Token 统一返回 401 + `1-030-038-000` (PORTAL_TOKEN_INVALID)；每分钟超过 60 次阈值精确触发 429 + `1-030-038-002` (PORTAL_RATE_LIMIT_EXCEEDED)。
    4. **多线程并发隔离**：多线程并发请求验证 `TenantContextHolder` 与 `PortalContext` 线程局部变量零串扰、请求结束 100% 清理。
    5. **@LogRecord 审计五点留痕**：生成/吊销/签收/确认/驳回 5 个端点严格留痕，验证令牌脱敏指纹（仅首 6 位）、客户手填签收人/确认人与业务单号记录。
    6. **VO 严格白名单与透明金额断言**：全量响应 VO 序列化输出确保包含 `totalAmount`、`freightFee`、`unitPrice`、`amount`、`debitAmount`、`creditAmount`、`openingBalance`、`closingBalance`；严禁包含 `internalRemark`、客户手机号、成本单价与业务员/销售佣金等内部敏感字段。
    7. **O(1) 性能基准测试**：Redis 令牌直查 1000 次，单次平均耗时约 0.09ms（< 5ms 基线），无模糊扫描。
- **后端整体门户测试**：
  - 命令：`mvn test "-Dtest=*Portal*" -pl lunasrv-module-lunaerp`
  - 结果：116/116 全部通过，0 Failures，0 Errors。
- **前端 375px 移动端冒烟测试**：
  - 测试文件：`src/views/portal/__tests__/portal-capstone-smoke.spec.ts`
  - 命令：`pnpm test run src/views/portal/__tests__/portal-capstone-smoke.spec.ts`
  - 结果：2/2 全部通过。
  - 覆盖内容：
    1. 业务员生成链接 -> 复制链接携带 `?t=token` -> 客户打开页面 -> 地址栏平滑清除 `?t=` 并写入 `sessionStorage` -> 客户浏览发货单列表与详情 -> 登记签收（手填姓名） -> 切换至对账单列表与详情 -> 确认对账 -> 驳回对账 -> 业务员吊销令牌 -> 下次请求拦截并引导跳转至 `/portal/invalid` 友好失效页。
    2. 扫码直达兼容：支持 `/portal/shipment?no=xxx` 与 `/portal/statement?no=xxx`，在无凭证上下文时安全拦截并引导联系业务员。
- **前端门户测试套件全绿**：
  - 命令：`pnpm test run portal`
  - 结果：7 个测试文件、57/57 测试全部通过。

### 2. DoD 达成对照
- **零新表**：门户令牌存 Redis 双键（主键+反向索引），无需迁移表结构；SQL 仅包含 10150 按钮权限幂等脚本。
- **端点收敛**：统一 `/app-api/lunaerp/portal/**`，`PortalTokenFilter` 实现免登校验、上下文注入、滑动续期与 60/min 限流。
- **零新状态**：签收复用发货单既有状态机（PENDING/SIGNED/REFUSED），对账单复用 10 号既有状态机（SENT/CONFIRMED/REJECTED）。
- **契约兼容**：二维码 URL 契约 `{luna.portal.base-url}/portal/shipment?no=xxx` 保持完全兼容。
- **文档与标准**：错误码 038 段及客户门户术语登记于 `docs/contexts/shared/GLOSSARY.md`；ADR-0044 已接收；`_order.md` 与 `spec.md` 全部核销标记完成。
