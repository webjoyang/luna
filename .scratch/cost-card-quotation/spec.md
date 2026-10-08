# 售前链 - 成本卡与智能报价（纱价主档 + 成本卡 + 报价单版本/有效期/转合同）

Status: ready-for-agent
波次：W1 售前链（依赖 common-foundation W0 通用件、sales-contract-mvp）

## Problem Statement

售前链缺"成本→报价→成交"的定价闭环：

1. **纱价无主档**：纱线规格表 `lunaerp_production_yarn_spec` 只有一个裸 `ref_price`，无价类、无生效日期、无历史；成本测算无法回答"按哪天的纱价算的"。
2. **成本知识散落在产品档案**：产品表上有加工费三列/包装费/税率与 `cost_items` JSON，纱线成本纯函数只在**前端**（[product-yarn-cost.ts](../../luna-ui/src/views/lunaerp/baseinfo/product/components/product-yarn-cost.ts)，ADR-0035），只覆盖纱线+织造/针织费，**染色/定型/检验/运费缺失**，服务端无权威算价、金额不可测；同时 ADR-0011 的 `yarn_recipe_bom` 结构化配比与产品 `yarn_specs` JSON 双轨并存。
3. **无报价单**：老系统（linaagi QuotationRecord/Item）只有"成本快照+固定加成"的简表，无版本、无有效期、无客户专属价、无转合同；新栈销售合同（`business.salescontract`）没有报价来源。
4. 老系统 AI 一键核价/爬虫行情/核价申请流程本期不做（Q5=A 规则型；核价申请二期）。

业务需要：维护带生效日期的纱价；以**成本卡**（某产品某时点的单位成本凭证）为报价取数源；报价单支持多版本、有效期、按**真毛利率**反算售价、外币快照汇率；CONFIRMED 报价可一键预填生成销售合同。成本/毛利按 W0 `@Sensitive` 脱敏，报价单提供不含底牌的对外套打。

## Solution

在 luna-srv `lunasrv-module-lunaerp` 落地：

- 生产域新增**纱价表** `lunaerp_production_yarn_price`（规格 + 价类 + 生效日期 + 历史），维护入口在既有纱线档案页内（抽屉），不设一级菜单；
- 业务域新增**成本卡**聚合 `lunaerp_business_cost_card` + 纱线行/费用行两张结构化子表，状态机 DRAFT/EFFECTIVE/EXPIRED，同产品同时仅一张生效卡，生效即冻结；
- 业务域新增**报价单**聚合 `lunaerp_business_quotation` + 明细行，同号多版本，状态 DRAFT/CONFIRMED/SUPERSEDED/CANCELLED + 有效期派生"已失效"展示；
- 三模式算价公式从前端 TS 移植为**后端 Java 纯函数 + 试算接口**（BigDecimal、金额 4 位小数、金额逻辑全测试），`yarn_recipe_bom_item` 补齐 5 个测算列，成为唯一配比权威源；产品 `yarn_specs` JSON 与前端 TS 函数冻结（保留代码与测试，不再是权威算价路径，见 ADR-0039）；
- 客户表加默认毛利率、销售合同表加报价来源两列；报价单「生成销售合同」= 预填新合同编辑页，可多次转、不占数量；
- 报价单注册 W0 固定套打模板（**打印件永不出现成本/毛利**）；成本卡 DRAFT、报价单 DRAFT、纱价记录注册 W0 统一回收站 handler；纱价注册 W0 Excel 导入模板。

## User Stories

### 纱价主档（生产域）

1. 作为成本管理员，我想在"生产管理→纱线档案"某规格上打开「价格记录」抽屉，分采购参考价/市场参考价两页维护价格（元/KG、生效日期、备注、来源），以便所有成本测算按同一主档取价。
2. 作为成本管理员，我想同一规格同一价类同一生效日期只能有一条记录，保存撞日得到明确报错，以便价格历史不重叠。
3. 作为成本管理员，我想取价永远取"不晚于今天的最近生效价"，并能在抽屉里看到完整历史曲线（日期+价格），以便掌握涨跌。
4. 作为成本管理员，我想用 Excel 批量导入纱价（仅新增、全量校验、错误行回 Excel，W0 导入规范 5000 行上限），以便月初集中调价。
5. 作为成本管理员，我想删除错误价格走软删并在回收站恢复，以便误删可救。
6. 作为系统，我想在纱线采购单已有实际采购单价的情况下，于价格抽屉提供"最近采购价"只读聚合参考（取最近一条已审核采购明细单价），但不自动回写任何价格字段，以便人工判断后录入。

### 成本卡建模与测算

7. 作为成本管理员，我想新建成本卡时先选产品（远程搜索），选中后带出产品快照（编号/品名/成分/门幅/克重/织物结构/估算模式/税率）与其纱线配比 BOM，并按卡头选定的价类（缺省采购参考价）自动取当日纱价，以便一键成卡。
8. 作为成本管理员，我想产品没有配比 BOM 或 BOM 无明细时被明确拦截（"该产品无有效配比 BOM，请先在配比管理维护"），而不是算出一张 0 成本卡。
9. 作为成本管理员，我想纱线行展示：纱线规格（材质+规格名快照）、取价/价类/生效日期、比例%、方向（经纬精算）、密度、织缩率%、损耗率%、每米用量、行贡献额（每 KG/每 M 双口径），草稿态可改可用价/参数并「重新取价」。
10. 作为成本管理员，我想费用行按六项维护（织造/染色/定型/检验/包装/运费，字典可扩充），每行选计费口径（元/KG、元/M）、单价、损耗率%；费用可缺行，运费可空，以便按工厂实际报价组装。
11. 作为成本管理员，我想选产品时用产品档案现有针织加工费/织造费/其他加工费/包装费预填初始费用行，预填后可任意增删改，以便不重复录入。
12. 作为成本管理员，我想卡片实时展示：纱线成本合计、费用合计、不含税/含税单位成本（每 KG、每 M 两口径，无法换算的口径显示"—"）、缺价纱线数；测算既有保存动作的服务端重算，也有不落库的 `/trial` 试算接口供产品档案成本分区与编辑页实时调用。
13. 作为成本管理员，我想针织卡（basis=KG）上录元/M 费用、梭织卡（basis=METER）上录元/KG 费用时系统按每米重量自动换算，缺克重/门幅无法换算时拦截该费用行并提示。
14. 作为成本管理员，我想保存草稿、继续编辑、删除草稿（进回收站）；草稿编号可自动取号（`CC20260001`）或手填（撞号报错）并能预览下一号。
15. 作为成本管理员，我想点「生效」时系统校验不存在缺价纱线，校验通过后同事务把该产品旧生效卡置为失效；生效后卡片全部字段冻结不可改，要调价只能「复制新建」生成草稿重新取价编辑再生效。
16. 作为成本管理员，我想对生效卡手工「失效」（无替代主动停用）；生效/失效/冻结态在列表一目了然，失效卡只读留痕。
17. 作为成本管理员，我想在成本卡列表按卡号/产品编号/品名关键词、状态、产品筛选，默认按生效日期+更新时间倒序分页。

