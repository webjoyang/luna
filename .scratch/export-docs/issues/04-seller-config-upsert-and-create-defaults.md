# 04: 卖方出口配置 upsert + 创建带出

**What to build:** 落地卖方出口配置表 `lunaerp_business_export_seller_config`（一租户一行、tenant_id 唯一）的读写端点：`GET /seller-config` 读本租户配置（不存在返回空对象，不报错）；`PUT /seller-config` upsert（不存在即建、存在即更新，并发兜底报 EXPORT_DOC_SELLER_CONFIG_SAVE_FAILED），不提供删除端点。创建单证时（03 的 create 流程补此逻辑）从租户配置缺省带出卖方英文名/地址/收款银行信息（seller_name/seller_address/seller_bank_info），**仅创建时带出，此后编辑不回写**——卖方配置后续修改不影响已存在的单证套。配置缺失时创建单证降级为空字段不报错。维护入口为列表页"卖方出口资料"按钮 + 弹窗（前端入口待 09 接，本票只交付后端端点 + 权限位 `lunaerp:business:export-doc:seller-config`）。**租户隔离专测**：A 租户读不到 B 租户配置。

**Blocked by:** 03（DO/Service 骨架 + create 流程）

**Status:** ready-for-human

- [x] GET/PUT `/seller-config` 端点，upsert 幂等（首次/再次），并发兜底报 EXPORT_DOC_SELLER_CONFIG_SAVE_FAILED，无删除端点
- [x] 创建单证时从租户配置缺省带出卖方英文名/地址/银行（仅创建时带出，此后编辑不回写）
- [x] 配置缺失时创建单证降级为空字段不报错；GET 不存在返回空对象不报错
- [x] 权限位 `lunaerp:business:export-doc:seller-config`
- [x] 单测：upsert 幂等、创建时配置带出、此后编辑不回写、配置缺失降级、**租户隔离专测**（A 租户读不到 B 租户配置）
