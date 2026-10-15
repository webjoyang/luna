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

## 4. Mapper 纯粹性与 Mockito 集合匹配 (Mapper Mocking)

- **场景**：Mapper 接口方法定义及纯 Mockito 单元测试。
- **强制规约**：
  - **接口纯粹性**：Mapper 专注定义 SQL 映射方法，空安全由 Service 前置守卫负责，避免在 Mapper 中编写冗余 `default` 包装方法（容易导致 Mockito 穿透执行真实方法体及产生推测性死代码）。
  - **空参数匹配**：在纯 Mockito 单测中，当打桩方法的重载参数为 `Collection` 且可能为 `null` 时，必须使用 `nullable(Collection.class)` 或明确的具体对象，严禁使用 `anyCollection()`（`null` 会导致匹配失败）或无限定的裸 `any()`（产生重载歧义）。
  - **集合类型相等性**：Java 中 `Set.equals(List)` 恒为 `false`。若 Service 实现层传入 `Set`（如 `Map.keySet()`），单测打桩严禁使用 `eq(List.of(...))`，优先使用 `argThat(c -> c.contains(...))`、`nullable(Collection.class)` 或传入匹配的具体集合类型。

## 5. 工作区快速代码与文件检索 (Fast Code & File Search)

- 混合仓库根目录下包含 `luna-ui/node_modules/`、`target/`、`.claude/` 等深度依赖和构建产物目录；**仓库根目录下无 `src/` 源码目录**。
- **检索规约**：
  - 禁止在根目录使用无深度限制且未排除依赖目录的 `Get-ChildItem -Recurse`。
  - 文件路径定标优先使用 `git ls-files "*<keyword>*"`。
  - 文件内容/代码符号检索一律使用跨平台原生的 `git grep "<pattern>"`（或 `git grep -i "<pattern>"`），严禁在 PowerShell 下使用 `xargs` / Linux `grep` 管道；必须显式指定子工程目录路径（如 `git grep "<pattern>" luna-ui/ luna-srv/`），严禁使用不存在的根路径 `src/`。

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

## 7. 前端单测与 Vitest 纯 Node 环境守卫 (Frontend Vitest Hygiene)

- **Node 环境与无 DOM/SFC 边界**：
  - `luna-ui/vitest.config.ts` 配置为纯 `node` 环境，未挂载 `@vitejs/plugin-vue` 与 JSDOM。
  - **强制规约**：前端单测定位为领域纯函数与数据适配器测试（`model/*.ts`、`utils/*.ts`），**严禁在单测中直接 import `.vue` 组件**。
- **Axios 顶层打桩规约**：
  - `@/config/axios` 会级联导入系统路由并在根作用域执行 `createWebHistory`（依赖 `window`）。
  - **强制规约**：单测中若间接依赖 API 调用，必须在测试文件顶层显式声明 `vi.mock('@/config/axios', ...)`，避免在纯 Node 运行环境中报 `ReferenceError: window is not defined`。

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




