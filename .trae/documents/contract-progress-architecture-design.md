# 架构设计：生产进度（合同锚点 + 发染记录 + 只读聚合视图）

> "生产管理-生产进度"功能设计文档。2026-09-28 经 grill-with-docs 三轮访谈收敛，命名由原"合同进度/ContractProgress"体系统一更改为"生产进度/ProductionProgress"体系。配套决策：ADR-0033（发染记录）、ADR-0034（生产进度只读聚合视图）。

## Context

旧系统存在"订单进度(OrderProgress)与三张指示单(胚布/染色/检验)是两条平行线"的结构性缺陷：

1. 订单进度靠手动填字符串（`fabricProgress`/`dyeingProgress`），不从指示单数据自动推导
2. 入库挂在指示单上不在订单上（StockIn → greigeInstructionId/dyeingInstructionId）
3. 发货关联合同不关联订单（Shipment → contractNumber 文本弱关联）
4. 同一维度出现三套口径（订单的拉胚量、指示单的入库量、缸卡的排产量各算各的）
5. 拉胚记录(FabricPullRecord)挂在订单上，但缸卡(DyeingVat)挂在合同上——两个维度互不引用

新系统已删除 OrderProgress 实体，合同(SalesContract)作为唯一锚点。本方案回答：去掉订单后，"合同→织造→染厂→检验→出货"这条全链路的进度怎么跟踪。

## 0. 已敲定决策一览（访谈结论）

| # | 决策点 | 结论 |
|---|--------|------|
| 1 | 命名 | 菜单"生产管理 → 生产进度"；概念 ProductionProgress、视图 ProductionProgressView；避免"订单进度/合同进度" |
| 2 | MVP 范围 | 核心链路：发染记录（后端+双入口）+ PROCESS_OUT 流水 + ProductionProgressQueryService（page+get）+ 独立"生产进度"页（列表+抽屉）；合同工作台 Tab、看板 widget 后置 |
| 3 | 流水按合同聚合 | **join 反查**：不给 `lunaerp_stock_ledger` 加 `sales_contract_id` 列，按 related_doc join 入库单/发货单取合同 |
| 4 | 单位口径 | 进度只聚合 KG；M 单位明细原样展示不进合计，不折算 |
| 5 | 预警 | 三条：临期（交期≤7天未发完）/ 超期（交期已过未发完）/ 延期缸 |
| 6 | API | `page`（跨合同列表）+ `get`（单合同全链路）两个接口 |
| 7 | 发染仓库 | 弹窗必选仓库，记录存仓库 ID+名称快照，红冲沿用同一仓库 |
| 8 | 发染编辑性 | 受限编辑：仅补填染色单/染厂、改备注；数量/日期/匹数/仓库锁定；错删重录 |
| 9 | 发染粒度 | 按胚布单整单一条（产品级快照，颜色不细分；颜色级血缘后置） |
| 10 | 发染负库存 | 拦截报错（同领纱发纱 ADR-0027）；直发场景先入库，直发免入库后置 |
| 11 | 发染入口 | 胚布单侧=列表页行操作→抽屉（无工作台）；染色单侧=workspace 页签（同进度反馈页签先例） |
| 12 | 入库区分 | `greigeOrderId` 非空=胚布入库、`dyeingOrderId` 非空=成品入库；约定不同时填、不强校验 |
| 13 | 取消单 | 一律排除 CANCELLED：合同不进默认列表，各环节只统计非取消单据 |
| 14 | 性能 | 实时批量聚合（IN+GROUP BY，无 N+1）；无定时任务/物化/缓存 |
| 15 | 错误码 | `1-030-018` 段=发染记录；`1-030-019` 段=生产进度（只读预留） |

## 一、全链路业务环节对照

| 环节 | 旧系统实体 | 新系统实体 | 状态 |
|------|-----------|-----------|------|
| 合同签订 | SalesContract | SalesContract | 已有 |
| 胚布下单 | GreigeInstruction | GreigeOrder | 已有 |
| 织造执行 | (无独立实体) | WeavingOrder + WeavingTicket | 已有 |
| **胚布发染厂** | **FabricPullRecord（挂订单）** | **GreigeDispatchRecord（挂胚布单，ADR-0033）** | **本期新建** |
| 染色下单 | DyeingInstruction | DyeingOrder | 已有 |
| 染色进度 | DyeingVat + DyeingVatProgress | ProgressFeedback / ProgressVat | 已有（ADR-0031） |
| 检验 | InspectionInstruction | InspectionOrder | 已有 |
| 入库 | StockIn (挂指示单) | StockIn (挂合同/胚布/染色) | 已有 |
| 出货 | Shipment (挂合同) | Shipment (挂合同) | 已有（SHIPMENT_OUT 已过账） |
| **合同全链路进度** | OrderProgress (手动填) + useContractProgress (前端聚合) | ProductionProgressQueryService（后端聚合，ADR-0034） | **本期新建** |
| 损耗分析 | LossRecord + DyeingVat.inputWeight/outputWeight | 无 | 暂缓 |
| 排产建议 | 智能排单 (AI) | 无 | 暂缓 |

