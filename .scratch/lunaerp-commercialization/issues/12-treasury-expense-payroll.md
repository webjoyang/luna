# 12-treasury-expense-payroll: 资金账户/收支总览/报销/工资初稿

Type: grilling
Status: resolved
Blocked by: 01, 02, 04, 17

## Question

设计资金与费用域并产出 spec（W3），grilling 时先调 Skill "grilling" 与 "domain-modeling"，并先读 17 号研究结论：

1. 资金账户：多账户模型（现金/银行/微信/支付宝）、期初与流水（收付款/报销/工资的入账统一写流水，对照 ADR-0017 流水不存余额的模式）；
2. 收支总览：按账户/期间/类别的汇总页、利润视角（收入−加工采购成本）的口径界定；
3. 报销申请：费用类型字典、附件、走 02 号审批链（Q14-d）、审批通过入资金流水；
4. 工资初稿（Q14-b）：质检次品扣款联动（现有 inspection-confirmation 已有 payroll summary 伏笔，先读 `.scratch/inspection-confirmation-mvp/issues/07-print-and-payroll-summary.md`）、手工录入计件/工资项、工资单审批、发放登记入资金流水；不含考勤算薪（写进非目标）；
5. 汇率主档：外币折算用汇率主档落本 spec 还是 10 号？需决定并互写引用。

产出：`.scratch/treasury-expense-payroll/spec.md`（按统一 DoD）。

## Answer

两轮 grilling 19 问（Q1–Q19）全部定稿，spec 落 `.scratch/treasury-expense-payroll/spec.md`（ready-for-agent），新建 **ADR-0043**（单一经营分录账 + current_balance 受控物化余额）。要点：

- **一种分录账**：10 号定稿的 `lunaerp_finance_ledger_entry` 一表承载资金流水（account_id 非空）与往来分录（可空）；只追加不删，红冲二期追加反向行；**唯一资金写入口 `FinancePostingService.post()`**（12 号落地，10/11/13 收口：条件更新余额+INSERT 同事务，余额不足统一 023_004、停用 020_101）。
- **余额口径冲突仲裁 Q18=A**：接受并行定稿的 10 号 current_balance 条件更新（放弃 grilling 初选的纯聚合）；无手工改余额端点、无重建；期初是账户主数据，本 spec 页面级放宽为"无分录可改"。
- **汇率主档冲突仲裁 Q19=A**：表/接口/菜单 10780/错误码 200–202 全归 10 号，12 号纯消费；SPOT 最近生效取价并快照，MONTH_END/结汇/汇兑损益/跨币种转账二期。
- **9 张新表 + 2 处 ALTER**：账户扩 account_no/account_nature(公私户)/remark，分录扩 party_name/ref_no；转账单 ZZ、其他收支单 QT（登记即终态不审批）、报销主从（BX，附件 jsonb ≤10）、工资五表（GZ 单/员工行/统一款项行/次品引用/发放批次 GF）。
- **报销一步、工资两步**：报销审批通过同事务过账（付款账户必填一次全额）；工资 Handler 空，出纳发放登记才写 OUT_SALARY，可多次跨账户，已发清派生不落字段；APPROVED 零批次可作废并释放次品指纹。
- **次品扣款逐卷可追溯**：区间版 payroll-summary 口径（confirmed_at 月内+CONFIRMED+OPERATOR），统一单价不存配置，引用指纹防重、作废释放；只读质检表不回写、不引入 payrollSyncedAt；事后 REVOKED 草稿重验/APPROVED 不受影响。
- **收支总览（新菜单 10715）**：资金视角 + 经营毛利参考管理卡片（收入=AR_INC 净额直接查分录账、成本=11 号 confirmed 对账单，永久标注非法定报表，不复制发货白名单）。
- 号段：错误码 020_105–110 续占 + 024/025 独占；字典 **2282–2287**（避让 10 号 2272–2278、13 号 2279–2281）；菜单 10715 + 10750/10760 按钮，10780 sort 校正 9；SQL 3 个（10 号公共表 SQL 先行）。
- 非目标：红冲、结汇/汇兑损益、考勤算薪/社保个税/工资结构/员工账户/工资条、借支备用金/OCR 风控、Excel 批量导工资、银行余额调节表。
- 已回填 finance GLOSSARY（12 个新术语+错误码+菜单）；与 10/11/13 号 12 项共享件收口清单随 spec 合并；已在 10/13 号票据留评论。
