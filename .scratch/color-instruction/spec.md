# 打色指示单（Color Instruction）

Status: ready-for-agent（grilling 两轮十七问全部按推荐锁定，源自 `.scratch/lunaerp-commercialization/issues/06-color-instruction.md`）

## Problem Statement

商业化一期核心包的售前→生产链需要"打色"环节的系统承载：客户下单前，需按客户提供的色号/色卡/参考样，由本厂打色房或染厂打出小样，经客户（或内部）在指定灯源下对色确认后，才能投产胚布与染色大货。现状缺口：

1. 新栈没有任何打色单据，色号/灯源/对色结果只存在于业务员微信与纸质指示单，无法追溯"这个色是谁确认的、第几轮过的"。
2. 染色颜色明细（`DyeingColorDO`）只有 `customerColorNo/colorName` 与数量字段，**没有灯源、色卡参考、样布照片**，也没有确认过程；打色数据无落点。
3. 胚布单/染色单无法声明"颜色依据哪张打色单"，颜色血缘断裂。

**老系统参照**：`LabDipInstruction` + `LabDipColor`（主辅光源、份数、轮次、行状态、色值、头附件）。一期按 Q14-h 取舍：**只做打色指示单**，寄样/选样/调样/门市剪样二期。

**关键边界**：打色单**独立建档**，不依赖样品链、不挂样品菜单；一个打色单服务一个客户的一个产品；不产生任何库存/账务副作用。

## Solution 总览

grilling 锁定要点：

1. **两张表**：主表 `lunaerp_production_color_instruction` + 颜色行子表 `lunaerp_production_color_instruction_color`。颜色行随单聚合、无独立界面/权限码；保存采用**差异保存 + 行状态守卫**（非删旧插新——须保留 REJECTED 轮次留痕）。
2. **颜色身份字段全域对齐**：`customerColorNo`（客户色号）+ `colorName`（颜色名称）与染色颜色明细/发货明细同口径，另加 `factoryColorNo`（厂内色号）、`colorHex`（色值）。不沿用合同明细的弱文本字段 `customerColor`。
3. **灯源**：字典 `lunaerp_color_light_source`，主灯源必填、辅灯源可空。
4. **单据五态**：PENDING 待打色 / IN_PROGRESS 打色中 / CONFIRMED 已确认 / IN_PRODUCTION 已投产 / VOID 已作废；确认类型 `confirmType=CUSTOMER/INTERNAL`；投产由下游单据引用**自动触发**；IN_PRODUCTION、VOID 为终态。
5. **行三态 + 轮次**：PENDING/APPROVED/REJECTED，默认 round=1；"驳回重打"冻结旧行、克隆 round+1 新行。确认前置：无 PENDING 行且至少一行 APPROVED。
6. **下游引用**：胚布单、染色单各加 `color_instruction_id` + `color_instruction_no` 可空显式列（对照 shipment from-contract 做法，不引入通用 sourceType/sourceId）；被有效下游引用时禁作废/删除。
7. **菜单**：生产管理目录下 10230 打色指示单（sort=0，时序在胚布单之前）。
8. **打印**：本期实现 A4 固定套打模板 `COLOR_INSTRUCTION_SHEET`，注册统一打印注册表 + 独立 print-data 接口 + 操作日志。
9. 一期**不接审批引擎、不发站内消息、不做 Excel 导入导出**。

### 工程落点

**后端**（`luna-srv/lunasrv-module-lunaerp`）：

```
com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.production.colorinstruction
├── ColorInstructionDO.java
└── color/
    └── ColorInstructionColorDO.java
# 横切注册：
service/production/colorinstruction/ColorInstructionRecycleHandler.java   # 统一回收站 SPI
```

建表 SQL：`luna-srv/sql/postgresql/lunaerp-production-color-instruction.sql`（建表 + 菜单 10230~10235 + 字典 2255/2256 + dict_data 3736 起）。

胚布单、染色单现有建表 SQL 各追加两列：`color_instruction_id BIGINT NULL`、`color_instruction_no VARCHAR NULL`。

