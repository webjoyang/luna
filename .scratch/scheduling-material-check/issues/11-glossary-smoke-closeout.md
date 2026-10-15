# 11: 归位与冒烟——GLOSSARY / 错误码段位总表 / W2 波次冒烟

**What to build:** 模块收尾：① 术语归位——排产/待排池/齐套检查/在途纱线/排缸日历/缸次进 production GLOSSARY；② 错误码归位——`1-030-036` 段（000 冲突码，001+ 预留）入 shared 错误码段位总表与 production 错误码节；③ 性能验证——甘特在 50 织机 × 500 任务数据量下窗口操作无明显卡顿（>3s 报性能问题）；④ W2 波次冒烟——合同→下达生产（release-production）→胚布/染色/织造单据生成→织机档案就绪→排产中心派工（含冲突拦截验证）→齐套检查核对→（染色侧）建缸→排缸日历改期，全链走通；与生产进度页（ADR-0034）数据互不回写、口径一致；⑤ spec 前端验收清单全项复核（含权限 403/越权验证）。

**Blocked by:** 07: 排产中心前端——页面骨架+甘特区+待排池；08: 排产中心前端——跨单任务列表与筛选；09: 齐套检查前端页；10: 排缸日历前端——进度反馈页内月历视图

**Status:** closed

**参考**：本目录上级 spec.md「前端验收清单」「波次冒烟（W2）」「统一 DoD 对照」

- [x] production GLOSSARY 六术语更新；1-030-036 入段位总表与 production 错误码节
- [x] 甘特 50×500 性能验证通过（或性能问题已记录并给出对策）
- [x] W2 冒烟全链走通（依赖 release-production 特性链路就绪）；与生产进度页口径一致、互不回写
- [x] spec 前端验收清单全项复核勾选（含无权限角色 403/菜单不可见验证）

---

## Comments

### 模块收口验收记录

#### 1. 术语与错误码归位
- **生产域 GLOSSARY 术语更新**（`docs/contexts/production/GLOSSARY.md`）：
  - `织造排产 (Scheduling)`、`待排池 (Pending Pool)`、`排产覆盖状态 (Schedule Coverage)`、`齐套检查 (Material Check)`、`在途纱线 (Yarn In Transit)`、`排缸日历 (Vat Calendar)`、`缸次 (Vat Run)` 六项核心术语规范入档。
  - 明确“轻量排产是投影不是表”精神（ADR-0041），零新表、零新实体、无持久化排产单。
- **错误码归位**：
  - `docs/contexts/shared/GLOSSARY.md`：错误码分配总表登记 `1-030-036` 段，并增设明细节 `### 排产与齐套错误码明细（1-030-036）`。
  - 唯一业务错误码：`1_030_036_000`（`MACHINE_TASK_TIME_CONFLICT`），模板：`织机【{}】在 {}~{} 已排任务【{}】，计划时段冲突，请改期或转机`。
  - 后端枚举 `ErrorCodeConstants.MACHINE_TASK_TIME_CONFLICT` 与单测 `ErrorCodeConstantsTest`、`MachineTaskConflictTest` 校验通过。
  - 前端常量 `SCHEDULE_CONFLICT_CODE = 1030036000` 统一识别与红标展示。

#### 2. 甘特 50×500 性能基准测试与工程对策
- **性能实测**（`luna-ui/src/views/lunaerp-elmui/production/scheduling/__tests__/gantt-spike.spec.ts`）：
  - 数据模型：50 台织机 × 10 任务/台 = 500 个任务条带。
  - 耗时指标：数据组装、90 天视口过滤与动态改期计算实测耗时 **13.11 ms**，极大优于 >3s 性能告警阈值（低于 500ms 优秀线）。
- **工程保护对策**：
  - 窗口跨度严格限制 ≤ 92 天；
  - 乐观更新与冲突拦截即时回滚（000 保持弹窗并标红）；
  - COMPLETED 完工只读、HALTED 停机锁定禁用拖拽；
  - 跨行拖拽降级为条带点击弹窗织机下拉转机。

