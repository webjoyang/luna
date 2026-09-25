# 生产管理 - 质检确认 MVP（卷级责任认定 + 撤销留痕链 + 手工登记 + 打印 + 扣款汇总）

Status: ready-for-agent

## Problem Statement

质检员逐卷录检后会产生二等品（B）、三等品（C）和等外品（DEFECTIVE）卷。车间领班需要在下班前对这些降等/次品卷**逐卷认定质量责任**：是设备故障（免扣）、挡车工操作失误（要扣挡车工当日工资），还是免扣不追责；人事/财务再依据领班的认定结果核算当日工资扣款。认定结果是扣款凭据，允许改判，但改判过程必须留痕可查。

旧系统（linaagi，Prisma + Next.js/React）已有这条业务链：检验结果经"同步入库"服务写入入库明细的卷状态（待复检/不合格）后，同步钩子按卷自动生成待确认记录；领班三键认定责任；撤销时旧记录冻结、克隆一条新待确认；文员可手工代录（含跨天补录）；另有按发现日的批量打印签字单、按确认日的工资扣款汇总。

旧实现无法照搬到新系统，三个前置条件在新系统都不成立：

1. 新系统入库明细没有卷级质量字段（无卷状态/疵点/机台），且"检验同步入库"经 ADR-0013 明确延后，旧钩子没有挂载点；
2. 新系统没有织机/机台任务主数据，旧系统"机台号 → 当日机台任务 → 挡车工"的自动反查链不存在；
3. 新系统没有工资模块，工资扣款无法直接写入工资单。

但新系统已具备更直接的卷级质量数据源：检验单录检明细（ADR-0014）逐卷记录了卷号/缸号/等级/机台/重量/米数/产品快照。业务侧希望在 luna-srv（Spring Boot + MyBatis-Plus + PostgreSQL）与 luna-ui（Vue3 + Element Plus，lunaerp-elmui 先行版目录，ADR-0001）中，用新栈把"生产管理-质检确认"的最小可用闭环跑通。

## Solution

在 luna-srv 的 `lunasrv-module-lunaerp` 新增 `production.inspectionconfirmation` 子包（单包单实体：认定链上的每一行——待确认/已认定/已撤销——都是同一张表的一行，不是主从结构），落地一张质检确认表与 9 个端点；在 luna-ui 的 `views/lunaerp-elmui/production/inspection-confirmation/` 落地一个菜单页（列表 + 页内弹窗：责任认定、手工登记、按日打印、工资扣款汇总、认定链详情），并在检验单工作台加"生成质检确认"按钮。配套一份 PostgreSQL 脚本完成 DDL、两个字典（状态/责任类型）、system 域"挡车工"岗位 seed、10270 菜单与 10271-10275 按钮权限。

核心业务规则：

- **来源**：检验单工作台显式触发生成（扫描单内 B/C/DEFECTIVE 卷，幂等——同一录检卷只存在一条有效确认行，重复点击只补新卷），或文员手工登记（不挂检验单，提交即已认定，可补录认定日期）。详见 ADR-0019。
- **认定**：三选一 EQUIPMENT 设备故障（免扣）/ OPERATOR 操作失误（扣款，挡车工必选）/ WAIVED 免扣；认定人取当前登录用户，记录认定时间与原因备注。
- **改判**：撤销 = 旧行冻结为 REVOKED（字段全部保留）+ 按同一认定链克隆一条新 PENDING；所有记录一律不可删除，误生成的待确认只能以免扣（WAIVED）认定结案。详见 ADR-0020。
- **挡车工**：复用 system 域——`system_post` seed"挡车工"岗位，挡车工即挂该岗位的 `system_users` 用户，存 userId + 姓名快照，不新建员工表。详见 ADR-0021。
- **打印/汇总双日期口径**：打印签字单按发现日（记录创建日），工资扣款汇总按确认日（认定日）；汇总为只读查询，不写工资系统、不做已同步标记。

## User Stories

### 列表与检索

1. 作为领班，我想在"生产管理 → 质检确认"菜单看到确认记录列表（检验单号、卷号、缸号、等级、产品名称、机台号、来源、责任类型、挡车工、状态、认定人、认定时间、创建时间），以便逐卷处理责任认定。
2. 作为领班，我想列表默认只显示有效行（待确认 + 已认定），已撤销的冻结行不出现在主列表，以便日常工作台不被历史留痕干扰。
3. 作为领班，我想按状态筛选（待确认/已认定/已撤销），选"已撤销"时可以查看冻结留痕行，以便审计改判历史。
4. 作为领班，我想按责任类型筛选（设备故障/操作失误/免扣），以便只看待扣款的操作失误卷。
5. 作为领班，我想按来源筛选（检验单引入/手工登记），以便区分系统生成与文员代录。
6. 作为领班，我想按挡车工筛选，以便查看某挡车工名下的全部责任卷。
7. 作为领班，我想按检验单筛选，以便回看某张检验单生成了哪些确认记录。
8. 作为领班，我想在关键词框输入词同时匹配卷号、缸号、产品名称、检验单号，以便只记其一也能定位记录。
9. 作为领班，我想按发现日（创建时间）区间筛选并分页、默认按创建时间倒序，以便聚焦最新待办。
10. 作为领班，我想状态列与责任类型列用 DictTag 渲染、来源列用标签区分，以便一眼识别记录性质。
11. 作为领班/审计，我想点某行打开认定链详情，看到同一卷的完整认定链（当前有效行 + 历次已撤销冻结行，含每轮的责任类型/挡车工/认定人/认定时间/撤销人/撤销时间/备注），以便追溯改判经过。

