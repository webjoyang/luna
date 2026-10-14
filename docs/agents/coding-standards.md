# Coding standards for agents

后端（`luna-srv`）与前端（`luna-ui`）开发规范与 Reviewer 审查基准。

## 1. 集合查询与条件构造 (Collection Query & Wrapper Safety)

- **集合空安全陷阱**：`LambdaQueryWrapperX.inIfPresent(col, values)` 在 `values` 为 `null` 或**空集合**时均会跳过条件。若将派生 ID 集合直接喂入 `inIfPresent`，会导致 `IN` 条件丢失，退化为全表/全租户无限制扫描。
- **集合空安全强制规约**：
  - 依赖派生集合过滤的业务 Service，必须前置短路守卫：`if (CollUtil.isEmpty(ids)) { return Collections.emptyList(); }`，避免无谓的数据库往返。
  - 框架已在 `LambdaQueryWrapperX` 提供 `inOrEmpty(col, values)`：当 `values` 为空集或 `null` 时自动追加 `1 = 0` 永假条件；非空时拼接 `IN`。在必须强制以集合过滤的 SQL 条件中优先使用 `inOrEmpty`。
- **MyBatis-Plus Wrapper 链式调用类型截断陷阱**：
  - MyBatis-Plus 原生 `AbstractLambdaWrapper` 的泛型返回参数固定为 `LambdaQueryWrapper<T>`，一旦先调用原生 `.ge()` / `.le()` / `.eq()` / `.orderBy*()`，方法链返回类型退化为原生 Wrapper，导致后续链式调用 `eqIfPresent` / `inOrEmpty` 报编译错误（找不到符号）。
  - **强制规约**：`LambdaQueryWrapperX` 自定义方法（如 `*IfPresent`、`inOrEmpty`）必须链式书写在原生方法（`ge`、`le`、`orderBy*`）**之前**，或直接使用扩展提供的 `geIfPresent`、`leIfPresent`、`betweenIfPresent`。
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

- 混合仓库根目录下包含 `luna-ui/node_modules/`、`target/`、`.claude/` 等深度依赖和构建产物目录。
- **检索规约**：
  - 禁止在根目录使用无深度限制且未排除依赖目录的 `Get-ChildItem -Recurse`。
  - 文件路径定标优先使用 `git ls-files "*<keyword>*"`。
  - 文件内容/代码符号检索一律使用跨平台原生的 `git grep "<pattern>"`（或 `git grep -i "<pattern>"`），严禁在 PowerShell 下使用 `xargs` / Linux `grep` 管道。

## 6. 前端组件与交互规范 (Frontend Standards)

- **按钮级权限指令守卫 (`v-hasPermi`)**：
  - 所有触发状态流转或数据写操作的按钮（新增、编辑/修改、删除、转机、调度/派工、审批等），必须显式绑定 `v-hasPermi="['<permission-code>']"`，禁止裸露可操作按钮。
- **业务错误码常量化与异常拦截 (Error Code Constants)**：
  - 接口业务异常与拦截（如排产时段冲突 `1030036000`）必须优先以常量形式定义并复用（如 `SCHEDULE_CONFLICT_CODE`），通过 `err.code === CONSTANT` 进行精确匹配；严禁仅依赖 `errorMsg.includes('...')` 模糊中文文本匹配作为核心业务逻辑分支。
- **只读投影回写参数对齐 (Round-trip Form Payload)**：
  - 在前端弹窗、抽屉或拖拽改期中回传实体保存接口时，必须完整回显/携带原有关键外键与快照（如操作员 ID、织机 ID、计划版本等），避免只提交修改项导致未改项被覆写清空。
- **看板图形视口与明细列表数据口径解耦 (View Disconnection)**：
  - 在包含甘特图/时间轴图形与下挂明细表格/分页列表的复合页面中，图形视口通常受用户缩放（日/周/月）或水平滚动窗口裁剪；而明细列表承担全局检索与分页排查职责。
  - **强制规约**：明细列表的筛选与查询请求必须且仅严格绑定顶部显式过滤条件（或独立分页器），严禁将明细列表静默绑定到图形视口的临时可见时间窗口，避免造成用户在列表中“搜不到视口外已有任务”的交互困惑。
- **业务操作提交助手单一来源 (DRY Submit Helpers)**：
  - 同一领域实体的更新逻辑可能散布在多个交互场景（如甘特图拖拽调整、行内操作菜单、独立编辑弹窗）。
  - **强制规约**：对同一后端修改 API 的参数对齐、冲突错误码拦截（如 `SCHEDULE_CONFLICT_CODE`）、消息提示与局部/全局数据刷新，应提取统一的 Composable 或 Model 辅助函数封装，严禁在多处 Vue 组件模板中复制散落的 `try-catch` 和错误分支拼装代码。


