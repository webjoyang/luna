# 数据范围接线：销售归属链接通芋道 4 档行级数据范围

Status: ready-for-agent
波次：W0 后（依赖 module-gateway、common-foundation；16 号验收 gate G2 依赖本票）
定位：把芋道底座 DataPermission 5 档收敛为商业化 **4 档**（全部 / 本部门及以下 / 本部门 / 本人，弃用并隐藏"自定义部门"档），仅给**销售归属链**接通行级数据范围，并冻结"系统侧调用一律显式关闭"的后续模块契约。取舍记录见 **ADR-0046**。

> 验收基准：商业化验收 spec `.scratch/commercialization-acceptance/spec.md` §6.2（4 档走查）即本票验收底稿，本 spec 不另立口径，只保证 5 表在该组织构造下全部可过。

---

## 1. 决策记录（grilling 三轮 Q1–Q18，全部按推荐锁定）

| # | 决策 |
|---|---|
| Q1 | 4 张持单/档案表统一归属列：user 列一律 `salesperson_user_id`（合同/发货已有不改名，客户新 ALTER，报价随 05 票建表自带），dept 列一律 `dept_id`；联系人表零列继承（Q2） |
| Q2 | 客户联系人**不加列、不注册**规则；列表 EXISTS 子查询继承客户归属，详情/写操作先做客户可见性校验，不可见统一"客户联系人不存在" |
| Q3 | 新建默认归属=当前登录用户（表单可改）；`dept_id` **服务端按最终归属人当前部门实时推导**，不信前端入参；归属人为空则两列皆空，空归属行仅 ALL 档可见；Excel 导入无归属列，归属操作人 |
| Q4 | 存量回填幂等只填空：客户 owner=creator→租户管理员兜底；合同/发货 dept 优先已有 salesperson 的部门，再退 creator；兜底=该租户 tenant_admin/根部门（子查询定位，不写死 id） |
| Q5 | **调岗同步**：lunaerp 切面拦截 system 用户更新，dept_id 变化时同事务刷 4 表 dept_id；另附幂等校正 SQL。满足 16 号 §6.2"换岗立即可见性变化"，不采用芋道快照默认 |
| Q6 | 自定义档四层收口：前端白名单过滤 value=2、后端 `assignRoleDataScope` 拒绝 2（system 新错误码 1_002_002_006）、底座枚举/分支代码不删、存量 `common`→SELF、`test-dp`→ALL 并清 deptIds |
| Q7 | 11 seed 角色 data_scope 基线：boss/finance/planner/purchaser/warehouse_manager/warehouse_keeper/inspector=1；sales_manager=4；salesperson/workshop_worker/workshop_inspector=5。本票脚本只按 code 幂等对齐这两列，不抢角色菜单授权 |
| Q8 | customizer 注册 4 表 user/dept 映射；系统侧调用（取号扫描/未发量聚合/生产进度/审批回写/财务过账/回收站/删除守卫）Service 方法显式 `@DataPermission(enable=false)`，租户条件照加；冻结"用户侧开启、系统侧关闭"契约 |
| Q9 | 带单继承只向前一次：报价转合同继承 owner（停用回退登录人），合同建发货带合同 owner；dept_id 均服务端推导；之后各自独立零回刷（客户/合同改归属不回刷下游），联系人实时继承客户 |
| Q10 | 每表两个部分索引 `(tenant_id, dept_id)`/`(tenant_id, salesperson_user_id) WHERE deleted=0`，受限档 BitmapOr，ALL 档零影响；5 表万行 P95<1s、EXPLAIN 无 seq scan |
| Q11 | 联系人列表裸 EXISTS 片段（JSqlParser 给子查询中 customer 表自动加规）；四入口统一"客户联系人不存在"零泄漏；未来导出复用同一过滤 mapper |
| Q12 | 单条写保持"先 selectById 校验再写"现状；批量删改为逐个可见性校验，任一不可见整体失败不部分成功；每表×每档含直连 id 写拦截+DB 零变更断言 |
| Q13 | 立 **ADR-0046**（联系人 EXISTS 继承 / 调岗同步刷列 / 自定义档收口三取舍） |
| Q14 | lunaerp 1-030-041 段**不占用**（不存在场景全复用现有码）；仅 system 角色段新增 `1_002_002_006` |
| Q15 | 零新字典（字典 id=35 留库）、零新菜单/按钮 |
| Q16 | 只回改 05/04 两份 spec 小段；02/10/13 以本票"系统侧调用契约"表为汇集点，不回改文件；16 号 spec 不动 |
| Q17 | 新增 `luna-srv/sql/postgresql/lunaerp-data-permission-wiring.sql`（DDL+索引+回填+角色对齐，install `20 商业化` 段 approval-engine 之后）；`_order.md` 插 21 位、其后顺延 |
| Q18 | 本票改现有 4 模块后端+2 处前端；报价只出 DDL/契约（05 票未建代码）；测试覆盖 4 档×5 表四入口、多角色并集、空归属、调岗、取号不受档影响；非目标见 §13 |

