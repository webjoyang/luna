# 05: 从合同/发货单带出（from-contract / from-shipment）

**What to build:** 落地两个"一键带出草稿"端点，返回预填好的草稿数据供工作台接收（不落库，前端调 create 时才持久化）。`GET /from-contract?contractId=`：按合同预填草稿——4 条款列（incoterm/两港/paymentTerm，来自 02 加的合同列）+ 买方英文名址（经 customer_id 读客户档案 name_en/address_en，为空回退合同 client_name/client_address 中文快照）+ 行快照映射（name→nameCn、composition+width→spec、quantity/unit/unitPrice/amount；**netWeight/grossWeight/packages/volume 一律留空**因合同 weight 是克重 GSM 不是净重）；非 EXPORT 合同拒绝（EXPORT_DOC_CONTRACT_NOT_EXPORT）。`GET /from-shipment?shipmentId=`：按发货预填——卷清单净重按明细行 SUM 聚合（`Σ shipment_roll.weight`，比老系统行级 actualWeight 更细）、实发客户名优先于合同抬头、合同条款经发货单的合同联动带出；grossWeight/packages/volume 留空手填。buyerBankInfo 始终留空手填（随单变化，不进任何主数据）。currency 取合同/发货单 currency。卖方抬头缺省取 04 的租户配置。单测覆盖字段映射断言 + 非 EXPORT 拒绝 + 卷净重 Σ 聚合 + 实发客户名优先。

**Blocked by:** 03（Service 骨架）, 02（合同 4 条款列 + 客户英文名址需先就位）

**Status:** ready-for-human

- [x] `GET /from-contract?contractId=`：4 条款列 + 客户英文名址（为空回退中文快照）+ 行快照；净重/毛重/箱数/体积留空；非 EXPORT 合同拒绝 EXPORT_DOC_CONTRACT_NOT_EXPORT
- [x] `GET /from-shipment?shipmentId=`：卷清单净重按明细行 SUM 聚合、毛重/箱数/体积留空手填、实发客户名优先、合同条款经发货单的合同联动带出
- [x] buyerBankInfo 始终留空手填；currency 取合同/发货单；卖方抬头缺省取租户配置
- [x] 权限 `lunaerp:business:export-doc:create`
- [x] 单测：从合同带出字段映射断言 + 净重留空 + 非 EXPORT 拒绝；从发货带出卷净重 Σ 聚合 + 实发客户名优先 + 合同条款联动
