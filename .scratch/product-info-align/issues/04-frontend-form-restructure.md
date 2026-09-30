# 04: 前端表单重构 — 5 分区布局 + 删除字段 + 字典下拉 + 梭织参数

**What to build:** 重构 ProductEditModal.vue 为 5 个分区（按顺序）：图片（封面+详情多图）、基础信息（产品编号/品名/类别下拉/供应商样号/成分/备注/产品标签/对外展示/首页推荐）、技术规格（门幅/克重/颜色/工艺/机型/布种下拉/胚布品种/标准米长/梭织参数5字段）、纱线规格（占位，下个 ticket 填充）、成本结构明细（占位，下个 ticket 填充）。删除产量/花型号/临时编号/算长系数 4 个字段。类别字段改为 DictSelect（字典 lunaerp_product_category，存 dict_value + dict_label 冗余）。布种字段改为 DictSelect（字典 lunaerp_fabric_type，存 dict_value + dict_label 冗余）。技术规格分区新增 5 个梭织参数输入框（总经根数/筘号/经密/纬密/上机纬密，全选填）。supplierCode 标签改为"供应商样号"。

**Blocked by:** 02 (Backend DO/VO alignment)

**Status:** done

- [x] ProductEditModal.vue 表单按 5 分区顺序排列（图片→基础信息→技术规格→纱线规格→成本结构明细）
- [x] 删除产量(output)/花型号(patternNo)/临时编号(tempCode)/算长系数(conversionRate) 4 个表单字段
- [x] 类别字段使用 DictSelect 组件，从 lunaerp_product_category 字典取数，选中存 dict_value + dict_label
- [x] 布种字段使用 DictSelect 组件，从 lunaerp_fabric_type 字典取数，选中存 dict_value + dict_label
- [x] 技术规格分区新增 5 个梭织参数输入框（总经根数/筘号/经密/纬密/上机纬密）
- [x] supplierCode 字段标签显示为"供应商样号"
- [x] 纱线规格和成本结构明细分区有占位区域，不报错
- [x] 保存产品时新字段（梭织参数/布种/类别标签）正确提交到后端
- [x] 编辑已有产品时所有新字段正确回显

## Comments

- 2026-09-30（来自工单 02）：后端契约已就绪——ProductSaveReqVO/ProductRespVO 已删 output/patternNo/tempCode/conversionRate，已增 typeLabel、fabricType/fabricTypeLabel 与 5 个梭织参数（wovenTotalEnds/wovenReedNo/wovenWarpDensity/wovenWeftDensity/wovenLoomWeftDensity，均 BigDecimal）。
  - 前端 `api/lunaerp/baseinfo/product/index.ts` 的类型定义需同步删 4 增 17，否则提交的旧字段会被后端静默丢弃（Jackson 不报错）。
  - **清空字段注意**：后端 `updateProduct` 用 `updateById`，null 字段被 MyBatis-Plus 跳过、库里旧值残留。梭织参数用 el-input-number 清空后若提交 null 就删不掉旧值，需以空串提交（或回头给 ProductDO 可空字段补 `@TableField(updateStrategy = FieldStrategy.ALWAYS)`）。
