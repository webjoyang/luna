# 01: 横切地基 prefactor（错误码 / ADR-0039 / 术语 / infra_config seed）

**What to build:** 落盘其余 9 票都要引用的横切地基：1-030-034 段位错误码常量（成本卡 000-099 + 报价单 100-199 共 13 个）+ 纱价 1-030-008-008/009 追加码；ADR-0039（成本卡快照模型 + 双配比源收敛 BOM + 后端算价权威性，显式修订 ADR-0035 的算价位置，不推翻字段迁移）；shared GLOSSARY 段位表新增 1-030-034 + 历史避让记录（032 打色指示单、033 出口单证已占，故顺延 034）；business/production/baseinfo 三域 GLOSSARY 术语（成本卡/费用行/报价单/报价版本/有效期/毛利率/转合同/纱价两价类/取价回退/BOM 五测算列/客户默认毛利率/成本分区改后端 trial 的修订说明）；infra_config seed `quotation.min-margin-rate=0`；前端 `luna-ui/src/utils/dict.ts` 增 7 个字典常量。本票不交付用户行为，是其余 9 票的引用地基，刻意单独成票以避免后续票重复横切改动。

**Blocked by:** None（can start immediately）

**Status:** ready-for-agent

- [x] lunaerp 模块 `ErrorCodeConstants.java` 落 1_030_034_000-107 + 1_030_008_008-009 常量（13 + 2 共 15 条），注释含中文名与触发场景，与 spec「错误码」节一致
- [x] `docs/adr/0039-cost-card-snapshot-bom-convergence-backend-pricing.md` 落 ADR-0039（含「修订 ADR-0035 算价位置」明示，不推翻字段迁移结论）
- [x] shared GLOSSARY 段位表追加 1-030-034 段位（卡 000-099 / 报价 100-199），含 032/033 已占避让记录、纱价追加 1-030-008-008/009 记录
- [x] business GLOSSARY 增术语：成本卡 / 费用行 / 报价单 / 报价版本 / 有效期 / 毛利率 / 转合同
- [x] production GLOSSARY 增术语：纱价（采购参考价 / 市场参考价）、取价回退（SPEC_REF_FALLBACK）、BOM 五测算列（损耗 / 方向 / 密度 / 织缩 / 每米用量）
- [x] baseinfo GLOSSARY 增术语：客户默认毛利率、产品成本分区算价改后端 trial 的修订说明
- [x] infra_config seed `quotation.min-margin-rate=0`（沿用 W0/infra 既有配置机制，单键，见 02 SQL）
- [x] 前端 `luna-ui/src/utils/dict.ts` 增 7 个字典常量：`LUNAERP_COST_CARD_STATUS` / `LUNAERP_COST_CARD_BASIS` / `LUNAERP_COST_CARD_FEE_TYPE` / `LUNAERP_COST_FEE_UNIT` / `LUNAERP_YARN_PRICE_TYPE` / `LUNAERP_QUOTATION_STATUS` / `LUNAERP_QUOTATION_COST_SOURCE`