### 报价单（版本/有效期/毛利反算/外币）

18. 作为销售，我想新建报价单选客户后带出客户名称/联系人/电话快照与客户默认毛利率（客户档案无则用全局缺省 15%），选产品后自动带出该产品当前 EFFECTIVE 成本卡并整份快照（卡号/取价日期/双口径成本/纱线费用明细），行成本来源标记为"成本卡"。
19. 作为销售，当产品没有生效成本卡时，我仍可把该行加入报价：成本价为 0 并亮"无成本卡"警示、毛利率不显示；有成本编辑权限（amount 脱敏位的反面，即未被脱敏）的人可手填成本价，行来源标记"手工"。
20. 作为销售，我想每行录入目标毛利率后系统按 `报价单价=成本/(1−目标毛利率)` 反算单价；我直接改单价时系统反算实际毛利率 `1−成本/报价`；毛利率输入范围 (−100%, 100%)，等于 100% 数学不可反算，拦截报错。
21. 作为销售，我想行金额=数量×单价、行成本额=数量×折报价币成本，全部由服务端权威重算；单头总数量、总金额、总成本额、整单毛利率（金额加权 `(总金额−总成本)/总金额`）由服务端重算，我传入的汇总值一律忽略。
22. 作为销售，当行毛利率或整单毛利率低于系统阈值（infra_config 单键 `quotation.min-margin-rate`，缺省 0%）时，我想看到红字低价预警但不被阻断保存。
23. 作为销售，我想做外销报价时选币种（复用合同币种字典，缺省 RMB）与汇率（非 RMB 必填，手工录入，含义=1 单位外币兑人民币，保存时快照不随后续波动）；RMB 成本按汇率折外币后在同币种内计算毛利；无 W3 汇率主档前不自动带汇率。
24. 作为销售，我想报价单有报价日期、有效期自（缺省今天）、有效期至（可空=长期）；过期的已确认单在列表显示"已失效"派生徽章，转合同时弹警告但不硬锁。
25. 作为销售，我想保存草稿后「确认报价」（DRAFT→CONFIRMED，一期无审批流）；只有最新 CONFIRMED 版本可以「修订」：复制头行生成同号新版本草稿（version+1），旧版同事务置"已修订"只读；同一报价号同时至多一张草稿。
26. 作为销售，我想页面把编号展示为 `QT20260001-v2`，可查看同号全部历史版本（只读）；已修订/已取消版本不可编辑不可删除；只有草稿可删除（进回收站）。
27. 作为销售，我想对最新已确认版本「取消报价」（→CANCELLED 终态留痕）。
28. 作为销售，我想在最新已确认报价单上点「生成销售合同」：系统校验状态与版本后，把报价头（客户/联系人/币种/贸易类型缺省内销）与明细（产品编号/品名/色号/成分/门幅/克重/数量/报价单价）预填进销售合同新建页，我仍可增删改后保存；合同保存回写报价来源。
29. 作为销售，一张报价单我想允许分批多次转合同，系统不锁数量、不追踪已转量；转合同预填的币种与报价一致（合同页不可改币种，只能改价与量）。
30. 作为销售，我想对已确认报价单做固定套打（客户抬头/报价号-版本/有效期/币种/产品规格行/数量/单价/金额/备注，签章区），打印件永远不出现成本与毛利；每次打印写操作日志。
31. 作为没有成本/毛利可见权限的销售，我在报价列表与详情接口拿到的数据里不含成本价、成本额、毛利率、成本快照字段（服务端直接抹除，非仅前端隐藏），但报价单价与金额正常可见。
32. 作为销售，我想按报价号/客户名关键词、状态（含"已失效"筛选项）、客户筛选报价列表，默认报价日期倒序；列表默认只显示每个报价号的最新版本，可勾选"包含历史版本"。

### 管理与分权

33. 作为系统管理员，我想执行一份可重复执行的 SQL 脚本完成：新表 DDL、三表 ALTER（BOM 行加列/客户加列/合同加列）、菜单与按钮、7 个字典初始化、infra_config 缺省键，部署零手工配置。
34. 作为系统管理员，我想成本卡与纱价维护权限默认不授予销售角色；报价的成本/毛利可见性挂 W0 `@Sensitive` amount 权限位，不再新建独立脱敏位。
35. 作为开发，成本卡/报价单/纱价全部 DO 继承 TenantBaseDO，写接口有租户隔离测试，横向（跨租户）与纵向（跨权限位）越权必测。

## Data Model

命名与基类约定：全部新表继承 TenantBaseDO（id/creator/create_time/updater/update_time/deleted/tenant_id），主键 int8 + 独立序列，金额 `numeric(18,4)`（每米用量/汇率/比率等小数量级见各列），不建数据库外键，仅逻辑关联。SQL 单文件：`luna-srv/sql/postgresql/lunaerp-business-cost-card-quotation.sql`。

### 新表 1：纱价 `lunaerp_production_yarn_price`（生产域）

| 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|
| id | int8 | - | 序列 `lunaerp_production_yarn_price_seq` |
| yarn_spec_id | int8 | ✓ | 关联 `lunaerp_production_yarn_spec.id` |
| yarn_type_id | int8 | ✓ | 冗余材质 id（列表分组/查询用，取规格父级回填） |
| spec_name | varchar(128) | ✓ | 规格名称快照（防规格改名后历史价不可读） |
| price_type | varchar(16) | ✓ | 字典 `lunaerp_yarn_price_type`：PURCHASE 采购参考价（缺省）/ MARKET 市场参考价 |
| price | numeric(18,4) | ✓ | 单价，元/KG，必须 >0 |
| effective_date | date | ✓ | 生效日期 |
| source | varchar(16) | ✓ | HAND 手工（缺省）/ EXCEL 导入 / PURCHASE_AGG 采购聚合（一期聚合只展示不落此表；预留枚举） |
| remark | varchar(256) | - | 备注 |