## 二、设计方案

### 1. 不新建"订单/进度跟踪"实体

**理由**：再建一个独立订单实体等于换皮重做旧系统问题——要么存进度字段（重蹈手动字符串漂移覆辙），要么反向引用生产单据（引入双向耦合）。进度不是"另一张单"，而是合同下各生产单据状态的投影（ADR-0034）。

### 2. 实体关系：合同锚点 + 单层关联

```
SalesContract（唯一锚点）
├─ GreigeOrder (salesContractId + contractNo)
│   ├─ GreigeOrderColor
│   ├─ WeavingOrder (仅 SELF 模式, greigeOrderId)
│   │    ├─ MachineTask
│   │    ├─ WeavingTicket ★布票状态回写织造单/机台任务（唯一例外, ADR-0024）
│   │    └─ YarnRequisition
│   └─ GreigeDispatchRecord（发染记录，ADR-0033）★创建/删除写 PROCESS_OUT 流水
│
├─ DyeingOrder (salesContractId + contractNo)
│   ├─ DyeingColor（稳定 ID, ADR-0032）
│   └─ ProgressFeedback / ProgressVat（缸级覆盖层, 零回写染色单, ADR-0031）
│        └─ ProgressNote
│
├─ InspectionOrder
│   ├─ type=GREIGE  → greigeOrderId
│   └─ type=FINISHED → dyeingOrderId
│
├─ StockIn（contract/greige/dyeing/inspection 全可空, 零回写）
│   └─ StockInItem + Placement → 写 STOCK_IN 流水
│
└─ Shipment (salesContractId)
    └─ ShipmentItem + Roll → 写 SHIPMENT_OUT 流水（ADR-0029，已实现）

ProductionProgressView（只读聚合 DTO，不是表）← 聚合以上全部
```

**关联方式**：ID + 名称快照（项目惯例），不用文本业务键（旧系统弱关联坑根除）。

### 3. 胚布发染厂环节：GreigeDispatchRecord（ADR-0033）

#### 3.1 与旧系统 FabricPullRecord 对比

| 维度 | 旧系统 FabricPullRecord | 新系统 GreigeDispatchRecord |
|------|------------------------|---------------------------|
| **挂载点** | OrderProgress（orderId 外键） | GreigeOrder（greigeOrderId 外键） |
| **粒度** | 产品级（productKey 文本复合键） | 胚布单整单一条（产品快照，颜色不细分；颜色级血缘后置） |
| **库存影响** | 直接扣减可用量（不写流水） | 写正式 PROCESS_OUT 流水（库存闭环）+ 不足拦截 |
| **删除** | 软删，权限控制 | 软删 + PROCESS_OUT_REVERSAL 红冲（关联原流水） |
| **编辑** | 可改 | 受限编辑：仅染色单/染厂/备注；数量日期匹数仓库锁定 |
| **关联方式** | orderId + productKey 文本 | greigeOrderId/dyeingOrderId 外键（ID 关联） |
| **编号** | 无 | 无（靠"胚布单+日期"识别，仿 ADR-0009 先例） |

#### 3.2 实体定义

```
GreigeDispatchRecord（发染记录）— 轻量记录，非正式单据
├─ greigeOrderId        — 必填，挂胚布单（外键）
├─ dyeingOrderId        — 可空，指向具体染色单（可后续补填）
├─ dyeingFactoryId/Name — 可空，承染染厂（供应商 ID+名称快照，可补填）
├─ warehouseId/Name     — 必填，出库仓库（弹窗选择+快照；红冲沿用同一仓库）
├─ dispatchDate         — 发染日期（必填，创建后锁定）
├─ quantity             — 数量 KG（>0，创建后锁定）
├─ pieces               — 匹数（>0，创建后锁定）
├─ productName/productCode/composition/width/weightGsm — 产品快照（取胚布单品种）
├─ remark               — 备注（可改）
├─ createdBy/updatedBy  — 操作人
└─ 标准 BaseDO 字段（tenantId, deleted, createTime, updateTime）

状态机：无（创建即生效，删除即红冲）    审批流：无    编号：无
```