### 从检验单生成

12. 作为质检员/领班，我想在检验单工作台点"生成质检确认"，系统扫描该单录检明细中等级为 B/C/DEFECTIVE 的卷并逐卷生成待确认记录，以便不用手工逐卷建档。
13. 作为质检员/领班，我想等级为 A（一等品）的卷不生成确认记录，以便只有需要追责的卷进入待办。
14. 作为质检员/领班，我想重复点"生成质检确认"是幂等的——已经生成过有效确认行的卷自动跳过，只补新增的非一等品卷，点击后提示"新增 X 卷 / 跳过 Y 卷"，以便录检过程中可以随时安全补生成。
15. 作为质检员/领班，我想撤销后克隆产生的待确认行不会导致该卷被重复生成（幂等判定以"是否存在有效行"为准），以便一条卷永远只有一条在办记录。
16. 作为质检员/领班，我想只有录检中（ACTIVE）/录检完成（COMPLETED）的检验单能生成，草稿/已取消的检验单点击会被拒绝并得到明确提示，以便不对未定稿数据追责。
17. 作为质检员/领班，我想检验单不存在时生成操作报错，以便拦截非法请求。
18. 作为质检员/领班，我想生成的待确认行自动快照卷号、缸号、等级、机台号、产品编码/名称、颜色/色号/款号、重量、米数、检验单 ID/单号、录检明细 ID，以便列表与认定时无需回查检验单，且检验单以后改动不影响认定依据。
19. 作为质检员/领班，我想生成后即使检验单或其录检明细事后被改等级、改卷号、删除，确认记录也不回写、不级联变化，以便认定凭据稳定。
20. 作为质检员/领班，我想"生成质检确认"按钮只对拥有质检确认 `:generate` 权限的人可见可点（它创建的是质检确认数据，不要求检验单编辑权限），以便权责分开。

### 责任认定

21. 作为领班，我想对待确认行执行认定：选择设备故障/操作失误/免扣三者之一、可填原因备注（如"氨纶灯不亮"），提交后该行变为已认定并记录我（当前登录用户）与认定时间。
22. 作为领班，我想认定对话框中只读展示该卷的快照信息（卷号/缸号/等级/机台/产品/重量/米数/来源检验单），以便核对认定对象。
23. 作为领班，我想选择"操作失误"时必须从挡车工下拉中选择具体挡车工，否则不能提交，以便扣款主体明确。
24. 作为领班，我想挡车工下拉只列出岗位为"挡车工"的系统用户（id + 姓名），以便不会误选行政/财务人员。
25. 作为领班，我想选择"设备故障"或"免扣"时挡车工可留空，以便免扣场景不强迫指认无关人员。
26. 作为领班，我想选定挡车工后记录其姓名快照，以后该用户改名不影响历史记录展示。
27. 作为领班，我想已认定的行不能再次认定，系统提示须先撤销，以便认定结果不可被静默覆盖。
28. 作为领班，我想已撤销的冻结行不能被认定，以便只能对撤销后克隆出的新待确认行操作。
29. 作为领班，我想传入非法责任类型时请求被拒绝并报错，以便数据不被污染。

### 撤销与留痕

30. 作为领班，我想对已认定行执行"撤销重录"：原行被标记为已撤销且责任/挡车工/认定人/认定时间/备注全部原样保留，同时同卷出现一条新的待确认行，以便改判后重新认定且两轮凭据都在。
31. 作为领班，我想撤销操作记录撤销人（当前登录用户）与撤销时间，以便知道谁在何时改判。
32. 作为领班，我想新克隆的待确认行沿用原行的卷快照与挡车工（责任/认定字段重置为空），并与原行挂在同一认定链上，以便重新认定时少填重复信息、链条可串联。
33. 作为领班，我想待确认行不能被撤销（尚未认定无需撤销）、已撤销行不能重复撤销，系统给出明确错误。
34. 作为领班/审计，我想撤销前有二次确认提示（告知原记录留痕、会产生新待确认），以便避免误触。
35. 作为任何角色，我想系统不提供删除质检确认记录的任何入口（待确认/已认定/已撤销均不可删），以便扣款凭据链完整；录检误判导致的误生成待确认，我可以用"免扣"认定结案使其不再进扣款汇总。

