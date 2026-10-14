## Agent skills

### Issue tracker

问题（issue）以 Markdown 文件形式跟踪在 `.scratch/<feature>/` 下。见 `docs/agents/issue-tracker.md`。

### Triage labels

状态标签映射：`needs-triage`→`bug:triage`、`needs-info`→`needs-info`、`ready-for-agent`→`ready-for-agent`、`ready-for-human`→`dev:todo`、`wontfix`→`wontfix`；已完成闭环工单使用 `closed`（已开发完结）/`resolved`（已决）。见 `docs/agents/triage-labels.md`。

### Domain docs

多上下文布局：根目录 `GLOSSARY-MAP.md` 路由到 `docs/contexts/` 下各上下文的 `GLOSSARY.md`（shared + baseinfo/business/production/stock/sample/finance）；ADR 在 `docs/adr/` 全局连续编号。见 `docs/agents/domain.md`。

### 以上文档请尽量用中文编写

## 前端项目
- 在luna-ui项目中
- 技术实现：Vue3 + TypeScript + ElementPlus

## 后端项目
- 在luna-srv项目中
- 技术实现：springboot + postgresql +  MyBatis Plus + Redis
- 单测执行：`mvn test "-Dtest=TargetTest"` 或指定模块 `mvn test -pl lunasrv-module-lunaerp "-Dtest=TargetTest"`（多类测试在 PowerShell 下务必使用双引号包裹 `"-Dtest=TestA,TestB"`）