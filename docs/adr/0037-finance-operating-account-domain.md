# ADR-0037：财务域独立成域——只做经营账，不做借贷会计

**状态**：Accepted
**日期**：2026-10-07
**关联**：商业化决策 Q4=C / Q11=A / Q18（菜单 9+1）；wayfinder 地图 `lunaerp-commercialization` 01 号 ticket；17 号《老系统财务深挖》研究结论；ADR-0001（正式 UI 目录）、ADR-0017（通用库存台账/流水不存余额）、ADR-0034（只读聚合视图）；后续 10/11/12 号 spec 直接引用本 ADR

## 背景

商业化一期 W3 要补齐资金闭环：客户对账/开票/收款、供应商对账/付款、资金账户/报销/工资。立项前有三条已查清的事实：

1. **老系统（linaagi）财务的 P0 根因是三本账冗余**：Transaction（资金流水）、FinancialEntry（收支分录）、LedgerEntry（往来台账）各记一遍，再靠 `recalculateStanding` 删库全量重建对齐，sourceId 漂移曾导致全额重复收款。17 号研究给出的总纲①是：新栈只保留一种 **append-only 分录账，永不删除重建**。
2. **现仓库前端存在一套借贷会计脚手架** `luna-ui/src/views/fms/`（凭证 voucher、账簿 ledger、三大报表 report、期末结转 closing、账套/科目/币种 config、财务首页 home，纯前端、无 luna-srv 后端对应物）。它不在激活路由中——唯一路由引用残留在备份文件 `src/router/modules/remaining copy.ts.txt`（非 `.ts`，不被加载），当前不挂任何菜单。
3. **lunaerp 现有五个业务域**（baseinfo/business/production/stock/sample）的包分层、错误码段位（`1-030-001`~`019`）、菜单 ID 段（10000/10100/10200/10300/10600）均已成型，财务模块需要明确的落点约定供 10/11/12 号 spec 直接引用，避免各自取包名、抢错误码段、菜单号冲突。

charting 已锁定方向（Q4=C）：一期财务 = **经营账**（面向老板的钱/往来/费用视角），不做借贷会计；`views/fms` 不迁后端、不挂菜单、保留代码（Q11=A）；预留金蝶/用友对接接口。本 ADR 记录该决策及三件落地骨架（包结构、错误码段、菜单）。

## 决策

### 1. 财务独立为第六个业务域 finance，定位"经营账"

新增业务域 `finance`（中文名**财务**），与 baseinfo/business/production/stock/sample 平级，承载：资金账户与资金流水、唯一的 append-only 经营分录账、客户对账、供应商对账、收付款与核销、报销、工资、发票申请、汇率主档。

经营账的含义：记录"钱从哪来、到哪去、谁欠谁"，服务资金与往来管理；**不引入借贷记账法**。一期明确不做：会计凭证、会计科目、期初余额试算平衡、期末结转、资产负债表/利润表/现金流量表、账套。这些能力已由 `views/fms` 脚手架演示过形态，但其后端从未迁入，一期不补建；客户若有法定账务诉求，走金蝶/用友对接（决策 4），不自造。

金额一律 DECIMAL（或整数分），禁用浮点；分录账原生携带**原币金额 + 汇率 + 本位币金额**、`refType/refId` 业务单据回指、冲销/红冲标记（沿用 17 号研究 g/b 两节结论）。账户余额按分录实时聚合或条件更新，不做删除重建（参照 ADR-0017 流水不存余额的模式）。

### 2. `views/fms` 不迁后端、不挂菜单、保留代码

`luna-ui/src/views/fms/` 维持现状三不：

- **不迁后端**：不为其凭证/科目/账簿/报表建任何 luna-srv 表、接口或包；
- **不挂菜单**：不注册路由、不在 10700 财务目录（见决策 5）下给入口；`remaining copy.ts.txt` 备份文件保持不复活；
- **不改代码**：脚手架原样保留作为未来对接金蝶/用友时的字段与交互参照，允许编译期静态留存，不做功能维护。

finance 域所有新页面进入正式/临时业务目录（决策 3），与 fms 零代码共享、零路由互链。

### 3. 包结构骨架

后端落在 `luna-srv/lunasrv-module-lunaerp`，严格沿用现有五域"层 → 业务域 → 子模块"分层（注意两个现状：controller 下**无** `admin` 中间层；DAL Mapper 包名历史上叫 `dal.mysql`，实际数据库为 PostgreSQL，新域沿用不改）：

