# ADR-0035: 产品信息字段对齐旧系统与纱线成本计算迁移

**日期**：2026-09-30
**状态**：已接受

## 背景

新系统（luna）的「基础信息-产品信息」模块在从旧系统（linaagi）迁移时，遗漏了多个业务字段和逻辑，且前端布局与旧系统不一致。经 grilling 会话（3 轮 17 问）与用户确认，需要做以下对齐：

### 缺失内容
1. **供应商样号**：字段 `supplierCode` 存在但标签错误（"原厂编号"应为"供应商样号"）
2. **布种**：缺失字典下拉选择（旧系统从 `FABRIC_TYPE` 字典取数）
3. **梭织参数**：5 个字段（总经根数/筘号/经密/纬密/上机纬密）完全缺失
4. **纱线规格双 Tab**：旧系统支持针织/梭织切换，梭织下还有简易估算/经纬精算两种模式
5. **成本结构明细计算**：旧系统有完整的纱线成本计算逻辑（`product-yarn-cost.ts`），新系统只有简单表格
6. **产品类别字典化**：`type` 字段应为字典 `PRODUCT_CATEGORY` 下拉，而非自由文本

### 需删除的多余字段
- `output`（产量）、`patternNo`（花型号）、`tempCode`（临时编号）、`conversionRate`（算长系数）

## 决策

### D1: 数据库表结构变更（`lunaerp_baseinfo_product`）

**删除列**：`output`、`pattern_no`、`temp_code`、`conversion_rate`

**新增列**：

| 列名 | 类型 | 说明 |
|------|------|------|
| `fabric_type` | varchar(64) | 布种字典值（`FABRIC_TYPE`） |
| `fabric_type_label` | varchar(128) | 布种字典标签（名称冗余） |
| `type_label` | varchar(128) | 产品类别字典标签（名称冗余，`type` 列保留存字典值） |
| `woven_total_ends` | numeric(18,2) | 总经根数 |
| `woven_reed_no` | numeric(18,2) | 筘号 |
| `woven_warp_density` | numeric(18,2) | 经密（根/CM） |
| `woven_weft_density` | numeric(18,2) | 纬密（根/CM） |
| `woven_loom_weft_density` | numeric(18,2) | 上机纬密（根/CM） |
| `fabric_construction` | varchar(16) | 织物结构（KNIT 针织 / WOVEN 梭织） |
| `woven_calculation_mode` | varchar(16) | 估算方式（SIMPLE 简易 / PROFESSIONAL 经纬精算） |
| `greige_weight_gsm` | numeric(18,2) | 坯布克重（GSM） |
| `greige_width_cm` | numeric(18,2) | 坯布门幅（CM） |
| `knit_processing_fee_per_kg` | numeric(18,4) | 针织加工费（元/KG） |
| `weaving_fee_per_meter` | numeric(18,4) | 织造费（元/M） |
| `other_processing_fee_per_meter` | numeric(18,4) | 其他加工费（元/M） |
| `packaging_fee` | numeric(18,4) | 包装费 |
| `tax_rate` | numeric(18,4) | 税率 |

**设计依据**：
- 梭织参数和成本设置项均用独立列（不沿用旧系统的 `extensionData` JSON），用户明确要求（Q3/Q7/Q15）。
- 字典字段采用 value + label 冗余存储：`type` 存 dict_value，新增 `type_label` 存 dict_label；`fabric_type` 同理（Q11/Q13）。RuoYi 字典习惯用 value 关联，跨环境迁移时 value 比 id 稳定。
- `supplierCode` 仅改标签为"供应商样号"，无列变更（Q1）。

### D2: 纱线规格 JSON 结构（`yarn_specs`）

后端继续以 JSON 字符串存储，不关心内部结构。前端根据模式动态渲染不同列。

行数据结构（统一超集）：
```json
{
  "yarnSpecId": 123,          // 纱线规格ID（两级下拉从纱线档案选择）
  "yarnSpecName": "32S 棉",   // 纱线规格名称（冗余）
  "yarnSpec": "32S",          // 手动输入时的文本回退
  "spinMethod": "环锭纺",
  "ratio": "60",              // 针织/梭织简易模式用
  "remark": "",
  "price": 25.00,
  "wasteRate": 3,             // 损耗率%
  "direction": "WARP",        // 梭织经纬精算模式用（WARP经纱/WEFT纬纱）
  "density": 52,              // 梭织经纬精算模式用
  "crimpRate": 5,             // 梭织经纬精算模式用
  "consumptionKgPerMeter": 0.012  // 梭织经纬精算模式用
}
```

