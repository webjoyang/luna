# 库存管理域术语表（stock）

本文件是 Luna ERP **库存管理域（stock）**的权威术语表，收录入库单、仓库/库位、库存台账/余额的领域概念。
仓库领域文档总入口见根目录 `CONTEXT-MAP.md`；跨域平台约定（租户、软删除、前后端模块约定、错误码段位总表）见 `docs/contexts/shared/CONTEXT.md`。
当对话或代码中含糊使用某个词时，以本文件为准。

## 库存管理

**入库单 (StockIn)**：
登记货物实际入库的库存凭证，是库存管理域（stock）的第一个单据，也是继 baseinfo/business/production 之后第四个顶级业务域。入库单是**库存账的凭证**：审核即在同事务写库存流水（过账），已审核单只能取消（红冲），状态机严格守卫（ADR-0016），刻意不一致合同/胚布/染色/检验单 MVP 的"编辑页自由切态"惯例。主表含入库类型、入库仓库（一单一仓库、必填）、供应商（可空）、销售合同/胚布单/染色单/胚布品种四类上游关联（**全部可空、仅存在性校验与名称快照、零回写零联动**）、金额合计、单据日期。主表**不设数量合计**：明细单位允许 KG/M 混用，跨单位求和无意义。
_避免_：入库指示单、收货单。旧系统 Prisma 模型即名 `StockIn`（英文沿用），旧系统的审批态/QC_PENDING、应付付款/对账财务锁、订单进度回写、批次转可用、OCR 智能录入、多合同分摊均不进入 MVP。

**入库类型 (StockInType)**：
字典 `lunaerp_stock_in_type`（id=2234），回答"货从哪来"的四类稳定场景：`PURCHASE` 采购入库 / `PRODUCTION` 生产入库（胚布、成品布生产回库）/ `RETURN` 退货入库 / `OTHER` 其他入库。不沿用旧系统的纺织链路细分 8 型（GREIGE/YARN/DYEING/WASHING/POST/PRODUCT/INSPECTION/SPOT）——"是什么货"由明细产品表达，细分类型是为旧系统批次联动服务的，本期联动不做。采购退货（出库方向）不属本类型集，随出库模块再引入。

**入库明细 (StockInItem)**：
入库单中逐行登记的实收条目，通过 `itemType` 区分两类物品（ADR-0026）：`PRODUCT` 产品（缺省，关联产品档案 + 编码/名称快照）/ `YARN_SPEC` 纱线规格（关联 `yarnSpecId`+`yarnSpecName` 快照，单位固定 KG，产品字段禁填）。两类共用字段：batchNo 批次号/缸号（自由文本）、rollNo 卷号、pieceCount 匹数（缺省 1）、quantity 数量（>0）、unit 单位（产品行 KG/M 允许同单混用；纱线行固定 KG）、unitPrice 单价、amount 金额（=数量×单价，服务端权威重算）、remark。纱线采购入库经此进入通用库存（type=PURCHASE），不另建纱线入库凭证。明细**随入库单聚合保存、级联删除，无独立端点/权限码**（ADR-0003 模式；与检验单录检明细的独立子资源 ADR-0014 刻意不同）。允许零明细草稿，审核时至少一条明细。

**库位分配 (StockInPlacement)**：
入库明细到库位的放置记录（`lunaerp_stock_in_item_placement`）。一条明细可拆多条放置：库位（必须属于本单入库仓库的启用库位）+ 匹数 + 数量 + 备注，表达"同一产品行 10 匹分放两个库位"。随入库单聚合保存，无独立端点。MVP **不强制齐套**：不校验放置匹数合计等于明细（前端仅提示"已分配 X/共 Y 匹"），未分配明细审核照常过账（流水库位为空），支持先入账后上架。放置记录是库存流水按库位展开的数据来源（ADR-0017/0018）。

**仓库 (Warehouse)**：
存放货物的物理仓库主数据。字段：仓库编号 warehouseNo（`WH`+年份+4 位流水，如 WH20260001，服务端取号/可手填，租户内全量唯一含软删行）、名称必填、地址、状态（ACTIVE/INACTIVE，不建字典）、备注。删除守卫：下有库位或已被入库单引用则禁止删除、引导停用；停用仓库不可被新单选择，历史快照不变。一张入库单只能入一个仓库。
_避免_：WarehouseMap（旧系统含 2D 平面图画布/尺寸/版本，MVP 不做）。

**库位 (WarehouseLocation)**：
仓库内的具体存放位置，两级简版（仓库→库位，无库区）。字段：所属仓库、code 编码（手填如 `A-01-02`，**同一仓库内唯一**含软删行）、名称、排序、状态、备注。明确不建旧系统的库区（WarehouseZone）、平面图、行列层/xyz 坐标、容量、二维码（ADR-0018）。被放置记录引用的库位禁止删除。

