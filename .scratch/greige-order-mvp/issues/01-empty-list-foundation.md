# 01: 胚布单地基：空列表可访问

**What to build:** 用户从"生产管理 → 胚布单"菜单进入，看到一个可正常分页、可按关键词与状态查询的胚布单列表（本期数据为空时显示空状态，不报错）。整条路径从数据库、后端到前端菜单全部打通，作为后续票据的地基。

**Blocked by:** None (can start immediately)

**Status:** ready-for-human（代码实现与单测完成，运行时验收待人工执行，见 Comments）

- [x] 胚布单主表已建（含主键序列、租户/逻辑删除等框架托管列、状态/单据日期辅助索引）
- [x] "生产管理"顶级目录与"胚布单"菜单已注册，菜单承载 query 权限，脚本可重复执行
- [x] 胚布单状态字典（ACTIVE/COMPLETED/CANCELLED）已初始化
- [x] 后端分页接口可用：支持关键词（匹配单号/合同号/产品名称/产品编号）与状态过滤，默认单据日期倒序
- [x] 前端列表页可访问：搜索区（关键词+状态）、8 列结构、分页组件均正常
- [x] 前端隐藏编辑路由已注册（无 id 新建 / 有 id 编辑），本期页面可为占位
- [x] 测试覆盖：分页条件（keyword/status）正确透传到查询构造

## Comments

- 2026-09-19（agent）：代码实现完成。落点：SQL 脚本 `luna-srv/sql/postgresql/lunaerp-production-greige-order.sql`（主表 + 序列 + (tenant_id, order_no) 全量唯一索引不带 WHERE deleted=0（ADR-0006）+ status/order_date 辅助索引 + 10200 生产管理目录/10210 胚布单菜单/10211-10213 按钮 + 状态字典 2221/3568-3570，事务内先 DELETE 再 INSERT 可重复执行）、后端 `production.greigeorder` 四层（DO/Mapper/Service/Controller，GET /admin-api/lunaerp/production/greige-order/page，keyword 对 order_no/contract_no/product_name/product_code 做 LOWER+LIKE OR 模糊、status 等值、默认 order_date DESC）、前端 `lunaerp-elmui/production/greige-order/`（API+TS 类型、列表页关键词+状态搜索/8 列/分页/空态、占位编辑页、remaining.ts 隐藏路由 edit/:id?、DICT_TYPE.LUNAERP_GREIGE_ORDER_STATUS）。`GreigeOrderServiceImplTest` 3 例（含空白 keyword 不拼模糊）通过；lunaerp 模块全量单测 90 例通过；前端 vue-tsc 对新增文件零错误（存量错误属其他模块）、eslint 零告警。范围刻意收敛：create/get-next-no/update/delete、颜色子表、错误码 1_030_006 段常量分别留给 02/03/04/05 票；列表页删除按钮留给 05 票。运行时验收（对库执行 SQL、启服务确认导航出现"生产管理 > 胚布单"、空列表走查、关键词/状态筛选与分页、无权限角色接口被拒）待人工执行。