索引：唯一索引 `(tenant_id, yarn_spec_id, price_type, effective_date)` **普通唯一（含软删行）**——价格史按日期连续，软删后允许重新插入同日记录会造成取价分叉，刻意与业务编号的"含删唯一"不同：此处用部分唯一索引 `WHERE deleted=0`，同一天删错可重录；取价查询走 `(yarn_spec_id, price_type, effective_date DESC)`。
取价规则：`price_type` 内取 `effective_date <= 当日` 的未删最近一条；无记录回退 `yarn_spec.ref_price` 并在响应中标记 `price_source=SPEC_REF_FALLBACK`（成本卡缺价统计：回退价**不计** missing，但行上明示来源，供成本员判断）；连 ref_price 都没有才计缺价。

### 新表 2：成本卡主表 `lunaerp_business_cost_card`

| 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|
| id | int8 | - | 序列 `..._cost_card_seq` |
| card_no | varchar(32) | ✓ | 成本卡号 `CC{yyyy}NNNN`，全量唯一含软删行（ADR-0004 模式） |
| product_id | int8 | ✓ | 产品档案逻辑关联 |
| product_code/product_name/composition | varchar 快照 | ✓/✓/- | 选品时复制，不回写 |
| width_snapshot/weight_snapshot | varchar(64) | - | 门幅/克重文本快照（对齐合同明细快照风格） |
| fabric_construction | varchar(16) | ✓ | KNIT/WOVEN，来自产品 |
| woven_calculation_mode | varchar(16) | - | SIMPLE/PROFESSIONAL（梭织必填） |
| yarn_price_type | varchar(16) | ✓ | 本卡取价价类，缺省 PURCHASE |
| greige_weight_gsm | numeric(18,2) | - | 坯布克重（梭织简易；针织换算取产品技术规格克重，落同列按结构解释） |
| greige_width_cm | numeric(18,2) | - | 坯布门幅 CM |
| kg_per_meter | numeric(18,6) | - | 每米重量 KG/M = 克重×门幅/100000（服务端算） |
| tax_rate | numeric(9,6) | ✓ | 税率，选品预填可改（缺省 0） |
| basis | varchar(8) | ✓ | KG（针织）/METER（梭织） |
| yarn_cost_per_kg | numeric(18,4) | ✓ | 纱线成本 元/KG |
| yarn_cost_per_meter | numeric(18,4) | - | 纱线成本 元/M（针织换算可得则填，否则 null） |
| total_fee_per_kg | numeric(18,4) | ✓ | 费用合计 元/KG（缺省 0） |
| total_fee_per_meter | numeric(18,4) | - | 费用合计 元/M |
| unit_cost_per_kg | numeric(18,4) | ✓ | 不含税单位成本 元/KG |
| unit_cost_per_kg_with_tax | numeric(18,4) | ✓ | 含税单位成本 元/KG |
| unit_cost_per_meter | numeric(18,4) | - | 不含税 元/M（不可算为 null） |
| unit_cost_per_meter_with_tax | numeric(18,4) | - | 含税 元/M |
| missing_price_count | int4 | ✓ | 缺价纱线行数（服务端重算） |
| effective_date | date | - | 生效日期，生效动作时落当天 |
| status | varchar(16) | ✓ | DRAFT 草稿（缺省）/EFFECTIVE 生效/EXPIRED 失效 |
| remarks | varchar(1000) | - | 备注 |

索引：卡号普通唯一 `(tenant_id, card_no)`（含删）；**部分唯一索引** `(tenant_id, product_id) WHERE status='EFFECTIVE' AND deleted=0`（同产品同时仅一生效卡，兜底并发）；普通索引 `(product_id)`、`(status)`。

### 新表 3：成本卡纱线行 `lunaerp_business_cost_card_yarn`

| 字段 | 类型 | 备注 |
|---|---|---|
| id | int8 | 序列 `..._cost_card_yarn_seq` |
| card_id | int8 | 所属成本卡 |
| line_no | int4 | 保存时按数组重排 |
| yarn_type_id / yarn_spec_id | int8 | 配比 BOM 关联（强关联，不建外键） |
| yarn_spec_name | varchar(128) | 规格名快照 |
| direction | varchar(16) | GENERAL/WARP/WEFT（经纬精算） |
| ratio | numeric(9,4) | 配比百分比数字（49=49%） |
| density | numeric(18,2) | 密度（经纬精算参数） |
| crimp_rate | numeric(7,2) | 织缩率% |
| waste_rate | numeric(7,2) | 投入损耗率% |
| consumption_kg_per_meter | numeric(18,6) | 每米用量 KG/M（经纬精算） |
| price | numeric(18,4) | 取价快照 元/KG（缺价为 0） |
| price_type | varchar(16) | 本行实际价类（含回退来源标记，落 `price_source`：PRICE_TABLE/SPEC_REF_FALLBACK/MANUAL） |
| price_effective_date | date | 所取价格的生效日期（回退/手填为 null） |
| contribution_per_kg | numeric(18,4) | 行贡献 元/KG（含损耗，服务端重算） |
| contribution_per_meter | numeric(18,4) | 行贡献 元/M（不可算为 null） |

索引：`(card_id)`。

### 新表 4：成本卡费用行 `lunaerp_business_cost_card_fee`

| 字段 | 类型 | 备注 |
|---|---|---|
| id | int8 | 序列 `..._cost_card_fee_seq` |
| card_id / line_no | int8/int4 | 所属卡/行序 |
| fee_type | varchar(24) | 字典 `lunaerp_cost_card_fee_type`（2264）：WEAVING 织造/DYEING 染色/SETTING 定型/INSPECTION 检验/PACKAGING 包装/FREIGHT 运费（字典可扩充，后端不写死枚举集合，公式只认 fee_unit） |
| fee_unit | varchar(10) | PER_KG / PER_METER（字典 `lunaerp_cost_fee_unit`，2265） |
| price | numeric(18,4) | 单价，必须 ≥0 |
| loss_rate | numeric(7,2) | 投入损耗率%，缺省 0 |
| contribution_per_kg / contribution_per_meter | numeric(18,4) | 双口径行贡献（不可算口径 null） |
| remark | varchar(256) | 可空 |

索引：`(card_id)`。费用行结构化（不沿用产品档案 `cost_items` 自由 JSON），因为服务端要权威重算与汇总。

### 新表 5：报价单主表 `lunaerp_business_quotation`