**前端**（`luna-ui`）：

```
src/views/lunaerp-elmui/production/color-instruction/
├── index.vue                         # 列表页
├── edit.vue                          # 新建/编辑（单据头 + 颜色行表 + 行操作）
└── components/print/
    └── ColorInstructionSheet.vue     # A4 套打模板
src/api/lunaerp-elmui/production/color-instruction/index.ts
```

---

## 1. 数据模型

### 1.1 主表 `lunaerp_production_color_instruction`

| Java 字段 | DB 列 | 必填 | 说明 |
|---|---|---|---|
| id | id | 是 | 主键 |
| instructionNo | instruction_no | 是 | 打色单号，见第 2 节 |
| customerId | customer_id | 是 | 客户档案 ID（`lunaerp_baseinfo_customer.id`，逻辑关联） |
| clientName | client_name | 是 | 客户名快照 |
| contactPerson | contact_person | 否 | 客户联系人（文本，可含电话） |
| productId | product_id | 是 | 产品档案 ID 强关联（同胚布单先例） |
| productCode | product_code | 是 | 产品编号快照 |
| productName | product_name | 是 | 产品名称快照 |
| labFactoryId | lab_factory_id | 否 | 打色承接方供应商 ID（`lunaerp_baseinfo_supplier.id`；**空=本厂打色房自做**，不用占位供应商，同 ADR-0022 自织先例） |
| labFactoryName | lab_factory_name | 否 | 承接方公司名快照 |
| composition | composition | 否 | 成分（文本快照） |
| width | width | 否 | 幅宽（文本快照） |
| gsm | gsm | 否 | 克重（文本快照） |
| instructionDate | instruction_date | 是 | 单据日期，缺省当天 |
| deliveryDate | delivery_date | 否 | 要求完成日 |
| status | status | 是 | 五态，缺省 PENDING（字典 `lunaerp_color_instruction_status`） |
| confirmType | confirm_type | 否 | `CUSTOMER` 客户确认 / `INTERNAL` 内部确认，确认时必填 |
| customerFeedback | customer_feedback | 否 | 客户反馈意见（确认/驳回时可填） |
| confirmedBy | confirmed_by | 否 | 确认人用户 ID |
| confirmedAt | confirmed_at | 否 | 确认时间 |
| voidReason | void_reason | 否 | 作废原因 |
| remark | remark | 否 | 备注 |

框架列：TenantBaseDO（`tenant_id` + create/update/deleted）。

**刻意不包含**：pH、缩水率、八项色牢度等大货质量字段（归染色单）；单据头附件（打色结果天然按色号区分，附件全部落颜色行）。

索引：

- `UNIQUE (tenant_id, instruction_no)`——全量唯一含已删行（仿 ADR-0006）
- `INDEX (tenant_id, customer_id)`、`INDEX (tenant_id, status)`、`INDEX (tenant_id, instruction_date)`

### 1.2 颜色行子表 `lunaerp_production_color_instruction_color`

| Java 字段 | DB 列 | 必填 | 说明 |
|---|---|---|---|
| id | id | 是 | 主键 |
| instructionId | instruction_id | 是 | 打色单 ID |
| lineNo | line_no | 是 | 行号，服务端重排（同色号多轮行相邻，最新轮在前） |
| customerColorNo | customer_color_no | 是 | **客户色号**，与 DyeingColorDO/ShipmentItemDO 同口径 |
| colorName | color_name | 是 | 颜色名称，与上述同口径 |
| factoryColorNo | factory_color_no | 否 | 厂内色号（老系统 factoryNo） |
| colorHex | color_hex | 否 | 色值 `#rrggbb`（列表色块/取色器；空则灰占位） |
| lightPrimary | light_primary | 是 | **主灯源**，字典 `lunaerp_color_light_source` |
| lightSecondary | light_secondary | 否 | 辅灯源，同字典 |
| colorCardRef | color_card_ref | 否 | 色卡参考（自由文本，如 `Pantone TPX 19-4052`） |
| samplePhotos | sample_photos | 否 | 样布照片 JSON 数组 `[{"name":"...","url":"..."}]`，芋道 UploadImgs 走 infra 文件服务，可多张 |
| copies | copies | 否 | 份数（Integer） |
| round | round | 是 | 打色轮次，缺省 1 |
| status | status | 是 | `PENDING` 待结果 / `APPROVED` 通过 / `REJECTED` 驳回（字典 `lunaerp_color_line_status`） |
| rejectReason | reject_reason | 否 | 驳回/否决意见 |
| remark | remark | 否 | 行备注 |

