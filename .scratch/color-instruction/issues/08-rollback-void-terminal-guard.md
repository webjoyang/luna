# 08: 回退、作废与终态守卫

**What to build:** CONFIRMED 态打色单无任何下游胚布单/染色单引用时可"回退"至 IN_PROGRESS（二次确认，写操作日志，行状态不回退——APPROVED 行保持，业务员可逐行"驳回重打"）。PENDING/IN_PROGRESS/CONFIRMED 可"作废"（填作废原因），存在有效（未作废、未删除）下游引用时报 1_030_032_005，IN_PRODUCTION 禁作废。IN_PRODUCTION/VOID 为终态，拒绝一切写操作（编辑/作废/删除）。软删仅 PENDING 可走（CONFIRMED 取消走作废）。

**Blocked by:** 06 下游引用列与保存校验

**Status:** closed

- [x] "回退"接口 PUT `/rollback`（`:confirm` 权限，body: id）：CONFIRMED→IN_PROGRESS，守卫——仅当无任何下游胚布单/染色单有效引用（status ≠ CANCELLED 且未删除），否则报 1_030_032_005；二次确认由前端弹窗实现；写操作日志 `@LogRecord`；行状态不回退（APPROVED 行保持）
- [x] "作废"接口 PUT `/void`（`:confirm` 权限，body: id, voidReason）：PENDING/IN_PROGRESS/CONFIRMED 可作废；守卫——存在有效下游引用报 1_030_032_005；IN_PRODUCTION 禁作废（报 1_030_032_002）；写入 voidReason、status=VOID；条件更新防并发
- [x] 终态守卫：IN_PRODUCTION/VOID 拒绝一切写操作（编辑 update/作废 void/删除 delete/行级动作 approve/reject-restrike），统一报 1_030_032_002；软删仅 PENDING 可走（02 票已实现 PENDING 软删，本票增强 CONFIRMED/IN_PRODUCTION/VOID 软删拦截报 1_030_032_002）
- [x] 下游引用查询服务：`ColorInstructionService.hasActiveDownstreamRef(id)` 供回退/作废守卫复用（查胚布单 + 染色单 status ≠ CANCELLED 且 deleted=0 且 color_instruction_id = id 的行数 > 0）
- [x] `@LogRecord`（回退/作废）入 `system_operate_log`
- [x] 前端编辑页页头"回退"按钮（CONFIRMED 态显示，二次确认弹窗）、"作废"按钮（PENDING/IN_PROGRESS/CONFIRMED 显示，弹窗填 voidReason）；终态单据全只读、无操作按钮
- [x] 测试覆盖：回退无下游成功 CONFIRMED→IN_PROGRESS、回退有下游报 1_030_032_005、作废各状态（PENDING/IN_PROGRESS/CONFIRMED 成功、IN_PRODUCTION 报错）、作废下游守卫（有有效引用报 1_030_032_005，下游 DRAFT 染色单同样计入）、终态守卫（IN_PRODUCTION/VOID 拒绝 update/void/delete/行级动作）、条件更新防并发
- [x] IN_PRODUCTION 终态运行时回归依赖 07 票落地（投产自动触发）后补验
