# ADR-0043：全域唯一经营分录账——一张分录表 + current_balance 受控物化余额

**状态**：Accepted
**日期**：2026-10-07
**关联**：ADR-0037（财务域/经营账定位）、ADR-0017（库存流水不存余额的模式参照）；wayfinder 10 号 spec（域公共三表定稿）、12 号 spec（统一过账通道）、11 号 spec（付款 seam 契约）；17 号《老系统财务深挖》总纲①

## 背景

ADR-0037 锁定了 finance 域"只做经营账、全域只有一种 append-only 分录账、永不删库重建"，但物理形态在 10/12 两个并行 spec 中分别回答、且一度给出不同答案：

1. **资金流水与往来分录是否同一张表？** 老系统（linaagi）把账务拆成 Transaction / FinancialEntry / LedgerEntry 三本账，各记一遍再靠 `recalculateStanding` 删库全量重建对齐——sourceId 漂移曾导致全额重复收款，是财务域绝大多数 P0 的根因（17 号 a/b 节、总纲①）。
2. **账户余额存不存？** ADR-0017 在库存侧确立了"流水只增、余额实时聚合"；10 号 spec 选择账户表带 `current_balance` 冗余列 + 过账条件更新（防透支、防停用）；12 号 grilling 一度选择纯聚合。两份 spec 均 ready-for-agent 后冲突被显式仲裁（12 号 Q18=A）：**采用 10 号定稿的 current_balance 口径**，本 ADR 把两者一起钉死。

## 决策

### 1. 一张分录表，账户可空

`lunaerp_finance_ledger_entry` 是 finance 域**唯一**账本，同时承载两类分录（物理表 DDL 归 10 号 spec）：

- `account_id` **非空** = 资金流水：真金白银动了某个资金账户（IN_CUSTOMER/OUT_SUPPLIER/OUT_EXPENSE/OUT_SALARY/TRANSFER_IN/TRANSFER_OUT/IN_OTHER/OUT_OTHER）。资金流水页是它按账户过滤的查询视图。
- `account_id` **为空** = 往来分录：不动钱，只记往来（AR_INC/AR_INC_REVERSAL/AR_RETURN_REVERSAL 等）。

两类记录共用同一组维度：biz_date、direction（DEBIT/CREDIT）、category（字典 2274，值集可扩展不改表）、party_type/party_id/party_name、currency/original_amount/exchange_rate/base_amount 三金额、ref_type/ref_id/ref_item_id/ref_no 业务回指、reversal_of_id 红冲预留。

### 2. 只追加：只接受 INSERT，不提供 update/delete 代码路径

- 分录行 `deleted` 恒 0；冲销以"追加反向分录"实现（reversal_of_id 已预留列，红冲二期建设）。
- 唯一资金写入口是 `FinancePostingService.post(...)`（归 12 号 spec 落地）：校验账户 → 条件更新余额 → INSERT 分录，同事务完成。10 号收款 handler、11 号付款登记、12 号报销/工资/转账/其他收支、13 号均收口于此；任何模块不得自行 UPDATE 余额或 INSERT 分录。

### 3. 余额 = 账户上的受控物化缓存，不是第二份真相

- `lunaerp_finance_account` 存 `opening_balance`（期初主数据）与 `current_balance`（物化的运行余额，建户时=期初）。
- `current_balance` **只允许**在过账事务内随分录一起变更，方式是单条条件 UPDATE：
  - 流入：`SET current_balance = current_balance + ? WHERE id=? AND status='ENABLE'`
  - 流出：`SET current_balance = current_balance - ? WHERE id=? AND status='ENABLE' AND current_balance >= ?`
  - 影响行数 0 即停用（`1-030-020-101`）或透支（统一 `1-030-023-004`）。
- **没有任何手工改余额的端点/后台**；不做定时重建、不做删库重算。current_balance 与分录同源同事务写入，结构性地不可能漂移——老系统的漂移来自三本账+全量重建，本设计两者都不存在。
- 期初是账户属性（发生在账本之前的建账事实），不是一条 OPENING 分录；仅在该账户无任何业务分录时可修改（`1-030-020-104`）。
- 未来若要审计校验，可加只读对账作业（Σ 分录+期初 vs current_balance），属于增强而非修复机制。

### 4. 账户单币种，汇率在单据与分录上快照

账户带 currency 且建户后不可改；分录币种=账户币种；汇率取汇率主档（SPOT，业务日最近生效，表/菜单归 10 号）后随业务单据与分录快照；RMB 恒为 1。加权账面汇率、结汇、汇兑损益、期末重估（MONTH_END）均为二期。

## 理由

1. **一表而非两表/三表**：资金流水与往来分录维度形状相同，拆表必然引入双写对齐——老系统三本账 P0 不是实现粗心，而是同一件事存三份的结构性后果。一个可空 account_id 列消掉整张冗余表，"这笔分录动没动钱"成为可查询字段。
2. **物化余额而非纯聚合**：ADR-0017 的库存纯聚合适用于"高频写入、余额只在少数页面读"的库存；资金账户每租户仅十几个，却在**每次付款/报销/工资提交时都要原子判余额**——条件 UPDATE 把"判定+扣减"变成一条原子语句，天然防并发透支，语义比"SELECT FOR UPDATE 后 SUM 全部分录"更简单直接；账户流水页/总览也省掉每账户 SUM 全历史。
3. **物化不等于双真相**：真正制造 P0 的是"余额可被独立维护 + 靠重建与流水对账"。本设计余额无独立写入路径（无端点、无导入、无重建），它与分录是同一事务的两个投影，而非两份各自演化的事实。放弃纯聚合并未重新引入老系统失败模式。
4. **期初列而非 OPENING 分录**：append-only 与上线前调整期初相互冲突；期初落账户主数据 + "无分录可改"守卫，语义干净，且不影响"运行余额不手改"。
5. **写入口收窄为单服务**：老系统口径散落（发货白名单 8 处）与三本账互为表里。统一通道让余额校验、汇率折算、停用拦截、红冲语义只存在一处。

## 考虑过的备选

- **资金流水、往来分录各建一表**：转账与收款核销等场景必然双写，对齐问题复现。否决。
- **不存余额，行锁 + SUM 聚合**（12 号 grilling 初版选择）：最贴合 ADR-0017 字面，但每次过账锁行聚合全历史、判余额逻辑分散；10/11 号 spec、错误码与测试已按条件更新定稿，返工爆炸半径大且无额外正确性收益（漂移风险已被单通道消除）。经冲突仲裁否决。
- **期初造成 OPENING 分录**：需为上线前调整开删除口子，腐蚀只追加约束。否决。
- **余额列 + 手工调整/定时重建**：老系统形态，漂移温床。明确否决：无手工端点、无重建。

## 后果

- 10/11/13 号的资金动作统一注入 `FinancePostingService`；新增类别只扩 2274 字典值，不改表。
- 资金流水页、收支总览、金蝶/用友未来出口（ADR-0037 决策 4）都从同一张分录表取数；账户余额展示直接读 current_balance。
- 二期红冲只增加反向分录写入路径：余额随反向行经同一条件更新回加/回减，表结构与余额口径不变。
- 若未来需要会计期间冻结，在过账服务加"期间冻结"守卫即可，仍不需要第二张账表或余额重建。
