# 合同「下达生产」工作台（Release Production Workbench）

Status: ready-for-agent（grilling 一轮十二问全部按推荐锁定，源自 `.scratch/lunaerp-commercialization/issues/08-release-production-workbench.md`）

## Problem Statement

合同→生产是商业化一期核心链 W2 的咽喉。现状缺口：

1. **无统一入口**：新栈已有打色指示单、胚布单、染色单、检验单四类独立单据，但全部靠人工逐张新建、从合同复制数据；老系统更有三套下达入口并存（合同页/胚布页/染色页各一套拆单逻辑），口径漂移与漏单是常态。
2. **合同行无下游血缘**：胚布/染色单头有 `sales_contract_id`（合同级），颜色行无合同行关联；无法按合同行聚合生产进度，也无法做部分下达/追加下达/超下达拦截。
3. **无已下达量记录**：不知道某合同行还能下达多少，重复下达全靠人工记忆。

charting Q7=A 已锁定：合同→生产**唯一入口**=合同详情页「下达生产」工作台，底层沿用四类独立单据、**不建聚合实体**。本 spec 落地该入口。

## Solution 总览

grilling 锁定要点（12 问）：

1. **入口形态**：合同详情页新增「生产」Tab，内嵌下达工作台（Q11=A）；不弹窗、不跳新页。
2. **四类单生成**：一次下达生成 胚布单 + 染色单 + 胚布检验单（GREIGE，DRAFT）；打色指示单**可选**——选已有 CONFIRMED 打色单则胚布/染色引用它，新建 PENDING 打色单则胚布/染色**不引用**（待确认后手动补填，零回写）（Q1=A）。
3. **成品检验**：不在下达时生成；染色单 COMPLETED 后在染色工作台提供「生成成品检验单」便利按钮（手动触发，非自动，尊重检验单 spec「无自动触发」边界）（Q2=C）。
4. **合同行关联**：`greige_order_color`、`dyeing_color` 各加 `contract_line_id` + `contract_line_no` 显式列；检验单明细（逐卷）不加合同行（一卷可跨多行）（Q3=A）。
5. **数量分配**：一行合同行默认全量下达（剩余量），可手工改小实现拆分；同合同同产品多行合并到同一张胚布/染色单（多颜色行）（Q4=A、Q6=A）。
6. **损耗**：胚布单 `suggested_loss` 缺省 0（产品档案无单一默认损耗字段，损耗散落在成本项/纱线规格），工作台允许逐行覆盖（Q5=A）。
7. **已下达量**：合同行加 `released_qty`，下达时同事务条件更新累加；超下达（`released_qty + delta > quantity`）拦截；下游单据修改/删除**不回写**（ADR-0040）（Q7=A、Q9=A）。
8. **合同变更/取消**：合同行变更不影响已下达单据，只约束未来下达；合同 CANCELLED 不自动联动在途单据，仅提示由人工处理（Q8=A、Q10=A）。
9. **缸次**：一期不做缸次拆分（DyeingVat 二期）；需分缸通过拆成多张染色单实现（Q12=A）。
10. **唯一入口**：下达生产只此一个入口，不做第二套拆单路径（老系统教训写入非目标）。

### 工程落点

**后端**（`luna-srv/lunasrv-module-lunaerp`）：

```
com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.production.releaseproduction
├── ReleaseProductionController.java
├── ReleaseProductionService.java          # 编排：from-contract 预填 + release 下达
└── dto/
    ├── ReleaseFromContractRespVO.java
    └── ReleaseProductionReqVO.java
```

横切改动：
- `lunaerp_business_sales_contract_item` 加 `released_qty` 列
- `lunaerp_production_greige_order_color` 加 `contract_line_id` / `contract_line_no` 列
- `lunaerp_production_dyeing_color` 加 `contract_line_id` / `contract_line_no` 列
- 染色单工作台加「生成成品检验单」按钮（调本模块服务）

建表/变更 SQL：`luna-srv/sql/postgresql/lunaerp-production-release-production.sql`（ALTER TABLE 加列 + 权限菜单按钮，不新建表）。

**前端**（`luna-ui`）：

```
src/views/lunaerp-elmui/business/sales-contract/
├── index.vue                              # 列表页（新增「下达生产」行操作，跳详情 Tab）
└── workspace/
    ├── index.vue                          # 合同详情工作台（已有，加 Tab）
    └── tabs/
        └── ReleaseProductionTab.vue       # 下达生产工作台
src/api/lunaerp-elmui/production/release-production/index.ts
```

