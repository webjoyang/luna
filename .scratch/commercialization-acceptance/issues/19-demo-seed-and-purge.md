# 19: 可选演示数据 demo-seed + 一键 purge 清理

**What to build:** 演示培训场景的独立可选数据包：执行 demo-seed 后系统里有一客户、一供应商、两个产品与若干合同/单据可直接跑演示；执行 purge 一键清干净回到出厂默认状态。默认安装绝不灌这套数据，文档红字警示演示库不得当正式库交付。

**Blocked by:** 01（可与主线全程并行，不阻塞任何 gate）

**Status:** ready-for-agent

- [ ] `luna-srv/sql/postgresql/demo/lunaerp-demo-seed.sql`：一客户、一供应商、两产品 + 若干合同/发货/对账等单据，可支撑售前演示走通主链关键节点
- [ ] `lunaerp-demo-purge.sql`：一键清掉全部演示数据并回到默认 seed 状态，不触碰菜单/字典/角色等出厂数据
- [ ] seed 与 purge 均幂等：重复执行不报错；purge 后再 seed 结果与首次一致
- [ ] install 总入口不引用 demo-seed；install.md 红字警示演示库禁止当正式库交付（与 05 号票联动核对）
