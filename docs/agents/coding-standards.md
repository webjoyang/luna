# Coding standards for agents

后端（`luna-srv`）与前端（`luna-ui`）开发规范与 Reviewer 审查基准。

## 1. 集合查询与条件构造 (Collection Query & Wrapper Safety)

- **集合空安全陷阱**：`LambdaQueryWrapperX.inIfPresent(col, values)` 在 `values` 为 `null` 或**空集合**时均会跳过条件。若将派生 ID 集合直接喂入 `inIfPresent`，会导致 `IN` 条件丢失，退化为全表/全租户无限制扫描。
- **集合空安全强制规约**：
  - 依赖派生集合过滤的业务 Service，必须前置短路守卫：`if (CollUtil.isEmpty(ids)) { return Collections.emptyList(); }`，避免无谓的数据库往返。
  - 框架已在 `LambdaQueryWrapperX` 提供 `inOrEmpty(col, values)`：当 `values` 为空集或 `null` 时自动追加 `1 = 0` 永假条件；非空时拼接 `IN`。在必须强制以集合过滤的 SQL 条件中优先使用 `inOrEmpty`。
- **MyBatis-Plus Wrapper 链式调用类型截断陷阱**：
  - MyBatis-Plus 原生 `AbstractLambdaWrapper` 的泛型返回参数固定为 `LambdaQueryWrapper<T>`，一旦先调用原生 `.ge()` / `.le()` / `.eq()` / `.orderBy*()`，方法链返回类型退化为原生 Wrapper，导致后续链式调用 `eqIfPresent` / `inOrEmpty` 报编译错误（找不到符号）。
  - **强制规约**：`LambdaQueryWrapperX` 自定义方法（如 `*IfPresent`、`inOrEmpty`）必须链式书写在原生方法（`ge`、`le`、`orderBy*`）**之前**，或直接使用扩展提供的 `geIfPresent`、`leIfPresent`、`betweenIfPresent`。当需要将 Wrapper 实例赋予局部变量以便跨分支追加条件时，局部变量类型应声明为基类 `LambdaQueryWrapper<T>`（或使用 `var`），避免多层泛型擦除导致的类型不匹配与编译失败。
- **可空日期区间相交陷阱 (Nullable Date Range Overlap Pitfall)**：
  - 在 SQL 中，闭区间重叠判定条件 `planned_end >= startDate AND planned_start <= endDate` 当记录的起止列允许为 `NULL`（如待排池中尚未排期的任务）时，三值逻辑比较会导致结果为 `UNKNOWN`（被 `WHERE` 视为 `false`），未排期或单侧日期的记录会被静默过滤剔除。
  - **强制规约**：当查询语义要求包含未定日期的记录时，必须显式显化可空分支：`(planned_end IS NULL OR planned_end >= :startDate) AND (planned_start IS NULL OR planned_start <= :endDate)`，或通过 `COALESCE` 显式设置极端边界时间兜底，严禁直接对可空列进行裸比较。

## 2. 防 N+1 批量快照模式 (Batch Snapshot Pattern)

- **强制规约**：
  - 禁止在列表遍历或分页拼装循环内单条查表（严禁 N+1）。
  - 关联单据/实体快照一律采用批量抽取与 Map 索引：提取私有方法 `batchLoadXxx(Collection<Long> ids)`，返回 `Map<Long, DO>`，主循环中通过 `map.get(id)` 内存读取。
  - `batchLoadXxx` 内部必须遵循集合空安全，空集合直接返回 `Collections.emptyMap()`。

## 3. 只读聚合子包规约 (Read-only Projections)

- 遵循 ADR-0041 与 ADR-0034（“投影不是表”）。
- 排产中心（`production.scheduling`）、齐套核算（`production.materialcheck`）、进度反馈（`production.progress`）等只读聚合视图：
  - 零新增物理表、零新增实体 DO。
  - 严禁新增写接口；跨单分页在原有 Mapper 扩展条件并在现有 Service 实现上平滑委托。
  - 直接复用既有模块的基础 Mapper / Service。
- **仓储层跨域与分层倒置防范 (DAL Layer Hygiene)**：
  - DAL Mapper 严禁依赖 Controller 层的 VO 包（严禁分层倒置）。
  - 跨域只读聚合查询优先复用所属域的既有领域模型（如库存域的 `InventoryBalanceRespVO`、采购域的 `YarnOrderItemDO`），严禁在基础仓储层引入调用方只读子包的专属 VO/DTO。
- **回写场景的投影 ID 保全规约 (Round-trip Projection Parity)**：
  - 若前端交互支持基于只读投影直接触发关联实体的更新/转机/改派（调用既有实体 Update API），只读 VO **必须**投影并透传被引用实体的关联外键 ID（如 `workerUserId`、`customerId`），严禁仅投影纯文本展示字段，防止反向回写时底层实体外键被静默置空（数据丢失）。
- **跨域领域事件与快照 DTO 命名归属规约 (Domain Event Decoupling Pattern)**：
  - 当上游业务动作（如发货审核/红冲、采购入库）需要通过 Spring 事件机制驱动下游领域（如财务记账、库存过账）同事务或异步联动时：
  - **强制规约**：事件类与明细行快照 DTO 必须统一定义在**事件发布方**的域包内（如 `service.business.shipment.event.ShipmentAuditedEvent`）；事件载荷必须使用发布方自有快照 DTO（或基础标量），严禁发布方反向 import 订阅方（finance/stock）的任何类；下游订阅方单向依赖并监听上游事件，实现领域单向解耦。

## 4. Mapper 纯粹性与 Mockito 集合匹配 (Mapper Mocking)