---

## 2. 范围与术语

**销售归属链 5 表**（本票唯一接通行级数据范围的对象）：

| 业务名 | 表名 | owner 列 | dept 列 | 代码现状 |
|---|---|---|---|---|
| 客户 | `lunaerp_baseinfo_customer` | `salesperson_user_id`（新 ALTER） | `dept_id`（新 ALTER） | 已上线 |
| 客户联系人 | `lunaerp_baseinfo_customer_contact` | — 不设列 — | — 不设列 — | 已上线，继承客户 |
| 报价单 | `lunaerp_business_quotation` | `salesperson_user_id`（05 票规划） | `dept_id`（本票补入 05 票 DDL） | 仅 spec |
| 销售合同 | `lunaerp_business_sales_contract` | `salesperson_user_id`（已有） | `dept_id`（新 ALTER） | 已上线 |
| 发货单 | `lunaerp_business_shipment` | `salesperson_user_id`（已有） | `dept_id`（新 ALTER） | 已上线 |

**数据范围四档**（角色 `system_role.data_scope`，沿用芋道编码）：`1 全部` / `4 本部门及以下` / `3 本部门` / `5 本人`；编码 `2 自定义部门` 一期停用（见 §8）。

**归属人（=归属业务员，列 `salesperson_user_id`）**：销售数据行的可见性锚点用户；**归属部门（列 `dept_id`）**：归属人落库时点的所属部门，服务端推导。术语同步进 shared GLOSSARY。

底座行为（已核源码，施工与测试以此为准）：

- 规则按**表名**注册 user/dept 列，受限档拼 `(dept_id IN (...) OR salesperson_user_id = 当前用户)`；多角色权限**取并集**，任一启用角色 ALL 即全部（`PermissionServiceImpl#getDeptDataPermission`）。
- 无登录用户不生效；用户无启用角色时底座给 `self=true`（只能看本人）；deptIds 与 self 皆空时拼 `null=null`（查不到任何行）。
- 拦截基于 MP `DataPermissionInterceptor`+JSqlParser，**SELECT/UPDATE/DELETE 均改写**，子查询/JOIN 中出现的注册表按别名各自追加条件；`@DataPermission(enable=false)` 经 AOP 在 Service 方法（Spring Bean）级压栈生效，Mapper 方法上不生效；easy-trans 翻译调用底座已自动豁免。
- 租户拦截器独立工作，关闭数据权限**不影响** tenant_id 追加。

---

## 3. 归属写入规则（默认值 / 推导 / 继承）

### 3.1 新建与编辑（客户 / 合同 / 发货；报价随 05 票同规则）

1. 新建时 `salesperson_user_id` 缺省 = 当前登录用户；前端归属业务员选择器可改（合同/发货现状已可改；**客户表单新增归属业务员选择器**；报价 05 票已规划）。
2. `dept_id` 一律以后端保存时"最终归属人"的 `system_users.dept_id` 推导落库：
   - create：owner 为空 → 两列皆空；owner 非空 → dept_id=其当前部门；
   - update：归属人变更 → dept_id 同事务重算（含清空归属人则两列清空）。
3. 前端不得直接提交 `dept_id`（VO 不暴露该字段；即使传入也忽略）。
4. 归属列**不设 NOT NULL、不设外键**（沿用全仓逻辑关联约定）；空归属行仅 ALL 档角色可见，部门档/本人档不可见。

### 3.2 带单继承（只向前一次，之后独立）

