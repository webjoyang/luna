# 05: 纱线采购单地基 + 核心测算接口

**What to build:** 落地纱线采购单主从表（主表+明细表，明细无 line_no）+ 列表页（可分页/按采购单号+关联胚布单号关键词/状态/日期区间查询）+ 工作台编辑页骨架（基本信息区 + 关联胚布单区 + AI 测算按钮 + 明细表格）+ **核心测算接口 POST /yarn-order/calculate**（按勾选胚布单 → 查配比 BOM → 按 yarn_spec_id 合并多单需求 → 查 StockLedger 拿在库量 → 算 requiredQty/purchaseQty → 历史价回填）。这是整个 MVP 的核心业务流（勾选胚布单→AI测算→生成明细）。

**Blocked by:** 01 纱线档案 + 02 配比 BOM + 03 胚布单加 product_id + 04 库存台账

**Status:** ready-for-agent

- [ ] `luna-srv/sql/postgresql/lunaerp-production-yarn-order.sql` 已落（包含 04 的 stock_ledger 段）：
  - [ ] 表 6 `lunaerp_production_yarn_order` DDL（order_no/source_instructions(Text JSON)/order_date/delivery_date/loss_rate/status/remark + 框架列）+ 主键 DO 块 + 列注释 + 序列
  - [ ] 唯一索引 `(tenant_id, order_no)`（含软删行，按天取号不释放）+ 普通索引 `(status)` `(order_date)`
  - [ ] 表 7 `lunaerp_production_yarn_order_item` DDL（order_id/yarn_type_id/yarn_spec_id/supplier_id/supplier_name/ratio/loss_rate/base_qty/required_qty/stock_qty/purchase_qty/unit_price/total_amount/ref_unit_price/remark + 框架列，**无 line_no**）+ 主键 DO 块 + 列注释 + 序列
  - [ ] 普通索引 `(order_id)` `(yarn_spec_id)`
  - [ ] 菜单 10230 `纱线采购`（parent=10200，component `lunaerp-elmui/production/yarn-order/index`，componentName `LunaerpElmuiProductionYarnOrder`）+ 按钮 10231/10232/10233（query/create/update/delete）
  - [ ] 字典 type 2230 `lunaerp_yarn_order_status`（dict_data id 3580-3583：DRAFT/ACTIVE/COMPLETED/CANCELLED，紧接染色单 3571-3574）
  - [ ] 末尾追加胚布单 ALTER（详见 issue 03）：`ALTER TABLE lunaerp_production_greige_order ADD COLUMN IF NOT EXISTS product_id int8 NULL;` + COMMENT + 索引
- [ ] 后端 `production.yarnorder` 子包：
  - [ ] `YarnOrderDO`（主表，继承 TenantBaseDO）
  - [ ] `YarnOrderItemDO`（明细，继承 TenantBaseDO，可放同包或 `.item` 子包）
  - [ ] `YarnOrderMapper` + `YarnOrderItemMapper`
  - [ ] `YarnOrderItemSaveReqVO`（yarn_type_id/yarn_spec_id/supplier_id/supplier_name/ratio/loss_rate/base_qty/required_qty/stock_qty/purchase_qty/unit_price/total_amount/ref_unit_price/remark）
  - [ ] `YarnOrderSaveReqVO`（order_no/source_instructions/order_date/delivery_date/loss_rate/status/remark + `items: List<YarnOrderItemSaveReqVO>`）
  - [ ] `YarnOrderPageReqVO`（keyword/status/orderDateStart/orderDateEnd/deliveryDateStart/deliveryDateEnd）
  - [ ] `YarnOrderRespVO`（内嵌 items）
  - [ ] `YarnOrderCalculateReqVO`（greigeOrderIds: List<Long> + lossRate: BigDecimal）
  - [ ] `YarnOrderCalculateRespVO`（items 列表 + sourceGreigeOrders 展示快照，结构详见 spec.md）
  - [ ] `YarnOrderService` + `YarnOrderServiceImpl`：
    - [ ] 采购单号生成：`YP+年月日+3位日流水`，按天取号、租户内全量唯一含已删除行（仿胚布单 ADR-0006 模式）
    - [ ] create：order_no 缺省由服务端生成；撞号报 1_030_009_001；source_instructions 中胚布单不存在报 1_030_009_003；明细 yarn_spec_id 不存在报 1_030_009_003；明细 supplier_id（若传）不存在报 1_030_009_003；status 缺省 DRAFT；传 ACTIVE 直接下达；金额服务端权威重算 total_amount = unit_price × purchase_qty
    - [ ] update：仅 DRAFT 可更新，非 DRAFT 报 1_030_009_002；不存在报 1_030_009_000；明细整体替换；金额服务端权威重算
    - [ ] delete：仅 DRAFT 可删，非 DRAFT 报 1_030_009_002；级联逻辑删除明细
    - [ ] effect：DRAFT→ACTIVE
    - [ ] complete：ACTIVE→COMPLETED（**issue 06 落地自动写 STOCK_IN 台账**，本 issue 先留接口骨架）
    - [ ] cancel：ACTIVE→CANCELLED
    - [ ] calculate（**核心测算**）：算法详见 spec.md "测算接口 POST /yarn-order/calculate 契约"
      - 校验 greigeOrderIds 非空
      - 按 greigeOrderIds 查胚布单，校验存在（不存在报 1_030_006_000）+ 有 product_id（无则报 1_030_009_003 "关联的产品档案不存在"）+ 有配比 BOM（无则报 1_030_008_004）
      - 按 yarn_spec_id 合并多单多 BOM 明细需求 baseQty
      - 调 StockLedgerService.getBalances 一次拿 stockQty
      - 算 requiredQty = baseQty × (1 + lossRate/100)、purchaseQty = max(0, requiredQty - stockQty)
      - 查最近已下达（ACTIVE/COMPLETED）采购单明细 unit_price 历史价，无则取 yarn_spec.ref_price 兜底
      - 分组 sourceGreigeOrderIds/Nos 按 yarn_spec_id
  - [ ] `YarnOrderController`（10 端点）：
    - [ ] `GET /yarn-order/page` / `GET /yarn-order/get?id=` / `POST /yarn-order/create` / `PUT /yarn-order/update` / `DELETE /yarn-order/delete?id=`
    - [ ] `PUT /yarn-order/effect?id=` / `PUT /yarn-order/complete?id=` / `PUT /yarn-order/cancel?id=`
    - [ ] **`POST /yarn-order/calculate`**（核心测算接口）
    - [ ] `GET /yarn-order/get-next-no`（预览下一个采购单号，可选）
    - [ ] `@PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-order:*')")`