索引：`INDEX (tenant_id, instruction_id)`、`INDEX (tenant_id, status)`。

### 1.3 差异保存与行状态守卫

保存请求中颜色行携带 `id`，服务端在同一事务内：

1. **插入**：无 id 的行（round=1、status=PENDING）。
2. **更新**：带 id 的行——**仅 PENDING 行可改**；对 APPROVED/REJECTED 行提交任何字段变更，报 `1_030_032_006`。
3. **删除**：请求中缺失的持久化行——**仅 PENDING 行可删**；冻结行缺失不处理（不报错），显式提交删除冻结行报 `1_030_032_006`。
4. 服务端重排 `lineNo`：按色号分组（同 `customerColorNo` 多轮相邻、最新轮在前），从 1 连续编号。

不引入 ADR-0032 完整差异保存（打色行无外部子资源引用行 ID，仅需本模块内守卫）。

## 2. 打色单号

形如 `DS20261007001`：`DS` + 年月日 8 位 + 3 位日流水（超 999 自然扩位），**按天取号**，租户内全量唯一、软删不释放（仿 ADR-0006 胚布单号、YP 单号的无连字符风格）。新建缺省服务端生成，也可手填，撞号报 `1_030_032_001`。

## 3. 状态机

### 3.1 单据状态与流转

```
PENDING ──开始打色──> IN_PROGRESS ──确认──> CONFIRMED ──下游引用(自动)──> IN_PRODUCTION
   │                     │  ▲                  │
   │                     │  └──── 回退(无下游引用) ┘
   └────── 作废 ─────────┴──────── 作废 ───────┘
                                           VOID（终态）
```

- 五态：`PENDING` 待打色（缺省）/ `IN_PROGRESS` 打色中 / `CONFIRMED` 已确认 / `IN_PRODUCTION` 已投产 / `VOID` 已作废。
- **PENDING→IN_PROGRESS**："开始打色"动作（`:update` 权限）；便利规则——首次登记行结果（通过/驳回）时若单据仍 PENDING，自动转 IN_PROGRESS。
- **IN_PROGRESS→CONFIRMED**：确认动作，守卫见 3.3。
- **CONFIRMED→IN_PROGRESS（回退）**：仅当**无任何下游胚布单/染色单引用**，二次确认，写操作日志；行状态不回退（APPROVED 行保持，业务员可逐行"驳回重打"）。
- **作废**：PENDING/IN_PROGRESS/CONFIRMED 可作废（填 voidReason）；存在有效（未作废、未删除）下游引用时报 `1_030_032_005`。**IN_PRODUCTION 禁作废**。
- **终态**：IN_PRODUCTION、VOID——终态后不可编辑、不可作废、不可删除（终态守卫）。
- **软删（进回收站）**：仅 PENDING 态可删；CONFIRMED/IN_PRODUCTION 取消走作废。

编辑守卫：仅 PENDING/IN_PROGRESS 可整体编辑；CONFIRMED 后先回退再改。

### 3.2 行级动作：通过 / 驳回重打

- **通过（APPROVE）**：PENDING→APPROVED（`:confirm` 权限）；单据 PENDING 时自动转 IN_PROGRESS。
- **驳回重打（REJECT-RESTRIKE）**：允许对 **PENDING 或 APPROVED** 行使用（覆盖"电话里先通过、次日又改口"场景），单据须 PENDING/IN_PROGRESS：
  1. 旧行置 `REJECTED` 冻结，记录 rejectReason；冻结行此后不可改不可删。
  2. 同事务克隆新行：继承 `customerColorNo/colorName/factoryColorNo/colorHex/lightPrimary/lightSecondary/colorCardRef/copies`，`round = 旧行 round + 1`、`status=PENDING`；**samplePhotos 不继承**（上一轮结果物）。
  3. REJECTED 行不可再"重打"（如需三轮，对最新 PENDING 行继续驳回重打）。