| 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|
| id | int8 | - | 序列 `..._quotation_seq` |
| quotation_no | varchar(32) | ✓ | `QT{yyyy}NNNN`，同号多版本共享 |
| version_no | int4 | ✓ | 版本号，首版 1 |
| customer_id | int8 | ✓ | 客户档案逻辑关联 |
| client_name | varchar(128) | ✓ | 客户名快照 |
| client_contact / phone | varchar(64/32) | - | 联系人/电话快照 |
| salesperson_user_id | int8 | - | 业务员（=归属人，数据范围 user 列），缺省当前用户可改 |
| dept_id | int8 | - | 归属部门（数据范围 dept 列），服务端按业务员当前部门推导，不接受前端入参（21 号数据范围接线，ADR-0046） |
| quotation_date | date | ✓ | 报价日期，缺省今天 |
| valid_from | date | ✓ | 有效期自，缺省今天 |
| valid_to | date | - | 有效期至，null=长期 |
| currency | varchar(8) | ✓ | 复用 `lunaerp_sales_contract_currency`，缺省 RMB |
| exchange_rate | numeric(18,6) | 条件 | 1 单位外币兑人民币；currency≠RMB 必填且 >0；RMB 时存 1 |
| total_quantity | numeric(18,2) | ✓ | 服务端重算 |
| total_amount | numeric(18,4) | ✓ | 总金额（报价币种），服务端重算 |
| total_cost_amount | numeric(18,4) | ✓ | 总成本额（报价币种），服务端重算；脱敏字段 |
| avg_margin_rate | numeric(9,6) | ✓ | 整单毛利率（金额加权），服务端重算；脱敏字段 |
| status | varchar(16) | ✓ | DRAFT/CONFIRMED/SUPERSEDED/CANCELLED |
| remarks | varchar(1000) | - | |

索引：全量唯一 `(tenant_id, quotation_no, version_no)`（含删）；**部分唯一索引** `(tenant_id, quotation_no) WHERE status='DRAFT' AND deleted=0`（同号至多一草稿）；非唯一索引 `(customer_id)`、`(status)`、`(quotation_date)`。列表"仅最新版本"由窗口查询/按 no 取 max(version_no) 实现。

**数据范围（21 号票接线，ADR-0046）**：报价单主表注册销售归属链 4 档行级数据范围（user 列 `salesperson_user_id`、dept 列 `dept_id`），建表脚本另建两个部分索引 `(tenant_id, dept_id) WHERE deleted=0`、`(tenant_id, salesperson_user_id) WHERE deleted=0`；明细表不注册。新建 owner 缺省登录用户、dept_id 服务端推导；「生成销售合同」草稿 owner 继承报价单（业务员已停用则回退登录用户），合同 dept_id 按其 owner 重新推导，不继承快照；归属向前继承一次，此后各自独立。

### 新表 6：报价明细 `lunaerp_business_quotation_item`

| 字段 | 类型 | 备注 |
|---|---|---|
| id | int8 | 序列 `..._quotation_item_seq` |
| quotation_id / line_no | int8/int4 | |
| product_id | int8 | 可空（允许档案外品种手填） |
| product_code/product_name | varchar | 快照，code 可手填 |
| customer_color/composition/width/weight | varchar(64/128/64/64) | 规格快照，对齐合同明细行 |
| unit | varchar(16) | KG/METER，复用合同单位字典；只能选成本卡可提供口径（针织缺省 KG、梭织缺省 METER） |
| quantity | numeric(18,2) | 数量，缺省 0 |
| cost_card_id / cost_card_no | int8/varchar(32) | 来源卡（手工行可空） |
| cost_source | varchar(16) | CARD 成本卡 / MANUAL 手工（字典 `lunaerp_quotation_cost_source`） |
| cost_snapshot | text(JSON) | 成本卡摘要快照：卡号/取价日期/basis/kgPerMeter/税率/双口径不含税成本/纱线行与费用行明细冗余；脱敏时整字段抹除 |
| cost_unit_price_rmb | numeric(18,4) | 成本单价（人民币，卡快照或手填）；脱敏字段 |
| exchange_rate_snapshot | numeric(18,6) | 行冗余头汇率（修订前历史自解释） |
| cost_unit_price | numeric(18,4) | 折报价币成本单价 = RMB 成本 ÷ 汇率；脱敏字段 |
| target_margin_rate | numeric(9,6) | 目标毛利率（选客户预填，可改行）；脱敏字段 |
| unit_price | numeric(18,4) | 报价单价（报价币种，可由毛利反算或手填） |
| cost_amount | numeric(18,4) | 行成本额=数量×折币成本（服务端重算）；脱敏字段 |
| amount | numeric(18,4) | 行金额=数量×单价（服务端重算） |
| margin_rate | numeric(9,6) | 实际毛利率=1−成本额/金额（成本 0 或金额 0 时 null）；脱敏字段 |
| low_margin_warning | bool | 保存时按 infra_config 阈值计算落标（阈值变更不回刷，列表实时计算徽章亦可） |
| remark | varchar(256) | |

索引：`(quotation_id)`、`(cost_card_id)`（"某卡被哪些报价引用"查询）。

### ALTER 既有表（同一 SQL 脚本，ADD COLUMN IF NOT EXISTS）

1. `lunaerp_production_yarn_recipe_bom_item` 加 5 列：`waste_rate numeric(7,2)`、`direction varchar(16)`、`density numeric(18,2)`、`crimp_rate numeric(7,2)`、`consumption_kg_per_meter numeric(18,6)`，均可空（存量 BOM 默认 null，针织损耗/比例既有列承载）。
2. `lunaerp_baseinfo_customer` 加 `default_margin_rate numeric(9,6)`（可空；空=回退全局 15%）。
3. `lunaerp_business_sales_contract` 加 `source_quotation_id int8`、`source_quotation_no varchar(32)`（均可空；不建外键，仅展示与追溯，不回写报价单状态）。

## 算价规则（后端纯函数，金额逻辑必须测试）

新增 `service/business/costcard/pricing/CostCardPricingCalculator.java`（Spring 无状态组件或静态纯函数类），入参为纱线行/费用行/设置的值对象，出参对齐历史 TS 结果集并扩展双口径费用/税额。BigDecimal 运算，**金额一律 4 位小数四舍五入**（HALF_UP），比率保留 6 位；损耗率负值钳 0、≥100% 钳 99.9%（与前端函数一致）。

1. 每米重量：`kgPerMeter = gsm × widthCm / 100000`。
2. 投入损耗：`毛额 = 净额 ÷ (1 − 损耗率)`，纱线与费用同口径。
3. 纱线成本（basis）：
   - 针织 KG：`yarnCostPerKg = Σ 损耗(price × ratio/100)`；
   - 梭织简易 METER：`blendedPricePerKg = Σ 损耗(price × ratio/100)`，`yarnCostPerMeter = kgPerMeter × blendedPricePerKg`；
   - 梭织经纬精算 METER：`yarnCostPerMeter = Σ 损耗(price × consumptionKgPerMeter)`（按 WARP/WEFT 行独立累加）；
   - 纱线另一口径：针织 `yarnCostPerMeter = yarnCostPerKg × kgPerMeter`（需要克重/门幅，缺失 null）；梭织 `yarnCostPerKg = yarnCostPerMeter ÷ kgPerMeter`（kgPerMeter=0 报错 COST_CARD_KG_METER_CONVERSION_MISSING）。
