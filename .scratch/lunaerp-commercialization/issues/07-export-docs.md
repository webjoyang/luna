# 07-export-docs: 出口单证（PI/CI/装箱单）字段与模板研究

Type: research
Status: resolved
Blocked by: 

## Question

研究出口单证并为后续 spec 铺路（W1，Q12=A）：

1. 调研老系统 linaagi 的出口单证实现（数据来源表、字段、模板样式）；
2. 调研外贸单证惯例：PI（形式发票）/CI（商业发票）/PL（装箱单）的标准抬头、条款、签章位、中英文栏位；
3. 核对新栈数据充分性：销售合同（多币种/内外销标记/合同行）+ 发货单（卷明细/毛净重）能否直接支撑三种单证，列出缺口；
4. 输出：字段映射清单（单证字段 ← 系统数据来源）+ 缺口清单 + spec 草案 `.scratch/export-docs/spec.md`（含三单证的打印模板内容结构，按统一 DoD）。

本 ticket AFK，可由 research 子代理直接解决。

## Answer

**spec 草案**：`.scratch/export-docs/spec.md`（含三单证字段映射清单、缺口清单、打印模板内容结构、验收标准）。

**研究结论摘要**：

1. **老系统实现**（`linaagi/docs/architecture/modules/export-documents.md`、`2026-06-18-foreign-trade-export-documents-design.md`、`server/src/lib/export-doc-mapping.ts`、`export-docs/templates/*`）：核心是"出口单证套"（ExportDocSet + ExportDocLine）——共享抬头+共享明细为单一真相来源，PI/CI/PL 各叠加专属 meta JSON；从合同/发货单带出 + resync 只填空字段（优先级 发货单>合同>企业配置）；卖方默认信息存 `Tenant.brandConfig.exportInfo`；模板为 760px A4 全英文（PI 含有效期/定金/分批转运条款、CI 含原产地与真实性声明、PL 含唛头/毛净重/CBM/手填汇总优先）。
2. **新栈数据充分性**：销售合同（trade_type/currency/明细）+ 发货单（卷清单逐卷实称重）可支撑三单证落地，但合同 7 个外贸字段与发货毛重/体积/箱数在 MVP 时均已砍（G1/G4）；客户档案无英文名址（G3）、无租户卖方出口配置（G2）、无唛头（G7）。
3. **缺口是否致命：不致命。** 老系统核心工作模式本就是"部分带出 + 人工补录"——从合同带出后净重/HS/英文名也全留空（合同 weight 是克重 GSM 不能当净重），新栈沿用该模式即可消化全部缺口，只损失一键带出率。唯一必须随模块落地的前置是 G2（租户卖方出口配置，用 `infra_config` 键 `lunaerp.export.info` 解决，零新表）。
4. **报关数据项预留**（map.md 悬置项）：一期只做 PI/CI/PL，不落 customsMeta/coMeta 列；meta JSON 透传模式下二期后补成本极低，无需现在预留。
5. **打印方案**（Q12=A）：Vue 固定套打模板 + `window.print()`（沿用 inspection-confirmation PrintDialog 先例）+ 全局 Qrcode 组件渲染单证号二维码；一期不做服务端 PDF/Excel 与模板设计器。

落地建议：spec 草案 Status: draft，需一次 grilling 定稿（重点：合同是否回补 7 个外贸字段 = G1、卖方配置落点、错误码段位）。
