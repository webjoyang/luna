# 04: 资金流水分页 + 收支总览（账户汇总 + 类别钻取 + 毛利参考卡片）

**What to build:** 财务在账户上可看统一流水分页（业务日期、借/贷、类别 DictTag、往来主体、原币金额、汇率、本位币金额、来源单号、备注），可按账户/期间/类别/方向/主体/来源单号筛选，来源单号跳转源单；老板在"收支总览"选期间（缺省本月）即见：按账户期初/本期流入/流出/期末+合计行、按类别汇总本期借贷发生额（点击钻取流水）、同页"经营毛利参考（管理口径）"卡片（收入=AR_INC 净额、成本=11 号 confirmed 对账单，差额/毛利率，永久标注"管理参考、期间错配、非法定报表"；应收侧无分录时空态说明）。

**Blocked by:** 03

**Status:** closed

- [x] `GET /ledger/page`：10 号 ledger 查询的账户视图参数化——accountId/category/direction/party/期间/keyword（匹配 ref_no·party_name）；默认 biz_date DESC, id DESC；来源单号按 ref_type→路由映射可跳转源单
- [x] `GET /overview/summary?periodStart=&periodEnd=&accountId=`：每账户 期初(opening_balance)/本期流入/本期流出/期末（口径自洽，期末=期初+流入−流出，与 current_balance 可核对），合计行汇总
- [x] 类别借贷汇总（summary 同接口或并列返回）；`GET /ledger/page` 带 category 筛选即钻取落地
- [x] `GET /overview/gross-margin?periodStart=&periodEnd=`：收入=区间发货日口径 AR_INC−AR_INC_REVERSAL 净额（直接查 ledger_entry，事实由 10 号 shipment 审核事件化保证，不复制发货白名单）；成本=确认日落区间 11 号 confirmed 对账单 total_amount；永远 200、无 enabled 开关；10/11 号未落地时以 Service 内 mock seam 表达（沿用 26 号 06 票先例），测试 mock 之；无 AR 分录时收入区空态说明
- [x] 权限码 overview:query / ledger 查询随 account:query；本票无写端点
- [x] Mockito 单测：账户期初/流入/流出/期末口径（含跨账户合计与合计行）/类别汇总与钻取筛选透传/毛利=AR_INC 净额−confirmed 对账单（11 号 Mapper mock）/无 AR 数据空态不报错/分页默认排序/横向越权报不存在
