# 基本信息模块 - 产品信息对齐旧系统（字段补全 + 梭织参数 + 纱线规格双Tab + 成本计算迁移 + 布局重构）

Status: ready-for-agent

## Problem Statement

新系统（luna）的「基础信息-产品信息」模块在从旧系统（linaagi）迁移时，遗漏了多个关键业务字段和逻辑，且前端布局与旧系统不一致。具体表现为：

1. **缺失字段**：供应商样号标签错误（"原厂编号"应为"供应商样号"）、布种（需字典下拉）、梭织参数（总经根数/筘号/经密/纬密/上机纬密 5 个字段完全缺失）、产品类别未字典化（type 应从字典 PRODUCT_CATEGORY 下拉）。
2. **缺失逻辑**：纱线规格缺少针织/梭织双 Tab 设计（梭织下还分简易估算/经纬精算两种模式）、成本结构明细的计算逻辑完全缺失（旧系统有完整的纱线成本计算）。
3. **多余字段**：产量（output）、花型号（patternNo）、临时编号（tempCode）、算长系数（conversionRate）需要删除。
4. **布局不对**：前端表单分区顺序应为「图片→基础信息→技术规格→纱线规格→成本结构明细」，当前为「基本信息→胚布品种→产品标签→纱线规格→成本项→产品图片」。
5. **纱线规格选择方式**：当前为手动文本输入，应改为从纱线档案两级下拉选择（左材质→右规格），存 id+name 冗余。

详见 ADR-0035。

## Solution

对 `lunaerp_baseinfo_product` 表做结构变更（删 4 列、加 17 列），对后端 DO/VO 做对应修改，对前端 `ProductEditModal.vue` 做布局重构和组件拆分，迁移旧系统的纱线成本计算逻辑（`product-yarn-cost.ts`），创建两个字典类型（PRODUCT_CATEGORY、FABRIC_TYPE）的初始化数据。

### 数据库变更

**删除列**：`output`、`pattern_no`、`temp_code`、`conversion_rate`

**新增列**（17 列）：

| 列名 | 类型 | 说明 |
|------|------|------|
| `fabric_type` | varchar(64) | 布种字典值（FABRIC_TYPE） |
| `fabric_type_label` | varchar(128) | 布种字典标签（名称冗余） |
| `type_label` | varchar(128) | 产品类别字典标签（名称冗余，type 列保留存字典值） |
| `woven_total_ends` | numeric(18,2) | 总经根数 |
| `woven_reed_no` | numeric(18,2) | 筘号 |
| `woven_warp_density` | numeric(18,2) | 经密（根/CM） |
| `woven_weft_density` | numeric(18,2) | 纬密（根/CM） |
| `woven_loom_weft_density` | numeric(18,2) | 上机纬密（根/CM） |
| `fabric_construction` | varchar(16) | 织物结构（KNIT/WOVEN） |
| `woven_calculation_mode` | varchar(16) | 估算方式（SIMPLE/PROFESSIONAL） |
| `greige_weight_gsm` | numeric(18,2) | 坯布克重（GSM） |
| `greige_width_cm` | numeric(18,2) | 坯布门幅（CM） |
| `knit_processing_fee_per_kg` | numeric(18,4) | 针织加工费（元/KG） |
| `weaving_fee_per_meter` | numeric(18,4) | 织造费（元/M） |
| `other_processing_fee_per_meter` | numeric(18,4) | 其他加工费（元/M） |
| `packaging_fee` | numeric(18,4) | 包装费 |
| `tax_rate` | numeric(18,4) | 税率 |

**标签变更**：`supplier_code` 的业务标签从"原厂编号"改为"供应商样号"（无列变更）。

### 前端布局（5 分区，按顺序）

1. **图片**：封面图 + 详情多图
2. **基础信息**：产品编号、品名、类别（字典 PRODUCT_CATEGORY 下拉，value+label 冗余）、供应商样号、成分、备注、产品标签、对外展示/首页推荐
3. **技术规格**：门幅、克重、颜色、工艺、机型、布种（字典 FABRIC_TYPE 下拉，value+label 冗余）、胚布品种、标准米长、梭织参数（5 个独立字段）
4. **纱线规格**：针织/梭织双 Tab → 梭织下简易估算/经纬精算 → 纱线从纱线档案两级下拉（id+name 冗余）→ 表格列随模式动态变化
5. **成本结构明细**：成本项表格（供应商 id+name 冗余 / 工序 / 单价 / 损耗率）+ 加工费设置 + 成本汇总展示

### 成本计算逻辑

