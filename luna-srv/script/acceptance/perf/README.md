# LunaERP 销售归属链 4 档数据范围万级性能基准测试与 EXPLAIN 实证目录

本目录纳入商业化验收清单（16 号 spec §2.2 与 §7.1）perf 脚本清单，用于销售归属链 5 表在行级数据范围开启后的万级数据量性能验证与 G2/G3 Gate 验收。

## 1. 脚本清单 (Manifest)

| 文件名 | 类型 | 说明 |
|---|---|---|
| `data-permission-perf-seed.sql` | SQL 灌数脚本 | 5 表各灌 1 万行（双租户 9000:1000、含 5% 软删，覆盖各状态与 ≥12 个月时间跨度；报价表带存在性守卫） |
| `data-permission-explain-bench.sql` | SQL 验证脚本 | 4 档位（ALL / DEPT_AND_CHILD / DEPT_ONLY / SELF）各表 EXPLAIN (ANALYZE, BUFFERS) 语句与调岗切面批量更新验证 |
| `data-permission-perf-report.md` | Markdown 验收报告 | 连续 100 次 list/detail 延迟测量结果（P50/P90/P95/P99）、EXPLAIN BitmapOr 证明与调岗毫秒级更新报告 |
| `run-perf-bench.sh` | Shell 脚本 | Linux / Docker 环境下的一键灌数、EXPLAIN 检查与报告校验工具 |
| `run-perf-bench.ps1` | PowerShell 脚本 | Windows 环境下一键执行工具 |

## 2. 验收口径对照 (16 号 §2.2 与 Issue 08)

1. **万级数据与双租户分布**：
   - 每表灌 10,000 行：租户 1 占 9,000 行，租户 2 占 1,000 行。
   - 软删除行比例严格为 5.0%（500 行 `deleted = 1`）。
   - 时间跨度均匀覆盖 12 个月（365 天）。
2. **延迟与通过线**：
   - 连续 100 次请求，丢弃第 1 次预热，后 99 次取 P95。
   - 通过标准：**P95 < 1000ms**。
   - 调岗切面同事务批量更新走 `(tenant_id, salesperson_user_id)` 索引，耗时在毫秒级（< 10ms）。
3. **EXPLAIN 执行计划与索引**：
   - 受限档（DEPT_AND_CHILD / DEPT_ONLY）：`BitmapOr` 成功合并 `idx_lb_xxx_dp_dept` 与 `idx_lb_xxx_dp_user` 两个部分索引。
   - SELF 档：直接走 `idx_lb_xxx_dp_user` 部分索引。
   - ALL 档：原有查询计划不受影响。
   - 严禁出现全表扫描：`Seq Scan` 出现即判定为失败。
4. **报价表说明**：
   - 报价表随 05 票建表后同批补跑，本脚本已内置其灌数与更新逻辑（通过 DO 块检测表存在性安全守卫）。

## 3. 执行方式

### 方式 A：通过 Maven JUnit 自动测试
```bash
cd luna-srv
mvn test -Dtest=LunaerpDataPermissionPerfBenchTest -pl lunasrv-module-lunaerp
```

### 方式 B：通过 Shell 脚本（有 PostgreSQL 客户端环境）
```bash
export DB_HOST=127.0.0.1
export DB_PORT=5432
export DB_NAME=ruoyi-vue-pro
export DB_USER=postgres
export DB_PASSWORD=postgres
./luna-srv/script/acceptance/perf/run-perf-bench.sh
```

### 方式 C：通过 PowerShell 脚本（Windows）
```powershell
.\luna-srv\script\acceptance\perf\run-perf-bench.ps1
```