补充规则：
- **一张胚布单可多次发染**（多条记录，分批发运是常态），该单已发染量 = 有效记录合计；发染日期缺省当天
- **补填/修改染色单时校验同合同**：染色单的 salesContractId 必须与胚布单一致，不一致报错（防进度血缘跨合同错乱）
- 仓库仅可选**启用仓**（停用仓不可被新单选择，同仓库档案守卫）
- 染色单侧新增时先选胚布单（仅可选非取消单），带出产品快照；弹窗内提示该仓该产品当前可用量，超出即被后端拦截

#### 3.3 库存流水与负库存拦截

| 动作 | 流水类型 | 数量符号 | 关联凭证 |
|------|---------|---------|---------|
| 创建发染记录 | `PROCESS_OUT` | 负数（扣减胚布库存） | related_doc_type=GREIGE_DISPATCH, related_doc_id=记录ID |
| 删除发染记录 | `PROCESS_OUT_REVERSAL` | 正数（红冲恢复） | 关联原 PROCESS_OUT 流水 |

- 流水携带：仓库（弹窗所选）、产品ID、合同ID 从胚布单带出（胚布单有 salesContractId）、单位 KG。
- **不足拦截**：同仓同产品 KG 余额（`SUM(quantity)`，ADR-0017 余额口径）< 发染量时报错，返回需求量/可用量（同领纱发纱 ADR-0027）。
- **直发场景**：坯布从坯布厂直发染厂、不进本厂仓的，须先补入库单（采购/生产入库均可）再发染；"直发免入库"（发染不写流水仅登记物流）为后置扩展。

#### 3.4 不回写

不回写 GreigeOrder 状态、不回写 DyeingOrder 状态（零回写惯例）；仅写库存流水。

#### 3.5 前端入口（双入口）

1. **胚布单列表页 → 行操作"发染记录"→ 抽屉**：该单的发染记录列表 + 新增 + 删除（胚布单当前无工作台，选抽屉模式，改动最小；后续建工作台可迁成页签）
2. **染色单工作台 → "发染记录"页签**：该单收胚记录列表 + 新增（新增时选择胚布单）+ 删除

两入口复用同一弹窗和同一套接口。染色单侧只读胚布单信息，不回写。

### 4. 合同全链路进度视图（只读聚合服务，ADR-0034）

新增 `ProductionProgressQueryService` 返回 `ProductionProgressView` DTO（**不是表**），多个界面共用。

#### 4.1 视图数据结构

参考旧系统 `oldweb/lib/dashboard/contractProgressTypes.ts` 的 `ContractProgress` 类型，用新系统实体重新设计：

```
ProductionProgressView {
  // 合同头
  contractId, contractNo, customerName, salespersonName, deliveryDate, status

  // 全链路环节进度
  greige: {           // 胚布环节
    demandKg          // 合同需求量（KG 明细合计）
    plannedKg         // 胚布单计划投胚量合计（planned_qty）
    stockedInKg       // 胚布入库量（= stock.greigeInKg，同源复用）
    sentToDyeingKg    // 已发染厂量（发染记录表聚合，ADR-0033）
    weavingProgress   // 自织进度（WeavingTicket 完工/计划，仅 SELF 模式胚布单；OUTSOURCE 显示"外发"）
  }
  dyeing: {           // 染色环节
    demandKg          // 合同需求量
    plannedKg         // 染色单投染量合计（dyeing_input_qty，KG 行）
    vatCoverage       // 排产覆盖率（ProgressVat 合计 / 颜色行染色数量，ADR-0031 已有口径）
    vatStatusSummary  // 缸状态汇总（未排/已排/染色中/固色中/质检中/已完成 各几缸）
    delayedVatCount   // 延期缸数（进度反馈"延期"判定复用）
  }
  inspection: {       // 检验环节
    totalItems        // 录检明细总数
    gradeSummary      // 等级汇总（A/B/C/DEFECTIVE 各几条，字典 lunaerp_inspection_grade）
  }
  stock: {            // 入库环节
    greigeInKg        // 胚布入库量（STOCK_IN 流水 join 入库单，greigeOrderId 非空）
    finishedInKg      // 成品入库量（同上，dyeingOrderId 非空）
  }
  shipment: {         // 出货环节
    shippedKg         // 已发货量（SHIPMENT_OUT 流水 join 发货单取合同）
    unshippedKg       // 合同未发量（demandKg - shippedKg，GLOSSARY.md ContractUnshippedQuantity 同口径）
    shipmentCount     // 发货单数
  }
  alerts: string[]    // 预警（见 4.4）
}
```