从旧系统 `product-yarn-cost.ts` 迁移三种模式的成本计算：
- **针织**：纱线成本 = Σ(单价 × 比例% / (1 - 损耗率))；总成本 = 纱线成本 + 针织加工费
- **梭织简易估算**：每米重量 = 坯布克重 × 坯布门幅 / 100000；纱线成本 = 每米重量 × 混纺单价；总成本 = 纱线成本 + 织造费 + 其他加工费
- **梭织经纬精算**：按经纱/纬纱分组，纱线成本 = Σ(每米用量 × 单价 / (1 - 损耗率))；总成本同上
- **最终成本** = (总成本 + 包装费) × (1 + 税率)

## User Stories

### 字典与数据初始化

1. 作为系统管理员，我想在 system_dict_data 表中有 `lunaerp_product_category` 字典类型（如成品/胚布/半成品），以便产品类别字段下拉取数。
2. 作为系统管理员，我想在 system_dict_data 表中有 `lunaerp_fabric_type` 字典类型（如针织/梭织/经编/纬编），以便布种字段下拉取数。
3. 作为系统管理员，我想字典初始化 SQL 脚本可重复执行（先 DELETE 再 INSERT），以便不影响已有数据。

### 数据库迁移

4. 作为开发者，我想产品表删除 output、pattern_no、temp_code、conversion_rate 四列，以便清理不需要的字段。
5. 作为开发者，我想产品表新增 17 列（布种 2 列 + 类别标签 1 列 + 梭织参数 5 列 + 成本设置 7 列 + 包装费/税率 2 列），以便支撑完整的业务字段。
6. 作为开发者，我想迁移脚本可重复执行（ALTER TABLE ADD COLUMN IF NOT EXISTS），以便安全地在已部署环境上运行。

### 后端 DO/VO/Controller

7. 作为开发者，我想 ProductDO 删除 output/patternNo/tempCode/conversionRate 字段，新增 17 个字段及对应 @TableField 注解（output 列已删，无需 @TableField 转义），以便与表结构一致。
8. 作为开发者，我想 ProductSaveReqVO 和 ProductRespVO 同步增删字段，以便前端接口契约对齐。
9. 作为开发者，我想 type 字段存字典 value，新增 type_label 存字典 label，以便前端下拉选中后同时存 value+label。
10. 作为开发者，我想 fabric_type 和 fabric_type_label 同理存 value+label，以便布种字段支持字典冗余。
11. 作为开发者，我想 yarnSpecs 和 costItems 继续以 JSON 字符串存储，后端不关心内部结构，以便前端灵活控制行数据格式。

### 前端 - 布局重构

12. 作为产品管理员，我想编辑表单的第一个分区是"图片"（封面图 + 详情多图），以便优先上传产品视觉资料。
13. 作为产品管理员，我想第二个分区是"基础信息"（产品编号、品名、类别下拉、供应商样号、成分、备注、标签、对外展示/首页推荐），以便录入产品身份属性。
14. 作为产品管理员，我想第三个分区是"技术规格"（门幅、克重、颜色、工艺、机型、布种下拉、胚布品种、标准米长、梭织参数 5 字段），以便录入规格参数。
15. 作为产品管理员，我想第四个分区是"纱线规格"（双 Tab + 动态表格），以便按织物结构录入纱线配比。
16. 作为产品管理员，我想第五个分区是"成本结构明细"（成本项表格 + 加工费 + 汇总），以便核算产品成本。

### 前端 - 字典下拉

17. 作为产品管理员，我想"类别"字段是字典下拉（从 lunaerp_product_category 取数），选中后存 dict_value + dict_label，以便标准化分类。
18. 作为产品管理员，我想"布种"字段是字典下拉（从 lunaerp_fabric_type 取数），选中后存 dict_value + dict_label，以便标准化布种。
19. 作为产品管理员，我想字典下拉使用新系统已有的 DictSelect 组件，以便保持 UI 一致性。

### 前端 - 纱线规格双 Tab

20. 作为产品管理员，我想纱线规格区域有"针织"和"梭织"两个 Tab 切换按钮，以便根据织物结构选择录入模式。
21. 作为产品管理员，当我选择"针织"Tab 时，表格列显示：纱线规格（两级下拉）、单价、比例%、损耗率%、成本贡献，以便录入针织纱线配比。
22. 作为产品管理员，当我选择"梭织"Tab 时，出现"简易估算"和"经纬精算"两个子模式切换按钮，以便选择估算精度。
23. 作为产品管理员，当我选择"梭织-简易估算"时，表格列与针织相同（纱线规格/单价/比例/损耗率/每米金额），上方多出"坯布克重/坯布门幅/每米重量"三个输入/展示，以便估算每米成本。
24. 作为产品管理员，当我选择"梭织-经纬精算"时，表格按"经纱"和"纬纱"分组显示，列变为：纱线规格/单价/密度/织缩率%/损耗率%/每米用量/每米金额，以便精确计算经纬纱用量。

### 前端 - 纱线两级下拉