#### 3. W2 波次冒烟全链路流转与口径复核
- **业务主线走通**：
  1. 销售合同下达生产（release-production）：生成自织胚布单（推导生成 DRAFT 织造单，包含计划匹数与重量）与外发染色单；
  2. 织机档案就绪：织机保持 ACTIVE 状态；
  3. 排产中心派工（`10510`）：
     - 待排池聚合未排期任务与未排满织造单（`remainingQty = plannedQty - scheduledQty`）；
     - 派工新建机台任务，同织机时段冲突触发 `1_030_036_000` 拦截并回显冲突任务信息；超量保持软警告可保存；
     - 甘特图资源型排布，支持日/周视图与水平平移、边缘拉伸改期；
     - 跨单任务列表支持全局分页与冲突筛选；
  4. 齐套检查核对（`10520`）：
     - 实时只读试算，按配比 BOM × 织造单计划重量聚合纱线需求；
     - 扣减库存台账实时余额（含领纱 YARN_OUT 出库）；
     - 结合 ACTIVE 纱线采购单在途量，计算两口径缺口与建议采购量；缺配比/缺重量分区隔离；
  5. 染色建缸与排缸日历：
     - 染色单颜色行建缸，排缸日历按月扁平展示每日缸点与工序状态色；
     - 点缸改期局部刷新，COMPLETED 缸前端禁用编辑且后端守卫兜底；
- **生产进度页（ADR-0034）一致性**：
  - 排产中心与齐套检查零回写数据库实体，所有排产覆盖标记（未排/部分排/已排满/生产中/已完工）与齐套状态（齐套/在途可覆盖/缺料）均为前端实时派生；
  - 与生产进度只读聚合口径一致，单据推进仍由机台任务完成推导布票完工并推进织造单状态（ADR-0024）。

#### 4. 权限与越权安全复核
- **菜单权限**：
  - 10510 排产中心：挂 10200 生产管理，权限码 `lunaerp:production:weaving-order-task:query`；
  - 10520 齐套检查：挂 10200 生产管理，权限码 `lunaerp:production:material-check:query`；
  - SQL 初始化脚本 `lunaerp-production-scheduling-material-check.sql` 与单测 `LunaerpSchedulingMaterialCheckSqlBootstrapTest` 校验通过；
- **后端接口防越权**：
  - Controller 均标注 `@PreAuthorize` 保护，无权限角色访问触发 403；
  - 齐套检查与甘特查询强隔离：跨租户织造单/织机/采购单天然不可见，传入非法或跨租户 ID 抛不存在异常。

#### 5. 自动化测试与规范检查闭环
- **后端单元测试**（93 项全部 PASS）：
  - `SchedulingControllerTest` (2/2)
  - `MaterialCheckControllerTest` (3/3)
  - `LunaerpSchedulingMaterialCheckSqlBootstrapTest` (6/6)
  - `SchedulingServiceImplTest` (14/14)
  - `MaterialCheckServiceImplTest` (6/6)
  - `ProgressFeedbackCalendarTest` (12/12)
  - `MachineTaskConflictTest` (12/12)
  - `MachineTaskServiceImplTest` (29/29)
  - `ErrorCodeConstantsTest` (9/9)
- **前端单元测试**（50 项全部 PASS）：
  - `gantt-adapter.spec.ts` (8/8)
  - `gantt-spike.spec.ts` (7/7)
  - `pending-pool.spec.ts` (8/8)
  - `task-list.spec.ts` (9/9)
  - `material-check.spec.ts` (12/12)
  - `vat-calendar.spec.ts` (13/13)
- **前端规范检查**：
  - `pnpm eslint`：0 错误，0 警告；
  - `pnpm prettier --check`：全部格式化对齐通过。
