# 06: 4 档收口（UI 角色弹窗过滤 + system API 拒绝 + 错误码 `1_002_002_006`）

**What to build:** 自定义档（value=2）四层收口——前端白名单过滤 value=2 不可见；system 模块 `assignRoleDataScope`（或 Controller 入参校验层）拒绝 `dataScope=2` 并新增错误码 `1_002_002_006`；底座枚举/分支代码保留不删；存量 `common`→SELF、`test-dp`→ALL 迁移由 01 完成。角色弹窗 4 选项无"指定部门"，部门树卡片永不触发。

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] luna-ui `RoleDataPermissionForm.vue`：下拉不再直接全量渲染字典，改为对 `getIntDictOptions(SYSTEM_DATA_SCOPE)` 结果过滤 `value !== 2`（白名单常量 `[1,4,3,5]`，顺序 = 全部/本部门及以下/本部门/本人）
- [ ] 部门树卡片现有 `v-if dataScope===DEPT_CUSTOM` 条件随之永不触发，可不删模板（保留底座最小改动）
- [ ] system 模块 `PermissionServiceImpl#assignRoleDataScope`（或 Controller 入参校验层）拒绝 `dataScope=2`：新增错误码 `1_002_002_006` `ROLE_DATA_SCOPE_CUSTOM_DISABLED`，文案"数据范围【自定义部门】已停用，请选择全部/本部门及以下/本部门/本人"
- [ ] 错误码落 system 模块 `ErrorCodeConstants` 角色段（现有用到 005，本票占 006），登记进 lunaerp 段不占用（1-030-041 段保持空闲）
- [ ] `DataScopeEnum.DEPT_CUSTOM` 枚举与计算分支代码保留不删（底座最小改动；存量库可能瞬时存在 2）
- [ ] 字典 `system_dict_data` id=35（value=2 指定部门）保留不删，底座升级兼容；前端过滤不可见
- [ ] §9.2 用例 9（自定义档收口）通过：角色弹窗无"指定部门"选项；API 直调 `assignRoleDataScope(dataScope=2)` → `1_002_002_006`；`common`/`test-dp` 迁移值正确（迁移在 01 落地，本票只验）
- [ ] §9.3 UI 手工：角色弹窗 4 选项无"指定部门"、部门树不出现；角色列表 `dataScope` 标签对 value=2 的文案在存量迁移后无数据可显，不做特殊处理
- [ ] 零新字典（字典 id=35 留库）、零新菜单/按钮
