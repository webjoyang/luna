# 03: 胚布指示单加 product_id 关联

**What to build:** 胚布指示单（已实现的 production-greigeorder 模块）追加 `product_id` 字段强关联产品档案，用于纱线测算时按 product_id 查配比 BOM（ADR-0011）。修改既有 DDL（ALTER TABLE 追加列 + 索引）+ DO/VO 字段 + 前端编辑页加产品档案远程搜索下拉（选中带出 productId+productCode+productName 快照）。既有的胚布单（无 product_id）也能补填。

**Blocked by:** None (可与 01/02 并行，不依赖纱线档案/BOM 表本身存在)

**Status:** completed

- [x] DDL 修改（在 `lunaerp-production-yarn-order.sql` 末尾追加 ALTER 段，集中管理纱线域变更）：
  - [x] `ALTER TABLE lunaerp_production_greige_order ADD COLUMN IF NOT EXISTS product_id int8 NULL;`
  - [x] `COMMENT ON COLUMN lunaerp_production_greige_order.product_id IS '产品档案ID（强关联，用于纱线测算时查配比BOM，见 ADR-0011）';`
  - [x] `CREATE INDEX IF NOT EXISTS idx_lb_greige_order_product_id ON lunaerp_production_greige_order (product_id);`
  - [x] （注：既有 `product_code`/`product_name` 列已存在弱关联快照，不重复添加）
- [x] 后端 `production.greigeorder` 改造：
  - [x] `GreigeOrderDO` 加 `productId` 字段（Long，可空）
  - [x] `GreigeOrderSaveReqVO` 加 `productId` 字段
  - [x] `GreigeOrderRespVO` 加 `productId` 字段
  - [x] `GreigeOrderServiceImpl` create/update 时校验 productId（若传）对应产品存在（不存在报 1_030_006_002 "关联的产品档案不存在或不可用"），并同步刷新 product_code/product_name 快照（若产品档案改名，编辑胚布单时跟随刷新）
  - [x] 既有 Mapper selectXxx 自动覆盖新字段（MyBatis-Plus 自动映射）
- [x] 后端单测 `GreigeOrderServiceImplTest` 追加用例：
  - [x] create 时 productId 不存在报 1_030_006_002
  - [x] create 时 productId 存在则 product_code/product_name 快照正确写入
  - [x] update 时刷新 product_code/product_name 快照（产品档案改名后跟随刷新）
- [x] 前端修改 `luna-ui/src/views/lunaerp-elmui/production/greige-order/edit/index.vue`（或 workspace/index.vue，按既有实现）：
  - [x] 基本信息区追加"产品档案"远程搜索下拉（el-select + remote + filterable）
  - [x] 候选列表调既有产品档案分页接口（按 productCode/productName 关键词模糊）
  - [x] 选中后保存 productId + product_code + product_name 快照到表单
  - [x] 编辑既有胚布单（无 product_id）时也能补填
- [x] 前端 API `luna-ui/src/api/lunaerp-elmui/production/greige-order/index.ts`：
  - [x] TypeScript 类型 `GreigeOrderVO` / `GreigeOrderSaveReqVO` 追加 `productId: number | null` 字段
- [x] 验证：
  - [x] 后端 `mvn test -pl lunasrv-module-lunaerp -Dtest=GreigeOrderServiceImplTest` 全绿
  - [x] 前端 `pnpm build:local` 与 `pnpm ts:check` 改动文件零报错
  - [x] 手动验证：编辑既有胚布单选产品档案保存，刷新后 productId/productCode/productName 正确显示