- **场景**：Mapper 接口方法定义及纯 Mockito 单元测试。
- **强制规约**：
  - **接口纯粹性**：Mapper 专注定义 SQL 映射方法，空安全由 Service 前置守卫负责，避免在 Mapper 中编写冗余 `default` 包装方法（容易导致 Mockito 穿透执行真实方法体及产生推测性死代码）。
  - **空参数匹配**：在纯 Mockito 单测中，当打桩方法的重载参数为 `Collection` 且可能为 `null` 时，必须使用 `nullable(Collection.class)` 或明确的具体对象，严禁使用 `anyCollection()`（`null` 会导致匹配失败）或无限定的裸 `any()`（产生重载歧义）。
  - **集合类型相等性**：Java 中 `Set.equals(List)` 恒为 `false`。若 Service 实现层传入 `Set`（如 `Map.keySet()`），单测打桩严禁使用 `eq(List.of(...))`，优先使用 `argThat(c -> c.contains(...))`、`nullable(Collection.class)` 或传入匹配的具体集合类型。
  - **`BaseMapperX` 重载消歧与打桩范式**：
    `BaseMapperX` 包含 `selectOne(Wrapper)`、`selectOne(String, Object)` 与 `selectOne(SFunction, Object)` 等多个同名重载。单测打桩严禁使用裸 `when(mapper.selectOne(any(), any()))`，会导致 Java 编译器报 `ambiguous call` 编译失败；亦严禁裸 `selectOne(any())`，易因 Mockito strict stubbing 判定签名不匹配报 `PotentialStubbingProblem`。
    **强制规约**：打桩多参数重载必须显式指定类型：`when(mapper.selectOne(any(SFunction.class), any())).thenReturn(...)`；或在实现与测试中统一优先使用 `LambdaQueryWrapperX`：`when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(...)`。

## 5. 工作区快速代码与文件检索 (Fast Code & File Search)

- 混合仓库根目录下包含 `luna-ui/node_modules/`、`target/`、`.claude/` 等深度依赖和构建产物目录；**仓库根目录下无 `src/` 源码目录**。
- **检索规约**：
  - 禁止在根目录使用无深度限制且未排除依赖目录的 `Get-ChildItem -Recurse`。
  - 文件路径定标优先使用 `git ls-files "*<keyword>*"`。
  - 文件内容/代码符号检索一律使用跨平台原生的 `git grep "<pattern>"`（或 `git grep -i "<pattern>"`），严禁在 PowerShell 下使用 `xargs` / Linux `grep` 管道；必须显式指定子工程目录路径（如 `git grep "<pattern>" luna-ui/ luna-srv/`），严禁使用不存在的根路径 `src/`；特性开发中检索刚创建未暂存的文件必须携带 `--untracked`（如 `git grep --untracked "<pattern>"`）。

## 6. 前端组件与交互规范 (Frontend Standards)

- **按钮级权限指令守卫 (`v-hasPermi`)**：
  - 所有触发状态流转或数据写操作的按钮（新增、编辑/修改、删除、转机、调度/派工、审批等），必须显式绑定 `v-hasPermi="['<permission-code>']"`，禁止裸露可操作按钮。
  - **二级弹窗/抽屉确认按钮守卫**：子组件弹窗（`el-dialog`）或抽屉（`el-drawer`）内部的确认提交按钮（如 `handleConfirm*`、`handleSave*`、`handleSubmit*`），严禁因外层入口已有控制而遗漏，必须严格且同等绑定对应写权限码；开发过程中应针对当前模块目录运行 `pnpm lint:perms <path>` 进行机械化静态扫描校验。
- **月历与时序看板“今天回位”联动规约 (Calendar Navigation & Today Reset Parity)**：
  - 在以月度或视口周期为聚合粒度监听（如 `watch(() => formatYearMonth(date))`）的日历/甘特组件中，当提供“今天/当前周期回位”快捷按钮时，若当前视图已经处于目标周期，单纯重置 `Date` 对象并不会改变格式化后的周期字符串，导致 `watch` 监听静默失效。
  - **强制规约**：在“今天回位”事件回调中必须显式检测是否已在当前周期；若处于当前周期，必须显式调用数据加载函数（如 `loadCalendarData()`），保证操作反馈的一致性与即时性。
- **业务错误码常量化、异常拦截与文档即时回填 (Error Code Constants & Glossary Sync)**：
  - 接口业务异常与拦截（如排产时段冲突 `1030036000`）必须优先以常量形式定义并复用（如 `SCHEDULE_CONFLICT_CODE`），通过 `err.code === CONSTANT` 进行精确匹配；严禁仅依赖 `errorMsg.includes('...')` 模糊中文文本匹配作为核心业务逻辑分支。
  - **错误码文档即时回填规约**：新业务错误码段位落库时，在首个引入错误码常量的工单（如后端冲突拦截开发），必须同步在 `docs/contexts/shared/GLOSSARY.md` 段位总表及明细表格中登记（含错误码、常量名、模板文案与所属域），严禁拖延至最终收口工单才集中补录，避免中间工单代码审查信息断层。
- **只读投影回写参数对齐 (Round-trip Form Payload)**：
  - 在前端弹窗、抽屉或拖拽改期中回传实体保存接口时，必须完整回显/携带原有关键外键与快照（如操作员 ID、织机 ID、计划版本等），避免只提交修改项导致未改项被覆写清空。
- **看板图形视口与明细列表数据口径解耦 (View Disconnection)**：
  - 在包含甘特图/时间轴图形与下挂明细表格/分页列表的复合页面中，图形视口通常受用户缩放（日/周/月）或水平滚动窗口裁剪；而明细列表承担全局检索与分页排查职责。
  - **强制规约**：明细列表的筛选与查询请求必须且仅严格绑定顶部显式过滤条件（或独立分页器），严禁将明细列表静默绑定到图形视口的临时可见时间窗口，避免造成用户在列表中“搜不到视口外已有任务”的交互困惑。
- **业务操作提交助手单一来源 (DRY Submit Helpers)**：
  - 同一领域实体的更新逻辑可能散布在多个交互场景（如甘特图拖拽调整、行内操作菜单、独立编辑弹窗）。
  - **强制规约**：对同一后端修改 API 的参数对齐、冲突错误码拦截（如 `SCHEDULE_CONFLICT_CODE`）、消息提示与局部/全局数据刷新，应提取统一的 Composable 或 Model 辅助函数封装，严禁在多处 Vue 组件模板中复制散落的 `try-catch` 和错误分支拼装代码。
- **远程搜索多选下拉选项防脱落守卫 (Remote Select Option Eviction Guard)**：
  - 在 Element Plus 远程多选下拉（`el-select multiple filterable remote`）中，`remote-method` 返回的新分页结果若直接覆写 `options`，会导致当前已被勾选但不在新结果页内的项丢失 label，标签退化显示为纯数字 ID。
  - **强制规约**：远程搜索回调更新 options 时，必须提取 Map 合并保留当前已选中的实体集合（`queryParams.xxxIds`），保持已选标签名称稳定呈现。
- **跨页跳转过滤参数的主键 ID 优先透传规约 (Cross-page ID Parity)**：
  - 跨模块跳转携带过滤条件时（如齐套检查跳转配比 BOM 或采购单列表），若目标查询接口支持实体领域主键（如 `productId`、`customerId`），路由参数**必须**优先透传主键 ID，并在目标列表页的 `queryParams` 中优先绑定，同时可附带可读编码/名称快照，严禁单向降级为纯文本而丢弃主键 ID。
