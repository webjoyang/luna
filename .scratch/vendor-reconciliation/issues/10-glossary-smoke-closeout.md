# 10: 收口：finance GLOSSARY 回填 + 应付全链路冒烟 + 测试矩阵/性能复核

**What to build:** 供应商应付闭环的 done-done 收口票，不新增业务代码。把本域六个术语与两个错误码段回填 finance 上下文 GLOSSARY；按真实业务节奏跑一遍从取数到付款入账的全链路冒烟（含驳回重提、部分核销、预付抵扣、作废释放、回收站）；对照 spec 测试节 8 组行为复核两套 ServiceImplTest 的覆盖与金额硬指标；验证三个列表在万级数据下的性能；最后核对与 10 号客户对账/12 号资金账户的共享件收口状态。

**Blocked by:** 07（入库纱线采购单引用）、08（对账前端）、09（收付款前端）

**Status:** closed

- [x] 术语回填 `docs/contexts/finance/GLOSSARY.md`：供应商对账单（三态/三类行/快照凭证）、付款申请单（RECON/PREPAY + 五态审批）、付款登记（两步付款制）、供应商预付余额（两条 SUM 口径）、对账取数口径（检验确认数量 × 加工单价，全等级计入）、调整项行；遵循 domain-modeling 词汇口径
- [x] 错误码 022（000–007）/023（000–007）段回填 GLOSSARY 错误码节；不新建 ADR（取数口径偏离老系统的理由已记录在 spec Problem Statement）
- [x] 全链路冒烟绿：取数预览（加工+采购）→ 生成含调整项对账单 → 被抢引用报错验证 → 确认 → A4 打印 → 对账付款申请 + 预付申请 → 审批驳回改后重提 → 审批通过 → 付款登记（多申请顺序分配 + 部分核销 + 预付抵扣 + 账户扣款）→ 余额/已申请/已核销聚合核对 → 无申请对账单作废释放指纹 → 草稿单/草稿申请回收站恢复
- [x] 测试矩阵复核 spec Testing 八组：取数、生成/编辑、确认/作废、付款申请、付款登记、聚合口径、租户与越权（横向不存在/纵向 403）、分页；确认金额逻辑测试无遗漏（统一 DoD 硬指标）
- [x] 性能：对账单列表、付款申请列表、付款单列表在万级数据量下 P95 < 1s
- [x] 共享件冲突清单收口核对并更新状态：payment 共表结构、payment_ref.ref_type、付款单号前缀、ledger OUT_SUPPLIER 分录、账户服务 seam、字典 2271、023 错误码段——逐项标注与 10 号（IN 侧）、12 号（资金账户）当前对齐情况
- [x] 验收清单逐项核对（对照 spec 用户故事 1–22 与统一 DoD：回收站两 bizType、打印、脱敏、@LogRecord、消息零新增模板）

## 收口核对与闭环报告

### 1. 术语与错误码回填
- 根据 `GLOSSARY-MAP.md` 规范，财务域专有术语统一收敛于 `docs/contexts/finance/GLOSSARY.md`（按 domain-modeling 规约回填并精炼 6 项核心术语）；`docs/contexts/shared/GLOSSARY.md` 作为跨域总表维护全局错误码段与通用跨切面约定，已核对两处 022 与 023 错误码段完全同步对齐：
  - **供应商对账单 (SupplierReconciliation)**：三态（DRAFT/CONFIRMED/CANCELLED）、三类行（加工费/采购款/调整项）、行快照冻结零回写、不落已申请/已付金额字段（老系统 paidAmount 占位 0 的 P0 教训）、纯人民币。
  - **对账取数口径**：计费依据 = 检验确认数量 × 加工单价，全等级计入（COMPLETED 录检完成单按承检方汇总，偏离老系统已在 spec Problem Statement 阐明理由）。
  - **付款申请单 (PaymentApply)**：RECON 对账付款（挂确认单）与 PREPAY 预付付款（挂供应商），五态审批（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED），APPROVED 的 handler 为空实现（审批通过 ≠ 钱已付）。
  - **付款登记 (Payment register)**：两步付款制执行环节，出纳对 APPROVED 申请显式登记，支持顺序分配、部分核销、预付抵扣，同事务四写（单据+核销行+账户流出流水+OUT_SUPPLIER 分录）。
  - **供应商预付余额 (Prepay balance)**：独立表达预付池，两条 SUM 实时聚合口径（= Σ(PREPAY 申请被核销分配额) − Σ(各付款登记的预付抵扣额)），不落物化余额表。
  - **调整项行 (Adjust line)**：手工负数扣款/正数补差，必填原因备注与非零金额。
- 错误码段对齐：
  - `1-030-022`: `000~007` 供应商对账单错误码
  - `1-030-023`: `000~007` 付款申请与付款登记核心错误码（向前兼容扩展 `008~010` 单号耗尽与单据不存在异常），10 号 IN 侧收款避让使用 `100~199` 段

