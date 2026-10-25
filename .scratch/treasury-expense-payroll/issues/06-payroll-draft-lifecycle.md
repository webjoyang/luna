# 06: 工资单主从生命周期（员工行/款项行/金额重算 + 空 Handler 审批 + 作废 + 派生态）

**What to build:** 人事可按月新建工资单（period=YYYY-MM，同租户同期间仅一张有效单），手工增删员工行（UserSelect 全员可选、不按岗位过滤）并给每人录收入/扣款项行（计件支持数量×单价），应发/扣款/实发服务端重算、任一员工实发不可为负；提交走 02 号审批链但 Handler 为空实现（APPROVED 不发钱——写账动作是发放登记，对齐 11 号付款两步制先例）；APPROVED 且零发放可作废（原因必填）；列表直接看到派生发放态（未发放/部分发放/已发清，不落状态字段）。

**Blocked by:** 01（SQL 基建；审批引擎集成模式与 05 票相同但不依赖其落地，可并行）

**Status:** closed

- [x] `POST /payroll/create`：period 同租户同期间唯一（应用守卫+部分唯一索引兜底，并发双单 025_002）；payroll_no=`GZ`+YYYYMM+3 位月流水唯一含软删行；title 缺省"{period} 工资表"可改；PAID 不落库
- [x] `PUT /payroll/update`（仅 PENDING/REJECTED）：员工行/款项行整体替换保存；员工行 user_id 经 AdminUserApi 校验+昵称快照（无效 025_012）、同单不重复 025_003、≥1 行 025_009；款项行 item_kind（EARNING/DEDUCTION 后端枚举）/item_type 值集校验（2285/2286）、amount>0 025_004、qty×price 服务端算、手工行不含 DEFECT（次品行仅导入生成，07 票）
- [x] 金额服务端重算：earning_total/deduction_total/net_amount 单据级与员工行级快照；任一员工实发<0 报 025_011；工资单一期仅 RMB
- [x] `DELETE /payroll/delete?id=`：仅草稿（PENDING/REJECTED），进回收站 PAYROLL（恢复不补账；恢复随之恢复次品指纹占用——07 票语义）
- [x] `POST /payroll/submit?id=`：守卫全过后建审批实例 biz_type=payroll（无启用流程 030_030_001）；提交时重验 refs（025_005——refs 由 07 票引入，本票以空集天然通过）；PayrollApprovalHandler **空实现**（引擎置 APPROVED，不写账）；`POST /payroll/cancel-approve?id=`：APPROVING→PENDING
- [x] `PUT /payroll/cancel?id=`：仅 APPROVED 且发放批次=0（有发放 025_006）、原因必填、置 CANCELLED+cancelled_by/at/cancel_reason、释放次品指纹（07 票语义）、审批记录保留
- [x] `GET /payroll/page`（期间/审批状态/派生态筛选）：派生发放态=Σ 批次 original_amount vs net_amount——0 未发放/区间内 部分发放/=net 已发清，不落状态字段（批次表本票为空恒显示未发放，08 票落地后自然生效）；`GET /payroll/get?id=`（员工行+款项行+refs+批次+已发放聚合）；`GET /payroll/print-data?id=`（仅 APPROVED：员工一行 应发/扣款/实发+签收栏+合计，不做个人工资条）
- [x] 权限码 payroll: query/create/update/delete/submit/cancel/print；提交/作废 @LogRecord；金额 @Sensitive(AMOUNT)（打印 VO 豁免）
- [x] Mockito 单测：期间并发双单 025_002（部分唯一索引兜底）/员工重复 025_003/空单 025_009/负实发 025_011/金额服务端重算/计件 qty×price/APPROVED 无写账（Handler 空）/有发放禁作废 025_006（批次聚合 mock）/作废原因必填/APPROVED 零发放可作废且审批记录保留/print-data 仅 APPROVED/横向越权报不存在
