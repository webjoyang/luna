# 05: 回收站端到端：SPI + 客户 handler + 前端页

**What to build:** 仓管误删了一张草稿入库单/客户档案后，能在「系统管理→回收站」找到并一键恢复，明细行完整回来。本 ticket 是回收站 tracer bullet：交付 `RecycleBinHandler` SPI 接口 + `RecycleBinHandlerRegistry`（启动收集所有 Bean 按 bizType 路由）+ Controller + `RecycleItemRespVO`/`RecyclePageReqVO`，并以 CUSTOMER handler 为首个落地（含客户联系人级联复活）；前端回收站页按 bizType 分 tab，列：名称/单号、删除人、删除时间，操作：恢复/彻底删除。零新表（复用各表现有 `deleted` 列）。查询/复活/物理删除 Mapper 全部走手写 XML（显式 `deleted=1 AND tenant_id=?`，绕开 MP 逻辑删除与租户拦截器自动改写）。恢复规则：主数据恢复为 INACTIVE（用户手动启用）；级联子表父→子复活；引用阻断（引用的主数据也在回收站报 1_030_031_002）；恢复幂等（仅 `deleted=1` 可恢复，否则报 001）。彻底删除独立权限 + 引用守卫（`hasActiveReference` 报 003）。CUSTOMER/SUPPLIER 等系统侧 handler 的 Service 方法显式 `@DataPermission(enable=false)`（回收站是管理视图，租户条件照加）。恢复/彻底删除加 `@LogRecord`。菜单落系统管理→回收站（id 10900）+ 3 权限码。

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] `RecycleBinHandler` SPI 接口（getBizType / pageDeleted / validateRestore / restore / hasActiveReference / purge）落在 `common/recycle` 子包
- [ ] `RecycleBinHandlerRegistry` 启动收集所有 Bean，按 bizType 路由；未注册类型调接口报 1_030_031_000
- [ ] `RecycleItemRespVO`（bizType/bizId/displayName/deletedBy/deletedTime）/ `RecyclePageReqVO`（bizType 必填）
- [ ] Controller 3 端点：`GET /lunaerp/common/recycle/page`（query）/ `PUT .../restore`（body: bizType,bizId）/ `DELETE .../purge`（bizType,bizId），权限码 `lunaerp:common:recycle:query/restore/purge`
- [ ] CUSTOMER handler 落在客户业务包（含联系人级联复活）；查询/复活/物理删除 Mapper 走手写 XML（显式 `deleted=1 AND tenant_id=?`）
- [ ] 恢复规则：主数据恢复后 `status=INACTIVE`；级联子表父→子批量 `deleted=0`；引用阻断（报 002 按依赖链列出需先恢复对象名称）；幂等（仅 `deleted=1` 可恢复，否则报 001）
- [ ] 彻底删除：独立权限码 + `hasActiveReference` 引用守卫（报 003）；不做自动清空、不做批量彻底删除（一期逐条）
- [ ] CUSTOMER handler Service 方法显式 `@DataPermission(enable=false)`（租户条件照加）
- [ ] 恢复/彻底删除加 `@LogRecord`
- [ ] 菜单 SQL：系统管理→回收站（id 10900，path `recycle-bin`，挂芋道系统管理目录，随系统管理模块开通不参与模块套餐网关判定）+ 3 按钮权限码（可重复执行）
- [ ] 前端 `views/lunaerp/common/recycle/index.vue` 按 bizType 分 tab，列：名称/单号、删除人、删除时间，操作：恢复/彻底删除；api 客户端 `api/lunaerp/common/recycle/index.ts`
- [ ] 测试：恢复（含级联/引用阻断/幂等）、彻底删除引用守卫、并发条件更新 SQL 带 `deleted=1 AND tenant_id=?` 影响行数 0 即报错、租户隔离、横向越权（他租户 bizId）必测