### 手工登记（文员代录）

36. 作为文员，我想点"手工登记"打开表单，不依赖检验单直接登记一卷的责任认定：卷号（必填）、等级（必填，A/B/C/DEFECTIVE）、责任类型（必填）、挡车工（操作失误时必填）、缸号/机台号/产品/颜色/重量/米数/备注（选填），以便处理漏录或线下补录。
37. 作为文员，我想手工登记可以补录认定日期时间（缺省当前时间），认定人记为当前登录用户，以便跨天补录昨天的认定。
38. 作为文员，我想手工登记提交后该行直接是"已认定"状态（无需再到列表走一次认定），来源标记为手工登记，以便一步完成代录。
39. 作为文员，我想手工登记也可选择性关联一张检验单（仅做存在性校验与单号快照，不校验该卷是否在单内），以便补录数据尽量可溯源。
40. 作为文员，我想手工登记缺必填项（卷号/等级/责任类型，或操作失误时缺挡车工）时表单与后端双重拦截，以便脏数据进不来。
41. 作为文员，我想手工登记产生的已认定行同样可以撤销重录，撤销克隆规则与检验单引入行完全一致。

### 批量打印签字单

42. 作为领班，我想点"批量打印"选择日期（缺省今天），查看该**发现日**（记录创建日）的全部确认记录（待确认/已认定/已撤销都列出并带状态标记），以便打印纸质单据供线下签字。
43. 作为领班，我想打印单上每条含检验单号/卷号/缸号/等级/产品/机台/挡车工/责任类型/备注/认定人/认定时间，并在页面显著位置标注口径为"按发现日"，以便签字现场信息完整、不与扣款口径混淆。
44. 作为领班，我想在打印弹窗中用浏览器打印即可完成纸质输出，无需导出文件。

### 工资扣款汇总（只读）

45. 作为人事，我想点"工资扣款汇总"选择日期（缺省今天），看到该**确认日**（认定时间所在日）所有已认定、责任为操作失误、未撤销的记录，按挡车工聚合成"挡车工 + 扣款卷数 + 卷明细列表"，以便对照录入当日工资扣款。
46. 作为人事，我想设备故障与免扣的记录不进汇总、已撤销冻结行不进汇总，以便只看到真正扣款的数据。
47. 作为人事，我想汇总中每个挡车工可展开查看卷明细（卷号/缸号/等级/检验单号/认定时间/备注），以便核对每一笔扣款的凭据。
48. 作为人事，我想汇总只是只读查询，系统不提供"生成工资初稿/已同步"标记、不写任何工资数据，以便本期在没有工资模块的情况下不制造虚假闭环。

### 管理与分权

49. 作为系统管理员，我想执行一份可重复执行的 SQL 脚本即可完成建表、两个字典（含字典项）、"挡车工"岗位 seed、菜单与按钮权限初始化，以便部署无手工配置。
50. 作为系统管理员，我想质检确认有六个独立权限码：query/create/generate/confirm/print/payroll，以便领班只授 query+generate+confirm+print、人事只授 query+payroll、文员授 query+create。
51. 作为系统管理员，我想质检确认状态字典为 `lunaerp_inspection_confirmation_status`（PENDING/CONFIRMED/REVOKED）、责任类型字典为 `lunaerp_inspection_responsibility`（EQUIPMENT/OPERATOR/WAIVED），以便业务方后续维护枚举展示。
52. 作为开发，我想确认记录 DO 继承 TenantBaseDO，多租户隔离由框架自动处理。
53. 作为开发，我想对检验单/录检明细/挡车工用户的关联全部为逻辑关联（不建数据库外键）并保存名称快照，以便对方数据变更不影响本域、列表无需跨模块 JOIN。
54. 作为开发，我想挡车工用户校验与候选复用 system 域现成 API（AdminUserApi：按岗位取用户、校验用户有效性；PostApi：按岗位编码定位岗位），跨模块只依赖 API 层不直接访问 system 的 Mapper。

## Implementation Decisions

### 模块与包路径