4. 费用行毛额：`gross = price ÷ (1 − lossRate/100)`；归一：PER_KG 行贡献/kg=gross；PER_METER 行贡献/m=gross；跨口径换算用 kgPerMeter（乘/除），换算条件不满足则该张卡禁止使用此费用行口径组合并报 005。
5. 汇总：`单位成本(不含税) = 纱线成本 + Σ 费用行贡献`（包装/运费均为费用行，参与合计）；`含税 = 不含税 × (1 + taxRate)`；卡头四个单位成本列 + 纱线/费用合计全部由服务端在 create/update/trial/effect 时重算，前端传值忽略。
6. 报价行折币：`costUnitPrice = costUnitPriceRmb ÷ exchangeRate`（汇率=1 外币兑 X 人民币；RMB 时汇率 1）。
7. 毛利反算：`unitPrice = costUnitPrice ÷ (1 − targetMarginRate)`；反手 `marginRate = 1 − costAmount ÷ amount`；整单 `avgMarginRate = (totalAmount − totalCostAmount) ÷ totalAmount`（totalAmount=0 时 null）。
8. 试算接口 `POST /lunaerp/business/cost-card/trial`：body 同保存 VO（允许无 id、无卡号），返回完整测算结果（含双口径、缺价数、各行列贡献），不落库；产品档案"成本结构明细"分区改为调用本接口展示。

与历史实现的关系：[product-yarn-cost.ts](../../luna-ui/src/views/lunaerp/baseinfo/product/components/product-yarn-cost.ts) 三模式公式原样移植（含 4 位小数与损耗钳制），Java 侧用同一组边界用例（空数组/全零/多纱线/含损耗/无价格/超 100% 钳制/经纬分组）保证与既有 TS 单测结果一致；TS 文件与测试保留但标注"公式权威实现已迁移至后端 CostCardPricingCalculator（ADR-0039）"，产品页改调试算接口。

## 状态机

### 成本卡 `status`（白名单 + 终态守卫 + 并发条件更新）

```
DRAFT ──effect──▶ EFFECTIVE ──expire(手工)──▶ EXPIRED(终态)
  │                  ▲
  │              被同产品新生效卡 effect 时同事务顶下 → EXPIRED(终态)
  └─delete──▶ 回收站（仅 DRAFT）
```

- `effect`：条件更新 `UPDATE ... SET status='EFFECTIVE', effective_date=今天 WHERE id=? AND status='DRAFT' AND deleted=0`，影响行=0 报 COST_CARD_STATUS_NOT_ALLOWED；前置校验 missing_price_count=0（报 003）、产品存在有效 BOM（报 004）；同事务把同产品旧 EFFECTIVE 卡条件更新为 EXPIRED；并发双生效由部分唯一索引兜底（捕获唯一约束冲突转 002/003 类业务错）。
- `expire`：仅 EFFECTIVE→EXPIRED，条件更新守卫；EXPIRED 终态只读。
- EFFECTIVE/EXPIRED 不可 update/delete（API 层报 002）；变更价格走「复制新建」（copy 生成 DRAFT 新卡，行数据复制但价格标记为复制时点快照，可重新取价）。

### 报价单 `status`

```
DRAFT ──confirm──▶ CONFIRMED ──revise──▶ 旧版 SUPERSEDED(终态) + 新版 DRAFT(version+1)
  │                   │
  │                   └──cancel──▶ CANCELLED(终态)
  └─delete──▶ 回收站（仅 DRAFT）
```

- 守卫全部用条件更新（WHERE 带当前 status 与 deleted=0），影响行 0 转 QUOTATION_STATUS_NOT_ALLOWED。
- `revise`：仅"同号最大版本且=CONFIRMED"可操作（SELECT FOR UPDATE 同号行或唯一索引串行化）；旧版置 SUPERSEDED，插入 version_no+1 的 DRAFT（头行复制，有效期重置为今天起/至空，成本快照保留并标记"复制自 vN，可重新取价"）；同号草稿唯一性由部分唯一索引兜底（冲突报 QUOTATION_DRAFT_EXISTS）。
- DRAFT 新版 confirm 后成为新最新版；DRAFT 允许删除（进回收站；删除后该号止于 SUPERSEDED，如需继续报价开新号——刻意不做"复活"旧版）。
- CONFIRMED 可 cancel；SUPERSEDED/CANCELLED 全只读、不可删。
- **"已失效"不落状态**：`status=CONFIRMED 且 valid_to < 当天` 的派生徽章（字典保留 EXPIRED 标签值仅供展示，不可写入）；转合同遇失效弹警告，用户确认后继续（不硬锁）。

### 无库存/账务副作用

成本卡与报价单是售前单据，生效/确认/修订/取消**不写库存流水与经营分录账**；副作用仅有状态流转、合同来源回写与操作日志。DoD 中"状态流转副作用测试"在本模块体现为状态机守卫 + 金额重算 + 快照冻结测试。

## API

VO 带 Swagger 与 jakarta.validation；CommonResult/PageResult；权限注解风格对齐 customer/sales-contract。

### 纱价（生产域）`/lunaerp/production/yarn-price`

- `GET /page`：yarnSpecId / yarnTypeId / priceType / 生效日期区间；默认 effective_date DESC
- `GET /list-by-spec?yarnSpecId=&priceType=`：抽屉与取价用（仅未删，按日期倒序）
- `POST /create` / `PUT /update` / `DELETE /delete?id=`：撞同日唯一报 1_030_008_009；权限 `lunaerp:production:yarn-price:{query,create,update,delete}`
- 取价为内部 Service（`YarnPriceService#getEffectivePrice(specId, type, date)`），不对前端开放

### 成本卡（业务域）`/lunaerp/business/cost-card`

