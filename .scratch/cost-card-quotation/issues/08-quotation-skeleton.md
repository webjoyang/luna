# 08: 报价单骨架端到端（CRUD / confirm / cancel + 毛利反算 / 外币 / 脱敏 + 前端列表/编辑）

**What to build:** 用户能新建报价单（选客户带出客户名称/联系人/电话快照 + 客户默认毛利率，客户档案无则用全局缺省 15%；选产品后自动带出该产品当前 EFFECTIVE 成本卡整份快照（卡号/取价日期/双口径成本/纱线费用明细），行成本来源标记 `CARD`；产品没有生效成本卡时仍可加入：成本价为 0 亮「无成本卡」警示、毛利率不显示，有 amount 权限位的人可手填成本价、行来源标记 `MANUAL`）；每行录入目标毛利率后服务端按 `报价单价 = 成本 / (1 − 目标毛利率)` 反算单价、改单价则反算实际毛利率 `1 − 成本额/金额`；毛利率输入范围 (−100%, 100%)，等于 100% 数学不可反算拦截报 `1_030_034_106`；行金额 = 数量 × 单价、行成本额 = 数量 × 折报价币成本，全部由服务端权威重算；单头总数量/总金额/总成本额/整单毛利率（金额加权 `(总金额 − 总成本)/总金额`）由服务端重算，前端传值忽略；行/单毛利率低于 infra_config `quotation.min-margin-rate`（缺省 0%）时红字低价预警但不阻断保存。做外销报价时选币种（复用 `lunaerp_sales_contract_currency` 字典，缺省 RMB）与汇率（非 RMB 必填报 `1_030_034_105`，手工录入、含义 = 1 单位外币兑人民币、保存时快照不随后续波动；RMB 汇率存 1）；RMB 成本按汇率折外币后在同币种内计算毛利；无 W3 汇率主档前不自动带汇率。报价日期缺省今天、有效期自缺省今天、有效期至可空 = 长期；过期已确认单在列表显示「已失效」派生徽章但状态不入 EXPIRED。保存草稿（编号 `QT{yyyy}NNNN` 自动取号或手填撞号报 `1_030_034_101`、扫描含删不延号，`version_no=1`）→ 「确认报价」（DRAFT→CONFIRMED，无审批流）；CONFIRMED 可「取消」（→CANCELLED 终态留痕）；DRAFT 可删除（进回收站，注册 W0 handler 级联复活明细）；SUPERSEDED/CANCELLED 全只读不可删。同号至多一草稿（部分唯一索引兜底，冲突报 `1_030_034_104`）。`@Sensitive` amount 权限位对无权限用户在序列化层抹除：`page`/`get` 的 `cost_unit_price_rmb`/`cost_unit_price`/`target_margin_rate`/`cost_amount`/`margin_rate`/`avg_margin_rate`/`total_cost_amount`/`cost_snapshot` 整字段抹除（手填成本 MANUAL 同样受脱敏位约束——能不能看见/手填成本由同一个 amount 位决定，无位者该行成本控件 disabled 且提交值被忽略）；低价预警徽章不泄漏金额（仅布尔）。21 号票数据范围接线：`salesperson_user_id`/`dept_id` 4 档行级数据范围注册（owner 缺省登录用户、dept_id 服务端推导不接受前端入参）。列表默认仅最新版本（按 no 取 `max(version_no)` 聚合）、关键词（报价号/客户名/客户合同号）/状态（含「已失效」派生筛选项）/客户筛选、默认报价日期倒序；列表与详情接口按权限脱敏。`QuotationServiceImplTest` 单测覆盖编号+版本号、状态守卫、毛利正反算/整单加权/100% 拦截、外币折币（÷汇率，RMB 汇率 1）、非 RMB 缺汇率报 105、低价预警落标、无 amount 位响应字段抹除、跨租户不可达。

**Blocked by:** 05（`CostCardPricingCalculator` 算价 + 毛利纯函数）, 06（`/effective-by-product` 取卡）, 07（customer.default_margin_rate 预填）

**Status:** ready-for-agent