- [ ] 后端单测 `YarnOrderServiceImplTest`（**重点覆盖测算**）：
  - [ ] calculate：greigeOrderIds 为空报错；胚布单不存在报 1_030_006_000；胚布单无 product_id 报 1_030_009_003；无配比 BOM 报 1_030_008_004；多单多 BOM 明细合并 baseQty 正确；requiredQty = baseQty × (1 + lossRate/100)；purchaseQty = max(0, requiredQty - stockQty)；历史价回填（最近已下达 unit_price > 规格 ref_price 兜底）；sourceGreigeOrderIds/Nos 分组正确
  - [ ] create：order_no 缺省生成；撞号报 1_030_009_001；status 缺省 DRAFT；传 ACTIVE 直接下达；金额服务端权威重算
  - [ ] update：仅 DRAFT 可更新；明细整体替换
  - [ ] delete：仅 DRAFT 可删；级联逻辑删除明细
  - [ ] effect/complete/cancel：状态流转校验，非法流转报 1_030_009_002
- [ ] 前端 API `luna-ui/src/api/lunaerp-elmui/production/yarn-order/index.ts`：
  - [ ] URL 前缀 `/admin-api/lunaerp/production/yarn-order`
  - [ ] page/get/create/update/delete + effect/complete/cancel + calculate
  - [ ] TS 类型：YarnOrderVO / YarnOrderItemVO / YarnOrderSaveReqVO / YarnOrderCalculateReqVO / YarnOrderCalculateRespVO
  - [ ] dict.ts 追加 `lunaerp_yarn_order_status` 枚举（DRAFT/ACTIVE/COMPLETED/CANCELLED）
- [ ] 前端视图：
  - [ ] `yarn-order/index.vue` 列表页：搜索区（关键词+状态+单据日期区间+交货日期区间）+ 列（采购单号/单据日期/交货日期/状态 DictTag/关联胚布单号拼接/明细数/总金额/创建人/创建时间/操作）+ 新建按钮跳工作台无 id 路径 + 删除二次确认（仅草稿态）
  - [ ] `yarn-order/workspace/index.vue` 工作台骨架（核心流程交互）：
    - [ ] 顶部基本信息区：采购单号（新建时调 get-next-no 预览）+ 单据日期 + 交货日期 + 损耗率 + 备注 + 状态 DictTag
    - [ ] 关联胚布单区（el-table 多选 + 远程搜索）：远程搜索框（按胚布单号/产品名模糊）+ 已选胚布单表格（胚布单号/产品/成品量/损耗预设/操作移除）
    - [ ] **AI 测算按钮**（高亮 primary）：点击调 `POST /yarn-order/calculate`，入参 greigeOrderIds + lossRate
    - [ ] 采购明细表格（测算结果展示 + 可编辑）：列含纱线规格（材质+规格名）/ ratio 只读 / base_qty 只读 / required_qty 只读 / stock_qty 只读 / purchase_qty 可编辑 / 供应商每行独立远程搜索 / unit_price 可编辑默认 ref_unit_price / total_amount 只读（前端实时算 unit_price × purchase_qty）/ ref_unit_price 只读 / 备注 / 删除行
    - [ ] 底部汇总行：合计实采量 / 合计金额
    - [ ] 操作栏：草稿态 [保存草稿][下达][删除]，进行中态 [完成][取消]（**完成按钮的实际写入台账在 issue 06 落地**，本 issue 先调用 complete 接口不验证台账写入）
  - [ ] 路由注册 `/production/yarn-order/workspace/:id?`（remaining.ts 追加，参照 dyeing-order workspace 路由范例）
- [ ] 验证：
  - [ ] 后端 `mvn test -pl lunasrv-module-lunaerp -Dtest=YarnOrderServiceImplTest` 全绿（重点 calculate 测试用例）
  - [ ] 前端 `pnpm build:local` 与 `pnpm ts:check` 改动文件零报错
  - [ ] 手动验证：新建采购单 → 选 2 张胚布单（已有 product_id+配比 BOM）→ 填损耗率 → 点 AI 测算 → 明细表格正确展示（三量+历史价）→ 调供应商/单价 → 保存草稿/下达