### D3: 成本项 JSON 结构（`cost_items`）

```json
{
  "supplierId": "uuid",
  "supplier": "供应商名称（冗余）",
  "process": "工序",
  "price": 5.00,
  "lossPercent": 2.5
}
```

### D4: 前端页面布局（5 个分区，按顺序）

1. **图片**：封面图 + 详情多图
2. **基础信息**：产品编号、品名、类别（字典 `PRODUCT_CATEGORY` 下拉 value+label）、供应商样号、成分、备注、产品标签、对外展示/首页推荐
3. **技术规格**：门幅、克重、颜色、工艺、机型、布种（字典 `FABRIC_TYPE` 下拉 value+label）、胚布品种、标准米长、梭织参数（5 个独立字段）
4. **纱线规格**：针织/梭织双 Tab → 梭织下简易估算/经纬精算 → 纱线从纱线档案两级下拉（左材质 → 右规格，id+name 冗余） → 表格列随模式动态变化
5. **成本结构明细**：成本项表格（供应商/工序/单价/损耗率）+ 加工费设置 + 成本汇总展示（总成本/等效成本/每公斤约）

### D5: 成本计算逻辑迁移

从旧系统 `product-yarn-cost.ts` 迁移以下计算逻辑到新系统：

- **针织模式**：纱线成本 = Σ(单价 × 比例% / (1 - 损耗率))；总成本 = 纱线成本 + 针织加工费(元/KG)
- **梭织简易估算**：每米重量 = 坯布克重 × 坯布门幅 / 100000；纱线成本 = 每米重量 × Σ(单价 × 比例% / (1 - 损耗率))；总成本 = 纱线成本 + 织造费 + 其他加工费
- **梭织经纬精算**：纱线成本 = Σ(每米用量 × 单价 / (1 - 损耗率))，按经纱/纬纱分组；总成本 = 纱线成本 + 织造费 + 其他加工费
- **最终成本** = (总成本 + 包装费) × (1 + 税率)

### D6: 字典数据初始化

在 `system_dict_data` 表中创建：
- 字典类型 `PRODUCT_CATEGORY`：产品类别（成品/胚布/半成品等，值由后台配置）
- 字典类型 `FABRIC_TYPE`：布种（针织/梭织/经编/纬编等，值由后台配置）

### D7: 纱线规格两级下拉

复用新系统已有的纱线档案表（`lunaerp_production_yarn_type` 材质主表 + `lunaerp_production_yarn_spec` 规格子表），实现两级下拉：
- 第一级：材质分组（`yarn_type.name`）
- 第二级：该材质下的规格项（`yarn_spec.spec_name` + `luster` 光泽去重展示）
- 选中后存 `yarn_spec_id`（Long）+ `yarn_spec_name`（String）到 yarnSpecs JSON 行中

## 验收标准

1. 数据库迁移脚本可重复执行（CREATE TABLE IF NOT EXISTS / ALTER TABLE ADD COLUMN IF NOT EXISTS）
2. 前端表单 5 个分区按 D4 顺序排列
3. 针织/梭织双 Tab 切换时表格列正确变化
4. 梭织简易/经纬精算切换时表格列正确变化
5. 纱线两级下拉从纱线档案取数，选中后 id+name 冗余存储
6. 字典下拉（产品类别/布种）从 `system_dict_data` 取数，value+label 冗余存储
7. 成本计算结果与旧系统 `calculateProductYarnCost` 一致
8. 删除字段 `output`/`pattern_no`/`temp_code`/`conversion_rate` 不影响已有数据（字段已清空或不存在）

## 相关

- 旧系统代码：`oldweb/app/staff/components/product/product-yarn-cost.ts`（成本计算逻辑）
- 旧系统代码：`oldweb/app/staff/components/product/ProductYarnCostSection.tsx`（前端组件）
- 旧系统代码：`oldweb/app/staff/components/YarnSelect.tsx`（两级下拉）
- 旧系统代码：`oldweb/app/staff/components/production/product-record/ProductEditableArchiveView.tsx`（布局参考）
- 旧系统字段账本：`oldserver/src/services/form-config/field-ledger.ts`
- 新系统产品表：`luna-srv/sql/postgresql/lunaerp-baseinfo-product.sql`
- 新系统纱线档案：`luna-srv/sql/postgresql/lunaerp-production-yarn-master.sql`（ADR-0011）
- 新系统前端：`luna-ui/src/views/lunaerp/baseinfo/product/components/ProductEditModal.vue`
- 新系统 DictSelect：`luna-ui/src/components/FormCreate/src/components/DictSelect.vue`
