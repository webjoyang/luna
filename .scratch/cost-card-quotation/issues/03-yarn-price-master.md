# 03: 纱价主档端到端（DO/Service/Controller/取价/Excel/回收站/抽屉）

**What to build:** 用户在「生产管理 → 纱线档案」某规格行打开「价格记录」抽屉，分采购参考价（PURCHASE 缺省）/市场参考价（MARKET）两 Tab 维护价格（元/KG、生效日期、备注、来源 HAND/EXCEL/PURCHASE_AGG 预留枚举）；同一规格同一价类同一生效日期只能有一条记录，撞日报 1_030_008_009；取价永远取「不晚于今天的最近生效价」并展示完整历史曲线（日期倒序）；Excel 批量导入（仅新增、5000 行上限、错误行回 Excel、规格按 standard_name 或 材质+规格名匹配、价类必填、生效日期 yyyy-MM-dd、价格必须 >0）；删除走软删并可在回收站恢复（注册 W0 回收站 handler，主数据恢复无状态特殊处理）；抽屉展示「最近采购价」只读聚合（取最近一条已审核采购明细单价，不自动回写任何价格字段，供人工判断后录入）。`YarnPriceService#getEffectivePrice(specId, type, date)` 作为内部 Service 供成本卡调用：取 `effective_date <= date` 的未删最近一条；无记录回退 `yarn_spec.ref_price` 并在响应标记 `price_source=SPEC_REF_FALLBACK`（成本卡缺价统计：回退价不计 missing，但行上明示来源）；ref_price 也无才计缺价。SQL DDL 在 02 票已落库，本票只接 DO/VO/Service/controller + 前端。

**Blocked by:** 02（SQL 基建）

**Status:** ready-for-agent

- [ ] 后端 `business/production/yarnprice` 四层（DO/Mapper/Service/Controller）+ VO（SaveReqVO/PageReqVO/RespVO，带 Swagger 与 jakarta.validation）
- [ ] Controller 基础路径 `/lunaerp/production/yarn-price`；`GET /page`、`GET /list-by-spec`、`POST /create`、`PUT /update`、`DELETE /delete`；权限 `lunaerp:production:yarn-price:{query,create,update,delete}`
- [ ] `YarnPriceService#getEffectivePrice(specId, type, date)` 取价规则（不晚当日最近一条、价类隔离、无记录回退 ref_price 标 `SPEC_REF_FALLBACK`、ref_price 也无→缺价）；不对前端开放
- [ ] 撞同日报 `1_030_008_009`；删除/获取不存在报 `1_030_008_008`
- [ ] Excel 导入接 W0 Excel 契约：仅新增、5000 行上限、错误行回 Excel、规格按 standard_name 或 材质+规格名匹配
- [ ] W0 回收站 handler 注册（主数据，恢复无状态特殊处理）
- [ ] 最近采购价只读聚合：取最近一条已审核采购明细单价，不自动回写
- [ ] 前端：纱线档案规格行抽屉入口、两类 Tab、日期倒序历史、新增/编辑/删除校验（同日重复提示）、Excel 导入按钮与错误 Excel 下载、最近采购价只读区
- [ ] 前端 API 镜像 `src/api/lunaerp/production/yarn-price/`
- [ ] `YarnPriceServiceImplTest` 单测（纯 Mockito 不启 Spring）：取价 = 不晚当日最近一条、价类隔离、无价回退 spec.ref_price 且标记来源、ref_price 也无→缺价、同日唯一冲突、跨租户不可达
- [ ] 验收：建两个规格的采购价（含历史日期）→ Excel 导入一批 → 取价验证（含历史日期回退与 ref_price 回退两种路径）→ 软删 → 回收站恢复 → 内容完整