#### 4.2 聚合方式：join 反查流水（不给 ledger 加列）

`lunaerp_stock_ledger` 无 `sales_contract_id` 列，**决策：不加工列**（schema 变更+历史回填成本高），按 `related_doc_type + related_doc_id` join 源单取合同：

| 指标 | 数据源 | 聚合口径 |
|------|--------|---------|
| demandKg | sales_contract_item | Σ quantity WHERE unit='KG'；M 行不进合计 |
| plannedKg（胚布） | greige_order | Σ planned_qty WHERE sales_contract_id=? AND status≠CANCELLED |
| greigeInKg | stock_ledger ⨝ stock_in | Σ quantity WHERE txn_type='STOCK_IN' AND stock_in.greige_order_id IS NOT NULL（join 取合同） |
| finishedInKg | stock_ledger ⨝ stock_in | 同上，dyeing_order_id IS NOT NULL |
| sentToDyeingKg | greige_dispatch_record ⨝ greige_order | Σ quantity（记录表直聚，不走流水） |
| weavingProgress | weaving_order ⨝ weaving_ticket | SELF 胚布单关联织造单：完工票/计划（复用胚布单详情既有只读进度口径） |
| plannedKg（染色） | dyeing_color | Σ dyeing_input_qty WHERE unit KG |
| vatCoverage / 缸状态 / 延期 | progress_feedback（ProgressVat） | 复用进度反馈既有口径（ADR-0031） |
| inspection | inspection_order + inspection_order_item | COUNT 明细 + grade 分组 |
| shippedKg | stock_ledger ⨝ shipment | Σ ABS(quantity) WHERE txn_type='SHIPMENT_OUT' AND shipment.sales_contract_id=?（join 取合同） |
| unshippedKg | demandKg − shippedKg | 只读动态计算，不落库 |

**录入约定**：入库单 greigeOrderId 与 dyeingOrderId 不同时填（不强校验，不改现有 StockIn 行为）；异常双挂数据会双计，靠约定规避。

#### 4.3 单位与取消单口径

- **单位**：进度只聚合 KG；M 单位合同/明细原样展示、不进合计，不做克重折算。
- **取消单**：一律排除 CANCELLED——已取消合同不进默认列表（可切筛选查看）；各环节只统计非取消单据的数量（胚布/染色/检验/入库/发货同理）。

#### 4.4 预警（ProgressAlert）三条

1. **临期**：交期距今 ≤7 天且仍有未发量（unshippedKg > 0）
2. **超期**：交期已过且仍有未发量
3. **延期缸**：该合同染色单下存在延期反馈缸（延期判定复用进度反馈"延期"口径：缸交期取染色单交货日期）

排产超量、头缸延误等规则后置；预警只读计算，不在视图上手工维护。

#### 4.5 API 与消费方

- `GET /lunaerp/production/production-progress/page`：跨合同列表（合同头+各环节关键量+预警数），支持关键字（合同号/客户）筛选、交期排序、默认按交期升序
- `GET /lunaerp/production/production-progress/get?id=`：单合同全链路视图（含环节明细）

**page 列表列**：合同号、客户、业务员、交期（无交期排最后，同旧系统 deliveryParsed 处理）、合同状态、demandKg、plannedKg（胚布/染色）、sentToDyeingKg、vatCoverage、greigeInKg/finishedInKg、shippedKg/unshippedKg、预警数。**筛选**：关键字（合同号/客户）、交期范围、状态（默认排除 CANCELLED 可切换）、仅看有预警。

**单合同抽屉结构**：头部合同信息+预警标签；六环节分区卡片（环节进度条 + 关键数量对照）；环节内可下钻关键明细——发染记录列表、缸状态汇总、检验等级汇总、最近入库/发货（MVP 先做汇总与发染/缸明细，其余明细下钻随用随加）。