---

## 1. 数据模型变更

### 1.1 合同行加 `released_qty`

`lunaerp_business_sales_contract_item` 新增列：

| 列 | 类型 | 必填 | 说明 |
|---|---|---|---|
| released_qty | numeric(18,2) | ✓ | 已下达生产数量 KG，缺省 0；下达时累加，下游单据修改/删除不回写（ADR-0040） |

无新索引（合同行查询本就按 `contract_id`）。

### 1.2 胚布颜色行加合同行关联

`lunaerp_production_greige_order_color` 新增列：

| 列 | 类型 | 必填 | 说明 |
|---|---|---|---|
| contract_line_id | int8 | 否 | 来源合同行 ID（`lunaerp_business_sales_contract_item.id`），逻辑关联不建外键 |
| contract_line_no | int4 | 否 | 来源合同行号快照 |

### 1.3 染色颜色行加合同行关联

`lunaerp_production_dyeing_color` 新增列：

| 列 | 类型 | 必填 | 说明 |
|---|---|---|---|
| contract_line_id | int8 | 否 | 来源合同行 ID |
| contract_line_no | int4 | 否 | 来源合同行号快照 |

### 1.4 检验单

**不加列**：检验单头已有 `sales_contract_id`、`greige_order_id`、`dyeing_order_id`；录检明细（逐卷）不加合同行关联（一卷可跨多合同行，业务非一一对应）（Q3=A）。

---

## 2. 「下达生产」服务

### 2.1 from-contract 预填接口

对照 shipment `from-contract` 先例，但剩余量用落库 `released_qty`（非实时聚合）。

`GET /lunaerp/production/release-production/from-contract?contractId=`

返回：

```jsonc
{
  "contractId": 123,
  "contractNo": "CH20260001",
  "clientName": "某某纺织",
  "lines": [
    {
      "contractLineId": 1,
      "lineNo": 1,
      "productCode": "P001",
      "productName": "纯棉汗布",
      "composition": "...", "width": "...", "gsm": "...",
      "customerColor": "C101",          // 合同行客户色号（弱文本）
      "contractQty": 1000.00,             // 合同数量
      "releasedQty": 400.00,              // 已下达量
      "remainingQty": 600.00,             // 剩余可下达 = contractQty - releasedQty
      "defaultLossRate": 0                // 产品无默认损耗字段，缺省 0
    }
  ],
  "existingColorInstructions": [          // 该合同下已有的打色单（供选择引用）
    { "id": 5, "instructionNo": "DS20261007001", "status": "CONFIRMED", "productCode": "P001" }
  ]
}
```

校验：合同不存在报 `1_030_035_000`；合同 CANCELLED 仍可查询但下达时拦截（见 2.3）。

### 2.2 下达接口

`POST /lunaerp/production/release-production/release`

请求体（按**同合同同产品分组**，组内多颜色行合并到同一张胚布/染色单）：

```jsonc
{
  "contractId": 123,
  "greigeFactoryId": 456,                 // 承织坯布厂（必填，OUTSOURCE 缺省）
  "dyeingFactoryId": 789,                 // 承染方（必填）
  "groups": [
    {
      "productCode": "P001",
      "productName": "纯棉汗布",
      "composition": "...", "width": "...", "gsm": "...",
      "colorInstructionMode": "EXISTING", // EXISTING 选已有 / NEW 新建 PENDING / NONE 不关联
      "existingColorInstructionId": 5,    // EXISTING 时必填，须 CONFIRMED
      "newColorInstruction": null,        // NEW 时填（见 2.4）
      "lines": [
        {
          "contractLineId": 1,
          "customerColor": "C101",
          "releaseQty": 600.00,           // 本次下达量（≤ remainingQty）
          "lossRate": 0.05                // 胚布单该颜色行损耗，缺省 0
        }
      ]
    }
  ]
}
```

返回：

```jsonc
{
  "greigeOrders": [{ "id": 11, "orderNo": "PB1007001" }],
  "dyeingOrders": [{ "id": 21 }],
  "inspectionOrders": [{ "id": 31, "code": "INS-20261007-001", "type": "GREIGE" }],
  "colorInstructions": [{ "id": 5 }]      // NEW 模式返回新建的打色单 ID
}
```

### 2.3 下达编排逻辑（同一事务）