- 后端模块：`lunasrv-module-lunaerp`（已存在，新增子包）
- 后端包（单包单实体，无 item 子包）：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.production.inspectionconfirmation`
- 跨模块依赖：lunaerp 已有/新增对 `lunasrv-module-system` API 层的依赖，使用 `AdminUserApi`（getUserListByPostIds / getUser / validateUser）与 `PostApi`；不直接访问 system 模块内部 Service/Mapper。**PostApi 需做一处纯增量扩展**：现有 PostApi 只有 getPostList(ids)/validPostList，无按编码查询；本期新增 `PostRespDTO getPostByCode(String code)`（PostApiImpl 经 PostService 新增同名透传方法，底层 PostMapper.selectByCode 已存在，无需新增 SQL），用于按岗位编码 LUNA_WORKER 解析"挡车工"岗位。不改动 PostApi 既有方法签名。
- 前端视图：`luna-ui/src/views/lunaerp-elmui/production/inspection-confirmation/`（单列表页 + 页内弹窗组件）
- 前端 API：`luna-ui/src/api/lunaerp-elmui/production/inspection-confirmation/index.ts`
- 前端检验单工作台改动点：现有 `views/lunaerp-elmui/production/inspection-order/` 工作台内加"生成质检确认"按钮（仅 ACTIVE/COMPLETED 态可见，受 `:generate` 权限控制）
- 沿用 ADR-0001：落在 `lunaerp-elmui/` 先行快速版目录

### 数据模型

**质检确认表 `lunaerp_production_inspection_confirmation`**（继承 TenantBaseDO：id/creator/create_time/updater/update_time/deleted/tenant_id）：

| 分组 | 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|------|
| 认定链 | chain_key | varchar(48) | ✓ | 认定链标识：首行由服务端用 ID 生成器预生成；撤销克隆沿用首行值；同链多行同值。普通索引，不建唯一约束 |
| 来源 | source | varchar(16) | ✓ | INSPECTION 检验单引入 / MANUAL 手工登记（后端枚举，不入字典） |
| 状态 | status | varchar(16) | ✓ | PENDING/CONFIRMED/REVOKED，字典 2236；生成与克隆缺省 PENDING，手工登记直接 CONFIRMED |
| 来源检验单 | inspection_order_id | int8 | - | 逻辑关联检验单，不建外键；MANUAL 可空 |
| | inspection_order_code | varchar(32) | - | 检验单号快照 |
| 来源录检明细 | inspection_order_item_id | int8 | - | INSPECTION 必填；MANUAL 为空 |
| 卷快照 | roll_no | varchar(64) | ✓ | 卷号（MANUAL 亦必填） |
| | vat_no | varchar(64) | - | 缸号（INSPECTION 从明细快照；MANUAL 可空） |
| | grade | varchar(16) | ✓ | 等级快照，取值复用字典 lunaerp_inspection_grade |
| | machine_no | varchar(64) | - | 机台号快照 |
| | product_code | varchar(64) | - | 产品编码快照 |
| | product_name | varchar(128) | - | 产品名称快照 |
| | color_name | varchar(128) | - | 颜色名称快照 |
| | color_no | varchar(64) | - | 色号快照 |
| | style_no | varchar(64) | - | 款号快照 |
| | measured_weight | numeric(18,2) | - | 实称重量 KG 快照 |
| | length_m | numeric(18,2) | - | 米数快照 |
| 认定 | responsibility | varchar(16) | - | EQUIPMENT/OPERATOR/WAIVED，字典 2237；PENDING 行为空 |
| 挡车工 | worker_user_id | int8 | - | 逻辑关联 system_users，不建外键；OPERATOR 必填 |
| | worker_name | varchar(64) | - | 挡车工昵称快照 |
| | confirmed_by_user_id | int8 | - | 认定人（当前登录用户） |
| | confirmed_by_name | varchar(64) | - | 认定人姓名快照 |
| | confirmed_at | timestamp | - | 认定时间；手工登记可补录（仅日期时间，不允许未来时间之外的额外限制） |
| | note | varchar(1000) | - | 原因备注 |
| 撤销留痕 | revoked_by_user_id | int8 | - | 撤销人 |
| | revoked_by_name | varchar(64) | - | 撤销人姓名快照 |
| | revoked_at | timestamp | - | 撤销时间；非空即 REVOKED 冻结行 |

索引（均为普通 B-Tree 索引）：`(tenant_id, status)`、`(tenant_id, chain_key)`、`(tenant_id, inspection_order_id, inspection_order_item_id)`（幂等查重）、`(tenant_id, created_at)`（打印/列表）、`(tenant_id, confirmed_at)`（扣款汇总）。

**无业务编号**（ADR-0009 先例）：卷级流水记录靠"检验单号+卷号+认定链"识别。**不提供任何删除端点**（ADR-0020）。

### 幂等生成规则（ADR-0019）

入口：`POST /generate-from-order`，入参 inspectionOrderId。

1. 校验检验单存在（不存在报 `1_030_014_002`）；状态必须为 ACTIVE/COMPLETED（否则报 `1_030_014_003`）。
2. 取该单全部未软删录检明细，过滤 grade ∈ {B, C, DEFECTIVE}（grade 为空或 A 不处理）。
3. 查本表该检验单下**已有有效行**（status ∈ {PENDING, CONFIRMED}）的 inspection_order_item_id 集合；在集合内的卷跳过，其余逐卷插入 PENDING 行，快照字段从录检明细复制，inspection_order_code 从检验单复制。
4. chain_key 对每行用 ID 生成器预生成（与主键同源），首行即链根。
5. 返回 `{ generatedCount, skippedCount }`，整体一个事务；跳过不报错（故无"重复生成"错误码）。
6. REVOKED 行不阻挡也不计入有效集合——撤销后克隆的 PENDING 已在有效集合中，幂等仍成立。

### 认定/撤销/手工登记规则

**认定** `POST /confirm`（入参 id / responsibility / workerUserId? / note?）：

- 行不存在报 `1_030_014_000`；status ≠ PENDING 报 `1_030_014_001`（含已认定须先撤销、已撤销不可认定两种语义）。
- responsibility 必须 ∈ 三值，否则报 `1_030_014_004`。
- responsibility=OPERATOR 时 workerUserId 必填（`1_030_014_005`）；经 AdminUserApi.validateUser 校验有效，取昵称写 worker_name 快照；其余两值若误传 workerUserId 可忽略或按空处理。
- 写 confirmed_by_user_id/name（当前登录用户，经 AdminUserApi 取昵称）、confirmed_at=服务端当前时间、note、status=CONFIRMED。confirmed_at 不由请求方传入（补录走手工登记入口）。

**撤销** `POST /revoke`（入参 id）：

- 行不存在报 `1_030_014_000`；status ≠ CONFIRMED 报 `1_030_014_001`（待确认无需撤销/已撤销不可重复撤销）。
- 同事务两步：① 旧行置 status=REVOKED，写 revoked_by_user_id/name、revoked_at，**其余所有字段原样不动**；② 插入新行：chain_key 沿用、source/来源关联/全部卷快照/worker_user_id/worker_name 沿用，status=PENDING，responsibility/confirmed_*/note/revoked_* 全部置空，新行 id 与 chain_key 不同键。
- 返回 { revokedId, freshId }。

**手工登记** `POST /manual-create`（入参见用户故事 36-39）：

- 必填：rollNo、grade、responsibility；responsibility=OPERATOR 时 workerUserId 必填；缺项报 `1_030_014_006`。
- 可选：vatNo/machineNo/productCode/productName/colorName/colorNo/styleNo/measuredWeight/lengthM/note、confirmedAt（缺省服务端当前时间，允许传过去时间补录）、inspectionOrderId（传则校验存在并带 code 快照，不存在报 `1_030_014_002`，不校验卷是否在单内）。
- 写入：source=MANUAL、status=CONFIRMED、新 chain_key、confirmed_by=当前登录用户、挡车工校验与快照规则同认定。
- 手工登记行的撤销走同一 `/revoke`，克隆出新 PENDING 行（source 仍为 MANUAL）。

### Controller 与 API 契约

基础路径 `/lunaerp/production/inspection-confirmation`，统一 CommonResult / PageResult，VO 带 Swagger 与 jakarta validation 注解，风格对齐 inspectionorder / stockin 模块：

- `GET /page` — pageNo/pageSize；筛选：status（PENDING/CONFIRMED/REVOKED；**缺省为 PENDING+CONFIRMED 两态**，显式传 REVOKED 才看冻结行）、source、responsibility、workerUserId、inspectionOrderId、keyword（对 roll_no/vat_no/product_name/inspection_order_code OR 模糊）、createTime 区间（发现日）；默认 create_time DESC，分页
- `GET /get?id=` — 记录详情；响应在记录字段之外附带 `chainRecords`（同 chain_key 全部行，按创建时间正序，含 REVOKED 冻结行）
- `POST /generate-from-order` — 见幂等生成规则；返回 { generatedCount, skippedCount }
- `POST /confirm` — 见认定规则；返回 id
- `POST /revoke` — 见撤销规则；返回 { revokedId, freshId }
- `POST /manual-create` — 见手工登记规则；返回 id
- `GET /print-view?date=YYYY-MM-DD`（缺省今天）— 该日 create_time 落在 [当日00:00, 次日00:00) 的**全部状态**记录（含 REVOKED，带状态字段供前端标记），按创建时间正序
- `GET /payroll-summary?date=YYYY-MM-DD`（缺省今天）— 见下
- `GET /worker-options` — 挡车工候选：经 PostApi 按岗位编码取"挡车工"岗位 → AdminUserApi.getUserListByPostIds，返回 `[{ id, nickname }]`；岗位未 seed/无用户时返回空数组（不报错）

权限注解 action ∈ {query, create, generate, confirm, print, payroll}：generate-from-order 用 `:generate`；manual-create 用 `:create`；confirm/revoke 用 `:confirm`；print-view 用 `:print`；payroll-summary 用 `:payroll`；worker-options 用 `:query`；page/get 用 `:query`。

**工资扣款汇总响应结构**（按确认日 confirmed_at 当日范围、status=CONFIRMED、responsibility=OPERATOR；已撤销行天然不满足 status 条件被排除；按 worker_user_id 聚合——OPERATOR 行挡车工必填，故不存在未分配桶）：

```
{
  "date": "2026-09-25",
  "groups": [
    {
      "workerUserId": 1024,
      "workerName": "李桂香",
      "count": 3,
      "records": [
        { "id": 8801, "rollNo": "P0309", "vatNo": "V12", "grade": "C",
          "inspectionOrderCode": "INS-20260925-001", "note": "断纱",
          "source": "INSPECTION", "confirmedAt": "2026-09-25 17:40:12" }
      ]
    }
  ]
}
```

### 权限码与菜单

权限码前缀 `lunaerp:production:inspection-confirmation:`，六个动作：query / create / generate / confirm / print / payroll。

SQL 菜单（10270 段，可重复执行，先 DELETE 再 INSERT，父菜单 10200 生产管理已存在；10280 织造段在其他脚本中占用，与本段不冲突）：

- 10270 `质检确认` 菜单（type=2，parent=10200，path `inspection-confirmation`，component `lunaerp-elmui/production/inspection-confirmation/index`，componentName `LunaerpElmuiProductionInspectionConfirmation`，承载 query）
- 10271 `手工登记` 按钮（:create，type=3）
- 10272 `生成质检确认` 按钮（:generate，type=3；按钮同时出现在检验单工作台，前端按权限码控制）
- 10273 `责任认定` 按钮（:confirm，覆盖认定与撤销）
- 10274 `批量打印` 按钮（:print）
- 10275 `工资扣款汇总` 按钮（:payroll）

### 字典与岗位 seed（2 个字典 + 1 个岗位）

- `lunaerp_inspection_confirmation_status`（dict_type id=2236）：PENDING 待确认(warning) / CONFIRMED 已认定(success) / REVOKED 已撤销(info)
- `lunaerp_inspection_responsibility`（dict_type id=2237）：EQUIPMENT 设备故障（免扣）(info) / OPERATOR 操作失误（扣款）(danger) / WAIVED 免扣(success)
- 等级快照值直接复用 `lunaerp_inspection_grade`（id=2232，已存在），不另建；source（INSPECTION/MANUAL）为后端枚举不入字典
- `system_post` seed：岗位编码 `LUNA_WORKER`、名称"挡车工"、启用状态；脚本按 code 幂等（存在则跳过/更新名称），不固定 id（服务层按 code 解析）

### 错误码（1-030-014 段，在 lunaerp ErrorCodeConstants 追加）

- `1_030_014_000` — INSPECTION_CONFIRMATION_NOT_EXISTS（质检确认记录不存在）
- `1_030_014_001` — INSPECTION_CONFIRMATION_STATUS_NOT_ALLOWED（当前状态不允许该操作）
- `1_030_014_002` — INSPECTION_CONFIRMATION_ORDER_NOT_EXISTS（检验单不存在）
- `1_030_014_003` — INSPECTION_CONFIRMATION_ORDER_STATUS_NOT_ALLOWED（当前检验单状态不允许生成质检确认）
- `1_030_014_004` — INSPECTION_CONFIRMATION_RESPONSIBILITY_INVALID（责任类型不合法）
- `1_030_014_005` — INSPECTION_CONFIRMATION_WORKER_REQUIRED（操作失误必须指定挡车工）
- `1_030_014_006` — INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS（手工登记必填项缺失：{}）

### 前端页面结构与交互

- **列表页**（index.vue，对齐 elmui 各模块列表范式）：
  - 搜索区：关键词 + 状态下拉（缺省"待确认+已认定"）+ 责任类型 + 来源 + 挡车工下拉（复用 worker-options）+ 检验单远程选择 + 发现日区间
  - 顶部操作：手工登记（:create）、批量打印（:print）、工资扣款汇总（:payroll）
  - 表格列：检验单号/卷号/缸号/等级(DictTag)/产品名称/机台号/来源标签/责任类型(DictTag)/挡车工/状态(DictTag)/认定人/认定时间/创建时间/操作
  - 待确认行操作：[认定]（打开认定对话框，三键选责任 + 挡车工下拉 + 备注）；已认定行操作：[查看链][撤销重录]（二次确认）；已撤销行：[查看链]
- **认定链详情抽屉**：头部当前有效行摘要 + 时间线列出同链全部行（含每轮责任/挡车工/认定人/认定时间/撤销人/撤销时间/备注，REVOKED 行置灰）
- **手工登记对话框**：卷号*、缸号、等级*（字典下拉）、责任类型*（三选一单选）、挡车工（el-select 消费 worker-options，仅 OPERATOR 时必填、其余隐藏/禁用）、机台号、产品编码/名称、颜色/色号/款号、重量、米数、关联检验单（远程搜索可选）、备注、认定时间（日期时间选择，缺省现在，限不晚于当前时间）
- **打印弹窗**：日期选择 + 查询 + 只读表格（按发现日，含全部状态标记）+ 打印按钮（打印样式作用域内简易排版，window.print）
- **工资扣款汇总弹窗**：日期选择 + 查询 + 按挡车工分组表格（el-table 展开行显示卷明细），只读
- **检验单工作台**：在"录检明细"Tab 工具栏（与同步入库占位按钮同区）放"生成质检确认"按钮，受 `lunaerp:production:inspection-confirmation:generate` 控制、仅 ACTIVE/COMPLETED 态可点；成功后 ElMessageBox 提示"新增 X 卷 / 跳过 Y 卷"
- 前端 API 与 TS 类型以本 spec 契约为唯一事实来源，表格 prop 与请求参数名对齐后端 VO 字段名（camelCase），不另造命名
- 挡车工下拉使用普通 el-select 消费 worker-options，**不改造** system 共享组件 UserSelect.vue

### SQL 脚本

文件：`luna-srv/sql/postgresql/lunaerp-production-inspection-confirmation.sql`，参照 `lunaerp-production-inspection-order.sql` / `lunaerp-stock.sql` 体例：

1. 表 DDL（CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 列注释 + 序列 + 5 个普通索引）
2. system_dict_type（2236 状态、2237 责任）+ system_dict_data（状态 3 项、责任 3 项）
3. system_post：挡车工岗位（code=LUNA_WORKER）幂等 seed
4. system_menu：10270 菜单 + 10271-10275 按钮（事务内先 DELETE 再 INSERT）

## Testing Decisions

### 测试哲学

只测外部业务行为（Service 的可观察语义：返回值、落库对象的字段、错误码），不断言 Mapper 调用次数等实现细节；测试命名描述行为而非方法。前端不做测试（lunaerp-elmui 先行快速版，ADR-0001，与检验单/入库单 spec 一致）。

### 测试 seam（唯一接缝）

**Service 层纯 Mockito 单元测试**，单个测试类 `InspectionConfirmationServiceImplTest`。不启动 Spring 上下文、不连数据库、不新建 MockMvc/嵌入式 PostgreSQL 基建。

Mock 依赖：本域 Mapper、检验单主表 Mapper、检验单录检明细 Mapper、`AdminUserApi`、`PostApi`、登录用户上下文（取当前用户 id 的既有机制）、ID 生成器（chainKey/新 id 用确定性桩值）。

Prior art：`InspectionOrderServiceImplTest`（同域关联校验/状态守卫）、`StockInServiceImplTest`（严格状态守卫与同事务双写：撤销冻结+克隆的模式与审核+写流水同构）、`SupplierServiceImplTest`（快照写入与跨模块校验模式）。

### 覆盖的行为

1. **生成**：B/C/DEFECTIVE 卷各生成一条 PENDING 且快照字段完整（检验单 code、明细卷字段逐项映射）；A 与无等级卷跳过；同单重复调用，已有有效行对应卷跳过、新增卷生成，返回计数正确；撤销克隆产生的新 PENDING 使该卷仍被跳过；检验单不存在报 002；DRAFT/CANCELLED 报 003；ACTIVE/COMPLETED 放行。
2. **认定**：PENDING + EQUIPMENT/WAIVED 无挡车工可认定成功并写认定人/时间；OPERATOR 无挡车工报 005；OPERATOR 有挡车工时调用 AdminUserApi 校验并写入姓名快照；CONFIRMED 行再认定报 001；REVOKED 行认定报 001；非法 responsibility 报 004；行不存在报 000。
3. **撤销**：CONFIRMED 撤销后旧行为 REVOKED 且原责任/挡车工/认定字段未被清空、撤销人时间写入；同事务克隆出新 PENDING（chain_key 相同、卷快照与挡车工沿用、认定字段为空）；PENDING 撤销报 001；REVOKED 重复撤销报 001。
4. **手工登记**：缺卷号/等级/责任报 006；OPERATOR 缺挡车工报 005；成功落 CONFIRMED + MANUAL + 新 chain_key + 认定人为当前用户；confirmedAt 缺省为当前时间、传入补录时间被采用；传 inspectionOrderId 不存在报 002。
5. **打印视图**：按 create_time 当日范围返回全部状态行（构造跨日数据验证边界），顺序为创建时间正序。
6. **扣款汇总**：仅 CONFIRMED+OPERATOR+确认日命中行入组；EQUIPMENT/WAIVED 不入组；REVOKED 不入组；跨日确认不入组；按 workerUserId 分组、count 正确、records 字段完整。
7. **挡车工候选**：PostApi 按 LUNA_WORKER 取到岗位时返回该岗位用户 id/昵称；岗位不存在时返回空列表不报错。
8. **分页查询**：status 缺省等价于 PENDING+CONFIRMED；显式 REVOKED 可查冻结行；其余筛选参数正确透传到查询构造。

## Out of Scope

- **工资系统写入**：不做工资初稿、不做 payrollSynced 已同步标记、不建任何工资表/接口（新系统无工资模块；汇总仅只读查询，旧系统的 draftPayroll 不迁移）
- **检验同步入库**（ADR-0013 仍延后）：不接入库明细、不在入库链路挂任何钩子；未来同步入库上线后如需在入库侧联动，另行决策
- **机台/机台任务/织机挡车工自动反查**：不建机台主数据、不做"机台→挡车工"解析（织造执行域的织机/机台任务是独立规划，不与本功能耦合）；挡车工一律由认定人在下拉中选择
- **独立员工（HR）主数据**：不新建员工表，挡车工复用 system_users + system_post（ADR-0021）
- **自动实时生成**：不做录检保存即生成的钩子；生成只能由检验单工作台显式按钮触发（ADR-0019）
- **记录删除**：不提供任何删除端点/入口（ADR-0020），误生成以免扣结案
- **疵点（defects）数据**：确认行只快照等级等既有明细字段，不做疵点数组
- **批量认定**：一次只能认定一卷（逐卷三键），不做勾选批量定责
- **自定义字段扩展（extensionData）**：旧系统的表单配置中心不迁移
- **通知/待办消息**：不发站内信/消息推送
- **打印模板定制与 Excel 导出**：仅浏览器打印的简易排版
- **乐观锁/并发编辑控制**：认定/撤销以状态守卫兜底，不做版本号
- **操作日志模块接入**：认定链详情仅呈现本域 chain 数据，不接系统操作日志
- **前端单元/组件测试、REST 集成测试与测试数据库基建**
- **正式 UI（views/lunaerp/）版本**

## Further Notes

### 已落地文档

- [CONTEXT.md](../../CONTEXT.md) — 已新增"生产管理：质检确认"术语分区（质检确认、责任类型、质检确认状态、挡车工、质检确认来源、认定日期双口径），并更新模块边界（10270 菜单/1-030-014 段）、字典（2236/2237）、错误码段位表
- [ADR-0019](../../docs/adr/0019-inspection-confirmation-source-and-generation.md) — 以检验单录检明细为源、显式幂等生成、快照零回写
- [ADR-0020](../../docs/adr/0020-confirmation-revoke-clone-chain.md) — 撤销冻结+克隆留痕链、一律不可删
- [ADR-0021](../../docs/adr/0021-worker-reuse-system-user-post.md) — 挡车工复用 system_users + system_post
- 相关既有决策：ADR-0013（检验同步入库延后）、ADR-0014（录检明细独立子资源——本功能的数据源）、ADR-0009（无业务编号先例：染色单）、ADR-0001（lunaerp-elmui 先行版）

### 实现顺序建议

1. SQL 脚本（1 表 + 索引 + 2236/2237 字典 + LUNA_WORKER 岗位 seed + 10270-10275 菜单）
2. 后端 DO + Mapper + 枚举（Source/Status/Responsibility，岗位编码常量）
3. VO 套装（PageReqVO/RespVO/ConfirmReqVO/RevokeReqVO/ManualCreateReqVO/GenerateRespVO/PrintViewRespVO/PayrollSummaryRespVO/WorkerOptionRespVO）+ 错误码常量（1-030-014 段）
4. system 模块 PostApi/PostService 纯增量加 getPostByCode（透传已有 PostMapper.selectByCode）；lunaerp Service（生成幂等、认定、撤销同事务双写、手工登记、打印查询、汇总聚合、worker-options 解析）+ Controller（9 端点）
5. ServiceImpl 纯 Mockito 单元测试（单测试类，行为见 Testing Decisions）
6. 前端 API 与 TS 类型、字典枚举
7. 前端列表页 + 认定对话框 + 认定链抽屉 + 手工登记对话框 + 打印弹窗 + 汇总弹窗
8. 检验单工作台"生成质检确认"按钮接入（含状态/权限控制与新增跳过提示）

### 参考

- 旧系统后端：`linaagi/server/src/services/inspection-confirmation.service.ts`（生成/认定/撤销克隆/打印/汇总语义来源，注意其数据源是入库明细、工资初稿标记本期不迁移）、`linaagi/server/src/routes/inspection-confirmations.ts`（端点语义）、`linaagi/server/prisma/schema.prisma`（InspectionConfirmation 模型字段对照）
- 旧系统前端：`linaagi/web/app/staff/components/production/InspectionConfirmList.tsx` 与同目录 `inspection-confirm/`（交互与口径提示参考；视觉为旧系统卡片风，本期不沿用，按 elmui 表格范式实现）
- 新系统后端模板：`production/inspectionorder`（同域包结构、检验单/明细 Mapper、字典用法）、`stock/stockin`（严格状态守卫、同事务双写模式、VO/Controller 范式）、`business/salescontract`（关联 system User 的 salespersonUserId 先例）
- 新系统前端模板：`views/lunaerp-elmui/production/inspection-order/`（列表+工作台、远程选择、DictTag、Pagination）、`views/lunaerp-elmui/stock/stock-in/`（审核类操作按钮+二次确认、弹窗范式）
- system API：`AdminUserApi`（getUserListByPostIds/getUser/validateUser）、`PostApi`（按编码查岗位）；前端用户选择组件为 `views/system/user/components/UserSelect.vue`（本期不改它，挡车工下拉独立实现）