消费方：
- **本期**："生产管理 → 生产进度"独立菜单页（列表 + 单合同抽屉详情）
- **后置**：销售合同工作台"生产进度"页签（合同工作台尚不存在，需先建骨架）；生产进度看板 widget（对标旧系统 ContractProgressWidget，page 接口可直接复用）；合同时间线、客户门户（按 customerId 过滤即可复用）

#### 4.6 性能策略

`page` 按页内合同 ID 集合逐环节**批量聚合**（IN + GROUP BY，避免逐合同 N+1 查询）；不做定时任务、物化表、缓存。数据量增长后再引入物化/缓存，不影响口径。

### 5. 零回写原则适用边界

- **适用**：入库不回写胚布/染色状态、发货不回写合同状态、进度反馈不回写染色单、发染记录不回写胚布单/染色单
- **唯一例外**：布票 → 织造单/机台任务状态（布票是织造执行粒度，状态需自动推导，ADR-0024）
- **合同侧不加 `progress` 字段**（旧系统 SalesContract.progress 是手动字符串，新系统不引入）

### 6. 暂缓项

- **合同工作台"生产进度"页签**：合同工作台骨架未建，后置；复用 `get` 接口
- **生产进度看板 widget**：对标旧系统 ContractProgressWidget（交期近/未排多/超期过滤排序），后置；复用 `page` 接口
- **直发免入库**：发染记录标记"直发"不写流水，后置
- **颜色级发染血缘**：发染记录挂 greigeOrderColorId 与染色颜色行对齐，后置
- **损耗分析**：旧系统按缸卡记录投入/产出重量计算损耗率；进度反馈已明确不做（ADR-0031），未来可在 ProgressVat 扩展或入库/出库量反算
- **排产建议/智能排单、晨会排雷简报（AI）**：`alerts` 字段已预留扩展点
- **客户门户**：`ProductionProgressQueryService` 按 customerId 过滤即可复用

## 错误码

| 段 | 模块 | 说明 |
|----|------|------|
| `1-030-018-xxx` | 发染记录 | 不足拦截（返回需求量/可用量）、上游单据不存在/已取消、受限编辑校验等 |
| `1-030-019-xxx` | 生产进度查询 | 只读接口，预留（合同不存在等） |

（`1-030-015` 织造执行、`1-030-016` 发货单、`1-030-017` 进度反馈已占用。）

## 落地步骤（MVP 核心链路）

1. **不新建**"订单/进度跟踪"表，不加 `SalesContract.progress` 字段，**不给** `lunaerp_stock_ledger` 加合同列
2. **新建 GreigeDispatchRecord**（ADR-0033）：建表 SQL + DO + Mapper + Service + Controller，落 lunaerp 模块 `production` 子包；`StockLedger` 新增 `PROCESS_OUT` / `PROCESS_OUT_REVERSAL` 枚举值；创建/删除同事务过账、不足拦截
3. **新增 ProductionProgressQueryService** + `ProductionProgressView` DTO（ADR-0034）：page + get 两接口，join 反查口径
4. **前端独立"生产进度"页**：`views/lunaerp-elmui/production/production-progress/`（列表 + 单合同抽屉），路由/菜单挂"生产管理"目录
5. **前端发染记录双入口**：胚布单列表行操作抽屉 + 染色单工作台"发染记录"页签；`api/lunaerp-elmui/production/greige-dispatch-record`
6. 菜单与权限 SQL：`lunaerp:production-progress:query`、`lunaerp:greige-dispatch-record:query|create|delete`
7. 错误码 `1-030-018` / `1-030-019` 段注册
8. 单元测试（仿 ProgressFeedbackServiceImplTest 先例）：发染过账/红冲/不足拦截、同合同校验、受限编辑校验、聚合口径（KG 过滤、CANCELLED 排除、join 反查、预警三条）

## 验证方式

- "生产管理 → 生产进度"页能展示跨合同列表（按交期排序、关键字筛选），单合同抽屉展示全链路六环节数据
- 各环节数据口径唯一：与胚布单/染色单/检验单/入库/发货各单据侧展示值一致（同一数据源）
- 染色环节数据来自进度反馈覆盖层，其余环节自动从执行数据聚合；不存在任何"手动改进度字符串"的操作
- 创建发染记录后库存余额减少、余额不足被拦截；删除后红冲恢复——库存账闭环
- 发染记录可从胚布单列表抽屉和染色单工作台页签两个入口查看和操作，数据一致
- M 单位合同不计入 KG 合计；CANCELLED 合同与单据不进统计
