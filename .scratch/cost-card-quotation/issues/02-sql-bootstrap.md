# 02: SQL 基建（6 新表 + 3 ALTER + 菜单/按钮 + 7 字典）

**What to build:** 落单文件 `luna-srv/sql/postgresql/lunaerp-business-cost-card-quotation.sql`，幂等可重复执行（事务内先 DELETE 再 INSERT 菜单/字典，CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 序列 + 全列 COMMENT），体例对齐 `lunaerp-baseinfo-customer.sql` / `lunaerp-business-sales-contract.sql`。覆盖：6 张新表（生产域 `lunaerp_production_yarn_price`、业务域 `lunaerp_business_cost_card` + `_yarn` 纱线行子表 + `_fee` 费用行子表、`lunaerp_business_quotation` + `_item` 明细子表，全部继承 TenantBaseDO 标准列、int8 主键 + 独立序列、金额 `numeric(18,4)`）；3 张 ALTER（`yarn_recipe_bom_item` +5 列、`lunaerp_baseinfo_customer` +`default_margin_rate`、`lunaerp_business_sales_contract` +`source_quotation_id`/`source_quotation_no`，全用 `ADD COLUMN IF NOT EXISTS`）；索引含部分唯一索引（同品同时仅一生效卡 `WHERE status='EFFECTIVE' AND deleted=0`、同号至多一草稿 `WHERE status='DRAFT' AND deleted=0`、卡号/报价号+版本号全量唯一含删、纱价 `(yarn_spec_id, price_type, effective_date) WHERE deleted=0`）+ 取价查询索引 `(yarn_spec_id, price_type, effective_date DESC)`；菜单 10132 成本卡 + 10136 报价单挂 10100 销售目录（sort 0 排在合同/发货/出口单证之前），按钮 10133-10135/10137-10140，纱价按钮 10245-10248 挂 10240 纱线档案；7 字典 type 2262-2268 + dict_data 3770-3791（标签颜色与 spec 一致，`lunaerp_quotation_status` 的 EXPIRED 标签 remark 注明「仅展示派生态，禁止写入」）。脚本仅做语法自查不直连共享库。完整字段清单、字典颜色、菜单 path/component/componentName 见 spec.md「Data Model」「字典」「菜单与路由」三节。

**Blocked by:** 01（横切地基 prefactor）

**Status:** ready-for-agent

- [x] 单文件 SQL 落库，重跑不报错且菜单/字典/表不重复
- [x] 6 新表 DDL 含 TenantBaseDO 标准列、主键 DO 块判存、序列、全列 COMMENT
- [x] 部分唯一索引就位：同品一生效卡 / 同号一草稿 / 卡号含删 / 报价号+版本号含删 / 纱价 (spec_id, type, effective_date) WHERE deleted=0
- [x] 取价辅助索引 `(yarn_spec_id, price_type, effective_date DESC)` 就位
- [x] 3 ALTER 用 `ADD COLUMN IF NOT EXISTS`，存量行默认 null
- [x] 10132 成本卡菜单 + 10136 报价单菜单挂 10100（sort 0）；按钮位 10133-10135 / 10137-10140 / 10245-10248 注册
- [x] 7 字典 2262-2268 + dict_data 3770-3791 初始化，标签颜色与 spec 一致
- [x] `lunaerp_quotation_status` 的 EXPIRED 标签 remark 注明「仅展示派生态，禁止写入」
- [x] 脚本头部含幂等 DO 块（避让 032/033 已占段位、菜单 id 段位、字典 id 段位的防重声明）