- 2026-09-30 实现完成，前端为主 + 一处后端补丁：
  - **新增纯函数层** `luna-ui/src/views/lunaerp/baseinfo/product/components/product-form.ts`：`createDefaultProductFormData` / `toProductFormData`（详情→回显，BigDecimal 字符串统一转 number，null/空串/非法值归一为 undefined）/ `toProductSavePayload`（回显→提交，可空数值列以 null 提交、字典标签按当前字典重算）。不依赖 Vue 响应式与 pinia，配套 `__tests__/product-form.spec.ts` 17 例（先红后绿），前端全量 `pnpm test` 31 例全绿。
  - **ProductEditModal.vue** 重排为 5 分区：图片（封面+详情多图）→ 基础信息（编号/品名/类别 DictSelect/供应商样号/成分/备注/产品标签/对外展示+首页推荐）→ 技术规格（门幅/克重/颜色/工艺/机型/布种 DictSelect/胚布品种/标准米长 + 梭织参数 5 字段）→ 纱线规格（占位）→ 成本结构明细（占位）。原「胚布品种」「产品标签」独立分区并入对应分区；删除 output/patternNo/tempCode/conversionRate 4 个字段与 yarnRows/costRows 动态行表格。
  - **字典标签冗余**：`typeLabel` / `fabricTypeLabel` 在提交时由 `toProductSavePayload` 按当前字典重算；字典查不到该值（未加载/已停用）时沿用后端原标签，避免把冗余列洗空。`DICT_TYPE` 新增 `LUNAERP_PRODUCT_CATEGORY` / `LUNAERP_FABRIC_TYPE`；`product-form.ts` 内另有一份同值常量，为的是让纯函数模块不 import `@/utils/dict`（该模块顶层就 `useDictStoreWithOut()`，node 环境单测会炸）。
  - **DictSelect 复用**：直接用 `@/components/FormCreate/src/components/DictSelect.vue`，并给它补 `inheritAttrs: false`（三分支多根节点下 attrs 本就无法自动继承、只会刷 "Extraneous non-props attributes" 警告，已由各分支 `v-bind="attrs"` 手动透传，行为不变）。
  - **API 类型同步** `api/lunaerp/baseinfo/product/index.ts`：删 4 增 17，可空数值列声明为 `number | null`、`greigeTypeId` 声明为 `string | null`，用类型表达「提交 null = 清空」。
  - **列表页兜底**：ProductGrid/ProductTable 的类别列改展示 `typeLabel || type`，否则字典化后会显示 FINISHED 这类原始值（spec Out of Scope #5 只说不改列表页交互，此处是字典化引入的必要兜底）。
  - **后端补丁（承接工单 02 遗留风险）**：给 `ProductDO` 的 `greigeTypeId`、`fabricType`/`fabricTypeLabel`（布种选填可清空，value 与冗余标签成对）以及 5 个梭织参数加 `@TableField(updateStrategy = FieldStrategy.ALWAYS)`；同时把 `ProductServiceImpl.doDelete` 从「new 一个只带 id/status 的局部 DO」改为基于已 `selectByIds` 查出的完整实体更新，否则 ALWAYS 列会在逻辑删除时被刷成 null。`ProductServiceImplTest` 补 2 例（反射断言 8 个字段策略为 ALWAYS、断言 deleteProduct 不洗掉可空列），模块全量 `mvn test` 757 例全绿。数字列无法用空串绕过（Jackson 会把 "" 强制转成 null，仍被 NOT_NULL 策略跳过），所以只能走 ALWAYS 这条路。
  - **CodeReview 评审**：有条件通过 → 已修。评审指出布种（fabricType/fabricTypeLabel）清空后提交的是 `undefined`，JSON 丢键 → 后端 null → 被 NOT_NULL 策略跳过，与梭织参数修复前同一个坑；已改为成对提交 `null` 并给两列补上 ALWAYS（ProductVO 的这两个字段类型同步宽化为 `string | null`，`product-form.spec.ts` 拆成 18 例）。
  - 类别（type/typeLabel）不做同样处理：type 是必填项（前端规则 + 后端 @NotBlank），没有清空路径，字典缺失时不带 typeLabel 键反而能保住后端原标签。
- 遗留（转给工单 05）：
  - **纱线规格/成本结构明细当前是纯占位**，提交载荷刻意不带 `yarnSpecs` / `costItems` 两个键——缺键 → DO 为 null → `updateById` 跳过 → 库里已有 JSON 不被覆盖。工单 05 接上 `ProductYarnCostSection.vue` 后必须重新带上这两个键，否则纱线/成本录入永远写不进去。
  - **工单 05 的列还没加 ALWAYS**：`fabricConstruction`、`wovenCalculationMode`、`greigeWeightGsm`、`greigeWidthCm`、`knitProcessingFeePerKg`、`weavingFeePerMeter`、`otherProcessingFeePerMeter`、`packagingFee`、`taxRate` 本次未加（表单还没接管它们，提前加反而会因载荷缺键被刷成 null）。工单 05 开始提交这些字段时，必须同批给它们补 `@TableField(updateStrategy = FieldStrategy.ALWAYS)`，并同步 `ProductServiceImplTest.CLEARABLE_FIELDS`（已加至 8 项：greigeTypeId/fabricType/fabricTypeLabel + 5 个梭织参数）。
  - 扩展表单字段请改 `product-form.ts` 的两个映射函数并同步补 `product-form.spec.ts`，不要再往 `ProductEditModal.vue` 里塞散落的映射逻辑。
  - 工作区仍有与本工单无关的未提交改动（CONTEXT.md 产品章节、YarnTypeDO/lunaerp-production-yarn-master.sql 的 seed 移除、.scratch/review_diff.txt），同工单 02 处理口径，未纳入本次提交。
