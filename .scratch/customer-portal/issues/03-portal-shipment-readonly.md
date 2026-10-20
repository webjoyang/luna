# 03: 门户发货单只读（列表+详情，id/?no= 双形态，越权必测）

**What to build:** 客户在门户查看本客户名下 COMPLETED 发货单：列表（状态 Tab + 时间倒序分页）与详情（头 + 逐行明细），金额透明展示（含运费）。同时支持 `/portal/shipment/:id` 与二维码冻结契约 `/portal/shipment?no=` 双形态直达（同一组件）。可 demo：客户打开门户→切 Tab 看发货单→点详情看逐行明细与金额；A 客户 token 打不开 B 客户的单。

**Blocked by:** 02（PortalTokenFilter 校验链 + 门户前端骨架）

**Status:** closed

- [x] `GET /app-api/lunaerp/portal/shipment/page`：按 PortalContext customerId 天然过滤，仅 COMPLETED，时间倒序分页；返回单号/日期/物流/签收状态/金额合计/运费；无搜索框（列表仅时间倒序分页 + 状态 Tab）
- [x] `GET /app-api/lunaerp/portal/shipment/get?id=`：头 + 逐行明细（品名/规格/色号/数量/卷数）；归属校验 customerId 不符 → 038-001
- [x] 金额透明：门户客户看自己的单据属对账场景，`@Sensitive(AMOUNT)` 内部权限位不适用，金额/运费正常透出
- [x] 双形态：`/portal/shipment/:id` 与 `/portal/shipment?no=`（04 号冻结二维码契约不回改）同一组件——有 `:id` 用 id，有 `?no=` 按 no 查询，守卫等价；无会话 `?no=` 直达（无 token）→ invalid 页引导先从业务员链接进入
- [x] `PortalShipmentRespVO` 白名单断言：序列化输出不含客户手机号、内部备注、成本相关字段；金额字段存在且正确
- [x] 前端 `shipment/list.vue`（Tab：待签收/已签收/已拒收/全部）+ `shipment/detail.vue`（逐行明细）；门户不提供打印
- [x] 越权专测：A 客户 token 访问 B 客户发货单（id 与 `?no=` 双入口）→ 038-001；跨租户 token（租户 X token 访问租户 Y 数据）→ 038-001；列表 customerId 过滤断言
