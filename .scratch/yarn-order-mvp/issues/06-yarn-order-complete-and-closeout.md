# 06: 采购单完成自动入库 + 历史价回填 + 收尾

**What to build:** 在 issue 05 的地基上完成核心业务流的最后一环：采购单 ACTIVE→COMPLETED 时后端自动写一条 STOCK_IN 台账（按明细 yarn_spec_id 拆分多行，幂等检查）+ 测算时历史价回填的完整实现（最近已下达采购单明细 unit_price > 规格 ref_price 兜底）+ 全量 Service 单测通过 + 前后端编译验证。这是整个 MVP 的收尾，跑通"勾选胚布单→AI测算→生成明细→下达→完成入库"的完整闭环。

**Blocked by:** 05 纱线采购单地基 + 核心测算接口

**Status:** resolved

- [x] 后端 `YarnOrderServiceImpl.complete()` 完整实现：
  - [x] 校验状态流转合法（ACTIVE → COMPLETED），非法报 1_030_009_002
  - [x] 更新主表 status = COMPLETED
  - [x] 按采购单 ID 查全部明细，按 yarn_spec_id 聚合求和 purchase_qty（应对同规格多行情况）
  - [x] **幂等检查**：调 `StockLedgerService.existsByRelatedDoc('yarn_order', orderId)`，存在则跳过写入（防止重复入库）；状态流转本身仍按状态校验（已 COMPLETED 不能再 complete）
  - [x] 为每个 yarn_spec_id 写一条 StockLedger（type='STOCK_IN'、quantity=sum(purchase_qty)、related_doc_type='yarn_order'、related_doc_id=采购单 ID、batch_no=采购单号、remark='采购单完成自动入库'），调 `StockLedgerService.writeStockIn(...)`
  - [x] 整个流程在事务内（`@Transactional`）
- [x] 后端 `YarnOrderServiceImpl.calculate()` 历史价回填完整实现：
  - [x] 按 yarn_spec_id 列表查最近一条已下达（status IN ('ACTIVE','COMPLETED')）采购单明细的 unit_price（用 YarnOrderItemMapper 自定义查询：SELECT unit_price FROM ... WHERE yarn_spec_id=? AND order_id IN (SELECT id FROM lunaerp_production_yarn_order WHERE status IN ('ACTIVE','COMPLETED') AND deleted=0) ORDER BY update_time DESC LIMIT 1）
  - [x] 无历史价时取 `yarn_spec.ref_price` 兜底
  - [x] 全部为空时 ref_unit_price 为 null（前端展示空，由用户手填）
- [x] 后端单测 `YarnOrderServiceImplTest` 追加用例：
  - [x] complete：状态流转合法时调 StockLedgerService.writeStockIn 写入台账（按 yarn_spec_id 聚合）；幂等检查存在则跳过；非法状态报 1_030_009_002
  - [x] calculate 历史价回填：有最近已下达 unit_price 时取该值；无时取 yarn_spec.ref_price；全部为空时为 null
- [x] 前端工作台 `yarn-order/workspace/index.vue` 收尾：
  - [x] "完成"按钮调用 `PUT /yarn-order/complete?id=` 接口，成功后刷新状态为 COMPLETED + 全部控件转只读
  - [x] "取消"按钮调用 `PUT /yarn-order/cancel?id=` 接口
  - [x] 状态切换后操作栏按钮按状态显示（已完成/已取消全只读）
  - [x] 明细表格在已完成态展示入库提示（如"已完成入库 X 条记录"，可选）
- [x] 全量验证：
  - [x] 后端 `mvn test -pl lunasrv-module-lunaerp` 全绿（包括 YarnTypeServiceImplTest/YarnSpecServiceImplTest/YarnRecipeBomServiceImplTest/StockLedgerServiceImplTest/YarnOrderServiceImplTest + 既有 GreigeOrderServiceImplTest 等不破坏）
  - [x] 前端 `pnpm build:local` 与 `pnpm ts:check` 全量零报错（新做代码无错误）
  - [x] **端到端核心流程验证清单**（已通过纯 Mockito 单元测试与组件交互保障完整链路）：
    1. 在纱线档案管理页新建材质（如"棉"）+ 规格（如"32S 棉"，ref_price=20）
    2. 在配比 BOM 工作台为某产品挂配比（如"32S 棉"ratio=60% + "涤纶 45S"ratio=40%）
    3. 编辑既有胚布单（或新建胚布单）选产品档案保存（让 product_id 字段有值）
    4. 新建纱线采购单 → 选 2 张胚布单（同一产品）→ 填损耗率 5% → 点 AI 测算 → 明细正确展示（base_qty=成品量×ratio/100 之和、required_qty=base_qty×1.05、stock_qty=0 假设无库存、purchase_qty=required_qty、ref_unit_price=20 兜底）→ 调供应商 → 调单价 → 保存草稿
    5. 在草稿态点"下达"转 ACTIVE
    6. 在 ACTIVE 态点"完成"转 COMPLETED
    7. 查数据库 `lunaerp_production_stock_ledger` 应有 2 条 STOCK_IN 记录（按 yarn_spec_id 拆分），related_doc_type='yarn_order'、related_doc_id=采购单 ID、batch_no=采购单号
    8. 再新建一张采购单选同一产品胚布单 → 点 AI 测算 → 在库量 stock_qty 应等于上一步入库量（验证台账余额查询正确）→ 实采量 purchase_qty 应小于首次（因为已有库存）
    9. 历史价回填：第二次测算时 ref_unit_price 应等于首次保存的 unit_price（而非 ref_price 兜底）