对每个 group：

1. **合同校验**：合同存在且非 CANCELLED（CANCELLED 报 `1_030_035_001`）；group 内所有 contractLineId 属该合同。
2. **超下达拦截**：逐行条件更新 `released_qty`：
   ```sql
   UPDATE lunaerp_business_sales_contract_item
   SET released_qty = released_qty + #{releaseQty}
   WHERE id = #{contractLineId} AND tenant_id = ?
     AND released_qty + #{releaseQty} <= quantity
   ```
   影响行数 0 → 报 `1_030_035_002`（并发或超下达）。
3. **打色单处理**（见 2.4）。
4. **创建胚布单**：调 `GreigeOrderService.create`，头填 contractId/contractNo、greigeFactoryId、product 快照、suggestedLoss=组内最大 lossRate 或 0、status=ACTIVE；颜色行每行填 color、finishedQty=releaseQty、lossRate、contractLineId/contractLineNo；若 colorInstructionMode=EXISTING 则填 colorInstructionId/colorInstructionNo。
5. **创建染色单**：调 `DyeingOrderService.create`，status=ACTIVE（直接下达，非 DRAFT——下达即生效），头填 contractId/contractNo/customerId/clientName、dyeingFactoryId、product 快照；颜色行填 customerColorNo（=合同行 customerColor）、quantity=releaseQty、contractQty=合同行 quantity、contractLineId/contractLineNo；EXISTING 模式填 colorInstructionId。
6. **创建胚布检验单**：调 `InspectionOrderService.create`，type=GREIGE、status=DRAFT、productId、greigeOrderId=刚建的胚布单 ID、salesContractId、targetWeight=组内 releaseQty 合计。
7. 提交事务。

**幂等**：无独立幂等键——幂等性由 `released_qty` 条件更新保证：同一行重复提交会因 `released_qty + delta > quantity` 被拦截（若 delta 相同）。若需同量重复下达（如追加），需用户手动改量；天然防重复点击。

### 2.4 打色单处理（Q1=A）

`colorInstructionMode` 三态：

- **EXISTING**：`existingColorInstructionId` 必填，须为 CONFIRMED 或 IN_PRODUCTION（打色单 spec 规则）；胚布单/染色单填 `color_instruction_id`。若状态不符报 `1_030_035_003`。
- **NEW**：从组内 lines 新建一张打色单（同合同同产品一张），status=PENDING，颜色行从合同行 customerColor 映射 customerColorNo、lightPrimary 由请求 `newColorInstruction.colors[].lightPrimary` 提供（必填，字典 `lunaerp_color_light_source`）。**胚布单/染色单不填 color_instruction_id**（PENDING 不可被引用）。返回新建打色单 ID，用户后续在打色单页面确认后手动到胚布/染色单补填引用（零回写）。
- **NONE**：不创建不引用打色单。

`newColorInstruction` 结构：

```jsonc
{
  "labFactoryId": null,                    // 可空=本厂打色房
  "colors": [
    { "contractLineId": 1, "customerColorNo": "C101", "colorName": "C101",
      "lightPrimary": "D65", "factoryColorNo": "", "colorCardRef": "" }
  ]
}
```

### 2.5 成品检验单便利生成（Q2=C）

染色单工作台（COMPLETED 态）新增「生成成品检验单」按钮，调：

`POST /lunaerp/production/release-production/create-finished-inspection?dyeingOrderId=`

逻辑：从染色单带出 productId/customerId/contractId/dyeingOrderId，创建 type=FINISHED、status=DRAFT 的检验单，targetWeight=染色单数量合计。**手动触发**（非染色单状态变更自动触发），尊重检验单 spec「无自动触发」边界。已存在同 dyeingOrderId 的 FINISHED 检验单则提示不重复创建（查非删除单）。

---

## 3. 合同行聚合查询衔接

生产进度聚合（ADR-0034）现有路径经 `sales_contract_id` 从单据头取合同。本 spec 新增行级关联后，合同行维度的进度可通过 `contract_line_id` 从胚布/染色颜色行聚合：

- 合同行已下达胚布量 = Σ `greige_order_color.finished_qty` WHERE `contract_line_id = ?`
- 合同行已下达染色量 = Σ `dyeing_color.quantity` WHERE `contract_line_id = ?`

