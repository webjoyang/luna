# 02: 纱线配比 BOM 主从表（按产品挂配比）

**What to build:** 用户从"生产管理 → 纱线配比BOM"菜单进入，看到所有产品的配比 BOM 列表（产品编码/产品名称/明细行数/状态/创建人/创建时间）。可新建配比 BOM（必选产品档案远程搜索，一产品一 BOM，租户内唯一），编辑时进入工作台页维护明细行（每行选纱线规格 + 填 ratio 百分比数字 + spin_method + ref_price），明细随主表聚合保存整体替换（同 ADR-0003 模式）。配比之和不强制等于 100。明细无 line_no。配比 BOM 是测算的唯一权威源（ADR-0011）。

**Blocked by:** 01 纱线档案地基（需要 yarn_type/yarn_spec 表与 cascade-options 接口）

**Status:** completed

- [x] `luna-srv/sql/postgresql/lunaerp-production-yarn-recipe-bom.sql` 已落：
  - [x] 表 3 `lunaerp_production_yarn_recipe_bom` DDL（product_id/product_code/product_name/remark/status + 框架列）+ 主键 DO 块 + 列注释 + 序列
  - [x] 唯一索引 `(tenant_id, product_id)`（一产品一 BOM，软删不释放）+ 普通索引 `(status)`
  - [x] 表 4 `lunaerp_production_yarn_recipe_bom_item` DDL（recipe_bom_id/yarn_type_id/yarn_spec_id/ratio/spin_method/remark/ref_price + 框架列）+ 主键 DO 块 + 列注释 + 序列
  - [x] 普通索引 `(recipe_bom_id)`
  - [x] 菜单 10250 `纱线配比BOM`（parent=10200，component `lunaerp-elmui/production/yarn-recipe-bom/index`，componentName `LunaerpElmuiProductionYarnRecipeBom`）+ 按钮 10251/10252/10253（create/update/delete）
  - [x] 无字典
- [x] 后端 `production.yarnrecipebom` 子包：
  - [x] `YarnRecipeBomDO`（主表，继承 TenantBaseDO）
  - [x] `YarnRecipeBomItemDO`（明细，继承 TenantBaseDO，可放同包或 `.item` 子包）
  - [x] `YarnRecipeBomMapper` + `YarnRecipeBomItemMapper`
  - [x] `YarnRecipeBomItemSaveReqVO`（yarn_type_id/yarn_spec_id/ratio/spin_method/remark/ref_price）
  - [x] `YarnRecipeBomSaveReqVO`（product_id/product_code/product_name/remark/status + `items: List<YarnRecipeBomItemSaveReqVO>`）
  - [x] `YarnRecipeBomPageReqVO`（productId/productCode/status）
  - [x] `YarnRecipeBomRespVO`（内嵌 items）
  - [x] `YarnRecipeBomService` + `YarnRecipeBomServiceImpl`：
    - [x] create：校验 product_id 存在（报 1_030_008_004 段相关）；product_id 已存在 BOM 报错或转为更新；明细 yarn_spec_id 校验存在（报 1_030_008_002）；保存 product_code/product_name 快照
    - [x] update：不存在报 1_030_008_004；明细整体替换（先删后插）；零明细合法
    - [x] delete：级联逻辑删除明细
    - [x] getByProductId：按 product_id 查 BOM + items（用于测算）
  - [x] `YarnRecipeBomController`（6 端点：page/get/create/update/delete + `GET /get-by-product-id?productId=`，`@PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-recipe-bom:*')")`）
- [x] 后端单测 `YarnRecipeBomServiceImplTest`：
  - [x] 创建：product_id 不存在报错；product_id 已存在报错；明细 yarn_spec_id 不存在报 1_030_008_002；聚合保存正确
  - [x] 更新：不存在报 1_030_008_004；明细整体替换；零明细合法
  - [x] 删除：级联逻辑删除明细
  - [x] getByProductId：返回 BOM + items
- [x] 前端 API `luna-ui/src/api/lunaerp-elmui/production/yarn-recipe-bom/index.ts`：
  - [x] URL 前缀 `/lunaerp/production/yarn-recipe-bom`
  - [x] page/get/create/update/delete + get-by-product-id
  - [x] TS 类型：YarnRecipeBomVO / YarnRecipeBomItemVO / YarnRecipeBomSaveReqVO
- [x] 前端视图：
  - [x] `luna-recipe-bom/index.vue` 列表页：搜索区（产品编码/产品名 + 状态）+ 列（产品编码/产品名称/明细行数/状态/创建人/创建时间/操作）+ 新建按钮跳工作台
  - [x] `yarn-recipe-bom/workspace/index.vue` 工作台：
    - [x] 顶部：产品档案远程搜索（必选，选中带出 productId/productCode/productName 快照）+ 状态 + 备注
    - [x] 配比明细表格（可编辑行）：纱线规格（两级级联带搜索下拉，调 yarn-spec/cascade-options，选规格自动带出 yarn_type_id+yarn_spec_id+ref_price 兜底）/ ratio（必填百分比数字 49=49%）/ spin_method（可空）/ ref_price（可空）/ 备注 / 操作（删行）
    - [x] 底部"添加明细行"按钮
    - [x] 配比之和不强制等于 100（展示当前合计值，仅提示不阻断）
    - [x] 操作栏：[保存][删除]（仅新建/编辑态可操作）
  - [x] 路由注册 `/production/yarn-recipe-bom/workspace/:id?`（remaining.ts 追加，参照 supplier/dyeing-order workspace 路由范例）
- [x] 验证：
  - [x] 后端 `mvn test -pl lunasrv-module-lunaerp -Dtest=YarnRecipeBomServiceImplTest` 全绿
  - [x] 前端 `pnpm build:local` 与 `pnpm ts:check` 改动文件零报错