```
com.luna.lunasrv.module.lunaerp
├── controller.finance
│   ├── account/            资金账户、资金流水（域公共）
│   │   └── vo/
│   ├── ledger/             经营分录账（append-only，域公共，只读查询为主）
│   ├── customerrecon/      客户对账单与确认、账龄（10 号 spec）
│   ├── supplierrecon/      供应商对账（11 号 spec）
│   ├── payment/            收付款单与核销（收款/付款按 direction 分型，10/11 号 spec）
│   ├── expense/            报销申请（12 号 spec）
│   ├── payroll/            工资单与扣款明细（12 号 spec）
│   ├── invoice/            发票申请/登记（10 号 spec）
│   └── exchangerate/       汇率主档（归属 10 或 12 号 spec 决定，包位预留）
├── service.finance.<同上子模块>
├── dal.mysql.finance.<同上子模块>
├── dal.dataobject.finance.<同上子模块>
└── enums.finance
    └── FinanceErrorCodeConstants   财务域错误码（独立文件，不再塞入顶层 ErrorCodeConstants）
```

- 未来金蝶/用友对接适配器预留包位 `service.finance.external`（一期**不建代码**，仅占名，禁止他用）。
- 表名前缀 `lunaerp_finance_*`；建表 SQL 为 `luna-srv/sql/postgresql/lunaerp-finance-<子模块>.sql`，菜单/字典随各模块 SQL 幂等插入（沿用现有 lunaerp SQL 模式）。
- URL 前缀 `/lunaerp/finance/<子模块>/...`；权限标识 `lunaerp:finance:<子模块>:<动作>`。
- 前端视图目标为正式目录 `luna-ui/src/views/lunaerp/finance/<子模块>/`、API 为 `src/api/lunaerp/finance/<子模块>/index.ts`（ADR-0001 方向）；若 W3 交付速度需要走临时区，则镜像到 `views/lunaerp-elmui/finance/` 与 `api/lunaerp-elmui/finance/`，由具体 spec 选定——菜单 ID、path、权限标识不随目录选择变化。

### 4. 预留金蝶/用友对接接口，一期不实现

商业化一期私有化买断交付，不内置任何财务软件对接实现，但模型设计留出稳定对接面：

- **对接的唯一权威出口是经营分录账**：append-only、含原币/汇率/本位额、业务单据回指、冲销标记——外部系统需要的凭证数据从分录账导出/推送，不允许反向回写分录账；
- 对接形态未来为 `service.finance.external` 下按厂商建 adapter（金蝶/用友），职责限定为"分录 → 对方凭证格式"的映射导出与科目映射表维护，不把对方的科目/凭证概念泄漏进 finance 域模型；
- 一期不建接口类、不建配置表、不排期；任何 spec 不得提前实现对接逻辑。

### 5. 错误码段位：finance 域预留 1-030-020 ~ 1-030-029

lunaerp 段位总表在 `docs/contexts/shared/GLOSSARY.md`。现有 001~019 已分配（012 为库存台账预留），且发现 **018 段被发染记录与样品分析双方实际占用**（历史重号，见 shared 总表注记，待后续清理）。财务域取连续段位块 **020–029**，按子模块各占一段：

| 段位 | 模块 | 落地 ticket |
|---|---|---|
| `1-030-020` | 域公共：资金账户、资金流水、经营分录账、汇率主档 | 10（DDL 先行）/ 12（扩列） |
| `1-030-021` | 客户对账（对账单/确认/账龄） | 10 |
| `1-030-022` | 供应商对账 | 11 |
| `1-030-023` | 收付款与核销（段内按百位再分：000 收款、100 付款、200 核销/预收预付，仿 015 织造域先例） | 10 / 11 |
| `1-030-024` | 报销 | 12 |
| `1-030-025` | 工资（含质检扣款明细行） | 12 |
| `1-030-026` | 发票申请/登记 | 10 |
| `1-030-027` ~ `029` | finance 域预留（结汇、经营汇总、外部对接适配器等） | — |

枚举常量统一声明在 `enums/finance/FinanceErrorCodeConstants.java`，取段前先查 shared 总表避让。

### 6. 菜单骨架：10700「财务」一级目录（Q18 9+1 分组之一）

现有一级目录 10000 基本信息(sort 100)/10100 业务管理(101)/10200 生产管理(102)/10300 库存管理(103)/10600 样品管理(104)，生产域子菜单已溢出借用 104xx/105xx 菜单号。财务目录取 **10700**、sort **107**（105/106 sort 与 104xx/105xx 号段均不动，留给 Q18 的"销售/供应链"分组重组时插位；未来审批中心取 108xx/sort 108）。

骨架（type=1 目录 + type=2 子菜单，query 权限挂菜单本身；按钮 type=3 由各 spec 按 +1…+9 自行占位）：

