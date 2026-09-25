# ADR-0024：布票/机台任务/织造单状态机——票态自动推导与人工停机终态

**状态**：Accepted
**日期**：2026-09-25
**关联**：生产管理-织造执行；ADR-0023（三层对象）、ADR-0025（匹装检验回写）

## 背景

参考系统的织造状态机经代码考古已确认（`weaving-status.util.ts`）：

- 扫码只能沿状态机向前走——缺守卫时，已入库票扫"开工"会倒流并把已完工批次降级；
  已完工票重复扫"完工"会刷新 completedAt，使昨日产量重复计入今日看板；
- 机台任务/批次状态由布票状态**自动推导**；
- "暂停异常"是**人工终态**：自动重算只更新 actualQty，不覆盖该态，须走"恢复"解除。

## 决策

### 1. 布票七态（字典 `lunaerp_weaving_ticket_status`）

`GENERATED` 已生成（缺省）/ `STARTED` 已开工 / `COMPLETED` 已完工 / `INSPECTED` 已检验 /
`STOCKED` 已入库 / `EXCEPTION` 异常 / `VOID` 作废。

合法转移（白名单，其余一律拒绝并提示当前态）：

| 动作 | 允许的来源态 |
| --- | --- |
| 开工 | GENERATED、STARTED、EXCEPTION |
| 完工 | STARTED |
| 异常 | GENERATED、STARTED |
| 作废（人工） | GENERATED、STARTED、EXCEPTION |

- **开工允许从 STARTED / EXCEPTION 进入**：异常票接好断纱、换机、换人后重新开工，
  以及转机时重扫以更新 `actualMachine`（该字段真相来自开工），都是设计内恢复路径；挡死会让异常票无路可走。
- `INSPECTED`、`STOCKED` 非死状态：本期联动在范围内——检验明细保存按 `weaving_ticket_id` 回写 INSPECTED（ADR-0025）；胚布入库审核过账回写 STOCKED。
- 作废为人工终态，作废时间与原因必填，作废票不参与任何产量推导。

### 2. 机台任务四态（字典 `lunaerp_weaving_task_status`）

`PENDING` 待开始（缺省）/ `IN_PROGRESS` 生产中 / `HALTED` 暂停异常 / `COMPLETED` 已完工。

- 自动推导：下属有效票（非作废）有任一在产（STARTED/COMPLETED/INSPECTED/STOCKED）→ IN_PROGRESS；
  全部有效票达完工口径（COMPLETED/INSPECTED/STOCKED）→ COMPLETED，`actualQty` = 完工票数，同步更新。
- **HALTED 人工终态**：任务被手动标停机后，自动推导只更新 actualQty、不覆盖 HALTED；须手动"恢复"后才重新参与状态推导。
- **已完工任务不得再标停机**（否则看板永久显示不存在的异常机台）；对已停机任务重复标停机只改写停机原因。
- 停机需填 `haltReason`，记录 `haltAt`。

### 3. 织造单四态（字典 `lunaerp_weaving_order_status`）

`DRAFT` 待排产（缺省）/ `ACTIVE` 生产中 / `COMPLETED` 已完工 / `CANCELLED` 已取消。

- 首个布票开工 → ACTIVE；全部有效票达完工口径 → COMPLETED。
- 不留"部分完工"态（ACTIVE 覆盖，避免列表状态噪音）。
- CANCELLED 手动，为终态；已取消单不可再排产、出票。

## 推导触发时机

状态推导在每次布票状态变更（开工/完工/异常/作废）与停机/恢复操作的**同一事务**内完成，
不使用异步任务——避免票态已变、任务/单态滞后的中间态被列表读到。

## 后果

- 状态机严格，误操作由后端明确拒绝；前端按钮需按当前态置灰，减少无效提交。
- 自动推导意味着任务/织造单状态不可手工指定（停机/取消除外）——现场若需"强制完工"，
  本期不提供后门；确有尾单不再织的场景，以"作废剩余有效票"收敛后自然完工。
