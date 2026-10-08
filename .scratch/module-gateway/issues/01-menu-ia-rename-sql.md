# 01: 菜单 IA 迁移 SQL（一级目录改名 + 幂等）

**What to build:** 执行 `lunaerp-*.sql` 迁移脚本后，5 个 lunaerp 一级目录的 `name` 更新为 Q18 目标分组名——基本信息→基础信息、业务管理→销售、生产管理→生产、库存管理→库存、样品管理→样品；`id`/`parent_id`/`path` 保持不变（一期不改 `path` 以避免前端组件路径批量变更）。脚本采用 `ON CONFLICT DO UPDATE`（或等价幂等模式）可重复执行。租户用户刷新侧边栏即见新分组名。

**Blocked by:** None（可立即开始）

**Status:** ready-for-agent

- [ ] 5 个一级目录（10000 基础信息 / 10100 销售 / 10200 生产 / 10300 库存 / 10600 样品）`name` 迁移完成，`id`/`parent_id`/`path` 不变
- [ ] 迁移脚本幂等可重复执行（`ON CONFLICT DO UPDATE` 或事务内 DELETE 再 INSERT）
- [ ] 前端刷新侧边栏显示新分组名，子菜单归属与层级未受影响
- [ ] 二级菜单/按钮的归属关系与 spec「菜单 IA 迁移映射表」一致（纱线采购、发染记录归生产；供应商信息、入库管理保持原位）
