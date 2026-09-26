# 04: 机台任务排产（创建任务 + 织机/挡车工 + 排期 + 计划匹数软校验）

**What to build:** 新建 `production.weavingorder.task` 子包与 `lunaerp_production_machine_task` 表，端点：任务分页（按织造单）、创建、更新（排期/挡车工/匹数）、停机/恢复（状态操作细节在 issue 05 随状态推导一起打通，本 issue 先落 CRUD 与字段）。字段：`code`、`weavingOrderId`（必填）、`loomId`+`loomNo`（必填+快照，仅 ACTIVE 织机可选）、`workerUserId`+`workerName`（挡车工，关联 system User 并按挡车工岗位过滤，可空，ADR-0021）、`plannedQty`（正整数）、`actualQty`（缺省 0，布票聚合只读）、`plannedStart`/`plannedEnd`（普通日期可空，end≥start）、`status`（PENDING/IN_PROGRESS/HALTED/COMPLETED，缺省 PENDING）、`haltReason`、`haltAt`、`remark`。编号派生：`MT-{织造单号去符号}-{单内 2 位序号}`，并发重试。单内任务 plannedQty 合计超织造单时**警告不拦截**。表 DDL 追加至 `lunaerp-production-weaving.sql`。

**Blocked by:** 02, 03

**Status:** closed

- [x] 机台任务表 + CRUD 端点；织机（仅 ACTIVE）/挡车工选择
- [x] 派生编号 MT-{织造单}-{2位}
- [x] 排期日期校验；单内超量软警告
- [x] DDL 追加；字典 id=2239
- [x] 单测：编号、织机/织造单存在性、排期校验