`released_qty` 作为下达历史口径，进度聚合作为在途现状口径，两者互补不互斥（ADR-0040）。本期不改动 `ProductionProgressQueryService`（仍按合同级聚合），行级聚合留待合同行进度页二期消费，仅落库 `contract_line_id` 留好血缘。

---

## 4. API

| 方法 | 路径 | 权限码 | 说明 |
|---|---|---|---|
| GET | `/lunaerp/production/release-production/from-contract` | `:query` | 合同行预填（剩余量 + 已有打色单） |
| POST | `/lunaerp/production/release-production/release` | `:release` | 下达生产（创建胚布/染色/胚布检 + 累加 released_qty） |
| POST | `/lunaerp/production/release-production/create-finished-inspection` | `:release` | 染色完成后生成成品检验单（手动） |

权限码前缀：`lunaerp:production:release-production:`。

全部写动作加 `@LogRecord`（如 `success="下达生产 合同【{{#contractNo}}】"`）。`release` 为一事务多单据创建，日志记录合同号与生成的单据号。

## 5. 前端工作台

### 5.1 合同详情「生产」Tab

布局：
- **顶部**：合同号、客户、交期、状态；若合同 CANCELLED 显示红色提示且禁用「下达生产」。
- **下达区**（可编辑表格，行=合同行）：
  - 列：行号、产品编号/名称、客户色号、合同数量、已下达量、**本次下达量**（可编辑，缺省剩余量）、**损耗率%**（可编辑，缺省 0）、打色单（下拉：不关联/选已有 CONFIRMED/新建）
  - 同产品行视觉分组（合并到同一张单的提示）
- **打色单配置区**（仅当某行选「新建」时展开）：每个需新建打色单的产品组，配置打色承接方、逐行主灯源（必填下拉）。
- **工厂区**：承织坯布厂（必填）、承染方（必填），远程搜索供应商。
- **底部**：本次下达汇总（胚布量/染色量/检验单数）+「下达生产」按钮。

交互：
- 本次下达量 ≤ 剩余量，前端校验；超出红框提示。
- 提交前二次确认（列出将生成的单据类型与数量）。
- 下达成功后刷新已下达量、清空本次下达量。

### 5.2 合同列表页

行操作新增「下达生产」按钮（跳转合同详情并激活「生产」Tab）；无 `:release` 权限则隐藏。

### 5.3 染色单工作台

COMPLETED 态操作栏新增「生成成品检验单」按钮，调 2.5 接口，成功后跳转检验单工作台。

## 6. 错误码（段位 `1-030-035`，避让 032 打色/033 出口单证/034 成本卡）

| 码 | 说明 |
|---|---|
| 1_030_035_000 | 销售合同不存在 |
| 1_030_035_001 | 合同已取消，不能下达生产 |
| 1_030_035_002 | 合同行【{}】已下达量超出合同数量（超下达或并发冲突） |
| 1_030_035_003 | 打色单【{}】状态不允许被引用（须 CONFIRMED/IN_PRODUCTION） |
| 1_030_035_004 | 打色单颜色行主灯源不能为空 |
| 1_030_035_005 | 存在相同染色单的成品检验单，请勿重复创建 |

## 7. 菜单与权限

不新增菜单项（工作台内嵌合同详情 Tab）。在销售合同菜单下加一个按钮权限：

| 权限 | ID | 位置 |
|---|---|---|
| 下达生产 | 10114，按钮 | 10110 销售合同菜单下 |

权限码 `lunaerp:production:release-production:query` / `:release`（按钮挂合同菜单下但权限码归生产域，因编排生产单据）。

## 8. 回收站

不新建回收站 handler（本模块无独立单据实体，仅编排现有四类单据与合同行字段）。`released_qty` 不进回收站恢复逻辑——合同行恢复时 `released_qty` 保持原值（已下达事实不回滚）。

## Testing Decisions

**只测外部行为**：`ReleaseProductionServiceImplTest`（纯 Mockito，mock 合同行 Mapper + 四类单据 Service + 打色单 Service）。

