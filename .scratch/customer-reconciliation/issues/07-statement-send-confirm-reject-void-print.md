# 07: 客户对账单 — 发送/确认/驳回/作废 + 打印

**What to build:** 对账单状态机 DRAFT→SENT→CONFIRMED（终态快照冻结）；SENT→REJECTED（必填异议原因）→改后回 DRAFT 重新发出；非终态可 VOID（释放行占用，不删分录）；CONFIRMED 不可作废。全部写端点状态白名单守卫，send/void/confirm 用条件 UPDATE 防并发。内部「代客确认」（`:confirm-on-behalf` 权限，必填线下方式 confirm_method + 备注）与门户确认落同一状态字段。对账单打印接 04 号内核，print-data 独立 VO 豁免脱敏 + @LogRecord 留痕 + 二维码（基址未配置降级提示不阻断）。

**Blocked by:** 06 客户对账单 — 生成/列表/详情/DRAFT 增删改

**Status:** ready-for-agent

- [ ] `PUT /send?id=`：DRAFT→SENT，条件更新
- [ ] `PUT /void?id=`：非终态（DRAFT/SENT/REJECTED）→VOID，释放行占用（不删分录）；CONFIRMED 报 STATEMENT_CONFIRMED_NOT_VOID
- [ ] `PUT /confirm-on-behalf?id=`（`:confirm-on-behalf` 权限，必填 confirm_method）：SENT→CONFIRMED，记录 confirm_type=ON_BEHALF / confirm_by_name / confirm_time / confirm_method / remark
- [ ] `PUT /reject-reply?id=`：回备注后可重新 SENT（SENT→REJECTED 必填 reject_reason，改后回 DRAFT）
- [ ] 状态机白名单守卫（STATEMENT_STATUS_NOT_ALLOW），条件 UPDATE 防并发
- [ ] `GET /{id}/print-data`：独立 VO 豁免 @Sensitive，含客户四要素/全量行/qrUrl；@LogRecord 留痕
- [ ] 打印模板 `FINANCE_STATEMENT` 注册 04 号内核（A4 纵向）；QrSlot 二维码 `{luna.portal.base-url}/portal/statement?no={statement_no}`，基址未配置降级提示不阻断
- [ ] 确认后源单红冲/退货不回改不阻塞（冲减发生在下期对账单）；SENT 期源单红冲门户行标红
- [ ] 前端：列表状态标签、工作台（生成/发送/确认/驳回/作废按钮按状态白名单显示）、打印预览
- [ ] 测试：状态流转（DRAFT→SENT→CONFIRMED、SENT→REJECTED→DRAFT、非终态 VOID）；作废释放行占用；CONFIRMED 不可作废；代客确认字段落库；打印二维码基址降级；条件更新防并发