- 打印模板显示**全部轮次行**（含 REJECTED），标注轮次与行状态。

### 3.3 确认动作守卫

点"确认"时服务端校验：

1. 单据状态为 IN_PROGRESS（CONFIRMED 重复确认报错）；
2. **无 PENDING 行**（存在未决行报 `1_030_032_004`）；
3. **至少 1 行 APPROVED**（全部驳回不能确认，报同一错误码）。

通过后：写入 `confirmType`（CUSTOMER/INTERNAL，弹窗必选）、`customerFeedback`（可空）、confirmedBy、confirmedAt，status=CONFIRMED。

### 3.4 投产自动触发

- 胚布单保存（创建即 ACTIVE）打色单引用后，同进程调用 `ColorInstructionService.markInProduction(id)`；
- 染色单：DRAFT 保存引用**不触发**，"下达"转 ACTIVE 时触发；已 ACTIVE 单补填引用立即触发；
- 实现：条件更新 `UPDATE ... SET status='IN_PRODUCTION' WHERE id=? AND tenant_id=? AND status='CONFIRMED' AND deleted=0`，影响行数 0 即已投产/已作废/已回退，**幂等静默**，不报错；
- 下游单作废/删除**不回退**打色单——"曾为此色投产"是事实，且可能有其他下游；IN_PRODUCTION 为终态。

## 4. 下游单据引用约定

对照 shipment from-contract（显式列 + 单号快照，非通用 sourceType/sourceId）：

- 胚布单表加 `color_instruction_id BIGINT NULL`、`color_instruction_no VARCHAR NULL`；
- 染色单表加同构两列。

校验规则（下游保存时）：

1. 引用打色单与当前单据同租户、未删除，否则报下游各自段的关联引用错误；
2. 仅 **CONFIRMED / IN_PRODUCTION** 状态可被引用（PENDING/IN_PROGRESS/VOID 报状态不允许）；
3. 一张胚布单/染色单至多引用一张打色单（同客户同产品口径）；
4. 打色单侧作废/删除守卫：存在有效下游（status ≠ CANCELLED 且未删除）引用即报 `1_030_032_005`；下游 DRAFT 染色单同样计入引用。

不引入通用 `source_type/source_id` 字段——保持全项目惯例一致。

## 5. API

| 方法 | 路径 | 权限码 | 说明 |
|---|---|---|---|
| GET | `/lunaerp/production/color-instruction/page` | `:query` | 分页（筛选见 7.1） |
| GET | `/lunaerp/production/color-instruction/get?id=` | `:query` | 详情含颜色行 |
| POST | `/lunaerp/production/color-instruction/create` | `:create` | 新建（PENDING；单号可空=自动取号） |
| PUT | `/lunaerp/production/color-instruction/update` | `:update` | 差异保存（仅 PENDING/IN_PROGRESS） |
| DELETE | `/lunaerp/production/color-instruction/delete?id=` | `:delete` | 仅 PENDING |
| PUT | `/lunaerp/production/color-instruction/start` | `:update` | PENDING→IN_PROGRESS |
| PUT | `/lunaerp/production/color-instruction/confirm` | `:confirm` | body: id, confirmType, customerFeedback |
| PUT | `/lunaerp/production/color-instruction/rollback` | `:confirm` | CONFIRMED→IN_PROGRESS（无下游） |
| PUT | `/lunaerp/production/color-instruction/void` | `:confirm` | body: id, voidReason |
| PUT | `/lunaerp/production/color-instruction/color/approve` | `:confirm` | body: id（行 ID） |
| PUT | `/lunaerp/production/color-instruction/color/reject-restrike` | `:confirm` | body: id, rejectReason |
| GET | `/lunaerp/production/color-instruction/{id}/print-data` | `:print` | 打印取数独立 VO |