| 链路 | 归属处理 |
|---|---|
| 报价「生成销售合同」（05 票） | 草稿合同 owner 继承报价单 owner；owner 用户已停用 → 回退当前登录用户；dept_id 服务端推导（不继承快照值）；草稿上可改 owner，保存重算 dept_id |
| 合同建发货单 | owner 缺省带合同 owner（前端可改，现状不变）；dept_id 服务端推导 |
| 客户联系人 | 始终**实时继承**客户归属（无自己的列，见 §5） |

客户改归属不回刷其合同/发货/报价；合同改归属不回刷发货。各自独立，杜绝回写风暴。

### 3.3 跨归属引用

保存链路对引用实体的存在性校验现状不变（selectById 已被规则自动过滤）：

- 合同/报价引用不可见客户 → 现有 `CUSTOMER_NOT_EXISTS`（1_030_004_000）；
- 发货引用不可见合同 → 现有 `SHIPMENT_RELATED_REF_NOT_EXISTS`（1_030_016_003，"关联的销售合同不存在或不可用"）；
- 联系人新建引用不可见客户 → `CUSTOMER_CONTACT_NOT_EXISTS`（1_030_004_001，见 §5，防 id 探测）。
- 不新增任何错误码。

### 3.4 Excel 导入（04 票客户模板）

客户导入模板**无归属列**；导入行 owner=执行导入的登录用户、dept_id=其当前部门（服务端推导，与 §3.1 同路径）。已在 04 票 spec §3.2 客户模板补注。

---

## 4. DDL、索引与存量回填

单文件：`luna-srv/sql/postgresql/lunaerp-data-permission-wiring.sql`，全程幂等（`ADD COLUMN IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS` / 回填 UPDATE 全部 `IS NULL` 守卫只填空不覆盖），空库（10 段先建表）与存量库重跑均不报错不丢数据。

### 4.1 加列

| 表 | 新增列 |
|---|---|
| lunaerp_baseinfo_customer | `salesperson_user_id int8 NULL`、`dept_id int8 NULL` + COMMENT |
| lunaerp_business_sales_contract | `dept_id int8 NULL` + COMMENT |
| lunaerp_business_shipment | `dept_id int8 NULL` + COMMENT |

报价两表 DDL 尚不存在，由 05 票建表脚本按 §6 增量直接带列，本文件不含其 ALTER。

### 4.2 数据权限索引（6 个，部分索引）

```sql
CREATE INDEX IF NOT EXISTS idx_lb_customer_dp_dept
    ON lunaerp_baseinfo_customer (tenant_id, dept_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lb_customer_dp_user
    ON lunaerp_baseinfo_customer (tenant_id, salesperson_user_id) WHERE deleted = 0;
-- 合同 / 发货同型：
-- idx_lb_sales_contract_dp_dept / _dp_user
-- idx_lb_shipment_dp_dept / _dp_user
```

受限档谓词 `(dept_id IN (...) OR salesperson_user_id = ?)` 由 PG BitmapOr 合并两索引；ALL 档不追加谓词，现有查询计划不变；调岗切面批量更新走 `(tenant_id, salesperson_user_id)` 定位。

### 4.3 存量回填（只填空，可重跑）

定位口径（Q4），全部按 tenant 维度，禁止写死 id：

- **兜底用户**：每租户启用用户中优先 `tenant_admin` 角色者、否则最小 id 用户（CTE `DISTINCT ON (tenant_id) … ORDER BY (r.code='tenant_admin') DESC, u.id`）。
- **兜底部门**：该租户根部门（`system_dept.parent_id=0 AND deleted=0`，取最小 id），否则兜底用户的 dept_id。
- `creator` 为 varchar 用户 id 串，关联时 `creator ~ '^[0-9]+$' AND system_users.id = NULLIF(creator,'')::int8`，并带同租户条件。

分步（每步均 `目标列 IS NULL` 守卫）：

1. 客户 owner：creator 用户 → 兜底用户；
2. 客户 dept：owner 当前部门 → creator 用户部门 → 租户根部门；
3. 合同/发货 dept：已有 salesperson 当前部门 → creator 用户部门 → 兜底用户部门 → 租户根部门（两表各自 4 条 UPDATE，不动 salesperson_user_id）。

租户 1 的实际兜底结果即 admin（id=1）/根部门（id=100），但由子查询得出而非字面量。

### 4.4 角色数据范围对齐

