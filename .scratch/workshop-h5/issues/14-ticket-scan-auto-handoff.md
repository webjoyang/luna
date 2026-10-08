# 14: ticket-scan 自动交接编排 + 胚布扫码定位

**What to build:** 落成胚布扫码定位编排端点。GET `/lunaerp/h5/workshop/inspection/ticket-scan`（code 布票码；`@PreAuthorize lunaerp:workshop:h5:inspection`）。规则：① 按 code 查布票（不存在/VOID → 039-001）；状态必须 COMPLETED/INSPECTED/STOCKED 之一，未完工 → 039-003，其余不可录态 → 039-004；② 内部直接调 `weavingOrderService.handoffInspection(ticket.weavingOrderId)`（幂等：已交接复用，按胚布单归一 find-or-create 仅建检验单表头，ADR-0025）——H5 Service 调用绕过 Web 权限点，不要求检验员具备 `:handoff-inspection`；③ 按 `weavingTicketId` 查录检行：存在 → 返回 `{inspectionOrderId, item}` 供改录；不存在 → 返回 `{inspectionOrderId, item:null, suggestedPieceNo: 布票code}`；④ 该检验单已 COMPLETED/CANCELLED → 010-002。可 demo：扫完工票→自动建交接单→定位/新建录检行（行占位）。

**Blocked by:** 13（检验单列表/详情已就位）, 10（get-by-code 已就位，扫码定位复用）, 07（扫码组件已就位用于扫布票码）

**Status:** ready-for-agent

- [ ] GET `/lunaerp/h5/workshop/inspection/ticket-scan`（code 布票码）
- [ ] 规则 ① 按 code 查布票，不存在/VOID → 039-001；状态必须 COMPLETED/INSPECTED/STOCKED 之一，未完工 → 039-003，其余不可录态 → 039-004
- [ ] 规则 ② 内部直接调 `weavingOrderService.handoffInspection(ticket.weavingOrderId)` 幂等 find-or-create 检验单表头，绕过 Web 权限点不要求检验员有 `:handoff-inspection`
- [ ] 规则 ③ 按 `weavingTicketId` 查录检行：存在 → 返回 `{inspectionOrderId, item}` 供改录；不存在 → 返回 `{inspectionOrderId, item:null, suggestedPieceNo: 布票code}`
- [ ] 规则 ④ 该检验单已 COMPLETED/CANCELLED → 010-002
- [ ] `@PreAuthorize lunaerp:workshop:h5:inspection`
- [ ] 测试：胚布扫码自动交接未交接织造单首扫→检验单表头创建+录检行关联 ticket；连扫两次同一票（第二票/重放同票）→同一检验单、不重复建行；无完工票 → 039-003；横向越权（质检不受机台归属限制但跨租户隔离）
- [ ] demo：扫完工票→自动建交接单→定位/新建录检行（行占位）