- `GET /page`（keyword 卡号/产品编号/品名；status；productId）、`GET /get?id=`（嵌套两子表，按 line_no）
- `POST /create`（头+`yarnItems[]`+`feeItems[]` 聚合保存，服务端取价/重算落库，状态 DRAFT）、`PUT /update`（仅 DRAFT，整体替换子表，模式同合同 ADR-0003：逻辑删旧行+插新行）
- `DELETE /delete?id=`（仅 DRAFT，级联软删，进回收站）
- `PUT /effect?id=`、`PUT /expire?id=`、`POST /copy?id=`（返新草稿 id）
- `POST /refresh-prices`（body=卡 id 或试算态行数组；按价类重取当日价，返回重算结果，草稿保存时同样重取）
- `POST /trial`（不落库试算）
- `GET /get-next-no`、`GET /effective-by-product?productId=`（报价带卡用；无生效卡返回空成功）
- 权限 `lunaerp:business:cost-card:{query,create,update,delete}`（effect/expire/copy/refresh 复用 update，get-next-no/effective/trial 复用 query）

### 报价单（业务域）`/lunaerp/business/quotation`

- `GET /page`（keyword 报价号/客户名/客户合同号；status 含 EXPIRED 派生筛选；customerId；latestOnly 缺省 true）、`GET /get?id=`（嵌套 items + 同号版本概要列表；按权限脱敏）、`GET /versions?quotationNo=`
- `POST /create`（头+`items[]`，DRAFT；无号走自动取号）、`PUT /update`（仅 DRAFT；整体替换明细；服务端重算全部金额/毛利/预警）
- `DELETE /delete?id=`（仅 DRAFT，级联软删进回收站）
- `PUT /confirm?id=`、`PUT /cancel?id=`、`POST /revise?id=`（返新版本草稿 id）
- `GET /contract-prefill?id=`：仅最新 CONFIRMED 可调（失效加 warning 标志不拦截），返回销售合同保存 VO 形的预填结构（头映射+items 映射，含 sourceQuotationId/No）
- `GET /print-data?id=`：W0 打印取数；**响应结构保证不含任何成本/毛利字段**（后端白诺字段，非依赖脱敏）；每次调用写打印操作日志
- `GET /get-next-no`
- 权限 `lunaerp:business:quotation:{query,create,update,delete,print}`（confirm/cancel/revise 复用 update；contract-prefill 复用 query）

### 销售合同侧接线

- `SalesContractSaveReqVO` 加可空 `sourceQuotationId/sourceQuotationNo`；新建页带 `quotationId` 参数时先调 `/quotation/contract-prefill` 预填；保存时服务端校验：报价存在、为最新 CONFIRMED、币种一致（不一致报 QUOTATION_CURRENCY_MISMATCH），写入两来源列；不做状态回写、不做数量占用。允许多次预填多份合同。

## 编号规则

- 成本卡 `CC{yyyy}NNNN`、报价单 `QT{yyyy}NNNN`：均沿用产品/合同的"租户内年前缀扫描 max+1、扫描含软删行（自定义 Mapper 绕过逻辑删除）、撞唯一索引有限重试"方案，不用 Redis/序列取号（序列仅主键）；手填撞号（含已删）报错不延号。
- 报价版本：revision 内 `SELECT max(version_no)`（同号行加锁）+1；`(quotation_no, version_no)` 全量唯一含删。

## 权限与脱敏

- 成本卡整套权限默认仅授成本/管理员角色；纱价四权限挂纱线档案菜单（10240）下为按钮 10245–10248，默认不给销售。
- 报价单成本可见性复用 W0 脱敏 starter 的 **amount 权限位**（`@Sensitive(type=AMOUNT)` 或 W0 spec 确定的等价注解）：报价 page/get/versions 的 cost_unit_price_rmb/cost_unit_price/target_margin_rate/cost_amount/margin_rate/avg_margin_rate/total_cost_amount/cost_snapshot 对无 amount 位用户在**序列化层抹除**；低价预警徽章不泄漏金额（仅布尔）。
- 打印 VO 独立白诺，豁免位也不出现成本（对外单据没有底牌开关）。
- 手填成本（cost_source=MANUAL）同样受脱敏位约束——能不能看见/手填成本由同一个 amount 位决定（无位者该行成本控件 disabled 且提交值被忽略）。

## 字典（7 个，type id 2262–2268；已避让样品 2251-2254、打色 2255-2257、出口单证 2258-2261；dict_data 3770–3791，已避让打色 3736-3749、出口单证 3750-3764）

- `lunaerp_cost_card_status`（2262，data 3770-3772）：DRAFT 草稿(info)/EFFECTIVE 生效(success)/EXPIRED 失效(danger)
- `lunaerp_cost_card_basis`（2263，3773-3774）：KG 按千克 / METER 按米
- `lunaerp_cost_card_fee_type`（2264，3775-3780）：WEAVING 织造/DYEING 染色/SETTING 定型/INSPECTION 检验/PACKAGING 包装/FREIGHT 运费
- `lunaerp_cost_fee_unit`（2265，3781-3782）：PER_KG 元/千克 / PER_METER 元/米
- `lunaerp_yarn_price_type`（2266，3783-3784）：PURCHASE 采购参考价(primary)/MARKET 市场参考价(warning)
- `lunaerp_quotation_status`（2267，3785-3789）：DRAFT 草稿(info)/CONFIRMED 已确认(success)/SUPERSEDED 已修订(warning)/CANCELLED 已取消(danger)/EXPIRED 已失效(info，**仅展示派生态，禁止写入**，remark 注明)
- `lunaerp_quotation_cost_source`（2268，3790-3791）：CARD 成本卡 / MANUAL 手工
- 报价币种/单位复用 `lunaerp_sales_contract_currency`、`lunaerp_sales_contract_unit`，不新建。

## 菜单与路由

SQL 菜单（先 DELETE 再 INSERT，可重复执行）：

- 10132 `成本卡` 菜单（type=2，parent=10100 销售目录，sort 0，component `lunaerp/business/cost-card/index`，componentName `LunaerpBusinessCostCard`，query 权限）；10133/10134/10135 创建/修改/删除按钮。
- 10136 `报价单` 菜单（parent=10100，sort 0，component `lunaerp/business/quotation/index`，componentName `LunaerpBusinessQuotation`）；10137/10138/10139/10140 创建/修改/删除/打印按钮。
- 两新菜单 sort 均取 0，按菜单 id 次序自然排在销售合同(10110,sort1)/发货(10120,sort2)/出口单证(10126,sort3)之前，形成"成本卡→报价单→合同→发货→单证"售前到履约顺序；ID 段避让 10126–10131 出口单证。
- 10245–10248 纱价查询/创建/修改/删除按钮挂在 10240 纱线档案菜单下（无独立菜单；前端为规格行抽屉）。
- 整页表单不进菜单：remaining.ts 隐藏路由注册 `/business/cost-card/edit/:id?`、`/business/quotation/edit/:id?`（无 id 新建、有 id 编辑/查看只读同页）。
- 前端落**正式目录**（非 lunaerp-elmui 临时区，对齐产品模块）：`luna-ui/src/views/lunaerp/business/{cost-card,quotation}/`，API 镜像 `src/api/lunaerp/business/{cost-card,quotation}/`、`src/api/lunaerp/production/yarn-price/`。

