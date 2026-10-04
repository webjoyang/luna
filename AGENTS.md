## Agent skills

### Issue tracker

Issues are tracked as markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical roles mapped to repo labels: `needs-triage`→`bug:triage`, `needs-info`→`needs-info`, `ready-for-agent`→`ready-for-agent`, `ready-for-human`→`dev:todo`, `wontfix`→`wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Multi-context layout: root `CONTEXT-MAP.md` routes to per-context `CONTEXT.md` files under `docs/contexts/` (shared + baseinfo/business/production/stock); ADRs stay globally numbered in `docs/adr/`. See `docs/agents/domain.md`.

### 以上文档请尽量用中文编写

## 前端项目
- 在luna-ui项目中
- 技术实现：Vue3 + TypeScript + ElementPlus

## 后端项目
- 在luna-srv项目中
- 技术实现：springboot + postgresql +  MyBatis Plus + Redis