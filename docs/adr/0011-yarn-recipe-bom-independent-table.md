# 0011 — 纱线配比独立 yarn_recipe_bom 表

## 状态

accepted

## 上下文与决定

纱线配比（yarn recipe）独立成表 `lunaerp_production_yarn_recipe_bom`（主表）+ `lunaerp_production_yarn_recipe_bom_item`（明细表），独立于产品档案维护。配比主表通过 `product_id` 强关联到产品档案 `lunaerp_baseinfo_product.id`，一个产品一份配比。

明细表字段：`yarn_type_id`（关联纱线材质主表）+ `yarn_spec_id`（关联纱线规格子表）+ `ratio`（百分比数字 49 = 49%）+ `spin_method`（纺法文本，可空）+ `remark`（备注，可空）+ `ref_price`（参考单价，可空）。

胚布单主表 `lunaerp_production_greige_order` **新增 `product_id int8 NULL` 字段**强关联产品档案。保存胚布单时若选了产品档案则必填 `product_id`（同时保留现有 `product_code` / `product_name` 文本快照）。**胚布单不加 `yarn_specs` 快照字段**（与参考系统 linaagi `GreigeInstruction.yarnSpecs` 冗余存配比的模式刻意不一致），测算时后端按 `product_id` 实时关联 `yarn_recipe_bom` 读配比。

产品档案 `lunaerp_baseinfo_product` 已有 `yarn_specs` JSON 字段（`ProductDO` L82-84，与参考系统 linaagi `Product.yarnSpecs` 同构），**废弃不用**。配比权威源是 `yarn_recipe_bom` 表。已建字段保留不删（避免迁移），但 DO/VO 不再暴露此字段、SQL 不再写入此列、前端不再维护此字段。

## 理由

1. **配比独立成表理由**：产品档案的 `yarn_specs` JSON 字段是文本快照（`yarnSpec` 字符串如 `"32S 棉"`），无法关联到纱线主数据（`lunaerp_production_yarn_type` / `yarn_spec` 两表）的 ID。新项目要求明细行用 `yarn_type_id` + `yarn_spec_id` 强关联纱线主表，必须用结构化关联表而非 JSON 文本。
2. **胚布单加 product_id 理由**：`yarn_recipe_bom` 关联到 `product_id`，则胚布单测算时必须能强关联到产品档案拿 `product_id` 查配比。当前胚布单只有 `product_code` 弱关联（字符串），产品编号改了会脱钩。加 `product_id` 强关联保证测算链路可靠。
3. **胚布单不加 yarn_specs 快照理由**：参考系统冗余存是为了测算时不查产品档案（性能 + 解耦），但新项目后端测算接口可以接受 join 查询；且配比是独立表（不在产品档案），冗余存到胚布单反而需要同步两份配比数据。直接实时查 `yarn_recipe_bom` 更干净。
4. **废弃产品档案 yarn_specs 字段理由**：避免双源不一致——若保留 `yarn_specs` 同时有 `yarn_recipe_bom` 表，配比可能在两处维护。废弃 `yarn_specs` 字段统一权威源。

## 考虑过的备选

- **复用产品档案 yarn_specs JSON 字段（不新建配比表）**：与参考系统 linaagi 完全一致。但 `yarn_specs` 是 JSON 文本字段，无法关联到纱线主表 ID，前端管理页必须做复杂 JSON 编辑器，且无法做"按纱线规格反查所有配比"的反向查询。
- **胚布单加 yarn_specs 快照字段（参考系统模式）**：与参考系统一致，测算时不依赖产品档案。但新项目配比独立成表，胚布单冗余存需要同步两份配比，反而复杂。
- **保留产品档案 yarn_specs 字段作"默认配比"**：双源可能不一致，配比可能在两处维护。废弃统一权威源更干净。
- **配比关联 product_code（弱关联，参考系统模式）**：产品编号改了会脱钩。强关联 `product_id` 更可靠。

## 后果

- 胚布单 DDL/DO/SQL 需加 `product_id` 字段；胚布单保存 VO 需加 `productId` 字段；前端胚布单编辑页需加产品档案下拉（远程搜索选产品，选中后带出 `product_code` / `product_name` 文本快照写入主表）。
- 产品档案 DO 的 `yarnSpecs` 字段废弃，前端产品档案管理页不再维护此字段（已建的保留不删）。
- 测算时后端按胚布单 `product_id` 查 `yarn_recipe_bom`（含明细），按 `yarn_type_id` / `yarn_spec_id` 关联纱线主表读规格文本 + 参考价。
- 配比管理有独立管理页（`YarnRecipeBomController` + 列表/编辑页），用户可在配比管理页为每个产品维护配比。
- 错误码段位 `1-030-008` 给纱线档案 + 配比 BOM 共用。