## 打印、回收站、Excel、消息（接 W0 契约）

- **打印**：报价单模板注册进 W0 打印内核与模板注册表（就近放 `views/lunaerp/business/quotation/print/`），A4 固定套打；打印数据走 print-data 白诺接口；注册名 `quotation-default`。成本卡一期不打印。
- **回收站**：注册三个 handler——成本卡（仅 DRAFT，级联复活两子表）、报价单（仅 DRAFT，级联复活明细）、纱价记录（主数据，恢复后无状态特殊处理）；生效/确认类单据不进回收站（ADR-0038）。
- **Excel 导入**：注册"纱价导入"模板（规格按 standard_name 或 材质+规格名匹配、价类、生效日期 yyyy-MM-dd、价格；仅新增、同日撞唯一进错误 Excel；5000 行上限）。
- **消息**：一期无审批不挂通知；不新增站内消息模板。
- **infra_config**：seed `quotation.min-margin-rate = 0`（config 键沿用 W0/infra 既有配置机制）。

## Testing Decisions

测试 seam 沿用本模块既有纯 Mockito 模式（不启 Spring 上下文、不连库），外加**纯函数单测**承接全部金额逻辑（DoD：金额逻辑必有测试）。

1. `CostCardPricingCalculatorTest`（纯函数，金额用例核心）：
   - 针织：纱线成本 Σ损耗(单价×比例%)、加工费/费用行合计、KG 口径；空数组/全零/多纱线/负损耗钳 0/≥100% 钳 99.9%。
   - 梭织简易：kgPerMeter=克重×门幅/100000、混纺单价×每米重量。
   - 梭织经纬精算：经/纬分组、每米用量×单价÷(1−损耗)。
   - 费用行：PER_KG/PER_METER 双口径归一与跨口径乘除 kgPerMeter；克重门幅缺失报 005。
   - 含税=不含税×(1+税率)；4 位小数 HALF_UP；与历史 TS 边界用例结果一致（同组数据双端对照）。
2. `YarnPriceServiceImplTest`：取价=不晚于当日最近一条；价类隔离；无价回退 spec.ref_price 且标记来源；ref_price 也无→缺价；同日唯一冲突。
3. `CostCardServiceImplTest`：自动/手填编号（扫描含删、撞号）；聚合保存重排 line_no 与服务端重算（忽略前端汇总）；effect 守卫（非草稿 002/缺价 003/无 BOM 004）、同事务顶旧卡为 EXPIRED、条件更新失败路径；expire 守卫；copy 为新草稿；生效/失效卡不可改删；部分唯一索引冲突转业务错。
4. `QuotationServiceImplTest`：编号+版本号；revise 仅最新 CONFIRMED 可操作、旧版 SUPERSEDED、同号草稿冲突 104；confirm/cancel/delete 状态守卫；毛利正算/反算/整单加权/毛利率=100% 拦截/成本 0 时毛利 null；服务端金额重算覆盖前端值；外币折币（÷汇率，RMB 汇率 1）、非 RMB 缺汇率报 105；低价预警落标；无 amount 位响应字段抹除；contract-prefill 非最新版/非确认态报 103、失效仅 warning、币种一致校验、来源列回写、可多次转。
5. **租户隔离**：全部新表查询自动带 tenant_id；写接口（create/update/effect/revise/转合同）各一条跨租户不可达用例（他租户 id → 不存在类错误，不得泄漏存在性）。
6. **越权**：纵向——无 cost-card 权限访问各端点 403；无 amount 位报价读接口无成本字段。横向见上条。
7. 性能：列表万级 P95<1s（索引覆盖：部分唯一索引、customer_id/status/日期索引，报价仅最新版查询用 max(version_no) 聚合）。

前端不做单测（lunaerp-elmui 先例不适用于正式目录，但本期测试资源仍以后端金额逻辑为界，前端靠验收清单与波次冒烟把关）。

## 前端验收清单

- 纱价抽屉：规格行入口、两类 Tab、日期倒序历史、新增/编辑/删除校验（同日重复提示）、Excel 导入按钮与错误 Excel 下载、最近采购价只读区。
- 成本卡列表：关键词/状态/产品筛选、状态 DictTag、卡号-产品-口径-单位成本（权限内）-生效日期列；新建/编辑/删除/生效/失效/复制按钮按状态与权限禁用。
- 成本卡编辑：选品带出快照+BOM+自动取价；纱线行/费用行可编辑表格、行贡献双口径实时展示（trial 接口）、缺价红字、重新取价；底部四个单位成本+缺价数；生效二次确认（提示旧卡失效）；生效卡全页只读。
- 报价列表：仅最新版本缺省、历史版本开关、EXPIRED 派生徽章、成本/毛利列按权限显隐；新建/修订/确认/取消/删除/打印/转合同按钮按状态版本权限启停。
- 报价编辑：选客户带默认毛利；选产品带有效卡快照；无卡警示+手工成本（按权限）；毛利↔单价双向反算；行/单金额实时、低价红字；外币时汇率必填控件；有效期；确认/修订流程；同号版本历史抽屉只读。
- 转合同：跳转合同新建页预填、币种锁定、保存后合同详情可见来源报价号；从报价侧与合同侧两侧可交叉打开。
- 套打预览：无任何成本/毛利字样。
- 回收站出现三类对象且恢复链可用（BOM/产品/客户被引用时的提示文案）。

### W1 波次冒烟（与 06 打色、08 发布工作台等 W1 模块合并执行的脚本要点）

1. 纱价：建两个规格的采购价（含历史日期）→ Excel 导入一批 → 取价验证。
2. 成本卡：为一针织、一梭织（简易+精算各一）产品建卡 → 缺价拦截 → 补价生效 → 旧卡失效 → 复制调价再生效。
3. 报价：RMB 报价毛利反算 → 修订出 v2 → 确认；再做一张 USD 报价（汇率快照）→ 过期警告 → 转合同 → 合同金额/来源核对 → 用同报价再转第二张合同。
4. 权限：销售角色登录看不到成本卡菜单、报价无成本列、打印预览无底牌。
5. 回收站：删草稿→恢复→内容完整。

## 统一 DoD 对照

