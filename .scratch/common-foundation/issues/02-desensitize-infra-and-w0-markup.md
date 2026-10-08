# 02: 脱敏注解基础设施 + W0 字段标注

**What to build:** 普通业务员看客户列表时联系人手机号显示 `138****5678`、车间账号在任何界面和导出里都看不到金额只显示 `****`，主管账号看到全量。交付自研 `@Sensitive(seePermission, type)` 权限条件脱敏注解、`SensitiveType` 枚举（MOBILE/CHINESE_NAME/BANK_CARD/FIXED_PHONE/EMAIL/AMOUNT，前五个复用底座 handler 规则，AMOUNT 为本 spec 新增：无权限序列化为字符串 `"****"`、有权限返回原值）、Jackson 条件序列化器（调用 `SecurityFrameworkService.hasPermission`，与 `@ss` 同源，无登录上下文按脱敏处理），两个固定权限位 `lunaerp:sensitive:amount` / `lunaerp:sensitive:contact` 以 type=3 按钮 menu 注册，并在现有 RespVO/导出 VO 上按 W0 清单标注（纱线规格 refPrice=AMOUNT、产品成本/价格类=AMOUNT、客户/供应商联系人姓名=CHINESE_NAME、手机/座机/银行账号/邮箱=对应类型、主体联系电话/银行信息）。列表/详情/导出同口径；打印走独立 print-data VO 豁免（不在脱敏 VO 上加开关绕过）；更新接口以 ID 为准且不接受 `"****"` 回写。

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] `@Sensitive` 注解 + `SensitiveType` 枚举（6 类型）落在 `common/desensitize` 子包
- [ ] AMOUNT handler：无权限序列化 `"****"`，有权限 BigDecimal 正常序列化
- [ ] Jackson 条件序列化器：调用 `SecurityFrameworkService.hasPermission(seePermission)`，无登录上下文（定时任务/MQ）按脱敏处理
- [ ] 两个权限位 `lunaerp:sensitive:amount`、`lunaerp:sensitive:contact` 以 type=3（按钮，无页面）菜单注册到系统管理目录 SQL（可重复执行）
- [ ] 现有 VO 标注清单全部落标：纱线规格 refPrice（RespVO/导出 VO）=AMOUNT；产品成本/价格类字段（含 costItems 内单价同标）=AMOUNT；客户/供应商联系人 RespVO 姓名=CHINESE_NAME、手机=MOBILE、座机=FIXED_PHONE、银行账号=BANK_CARD、邮箱=EMAIL；主体上联系电话/银行信息按对应类型
- [ ] 越权测试：构造有/无两类权限用户，对列表/详情/导出分别断言；写接口不接受 `"****"` 入库（更新以 ID 为准不依赖脱敏字段回传）
- [ ] 默认授予建议在角色管理可勾选：amount 授租户管理员/财务/老板，contact 授业务员/销售主管/管理员
