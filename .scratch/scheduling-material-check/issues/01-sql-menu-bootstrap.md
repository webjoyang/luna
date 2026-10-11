# 01: SQL 基建——10510/10520 菜单与角色授权

**What to build:** 新增 `lunaerp-production-scheduling-material-check.sql`，仅菜单与角色授权（零建表、零加列、零字典、零序列）：10510 排产中心（sort 12，权限码 `lunaerp:production:weaving-order-task:query`，挂 10200 生产管理下，组件 scheduling/index，图标 ep:calendar）、10520 齐套检查（sort 13，权限码 `lunaerp:production:material-check:query`，组件 material-check/index，图标 ep:files）。超管角色（super_admin/admin，角色 1/2）按既有 system_role_menu 模式授权。排产中心不新增按钮位（页内派工/改期/转机复用现有 weaving-order-task:create/update 权限组合）；齐套检查纯只读无按钮位；排缸日历不占菜单不加权限。

**Blocked by:** None (can start immediately)

**Status:** resolved

**参考**：本目录上级 spec.md §8（菜单与权限）

- [x] SQL 执行后超管角色可见并进入两菜单；无该权限的角色菜单不可见
- [x] 零 DDL：无建表/加列/字典/序列，命名遵循 `lunaerp-production-*.sql` 既有惯例

## Comments
- **SQL 基建与角色授权落地**（Commit: `9f9a233a`）：
  - 新增 SQL 脚本 `luna-srv/sql/postgresql/lunaerp-production-scheduling-material-check.sql`。
  - 注册菜单 10510（排产中心，sort 12，parent_id 10200 生产管理，组件 `lunaerp-elmui/production/scheduling/index`，图标 `ep:calendar`，权限码 `lunaerp:production:weaving-order-task:query`）。
  - 注册菜单 10520（齐套检查，sort 13，parent_id 10200 生产管理，组件 `lunaerp-elmui/production/material-check/index`，图标 `ep:files`，权限码 `lunaerp:production:material-check:query`）。
  - 超管与管理员角色（`super_admin`, `admin` / `role_id IN (1, 2)`）完成角色菜单授权（`system_role_menu`），包含 `system_role_menu_seq` 兼容性保护。
  - 保证脚本幂等可重复执行（先 `DELETE` 再 `INSERT`，关联授权 `ON CONFLICT DO NOTHING`）。
  - 遵循零 DDL 原则：无建表、无加列、无字典、无序列。
