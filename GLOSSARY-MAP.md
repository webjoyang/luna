# 上下文地图（GLOSSARY MAP）

本仓库采用**多上下文（multi-context）**领域文档布局。本文件是领域文档的唯一入口与路由：先读共享约定，再按工作主题读对应业务域的 GLOSSARY.md。各文件只收录本上下文的领域概念，术语定义以所属域文件为权威。

## 阅读顺序

1. **[docs/contexts/shared/GLOSSARY.md](docs/contexts/shared/GLOSSARY.md)** —— 跨域共享约定：多租户、软删除、前后端模块结构（含临时 UI 区 lunaerp-elmui）、错误码段位分配总表。**任何业务域工作前先读**。
2. 按主题选读一个或多个业务域文件：

| 上下文 | 覆盖范围 | 文档 |
|---|---|---|
| **baseinfo** 基本信息 | 客户与联系人、供应商与联系人、产品（含胚布品种、产品标签） | [docs/contexts/baseinfo/GLOSSARY.md](docs/contexts/baseinfo/GLOSSARY.md) |
| **business** 业务管理 | 销售合同、发货管理、出口单证、成本卡与报价、销售退货 | [docs/contexts/business/GLOSSARY.md](docs/contexts/business/GLOSSARY.md) |
| **production** 生产管理 | 胚布单（含发染记录）、染色单、纱线材质/规格/配比BOM/采购、检验单、质检确认、织造执行（织造单/机台任务/布票/织机/领纱/IoT）、进度反馈、生产进度聚合视图 | [docs/contexts/production/GLOSSARY.md](docs/contexts/production/GLOSSARY.md) |
| **stock** 库存管理 | 入库单（含明细/库位分配）、仓库、库位、库存台账/流水、库存余额 | [docs/contexts/stock/GLOSSARY.md](docs/contexts/stock/GLOSSARY.md) |
| **sample** 样品管理 | 样品分析（来样分析记录/针织工艺反推/成本报价/月结收费） | [docs/contexts/sample/GLOSSARY.md](docs/contexts/sample/GLOSSARY.md) |
| **finance** 财务 | 经营账：资金账户/经营分录账、客户对账、供应商对账、收付款与核销、报销、工资、发票申请、汇率主档（不做借贷会计；ADR-0037） | [docs/contexts/finance/GLOSSARY.md](docs/contexts/finance/GLOSSARY.md) |

## ADR（架构决策记录）

- 所有 ADR 统一存放在 [docs/adr/](docs/adr/)，全局连续编号（ADR-xxxx），被各域 GLOSSARY.md 与 `.scratch/` 下的 spec 跨域引用。
- 本仓库**不设域级 adr 子目录**：新增决策一律进 `docs/adr/` 取全局编号，避免迁移导致引用断链。

## 主要跨域关系（便于判断该读哪几个域）

- **产品（baseinfo）**是销售、生产、库存各域引用的核心主数据；纱线规格/配比 BOM 虽被产品成本结构引用，权威源在 production。
- **生产进度（production）**是只读聚合视图：数据来自 production 各执行单据、stock 库存流水、business 发货，零回写任何单据。
- **库存流水（stock）**是全域过账的共同落点：发货（business）写 `SHIPMENT_OUT*`；领纱发纱、发染（production）写 `YARN_OUT*` / `PROCESS_OUT*`；入库（stock）写 `STOCK_IN*`。
- **供应商（baseinfo）**被各域作为外部承制方引用：承织坯布厂、承染方、承检方均存供应商 ID + 名称快照。
- **样品分析（sample）**是只读消费方：ID + 名称快照引用客户、供应商（baseinfo）与纱线规格（production），零回写、零联动；样品分析自身是无状态机、无下游单据的分析记录（ADR-0036）。
- **经营分录账（finance）**是资金侧唯一 append-only 账本：收付款/报销/工资发放/退货冲减等一切资金与往来动作只追加分录、红冲冲销、永不重建；客户/供应商对账（finance）分别取数于发货（business）与入库（stock），只快照不回写业务单据（ADR-0037）。
- 字典 ID、错误码段位按域分配：明细见各域文件，全局段位总表见 shared。
