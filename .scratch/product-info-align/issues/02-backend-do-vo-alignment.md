# 02: 后端 DO/VO/Service 字段对齐

**What to build:** ProductDO 删除 output/patternNo/tempCode/conversionRate 4 个字段，新增 17 个字段及对应 @TableField 注解（wovenTotalEnds/wovenReedNo/wovenWarpDensity/wovenWeftDensity/wovenLoomWeftDensity/fabricConstruction/wovenCalculationMode/greigeWeightGsm/greigeWidthCm/knitProcessingFeePerKg/weavingFeePerMeter/otherProcessingFeePerMeter/fabricType/fabricTypeLabel/typeLabel/packagingFee/taxRate）。ProductSaveReqVO 和 ProductRespVO 同步增删字段。type 列存字典 value，新增 typeLabel 存字典 label；fabricType/fabricTypeLabel 同理。yarnSpecs/costItems 继续 String 类型 JSON 存取。Controller 无需新增端点，仅修改现有 create/update/get 的 VO 字段。

**Blocked by:** 01 (Schema migration and dict init)

**Status:** ready-for-agent

- [ ] ProductDO 删除 output/patternNo/tempCode/conversionRate，新增 17 个字段
- [ ] ProductSaveReqVO 同步增删字段，新增 typeLabel/fabricType/fabricTypeLabel 等
- [ ] ProductRespVO 同步增删字段，返回 typeLabel/fabricTypeLabel 用于前端展示
- [ ] type 列存字典 value，typeLabel 列存字典 label
- [ ] fabricType/fabricTypeLabel 成对存储
- [ ] yarnSpecs/costItems 仍为 String 类型，后端原样存取不解析 JSON 内部结构
- [ ] ProductServiceImpl 的 create/update/get 逻辑不报错（新字段正确持久化，旧字段不引用）
- [ ] 后端单元测试 ProductServiceImplTest 验证新字段持久化和旧字段为 null