- [ ] 后端 `business/business/quotation` 四层 + Controller + `QuotationItem` 子表 DO/Mapper（**不建** item 的 Controller/Service/权限码/错误码，聚合保存模式对齐合同 ADR-0003）
- [ ] Controller 路径 `/lunaerp/business/quotation`；`GET /page`、`GET /get`、`POST /create`、`PUT /update`、`DELETE /delete`、`PUT /confirm`、`PUT /cancel`；权限 `lunaerp:business:quotation:{query,create,update,delete}`
- [ ] 聚合保存：头 + `items[]` 整体替换（update 时按 `quotation_id` 逻辑删旧明细 + 插新明细），`line_no` 重排
- [ ] 服务端重算（调 `CostCardPricingCalculator`）：行 `amount`/`cost_amount`/`margin_rate`、单头 `total_quantity`/`total_amount`/`total_cost_amount`/`avg_margin_rate`；前端传值忽略
- [ ] 毛利反算：`unitPrice = cost/(1−margin)`、反手 `margin = 1 − cost/amount`、整单加权；100% 拦截报 `1_030_034_106`；负值/超界拦截
- [ ] 外币：currency 缺省 RMB、复用 `lunaerp_sales_contract_currency` 字典；非 RMB 必填 `exchange_rate > 0` 报 `1_030_034_105`；RMB 存 1；折币 `costUnitPrice = costUnitPriceRmb ÷ exchangeRate`
- [ ] 报价号 `QT{yyyy}NNNN` 扫描含删 + 撞号报 `1_030_034_101`；`version_no=1`；同号至多一草稿冲突报 `1_030_034_104`
- [ ] 状态机：confirm DRAFT→CONFIRMED、cancel CONFIRMED→CANCELLED；条件更新守卫，影响行 0 转 `1_030_034_102`；DRAFT 可删除（级联软删明细进回收站）；SUPERSEDED/CANCELLED 全只读不可删
- [ ] 「已失效」派生徽章：`status=CONFIRMED 且 valid_to < 当天`，不入 EXPIRED 状态
- [ ] 选产品自动带当前 EFFECTIVE 成本卡快照（调 06 票 `/effective-by-product`）；无卡时成本 0 + 「无成本卡」警示；手填成本行 `cost_source=MANUAL`
- [ ] 选客户带默认毛利率（调 07 票 `customer.default_margin_rate`，无则全局 15%）
- [ ] `@Sensitive` amount 位脱敏：page/get 抹除成本/毛利字段；低价预警徽章仅布尔不泄漏金额；MANUAL 行手填成本同样受脱敏位约束（无位者该行成本控件 disabled 且提交值被忽略）
- [ ] 低价预警按 infra_config `quotation.min-margin-rate` 阈值落标 `low_margin_warning`（不阻断保存）
- [ ] 21 号票数据范围接线：`salesperson_user_id`/`dept_id` 4 档行级数据范围注册；新建 owner 缺省登录用户、`dept_id` 服务端推导不接受前端入参
- [ ] W0 回收站 handler 注册（仅 DRAFT，级联复活明细）
- [ ] 前端正式目录 `views/lunaerp/business/quotation/` + API 镜像 `src/api/lunaerp/business/quotation/`；列表（仅最新版本缺省、历史版本开关、EXPIRED 派生徽章、成本/毛利按权限显隐、关键词/状态/客户筛选）、编辑页（选客户带默认毛利、选产品带有效卡快照、无卡警示+手工成本按权限、毛利↔单价双向反算、行/单金额实时、低价红字、外币汇率必填控件、有效期）、`remaining.ts` 隐藏路由 `/business/quotation/edit/:id?`
- [ ] `QuotationServiceImplTest` 单测（纯 Mockito）：编号+版本号、confirm/cancel/delete 状态守卫、毛利正算/反算/整单加权/100% 拦截、外币折币（÷汇率，RMB 汇率 1）、非 RMB 缺汇率报 105、低价预警落标、无 amount 位响应字段抹除、跨租户不可达
- [ ] 验收：RMB 报价毛利反算 → 保存草稿 → 确认；USD 报价（汇率快照）→ 保存；过期报价列表显示「已失效」徽章；销售角色登录看不到成本卡菜单、报价无成本列