- 权限码前缀：`lunaerp:production:color-instruction:`。
- 全部写动作与打印加 `@LogRecord`（如 `success="确认打色单 {{#instructionNo}}"`），入 `system_operate_log`，零新日志表。
- 所有状态更新走条件更新（带 `status=? AND tenant_id=? AND deleted=0`），影响行数 0 抛状态错误码，天然防并发覆盖。

## 6. 打印套打

- 注册表（`.scratch/common-foundation` 统一打印契约）：

```ts
{
  bizType: 'COLOR_INSTRUCTION',
  templateCode: 'COLOR_INSTRUCTION_SHEET',
  name: '打色指示单',
  paper: 'A4',
  permission: 'lunaerp:production:color-instruction:print',
  component: ColorInstructionSheet,
  fetchApi: (id) => colorInstructionApi.getPrintData(id),
}
```

- 列表/详情"打印"按钮统一走 `<PrintDispatcher biz-type="COLOR_INSTRUCTION" :biz-id="row.id" />`。
- A4 固定版式内容：抬头（单号/单据日期/客户/联系人/产品编号名称/成分幅宽克重/打色承接方/要求完成日）+ 颜色行表（行号/客户色号/厂内色号/颜色名/色块/主灯源/辅灯源/色卡参考/份数/轮次/行状态）+ 客户反馈/备注 + 签字位（制单/打色/客户确认）。全部轮次行均输出，REJECTED 行划线标注。
- 后端 print-data 返回独立打印 VO（不脱敏——无金额字段；联系人按打印豁免惯例全量），`@LogRecord(type="打印")` 留痕。

## 7. 前端页面

### 7.1 列表页

- 筛选：单号 / 客户 / 产品（编码或名称模糊）/ 客户色号 / 状态 / 单据日期区间。
- 列：单号、客户、产品编号/名称、颜色行摘要（色号 chips，含轮次数）、状态、单据日期、要求完成日、确认类型、操作（编辑/打印/作废）。
- 新建按钮；仅 PENDING 行显示删除。

### 7.2 编辑页

- 单据头表单：客户（选择后带联系人候选）、产品（强选产品档案，带出编号/名称/成分/幅宽/克重快照可改）、打色承接方（供应商，可空=本厂打色房）、日期、备注。
- 颜色行可编辑表格：行内字段 + 取色器（colorHex）、主辅灯源下拉、色卡参考、UploadImgs 多图上传；行操作：通过、驳回重打；按色组展示多轮行（REJECTED 行置灰、显示驳回意见）。
- 页头动作：开始打色、确认（弹窗选客户/内部确认 + 反馈意见）、回退、作废、打印、保存。

## 菜单与权限汇总

| 菜单/权限 | ID/类型 | 位置 |
|---|---|---|
| 打色指示单 | 10230，菜单（type=2，sort=0） | 10200 生产管理目录 |
| 创建 | 10231，按钮 | 10230 下 |
| 修改 | 10232，按钮 | 同上 |
| 删除 | 10233，按钮 | 同上 |
| 确认（含通过/驳回重打/回退/作废） | 10234，按钮 | 同上 |
| 打印 | 10235，按钮 | 同上 |

路由 path `color-instruction`，组件 `lunaerp-elmui/production/color-instruction/index`，图标建议 `ep:sunny`。

## 回收站

注册 `RecycleBinHandler`（bizType `COLOR_INSTRUCTION`）：

- 仅 PENDING 单可软删（打色单无 DRAFT，PENDING 为"从未生效"等价物）；
- 恢复为 PENDING，级联复活颜色行（含历史 REJECTED 轮次行）；
- 引用的客户/产品/供应商在回收站则阻断恢复（`1_030_031_002`）；
- CONFIRMED/IN_PRODUCTION/VOID 不进回收站（作废留单是业务事实）。

## 字典