- 存量：`common`（普通角色）data_scope `2→5`、清空 deptIds；`test-dp` `2→1`、清空 deptIds；
- 出厂基线（按 code，角色尚未 seed 时 UPDATE 影响 0 行；16 号默认数据 seed 与 15 号 H5 角色 seed 落地时直接按本值 INSERT）：

| data_scope | role code |
|---|---|
| 1 全部 | boss、finance、planner、purchaser、warehouse_manager、warehouse_keeper、inspector |
| 4 本部门及以下 | sales_manager |
| 5 本人 | salesperson、workshop_worker、workshop_inspector |

`data_scope_dept_ids` 对齐角色统一置 `''`。

---

## 5. 规则注册与系统侧忽略契约

### 5.1 注册（lunaerp 模块新增配置类）

仿 system 模块 `DataPermissionConfiguration`，在 lunaerp 模块新增一个 `@Configuration`，注册一个 `DeptDataPermissionRuleCustomizer` Bean：

| 表 | addDeptColumn | addUserColumn |
|---|---|---|
| CustomerDO | `dept_id` | `salesperson_user_id` |
| SalesContractDO | `dept_id` | `salesperson_user_id` |
| ShipmentDO | `dept_id` | `salesperson_user_id` |
| QuotationDO（05 票） | `dept_id` | `salesperson_user_id` |

联系人表、所有子表（quotation_item / contract_item / shipment_item / shipment_roll）**不注册**。

### 5.2 必须关闭数据权限的调用点（注解打在 Service 方法上）

**判定规则（冻结给后续所有模块）：凡系统侧读注册表——取号、聚合试算、引用守卫、审批回写、财务过账、回收站——一律 `@DataPermission(enable=false)`；凡用户侧请求——列表/详情/用户发起的写——一律保持开启。** 关闭数据权限不影响租户隔离。

