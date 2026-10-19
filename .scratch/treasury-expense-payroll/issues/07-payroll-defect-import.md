# 07: 次品扣款导入（质检取数 + 指纹防重 + 逐卷快照）

**What to build:** 人事在工资单上点【导入次品扣款】：按工资月拉取质检确认记录（confirmed_at ∈ [月首,次月首) + CONFIRMED + OPERATOR，REVOKED 天然排除）按员工聚合卷数，弹窗填统一扣款单价后生成次品扣款行，每卷一条快照可展开追溯（确认单号/卷号/等级/确认日）；已被其他有效工资单引用的确认记录不再导入（指明占用单号），作废/删草稿后指纹释放；只读质检库，production 域零改动，不回写质检表、不引入 payrollSyncedAt。

**Blocked by:** 06

**Status:** closed

- [x] `GET /payroll/defect-preview?period=`：PayrollDefectService 只读 `lunaerp_production_inspection_confirmation`——status=CONFIRMED AND responsibility=OPERATOR AND deleted=0 AND confirmed_at ∈ [period-01, 次月-01)，按 worker_user_id 聚合卷数+逐卷快照；口径常量化（同 inspection-confirmation 07 票 payroll-summary 口径）；不落库
- [x] 导入生成：每员工一条 DEDUCTION/DEFECT 款项行 + 每卷一条 defect_ref（user_id/nickname/confirmation_id/confirmation_no/roll_no/grade/confirmed_at/deduct_price 全部快照，不可手填）；弹窗统一扣款单价必填（仅本次使用不存配置，025_010）；行金额=单价×卷数、行金额可改、行可删；行与 refs 随工资单整体保存
- [x] 指纹防重：排除已被有效工资单（非 CANCELLED、deleted=0）defect_ref 引用的 confirmation_id；保存时服务端重查候选，并发被抢报 025_005（指明占用工资单号）；应用守卫+普通索引（同 11 号指纹模式）
- [x] 释放语义：工资单作废（cancel）或草稿删除后指纹释放可重新导入；DEFECT 行只能整行删除（连 refs），删后立即释放；不允许手工添加 ref
- [x] REVOKED 链重验：草稿保存/提交时重验每个 ref 对应确认记录仍 CONFIRMED，失效（被撤销换链）报 025_005 要求重新导入；APPROVED 后不受事后撤销影响（快照即凭证，争议款下月其他扣款项调整）
- [x] 权限码 payroll:import-defect；导入 @LogRecord
- [x] Mockito 单测：月边界聚合（含次月首日排除）/仅 CONFIRMED+OPERATOR（EQUIPMENT/WAIVED/REVOKED/跨月排除）/有效单引用排除 025_005 指明单号/CANCELLED 释放/删草稿释放/单价必填 025_010/单价×卷数与改额/删行释放/保存并发重查/REVOKED 草稿提交重验 025_005 与 APPROVED 不受影响
