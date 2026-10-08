# 02: 4 表 customizer 注册 + 系统侧 `@DataPermission(enable=false)`（仅现有方法 1–4）+ 4 档可见性验证

**What to build:** lunaerp 模块新增一个 `@Configuration` 注册 `DeptDataPermissionRuleCustomizer` Bean，把客户/合同/发货 3 表的 `dept_id` / `salesperson_user_id` 列注册进行级数据范围规则（报价随 05 票）。把现有系统侧读注册表的方法（取号扫描、未发量聚合、生产进度只读聚合）打 `@DataPermission(enable=false)`。落点后用 §9.1 组织造数 fixture 验证 4 档可见性在 3 张已存表的 list/detail/单条写路径上对齐底座行为（多角色取并集、任一 ALL 即全部）。

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] lunaerp 模块新增 `@Configuration`，注册 `DeptDataPermissionRuleCustomizer` Bean，映射 CustomerDO/SalesContractDO/ShipmentDO 三表的 `addDeptColumn("dept_id")` / `addUserColumn("salesperson_user_id")`（仿 system 模块 `DataPermissionConfiguration`）。QuotationDO 随 05 票；联系人表、所有子表（quotation_item / contract_item / shipment_item / shipment_roll）不注册
- [ ] §5.2 第 1 项：合同取号扫描+手填查重——`SalesContractServiceImpl#generateContractNo` / `generateContractNoWithRetry` / `validateContractNoUnique`（约 L410–478）及其底层两个 `…IncludingDeleted` @Select 打 `@DataPermission(enable=false)`
- [ ] §5.2 第 2 项：发货取号两方法——`ShipmentServiceImpl` 中调用 `selectByShipmentNoIncludingDeleted` / `selectListByShipmentNoPrefixIncludingDeleted` 的方法打 `@DataPermission(enable=false)`
- [ ] §5.2 第 3 项：合同未发量聚合——`ShipmentServiceImpl#selectListByContractIdAndStatus`（约 L321）打 `@DataPermission(enable=false)`，必须统计租户内全部 COMPLETED 发货，否则受限档会算虚假未发量导致超发
- [ ] §5.2 第 4 项：生产进度只读聚合——`ProductionProgressQueryServiceImpl#getProductionProgressPage`（候选合同 L145、发货 L521 等 selectListByContractIds）打 `@DataPermission(enable=false)`，系统级跨单聚合视图（ADR-0034）显式锁定语义
- [ ] §5.2 第 5–8 项是其他票契约（02 审批回写/10+13 财务过账/04 回收站/各删除引用守卫），不在本票实现；核"已落地者（如 approval-engine）是否已带注解"，缺则补注并记录在 Comments
- [ ] 用 §9.1 fixture 验证：ALL（boss/finance）见全部；DEPT_AND_CHILD（m）见 a+c 不见 d；DEPT_ONLY 见 a 不见 c/d；SELF（a）只见本人；多角色并集（用户同挂 SELF+DEPT_ONLY → 并集可见；挂一个 ALL → 全部）
- [ ] 边界：无登录用户规则不生效；用户无启用角色时底座给 `self=true`（只能看本人）；deptIds 与 self 皆空时拼 `null=null`；空归属行仅 ALL 档可见
- [ ] easy-trans 翻译走底座 `DataPermissionRuleFactoryImpl#isTranslateCall()` 自动豁免，不借道泄漏归属外字段
- [ ] 租户拦截器独立工作，关闭数据权限不影响 `tenant_id` 追加——专测一条断言跨租户隔离仍生效（对齐 16 号 §6.3）