| # | 调用点 | 文件/方法（现状定位） | 原因 |
|---|---|---|---|
| 1 | 合同编号取号扫描+手填查重 | `SalesContractServiceImpl#generateContractNo / generateContractNoWithRetry / validateContractNoUnique`（约 L410–478），底层两个 `…IncludingDeleted` @Select | ADR-0004 编号全租户全量（含软删）唯一，漏扫会重号 |
| 2 | 发货编号取号两方法 | `ShipmentServiceImpl` 中调用 `selectByShipmentNoIncludingDeleted / selectListByShipmentNoPrefixIncludingDeleted` 的方法 | 同上 |
| 3 | 合同未发量聚合 | `ShipmentServiceImpl`（[L321](file:///d:/work/luna/go0829/luna/luna-worktree-return/luna-srv/lunasrv-module-lunaerp/src/main/java/com/luna/lunasrv/module/lunaerp/service/business/shipment/ShipmentServiceImpl.java#L321) `selectListByContractIdAndStatus`） | 必须统计租户内全部 COMPLETED 发货，受限档会算出虚假未发量导致超发 |
| 4 | 生产进度只读聚合 | `ProductionProgressQueryServiceImpl#getProductionProgressPage`（候选合同 L145、发货 L521 等 selectListByContractIds） | 系统级跨单聚合视图（ADR-0034），显式标注锁定语义，不依赖 planner 恰为 ALL 档 |
| 5 | 审批引擎回写合同 | 02 票 ApprovalHandler 合同生效 execute | 授权由审批任务保证，引擎写业务表不受归属限制 |
| 6 | 财务过账读合同/发货 | 10 号 AR 事件监听、13 号退货冲减监听 | 过账为系统事务，账不能按登录人归属漏记 |
| 7 | 回收站 handler | 04 票 CUSTOMER/SALES_CONTRACT/SHIPMENT（及 05 票 QUOTATION）handler 的 deleted=1 查询与恢复引用校验 | 回收站管理视图（独立权限），规则只认 deleted=0 语义会漏行 |
| 8 | 删除引用计数守卫 | 客户删除查发货/合同计数、仓库删除查发货计数（`selectCountByCustomerId` 等）接线时 | 守卫必须全量计数，否则漏报引用导致误删 |

注：easy-trans 翻译已由底座 `DataPermissionRuleFactoryImpl#isTranslateCall()` 自动豁免；取号 @Select 显式列清单不含 dept_id 列无需改动（缺列映射 null，仅用于取号）。

### 5.3 联系人继承过滤

- **列表/分页**：`CustomerContactMapper.selectPage` 在现有 wrapper 上追加裸 EXISTS（无外部参数，注释放注入）：

```java
wrapper.exists("SELECT 1 FROM lunaerp_baseinfo_customer c "
        + "WHERE c.id = lunaerp_baseinfo_customer_contact.customer_id");
```

JSqlParser 解析子查询时对 `c`（注册表）自动追加数据权限与租户条件；contact 表未注册不被改写。

- **详情 / update / delete / 新建**：service 先取联系人（或直接用入参 customerId）→ `customerMapper.selectById(customerId)` 可见性校验（自动受规则过滤）→ 为空统一抛 `CUSTOMER_CONTACT_NOT_EXISTS`（1_030_004_001）。包括"客户真不存在"与"客户存在但不可见"两种情况，对外同一回答，杜绝逐 id 探测。
- **导出**：当前 5 表均无 export 端点；未来导出必须复用同一 selectPage/过滤 mapper，不得另开无过滤查询（与 16 号 §6.2"四入口一致"对齐）。

### 5.4 写操作与批量操作

- 单条 get/update/delete/状态动作（合同 update/delete、发货 update/audit/cancel/update-sign/delete、客户与联系人 update/delete）维持现有"先 selectById 校验存在再写"模式——受限用户在校验步即得 NOT_EXISTS，MP 对 UPDATE/DELETE 的改写为第二道保险。
- **客户批量删除** `deleteCustomerList`：现状 `selectBatchIds` 自动过滤会静默少删；改为逐个 id 可见性校验，任一不可见即整体抛 `CUSTOMER_NOT_EXISTS`，禁止部分成功。

---

## 6. 调岗归属同步切面（Q5/ADR-0046）

1. lunaerp 模块新增 `@Aspect`（bean 名切点，不反向依赖 system 模块内部类）：环绕 system 用户更新（`AdminUserServiceImpl#updateUser`，签名以施工时源码为准），比较更新前后 `dept_id`。
2. dept_id 未变化 → 直接放行。
3. 发生变化 → 在同一事务内、切面通过 `@DataPermission(enable=false)` 上下文（或 lunaerp 侧 Mapper 显式关闭方法）对 4 张注册表执行：

```sql
UPDATE <表> SET dept_id = :newDeptId
WHERE tenant_id = :tenantId AND salesperson_user_id = :userId AND deleted = 0
```

4. 效果对齐 16 号 §6.2 换岗用例：a 调 D2 后，D1 主管（本部门及以下）立即不可见 a 的旧单、D2 主管立即可见、a 本人 SELF 档始终见自己单；owner 列不变故 SELF 档不受调岗影响。
5. 迁移脚本附一条幂等"归属校正 SQL"（注释形态随脚本交付）：按 owner 当前部门全量重刷 dept_id，供排障/补刷使用（`IS NULL` 不适用此条，此条为显式校正工具，默认注释）。
6. 万级数据量下按 `(tenant_id, salesperson_user_id)` 索引更新为毫秒级；用户删除（停用不删）不在切面范围。

---

## 7. 前端改动（luna-ui，仅 2 处）

1. **角色数据范围弹窗** [RoleDataPermissionForm.vue](file:///d:/work/luna/go0829/luna/luna-worktree-return/luna-ui/src/views/system/role/RoleDataPermissionForm.vue)：下拉不再直接全量渲染字典，改为对 `getIntDictOptions(SYSTEM_DATA_SCOPE)` 结果过滤 `value !== 2`（白名单常量 `[1,4,3,5]`，顺序=全部/本部门及以下/本部门/本人）；部门树卡片现有 `v-if dataScope===DEPT_CUSTOM` 条件随之永不触发，可不删模板。
2. **客户表单归属业务员选择器**：客户新建/编辑页加"归属业务员"用户选择组件（缺省当前登录用户、可改、可清空），字段进 CustomerSaveReqVO；客户列表可按归属业务员筛选（非强制，施工时一并）。
3. 不新增菜单、按钮、页面；角色列表 dataScope 标签对 value=2 的文案在存量迁移后无数据可显，不做特殊处理。

---

## 8. 4 档收口：UI / API / 存量（Q6）

| 层 | 处理 |
|---|---|
| 字典 | `system_dict_data` id=35（value=2 指定部门）**保留不删**，底座升级兼容；前端过滤不可见 |
| 前端 | §7 第 1 项白名单过滤 |
| 后端 API | system 模块 `PermissionServiceImpl#assignRoleDataScope`（或 Controller 入参校验层）拒绝 dataScope=2：新增错误码 `1_002_002_006 ROLE_DATA_SCOPE_CUSTOM_DISABLED`，文案"数据范围【自定义部门】已停用，请选择全部/本部门及以下/本部门/本人"；枚举 `DataScopeEnum.DEPT_CUSTOM` 与计算分支代码**保留不删**（底座最小改动，且存量库可能瞬时存在 2） |
| 存量数据 | §4.4：common→5、test-dp→1 并清 `data_scope_dept_ids` |

---

## 9. 测试矩阵（JUnit + 手工，向 16 号 §6.2 看齐）

### 9.1 组织造数（与验收 §6.2 同构）

部门 D1（子部门 D1.1）、D2 平级；用户 m=D1 主管（sales_manager）、a=D1 业务员（salesperson）、c=D1.1 业务员、d=D2 业务员；另设一个 DEPT_ONLY 角色用户于 D1。4 张已存表每表造归属 a/c/d 的行各 ≥2（含不同状态），空归属行 ≥1。

### 9.2 必测用例

1. **四档×5 表可见性**：ALL（boss/finance）见全部；DEPT_AND_CHILD（m）见 a+c 不见 d；DEPT_ONLY 见 a 不见 c/d；SELF（a）只见本人；每表每档至少一条列表断言+一条直连 id 详情断言。报价表用例随 05 票施工补跑，用例文本本 spec 冻结。
2. **四入口一致性**：列表过滤、直连 id 详情、update/delete/状态动作、（未来）导出——不可见行统一返回该表 NOT_EXISTS 文案；写操作断言 DB 零变更。
3. **联系人继承**：a 只见本人客户名下联系人；直连他人联系人 id（列表/详情/改/删/新建五种调法）一律 `CUSTOMER_CONTACT_NOT_EXISTS`。
4. **批量删除**：ids 中混入不可见 id → 整体失败且零删除。
5. **换岗**：把 a 调到 D2 后重查——m 立即不见 a 的单、D2 主管立即可见、a 本人仍见（§6 切面）；同事务回滚造一次失败验证不半刷。
6. **多角色并集**：用户同时挂 SELF 角色与 DEPT_ONLY 角色 → 并集可见；挂一个 ALL 角色 → 全部。
7. **边界语义**：无角色用户仅见本人；空归属行仅 ALL 可见；归属人停用后历史行仍按列值可见；easy-trans 翻译不借道泄漏归属外字段。
8. **系统侧不受档影响**：salesperson 角色登录走"手填合同编号查重/发货取号/建发货单未发量/生产进度页（有权限时）"——取号扫描覆盖他人与软删行、未发量统计全量不超发。
9. **自定义档收口**：角色弹窗无"指定部门"选项；API 直调 assignRoleDataScope(dataScope=2) → 1_002_002_006；common/test-dp 迁移值正确。
10. **回填 SQL**：空库跑通；构造"creator 有效/creator 非数字/creator 用户已删"三类行各若干，重跑两次结果幂等；多租户各归各的 admin/根部门。本机无 PG 时按 16 号 §7.2 在验收库实证（双库重跑，不接受仅静态核查）。
11. **跨租户**：本票改动不削弱 tenant 隔离——系统侧关闭方法仍带 tenant_id 条件，专测断言一条（对齐 16 号 §6.3）。
12. **性能**：5 表各灌 1 万行（双租户 9000:1000、含 5% 软删，16 号 §2.2 口径），受限档与 ALL 档各连续 100 次 P95<1s；EXPLAIN 见 index/bitmap index scan，seq scan 即失败；结果纳入 16 号 perf 脚本清单 `luna-srv/script/acceptance/perf/`。

### 9.3 UI 手工

角色弹窗 4 选项无"指定部门"、部门树不出现；客户归属业务员选择器缺省/修改/清空；a/m/d 账号登录 5 表列表肉眼核对。

---

## 10. 错误码 / 字典 / 菜单

| 项 | 占用 |
|---|---|
| lunaerp 错误码 | **不新增**（1-030-041 段保持空闲；不存在/引用场景全复用 003/004/016 现有码） |
| system 错误码 | `1_002_002_006`（system 模块 `ErrorCodeConstants` 角色段，现有用到 005） |
| 字典 | 零新增、零删除（system_data_scope id=35 留库） |
| 菜单/按钮 | 零新增零改动 |
| ADR | ADR-0046 |
| GLOSSARY | shared：数据范围/归属人/归属部门/自定义部门档（停用） |

---

## 11. 施工顺序与文件清单

1. `lunaerp-data-permission-wiring.sql`：3 表 4 列 ALTER + 6 索引 + 回填 + 角色对齐；登记进 install `20 商业化` 段（approval-engine 之后）；
2. lunaerp：customizer 配置类（4 表映射）；
3. lunaerp：客户/合同/发货保存路径归属服务端推导（create/update），客户 VO 加 owner 字段；
4. lunaerp：§5.2 忽略注解（1–4 项现状代码立即标注，5–8 项在对应票施工契约中冻结）；
5. lunaerp：调岗切面；
6. lunaerp：联系人 EXISTS + 四入口校验 + 客户批量删除强校验；
7. system：assignRoleDataScope 拒绝 2 + 1_002_002_006；
8. luna-ui：角色弹窗过滤 + 客户表单选择器；
9. 测试：§9 矩阵 + perf 灌数入口。

报价表相关第 2/3/9 步内容随 05 票施工同批落地（05 票 DDL/继承小节已含本票增量）。

---

## 12. 对既有 spec 的增量（Q16）

- **05 票 cost-card-quotation**：报价主表加 `dept_id` 列与 2 个 dp 索引；转合同继承 owner、dept_id 服务端推导（已在其 spec 落注）。
- **04 票 common-foundation**：客户 Excel 模板无归属列、导入归属操作人（§3.2 已落注）；CUSTOMER/SALES_CONTRACT/SHIPMENT 回收站 handler 查询必须关闭数据权限（§2.5 已挂账）。
- **02/10/13 票**：不回改文件，以本 spec §5.2 第 5/6 项为契约，施工时照做。
- **16 号验收 spec**：不动；本票向其 §6.2 看齐并解阻 G2 DoD 总表 data-permission-wiring 行。

---

## 13. Out of Scope（一期不做）

- 销售归属链 5 表**以外**任何单据接通部门档（其余单据一期恒为"全部"，二期随本模式推广）；
- "自定义部门"档的一切功能（仅保留底座代码与字典行，不可选不可配）；字段级数据权限配置中心（Q9=A）；
- 公海/客户认领/线索池、归属批量分配、归属转交审批流、归属变更历史审计；
- 多租户角色模板自动下发（16 号 spec 已列非目标）；
- dept_id 历史多值/兼任部门可见性；用户离职（停用）后的归属自动交接；
- 报价/合同/发货之外的打印、门户、H5 侧数据范围（门户=客户维度令牌另成体系，H5=本人硬过滤，均见各自 ADR）。

---

## 统一 DoD 对照（本 spec 自身）

- 状态机/并发：无新状态机；调岗切面同事务、批量更新按 `(tenant_id, salesperson_user_id)` 走索引；归属推导在既有写事务内完成
- 芋道权限+脱敏：本票即行级数据范围本体；归属人选择器/列表遵循现有 @PreAuthorize；脱敏口径不变
- 回收站：不新增 handler；在 04 票 handler 契约上补"关闭数据权限"硬要求（§5.2 第 7 项）
- 打印+日志：无打印；拒绝自定义档 API 调用走全局异常日志；调岗刷列不另建审计（非目标）
- 核心流转/副作用测试：§9 四档×5 表四入口、换岗、并集、回填幂等、取号不受档影响
- 前端验收清单+波次冒烟：§9.3；W0 后出口并入 16 号 G2/G3
- 术语与错误码：shared GLOSSARY 四术语；system 1_002_002_006；lunaerp 段不新增（§10）
- P95：6 个 dp 部分索引 + 万行 EXPLAIN 实证（§9.2 第 12 项）
- 租户隔离/横纵向越权：§9.2 第 11 项 + 16 号 §6.3 全域矩阵
