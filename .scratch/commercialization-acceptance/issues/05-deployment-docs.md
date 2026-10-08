# 05: 部署/升级/验收操作三份文档

**What to build:** 交付工程师拿到三份文档即可独立完成私有化安装、版本升级和半天内的验收执行，不需要再问研发：install.md 覆盖 compose 与裸机两案，upgrade.md 固化"备份→重跑→校验"流程，acceptance-runbook.md 是验收当天的照做手册。

**Blocked by:** 04

**Status:** ready-for-agent

- [ ] `docs/deployment/install.md`：最低/推荐机器配置、端口、域名与证书要求（门户/H5 必须 HTTPS 或 localhost）；compose 部署案与裸机部署案（自备 PG15/Redis7/JDK17/nginx，含 JDK 与 node 构建步骤、参数表）；备份入口
- [ ] `docs/deployment/upgrade.md`：升级前全库备份命令 → 停服窗口 → 按 install 序重跑幂等脚本 → 启动 → 跑 §7.4 校验冒烟 → 版本号核对；明示**不提供 down 脚本、回滚=恢复备份**
- [ ] `docs/deployment/acceptance-runbook.md`：重建验收库、perf 灌数与测 P95、四链冒烟命令、越权矩阵造人脚本入口、脱敏抽查登录账号表；半天内可照册跑完
- [ ] 安装文档含红字警示：19 号票 demo-seed 为演示培训用，演示库禁止当正式库交付
- [ ] 三份文档中的命令与 01/04/06/07/13 实际脚本逐条核对可执行（文档与脚本同批评审）
