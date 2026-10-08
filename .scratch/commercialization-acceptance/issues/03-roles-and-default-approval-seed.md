# 03: 11 标准角色 seed + 菜单/按钮授权基线 + 六类默认审批流

**What to build:** 全新安装后，用 §6.1 十一个标准业务角色账号登录，菜单树与按钮可见性与出厂基线一致、销售链 5 表数据范围为基线默认值；六类单据各存在一条默认启用审批链，建单即可走审批。角色是客户现场可修改基线，本票只保证出厂默认值且 seed 幂等。

**Blocked by:** 01（data_scope 基线值以 21 号票 spec §4.4 为准；与 21 号票角色对齐 SQL 幂等共存，不固定角色 id）

**Status:** ready-for-agent

- [ ] 11 角色幂等 upsert（`system_role`，code 唯一、不固定 id、tenant_id=1，仿 workshop seed）：boss/finance/sales_manager/salesperson/planner/purchaser/warehouse_manager/warehouse_keeper/inspector/workshop_worker/workshop_inspector；super_admin/admin 沿用既有全量逻辑
- [ ] data_scope 基线按 21 号票：boss/finance/planner/purchaser/warehouse_manager/warehouse_keeper/inspector=1（全部）；sales_manager=4（本部门及以下）；salesperson/workshop_worker/workshop_inspector=5（本人）；`data_scope_dept_ids` 统一置空
- [ ] §6.1 菜单/按钮基线授权落 seed SQL：boss 全分组可操作（不含系统管理超管项）；finance 财务组全量+销售组只读与对账/收款/发票申请；sales_manager 销售组全量+生产内报价/打色/下达+客户产品档案+成本卡可读；salesperson 制单无 delete/audit、成本卡/纱价菜单不可见、报价无成本字段；planner 生产组不含成本卡纱价、库存只读；purchaser 供应商/纱线档案/纱线采购/纱价、供应商对账只读与申请、财务组不可见；warehouse_manager 库存全量含 :audit、发货 :audit、退货到货质检；warehouse_keeper 入库/发货制单无 :audit；inspector 检验单/录检/质检确认；workshop_worker 仅 H5 报工/领纱按钮（10531/10533）；workshop_inspector 仅 H5 质检按钮（10532）；后两者零 PC 菜单
- [ ] 细到按钮的授权以 seed SQL 为准且与 §6.1 矩阵逐行一致；amount/contact 两个敏感位按矩阵默认值授予
- [ ] 六类单据各一条默认启用审批流：销售合同生效、CUSTOMER_RECEIPT 收款、PAY_APPLY 付款、报销、工资单、销售退货（逐级单节点起步，客户可改）
- [ ] seed 重跑幂等：重复执行不产生重复角色、重复授权或重复流程；角色被客户修改后重跑不覆盖客户改动（按 code 只 upsert 出厂行，或提供明确的对齐策略并在 runbook 说明）
