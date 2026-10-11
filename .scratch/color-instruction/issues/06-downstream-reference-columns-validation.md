# 06: 下游引用列与保存校验

**What to build:** 胚布单与染色单表各加 `color_instruction_id BIGINT NULL` + `color_instruction_no VARCHAR NULL` 两列（对照 shipment from-contract 显式列 + 单号快照做法，不引入通用 sourceType/sourceId）。下游保存时校验：引用打色单与当前单据同租户、未删除、状态为 CONFIRMED 或 IN_PRODUCTION（否则报下游各自段的关联引用错误），一张胚布单/染色单至多引用一张打色单。前端在胚布单/染色单编辑页加打色单远程选择器，选中后带出单号快照。本期不实现投产自动触发（留 07 票）。

**Blocked by:** 05 确认动作与三道守卫

**Status:** resolved

- [x] 胚布单表 SQL alter 加 `color_instruction_id BIGINT NULL` + `color_instruction_no VARCHAR NULL`；染色单表同构两列；脚本可重复执行（ADD COLUMN IF NOT EXISTS 或等价幂等模式）
- [x] 胚布单保存（创建即 ACTIVE）时校验：引用打色单同租户、未删除（`deleted=0`）、状态 ∈ {CONFIRMED, IN_PRODUCTION}（否则报胚布单段关联引用错误，沿用胚布单错误码风格）；一张胚布单至多引用一张打色单
- [x] 染色单保存时同校验（沿用染色单错误码风格）；DRAFT 态保存也校验引用合法性
- [x] 引用打色单被软删/作废后，下游保存时引用校验应拦截（下游侧报引用错误）
- [x] 前端胚布单/染色单编辑页加打色单远程选择器：按客户/产品/单号模糊搜索 CONFIRMED/IN_PRODUCTION 打色单，选项显示"单号+客户+产品+颜色行摘要"，选中后带出 instruction_no 快照；可清空（取消引用）
- [x] 测试覆盖：下游保存引用校验各失败路径（他租户、已删、状态非 CONFIRMED/IN_PRODUCTION、引用打色单被软删）、一张下游单至多一张打色单、引用打色单与下游单客户/产品口径一致（同客户同产品）
