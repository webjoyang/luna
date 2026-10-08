# 06: 成本卡聚合端到端（CRUD / 状态机 / copy / refresh / trial + 前端列表/编辑 + 产品档案切 trial）

**What to build:** 用户能新建成本卡（远程搜索选品带出产品快照 + BOM + 按卡头选定的价类缺省 PURCHASE 自动取当日纱价；产品无有效配比 BOM 或 BOM 无明细时报 `1_030_034_004` 拦截，不算出 0 成本卡）；维护纱线行（规格名快照、取价/价类/生效日期、比例、方向、密度、织缩率、损耗率、每米用量、行贡献双口径、缺价红字）与费用行（六项字典 WEAVING/DYEING/SETTING/INSPECTION/PACKAGING/FREIGHT + PER_KG/PER_METER + 单价 + 损耗 + 行贡献双口径）；产品档案针织加工费/织造费/其他加工费/包装费预填初始费用行（可任意增删改）；底部实时显示四单位成本（不含税/含税 × KG/METER，无法换算口径 null 显示「—」）+ 缺价纱线数（走 `/trial` 接口实时试算）；针织卡录元/M 费用、梭织卡录元/KG 费用时按每米重量自动换算，缺克重/门幅报 `1_030_034_005` 拦截该费用行。保存草稿（编号 `CC{yyyy}NNNN` 自动取号或手填撞号报 `1_030_034_001`，扫描含删、撞号不延号）/继续编辑/删除（仅 DRAFT 进回收站，级联软删两子表，注册 W0 handler 级联复活）；点「生效」校验 `missing_price_count=0`（报 `1_030_034_003`）+ BOM 有效（报 `1_030_034_004`）后同事务把同产品旧 EFFECTIVE 卡条件更新为 EXPIRED、本卡落 `effective_date=今天`、字段冻结；生效卡可手工「失效」（仅 EFFECTIVE→EXPIRED，条件更新守卫）；调价走「复制新建」（copy 生成 DRAFT 新卡，行数据复制但价格标记为复制时点快照，可重新取价）；生效/失效卡不可 update/delete（API 层报 `1_030_034_002`）。`POST /trial` 不落库试算接口供产品档案成本分区改调（替代前端 `product-yarn-cost.ts`，TS 文件加「权威实现已迁移至后端 CostCardPricingCalculator，ADR-0039」注释，TS 既有测试保留但不再是权威算价路径）；`POST /refresh-prices` 按价类重取当日价；`GET /get-next-no`、`GET /effective-by-product?productId=`（无生效卡返回空成功）供报价带卡用。列表按卡号/产品编号/品名关键词/状态/产品筛选，默认生效日期+更新时间倒序分页。`CostCardServiceImplTest` 单测覆盖编号/状态守卫/重算/部分唯一索引冲突转业务错。

**Blocked by:** 03（取价 Service `YarnPriceService#getEffectivePrice`）, 04（BOM 5 列 DO 形状）, 05（`CostCardPricingCalculator` + trial 接口基础）

**Status:** ready-for-agent

- [ ] 后端 `business/business/costcard` 四层（DO 主表+纱线行子表+费用行子表 DO/Mapper/Service/Controller）；VO 头+嵌套 `yarnItems[]`/`feeItems[]` 聚合保存，带 Swagger 与 jakarta.validation
- [ ] Controller 路径 `/lunaerp/business/cost-card`；`GET /page`、`GET /get`、`POST /create`、`PUT /update`、`DELETE /delete`、`PUT /effect`、`PUT /expire`、`POST /copy`、`POST /refresh-prices`、`POST /trial`、`GET /get-next-no`、`GET /effective-by-product`；权限 `lunaerp:business:cost-card:{query,create,update,delete}`（effect/expire/copy/refresh 复用 update，get-next-no/effective/trial 复用 query）
- [ ] 聚合保存：头+纱线行+费用行整体替换（update 时按 contract_id 模式逻辑删旧行+插新行），`line_no` 按数组重排
- [ ] 服务端取价（调 `YarnPriceService#getEffectivePrice`）+ 重算（调 `CostCardPricingCalculator`）落库；前端传值忽略
- [ ] 卡号 `CC{yyyy}NNNN` 扫描含删（绕过逻辑删除的自定义查询，注释 ADR-0004 意图）+ 撞号报 `1_030_034_001`；自动号撞唯一索引有限次顺延重试
- [ ] effect 守卫：非草稿报 `1_030_034_002`、缺价报 `1_030_034_003`、无 BOM 报 `1_030_034_004`；同事务顶旧 EFFECTIVE 卡为 EXPIRED；条件更新影响行 0 报 002；部分唯一索引冲突转业务错
- [ ] expire 仅 EFFECTIVE→EXPIRED；EFFECTIVE/EXPIRED 不可 update/delete（API 层报 002）
- [ ] copy 生成 DRAFT 新卡，行数据复制，价格标记为复制时点快照，可重新取价
- [ ] `/trial` 不落库试算，body 同保存 VO（允许无 id/无卡号），返回完整测算结果（含双口径、缺价数、各行列贡献）
- [ ] `/effective-by-product?productId=` 无生效卡返回空成功（供报价带卡用）
- [ ] `/refresh-prices` 按价类重取当日价，返回重算结果，草稿保存时同样重取
- [ ] W0 回收站 handler 注册（仅 DRAFT，级联复活两子表）
- [ ] 产品档案成本分区改调 `/trial`；`product-yarn-cost.ts` 加「权威实现已迁移至后端 CostCardPricingCalculator，ADR-0039」注释，既有 TS 测试保留
- [ ] 前端正式目录 `views/lunaerp/business/cost-card/` + API 镜像 `src/api/lunaerp/business/cost-card/`；列表（关键词/状态/产品筛选、状态 DictTag、卡号-产品-口径-单位成本-生效日期列、按钮按状态权限禁用）、编辑页（选品带快照+BOM+自动取价、纱线行/费用行可编辑表格、行贡献双口径实时 `/trial`、缺价红字、重新取价、底部四单位成本+缺价数、生效二次确认提示旧卡失效、生效卡全页只读）、`remaining.ts` 隐藏路由 `/business/cost-card/edit/:id?`
- [ ] `CostCardServiceImplTest` 单测（纯 Mockito）：编号/扫描含删/撞号、聚合保存重排 `line_no` 与服务端重算、effect 守卫三条（002/003/004）、同事务顶旧卡、expire 守卫、copy 为新草稿、生效/失效卡不可改删、部分唯一索引冲突转业务错、跨租户不可达
- [ ] 验收：建针织卡 + 梭织简易卡 + 梭织经纬精算卡各一张；缺价拦截 → 补价生效 → 旧卡失效 → 复制调价再生效；生效卡全页只读；产品档案成本分区显示 trial 结果；跨租户不可达
