# 05: 布票生成与状态操作（每匹一码 + 扫码守卫 + 三层状态同事务推导 + 停机/恢复）

**What to build:** 新建 `production.weavingorder.ticket` 子包与 `lunaerp_production_weaving_ticket` 表。端点：按机台任务批量生成布票、布票分页（按织造单/任务/状态）、开工/完工/异常/作废操作。布票字段：`code`、`weavingOrderId`、`machineTaskId`、`plannedMachine`、`actualMachine`、`plannedWeight`（单匹计划重量，生成时按 总重÷匹数 带入）、`workerUserId`+`workerName`、`materialBatchNo`、`status`（七态，缺省 GENERATED）、`exceptionType`+`exceptionNote`、`startedAt`/`completedAt`、`voidReason`/`voidedAt`。编号：`BT-{机台任务号去符号}-{4 位流水}`，**流水按织造单维度计数**、作废占号不重用；生成在事务内锁任务行+余量校验（超 plannedQty 拒绝），撞键重试。操作白名单（ADR-0024）：开工允许 GENERATED/STARTED/EXCEPTION（写 actualMachine/工人/startedAt）、完工允许 STARTED（写 completedAt）、异常允许 GENERATED/STARTED、作废为终态；违规报 111/112，异常类型非法 113。**每次布票变更同事务推导**：任务 actualQty/状态（含全部完工→COMPLETED）与织造单状态（首开工→ACTIVE、全完工→COMPLETED）。另做任务停机（haltReason 必填、已完工禁停 101）/恢复：HALTED 期间推导只更新 actualQty 不覆盖状态。DDL 追加至 `lunaerp-production-weaving.sql`。

**Blocked by:** 04

**Status:** done

- [x] 布票表 + 按任务批量生成（事务行锁、余量、单匹重量）
- [x] BT 派生编号（按织造单计流水、4 位）
- [x] 开工/完工/异常/作废白名单守卫（111/112/113）
- [x] 任务/织造单状态同事务自动推导；停机/恢复人工终态
- [x] DDL 追加；字典 id=2240（票态）、2244（异常类型）
- [x] 单测：超量生成、非法流转、推导（含 HALTED 不覆盖）、并发
