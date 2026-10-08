# 03: 对账单草稿链路（预览/生成/编辑/重取/删除/列表）

**What to build:** 财务在"供应商对账"选供应商 + 区间（缺省上月）点取数预览，看到加工费行与采购货款行，逐行填/改单价、加正负调整项后保存生成对账单（SR+日期+日流水单号）；预览与生成同源——生成瞬间服务端用同一取数服务重取，若某条明细已被别人抢走引用则明确报错"源明细已被对账单【单号】引用"；区间内什么都没有时拒绝生成空单。草稿可改单价/计价基准、删行、整体重取（调整项保留）、删除进回收站；列表实时显示每张单的已申请金额（不落库）。

**Blocked by:** 02（域骨架 + 取数服务）

**Status:** ready-for-agent

- [ ] `GET /supplier-recon/page`：筛选 supplierId/status/区间/keyword（单号或供应商名），默认排序透传；`GET /get` 内嵌 items + 只读"已申请金额"实时聚合（Σ 挂该单且 approval_status ∉ {REJECTED, CANCELLED} 的申请额），前端不自行计算
- [ ] `POST /preview`：入参 supplierId/periodStart/periodEnd，出参两类候选行不落库；供应商不存在报 002；区间内无可对账明细在生成时报 003（预览给空态）
- [ ] `POST /create`：服务端以同一取数服务重取并逐指纹比对，源明细被其他有效对账单抢引用报 004（消息带对方单号）；行金额服务端权威重算（PROCESS=bill_qty×unit_price、PURCHASE=数量×单价、ADJUST 直接取填写额），覆盖任何前端入参；total_amount=Σ行金额（含负调整项）
- [ ] ADJUST 行守卫：金额为 0 或未填备注报 005；recon_no 按 ADR-0006 按天取号并处理撞号
- [ ] `PUT /update` 仅 DRAFT：改行单价/计价基准（bill_qty 随基准切换重算）/删行/增改调整项，每次保存服务端重算 total；`PUT /re-fetch` 仅 DRAFT：重新取数替换全部 PROCESS/PURCHASE 行，ADJUST 行保留
- [ ] `DELETE /delete` 仅 DRAFT：软删并进回收站，注册 RecycleBinHandler bizType=VENDOR_RECON（恢复回 DRAFT，不补账——对账单本身不写账）
- [ ] 生成/编辑/重取 @LogRecord；金额类字段序列化标 @Sensitive(amount)；权限码 :query/:create/:update/:delete 生效
- [ ] Mockito 单测：预览与生成不一致报 004、服务端重算覆盖前端值、005 两种非法形态、003 空区间、单号撞号重试、草稿外状态调 update/delete 报 001、横向越权（他人租户 id 返回不存在）
