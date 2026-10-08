# 01: 销售退货 SQL + 领域模型基建（3 表 + stock_in 来源列 + 字典/菜单/审批配置）

**What to build:** 执行两个 SQL 脚本后，销售退货主表、行表、卷表及索引就位；`lunaerp_stock_in` 增加通用来源两列与部分唯一索引；3 个业务字典（状态/原因/质检结果）与 `lunaerp_approval_biz_type` 的 SALES_RETURN 增量、默认审批链配置、「销售退货」菜单与按钮权限可在系统管理查看；DO×3、Mapper×3、枚举与错误码常量就位可注入。脚本幂等可重复执行。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 3 张表 DDL 落库（`lunaerp_business_sales_return` / `_item` / `_roll`），均继承 TenantBaseDO、全列注释、DO 块主键判存
- [ ] 索引就位：主表 `(tenant_id, return_no)` 唯一（含软删行）、`(tenant_id, customer_id, status)`、`(tenant_id, shipment_id)`、`(tenant_id, status, approval_status)`；行表 `(tenant_id, return_id)`、`(tenant_id, shipment_item_id)`；卷表 `(tenant_id, return_id)`、`(tenant_id, shipment_roll_id)`、`(tenant_id, return_item_id)`
- [ ] `lunaerp_stock_in` 加 `source_doc_type varchar(32)`、`source_doc_id int8` 两可空列（ADD COLUMN IF NOT EXISTS）+ 注释
- [ ] 部分唯一索引 `uk_stock_in_source` ON `(source_doc_type, source_doc_id) WHERE deleted=0 AND source_doc_type IS NOT NULL`；普通索引 `(tenant_id, source_doc_type, source_doc_id)`
- [ ] 字典 2279 `lunaerp_sales_return_status`（DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED）、2280 `lunaerp_sales_return_reason`（QUALITY/WRONG/EXCESS/CUSTOMER/OTHER）、2281 `lunaerp_sales_return_qc_result`（RESTOCK/SCRAP），dict_data 取 3850–3869 块
- [ ] `lunaerp_approval_biz_type` 增量数据行 SALES_RETURN + 默认审批链配置项
- [ ] 菜单 10141（销售退货，挂 10100 业务管理 sort 4）+ 按钮 10142–10149（create/update/delete/submit/receive/post/cancel/print）权限码 `lunaerp:business:sales-return:*`
- [ ] DO×3（主/行/卷）含全部字段与类型；Mapper×3 继承 BaseMapperX；枚举类 SalesReturnStatus/Reason/QcResult；错误码常量入 business 错误码段 1-030-037
- [ ] `lunaerp_stock_in` 对应 Entity 同步加 sourceDocType/sourceDocId 字段与 MyBatis 映射
- [ ] 脚本幂等可重复执行（DDL IF NOT EXISTS，数据先 DELETE 再 INSERT）
