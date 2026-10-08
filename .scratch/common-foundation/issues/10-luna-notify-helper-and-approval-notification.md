# 10: 站内消息 LunaNotifyHelper + 审批触达

**What to build:** 审批人单据到达其节点时铃铛立即出现未读，点开直达审批工作台；发起人审批通过/驳回后收到消息，驳回消息里能看到驳回意见。零新表全复用芋道 notify（`system_notify_template` / `system_notify_message`，该表已持久化 `template_params` JSON，点击跳转信息随参数携带，零 DDL），只包一层 `LunaNotifyHelper.send(userId, templateCode, params, bizJump)`；不新增第二个角标/红点（审批待办数只在审批中心页内查询）。点击跳转零 DDL：消息 `templateParams` 固定携带 `bizType / bizId / bizNo`，前端铃铛点击按前端维护的 bizType→路由表跳转（审批类 → `/approval/todo` 带实例 id）。W0 实装 3 个模板 `LUNA_APPROVAL_TODO` / `LUNA_APPROVAL_APPROVED` / `LUNA_APPROVAL_REJECTED`，由 SQL 种子写入可重复执行（ON CONFLICT 更新）。与审批引擎（02 号 spec，feature 20）接线：引擎在创建 task / 实例终态处调 `LunaNotifyHelper`（同事务 BEFORE_COMMIT 语义，消息发送失败仅记 error 日志、**不回滚业务事务**——通知是旁路失败可容忍）；会签每人各一条，或签有人处理后其余待办自动关闭，已发消息不撤回。

**Blocked by:** 01, external: approval-engine（feature 20，审批任务创建/实例终态 ticket）

**Status:** ready-for-agent

- [ ] `LunaNotifyHelper.send(userId, templateCode, params, bizJump)` 封装落在 `common/notify` 子包，内部调 `NotifyMessageSendApi#sendSingleMessageToAdmin`，不新增表、不新增第二个角标
- [ ] templateCode 枚举（含 W0 三个 + 冻结契约位：`LUNA_SALES_CONTRACT_ACTIVE` / `LUNA_SHIPMENT_SIGNED` / `LUNA_AR_PERIOD_WARNING`——一期不接线，模板文案保留待二期）
- [ ] 3 个 W0 模板 SQL 种子写入 `system_notify_template`（ON CONFLICT 更新，可重复执行）：`LUNA_APPROVAL_TODO`（节点各审批人）、`LUNA_APPROVAL_APPROVED`（发起人）、`LUNA_APPROVAL_REJECTED`（发起人，内容带驳回意见）
- [ ] 模板内容用芋道占位符（如 `您有新的审批待办：{bizName} 单号 {bizNo}，提交人 {submitterName}`）
- [ ] 消息 `templateParams` 固定携带 `bizType / bizId / bizNo`
- [ ] 前端铃铛点击按 bizType→路由表跳转（审批类 → `/approval/todo` 带实例 id），无路由约定仅展示详情
- [ ] 与审批引擎接线：创建 task / 实例终态处调 `LunaNotifyHelper`，BEFORE_COMMIT 语义，失败仅日志不回滚业务事务
- [ ] 会签节点每个审批人各发一条；或签有人处理后其余待办自动关闭，已发消息不撤回（铃铛自然沉淀为已读/忽略）
- [ ] 测试：审批到达消息送达、点击跳转审批工作台、通过/驳回消息（驳回含意见）、消息失败不阻塞审批事务