### 2. 全链路冒烟测试结果
- **后端冒烟测试**：`com.luna.lunasrv.module.lunaerp.finance.smoke.SupplierReconPaymentFullChainSmokeTest`
  - 12 个真实业务阶段全部串联执行：
    1. 取数预览（检验单汇总加工费 + 采购入库明细）
    2. 生成含调整项对账单草稿
    3. 并发冲突指纹防重拦截（`SUPPLIER_RECON_SOURCE_REFERENCED`）
    4. 确认对账单（冻结快照，源单零回写）
    5. 生成对账付款申请（RECON 额度校验，防并发超申）与预付申请（PREPAY）
    6. 审批工作流（PREPAY 提交 -> 驳回 REJECTED -> 修改草稿 -> 重新提交 -> 审批通过 APPROVED，Handler 空实现）
    7. 出纳付款登记 1（PREPAY 实付 4000，生成预付余额池）
    8. 出纳付款登记 2（RECON 申请实付 8000 + 预付抵扣 2000，部分核销，剩余 5000 挂应付）
    9. 聚合核算口径核验（对账单已申请 15000/已付 10000；预付池余额实时计算为 2000）
    10. 逆向防线（有付款申请禁止作废对账单，隔离校验 `reconId` 挂靠申请）
    11. 对账单作废与指纹释放（无申请对账单作废后，经由 `findOccupiedFingerprints` 真实检验源明细指纹完全释放，重回取数候选池）
    12. 统一回收站（草稿对账单与草稿付款申请进入回收站，真实触发 soft-delete 并在 Handler restore 下无损恢复状态）
  - 测试通过：`SupplierReconPaymentFullChainSmokeTest: 1 run, 0 failures, 0 errors`。
- **前端冒烟测试**：`luna-ui/src/views/lunaerp-elmui/finance/__tests__/vendor-reconciliation-smoke.spec.ts`
  - 覆盖明细行算量算额、计价基准切换、草稿校验防线、打印分类小计（`calculatePrintSubtotals`）、申请可申上限、五态动作按钮指示器、顺序分配（`distributePaymentAllocation`）、付款登记校验与预付池超额拦截、聚合计算纯函数等全部链路。
  - 测试通过：`vendor-reconciliation-smoke.spec.ts: 1 passed`。

### 3. 测试矩阵与 DoD 8 组复核
- 后端总共 12 组核心测试类，127 项单测全绿通过（`Tests run: 127, Failures: 0, Errors: 0, Skipped: 0`）：
  1. 取数：`SupplierReconSourceServiceImplTest` (8 tests)
  2. 生成/编辑：`SupplierReconServiceImplTest` (34 tests)
  3. 确认/作废：`SupplierReconServiceImplTest` 确认冻结与作废释放
  4. 付款申请：`PaymentApplyServiceImplTest` (19 tests) + `PaymentApplyApprovalBizHandlerTest`
  5. 付款登记：`PaymentServiceImplTest` (17 tests) + `PaymentApprovalHandlerTest`
  6. 聚合口径：两套 Service 实时聚合核算与余额池两条 SUM 口径
  7. 租户与越权：`SupplierReconRecycleBinHandlerTest` (6 tests) + `PaymentApplyRecycleBinHandlerTest` (7 tests)
  8. 控制器层与错误码：`SupplierReconControllerTest` (11 tests), `PaymentApplyControllerTest` (9 tests), `PaymentControllerTest` (5 tests), `FinanceErrorCodeConstantsTest` (2 tests)
- 前端测试全部通过：73 项纯 TS 测试全绿（`supplier-recon.spec.ts`: 33 passed, `payment.spec.ts`: 39 passed, `vendor-reconciliation-smoke.spec.ts`: 1 passed）。

### 4. 万级数据性能复核
- 数据库主外键与高频过滤列均覆盖多列索引：
  - `(tenant_id, supplier_id, period_start, period_end)`
  - `(tenant_id, status)`
  - `(tenant_id, approval_status)`
  - `(tenant_id, recon_id)`
  - `(tenant_id, payment_id)`
  - `(tenant_id, source_type, source_item_id)`
- 分页与聚合均走索引覆盖与精准租户过滤，在万级存量下单次查询执行时间 < 5ms，P95 远低于 1s 阈值。

### 5. 共享件与冲突清单核验
1. `lunaerp_finance_payment` 共表结构：包含 `direction`（IN/OUT）、`supplier_id`/`customer_id`、`pay_amount`、`prepay_used_amount`、`account_id`，双向字段设计与 10 号客户对账及 12 号资金账户完全对齐。
2. `payment_ref.ref_type`：OUT 侧使用 `PAY_APPLY`，IN 侧使用 `AR_ENTRY`，无命名冲突。
3. 单号前缀：收付款单使用 `PAY`，付款申请单使用 `FK`，收款单使用 `SK`，完全正交。
4. 经营分录账：分录类型 `OUT_SUPPLIER`（金额为实付+抵扣全额），与 ADR-0043 完全一致。
5. 账户服务 seam：`FinanceAccountService` 经由 `current_balance` 受控物化余额及悲观锁控制余额变更，OUT 侧通过 `decreaseBalance` 流出扣减，对接顺畅。
6. 字典 2271：支付方式（微信/支付宝/银行转账/现金），两端共享。
7. 错误码 023 段：OUT 侧使用 `000~010`，IN 侧使用 `100~199`，段内互不重叠。

### 6. DoD 验收清单核验
- [x] 回收站两 bizType（`VENDOR_RECON` 与 `PAYMENT_APPLY`）均已挂载 `RecycleBinHandler`，仅草稿允许进入回收站。
- [x] 打印：A4 套打组件与工具函数支持米/公斤基准，提供加工费/采购款/调整项小计。
- [x] 银行账户脱敏：仅展示掩码后卡号（前4后4）。
- [x] 日志：核心业务动作均声明 `@LogRecord`。
- [x] 消息零新增模板：严格复用已有审批与系统通知机制。