- **看板汇总口径派生与交互解耦 (Metric Switch Decoupling)**：
  - 看板中的纯口径切换（如“含在途/不含在途”切换）：若仅影响顶部统计徽标或卡片，结果表格两口径列常驻展示，必须通过客户端响应式（`computed` / Model 纯函数）即时派生，严禁在无必要时触发全局表格重新请求与阻塞加载动画。
- **工作台终态流转前置保存规约 (Pre-flight Save on State Transition)**：
  - 在支持就地编辑明细的单据工作台（如改单价、改数量、增删行）中，执行“确认/提交/审核”等终态冻结操作时，必须在用户弹窗确认后、调用冻结接口前，**自动前置执行静默保存草稿**（`await handleSave(true)`）；保存成功并取得最新单据状态后方可触发终态流转，严禁直接提交内存旧版本引发数据丢失。
- **生产组件接口异常降级真实性规约 (Anti-Mock Fallback in Production)**：
  - 在前端组件处理远程数据（如账户列表、仓库选项、关联单据）加载异常的 `catch` 块中，严禁通过硬编码虚拟主键 ID（如 `id: 1`）、假实体或超大虚拟余额（如 `9999999`）进行“防御式 Mock”强行兜底。
  - **强制规约**：接口加载失败或返回空数据时，必须保持真实空状态，通过明确的空状态警示（如“暂无可用的有效资金账户，请先在账户管理中维护并启用”）或表单原生必填校验阻断后续提交，严禁伪造虚假数据绕过后端的真实可用性与权限校验。
- **分页数据服务端闭环与客户端展示规约 (Server-side Pagination Parity)**：
  - 凡调用服务端分页接口（`*Page`），前端表格数据必须且仅严格全量渲染服务端当前页返回的数据数组，**严禁在前端对 `res.list` 进行无脑 `filter` 裁剪**（极易引发单页记录不足甚至当前页全空、但分页器仍显示“共 N 条”的假分页坏味道与体验断裂）。
  - **强制规约**：对当前页中不满足业务操作前提的行（如“已核销完/已结清”），一律通过**行内状态 Tag（如【已结清】）+ 操作按钮禁用 / Checkbox 禁用（`:selectable="canSelectRow"`）** 在原位显化，坚决捍卫分页条总数、页码与表格物理行的严格一致性。
- **状态机驱动交互动作单一来源指示器规约 (Action Flags Single Source of Truth)**：
  - 由多状态流转或审批流驱动的单据视图（如付款申请、对账单工作台），其行内操作列（编辑、提交、撤回、删除、重提）可能随业务状态频繁演进。
  - **强制规约**：操作栏按钮的显隐与禁用判定，**必须统一消费领域模型（`model/*.ts`）输出的 Action Flags（如 `getPaymentApplyActionFlags(status)`）**，严禁在 Vue SFC 模板中直接手写多层状态枚举（`v-if="status === 'A' || status === 'B'"`）的 switch 级联，避免新增或调整状态分支时漏改散落的模板分支。
- **复合主子单流水的展开行懒加载规约 (Master-Detail Expansion Lazy Load Parity)**：
  - 在分页列表需支持展示子明细流水（如付款单展开展示核销单号及各单分配金额）且后端分页列表接口未预热子集合时：
  - **强制规约**：前端主表格的子单展开（`<el-table-column type="expand">`）统一采用**行展开事件驱动懒加载（`@expand-change` + 响应式 Map 缓存 + `getDetail(row.id)`）**，并在主单对应列显化“展开查看明细”引导，兼顾后端防 N+1 批量快照性能与前端多单展开交互。

## 7. 前端单测与 Vitest 纯 Node 环境守卫 (Frontend Vitest Hygiene)

- **Node 环境与无 DOM/SFC 边界**：
  - `luna-ui/vitest.config.ts` 配置为纯 `node` 环境，未挂载 `@vitejs/plugin-vue` 与 JSDOM。
  - **强制规约**：前端单测定位为领域纯函数与数据适配器测试（`model/*.ts`、`utils/*.ts`），**严禁在单测中直接 import `.vue` 组件**。
- **Axios 顶层打桩规约**：
  - `@/config/axios` 会级联导入系统路由并在根作用域执行 `createWebHistory`（依赖 `window`）。
  - **强制规约**：单测中若间接依赖 API 调用，必须在测试文件顶层显式声明 `vi.mock('@/config/axios', ...)`，避免在纯 Node 运行环境中报 `ReferenceError: window is not defined`。
- **全局打印注册表异步按需加载规约 (Async Print Registry Pattern)**：
  - 打印模板注册表（`luna-ui/src/components/Print/registry.ts`）新增业务模板时，必须统一使用 `defineAsyncComponent(() => import(...))` 与动态 `fetchApi: (id) => import('@/api/...').then(...)` 进行解耦注册。
  - **强制规约**：严禁在注册表顶层静态 import Vue SFC 组件与带路由级联副作用的 API 文件，防止破坏 Vitest 纯 Node 测试环境或在非打印单测中级联引发 `ReferenceError: window is not defined`。
- **前端领域模型纯净度与防逆向依赖规约 (Domain Model Pure Functions & Inverted Dependency Guard)**：
  - **场景**：`src/views/**/model/*.ts` 定义纯 TS 领域模型、金额计算函数、日期区间处理与表单校验器。
  - **逆向依赖陷阱**：领域模型文件若为了复用类型或错误码常量而反向 `import` 视图层接口文件（`@/api/**`），会导致级联引入 `@/config/axios` 及其绑定的 Vue Router 实例。在纯 Node 单测环境执行时，任何未显式声明 Axios Mock 的单测都会因此触发 `createWebHistory` 抛出 `ReferenceError: window is not defined`。
  - **强制规约**：
    1. 领域模型文件必须保持为纯 TypeScript 逻辑与类型，**严禁反向 import 视图层 API 文件（`@/api/**`）或 Vue SFC 组件**。
    2. 领域模型所需的错误码常量、枚举与纯计算逻辑定义于 `model/` 内部供视图消费；严禁让 API 层反向引入 View/Model 层模块（受 `eslint.config.mjs` 强制拦截）；Model 层亦严禁直接引入 `@/api/**`（防 Axios/Router 污染 Node 单测）。若需多层共享，应定义于纯 `types/` 声明文件中。
