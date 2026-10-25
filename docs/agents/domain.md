# 领域文档

工程类技能在探索代码库时，应按本文件约定消费本仓库的领域文档。

## 探索代码前，先读这些

本仓库采用**多上下文（multi-context）**布局：

1. **根目录 `GLOSSARY-MAP.md`**：领域文档的唯一入口。它路由到每个限界上下文各自的 `GLOSSARY.md`，并列出跨上下文关系。
2. **`docs/contexts/shared/GLOSSARY.md`**：跨上下文平台约定（多租户、软删除、前后端模块结构、错误码段位注册表）。无论工作主题是什么，先读它。
3. **与主题相关的业务域 `GLOSSARY.md`**，位于 `docs/contexts/`：
   - `baseinfo` —— 客户、供应商、产品主数据
   - `business` —— 销售合同、发货
   - `production` —— 胚布/染色/纱线/检验/质检确认/织造/进度
   - `stock` —— 入库、仓库/库位、库存台账/余额
   - `sample` —— 样品分析（来样分析、工艺反推、成本报价、月结收费）
   - `finance` —— 财务（经营账：资金账户/分录账、对账、收付款、报销、工资、发票；ADR-0037）
4. **`docs/adr/`**：阅读与你即将改动的区域相关的 ADR。

若以上文件不存在，**静默继续**。不要提示文件缺失，也不要建议预先创建。`/domain-modeling` 技能（经由 `/grill-with-docs`、`/improve-codebase-architecture` 调用）会在术语或决策真正落定时按需创建它们。

## 文件结构

本仓库包含两个顶层项目 —— `luna-srv`（Spring Boot 后端）与 `luna-ui`（Vue3 前端），每个限界上下文都横跨两者。因此上下文文档放在仓库级 `docs/contexts/` 目录，而不是任一项目内部：

```
/
├── GLOSSARY-MAP.md                        ← 上下文索引与路由
├── docs/
│   ├── adr/                               ← 全部 ADR，全局统一编号（ADR-xxxx）
│   └── contexts/
│       ├── shared/GLOSSARY.md             ← 跨上下文约定
│       ├── baseinfo/GLOSSARY.md
│       ├── business/GLOSSARY.md
│       ├── production/GLOSSARY.md
│       ├── stock/GLOSSARY.md
│       ├── sample/GLOSSARY.md
│       └── finance/GLOSSARY.md
├── luna-srv/                              ← 后端：lunaerp 模块，分包 baseinfo/business/production/stock/sample/finance
└── luna-ui/                               ← 前端
```

ADR **刻意不设上下文级 `adr/` 子目录**：全部在 `docs/adr/` 扁平存放、全局编号，因为 spec 与上下文文档会跨上下文按编号引用它们。

## 使用术语表中的词汇

当你的输出需要命名领域概念（issue 标题、重构提案、假设、测试名），或在前端/后端代码中定义状态机常量、字典标签映射（如 Tag label、下拉选项文案）时，必须严格使用对应上下文 `GLOSSARY.md` / 字典（如 `lunaerp_weaving_task_status`）中定义的标准术语与中文释义，不得随意使用近义词或手写字面量（例如将「待开始」写成「待排/未开工」、「生产中」写成「进行中」）。

若所需概念尚未进入术语表，这是一个信号：要么你在发明项目并不使用的语言（重新考虑措辞），要么确实存在术语缺口（记录给 `/domain-modeling`）。

## 标记 ADR 冲突

如果你的输出与某条既有 ADR 矛盾，要显式提出，而不是悄悄覆盖：

> _与 ADR-0007（事件溯源订单）冲突，但值得重新讨论，因为……_
