# 11: 回收站前向 + 脱敏 + 消息

**What to build:** RecycleBinHandler ×3 注册（bizType=STATEMENT / PAYMENT / INVOICE_APPLICATION），仅 DRAFT / PENDING 状态进站、级联恢复不补账；已过账/已发出单据走作废/红冲不进回收站。脱敏：对账单/收款/账龄/发票金额字段 @Sensitive(AMOUNT)、客户联系人 @Sensitive(CONTACT)，打印 VO 豁免脱敏。消息：对账单 REJECTED 时通知制单财务（LunaNotifyHelper，失败不阻塞事务）。

**Blocked by:** 05 收款单 — 红冲 + 预收转核销 + 并发核销专测、07 客户对账单 — 发送/确认/驳回/作废 + 打印、10 发票申请 — 创建/提交/登记/驳回

**Status:** ready-for-agent

- [ ] `RecycleBinHandler` ×3 注册：STATEMENT（仅 DRAFT）、PAYMENT（仅 approval_status=PENDING）、INVOICE_APPLICATION（仅 DRAFT）
- [ ] 恢复不补账：回收站恢复仅还原单据+行状态，不补任何分录/余额变更
- [ ] 删除进回收站仅限上述草稿态；COMPLETED/CONFIRMED/ISSUED 等走作废/红冲不进回收站（ADR-0038 边界）
- [ ] 脱敏：对账单/收款单/账龄/发票金额字段加 @Sensitive(AMOUNT)；客户联系人字段 @Sensitive(CONTACT)；打印 print-data VO 豁免脱敏
- [ ] 消息：对账单 SENT→REJECTED 时通过 LunaNotifyHelper 通知制单财务，发送失败不阻塞事务
- [ ] 测试：三类 handler 仅草稿进站、非草稿态删除拒绝；级联恢复（主表+行表）、恢复不补账；脱敏字段生效+打印 VO 豁免；REJECTED 消息发送（失败不阻塞）