- **前端表单日期默认值初始化时区偏差防御 (Date Initialization Timezone Parity)**：
  - **场景**：表单生效日、操作日、筛选开始结束日等默认值初始化。
  - **UTC 跨日截断陷阱**：使用 `new Date().toISOString().slice(0, 10)` 会取标准 UTC 时间。在东八区（UTC+8）早晨（00:00–07:59）调用时，会错误回退至昨天的日期字符串，造成默认日期失真。
  - **强制规约**：前端组件中初始化当前日期统一采用本地时区格式化：使用 `new Date().toLocaleDateString('en-CA')`（原生输出标准 `YYYY-MM-DD`）或 `dayjs().format('YYYY-MM-DD')`，严禁使用 `toISOString().slice(0, 10)`。

## 8. PostgreSQL 脚本与多端幂等规约 (PostgreSQL DDL/DML & Sequence Parity)

- **底座关联表主键非自增防 NULL 陷阱**：
  - 底座 ruoyi-vue-pro 的 PostgreSQL 定义中，`system_role_menu` 表的 `id` 列为主键 `int8 NOT NULL`，但**未绑定** `DEFAULT nextval('system_role_menu_seq')`（不同于 MySQL 的 `AUTO_INCREMENT`）。
  - **强制规约**：为角色分配菜单权限时，必须显式插入 `id` 列并使用 `nextval('system_role_menu_seq')` 生成自增 ID；赋权前必须前置序列判存与序列对齐同步，杜绝主键碰撞：
    ```sql
    CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;
    DO $$ BEGIN PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1)); END $$;
    DELETE FROM system_role_menu WHERE menu_id IN (...);
    INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
    SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
    FROM (SELECT DISTINCT id FROM system_role WHERE code IN ('super_admin', 'admin') OR id IN (1, 2)) r
    CROSS JOIN (SELECT ... AS id) m;
    ```
- **字典类型平滑避让规约 (Dict Type Dynamic Fallback Pattern)**：
  - 严禁在后续模块脚本中硬编码 `DELETE FROM system_dict_type WHERE id IN (...)`（极易击穿如审批引擎等先行脚本已占用的 `2270–2276` 元数据）。
  - **强制规约**：字典清理必须严格基于字典类型 `type IN ('lunaerp_xxx', ...)`；插入时通过 `DO` 块检测预期 ID 是否被其他功能占用，未占用则使用预期 ID，已占用则动态向后顺延分配空闲 ID。
- **跨端共享表最小列集守卫 (Shared Table Minimal Column Set Guard)**：
  - 针对如收付款共享单据（`lunaerp_finance_payment` / `ref`）等跨模块、跨前后端共用的表结构，单侧特性开发时必须严格依照当前工单或 Spec 声明的最小列集创建表结构。
  - **强制规约**：严禁未经声明提前引入单侧衍生快照列（如 `supplier_name`、`payment_date`）或未声明的单列索引，杜绝跨端范围蔓延（Scope Creep）。
- **跨模块字典与错误码多工单协作避让规约 (Dict & Error Code Reservation Workflow)**：
  - 编写涉及系统字典/错误码的新模块 SQL 时，**第一步先查 `docs/contexts/shared/GLOSSARY.md`「历史避让记录」**，确认本模块对应的起始号与既有工单的边界约定。
  - 数据 ID 判定标准化采用**双保险梯次判存模板**（优先判定预留段，若未被占用使用预留段；若意外被占则顺延空闲段，严禁硬编码跳过分配段）：
    ```sql
    IF NOT EXISTS (SELECT 1 FROM system_dict_data WHERE id BETWEEN <reserved_start> AND <reserved_end>) THEN
        v_offset := <reserved_start>;
    ELSIF NOT EXISTS (SELECT 1 FROM system_dict_data WHERE id BETWEEN <fallback_start> AND <fallback_end>) THEN
        v_offset := <fallback_start>;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), <fallback_end>), <fallback_end>) + 1 INTO v_offset FROM system_dict_data;
    END IF;
    ```
- **Druid SQL Parser 与 PostgreSQL 高级方言兼容边界**：
  - 单元测试中的 Druid PostgreSQL 解析器对复杂子查询 JOIN 后的 `ON CONFLICT DO NOTHING` 解析受限。
  - **强制规约**：在满足“先 DELETE 后 INSERT + 序列发号”的幂等操作下，无需且禁止在批量插入尾部追加冗余的 `ON CONFLICT DO NOTHING`，保持 SQL 纯正简洁并规避解析器异常。

## 9. 业务单号发号与顺延重试规约 (Sequential No Generation & Collision Retry)

- 遵循 ADR-0004（合同号）、ADR-0005（供应商号）、ADR-0006（按日发号）。
- **取号与顺延重试标准模式**：
  - **单次前缀扫描基准**：按租户与日期前缀（含软删除记录）扫描最大单号仅执行 1 次，提取已占用最大序号 `maxNum` 与标准位宽 `maxDigits`。
  - **严格递增探测顺延**：重试循环必须基于 `maxNum + i`（`for (int i = 1; i <= MAX_GENERATE_RETRY; i++)`）生成探测单号并查重；严禁在同事务快照下循环重复调用无偏移的生成方法（产生相同单号引发死循环或并发误伤）。
  - **耗尽业务异常阻断**：所有重试候选均被占用时，必须抛出明确的业务错误码（如 `SUPPLIER_RECON_STATUS_NOT_ALLOWED` 或单号冲突业务异常），严禁直接返回冲突单号转嫁给数据库唯一索引抛出底层异常。

## 10. 接口入参 DTO 校验与单测打桩边界 (DTO Validation & Mocking Hygiene)

- **复合更新 DTO 集合非空守卫 (Batch Update DTO Collection Guard)**：
  - 在支持“增删改复合动作”或“部分更新”的 Request VO 中（如既包含待更新 `items` 又包含显式删除 `deletedItemIds`），明细集合字段严禁无条件标记 `@NotEmpty`。
  - 避免仅执行删行操作时被 Bean Validation 400 误拦截；若需防止空请求，应使用 Service 联合判断 `if (CollUtil.isEmpty(items) && CollUtil.isEmpty(deletedItemIds))`。
- **短路分支最小打桩纪律 (Minimal Mocking on Guard Branches)**：
  - 编写参数校验拦截、前置状态守卫或并发抢占拦截等异常单测时，仅打桩至触发异常的最短调用路径。
  - 严禁从正常链路无差别拷贝全量 Mock 打桩，避免业务路径优化或前置校验前移时触发 Mockito 5 严格模式下的 `UnnecessaryStubbingException`。
