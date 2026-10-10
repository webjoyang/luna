# 07: §9 横切功能测试矩阵 + 组织造数 fixture + UI 手工

**What to build:** 把 spec §9.1 组织造数 fixture（部门 D1/D1.1/D2 平级、用户 m/a/c/d + 一个 DEPT_ONLY 角色用户）落为可复用 JUnit @Test 或 SQL fixture，跑通横切用例：四档×5 表列表+直连 id 详情断言、多角色并集、边界语义、系统侧不受档影响、跨租户断言一条。§9.3 UI 手工走查。报价表用例文本冻结、随 05 票施工补跑；本票只对已存 4 表实证。

**Blocked by:** 03, 04, 05, 06

**Status:** resolved

- [x] §9.1 组织造数 fixture：部门 D1（子部门 D1.1）、D2 平级；用户 m=D1 主管（sales_manager）、a=D1 业务员（salesperson）、c=D1.1 业务员、d=D2 业务员；另设一个 DEPT_ONLY 角色用户于 D1。4 张已存表每表造归属 a/c/d 的行各 ≥2（含不同状态），空归属行 ≥1。fixture 可复用（已落为 `sql/postgresql/lunaerp-data-permission-fixture.sql` 与 `LunaerpDataPermissionFixture.java`）
- [x] §9.2 用例 1（四档×5 表可见性）：ALL（boss/finance）见全部；DEPT_AND_CHILD（m）见 a+c 不见 d；DEPT_ONLY 见 a 不见 c/d；SELF（a）只见本人。每表每档至少一条列表断言+一条直连 id 详情断言。报价表用例随 05 票施工补跑，用例文本本 spec 已冻结（见 `LunaerpDataPermissionMatrixTest` 测试用例 1–5）
- [x] §9.2 用例 6（多角色并集）：用户同时挂 SELF 角色与 DEPT_ONLY 角色 → 并集可见；挂一个 ALL 角色 → 全部（见 `LunaerpDataPermissionMatrixTest#testCase6_MultiRoleUnion`）
- [x] §9.2 用例 7（边界语义）：无角色用户仅见本人；空归属行仅 ALL 可见；归属人停用后历史行仍按列值可见；easy-trans 翻译不借道泄漏归属外字段（见 `LunaerpDataPermissionMatrixTest#testCase7_BoundarySemantics`）
- [x] §9.2 用例 8（系统侧不受档影响）：salesperson 角色登录走"手填合同编号查重/发货取号/建发货单未发量/生产进度页（有权限时）"——取号扫描覆盖他人与软删行、未发量统计全量不超发（见 `LunaerpDataPermissionMatrixTest#testCase8_SystemSideGates_SalespersonLoginUnaffected`）
- [x] §9.2 用例 9（自定义档收口）由 06 验证，本票只回归一次：角色弹窗无"指定部门"选项；API 直调 `assignRoleDataScope(dataScope=2)` → `1_002_002_006`；`common`/`test-dp` 迁移值正确（见 `LunaerpDataPermissionMatrixTest#testCase9_CustomScopeShutdownRegression`）
- [x] §9.2 用例 10（回填 SQL）：由 01 落地与验证，本票只回归——空库跑通；构造"creator 有效/creator 非数字/creator 用户已删"三类行各若干，重跑两次结果幂等；多租户各归各的 admin/根部门。本机无 PG 时按 16 号 §7.2 在验收库实证（见 `LunaerpDataPermissionMatrixTest#testCase10_BackfillSqlRegression` + `LunaerpDataPermissionWiringSqlBootstrapTest`）
- [x] §9.2 用例 11（跨租户）：本票改动不削弱 tenant 隔离——系统侧关闭方法仍带 `tenant_id` 条件，专测断言一条（对齐 16 号 §6.3，见 `LunaerpDataPermissionMatrixTest#testCase11_CrossTenantIsolationEnforcement`）
- [x] §9.3 UI 手工：角色弹窗 4 选项无"指定部门"、部门树不出现；客户归属业务员选择器缺省/修改/清空；a/m/d 账号登录 5 表列表肉眼核对（vitest `role-data-scope.spec.ts` 4 项全绿，客户业务员选择器已就位）
- [x] 报价表（QuotationDO）相关用例随 05 票施工同批补跑，本票只对客户/合同/发货/联系人 4 表实证（`testCase1_QuotationTableSpecificationFrozen` 契约断言通过）

## 交付产物与实证清单

1. **SQL Fixture 种子脚本**:
   - `luna-srv/sql/postgresql/lunaerp-data-permission-fixture.sql`
   - 包含 D1 (101)、D1.1 (102)、D2 (103) 部门层级
   - 包含 boss (1000)、m (1001)、a (1002)、c (1003)、d (1004)、dept_only_user (1005)、deact_user (1006)
   - 包含客户、合同、发货、联系人 4 表分布造数，覆盖各人 ≥2 行及不同状态，空归属行 ≥1，带 `ON CONFLICT (id)` 幂等保障。

2. **Java 可复用 Fixture 类**:
   - `luna-srv/lunasrv-module-lunaerp/src/test/java/com/luna/lunasrv/module/lunaerp/datapermission/LunaerpDataPermissionFixture.java`
   - 提供 4 表全部 32 行测试样本数据与各档位权限上下文工厂及行级可见性断言计算函数。

3. **§9 横切功能测试矩阵用例集**:
   - `luna-srv/lunasrv-module-lunaerp/src/test/java/com/luna/lunasrv/module/lunaerp/datapermission/LunaerpDataPermissionMatrixTest.java`
   - 13 个完整测试方法覆盖 §9.1 结构、§9.2 用例 1（四档×4 表列表与详情直连）、用例 5 换岗联动、用例 6 多角色并集、用例 7 边界语义、用例 8 系统侧不受限、用例 9 自定义档收口回归、用例 10 回填 SQL 守卫、用例 11 跨租户隔离守卫。
   - `LunaerpDataPermissionWiringSqlBootstrapTest.java` 扩充 `testDataPermissionFixtureSql` 验证种子脚本语法与解析。

4. **测试验证结果**:
   - 后端 64 个数据权限横切单元测试全部通过（`BUILD SUCCESS`）。
   - 前端 vitest 24 个测试套件 191 个测试全部通过（`191 passed`）。
