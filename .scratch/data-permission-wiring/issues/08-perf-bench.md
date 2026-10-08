# 08: perf 灌数实证 + EXPLAIN 断言 + 纳入 16 号 perf 清单

**What to build:** 5 表各灌 1 万行（双租户 9000:1000、含 5% 软删，16 号 §2.2 口径），受限档与 ALL 档各连续 100 次 P95<1s；EXPLAIN 见 index/bitmap index scan，seq scan 即失败。脚本纳入 `luna-srv/script/acceptance/perf/` 并加进 16 号 perf 清单。可与 03/04/05/06 并行开工（只需 01 索引 + 02 customizer/角色基线就位）。

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] 5 表各灌 1 万行：双租户 9000:1000、含 5% 软删（16 号 §2.2 口径）；灌数脚本纳入 `luna-srv/script/acceptance/perf/`
- [ ] 受限档（DEPT_AND_CHILD / DEPT_ONLY / SELF）各连续 100 次 list/detail 查询 P95<1s
- [ ] ALL 档连续 100 次查询 P95<1s（不追加谓词，现有查询计划不变）
- [ ] EXPLAIN 见 index scan / bitmap index scan；受限档 BitmapOr 合并两索引；seq scan 即失败
- [ ] 调岗切面批量更新走 `(tenant_id, salesperson_user_id)` 索引，万级数据量毫秒级（与 04 联动验证）
- [ ] 脚本加入 16 号 perf 脚本清单 `luna-srv/script/acceptance/perf/`，纳入 G2 验收 gate
- [ ] 报价表 perf 用例随 05 票建表后补跑，本票只对客户/合同/发货/联系人 4 表实证