- **纯 Mockito 单元测试下 MyBatis-Plus Lambda 缓存预热规约 (Lambda Cache Pre-initialization in Pure Unit Tests)**：
  - **场景**：在采用 `@ExtendWith(MockitoExtension.class)` 的纯单元测试中，被测 Service 内部构建了 `new LambdaQueryWrapper<DO>()` 或 `new LambdaQueryWrapperX<DO>()`（如用于悲观锁条件构造、批量核销查询或分页筛选）。
  - **缓存缺失陷阱**：纯 Mockito 环境不加载 Spring Boot 容器与 MyBatis-Plus 实体自动扫描器，首次执行 Lambda 条件提取时抛出 `com.baomidou.mybatisplus.core.exceptions.MybatisPlusException: can not find lambda cache for this entity [com.luna...DO]`。该未捕获异常不仅中断测试，还会因提前跳出方法体导致后续打桩未被执行，级联触发 Mockito 5 的 `UnnecessaryStubbingException`。
  - **强制规约**：
    1. 凡涉及 Lambda 条件包装器的纯 Mockito 单元测试，必须在 `@BeforeEach void setUp()` 中为被引用的实体显式预热注入 TableInfo 缓存：
       ```java
       TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TargetDO.class);
       ```
    2. 若被测方法级联涉及多个实体（如主表实体、明细实体及关联来源单实体），所有在 LambdaWrapper 泛型或方法引用中出现的实体类必须在 `setUp()` 中一并预热，杜绝运行时缓存缺失与误导性测试失败。
- **单测基础实体 Mock 辅助工厂方法规约 (Test Fixture & Mock Factory Pattern)**：
  - 在大型领域 Service 单测中（如涉及仓库、供应商、产品、客户、采购单等多实体引用），测试类应集中定义私有基础实体构建工厂方法（如 `mockWarehouse(id, name, status)`、`mockProduct(id, name, code)`、`mockYarnOrder(id, orderNo)`），避免在单测用例内分散重复手写零碎的 `new EntityDO()` 样板代码，杜绝 TDD 红灯编译期因推测缺失 helper 方法导致的编译中断与往返构建损耗。
- **MyBatis-Plus Wrapper 动态打桩与参数惰性求值规约 (Wrapper Lazy Evaluation & Param Extraction in Mocks)**：
  - **场景**：在纯 Mockito 单元测试中，通过 `when(mapper.update(entity, wrapper))` 或 `when(mapper.selectCount(wrapper))` 拦截调用，并试图从 `Wrapper` 中提取条件参数（如通过 `aw.getParamNameValuePairs()` 提取目标 ID 或过滤值）以驱动动态打桩或内存仓储更新。
  - **惰性求值陷阱**：MyBatis-Plus 的 `AbstractWrapper` 中，参数命名与键值对（`MPGENVAL1`、`MPGENVAL2`...）是在构建 SQL 片段（调用 `getSqlSegment()` / `getCustomSqlSegment()`）时**惰性生成并填充**入 `paramNameValuePairs` 的。在 Mockito 拦截桩中，如果直接调用 `aw.getParamNameValuePairs()`，返回的将始终是一个空的 Map `{}`，导致无法按预期解析参数值，引发条件漏判、误更目标或测试假失败。
  - **强制规约**：
    1. 凡在 Mock 拦截逻辑中需要读取 `aw.getParamNameValuePairs()` 的，必须在读取前显式调用一次 `aw.getSqlSegment()` 触发包装器的内部惰性求值与参数映射：
       ```java
       when(mapper.update(any(), any(Wrapper.class))).thenAnswer(inv -> {
           Wrapper<?> wrapper = inv.getArgument(1);
           if (wrapper instanceof AbstractWrapper<?, ?, ?> aw) {
               aw.getSqlSegment(); // 关键：触发包装器惰性求值以填充 paramNameValuePairs
               Map<String, Object> params = aw.getParamNameValuePairs();
               // 此时 params 包含 MPGENVAL1 -> 1002L 等实际入参
           }
           ...
       });
       ```
    2. 针对包含集合条件的查询（如 `.in(Column, list)`），MyBatis-Plus 在生成 SQL 时会将集合展开为多个标量参数（`MPGENVAL2 -> "VAL_A"`, `MPGENVAL3 -> "VAL_B"`），`params.values()` 中的元素为具体标量而非 `Collection`，判断时应以具体标量匹配或检查 `aw.getSqlSegment().contains("column_name")`。
- **单测软删除真实状态翻转与防伪造打桩规约 (Soft-delete Repository State Mutation in Mocks)**：
  - **场景**：在单测中验证软删除、回收站恢复（`RecycleBinHandler.restore`）或状态逆向流转时。
  - **伪造打桩陷阱**：直接调用 Service 层的删除方法后，因底层 `mapper.deleteById(id)` 未打桩或默认返回 0，测试人员易在测试方法体内手动编写 `entity.setDeleted(true)` 等内存状态强制赋值，掩盖了真实删除逻辑未执行或异常未触发的缺陷。
  - **强制规约**：
    1. 严禁在测试主体中通过手动 `entity.setDeleted(true)` 伪造状态来强行配合后续断言。
    2. 必须在 `mapper.deleteById(anyLong())` 的 Mock 行为中真实模拟底层仓储翻转（`entity.setDeleted(true)`），确保 Service 删除调用的真实性与被测度：
       ```java
       when(mapper.deleteById(anyLong())).thenAnswer(inv -> {
           Long id = inv.getArgument(0);
           EntityDO entity = repo.get(id);
           if (entity != null) entity.setDeleted(true);
           return 1;
       });
       ```

## 11. 跨服务涌现语义单测归位规约 (Cross-service Emergent Behavior & Anti-Tautological Mocking)

- **自测桩反模式守卫 (Anti-Tautological Mocking)**：
  - 严禁在被测类的单测方法中直接调用并断言第三方依赖的 Mock 对象（例如 `when(serviceB.query()).thenReturn(empty); assert(serviceB.query().isEmpty());`）。这种调用脱离了被测对象（SUT）的方法执行，属于虚假的自测桩，无法验证真实的业务逻辑。
