# 01-finance-domain-adr: finance 域落地决策与 ADR

Type: task
Status: resolved
Blocked by: 18

## Question

新增 finance 业务域的落地决策（对应 charting 决策 Q4=C / Q11=A）：

1. luna-srv 包结构落点：`com.luna.lunasrv.module.lunaerp.**.finance.*`（与 baseinfo/business/production/stock/sample 平级），对照现有五域的目录分层定出 finance 的完整包骨架；
2. 错误码段位：查清现有各域错误码段位约定，为 finance 分配段位并写入约定文档；
3. 菜单归属：按 Q18 的 9+1 分组确定"财务"一级菜单骨架（子菜单位：资金账户/客户对账/供应商对账/收付款/报销/工资/发票申请）；
4. 建 `docs/contexts/finance/GLOSSARY.md` 骨架并回写 `GLOSSARY-MAP.md` 路由；
5. 产出一份 ADR（`docs/adr/` 全局连续编号）：记录"经营账独立成 finance 域、不做借贷会计、views/fms 不迁不挂菜单、预留金蝶/用友对接接口"的决策与理由。

验收：ADR 合并、GLOSSARY 骨架与路由更新、包结构/错误码段/菜单骨架三件事在 ADR 或约定文档中有明确记录。本 ticket 是 task 型：决策内容已由 charting 锁定，执行的是落文档与骨架，供 10/11/12 号 ticket 直接引用。

## Answer

五件交付物全部落地（2026-10-07，AFK task）：

1. **ADR-0037**：[docs/adr/0037-finance-operating-account-domain.md](../../../docs/adr/0037-finance-operating-account-domain.md)——记录四大决策（经营账独立成第六域、不做借贷会计、`views/fms` 不迁后端/不挂菜单/保留代码、金蝶/用友一期不实现只预留分录账出口与 `service.finance.external` 包位）及包结构/错误码/菜单三件骨架。
2. **包结构骨架**（ADR 决策 3）：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.finance.<子模块>` + `enums.finance.FinanceErrorCodeConstants`；子模块 account/ledger/customerrecon/supplierrecon/payment/expense/payroll/invoice/exchangerate（+external 占名）；表名 `lunaerp_finance_*`、URL `/lunaerp/finance/...`、权限 `lunaerp:finance:<子模块>:<动作>`；前端目标 `views/lunaerp/finance/`（elmui 为临时备选）。
3. **错误码段位**：查清 1-030-001~019 占用（012 库存台账预留），发现 **018 段发染记录与样品分析实际重号**（只记录避让、不修码）；finance 域预留连续块 **1-030-020~029**（020 域公共/021 客户对账/022 供应商对账/023 收付款核销/024 报销/025 工资/026 发票/027-029 预留），已写入 [shared/GLOSSARY.md](../../../docs/contexts/shared/GLOSSARY.md) 段位总表与历史避让记录。
4. **菜单骨架**（ADR 决策 6）：**10700 财务**一级目录（sort 107，`/finance`，ep:money）+ 10710–10770 七个子菜单（资金账户/客户对账/供应商对账/收付款/报销/工资/发票申请），含 path/icon/permission/component 命名；INSERT SQL 不提前插空菜单，随 W3 各模块建表脚本交付。
5. **术语骨架与路由**：新建 [docs/contexts/finance/GLOSSARY.md](../../../docs/contexts/finance/GLOSSARY.md)（域定位、模块清单表、核心术语骨架、非目标、相关 ADR；字段/状态机细节标注待 10/11/12 spec 回填）；回写 [GLOSSARY-MAP.md](../../../GLOSSARY-MAP.md)（finance 行 + 经营分录账跨域关系）、shared/GLOSSARY（六域/fms 冻结注记/总表）、docs/agents/domain.md、AGENTS.md。

验收对照：ADR 已建（docs/adr 全局连续编号 0037）；GLOSSARY 骨架 + 四处路由/清单更新完成；包结构、错误码段、菜单骨架均在 ADR-0037 与 shared 约定文档中有明确记录。10/11/12 号 ticket 的 `Blocked by: 01` 依赖已解除。
