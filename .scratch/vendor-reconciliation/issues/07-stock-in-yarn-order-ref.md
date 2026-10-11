# 07: 入库单"关联纱线采购单"引用增量（旁支，stock 域）

**What to build:** 在入库工作台录一笔采购入库时，可以像关联销售合同/胚布单/染色单一样，通过远程搜索挂上一张纱线采购单作为第 5 个可选上游引用；系统校验采购单存在后保存其 id 与单号快照，不回写采购单、不锁定其状态。老入库数据该字段为空，正常展示与编辑不受影响。本票是采购货款对账行能带出"纱线采购单号"快照的数据来源。

**Blocked by:** 01（stock_in 加列 SQL）——与 02–06 后端主链并行

**Status:** resolved

- [x] stock-in DO/保存 VO/详情 VO/台账行展示增加 yarnOrderId、yarnOrderNo 两个可空快照字段，MyBatis 映射与库列一致
- [x] 入库工作台基本信息区加"关联纱线采购单"远程搜索选择器（仿现有四类上游引用交互与样式），支持按单号关键词搜索、选中后回填 id+no，可清空
- [x] 保存时服务端存在性校验：id 非空时采购单必须存在，否则复用 stock 域既有"引用不存在"错误码，不新建错误码/字典/权限码/菜单
- [x] 零回写：不更新纱线采购单任何字段、不限制其状态流转；快照零依赖，采购单事后改动不影响入库单展示
- [x] 验证：选中保存后详情/台账显示单号；可改挂可清空；不存在 id 被拒；历史存量数据两列为空时页面与接口均正常

## 实现说明
1. 后端数据结构与校验：
   - `StockInDO`: 增加 `yarnOrderId` (Long) 与 `yarnOrderNo` (String)，标注 `@TableField(updateStrategy = FieldStrategy.ALWAYS)` 保证改挂/清空时更新为 null。
   - `StockInSaveReqVO` / `StockInRespVO`: 增加对应字段与 Swagger 契约说明。
   - `StockInServiceImpl`: 注入 `YarnOrderMapper`，在 `fillSnapshotsAndValidateRefs` 中校验纱线采购单存在性（不存在抛出 `STOCK_IN_REFERENCE_NOT_FOUND` ("纱线采购单")），命中并回填 `yarnOrderNo` 快照；分页 keyword 检索扩充匹配 `yarn_order_no`。零回写保障采购单不被修改或锁态。
2. 前端工作台与台账：
   - `api/lunaerp-elmui/stock/stock-in/index.ts`: `StockInVO` 增加 `yarnOrderId` 与 `yarnOrderNo`。
   - `views/lunaerp-elmui/stock/stock-in/workspace/BasicInfoTab.vue`: 新增"关联纱线采购单"远程搜索选择器，集成 `getYarnOrderPage`，支持输入关键词搜索、选中回填、一键清空及已存数据初始选项反显。
   - `views/lunaerp-elmui/stock/stock-in/workspace/index.vue`: 表单默认状态包含 `yarnOrderId` / `yarnOrderNo`。
   - `views/lunaerp-elmui/stock/stock-in/index.vue`: 列表台账表格增加"关联纱线采购单"展示列，搜索关键词占位说明支持纱线采购单。
3. 单元测试与构建验证：
   - `StockInServiceImplTest`: 增加纱线采购单存在校验回填快照测试、采购单不存在抛出异常测试、编辑时改挂与清空置 null 及零回写校验，全部 63 项测试均通过。
   - `stock-in-yarn-order-ref.spec.ts`: 前端自动化测试覆盖 VO 结构、API 载荷及工作台远程搜索、选中与清空逻辑。
   - `pnpm build:local` 与 `pnpm test` 全部构建与测试通过。