- **跨服务涌现行为的职责切分 (Testing Boundary for Emergent Behavior)**：
  - 当业务语义表现为“Service A 状态流转 -> Service B 的查询行为自动产生联动响应（如排除该状态、释放资源指纹）”时：
    1. **状态流转方单测 (Service A)**：职责限于验证 Service A 自身的动作。通过 `ArgumentCaptor` 严格验证写库实体状态是否正确变更为终态（如 `CANCELLED`），并验证前置业务守卫。
    2. **联动消费方单测 (Service B)**：职责验证数据过滤与释放口径。必须在 Service B 自身的单测文件（如 `SupplierReconSourceServiceImplTest`）中覆盖，验证候选池查询确实已将终态记录排除在有效占用之外，杜绝在 Service A 中虚假打桩。

## 12. 通用套打契约规范与脱敏豁免 (Print Contract Standards & Masking Exemption)

- **套打出参金额脱敏豁免 (Unmasked Print Contract)**：
  - 遵循 `docs/contexts/shared/GLOSSARY.md`：A4 物理套打单据面向线下签署、实物结算或核验，金额与关键业务字段必须原样明文打印。
  - 财务域架构守护（`ArchitectureStandardsTest`）中，套打出参（`*PrintRespVO` 或名称包含 `Print` 的 VO）显式豁免 `@Sensitive` 脱敏注解，严禁对套打单据金额追加星号脱敏。
- **套打接口路由与入参规范 (Single Query Endpoint)**：
  - 打印数据接口统一使用 `@GetMapping("/print-data")` 配合 `@RequestParam("id") Long id`，与 04 号通用套打前端契约单一对齐，严禁随意声明多重 restful 路径映射（如 `@GetMapping({"/print-data", "/{id}/print-data"})`）。
- **套打出参最小化与真实性守卫 (Minimal Print Scope Guard)**：
  - 打印 VO 字段定义必须严格依循工单 Spec 声明的必要范围，禁止投机性扩展非必要或终态生命周期元数据（如在正常对账打印 VO 中冗余引入 `cancelledBy` 等字段）。
  - 严禁声明无底层数据源支撑的“占位字段”（例如在 VO 中声明 `supplierContact` 但底层仅有 `phone` 导致字段永久为空）。

## 13. 累计上限并发守卫与行级悲观锁规约 (Concurrency Limit Guards & Row-level Locking)

- **场景**：业务中存在“累计已申请 / 已核销 / 已排产 / 已抵扣 + 本次操作额 ≤ 来源单据总额”的业务守卫（如付款申请挂靠对账单、排产派工挂靠订单）。
- **并发超申与幻读陷阱**：
  - 在可重复读（RR）或读已提交（RC）事务隔离级别下，若仅执行 `SELECT SUM(...)` 只读聚合计算并与上限比较后直接 `INSERT`，并发事务读取的是同一历史快照，均能通过校验，导致最终累计值严重击穿上限（超申/超额/超配）。
- **强制规约**：
  - **来源行悲观锁串行化**：在事务起始处，必须对被引用的来源根实体行施加行级排他锁（`FOR UPDATE`），将竞争同一来源单据的并发写事务串行化：
    `SourceDO source = sourceMapper.selectOneForUpdate(SourceDO::getId, sourceId);`
  - **锁后原子重查与上限校验**：持有行锁后，在同一事务上下文重新执行累计值聚合（排除终态或当前单据），若 `appliedSum + currentAmount > source.getTotalAmount()`，必须抛出明确业务错误码（如 `022_007`）。
  - **纯 Mockito 单元测试打桩规约**：
    `BaseMapperX.selectOneForUpdate(SFunction, Object)` 内部委托 `selectOneForUpdate(LambdaQueryWrapper)`。单测打桩应使用 `when(sourceMapper.selectOneForUpdate(any(LambdaQueryWrapper.class))).thenReturn(sourceDO);`，Service 内部可通过判空短路兜底支持 `selectById`，保证真实运行加锁生效与单元测试解耦兼顾。

## 14. 特性工单范围守卫与投机性机能防范 (Spec Scope Hygiene & Opportunistic Feature Guard)

- **场景**：特性工单开发过程中，编写 CRUD、查询筛选或关联引用时。
- **强制规约**：
  - **坚持 Spec 最小契约原则**：严禁在未获工单显式声明的情况下顺带扩展全局检索维度（例如在只要求关联选择器远程搜索的工单中，顺手向主单综合搜索 `keyword` 拼接非索引字段或更改搜索框 placeholder）。
  - **严禁跨工单字段顺带修补**：在修改底层手写 SQL（如 Mapper `@Select`）时，严禁顺带“修复”未包含在本工单范围内的其他未完成/未声明字段（如跨工单的检验单列），避免引发非预期回流审查与越界判定噪声。
  - **跨端扩展先声明后实现**：若联调确实需要扩展对侧字段或契约，必须在实现前在工单中显式声明向前兼容扩展点或增补 issue，严禁静默进行未经声明的跨端范围蔓延。
- **既有业务表单局部扩列与 Git Diff 噪声守卫 (Form Item Extension Hygiene)**：
  - 在向既有 Vue SFC 表单（如 `BasicProfileTab.vue`）或 API接口（如 `CustomerVO`）扩展字段时，严禁让自动化格式化工具对邻近无关代码行（如原有折叠单行的输入框、方法签名等）进行无差别换行展开。
  - **强制规约**：仅在目标插入点写入声明的新增表单项/类型属性；提交前执行 `git diff <file>` 自查，将非本工单引入的邻近格式化行恢复原样，杜绝 Diff 审查噪声与伪范围蔓延判定。
- **Update VO 属性极小集原则 (Update VO Minimal Field Set Guard)**：
  - 更新接口 Request VO 严禁默认全盘拷贝 Create VO 字段；必须严格依照 Spec 声明的可编辑列集合（如仅允许修改状态/备注的账户、仅允许变更特定配置的单据）。
  - **强制规约**：严禁投机性放开未在 Spec 声明的关键标识、名称、金额等敏感维度的更新入口；只读不可变字段（如银行账户名、期初余额、分录编号等）在 Update VO 中坚决不暴露，从契约根源杜绝越权或非法覆写。
  - **自动化守护保障**：全域由 `ArchitectureStandardsTest.testUpdateVOsFollowMinimalFieldSetConvention()` 自动反射检验所有 `*UpdateReqVO`，对黑名单不可变字段（`effectiveDate`, `accountName`, `currency`, `tenantId`, `openingBalance`, `applyNo`, `reconNo`, `statementNo`, `partyType`, `partyId`, `shipmentId`, `returnNo`, `sourceId`, `sourceNo`, `sourceDocType`, `sourceDocId` 等）进行前置物理拦截。
