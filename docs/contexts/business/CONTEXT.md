# 业务管理域术语表（business）

本文件是 Luna ERP **业务管理域（business）**的权威术语表，收录销售合同与发货管理两类单据的领域概念。
仓库领域文档总入口见根目录 `CONTEXT-MAP.md`；跨域平台约定（租户、软删除、前后端模块约定、错误码段位总表）见 `docs/contexts/shared/CONTEXT.md`。
当对话或代码中含糊使用某个词时，以本文件为准。

## 业务管理：销售合同

**销售合同 (SalesContract)**：
与客户签订的布匹销售约定，记载交易条款与商品明细，是业务管理域的第一个单据。
_避免_：合同（无前缀时与未来的采购合同歧义）、订单。

**合同明细 (SalesContractItem)**：
一份销售合同中，按单个品种（客户色号 / 门幅 / 克重组合）逐行记录的数量、单价、其他费用与金额条目。一份合同含 0~N 条明细，允许先建档后补明细。明细是合同聚合的一部分，随合同整体保存、级联删除，没有独立操作界面（与客户联系人的独立资源模式刻意相反，见 ADR-0003）。
_避免_：合同商品、明细行（口语）。

**贸易类型 (TradeType)**：
`DOMESTIC` 内销（缺省）/ `EXPORT` 外销，区分销售方向。MVP 仅作标记；外贸专属字段（贸易术语、港口、英文抬头等）与出口单证在后续版本补。

**订单属性 (OrderType)**：
`BULK` 大货（缺省）/ `SAMPLE` 样布，区分批量生产订单与打样。

**合同状态 (SalesContractStatus)**：
`ACTIVE` 进行中（新建缺省）/ `COMPLETED` 已完成 / `CANCELLED` 已取消。MVP 无审批流，由编辑页手动切换，不做流转约束。旧系统的审批态（draft/submitted/approved/rejected）、业务进度 progress、回款状态 paymentStatus 三套状态在 MVP 均不引入。

**业务员 (Salesperson)**：
负责该合同的内部销售人员，关联 system `User`，可空。区别于客户侧的客户联系人。

**客户名称快照 (clientName)**：
建单时从客户档案复制到合同上的客户公司名。客户档案后续改名不影响既有合同；客户为必选（`customerId` 逻辑关联，不建数据库外键），联系人与地址选客户时带出但可手工修改。

**合同编号 (ContractNo)**：
形如 `CH20260001`：`CH` + 年份 + 4 位顺序号（超 9999 自然扩位），租户内唯一，新建缺省由服务端生成、也可手填（撞号报错）。编号在软删除后也不释放、不重用（见 ADR-0004）。

## 业务管理：发货管理

**发货单 (Shipment)**：
向客户交付布匹货物的出库凭证，是业务管理域的第二个单据，也是库存出库的权威凭证（见 ADR-0029）。三态严格过账状态机（`DRAFT` / `COMPLETED` / `CANCELLED`）。一张发货单对应单一发货仓库。审核即出库过账写负库存流水（`SHIPMENT_OUT`），取消已发货单逐条红冲正流水（`SHIPMENT_OUT_REVERSAL`）。
_避免_：销售出库单（本系统以发货单兼作销售出库凭证，不另设销售出库单）、送货单。

**发货明细 (ShipmentItem)**：
一张发货单中按产品 + 客户色号 + 缸号聚合的品类汇总行，记录数量、匹数、单位、单价与金额。随发货单整单聚合保存、级联逻辑删除（见 ADR-0003）。行金额由服务端重算（数量 × 单价）。可关联来源入库明细（追溯用，零回写、不锁库）。

**卷清单 (ShipmentRoll)**：
发货明细下逐卷登记的布卷记录（卷号、实称重量 KG、米数、备注），独立子表存储，随发货单整单聚合保存，软齐套不锁卷（见 ADR-0030）。不设卷号数据库唯一约束。

**发货单号 (ShipmentNo)**：
形如 `SH-YYYYMMDD-NNN`：`SH` + 年月日 8 位 + 3 位日流水（超 999 自然扩位），按天取号，租户内全量唯一含软删行（ADR-0029）。

**发货单状态 (ShipmentStatus)**：
字典 `lunaerp_shipment_status`（id=2245）：`DRAFT` 草稿（缺省）/ `COMPLETED` 已发货 / `CANCELLED` 已取消。仅草稿可编辑删除；审核（发货）同事务写负流水；取消已发货单逐条红冲正流水；取消草稿仅作废不写流水。

**签收状态 (ShipmentSignStatus)**：
字典 `lunaerp_shipment_sign_status`（id=2246）：`PENDING` 待签收（缺省）/ `SIGNED` 已签收 / `REFUSED` 拒签。文员轻量内部登记，仅 COMPLETED 单可登记/修改；拒签不自动红冲库存。

**运费 (FreightFee)**：
发货物流产生的运费金额，独立字段记录，不并入货物金额合计。

**合同未发量 (ContractUnshippedQuantity)**：
销售合同某明细行的合同数量减去该合同已生效的发货出库数量（按 SHIPMENT_OUT 流水实时聚合），只读动态计算，不落库。

## 模块边界

销售合同落在 `business`（业务管理）子包，与 `baseinfo` 平级：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.business.salescontract[.item]`，前端 `lunaerp-elmui/business/sales-contract/`（视图与 API 目录镜像）。

## 字典

**销售合同字典 (Sales contract dictionaries)**：
合同的枚举字段同样走 `system_dict_*`，前缀 `lunaerp_sales_contract_`：`lunaerp_sales_contract_status`（ACTIVE/COMPLETED/CANCELLED）、`lunaerp_sales_contract_trade_type`（DOMESTIC/EXPORT）、`lunaerp_sales_contract_order_type`（BULK/SAMPLE）、`lunaerp_sales_contract_unit`（KG/M）、`lunaerp_sales_contract_currency`（RMB/USD/EUR/HKD）。

**发货管理字典 (Shipment dictionaries)**：
发货域建两个字典：`lunaerp_shipment_status`（id=2245，DRAFT 草稿 / COMPLETED 已发货 / CANCELLED 已取消）、`lunaerp_shipment_sign_status`（id=2246，PENDING 待签收 / SIGNED 已签收 / REFUSED 拒签）。单位复用库存域 `lunaerp_stock_in_unit`、币种复用销售合同 `lunaerp_sales_contract_currency`；承运商为自由文本不入字典；流水 txn_type（`SHIPMENT_OUT` / `SHIPMENT_OUT_REVERSAL`）为后端内部枚举不入字典。

## 错误码

错误码段位总表与新模块取段规则见 `docs/contexts/shared/CONTEXT.md`。本域占用段位：

- `1-030-003-xxx` 销售合同（主表与明细共用）
- `1-030-016-xxx` 发货单（主表/明细/卷清单共用）：000 发货单不存在 / 001 发货单号重复 / 002 状态不允许该操作 / 003 关联引用不存在（客户/仓库/合同/产品/来源入库明细/业务员）/ 004 审核失败发货单没有明细 / 005 库存不足（返回需求量与可用量）/ 006 签收状态非法。
