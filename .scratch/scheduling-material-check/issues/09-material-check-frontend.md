# 09: 齐套检查前端页

**What to build:** 10520 齐套检查页面（视图 material-check/index.vue）：顶部筛选——织造单多选（el-select multiple 远程搜索，旁置"全选未完工"按钮）、仓库多选（缺省全部）、"含在途"开关（缺省关，旁置 ⓘ 说明在途口径与已知限制）；结果表（el-table 可展开行）：纱线材质/规格、需求量、可用库存、在途量、缺口（不含在途）、缺口（含在途）、建议采购量、状态标签（齐套/在途可覆盖/缺料，前端派生不入字典；建议量为 0 显示"已齐套/在途中"），展开显示各织造单贡献量；顶部汇总徽标（缺料规格数/缺料合计 KG，口径随"含在途"开关切换）；两个无法试算 el-alert 分区（缺配比→跳配比 BOM 管理页按产品过滤；缺计划重量→跳织造单补录）；缺料行"去纱线采购单"按钮（跳纱线采购单列表页带纱线规格关键词）；不做一键建单、不做导出；页面说明 tooltip 写入在途口径限制三条；新增 material-check API 目录。

**Blocked by:** 01: SQL 基建——10510/10520 菜单与角色授权；05: 齐套检查后端——只读试算

**Status:** resolved

**参考**：本目录上级 spec.md §6.2、§4.4

- [x] 勾选织造单试算，需求量/库存/在途/两缺口/建议量与手工台账核对一致；展开贡献行正确
- [x] 缺配比、缺计划重量分区与跳转正确；缺料行跳纱线采购单带规格关键词
- [x] 含在途开关只改汇总徽标口径，两缺口列始终可见；状态标签三态正确
- [x] 无 10520 菜单权限的角色不可见且直连接口 403；ts:check、lint 通过

## Comments
- **齐套检查前端页面与端点交互**（Commit: `ca8c6453`）：
  - 新增 API 客户端目录 `src/api/lunaerp-elmui/production/material-check/index.ts`：
    - 完整声明类型：`MaterialCheckReqVO`、`MaterialCheckRespVO`、`MaterialCheckRowVO`、`MaterialCheckContributionVO`、`MissingBomOrderVO`、`MissingWeightOrderVO`。
    - 声明 `runMaterialCheck(data)` 端点交互函数，对接 `POST /lunaerp/production/material-check/run`。
  - 新增核心辅助函数 `src/views/lunaerp-elmui/production/material-check/helper.ts`：
    - `deriveMaterialStatus`：纯前端派生齐套状态标签（齐套 success / 在途可覆盖 warning / 缺料 danger，不入字典）。
    - `formatSuggestedQty`：建议采购量展示逻辑，0 显示"已齐套/在途中"，正数格式化显示 KG。
    - `calculateSummaryBadges`：纯响应式汇总徽标计算，随"含在途"开关切换缺料规格数与缺料合计 KG 口径。
    - `IN_TRANSIT_LIMITS`：在途口径三条已知限制（采购单状态近似无自动扣减、时点快照不冻结、理论折算不含损耗率）。
    - 路由跳转辅助函数：`getJumpToBomLocation`、`getJumpToWeavingOrderLocation`、`getJumpToYarnOrderLocation`。
  - 新增视图页面 `src/views/lunaerp-elmui/production/material-check/index.vue`（组件名 `LunaerpElmuiMaterialCheck` 对应 10520 菜单）：
    - 顶部筛选：织造单多选（支持 DRAFT/ACTIVE 织造单加载与搜索）+ "全选未完工"一键切换按钮；仓库多选（全仓缺省）；"含在途"开关 + ⓘ tooltip 说明。
    - 顶部汇总徽标：缺料规格数 / 缺料合计 KG，随"含在途"即时切换。
    - 两个无法试算警示分区（el-alert）：缺配比 BOM 单列表（带"去配比 BOM"跳转）与缺重量/匹数单列表（带"去织造单"跳转）。
    - 结果表格（el-table）：展开行展示各织造单贡献明细；展示纱线材质、纱线规格、需求量、可用库存、在途量、两缺口列（常驻展示）、建议采购量、派生状态标签；缺料行带"去纱线采购单"跳转按钮；未实现一键建单与 Excel 导出。
  - 编写前端单元测试 `src/views/lunaerp-elmui/production/material-check/__tests__/material-check.spec.ts`：
    - 覆盖 API Client 调用、三种状态标签派生、建议采购量展示、汇总徽标口径切换、在途限制说明、三类页面跳转路由参数。
    - 15 个 Vitest 用例全部通过；eslint 和 stylelint 静态检查 0 error 0 warning。