- **工单错误码声明与常量命名对齐规约 (Error Code Citation Hygiene)**：
  - 特性工单在描述业务校验要求时，若引用业务错误码，必须以常量标识符或对照 `docs/contexts/<domain>/GLOSSARY.md` 段位总表引用，严禁凭直觉或假设裸写未在基建工单确认的数字代号（如臆测数字），避免导致实现方与 Reviewer 因字面错位产生伪需求冲突；在常量已落库时优先写出明确常量名（如 `SALES_RETURN_REASON_EMPTY`）。
- **全局共享存量配置文件格式化防污染守卫 (Shared Config Formatting Hygiene)**：
  - 对 `luna-ui/src/utils/dict.ts`、`luna-ui/src/router/modules/remaining.ts` 等全局共享存量大文件，禁止执行整文件 `npx prettier --write`。
  - **强制规约**：必须采用局部单块代码替换或精细编辑，提交前通过 `git diff` 严格审查，严禁带入跨业务模块的排版与空白字符 Diff 噪音。
## 15. MyBatis-Plus 实体可空业务字段更新策略规约 (Nullable Field Update Strategy & Clearability Parity)

- **场景**：在 DO 实体中新增由前端表单或业务接口控制、且允许用户清空/取消绑定的业务字段（如信用账期天数 `creditDays`、归属人 `salespersonUserId`、部门 `deptId`、备注、开票信息等）。
- **MyBatis-Plus 默认更新策略静默丢失陷阱 (FieldStrategy.NOT_NULL Trap)**：
  - MyBatis-Plus 的 `BaseMapper.updateById(entity)` 默认字段更新策略为 `FieldStrategy.NOT_NULL`。
  - 当前端或调用方将该字段置为 `null`（如用户清空账期、解绑业务员）并发起更新时，MyBatis-Plus 生成的 `UPDATE` SQL 会直接忽略该列，导致数据库存量数据无法被清空，造成静默数据残留与状态无法回退。
- **强制规约**：
  1. **实体注解显式声明**：凡属于业务上允许置空、且由实体更新接口（如 `updateById`）承载的属性，在 DO 实体中必须显式声明注解：
     ```java
     @TableField(updateStrategy = FieldStrategy.ALWAYS)
     private Integer creditDays;
     ```
  2. **置空单测标准覆盖**：涉及该字段的 Service 单元测试必须标准化包含置空场景（如 `updateCustomer_clearCreditDays_success()`），通过 Mockito `ArgumentCaptor<EntityDO>` 捕获并断言更新对象的对应属性值为 `null`，确保更新策略在持久层生效。
  3. **快捷状态流转防静默清空**：当 Update VO 承载快捷操作（如停用/启用切换、审批流转、撤回）且未显式传递可空业务字段时，Service 层在组装更新实体前必须保留原值（`updateDO.setRemark(reqVO.getRemark() != null ? reqVO.getRemark() : existing.getRemark())`），杜绝因 `FieldStrategy.ALWAYS` 将历史业务备注或关键说明在单据流转时静默置空为 NULL。

## 16. 符号重命名与代码重构全引用检索纪律 (Symbol Rename & Refactoring Hygiene)

- **场景**：响应 Code Review 意见对类内私有方法、常量、复用变量或内部助手进行重命名/签名调整（如将包含副作用的 `backfillShortName` 重构为 `backfillDefaults`）。
- **局部直觉重构与长构建周期浪费陷阱**：
  - 仅凭当前阅读视窗修改了眼前的 1~2 处调用点及方法签名，遗漏了同文件或本模块其他方法（如 Excel 导入、批量转换、校验分支等）中的调用点。
  - 直接触发 Maven 测试或长周期构建命令，导致经过数十秒编译后才抛出 `cannot find symbol` 失败，严重消耗 token 并打断会话流畅度。
- **强制规约**：
  1. **重构前全量符号定标**：重命名或变更方法签名之前，**必须先在文件内全局检索目标符号**（或在模块级使用跨平台 `git grep "<symbol>" <dir>`），锁定全量调用点清单。
  2. **原子性全量替换**：依据清单一次性完成全部调用点与方法签名的替换，避免凭记忆散落修改。
  3. **前置语法轻量自检**：在发起 Maven 测试等后台构建前，确保所有声明与调用均已收敛闭环，杜绝将简单语法与编译符号错误转嫁给耗时的构建任务。

## 17. 财务经营账对账单余额守恒与跨期分录追加规约 (Balance Conservation Invariant & Historical Entry Parity)

- **场景**：客户对账单（`CustomerStatement`）与供应商对账单（`SupplierRecon`）等期间经营账对账单的生成、重新计算与草稿手工追加/移除明细分录。
- **对账单余额守恒定律 (Balance Conservation Invariant)**：
  - **守恒公式**：`期末应收/应付余额 = 期初未核销余额 + 本期借方发生合计 − 本期贷方核销合计`。
  - **期初余额口径**：期初余额是**对账起始日（`periodStart`）之前**全量未核销/未冲减分录的净值聚合快照。
- **历史分录追加防双计陷阱 (Historical Entry Double-Counting Trap)**：
  - 当在草稿态允许手工向对账单追加业务日期早于对账单起始日（`bizDate < periodStart`）的往期历史漏记分录时：
  - 该分录的原发生金额**早已隐式沉淀于期初未核销余额 `openingBalance` 中**。
  - 若直接将该分录插入对账单明细并仅累加至 `periodDebit` 或 `periodCredit`，会导致该笔金额在计算 `closingBalance = openingBalance + periodDebit - periodCredit` 时**被重复计算两次**，严重破坏账实守恒与客户对账权威性。
- **强制规约**：
  1. **期初余额同步逆向剥离**：
     手工向对账单追加 `bizDate < periodStart` 的历史分录时，必须在增加本期发生额的同时，**同步逆向调整对账单的 `openingBalance`**：
     - 若追加的是借方分录（应收增加）：`newOpening = statement.getOpeningBalance().subtract(newItem.getDebitAmount())`；
     - 若追加的是贷方分录（核销/冲减）：`newOpening = statement.getOpeningBalance().add(newItem.getCreditAmount())`；
     - 确保 `closingBalance` 账实守恒不变，同时分录在明细行中被显式化。
  2. **跨期红冲分录承接规约**：
     发货红冲分录（`AR_INC_REVERSAL`）若发生在当期，但其被冲减的原发货分录（`reversalOfId`）发生于前期（不在当期候选列表中）：
     严禁直接跳过该红冲分录；必须将其作为当期贷方冲减分录独立入单，确保前期已计入期初的发货金额在当期得到准确扣减。

