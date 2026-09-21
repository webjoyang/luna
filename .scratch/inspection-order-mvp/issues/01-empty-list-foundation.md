# 01: 检验单地基：空列表可访问

**What to build:** 用户从"生产管理 → 检验单"菜单进入，看到一个可正常分页、可按关键词/类型/状态/承检方/单据日期区间查询的检验单列表（本期数据为空时显示空状态，不报错）。整条路径从数据库、后端到前端菜单全部打通，作为后续票据的地基。检验单有业务编号 INS-YYYYMMDD-NNN（ADR-0015），列表展示编号列。

**Blocked by:** None (can start immediately)

**Status:** ready-for-human（代码实现与单测完成，运行时验收待人工执行，见 Comments）

- [x] 检验单主表 `lunaerp_production_inspection_order` 已建（含主键序列、租户/逻辑删除等框架托管列、唯一索引 `(tenant_id, code)` 含软删行、辅助索引 `(status)/(order_date)/(product_id)/(inspection_factory_id)`、全列注释）
- [x] 录检明细表 `lunaerp_production_inspection_order_item` 已建（含主键序列、框架托管列、普通索引 `(inspection_order_id)/(vat_no, roll_no)`、全列注释）；本期不建 defects 字段
- [x] "检验单"菜单（10260）已注册在"生产管理"目录（10200，已由胚布单脚本创建）下，菜单承载 query 权限；主表按钮 10261-10263、同步按钮 10264（预留禁用）、明细按钮 10265-10268 已注册，脚本可重复执行
- [x] 检验单状态字典 `lunaerp_inspection_order_status`（dict_type id=2231，DRAFT/ACTIVE/COMPLETED/CANCELLED 4 项）与检验等级字典 `lunaerp_inspection_grade`（dict_type id=2232，A/B/C/DEFECTIVE 4 项）已初始化
- [x] 后端主表分页接口可用：支持关键词（匹配 code/product_code/product_name/inspection_factory_name）、type、status、inspectionFactoryId、单据日期区间过滤，默认单据日期倒序
- [x] 前端列表页可访问：搜索区（5 个条件）、列结构（检验单号/单据日期/类型/产品编码/产品名称/承检方/目标重量/已录卷数/状态 DictTag/创建人/创建时间）、操作列、分页组件、空状态均正常
- [x] 前端隐藏工作台路由已注册（`/production/inspection-order/workspace/:id?`，无 id 新建 / 有 id 查看编辑），本期页面可为占位
- [x] 测试覆盖：主表分页条件（keyword/type/status/inspectionFactoryId/日期区间）正确透传到查询构造

## Comments

- 2026-09-21（agent）：代码实现与单元测试全部完成。
  - SQL 脚本：`luna-srv/sql/postgresql/lunaerp-production-inspection-order.sql`（主表 + 明细表 + 主键序列 + (tenant_id, code) 全量唯一索引含软删行（ADR-0015）+ 辅助索引 + 10260 检验单菜单 / 10261-10263 主表按钮 / 10264 同步按钮预留 / 10265-10268 录检明细按钮 + 状态字典 2231/3584-3587、等级字典 2232/3588-3591，事务内先 DELETE 再 INSERT 可重复执行）。
  - 后端：`production.inspectionorder` 包落地 DO/Mapper/Service/Controller/VO，`ErrorCodeConstants` 追加 1-030-010 段。`GET /lunaerp/production/inspection-order/page` 支持关键词（code/product_code/product_name/inspection_factory_name LOWER+LIKE 模糊）、type/status/inspectionFactoryId 等值过滤、orderDate 区间过滤、默认 order_date DESC。
  - 单测：`InspectionOrderServiceImplTest` 3 例（条件透传、无条件排序、空白 keyword 不拼模糊）通过；`lunasrv-module-lunaerp` 全量单测 214 例全部通过。
  - 前端：`luna-ui/src/utils/dict.ts` 追加状态与等级字典常量；`api/lunaerp-elmui/production/inspection-order/index.ts` 落地 API 与 TS 类型；`views/lunaerp-elmui/production/inspection-order/index.vue` 落地列表页（5 个搜索条件、11 列结构、操作列、分页、空状态）；`workspace/index.vue` 落地占位工作台；`remaining.ts` 注册 `/production/inspection-order/workspace/:id?` 隐藏路由。
  - 静态检查：ESLint 零错误零告警，vue-tsc 对新增文件零错误。
  - 运行时验收（对数据库执行 SQL、验证生产管理菜单展示检验单、列表查询与分页交互）待人工执行。

