# 02: 前置改动 - 销售合同外贸条款 + 客户英文名址（DO/VO/表单/Excel 导入）

**What to build:** 销售合同模块的 DO/VO 同步加 4 字段（incoterm/port_of_loading/port_of_discharge/payment_term），create/update 表单新增"外贸条款"区块——**仅 trade_type=EXPORT 时显示与校验**（非必填，仅控制显隐），内销合同完全无感；详情页只读展示该区块。客户档案模块的 DO/VO 加 name_en/address_en 两可空字段，表单/详情加两个可空输入。客户 Excel 导入模板同步加"英文名称/英文地址"两可空列（协调 common-foundation 08 客户导入模板，W0 施工时一并加列；若 CF-08 已先落地则补列，未落地则随其施工时生效，不阻塞本票主流程）。字典复用本模块 01 新建的 lunaerp_incoterm / lunaerp_payment_term（合同与单证共用一套，不重复建字典）。不做历史合同/客户数据回填。两项改动均为可空加列，对存量功能零影响。

**Blocked by:** 01（SQL 加列需先落地）

**Status:** ready-for-human

- [x] 销售合同 DO `SalesContractDO` +4 字段，create/update VO 同步，表单新增"外贸条款"区块仅 trade_type=EXPORT 显示与校验（非必填），详情只读展示
- [x] 客户档案 DO +name_en/address_en 两可空字段，表单/详情加两个可空输入
- [x] 客户 Excel 导入模板同步加"英文名称/英文地址"两可空列（协调 common-foundation 08；CF-08 已落地则补列，未落地则随其施工生效）
- [x] 字典复用 01 新建的 lunaerp_incoterm / lunaerp_payment_term，不重复建字典
- [x] 不做历史合同/客户数据回填；内销合同完全无感
