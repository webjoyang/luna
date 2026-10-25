# 02: 制单取数 + 草稿 CRUD + 防超退聚合

**What to build:** 销售内勤在「销售退货」列表点新建，选择一张已发货（COMPLETED）的发货单，系统自动带出客户、合同、币种、原发货仓库与发货明细行（产品/色号/缸号/原数量/单价/可退量）；按行录入退货数量（≤可退量）、必填原因、可改单价，服务端重算金额并守卫行金额≤可退量×原单价；逐卷登记（勾选原发货卷带回重量/米数可改，或手填散退卷号），每行至少一卷、卷号必填、重量/米数至少一项；可保存草稿（可改可删）、可退量随在途审批单实时占量扣减。行费卷量差异仅前端黄色提示不拦截。

**Blocked by:** 01 (SQL + 领域模型基建)

**Status:** closed

- [x] `GET /shipment-candidates`：客户远程筛选，仅返回 COMPLETED 且含可退量>0 行的发货单；非 COMPLETED/他租户发货单报 003
- [x] `GET /shipment-detail?shipmentId=`：返回发货头快照（客户/合同/币种/仓库）+ 逐行（原数量/原单价/Σ占量/可退量）+ 卷清单（原卷号/重量/米数）
- [x] `GET /page` 筛选（customerId/shipmentNo/status/approvalStatus/keyword/日期区间）、`GET /get?id=` 返回头+行+卷
- [x] `POST /create` / `PUT /update`：聚合差异保存（行/卷整单替换，ADR-0032）；服务端重算 total_amount/total_pieces
- [x] `DELETE /delete?id=`：仅 status=DRAFT 可删，级联软删行/卷
- [x] `SalesReturnAvailabilityService`（唯一口径）：可退量=发货明细 quantity − Σ占量退货行 return_qty；占量谓词常量化 `status<>'CANCELLED' AND approval_status IN ('APPROVING','APPROVED')`；可退额=可退量×发货行原单价
- [x] 校验：行原因必填（008）、卷号必填、重量/米数至少一项、行/卷血缘必须属于头单 shipment_id（行 004、卷 007）；数量超可退报 005、金额超可退额报 006
- [x] 行 return_qty 与卷合计仅前端软提示，后端不拦截（软齐套）
- [x] 单号 `RT-YYYYMMDD-NNN` 按天取号（ADR-0006 发货 SH 模式），租户内全量唯一含软删行
- [x] 前端：列表页（搜索区+状态双徽章列+操作按状态显隐）+ 编辑工作台（发货单选择→只读头快照+退货仓库可改→行表带可退量→卷区按行展开勾选原卷或手填→行费卷量差异黄色提示条→保存草稿）
- [x] 租户隔离：写接口校验归属当前租户，横向越权报不存在
