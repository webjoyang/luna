# 02: finance 域骨架 + 唯一取数服务 SupplierReconSourceService

**What to build:** 从财务/采购视角，选定供应商与对账区间后，系统能产出一份"还没被任何对账单引用过"的候选明细集合：加工费候选来自已录检完成的检验单（按承检方汇总，Σ实称重量/Σ米数/卷数，全等级计入，GREIGE 缺省按米、FINISHED 缺省按公斤），采购货款候选来自已审核的 PURCHASE 入库明细（退货入库排除）。候选装配只在内存中进行、不落库，并已排除被 DRAFT/CONFIRMED 对账单引用的指纹；对账单作废后指纹自动回到候选池。本票同时交付两个子包的骨架与全部领域常量，是老系统 P0 教训"取数口径收敛为单一服务、禁止他处复制口径"的核心落地件。

**Blocked by:** 01（SQL 基建）

**Status:** resolved

- [x] 子包骨架就位：`finance.supplierrecon` 与 `finance.payment` 的 controller/service/dal 分层；DO×5 + Mapper×5；枚举 ReconStatus / LineType(PROCESS/PURCHASE/ADJUST) / ApplyType(RECON/PREPAY) / Direction(IN/OUT) + 022/023 错误码常量
- [x] 加工费候选：仅 `status=COMPLETED AND deleted=0` 检验单，按承检方=供应商、单据日期 ∈ 区间（含端点）过滤；一张检验单汇总一行，数量 = 录检明细 Σmeasured_weight(KG)、Σlength_m(M)、卷数；等级 A/B/C/DEFECTIVE 全计入不过滤
- [x] 缺省计价基准：GREIGE→METER、FINISHED→KG，bill_qty 取基准对应量；快照检验单号/类型/产品；跨模块只走检验单/录检明细 Service 或同 module Mapper，不直连他人表
- [x] 采购货款候选：仅主表 COMPLETED 且 in_type=PURCHASE 的入库明细，按供应商与区间过滤；一条明细一行，带入库单号、批次/缸号、产品快照、quantity+unit、明细单价；in_type=RETURN 不进候选
- [x] 防重指纹 source_ref_type(INSPECTION/STOCK_IN_ITEM)+source_ref_id(+source_ref_item_id)：候选生成时排除被 DRAFT/CONFIRMED 对账单引用的指纹；CANCELLED 对账单的指纹释放回池；应用层守卫口径仅此一处实现
- [x] 金额/数量/单价全程 DECIMAL（BigDecimal），禁止 double 参与业务计算
- [x] Mockito 单测（不启动 Spring、不连库）：承检方过滤、ΣKG/ΣM/卷数聚合、两类缺省基准、PURCHASE 过滤与 RETURN 排除、指纹排除与作废释放、区间起止边界、空候选集合

### 交付说明
- 完成 `finance.supplierrecon` 与 `finance.payment` 骨架分层，包含 DO×5、Mapper×5 与 6 大核心领域枚举。
- 实现 `SupplierReconSourceService` 唯一取数真相口径，内存纯装配、不落库，支持 COMPLETED 检验单（全等级不过滤聚合）与已审核 PURCHASE 入库明细。
- 实现防重复取数指纹机制（DRAFT/CONFIRMED 排除，CANCELLED 释放回池）。
- 交付纯 Mockito 单测 `SupplierReconSourceServiceImplTest`，8 个测试用例全部通过，零连库零启动 Spring。
