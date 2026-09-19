# 05: 删除与全链路收尾

**What to build:** 用户删除胚布单前看到二次确认，确认后主表与全部颜色行一并逻辑删除、列表不再显示，已删编号永不释放。至此胚布单完整 CRUD 闭环与四个权限码分权全部可用。

**Blocked by:** 04 颜色投胚分配

**Status:** ready-for-human（代码实现与单测完成，运行时验收待人工执行，见 Comments）

- [x] 删除接口级联逻辑删除主表与颜色行；删除不存在的单据报"胚布单不存在"
- [x] 列表删除操作有二次确认，删除后列表即时刷新
- [x] 已删除单据编号不被新单据重用（取号/查重含软删，ADR-0006）
- [x] query/create/update/delete 四个权限码端到端验证可用（按角色分权生效）
- [x] 新建→查看→编辑→颜色分配→删除全流程走通无回归
- [x] 测试覆盖：主表与颜色行级联删除、编号不重用

## Comments

- 2026-09-19（agent）：代码实现完成。落点：后端 `DELETE /delete`（`@PreAuthorize lunaerp:production:greige-order:delete`，事务内先校验存在——不存在报 1_030_006_000"胚布单不存在"，再 `deleteById` 主表 + `deleteByOrderId` 级联逻辑删除全部颜色行，不区分当前状态，对齐 sales-contract 删除模式）；编号不重用（ADR-0006）在 02/04 票已落地（`selectListByOrderNoPrefixIncludingDeleted` / `selectByOrderNoIncludingDeleted` 手写 SQL 绕过逻辑删过滤，唯一索引不带 WHERE deleted=0），本票补行为测试确认删除后取号顺延不重用软删编号；前端 API 增 `deleteGreigeOrder`，列表页操作列加删除按钮（`v-hasPermi` delete 码、danger 样式），`message.delConfirm()` 二次确认后调接口、成功提示并 `getList()` 即时刷新；SQL 脚本 10213"胚布单删除"按钮权限码已在 01 票就绪。测试：`GreigeOrderServiceImplTest` 新增 3 例（删除级联主表+颜色行、删除不存在报 1_030_006_000、删除后自动号顺延跳过软删编号），共 35 例全绿；lunaerp 模块全量测试通过。运行时验收（启服务走查：列表删除弹确认、删除后列表即时刷新、按角色分权验证 delete 按钮与接口 403/404 行为、删除后新建单据编号不重用、新建→查看→编辑→颜色分配→删除全流程回归）待人工执行。