- [x] 状态机白名单+终态守卫+并发条件更新：卡三态/报价四态条件更新，部分唯一索引（同品一生效卡/同号一草稿）兜底并发
- [x] 芋道权限+脱敏：三模块权限码入菜单；报价成本/毛利挂 W0 @Sensitive amount 位，序列化抹除
- [x] 列表/详情/删除进回收站：卡 DRAFT/报价 DRAFT/纱价主数据注册 W0 handler，生效单据不进回收站（ADR-0038）
- [x] 打印+操作/审批日志：报价单 W0 固定套打+print-data 白诺+打印日志；全动作用 @LogRecord；一期无审批流
- [x] 核心状态流转与副作用测试：状态守卫/金额重算/快照冻结/转合同回写；售前单据无库存账务过账（已在 spec 明示）
- [x] 金额逻辑必有测试：CostCardPricingCalculator 纯函数 + 毛利反算/外币折算 Service 测试
- [x] 前端验收清单+波次冒烟：见上两节
- [x] 术语与错误码归位：business/production/baseinfo GLOSSARY + shared 段位表新增 1-030-034；纱价走 1-030-008 追加码
- [x] 性能：列表索引覆盖，万级 P95<1s
- [x] 租户隔离：全表 TenantBaseDO + 写接口跨租户用例
- [x] 横向/纵向越权：跨租户不可达、无权限位 403/无成本字段

## Out of Scope

- 生成式/AI 核价、纱价爬虫行情、参数自然语言提取（Q5=A，二期增值包）
- 报价审批流（approval_status 不挂；二期需要时按 02 号审批引擎接 biz_type）
- 核价申请单（CostPricingRequest 协作流，二期；一期缺卡线下催办）
- 客户协议价/专属价表（一客一价由报价单自身承载，历史报价可查）
- 工序费用主档（DyeingProcessInfo 式百科价表）；一期费用行卡内手工
- 采购价自动回写纱价（一期只展示最近采购价辅助录入）
- 报价/合同的税额、含税单价字段与价税合计模型（卡保留含税成本仅供内部透视）
- 转合同数量占用/已转量追踪、报价→发货/对账链路
- 报价 CRM 线索/商机漏斗、客户门户报价查看（门户一期仅发货/对账，见地图 Q13）
- W3 汇率主档自动带汇率（一期手填快照）
- 成本卡套打、成本卡多组织/多工厂维度

## 关联改动清单（实现时必须一并完成）

1. `yarn_recipe_bom_item` 加 5 列 + BOM 管理页/VO 补字段维护（损耗/方向/密度/织缩/每米用量）。
2. 后端算价纯函数 + `/trial`；产品档案成本分区改调 trial；前端 `product-yarn-cost.ts` 与既有测试保留并加"权威实现已迁移"注释（ADR-0039 显式修订 ADR-0035 的算价位置，不推翻字段迁移）。
3. 客户表/VO/表单加 default_margin_rate。
4. 销售合同表/VO 加来源两列 + 新建页 quotationId 预填接线。
5. ADR-0039（成本卡快照模型 + 双配比源收敛 BOM + 后端算价权威性）。
6. 术语：business GLOSSARY（成本卡/费用行/报价单/报价版本/有效期/毛利率/转合同）、production GLOSSARY（纱价两价类/取价回退/BOM 新增列）、baseinfo GLOSSARY（客户默认毛利率、成本分区算价改为后端试算的修订说明）；shared GLOSSARY 段位表加 1-030-034（032 打色指示单、033 出口单证已占）与 1-030-008 追加码。
7. W0 注册表：打印模板、回收站 handler×3、Excel 导入模板、infra_config 键。

## 错误码（1-030-034 段；032 打色指示单、033 出口单证已占；纱价追加 1-030-008 段）

成本卡 000–099：

- `1_030_034_000` COST_CARD_NOT_EXISTS（成本卡不存在）
- `1_030_034_001` COST_CARD_NO_DUPLICATE（成本卡号【{}】已存在）
- `1_030_034_002` COST_CARD_STATUS_NOT_ALLOWED（成本卡当前状态不允许该操作）
- `1_030_034_003` COST_CARD_MISSING_PRICE（成本卡存在 {} 个未取到价的纱线行，不能生效）
- `1_030_034_004` COST_CARD_RECIPE_BOM_MISSING（产品【{}】无有效配比 BOM 或 BOM 无明细）
- `1_030_034_005` COST_CARD_KG_METER_CONVERSION_MISSING（缺少克重/门幅，无法进行千克/米换算）

报价单 100–199：

- `1_030_034_100` QUOTATION_NOT_EXISTS（报价单不存在）
- `1_030_034_101` QUOTATION_NO_DUPLICATE（报价单号【{}】已存在）
- `1_030_034_102` QUOTATION_STATUS_NOT_ALLOWED（报价单当前状态不允许该操作）
- `1_030_034_103` QUOTATION_NOT_LATEST_VERSION（仅最新已确认版本可修订/转合同）
- `1_030_034_104` QUOTATION_DRAFT_EXISTS（该报价号已存在草稿版本，请在草稿上继续编辑）
- `1_030_034_105` QUOTATION_FX_RATE_REQUIRED（外币报价必须填写汇率）
- `1_030_034_106` QUOTATION_MARGIN_RATE_INVALID（毛利率必须介于 -100% 与 100% 之间且不能等于 100%）
- `1_030_034_107` QUOTATION_CURRENCY_MISMATCH（报价币种【{}】与合同币种不一致，不能转合同）

纱价（生产域既有 1-030-008 段追加，接续 007）：

- `1_030_008_008` YARN_PRICE_NOT_EXISTS（纱价记录不存在）
- `1_030_008_009` YARN_PRICE_DUPLICATE_DATE（纱线规格【{}】的{}价在 {} 已存在记录）

## 实现顺序建议

1. SQL：纱价表 + 成本卡三表 + 报价两表 + 三表 ALTER + 菜单/按钮（10132-10140/10245-48）+ 7 字典 + infra_config
2. 纱价 DO/Mapper/Service/Controller + 取价 Service（含 ref_price 回退）
3. BOM 行加字段（DO/VO/SQL/前端配比管理页）
4. CostCardPricingCalculator 纯函数 + 单测（先红后绿，对齐 TS 用例）
5. 成本卡聚合 CRUD/状态机/trial/copy/refresh + Controller + Mockito 测试
6. 客户默认毛利率、合同来源列接线
7. 报价单聚合 CRUD/版本状态机/毛利外币/预填/print-data + Controller + 测试
8. W0 注册（打印/回收站/导入/配置）
9. 前端：纱价抽屉 → 成本卡列表/编辑（trial 接线）→ 产品成本分区切 trial → 报价列表/编辑/版本/转合同/打印 → 合同页预填
10. ADR-0039、三域 GLOSSARY、shared 段位表