25. 作为产品管理员，我想纱线规格字段是两级下拉（左侧材质分组如棉/涤/人棉，右侧该材质下的规格项如 32S/40S），以便从纱线档案中选择而非手动输入。
26. 作为产品管理员，选中纱线规格后，自动带出单价（来自纱线档案 ref_price），以便减少手动录入。
27. 作为产品管理员，选中纱线规格后，存储 yarn_spec_id（Long）+ yarn_spec_name（String）到 yarnSpecs JSON 行中，以便 id 关联+名称冗余。
28. 作为产品管理员，如果纱线档案中没有需要的规格，我想能在下拉中快速新增（可选，MVP 可不实现），以便不中断录入流程。

### 前端 - 成本结构明细

29. 作为产品管理员，我想成本结构明细区域有一个成本项表格（供应商/工序/单价/损耗率），以便录入额外加工成本。
30. 作为产品管理员，成本项的"供应商"字段支持远程搜索选择，选中后存 supplierId + supplier(name) 冗余，以便关联供应商档案。
31. 作为产品管理员，我想根据织物结构模式（针织/梭织），成本区域显示不同的加工费输入（针织：针织加工费元/KG；梭织：织造费元/M + 其他加工费元/M），以便匹配成本计算口径。
32. 作为产品管理员，我想成本汇总区域展示：总成本（元/KG 或元/M）、等效成本（元/KG）、每公斤约多少米（梭织）/ 配比合计（针织），以便一眼掌握成本概况。
33. 作为产品管理员，我想如果某些纱线未设置价格，汇总区域提示"还有 N 根纱未设置价格，当前成本不完整"，以便提醒补全。

### 前端 - 梭织参数

34. 作为产品管理员，我想在"技术规格"分区有 5 个梭织参数输入框（总经根数/筘号/经密/纬密/上机纬密），以便录入梭织分析参数。
35. 作为产品管理员，梭织参数全部选填，不影响保存，以便不强制要求所有产品都填。

### 前端 - 删除字段

36. 作为产品管理员，编辑表单中不再出现产量、花型号、临时编号、算长系数字段，以便界面简洁聚焦于必要字段。

### 保存与回显

37. 作为产品管理员，保存产品时，前端将 yarnSpecs 和 costItems 序列化为 JSON 字符串提交，后端原样存入对应列，以便后端不关心 JSON 内部结构。
38. 作为产品管理员，编辑已有产品时，所有字段（含梭织参数、成本设置、布种/类别标签）正确回显，以便修改时不丢数据。
39. 作为产品管理员，切换针织/梭织、简易/经纬精算模式后，已有纱线行数据不丢失（字段按超集存储），以便模式切换不丢录入内容。

## Implementation Decisions

### 模块范围

- **后端 `baseinfo.product` 子包**：修改 `ProductDO`、`ProductSaveReqVO`、`ProductRespVO`、`ProductController`（如有必要）、`ProductServiceImpl`
- **前端 `lunaerp/baseinfo/product/`**：重构 `ProductEditModal.vue`，新增 `ProductYarnCostSection.vue`（纱线规格+成本计算组件）、`YarnSelect.vue`（两级下拉组件）
- **SQL 脚本**：更新 `lunaerp-baseinfo-product.sql`（表结构变更）+ 新增字典初始化 SQL

### 后端接口契约

- `ProductSaveReqVO`：删除 output/patternNo/tempCode/conversionRate；新增 17 个字段；type 和 fabric_type 存 dict value，type_label 和 fabric_type_label 存 dict label
- `ProductRespVO`：同步增删字段，type_label 和 fabric_type_label 返回给前端用于展示
- yarnSpecs / costItems 继续为 String 类型（JSON 字符串），后端原样存取
- ProductController 无需新增端点，仅修改现有 create/update/get 的 VO 字段

### 前端组件拆分

- `ProductEditModal.vue`：主弹窗，按 5 分区组织表单，协调各子组件
- `ProductYarnCostSection.vue`：纱线规格+成本结构明细的独立组件（参考旧系统 `ProductYarnCostSection.tsx`），包含：
  - 织物结构 Tab 切换（针织/梭织）
  - 梭织估算方式切换（简易/经纬精算）
  - 坯布参数输入（克重/门幅 → 每米重量计算）
  - 纱线行表格（列随模式动态变化）
  - 加工费输入
  - 成本汇总展示
- `YarnSelect.vue`：纱线规格两级下拉组件（参考旧系统 `YarnSelect.tsx`），调用纱线档案 API 获取材质+规格数据

### 成本计算函数

