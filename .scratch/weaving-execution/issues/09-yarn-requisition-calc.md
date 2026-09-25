# 09: 领纱单主表/明细（独立子资源）+ 配比 BOM 测算

**What to build:** 新建 `production.weavingorder.requisition` 子包，落 `lunaerp_production_yarn_requisition`（主：code、weavingOrderId、status 五态、remark）与 `lunaerp_production_yarn_requisition_item`（yarnTypeId/yarnSpecId/yarnSpecName、requiredQty、dispatchedQty 缺省0、unit=KG、remark）。主表端点：分页（按织造单）、详情、创建、更新、提交（DRAFT→ACTIVE）、关闭；明细**独立子资源**端点（创建/分页/更新/删除，仿 ADR-0014）。编号派生：`YS-{织造单号去符号}-{单内 2 位序号}`。**测算接口** `calculate`（后端权威，ADR-0027/0010）：`需求KG = 计划匹数 × 单匹计划重量(plannedWeight/plannedQty) × ratio/100`，取该产品 yarn_recipe_bom 按 yarnSpecId 合并；无 BOM/缺重量返回提示，结果可手改。发纱在 issue 10。本 issue 状态推进只到 ACTIVE；PARTIAL/DISPATCHED 推导随发纱。SQL 追加至 `lunaerp-production-weaving.sql`；菜单不开独立入口（织造单工作台领纱 Tab），权限码 `lunaerp:production:weaving-order-requisition:`。

**Blocked by:** 08

**Status:** ready-for-agent

- [ ] 领纱单/明细表 + 主表端点（提交/关闭）
- [ ] 明细独立子资源 CRUD
- [ ] YS 派生编号；BOM calculate 端点（合并/提示）
- [ ] 前端织造单工作台领纱 Tab（明细编辑 + 测算按钮 + 提交）
- [ ] 字典 id=2243；单测：测算口径、编号、提交守卫
