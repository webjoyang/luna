# 07: 客户默认毛利率字段端到端（customer 表/VO/表单）

**What to build:** 用户能在客户档案表单维护「默认毛利率」字段（`default_margin_rate numeric(9,6)`，可空；空 = 回退全局缺省 15%），保存后供报价单选客户时预填该客户的行目标毛利率。SQL ALTER 列已在 02 票落库（`ADD COLUMN IF NOT EXISTS`），本票只接 DO/VO + 前端表单。这字段是 08 票报价单选客户预填的入参来源，刻意单独成票以便 08 票依赖其 DO 形状稳定。

**Blocked by:** 02（SQL 基建）

**Status:** ready-for-agent

- [ ] `CustomerDO` + VO（SaveReqVO/RespVO）加 `default_margin_rate` 字段，Swagger 注解完备
- [ ] 客户档案表单加「默认毛利率」字段控件（百分比数字，如 15 表示 15%，可空）
- [ ] 存量客户迁移后字段为 null，编辑保存不报错
- [ ] 单测覆盖字段持久化与默认值（空 = 回退全局 15% 的回退逻辑由 08 票报价单侧实现）
- [ ] 验收：能在客户表单录入并保存默认毛利率；重开页面回显；存量客户打开编辑不报错