前端独立纯函数 `calculateProductYarnCost(specs, settings)`，从旧系统 `product-yarn-cost.ts` 迁移：
- 输入：纱线行数组 + 成本设置对象
- 输出：{ ratioTotal, missingPriceCount, yarnCostPerKg, yarnCostPerMeter, processingCost, totalCost, kgPerMeter, metersPerKg, basis }
- 损耗口径：投入损耗 = 净耗用 ÷ (1 - 损耗率)
- 针织：basis = 'KG'，总成本 = 纱线成本/KG + 针织加工费/KG
- 梭织：basis = 'METER'，每米重量 = 克重 × 门幅 / 100000，总成本 = 纱线成本/M + 织造费 + 其他加工费

### 字典初始化 SQL

在 `system_dict_type` 和 `system_dict_data` 表中插入：
- `lunaerp_product_category`：字典类型 + 若干默认值（成品/胚布/半成品）
- `lunaerp_fabric_type`：字典类型 + 若干默认值（针织/梭织/经编/纬编）

### 纱线规格 JSON 行结构（超集）

```json
{
  "yarnSpecId": 123,
  "yarnSpecName": "32S 棉",
  "yarnSpec": "32S",
  "spinMethod": "环锭纺",
  "ratio": "60",
  "remark": "",
  "price": 25.00,
  "wasteRate": 3,
  "direction": "WARP",
  "density": 52,
  "crimpRate": 5,
  "consumptionKgPerMeter": 0.012
}
```

### 成本项 JSON 行结构

```json
{
  "supplierId": "uuid",
  "supplier": "供应商名称",
  "process": "工序",
  "price": 5.00,
  "lossPercent": 2.5
}
```

## Testing Decisions

### 测试策略

成本计算是本 spec 的核心复杂逻辑，需要重点测试。测试 seam 选在前端纯函数层（与旧系统 `product-yarn-cost.test.ts` 一致）和后端 service 层。

### 前端单元测试

- **测试目标**：`calculateProductYarnCost` 纯函数
- **测试文件**：`luna-ui/src/views/lunaerp/baseinfo/product/components/__tests__/product-yarn-cost.spec.ts`
- **测试用例**（参考旧系统 `product-yarn-cost.test.ts`）：
  - 针织模式：单根纱/多根纱/含损耗/无价格
  - 梭织简易：坯布克重/门幅 → 每米重量计算/混纺单价/含损耗
  - 梭织经纬精算：经纱/纬纱分组/每米用量/含损耗
  - 边界：空数组/全零值/负值钳制/损耗率超 100% 钳制
- **断言口径**：金额精度 4 位小数（`Math.round(value * 10000) / 10000`）

### 后端 Service 层测试

- **测试目标**：`ProductServiceImpl` 的 create/update 逻辑（确保新字段正确持久化、删除字段不报错）
- **测试文件**：`ProductServiceImplTest.java`（参考 `StockInServiceImplTest.java` 模式）
- **测试用例**：
  - 创建产品含全部新字段
  - 更新产品删除旧字段值（output/patternNo/tempCode/conversionRate 为 null）
  - yarnSpecs/costItems JSON 字符串原样存取
  - type + type_label、fabric_type + fabric_type_label 成对存储

### 前端组件测试（可选）

- **测试目标**：`ProductYarnCostSection.vue` 的模式切换行为
- 验证针织→梭织切换时表格列变化
- 验证简易→经纬精算切换时经纱/纬纱分组出现

## Out of Scope

1. **AI 成衣设计**（aiDesignUrl/aiDesignCategory/aiDesignDescription）——不在本 spec 范围，保持现状
2. **产品标签管理**——保持现有 ProductTagZone 组件不变
3. **胚布品种选择**——保持现有 GreigeTypePicker 组件不变
4. **纱线档案管理**——使用已有的纱线档案模块（ADR-0011），不修改
5. **产品列表页**——不在本 spec 范围，仅改编辑弹窗
6. **字段配置中心**（旧系统的 field-ledger / showSystemField / labelOf 机制）——不迁移，新系统字段显隐固定
7. **extensionData 机制**——旧系统用 extensionData 存梭织参数和成本设置，新系统全部改用独立列，不保留 extensionData
8. **产品图片缩略图**——保持现有上传组件不变
9. **拍照建档草稿**（captureDraftId）——不在本 spec 范围
10. **事业部归属**（businessOwnership）——不在本 spec 范围

## Further Notes

- 本 spec 的所有设计决策来自 grilling 会话（3 轮 17 问），详见 ADR-0035
- 旧系统参考代码路径见 ADR-0035 的「相关」部分
- 纱线规格两级下拉的数据源 API 已存在（纱线档案模块的 cascade-options 端点），前端直接调用
- `output` 列名是 JSQLParser 保留字，删除后无需再处理 `@TableField("\"output\"")` 转义
- 成本计算纯函数应提取为独立 TS 文件（`product-yarn-cost.ts`），不依赖 Vue 响应式，以便单元测试
