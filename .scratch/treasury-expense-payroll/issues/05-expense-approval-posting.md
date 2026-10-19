# 05: 报销端到端后端（主从 + 附件 + 审批一步过账 + 套打数据）

**What to build:** 员工可新建报销单（报销人缺省自己可代他人填报、付款账户必填、多行费用+附件 ≤10、总额服务端重算、草稿可存），提交走 02 号审批链（biz_type=expense）；审批通过即同事务经统一过账服务在指定账户扣款并写 CREDIT/OUT_EXPENSE 分录（外币按通过日 SPOT 快照），余额不足整个审批回滚；APPROVED 为终态不可改删作废，可取 A4 套打数据。

**Blocked by:** 02

**Status:** closed

- [x] `POST /expense/create`、`PUT /expense/update`、`DELETE /expense/delete?id=`（仅 PENDING/REJECTED，进回收站 EXPENSE，恢复不补账）；expense_no=`BX`+YYYYMMDD+3 位唯一含软删行；applicant_user_id 可代填（AdminUserApi 校验+昵称快照，无效 024_006）；attachments jsonb 有序数组 ≤10（024_005），删除仅移除数组元素；account_id 必填（改账户联动币种）
- [x] 行守卫：至少 1 行 024_002、original_amount>0 024_004、occur_date 不晚于提交日 024_007、category 字典 2284 值集校验、remark 必填；total_amount 服务端=Σ行重算（不信任前端）；账户为 RMB 时 total=original_total
- [x] `POST /expense/submit?id=`：账户 ENABLE（024_003）+ 行/附件/日期守卫全过后建审批实例 biz_type=expense（无启用流程 030_030_001）；`POST /expense/cancel-approve?id=`：APPROVING→PENDING（发起人）；REJECTED 可改再提；审批中可撤回
- [x] ExpenseApprovalHandler：通过即同事务统一通道过账 CREDIT/OUT_EXPENSE（一次全额、party=EMPLOYEE 报销人+party_name 昵称快照、biz_date=通过日、外币按通过日 SPOT 快照、ref_no=expense_no）→ APPROVED+approve_time；余额不足回滚整个审批；REJECTED/CANCELLED 回置走引擎标准流程；消息复用 04 号 LUNA_APPROVAL_* 三模板
- [x] 终态守卫：APPROVED 不可改/删/作废 024_001（红冲二期）
- [x] `GET /expense/page`（报销人/审批状态/期间/费用类型筛选）、`GET /expense/get?id=`（items+attachments+审批进度摘要）、`GET /expense/print-data?id=`（仅 APPROVED，04 号契约，豁免金额脱敏：报销人+账户+行表+附件名清单+合计+签章区）
- [x] 权限码 expense: query/create/update/delete/submit/print；提交过账 @LogRecord；金额 @Sensitive(AMOUNT)（打印 VO 豁免）
- [x] Mockito 单测：行/附件/账户守卫（024_002/005/003）/金额非法 024_004/代填报人无效 024_006/发生日期 024_007/提交建实例（biz_type=expense）/Handler 通过同事务过账（外币取通过日汇率、失败回滚审批）/APPROVED 终态 024_001/REJECTED 改再提/撤回回 PENDING/print-data 仅 APPROVED/租户隔离与横向越权
