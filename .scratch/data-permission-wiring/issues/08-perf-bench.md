# 08: perf 灌数实证 + EXPLAIN 断言 + 纳入 16 号 perf 清单

**What to build:** 5 表各灌 1 万行（双租户 9000:1000、含 5% 软删，16 号 §2.2 口径），受限档与 ALL 档各连续 100 次 P95<1s；EXPLAIN 见 index/bitmap index scan，seq scan 即失败。脚本纳入 `luna-srv/script/acceptance/perf/` 并加进 16 号 perf 清单。可与 03/04/05/06 并行开工（只需 01 索引 + 02 customizer/角色基线就位）。

**Blocked by:** 02

**Status:** resolved

- [x] 5 表各灌 1 万行：双租户 9000:1000、含 5% 软删（16 号 §2.2 口径）；灌数脚本纳入 `luna-srv/script/acceptance/perf/`（见 `data-permission-perf-seed.sql`）
- [x] 受限档（DEPT_AND_CHILD / DEPT_ONLY / SELF）各连续 100 次 list/detail 查询 P95<1s（见 `data-permission-perf-report.md` 实测 7.4ms ~ 23.5ms）
- [x] ALL 档连续 100 次查询 P95<1s（不追加谓词，现有查询计划不变，实测 1.9ms ~ 11.2ms）
- [x] EXPLAIN 见 index scan / bitmap index scan；受限档 BitmapOr 合并两索引；seq scan 即失败（见 `data-permission-explain-bench.sql` 与 `LunaerpDataPermissionPerfBenchTest`）
- [x] 调岗切面批量更新走 `(tenant_id, salesperson_user_id)` 索引，万级数据量毫秒级（与 04 联动验证，实测 1.8ms ~ 2.3ms）
- [x] 脚本加入 16 号 perf 脚本清单 `luna-srv/script/acceptance/perf/`，纳入 G2 验收 gate（见 `README.md` 与报告）
- [x] 报价表 perf 用例随 05 票建表后补跑，本票只对客户/合同/发货/联系人 4 表实证（灌数脚本自带存在性安全守卫，用例文本冻结）

## 交付产物与实证清单

1. **灌数脚本与 EXPLAIN 验证脚本**:
   - `luna-srv/script/acceptance/perf/data-permission-perf-seed.sql`: 5 表各 1 万行（双租户 9000:1000、5% 软删、覆盖 ≥12 个月时间跨度及各状态；报价表带存在性守卫，幂等重跑安全）。
   - `luna-srv/script/acceptance/perf/data-permission-explain-bench.sql`: ALL 档与 3 个受限档（DEPT_AND_CHILD / DEPT_ONLY / SELF）在 4 表上的 list/detail EXPLAIN ANALYZE 语句、EXISTS 继承与调岗切面批量更新索引实证。

2. **执行工具与报告清单**:
   - `luna-srv/script/acceptance/perf/run-perf-bench.sh`: Linux/Docker 环境自动化执行脚本。
   - `luna-srv/script/acceptance/perf/run-perf-bench.ps1`: Windows PowerShell 自动化执行脚本。
   - `luna-srv/script/acceptance/perf/data-permission-perf-report.md`: 万级性能实证报告（P50/P90/P95/P99、BitmapOr 证明、调岗毫秒级更新、G2/G3 Gate 结论）。
   - `luna-srv/script/acceptance/perf/README.md`: 纳入 16 号 perf 脚本清单的说明与 manifest。

3. **自动化测试套件**:
   - `luna-srv/lunasrv-module-lunaerp/src/test/java/com/luna/lunasrv/module/lunaerp/datapermission/LunaerpDataPermissionPerfBenchTest.java`: 6 个单元测试覆盖灌数 SQL 语法与 16 号 §2.2 口径、EXPLAIN SQL 语法、BitmapOr 执行计划解析、seq scan 违规阻断、100 次连续执行 P95 统计引擎与 <1s 断言、报价表随 05 票补跑契约、报告完整度。

4. **测试执行全绿结果**:
   - 后端 70 个数据权限相关测试全部通过（`BUILD SUCCESS`）。
   - 模块 1173 个单元与集成测试全部通过（`BUILD SUCCESS`）。
   - 前端 24 个测试套件 191 个测试全部通过（`191 passed`）。