**库存台账/流水 (StockLedger)**：
全物品通用的库存流水账（`lunaerp_stock_ledger`），只存流水不存余额（ADR-0017，**supersedes 已废弃的纱线专用台账 ADR-0012**）。流水只增不改不删。携带物品身份：`product_id` 与 `yarn_spec_id` **互斥**（随入库明细 itemType 扩展，ADR-0026），以及仓库（必填）、库位（可空）、批次、带符号数量/金额、单位、关联凭证（类型+ID+单号）。收支类型 `txn_type` 为后端内部枚举、不入字典：入库审核 `STOCK_IN` 正数 / 入库红冲 `STOCK_IN_REVERSAL` 负数 / 领纱发纱 `YARN_OUT` 负数 / 撤销发纱 `YARN_OUT_REVERSAL` 正数（ADR-0027）/ 发染出库 `PROCESS_OUT` 负数 / 发染红冲 `PROCESS_OUT_REVERSAL` 正数（发染记录过账）。

**库存余额 (StockBalance)**：
不是表，而是流水实时聚合结果：`SUM(quantity) GROUP BY 物品（产品或纱线规格）+ 单位 + 仓库`。本期提供只读"库存查询"（产品与纱线规格共用余额列表 + 流水明细分页两个 Tab）。库位级余额、负库存校验（领纱发纱在应用层拦截）、初始库存导入本期均不做（库存账户从 0 起）。

**入库单号 (StockInNo)**：
形如 `IN-20260923-001`：`IN` + `-` + 年月日 8 位 + `-` + 3 位日流水（超 999 自然扩位），**按天取号**，租户内全量唯一、软删不释放。沿用检验单号 ADR-0015 的连字符风格（与 SUP/CH/PB/YP 无连字符惯例刻意不同，本期第二个带连字符编号）。新建缺省服务端生成、也可手填（撞号报错），提供预览下一个编号接口。

**入库单状态 (StockInStatus)**：
三态严格状态机（ADR-0016，字典 `lunaerp_stock_in_status`，id=2233）：`DRAFT` 草稿（缺省，可改可删）/ `COMPLETED` 已审核（过账完成，只读，只能红冲取消）/ `CANCELLED` 已取消（终态）。无 ACTIVE（审核是瞬时动作）、无 QC_PENDING（质检归检验单）、无审批流。草稿取消不写流水；已审核取消按原流水逐条写红冲。审核/红冲权限 `:audit` 与制单 `:create/:update` 独立分权。

## 模块边界

库存管理落新建的 `stock` 顶级子包（与 baseinfo/business/production 平级），含 4 个并列子包：`stock.stockin`（入库单主表 + 明细 + 库位分配三个 DO 同包，聚合保存，仅主表有 Controller）、`stock.stockledger`（库存流水写入与余额/流水只读查询）、`stock.warehouse`（仓库 CRUD + 取号 + 删除守卫）、`stock.warehouselocation`（库位 CRUD + 仓内唯一 + 删除守卫）。前端视图 `lunaerp-elmui/stock/stock-in/`（列表 + 工作台：基本信息/入库明细含 PlacementDialog/操作日志占位）、`stock/inventory-balance/`（余额 + 流水两 Tab）、`stock/warehouse/`（仓库/库位主从同页）；前端 API 镜像于 `api/lunaerp-elmui/stock/`。URL 前缀 `/lunaerp/stock/{stock-in|stock-ledger|inventory-balance|warehouse|warehouse-location}`，权限码前缀 `lunaerp:stock:<子域>:`（入库单含独立 `:audit` 审核/红冲权限），错误码段 `1-030-011`（入库单/明细/放置）、`1-030-012`（台账/余额，本期只读预留）、`1-030-013`（仓库/库位）。菜单新建 10300 库存管理目录（sort 103）：10310 入库管理 + 10311-10314（含审核按钮）、10320 库存查询、10330 仓库管理 + 10331-10336。

## 字典

**库存管理字典 (Stock dictionaries)**：
库存域建三个字典：`lunaerp_stock_in_status`（id=2233，DRAFT/COMPLETED/CANCELLED 三态严格过账，ADR-0016）、`lunaerp_stock_in_type`（id=2234，PURCHASE 采购入库/PRODUCTION 生产入库/RETURN 退货入库/OTHER 其他入库）、`lunaerp_stock_in_unit`（id=2235，KG/M；库存域独立单位字典，不复用 lunaerp_sales_contract_unit）。仓库/库位的 ACTIVE/INACTIVE 状态不建字典（对齐客户/供应商）；流水 txn_type（STOCK_IN/STOCK_IN_REVERSAL/**YARN_OUT/YARN_OUT_REVERSAL**）为后端内部枚举，不入字典（同 ADR-0012 先例）。

## 错误码

错误码段位总表与新模块取段规则见 `docs/contexts/shared/CONTEXT.md`。本域占用段位：

- `1-030-011-xxx` 入库单（主表/明细/放置共用，库存域）：000 入库单不存在 / 001 入库单号重复 / 002 状态不允许该操作 / 003 关联引用不存在（仓库/供应商/合同/胚布单/染色单/胚布品种/产品/库位）/ 004 库位不属于入库仓库 / 005 审核失败单无明细。
- `1-030-012-xxx` 库存台账/余额（本期只读查询，预留段位，暂不定义具体错误码）。
- `1-030-013-xxx` 仓库与库位：000 仓库不存在 / 001 仓库编号重复 / 002 仓库下有库位不能删除 / 003 仓库被入库单引用不能删除 / 100 库位不存在 / 101 同仓库内库位编码重复 / 102 库位被放置记录引用不能删除（主表段 000-003，库位段从 100 起）。
