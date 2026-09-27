# 07: 按日批量打印签字单 + 工资扣款汇总（只读，双日期口径）

**What to build:** 领班点"批量打印"选择日期（缺省今天），按**发现日**（记录创建日）看到当天全部状态的确认记录（含已撤销标记），用浏览器打印出纸质签字单；人事点"工资扣款汇总"选择日期（缺省今天），按**确认日**（认定日）看到当天已认定、操作失误、未撤销的记录，按挡车工分组显示扣款卷数并可展开逐卷明细，供对照手工录入工资。两个弹窗均显著标注各自日期口径，汇总纯只读、不写任何工资数据、不做已同步标记。

**Blocked by:** 06 (责任认定 + 撤销留痕链)

**Status:** completed

- [x] `GET /print-view?date=`（:print 权限，date 缺省今天）：返回 create_time 落在 [当日00:00, 次日00:00) 的**全部状态**行（含 REVOKED，带 status 供前端标记），按创建时间正序；行字段含检验单号/卷号/缸号/等级/产品/机台/挡车工/责任/备注/认定人/认定时间
- [x] `GET /payroll-summary?date=`（:payroll 权限，date 缺省今天）：confirmed_at 当日范围 + status=CONFIRMED + responsibility=OPERATOR；按 worker_user_id 聚合为 groups[{workerUserId,workerName,count,records[]}]，records 含卷号/缸号/等级/检验单号/备注/来源/认定时间；REVOKED 行天然排除，无未分配桶（OPERATOR 挡车工必填）
- [x] 前端打印弹窗（:print）：日期选择+查询+只读表格（已撤销行状态标记）+口径标注"按发现日"+打印按钮（作用域打印样式，window.print）
- [x] 前端汇总弹窗（:payroll）：日期选择+查询+按挡车工分组表格（el-table 展开行显示卷明细）+口径标注"按确认日"；全程只读，无任何写操作按钮
- [x] 列表页顶部两个按钮分别受 :print / :payroll 权限控制
- [x] Service 单测增补：打印按 create_time 当日边界（跨日数据验证）且含全部状态、正序；汇总仅 CONFIRMED+OPERATOR 入组、EQUIPMENT/WAIVED/REVOKED/跨日确认均不入组、按挡车工分组 count 与明细正确
- [x] 明确不做：工资初稿生成、payrollSynced 标记、工资系统任何写入、Excel 导出

