# 03: 后端骨架 - DO/Mapper/Service + 取号 + 聚合保存 + 状态守卫

**What to build:** 在 `lunasrv-module-lunaerp` 的 `business.exportdoc` 子包（与 shipment 同层）落地 ExportDocDO / ExportDocItemDO / ExportSellerConfigDO 三个 DO + 3 个 Mapper + Controller/Service/dal 分层骨架。CRUD 端点走通：`GET /page`（keyword/status/customerId/contractNo 过滤）、`GET /get?id=`（含 items + 三个 meta 原文）、`POST /create`（聚合保存主表+items）、`PUT /update`（仅 DRAFT，聚合整体替换 items、line_no 重排）、`DELETE /delete?id=`（仅 DRAFT，级联软删 items，进回收站）。取号 `ED{yyyy}{4位流水}`（如 ED20260001）按 ADR-0004 数字后缀 max+1 扫描含软删行 + 撞号重试，规避老系统字符串排序 101 套起建号失败的坑。状态机守卫：全部写端点做状态条件守卫，非 DRAFT 的写操作报 EXPORT_DOC_STATUS_NOT_ALLOW。聚合保存：明细整体替换、line_no 重排、amount=quantity×unit_price 服务端重算、跨单明细 ID 拒绝（EXPORT_DOC_ITEM_INVALID_ID）。本票不含卖方配置端点（04）、from-* 带出（05）、resync（06）、finalize/revoke（07）、print（08）。纯 Mockito Service 单测覆盖取号/状态守卫/聚合保存/金额重算/跨单明细拒绝。

**Blocked by:** 01（3 张新表 DDL）

**Status:** ready-for-agent

- [ ] 3 个 DO + 3 个 Mapper + Controller/Service/dal 分层骨架，URL 前缀 `/lunaerp/business/export-doc`，权限码前缀 `lunaerp:business:export-doc:`（query/create/update/delete/print/seller-config 6 个）
- [ ] 取号 `ED{yyyy}{4位流水}`：按年取号、含软删行、数字后缀 max+1 扫描 + 撞号重试、租户内全量唯一；手填撞号报 EXPORT_DOC_NO_DUPLICATE
- [ ] 状态机守卫：非 DRAFT 写操作报 EXPORT_DOC_STATUS_NOT_ALLOW；两态 DRAFT/FINALIZED，无审批流
- [ ] 聚合保存：明细整体替换、line_no 重排、amount=quantity×unit_price 服务端重算（金额逻辑必有测试）、跨单明细 ID 拒绝 EXPORT_DOC_ITEM_INVALID_ID
- [ ] page/get/create/update/delete 端点走通（含 items + 三个 meta 原文）；delete 仅 DRAFT、级联软删 items
- [ ] 单测：取号（含软删行、撞号重试、手填撞号）、状态守卫、聚合保存、金额重算、跨单明细拒绝