## 18. 财务经营分录级联状态守卫与账龄勾稽恒等式规约 (Ledger Entry Cascading State Guard & Aging Reconcile Invariant)

- **场景**：客户账龄汇总与下钻（`CustomerAgingService`）、客户/供应商对账单候选提取（`CustomerStatementService`）等消费经营分录账（`ledger_entry`）的统计计算与报表分析。
- **已红冲单据核销分录级联作废陷阱**：
  - 收款单（`PaymentDO`）执行红冲（`reverseReceipt`）时，系统追加反向 `RECEIPT_REVERSAL` 分录并将原单状态置为 `REVERSED`，使得关联核销行重开。
  - 但其在过账时产生的 `RECEIPT_WRITE_OFF` / `ADVANCE_WRITE_OFF` 经营分录**不会物理删除，亦不生成反向核销分录**（仅回指原 `ref_id = payment.id`）。
  - 若消费端服务仅按 `category IN ('RECEIPT_WRITE_OFF', 'ADVANCE_WRITE_OFF')` 汇总应收减少额，未反查关联收款单的生命周期终态，会导致已红冲单据的核销额继续非法冲减应收，造成应收虚减与重大财务审计事故。
- **发货红冲净额前置扣减与明细勾稽失衡陷阱**：
  - 在 FIFO 账龄纯函数计算中，若发货单（`AR_INC`）发生发货红冲（`AR_INC_REVERSAL`），纯函数通常以前置扣除红冲后的净额（`netOriginalAmount = originalAmount - reversalAmount`）参与 FIFO 队列冲抵。
  - 若下钻明细表格直接向前端暴露 `originalAmount`（原始金额）、`writtenOffAmount`（仅统计本次 FIFO 消耗额）与 `remainingAmount`（结存余额），会导致 `originalAmount - writtenOffAmount != remainingAmount`，呈现虚假的不平齐（不平金额恰好等于已红冲额）。
- **强制规约**：
  1. **核销分录级联状态守卫 (Reversed Payment Cascade Filter)**：
     - 所有消费 `RECEIPT_WRITE_OFF` / `ADVANCE_WRITE_OFF` 进行应收冲减或对账候选汇总的服务，**必须**批量提取所有 `refType = 'RECEIPT'` 的关联单据 ID，通过 `paymentMapper.selectBatchIds(ids)` 校验其状态。
     - 凡属于 `PaymentStatus.REVERSED` 的收款单，其派生的一切核销分录与预收分录必须严格排除，严禁计入冲减队列。
  2. **账龄明细下钻勾稽恒等式 (Aging Detail Balance Parity)**：
     - 在账龄未核销明细行（`AgingDetailItem` / `CustomerAgingDetailRespVO`）中，**必须保证严格数学恒等式**：
       `originalAmount（原始金额） - writtenOffAmount（已核销/冲减） == remainingAmount（未核销余额）`。
     - 当发生发货红冲时，`writtenOffAmount` 必须统合“发货红冲扣减额 + FIFO 核销消耗额”（列名定标为【已核销/冲减】），或在 VO 中独立声明并透传 `reversalAmount`（已红冲金额）列，严禁输出数学公式失衡的明细数据。

## 19. 回收站业务处理器校验与单测覆盖规约 (RecycleBinHandler Validation & Unit Test Matrix)

- **场景**：业务单据（客户对账单 `STATEMENT`、收付款单 `PAYMENT`、发票申请 `INVOICE_APPLICATION` 等）实现 `RecycleBinHandler` SPI 接口，支持草稿进入回收站、分页浏览、恢复（restore）与彻底物理删除（purge）。
- **生命周期准入与恢复不补账守卫（ADR-0038）**：
  - **仅初始草稿态允许进站**：进入回收站仅限未记账、未生效的初始草稿态（对账单仅 `status = 'DRAFT'`、收款单仅 `approval_status = 'PENDING'`、发票申请仅 `status = 'DRAFT'`）。已过账/生效终态（`CONFIRMED`/`COMPLETED`/`ISSUED`）严禁软删进回收站，逆向操作必须走财务冲减（如发货红冲、收款红冲、作废发票）。
  - **恢复纯净不补账**：回收站恢复操作仅还原主表与明细行子表软删除标记（`deleted = 0`），并恢复至草稿态，**严禁触发任何分录记账、资金账户余额变动或凭证重新生成**。
- **关联引用阻断与状态区分陷阱**：
  - 在 `validateRestore(id)` 中执行前置引用校验时，必须**严格区分“主数据在回收站中”与“主数据彻底不存在”**：
    - 查询已删除档案 `selectDeletedById(refId, tenantId) != null`：说明被引用对象处于回收站，抛出 `RECYCLE_BIN_REFERENCE_IN_RECYCLE(name)`，引导用户先去回收站恢复上游基础档案；
    - 查询未删除档案 `selectById(refId) == null`（且不在回收站）：说明主数据不存在或已被物理清空，**必须抛出领域业务异常**（如 `CUSTOMER_NOT_EXISTS`、`SUPPLIER_NOT_EXISTS`），严禁泛化抛出“XX在回收站中”引发运维误判。
- **强制规约与单测 4 维覆盖矩阵**：
  每个实现的 `RecycleBinHandlerTest` 单元测试**必须**完整覆盖以下 4 维用例，缺失任何一项视为测试不完备：
  1. **元数据正确性**：断言 `getBizType()` 与 `getName()` 契约与领域枚举对齐；
  2. **草稿恢复与级联复活**：断言正常草稿态恢复成功，主表与明细行子表均被级联恢复（`restoreByXxx` 被调用），且无分录记账副作用；
  3. **前置引用阻断两路校验**：
     - 上游档案在回收站中抛出 `RECYCLE_BIN_REFERENCE_IN_RECYCLE`；
     - 上游档案不存在抛出对应领域的 `XXX_NOT_EXISTS`；
  4. **非进站资格反向拒绝**：针对非草稿态（如已生效/过账）单据或不存在单据，SQL 过滤（如 `WHERE status = 'DRAFT'`）查得 `null`，执行 `restore(id)` / `purge(id)` 必须断言抛出 `RECYCLE_BIN_RECORD_NOT_EXISTS`。