| 菜单 ID | 名称 | type | sort | parent | path | icon | 权限 | component 名 |
|---|---|---|---|---|---|---|---|---|
| 10700 | 财务 | 1 | 107 | 0 | `/finance` | `ep:money` | — | — |
| 10710 | 资金账户 | 2 | 1 | 10700 | `account` | `ep:credit-card` | `lunaerp:finance:account:query` | `finance/account/index` |
| 10720 | 客户对账 | 2 | 2 | 10700 | `customer-reconciliation` | `ep:document-copy` | `lunaerp:finance:customerrecon:query` | `finance/customer-reconciliation/index` |
| 10730 | 供应商对账 | 2 | 3 | 10700 | `supplier-reconciliation` | `ep:files` | `lunaerp:finance:supplierrecon:query` | `finance/supplier-reconciliation/index` |
| 10740 | 收付款 | 2 | 4 | 10700 | `payment` | `ep:coin` | `lunaerp:finance:payment:query` | `finance/payment/index` |
| 10750 | 报销 | 2 | 5 | 10700 | `expense` | `ep:tickets` | `lunaerp:finance:expense:query` | `finance/expense/index` |
| 10760 | 工资 | 2 | 6 | 10700 | `payroll` | `ep:wallet` | `lunaerp:finance:payroll:query` | `finance/payroll/index` |
| 10770 | 发票申请 | 2 | 7 | 10700 | `invoice-application` | `ep:document` | `lunaerp:finance:invoice:query` | `finance/invoice-application/index` |

component 字符串按最终 UI 落点加 `lunaerp/` 或 `lunaerp-elmui/` 前缀，componentName 取 `LunaerpFinance<驼峰子模块>`。本 ticket 只锁定骨架，**INSERT SQL 随 W3 各模块建表脚本交付**，不提前插空菜单（租户套餐网关按菜单开通，空菜单会造成可开通不可用）。

## 后果

- 10/11/12 号 grilling/spec 直接在 finance 域骨架内展开，无需再决定包名、号段、菜单归属；术语表骨架见 `docs/contexts/finance/GLOSSARY.md`。
- 经营账与借贷会计的边界被显式钉死：凭证/科目/三大报表/期末结转任何时候都不进 finance 域，如客户提出，走外部对接或重新开图决策，不悄悄回流。
- `views/fms` 成为纯参照遗物：保留代码但无路由、无后端、无维护义务；新人误挂入口即违反本 ADR。
- 金蝶/用友一期没有可运行对接，只保证"分录账形状未来可对接"；私有化客户的法定账仍需外部财务软件完成。
- 018 段重号（发染记录/样品分析）是本次盘点的附带发现，本 ADR 只做记录与避让（财务从 020 起），**不在本 ticket 修码**；样品分析迁段另开 ticket 处理。

## 增补（2026-10-07，10 号 customer-reconciliation spec 定稿与收口）

- **菜单骨架补全**：决策 6 表追加 **10780 汇率**（type 2，sort 8，parent 10700，path `exchange-rate`，权限 `lunaerp:finance:exchangerate:query`，component `lunaerp-elmui/finance/exchange-rate/index`，按钮 10781/:create、10782/:update）——exchangerate"待定（配置位）"落定为正式二级菜单；10740 收付款页面定为三 Tab（收款｜付款申请｜付款登记），收款 Tab 与按钮 10745–10749 归 10 号，OUT 两 Tab 与按钮 10741–10743 归 11 号。
- **域公共表 DDL 归属**：决策 5 中 020 段的 `lunaerp_finance_ledger_entry`/`lunaerp_finance_account`/`lunaerp_finance_exchange_rate` 三表 DDL 与 `LedgerService`/`AccountService` 由 10 号 spec 先行落地（消除 11 号付款登记扣余额对"账户归 12 号"的先后依赖），11/12 号只消费与扩列（12 号扩外币双余额/账面加权汇率、MONTH_END 期末牌价）。
- **共享件收口清单 12 项无冲突核定**：
  1. `lunaerp_finance_payment` 主表：11 号先行建表，10 号 ADD COLUMN 扩 IN 侧 7 列（收口完成）；
  2. `lunaerp_finance_payment_ref`：ref_type 扩值 AR_ENTRY + IN 侧 4 扩列（收口完成）；
  3. 单号前缀：IN=RC-、OUT=FK-（收口完成）；
  4. 经营分录账：10 号落地 ledger_entry DDL 与 LedgerService，11 号 OUT_SUPPLIER 与 12 号消费同表；
  5. 资金账户：account DDL 与 AccountService 随 10 号落地，11 号/12 号消费同表与扩列；
  6. 字典 2271 direction：10 号复用 11 号；
  7. 审批状态字典：复用 02 号；
  8. 错误码 023 段：IN 侧取 100–199，复用共享 003/004/006；
  9. 10740 页面：三 Tab 页面结构，收款 Tab 归 10 号，OUT 两 Tab 归 11 号；
  10. ledger category 字典 2274：10 号建全集 9 值，11/12/13 号复用；
  11. 汇率主档：10 号落地，12 号扩 MONTH_END 期末牌价；
  12. 13 号退货冲减：`LedgerService.recordArReturnReversal` 契约冻结，退货审批通过时记账。
