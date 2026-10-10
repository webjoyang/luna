# 01: SQL 基建（3 新表 DDL + 前置加列 + 菜单 + 字典 + 错误码登记）

**What to build:** 执行 `luna-srv/sql/postgresql/lunaerp-business-export-doc.sql` 后，本模块 3 张新表（出口单证套主表 `lunaerp_business_export_doc` / 明细行表 `lunaerp_business_export_doc_item` / 卖方出口配置表 `lunaerp_business_export_seller_config`）连同序列、索引（含单证号唯一索引含软删行、卖方配置 (tenant_id) 唯一索引）一并就位；销售合同表 +4 外贸条款列（incoterm/port_of_loading/port_of_discharge/payment_term）、客户档案表 +2 英文列（name_en/address_en）经 ALTER ADD COLUMN IF NOT EXISTS 落地；菜单 10126–10131（出口单证父项 + 5 个子按钮）注册到父菜单 10100 销售下；字典 2258–2261（单证状态/Incoterm/付款方式/运输方式）就位；错误码段位 1-030-033 登记进 shared GLOSSARY 段位总表（000 单证套 / 100 明细 / 200 卖方配置三个分区）。脚本参照 shipment/contract 脚本体例（CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 列注释 + 索引），在空库与存量库重跑均幂等、零报错、零数据覆盖。

**Blocked by:** None（可立即开工）

**Status:** ready-for-human

- [x] 3 张新表 DDL + 序列 + 索引就位：主表 (status)/(customer_id)/(contract_id)/(shipment_id) 索引 + (tenant_id, doc_no) 唯一索引含软删行；明细 (doc_id) 索引；卖方配置 (tenant_id) 唯一索引
- [x] 销售合同表 +4 列、客户档案表 +2 列，全部 `ADD COLUMN IF NOT EXISTS` 幂等带 COMMENT
- [x] system_menu 10126–10131 先 DELETE 再 INSERT，父菜单 10100，工作台走前端隐藏路由（仿发货工作台）
- [x] system_dict_type 2258–2261 + dict_data（id 从 3750 起，已避让 sample-analysis 3709 起、product-info-align 3702–3708、color-instruction 3736–3749，按占用情况平移）
- [x] 错误码段位 1-030-033 登记进 shared GLOSSARY 段位总表（EXPORT_DOC_NOT_EXISTS / NO_DUPLICATE / STATUS_NOT_ALLOW / REFERENCE_NOT_FOUND / ITEM_EMPTY / CONTRACT_NOT_EXPORT / SOURCE_REQUIRED + 100 明细 + 200 卖方配置 SAVE_FAILED）
- [x] install `20 商业化` 段在 approval-engine 之后登记本脚本；`_order.md` 第 22 位已就位无需改动
- [x] 空库（10 段先建表）与存量库重跑两次均幂等、零报错、零数据覆盖