| dict_type id | type | 取值（dict_data id 自 3736 起） |
|---|---|---|
| 2255 | `lunaerp_color_light_source` 灯源 | `D65` 标准光源 D65（3736）/ `TL84` TL84 三基色（3737）/ `CWF` CWF 冷白荧光（3738）/ `A` A 光源/白炽灯（3739）/ `UV` 紫外光源（3740）/ `TL83` TL83 暖白荧光（3741） |
| 2256 | `lunaerp_color_line_status` 颜色行状态 | `PENDING` 待结果（3742）/ `APPROVED` 通过（3743）/ `REJECTED` 驳回（3744） |

单据五态状态字典 `lunaerp_color_instruction_status`：dict_type id **2257**，dict_data 3745-3749（PENDING/IN_PROGRESS/CONFIRMED/IN_PRODUCTION/VOID）。

`confirmType`（CUSTOMER/INTERNAL）为后端内部枚举，**不入字典**（同各单 source/type 先例）。

## 错误码（段位 `1-030-032`，入 shared GLOSSARY 段位总表）

| 码 | 说明 |
|---|---|
| 1_030_032_000 | 打色单不存在 |
| 1_030_032_001 | 打色单号【{}】重复 |
| 1_030_032_002 | 打色单当前状态不允许该操作 |
| 1_030_032_003 | 关联引用不存在（客户/产品/供应商） |
| 1_030_032_004 | 存在未决颜色行或无通过行，不能确认 |
| 1_030_032_005 | 已被下游有效单据引用，不能作废或删除 |
| 1_030_032_006 | 颜色行当前状态不允许修改或删除 |
| 1_030_032_007 | 灯源或确认类型非法 |
| 1_030_032_100 | 颜色行不存在 |

## 统一 DoD 对照

- [x] 状态机白名单+终态守卫：五态白名单流转；IN_PRODUCTION/VOID 终态拒绝一切写操作；行三态冻结守卫
- [x] 并发条件更新：全部状态动作 `WHERE status=? AND tenant_id=? AND deleted=0`，影响行数 0 报错；投产标记幂等
- [x] 芋道权限+脱敏：六个动作权限码 + 菜单注册；本模块无金额/联系人脱敏字段（联系人打印走豁免）
- [x] 列表/详情/删除进回收站：列表/详情齐备，注册 COLOR_INSTRUCTION handler，仅 PENDING 可删可恢复
- [x] 打印+操作/审批日志：COLOR_INSTRUCTION_SHEET 本期实现，独立 print-data；全部写动作/打印 @LogRecord
- [x] 金额逻辑测试：无金额字段（N/A，显式确认）
- [x] 核心副作用测试：差异保存（冻结行改/删拦截、行号重排）、确认三类守卫、驳回重打克隆内容与轮次、回退引用守卫、作废下游守卫、投产幂等触发
- [x] 前端验收清单+波次冒烟：建单→加 3 色→驳回 1 色重打→部分通过→确认拦截→全部通过→确认→胚布引用自动投产→打印含全部轮次
- [x] 性能：列表按 `(tenant_id, instruction_date/customer_id)` 分页，万级 P95<1s；色号 chips 走详情子查询，列表不 N+1
- [x] 租户隔离：全部查询/条件更新带 tenant_id；引用他租户打色单必测拦截
- [x] 纵向/横向越权：无 :confirm/:print/:delete 权限分别 403；他租户 id 访问 404/越权错误
- [x] 术语与错误码：打色指示单/客户色号/灯源/色卡参考/打色轮次进 production GLOSSARY；1-030-032 入段位总表

## Out of Scope

- 寄样/选样/调样/门市剪样（Q14，二期增值包）；样品链关联与样品菜单互跳（Q14-h 独立建档）
- 审批引擎接入、站内消息/待办（客户在系统外，由持 :confirm 权限人员代录 + 操作日志）
- Excel 导入与导出（打印件承载对外场景）
- 通用 source_type/source_id 多态引用；一张打色单多产品
- 大货质量字段（pH/色牢度/缩水率）、配方/染化料计算
- 客户门户打色进度查看、打色移动端
- 可视化打印模板设计器、打印模板租户自定义（Q12=A，二期）