覆盖行为：
1. from-contract：返回每行 remainingQty = quantity − releasedQty；合同不存在报 000。
2. release 正常流程：一组一行 → 创建胚布单（颜色行含 contract_line_id）、染色单（颜色行含 contract_line_id）、胚布检验单；`released_qty` 累加 releaseQty。
3. release 同产品多行合并：两行同产品 → 一张胚布单两个颜色行、一张染色单两个颜色行。
4. 超下达拦截：releaseQty > remainingQty → 条件更新影响行数 0 → 报 002，事务回滚（无单据创建）。
5. 并发超下达：mock Mapper update 返回 0 → 报 002。
6. EXISTING 打色单：CONFIRMED → 胚布/染色填 color_instruction_id；PENDING → 报 003。
7. NEW 打色单：创建 PENDING 打色单，胚布/染色不填 color_instruction_id；lightPrimary 缺失报 004。
8. NONE 模式：不创建不引用打色单。
9. 合同 CANCELLED 下达报 001。
10. 成品检验单生成：从染色单带出字段创建 FINISHED DRAFT；重复创建报 005。

## Out of Scope

- 第二套下达入口（胚布/染色单页面的「从合同创建」保留为数据带入快捷方式，但**生产下达**只此一个入口；老系统三套入口并存的教训——非目标）
- 缸次拆分与染缸批次卡（DyeingVat，二期）
- 打色单确认后自动回填胚布/染色引用（零回写，手动补填）
- 下游单据删除/修改时回写 `released_qty`（ADR-0040，二期若需精确管控再做）
- 合同取消时自动联动在途单据（提示+人工处理）
- 检验单同步入库（ADR-0013，延后）
- 合同行级生产进度聚合 UI（仅落库 contract_line_id 留血缘，行级进度页二期）
- 多合同合并下达（仅同合同同产品合并）
- AI 智能拆单（二期）

## 统一 DoD 对照

- [x] 状态机白名单+终态守卫：本模块无独立状态机；创建下游单据时复用各单状态机（胚布 ACTIVE、染色 ACTIVE、检验 DRAFT、打色 PENDING），各单终态守卫由各自 Service 保证
- [x] 并发条件更新：`released_qty` 累加走条件更新 `WHERE released_qty + delta <= quantity`，影响行数 0 报错，天然防并发覆盖与超下达
- [x] 芋道权限+脱敏：`:query`/`:release` 权限码 + 合同菜单按钮；本模块无金额字段，联系人不涉及
- [x] 列表/详情/删除进回收站：本模块无独立实体，不进回收站；合同行恢复不回滚 released_qty
- [x] 打印+操作/审批日志：下达与成品检验生成加 `@LogRecord`；无独立打印
- [x] 金额逻辑测试：无金额字段（N/A，显式确认）
- [x] 核心副作用测试：下达创建三类单 + released_qty 累加、超下达拦截事务回滚、打色三态、成品检验去重
- [x] 前端验收清单+波次冒烟：合同详情→生产 Tab→选行→改量→选打色→下达→验证胚布/染色/检验单生成 + 合同行已下达量更新 + 超下达拦截 + 染色完成生成成品检验
- [x] 性能：from-contract 单次查询合同行（按 contract_id 索引），万级合同行 P95<1s；release 单事务内顺序创建，无 N+1
- [x] 租户隔离：全部查询/条件更新带 tenant_id；跨租户合同/单据引用必测拦截
- [x] 纵向/横向越权：无 :release 权限 403；他租户合同 ID 访问 404/越权错误
- [x] 术语与错误码：下达生产、已下达量、剩余可下达量进 business/production GLOSSARY；1-030-035 入段位总表

## Further Notes

### 已落地文档
- [ADR-0040](../../docs/adr/0040-contract-line-released-qty-stored-zero-writeback.md) — 合同行 released_qty 落库累加 + 下游零回写
- [ADR-0034](../../docs/adr/0034-production-progress-read-only-aggregate-view.md) — 生产进度只读聚合（零回写边界）
- [ADR-0003](../../docs/adr/0003-sales-contract-item-aggregate-persistence.md) — 合同明细聚合保存
- 打色指示单 spec（color_instruction_id 引用规则）、胚布单 spec、染色单 spec、检验单 spec

### 实现顺序建议
1. SQL 变更（3 表加列 + 10114 按钮权限）
2. 后端 DO 加字段（contract_item.releasedQty、greige_order_color/dyeing_color.contractLineId/contractLineNo）
3. ReleaseProductionService（from-contract + release 编排 + create-finished-inspection）+ Controller
4. 错误码常量（1-030-035 段）
5. ServiceImpl 纯 Mockito 单测
6. 前端 API + 合同详情「生产」Tab 工作台
7. 染色单工作台「生成成品检验单」按钮
8. GLOSSARY 术语 + 段位总表更新
