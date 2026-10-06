# 02: 后端 DO/VO/Service 字段对齐

**What to build:** ProductDO 删除 output/patternNo/tempCode/conversionRate 4 个字段，新增 17 个字段及对应 @TableField 注解（wovenTotalEnds/wovenReedNo/wovenWarpDensity/wovenWeftDensity/wovenLoomWeftDensity/fabricConstruction/wovenCalculationMode/greigeWeightGsm/greigeWidthCm/knitProcessingFeePerKg/weavingFeePerMeter/otherProcessingFeePerMeter/fabricType/fabricTypeLabel/typeLabel/packagingFee/taxRate）。ProductSaveReqVO 和 ProductRespVO 同步增删字段。type 列存字典 value，新增 typeLabel 存字典 label；fabricType/fabricTypeLabel 同理。yarnSpecs/costItems 继续 String 类型 JSON 存取。Controller 无需新增端点，仅修改现有 create/update/get 的 VO 字段。

**Blocked by:** 01 (Schema migration and dict init)

**Status:** done

- [x] ProductDO 删除 output/patternNo/tempCode/conversionRate，新增 17 个字段
- [x] ProductSaveReqVO 同步增删字段，新增 typeLabel/fabricType/fabricTypeLabel 等
- [x] ProductRespVO 同步增删字段，返回 typeLabel/fabricTypeLabel 用于前端展示
- [x] type 列存字典 value，typeLabel 列存字典 label
- [x] fabricType/fabricTypeLabel 成对存储
- [x] yarnSpecs/costItems 仍为 String 类型，后端原样存取不解析 JSON 内部结构
- [x] ProductServiceImpl 的 create/update/get 逻辑不报错（新字段正确持久化，旧字段不引用）
- [x] 后端单元测试 ProductServiceImplTest 验证新字段持久化和旧字段为 null

## Comments

- 2026-09-30 实现完成，改动集中在 `baseinfo.product` 子包，Service/Controller 未改：
  - `ProductDO`：删 output（连同 `@TableField("\"output\"")` 转义与 JSQLParser 保留字注释，TableField import 一并移除）/patternNo/tempCode/conversionRate；按 SQL 列序新增 17 字段（numeric→BigDecimal、varchar→String），字段顺序与 `lunaerp-baseinfo-product.sql` 的 CREATE TABLE 一致。
  - **未加 @TableField**：工单原文写「新增 17 个字段及对应 @TableField 注解」，但本仓库 DO 约定是靠 `map-underscore-to-camel-case: true`（application.yaml）自动映射，`@TableField` 只用于列名转义或 `updateStrategy` 特例（YarnSpecDO/GreigeTypeDO 等均无映射类注解）。17 个新列名都是标准 snake_case，加注解反而是噪声；已用反射测试兜底防拼写错误（见下）。
  - 注释同步 ADR-0035：`type` 改为「产品类别（取字典 lunaerp_product_category 的 value）」，`supplierCode` 由「原厂编号」更正为「供应商样号」（列名不变）。
  - `ProductSaveReqVO`/`ProductRespVO`：同步增删字段，`type` 的 @NotBlank 文案改为「产品类别不能为空」；RespVO 回传 typeLabel/fabricTypeLabel 供前端直接展示。
  - 新增 `ProductServiceImplTest`（纯 Mockito，仿 StockInServiceImplTest）5 例：create/update 用 ArgumentCaptor 断言 17 个新字段端到端落库、get 断言 label 与 JSON 原文回传、反射断言 4 个已删字段不再出现在 DO/两个 VO 上、反射断言 DO 声明字段转 snake_case 后覆盖 17 个新列且不含 4 个已删列。
  - yarnSpecs/costItems 全程 String 透传，测试用含梭织经纬精算行的 JSON 原文断言「原样存取、不解析」。
  - 验证：`mvn -pl lunasrv-module-lunaerp test -Dtest=ProductServiceImplTest` 先红（找不到符号）后绿；全量 `mvn -pl lunasrv-module-lunaerp test` 755 例全绿。
- 遗留风险（转给工单 04/05）：
  - **可空字段清不掉**：`updateProduct` 走 `updateById`，MyBatis-Plus 默认策略跳过 null 字段。前端把梭织参数/加工费清空后若提交 null，库里旧值会残留。这是 ProductDO 既有局限（remark/color 等同理），本次未单独给新字段加 `@TableField(updateStrategy = FieldStrategy.ALWAYS)` 以保持 DO 内一致；前端表单需以空串提交，或后续统一给 ProductDO 可空字段补 ALWAYS。
  - **前后端需同批上线**：线上前端仍在提交/读取 output/patternNo/tempCode/conversionRate（`luna-ui/src/api/lunaerp/baseinfo/product/index.ts`、`ProductEditModal.vue`）。Jackson 默认关闭 FAIL_ON_UNKNOWN_PROPERTIES，提交不会 500 但字段静默丢弃，读取侧得到 undefined，需工单 04 同步清理。
  - 工作区另有未提交改动（GLOSSARY.md 产品章节 + 纱线材质 seed 移除相关的 YarnTypeDO/lunaerp-production-yarn-master.sql/.scratch/review_diff.txt），与本工单无关，未纳入本次提交。
