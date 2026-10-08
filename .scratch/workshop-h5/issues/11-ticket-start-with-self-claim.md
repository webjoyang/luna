# 11: 开工（含自助认领）+ 票卡开工按钮

**What to build:** 落成布票开工端点与票卡开工按钮。POST `/lunaerp/h5/workshop/ticket/start`（`{code, actualMachine?}`；**自助认领+开工**：任务 worker 为空→先把 `machine_task.worker` 写为登录人（条件更新 `WHERE worker_user_id IS NULL`，并发落败 039-002）再开工；worker=登录人直接开工；调 `startTicket`，`workerUserId`/`workerName` 强制取登录人，忽略客户端值；`actualMachine` 可空；`@PreAuthorize lunaerp:workshop:h5:report`；`@LogRecord` 留痕）。不开放：void、批量 start、改派他人、停机/恢复（HALTED 由 PC 领班操作）。前端 `src/views/h5/ticket.vue` 票卡页开工按钮（含未派工认领提示，扫票/手输码定位后展示布票卡 + 开工按钮）。可 demo：扫票→开工（含未派工认领）。

**Blocked by:** 10（get-by-code 已就位，票卡页定位）

**Status:** ready-for-agent

- [ ] POST `/lunaerp/h5/workshop/ticket/start`（`{code, actualMachine?}`）
- [ ] 自助认领：任务 worker 为空→条件更新 `WHERE worker_user_id IS NULL`，并发落败 039-002，再调 `startTicket`
- [ ] worker=登录人直接开工
- [ ] `workerUserId`/`workerName` 强制取登录人，忽略客户端值
- [ ] `actualMachine` 可空
- [ ] `@PreAuthorize lunaerp:workshop:h5:report`
- [ ] `@LogRecord` 留痕（开工动作落 system_operate_log）
- [ ] 不开放：void、批量 start、改派他人、停机/恢复（HALTED 由 PC 领班操作）
- [ ] 前端 `src/views/h5/ticket.vue` 票卡页开工按钮（含未派工认领提示）
- [ ] 测试：横向越权工人 A token 操作工人 B 布票 → 039-002；自助认领未派工任务扫票开工→worker=登录人 STARTED（客户端 workerUserId 被忽略）；并发两次不同人认领→一人成功一人 039-002；已派他人→039-002；纵向越权无 report 权限调端点 → 403/鉴权异常；未登录 → 401
- [ ] demo：扫票→开工（含未派工认领）
