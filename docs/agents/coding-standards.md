# Coding standards for agents

后端（`luna-srv`）与前端（`luna-ui`）开发规范与 Reviewer 审查基准。

## 1. 集合查询空安全 (Collection Query Safety)

- **陷阱**：`LambdaQueryWrapperX.inIfPresent(col, values)` 在 `values` 为 `null` 或**空集合**时均会跳过条件。若将派生 ID 集合直接喂入 `inIfPresent`，会导致 `IN` 条件丢失，退化为全表/全租户无限制扫描。
- **强制规约**：
  - 依赖派生集合过滤的业务 Service，必须前置短路守卫：`if (CollUtil.isEmpty(ids)) { return Collections.emptyList(); }`，避免无谓的数据库往返。
  - 框架已在 `LambdaQueryWrapperX` 提供 `inOrEmpty(col, values)`：当 `values` 为空集或 `null` 时自动追加 `1 = 0` 永假条件；非空时拼接 `IN`。在必须强制以集合过滤的 SQL 条件中优先使用 `inOrEmpty`。

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

## 4. Mapper 纯粹性与 Mockito 集合匹配 (Mapper Mocking)

- **场景**：Mapper 接口方法定义及纯 Mockito 单元测试。
- **强制规约**：
  - **接口纯粹性**：Mapper 专注定义 SQL 映射方法，空安全由 Service 前置守卫负责，避免在 Mapper 中编写冗余 `default` 包装方法（容易导致 Mockito 穿透执行真实方法体及产生推测性死代码）。
  - **空参数匹配**：在纯 Mockito 单测中，当打桩方法的重载参数为 `Collection` 且可能为 `null` 时，必须使用 `nullable(Collection.class)` 或明确的具体对象，严禁使用 `anyCollection()`（`null` 会导致匹配失败）或无限定的裸 `any()`（产生重载歧义）。
  - **集合类型相等性**：Java 中 `Set.equals(List)` 恒为 `false`。若 Service 实现层传入 `Set`（如 `Map.keySet()`），单测打桩严禁使用 `eq(List.of(...))`，优先使用 `argThat(c -> c.contains(...))`、`nullable(Collection.class)` 或传入匹配的具体集合类型。

## 5. 工作区快速文件检索 (Fast File Search)

- 混合仓库根目录下包含 `luna-ui/node_modules/`、`target/`、`.claude/` 等深度依赖和构建产物目录。
- **检索规约**：
  - 禁止在根目录使用无深度限制且未排除依赖目录的 `Get-ChildItem -Recurse`。
  - 优先使用 `git ls-files "*<keyword>*"` 快速精准定标文件。
