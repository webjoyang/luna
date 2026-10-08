# 03: 提交审批 + 撤回 + 作废(DRAFT)

**What to build:** 销售内勤对草稿点「提交审批」，系统在事务内对父发货明细行加行锁重算可退量后插入占量，退货单进入 APPROVING 占量态并推送审批中心；审批中可撤回（释放占量回到 PENDING），驳回后回到 REJECTED 可改再提；草稿态可直接作废置 CANCELLED。APPROVING 态后端拒绝编辑。

**Blocked by:** 02 (制单取数 + 草稿 CRUD + 防超退聚合)

**Status:** ready-for-agent

- [ ] `POST /submit?id=`：守卫 status=DRAFT、至少一行、每行至少一卷、卷号必填、重量/米数至少一项、行级原因必填；行锁（SELECT ... FOR UPDATE 发货明细行）内重算双维度防超退（005/006）后条件更新 approval_status=PENDING/REJECTED→APPROVING
- [ ] `POST /withdraw?id=`：仅 approval_status=APPROVING 可撤回，置 PENDING 并释放占量
- [ ] `POST /cancel?id=`（DRAFT 分支）：status=DRAFT（approval_status=PENDING/REJECTED）直接置 CANCELLED，无入库单无需库存动作；body 含 cancelReason
- [ ] 状态机守卫：APPROVING 期间后端拒绝 create/update/delete；CANCELLED 终态不可再动
- [ ] 占量释放：驳回（引擎回调）/撤回/作废时 approval_status 离开 APPROVING/APPROVED 即释放占量（AvailabilityService 谓词自动排除）
- [ ] 提交/撤回/作废均加 `@LogRecord` 操作日志
- [ ] 前端：列表操作按钮按状态显隐——草稿[编辑][提交][删除]、审批中[撤回]、已驳回[编辑][提交]、已作废[详情]；提交/撤回/作废交互与确认弹窗
- [ ] 并发：模拟两单同时提交同一发货明细行，第二单行锁等待后重算超退失败（005/006），不出现超退占量
