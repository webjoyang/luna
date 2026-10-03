# 05: 纱线规格双 Tab 组件 + 两级下拉 + 成本结构明细

**What to build:** 新增 ProductYarnCostSection.vue 组件，实现纱线规格和成本结构明细两个完整分区。纱线规格分区：针织/梭织双 Tab 切换，梭织下有简易估算/经纬精算两种子模式切换。表格列随模式动态变化（针织/简易：纱线规格/单价/比例/损耗率/成本贡献；经纬精算：纱线规格/单价/密度/织缩率/损耗率/每米用量/每米金额，按经纱/纬纱分组）。坯布参数输入（克重/门幅 → 每米重量自动计算）。新增 YarnSelect.vue 两级下拉组件（左侧材质分组从 lunaerp_production_yarn_type 取数，右侧规格项从 lunaerp_production_yarn_spec 取数，选中存 yarn_spec_id + yarn_spec_name 冗余，自动带出 ref_price 作为单价）。成本结构明细分区：成本项表格（供应商远程搜索存 id+name 冗余/工序/单价/损耗率）、加工费设置（针织模式显示针织加工费元/KG，梭织模式显示织造费元/M + 其他加工费元/M）、成本汇总展示（总成本/等效成本/每公斤约/未设价提示）。集成 calculateProductYarnCost 纯函数实时计算。保存时序列化 yarnSpecs/costItems 为 JSON 字符串提交。模式切换时不丢失已录入数据（字段按超集存储）。

**Blocked by:** 02 (Backend DO/VO alignment), 03 (Yarn cost calc function and tests), 04 (Frontend form restructure)

**Status:** done

- [x] ProductYarnCostSection.vue 组件实现针织/梭织双 Tab 切换
- [x] 梭织 Tab 下有简易估算/经纬精算两种子模式切换按钮
- [x] 针织/简易模式表格列：纱线规格/单价/比例%/损耗率%/成本贡献
- [x] 经纬精算模式表格列：纱线规格/单价/密度/织缩率%/损耗率%/每米用量/每米金额，按经纱/纬纱分组
- [x] YarnSelect.vue 两级下拉：左侧材质分组，右侧规格项，选中存 yarn_spec_id + yarn_spec_name
- [x] 选中纱线规格后自动带出 ref_price 作为单价
- [x] 坯布克重/门幅输入后自动计算每米重量（克重×门幅/100000）
- [x] 成本项表格：供应商远程搜索（存 id+name）/工序/单价/损耗率
- [x] 加工费设置随模式变化（针织：针织加工费/KG；梭织：织造费/M + 其他加工费/M）
- [x] 成本汇总展示：总成本/等效成本/每公斤约/未设价纱线数提示
- [x] 集成 calculateProductYarnCost 函数，输入变化时实时重算
- [x] 保存时 yarnSpecs/costItems 序列化为 JSON 字符串提交后端
- [x] 编辑已有产品时所有纱线行和成本项正确回显
- [x] 针织→梭织、简易→经纬精算模式切换时不丢失已录入纱线行数据

## Comments

- 2026-09-30（来自工单 02）：本分区依赖的后端列已全部就位（均 BigDecimal / String，后端不解析 JSON）：fabricConstruction（KNIT/WOVEN）、wovenCalculationMode（SIMPLE/PROFESSIONAL）、greigeWeightGsm、greigeWidthCm、knitProcessingFeePerKg、weavingFeePerMeter、otherProcessingFeePerMeter、packagingFee、taxRate、yarnSpecs、costItems。
  - 加工费/坯布参数/包装费/税率清空后若提交 null，`updateById` 会跳过该字段、旧值残留（同工单 04 的提醒）：数字输入框需以空串提交，或后续给 ProductDO 可空字段统一补 `@TableField(updateStrategy = FieldStrategy.ALWAYS)`。
  - 成本汇总为前端实时计算（工单 03 的 `calculateProductYarnCost`），后端不算、不存；costPrice/costStatus 仍由现有写入路径维护，本工单不要改。
- 2026-09-30（来自工单 04）：纱线规格与成本结构明细两个分区已按要求做成纯占位，本工单直接用 `ProductYarnCostSection.vue` 把这两块 `section-placeholder` 换掉即可（分区外壳 `.form-section` + `.section-title` 已就位，动画 delay 分别 0.11 / 0.14）。
  - **必须重新带上 yarnSpecs / costItems**：工单 04 的 `toProductSavePayload` 刻意不输出这两个键（缺键 → DO null → `updateById` 跳过 → 库里旧 JSON 不被覆盖）。本工单要把它们加回载荷，否则纱线/成本录入写不进库。
  - **表单字段请扩展 `components/product-form.ts`**（`ProductFormData` + `toProductFormData` + `toProductSavePayload`）并同步补 `__tests__/product-form.spec.ts`，不要回到 `ProductEditModal.vue` 里散写映射。该模块刻意零运行时依赖（不 import `@/utils/dict`，字典标签走注入的 `DictLabelResolver`），新增字段请保持这一约束。
  - **本工单的 9 个列需要同批补 `@TableField(updateStrategy = FieldStrategy.ALWAYS)`**：`fabricConstruction`、`wovenCalculationMode`、`greigeWeightGsm`、`greigeWidthCm`、`knitProcessingFeePerKg`、`weavingFeePerMeter`、`otherProcessingFeePerMeter`、`packagingFee`、`taxRate`。工单 04 只给它表单接管的列（greigeTypeId + 5 个梭织参数）加了 ALWAYS——提前给这 9 个列加反而会因载荷缺键在每次编辑时被刷成 null，所以留到本工单表单开始提交它们时一起加。加完记得同步 `ProductServiceImplTest.CLEARABLE_FIELDS`（该常量已有反射测试兜底），并注意 `doDelete` 已改为基于完整实体更新，新增局部 `updateById` 调用点前务必确认不会洗掉 ALWAYS 列。
