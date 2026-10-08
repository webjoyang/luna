# 01: install 编号总入口 + 空库/存量双库幂等实证

**What to build:** 从交付视角：一台空库机器按一个编号总入口从头跑到尾，即得到结构完整、默认数据落位的可运行系统；同一套脚本在已有存量数据的库上重跑不报错、不丢数据。不引入 flyway/liquibase，不提供 down 脚本。

**Blocked by:** None（可立即开工；特性级前提是 `_order` 各模块 SQL 已产出，缺口在本票 manifest 中登记）

**Status:** ready-for-agent

- [ ] `luna-srv/sql/postgresql/install/` 提供按序编号清单，README 维护 manifest 且验收时 manifest 与目录实际文件一致
- [ ] `00` 芋道 base（`ruoyi-vue-pro.sql`）+ `quartz.sql`
- [ ] `10` 存量 lunaerp 按序：baseinfo（customer→supplier→product）→ business（sales-contract→shipment）→ stock → production（yarn-master/recipe-bom/yarn-order/greige-order/dispatch-record/dyeing-order/weaving/inspection-order/inspection-confirmation/progress/progress-feedback/iot）→ sample-analysis
- [ ] `20` 商业化按序：common-foundation → module-gateway → approval-engine → data-permission-wiring（21 号票）→ color-instruction/export-docs/cost-card-quotation/release-production/scheduling → customer-reconciliation（域公共 3 表先行）→ vendor-reconciliation → sales-return → treasury → customer-portal（零表，仅菜单/角色/配置）→ workshop-h5（2 ALTER+菜单角色）
- [ ] 脚本规范：DDL 全程 `IF NOT EXISTS`/DO 块判存；菜单字典"先按 id DELETE 再 INSERT"；列迁移 information_schema 三态幂等
- [ ] 空库一次跑通；任意存量库重跑不报错不丢数据——双库重跑在验收库实证（本机无 PG 时必须验收库实证，不接受仅静态核查）
