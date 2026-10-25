# 10: 前端：报销页（luna-ui）

**What to build:** 员工在 luna-ui 完成报销全流程：工作台新建/编辑报销单（报销人远程选择可代填、账户下拉联动币种、多行费用、附件 ≤10），提交后详情页审批时间线（复用 02 号三视图与角标），列表按报销人/审批状态/期间/费用类型筛选，APPROVED 可 A4 套打（报销人+账户+行表+附件名清单+合计+签章区）。

**Blocked by:** 05

**Status:** closed

- [x] `views/lunaerp-elmui/finance/expense/`：列表（审批状态 DictTag、四维筛选）+工作台（报销人 UserSelect 远程搜索缺省本人可代填/付款账户下拉（停用过滤）联动币种展示/行表增删改（费用类型 2284、金额>0、发生日期、必填说明）/附件上传 ≤10 有序可移除、超限前端拦截）
- [x] 详情：主从信息+附件清单+审批进度摘要时间线；草稿/驳回可编辑再提、审批中可撤回（cancel-approve）；APPROVED 只读
- [x] 打印：APPROVED 调 print-data 走 04 号打印内核 A4 套打；打印视图金额豁免脱敏
- [x] API 镜像 `src/api/lunaerp-elmui/finance/expense/index.ts`；菜单 10750 挂接与按钮权限（10751–10755）显隐；金额千分位与 @Sensitive(AMOUNT) 脱敏
- [x] 简单冒烟：建单→提交→（审批通过）→列表 APPROVED→资金流水页可见 OUT_EXPENSE 分录（余额已扣）→套打预览正常
