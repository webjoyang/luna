# 05-cost-card-quotation: 成本卡与智能报价

Type: grilling
Status: resolved
Blocked by: 

## Question

设计售前链成本卡与智能报价并产出 spec（W1，对应 Q2 核心包），grilling 时先调 Skill "grilling" 与 "domain-modeling"：

1. 纱价主档：纱线材料价格主档（采购价/市场参考价、生效日期、更新机制），落 baseinfo 还是生产域？
2. 成本卡数据模型：引用产品 + 纱线配比 BOM + 克重 + 工序费用项（织造/染色/定型/检验/包装/运费）构成成本测算；与现有样品分析的成本计算（`product-info-align/issues/03-yarn-cost-calc-function-and-tests.md` 已有 yarn cost calc function）的关系——复用、扩展还是新算？必须先读现有实现再定；
3. 报价单：多版本、有效期、毛利率反算售价、客户专属价；
4. 报价→销售合同转化：由报价单生成合同行（`sales-contract-mvp` 现有结构的衔接）；
5. 智能报价的"智能"限定为规则型（Q5=A）：按成本卡参数化测算 + 毛利率目标，不做 AI。

产出：`.scratch/cost-card-quotation/spec.md`（按统一 DoD）。

## Answer

2026-10-07 经 3 轮 18 问 grilling（全部按推荐锁定），产出：

- Spec：`.scratch/cost-card-quotation/spec.md`（ready-for-agent，统一 DoD 全项对照、前端验收清单+W1 波次冒烟、实现顺序）；`_order.md` 序号 22。
- ADR：[ADR-0039](../../docs/adr/0039-cost-card-snapshot-bom-convergence-backend-pricing.md)（成本卡快照模型 + 双配比源收敛 BOM + 后端算价权威性；显式修订 ADR-0035 的算价位置）。
- 术语：business（成本卡/费用行/报价单/版本/有效期/毛利率/转合同）、production（纱价、BOM 五测算列）、baseinfo（客户默认毛利率、成本分区改 trial）三域 GLOSSARY；shared 段位表 1-030-034 + 历史避让记录。

五个问题的结论：

1. **纱价主档**：生产域新表 `lunaerp_production_yarn_price`（规格+PURCHASE/MARKET 价类+生效日期+历史，部分唯一索引，取价=不晚当日最近一条、无记录回退 ref_price）；维护入口挂纱线档案抽屉不设菜单；一期手工+Excel，最近采购价只读参考不自动回写。
2. **成本卡模型**：business 域独立单据（主表+纱线行+费用行两张结构化子表），DRAFT/EFFECTIVE/EXPIRED，同品同时仅一生效卡（部分唯一索引），生效冻结、调价复制新建；六项费用行（织造/染色/定型/检验/包装/运费，PER_KG/PER_METER+损耗，KG/M 双口径换算）；针织 KG/梭织 METER 双口径不含税/含税成本。与现有 yarn cost calc 的关系＝**后端移植复用**：三模式公式移为 Java 纯函数 `CostCardPricingCalculator` + `/trial` 试算，配比源收敛到 `yarn_recipe_bom`（item 补 5 列），产品 yarn_specs JSON 与前端 TS 函数冻结。
3. **报价单**：`QT{年}NNNN` 同号多版本（修订=旧版 SUPERSEDED+新版 DRAFT，同号至多一草稿，页面 `QT...-v2`）；有效期 valid_from/valid_to，"已失效"派生不入状态；真毛利率 `售价=成本/(1−毛利率)`（刻意弃用老系统加成率，对齐 W3 经营账）；客户专属价一期=客户档案 default_margin_rate 预填+报价单自身承载，不建协议价表；外币选手工快照汇率。
4. **报价→合同**：仅最新 CONFIRMED 可转，预填合同新建草稿（头+明细映射）、币种锁定、人工可改，保存回写 source_quotation_id/no；可多次转，不锁数量不追踪。
5. **智能限定**：规则型——参数化成本卡测算+毛利率目标+低价预警（infra_config 阈值，不阻断），不做 AI。

资源归位：错误码 1-030-034（000–099 卡 / 100–199 报价；纱价追加生产域 1-030-008-008/009；落稿前发现 032 打色、033 出口单证已占，顺延 034）；字典 type 2262–2268（避让 2258–2261）、dict_data 3770–3791（避让 3736–3764）；菜单 10132 成本卡/10136 报价单挂销售目录（sort 0 排在合同前，避让出口单证 10126–10131），纱价按钮 10245–10248 挂纱线档案；前端落正式目录 `views/lunaerp/business/`；报价单 W0 固定套打（打印 VO 白诺永不含成本/毛利）；卡 DRAFT/报价 DRAFT/纱价注册 W0 回收站，纱价注册 Excel 导入；售前单据无库存账务副作用。二期：核价申请流、报价审批、协议价表、工序费用主档、采购价自动回写、AI/爬虫行情。
