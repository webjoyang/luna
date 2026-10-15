## Agent skills

### Issue tracker

问题（issue）以 Markdown 文件形式跟踪在 `.scratch/<feature>/` 下。见 `docs/agents/issue-tracker.md`。
- 工单范围守卫：纯前端或纯后端工单若因联调需要扩展对侧契约，必须在实现前显式声明向前兼容扩展点或增补 issue，严禁静默进行未经声明的跨端范围蔓延（Scope Creep）。跨模块/跨工单实体守卫同理：严禁在特性工单中静默修改归属于后续 Issue（如未到开发阶段的关联域实体或表结构）的主实体，必须通过安全空值兜底、视图快照或显式前置依赖扩展，杜绝无序跨域蔓延。

### Triage labels

状态标签映射：`needs-triage`→`bug:triage`、`needs-info`→`needs-info`、`ready-for-agent`→`ready-for-agent`、`ready-for-human`→`dev:todo`、`wontfix`→`wontfix`；已完成闭环工单使用 `closed`（已开发完结）/`resolved`（已决）。见 `docs/agents/triage-labels.md`。

### Domain docs

多上下文布局：根目录 `GLOSSARY-MAP.md` 路由到 `docs/contexts/` 下各上下文的 `GLOSSARY.md`（shared + baseinfo/business/production/stock/sample/finance）；ADR 在 `docs/adr/` 全局连续编号。见 `docs/agents/domain.md`。

### Coding standards

开发规约与 Reviewer 审查基准（集合空安全、可空日期相交、防 N+1 批量快照、只读聚合、单测匹配、Wrapper 链式调用、前端权限与错误码）：见 `docs/agents/coding-standards.md`。

### 异步任务与工具经济性 (Zero-Poll 纪律)

系统具备反应式自动唤醒（Reactive Wakeup）。派发后台耗时任务（如 Maven 测试、构建命令）或通过 `invoke_subagent` 派发并行 Reviewer 子代理后，**严禁在会话中使用 `manage_task(status)` 或 `manage_subagents(list)` 进行高频自旋轮询，亦严禁额外调用 `schedule` 挂载短时定时器**。派发后应立即停调工具并结束当前轮次，静待系统消息通知唤醒，消除无效 token 消耗与进程挂起。

### 代码检索与命令行管道安全

- 混合工作区文件定标优先使用 `git ls-files "*<keyword>*"`，严禁未排除 `node_modules` / `target` 进行递归深搜。
- 内容/代码符号检索一律使用跨平台原生的 `git grep "<pattern>"`，严禁在 PowerShell 下使用 `xargs` / Linux `grep` 管道；混合工作区根目录无 `src/`，检索时必须显式指定子项目路径（如 `git grep "<pattern>" luna-ui/ luna-srv/`）。
- 严禁在 PowerShell 下将 `vue-tsc`、`mvn` 等长生命周期或海量输出的命令挂接管道传递给 `Select-String`（子进程标准流缓冲会导致进程假死 hang 住）。

### 以上文档请尽量用中文编写

## 前端项目
- 在luna-ui项目中
- 技术实现：Vue3 + TypeScript + ElementPlus
- 目录导航：业务视图集中在 `src/views/lunaerp-elmui/`（Element Plus 版），接口集中在 `src/api/lunaerp-elmui/`
- 类型与语法检查：当前项目全量 `pnpm ts:check` 存在历史存量错误，严禁在单个特性工单开发中无差别调用全量 `vue-tsc` 进行阻塞检查；优先通过针对当前开发目录的 `eslint`、`prettier`、`pnpm lint:perms <path>` 以及 `vitest` 实现单模块质量闭环。
- 单测执行：`Cwd` 必须切换至 `luna-ui` 目录，直接执行 `pnpm test <path>`（如 `pnpm test src/views/lunaerp-elmui/production/material-check/__tests__/material-check.spec.ts`）；仓库根目录非 monorepo workspace，勿使用 `--filter`。
- Vitest 纯 Node 环境守卫：`vitest.config.ts` 采用纯 Node 环境且未加载 Vue 编译插件与 JSDOM，单测仅覆盖纯 TS 领域模型（`model/*.ts`）与工具函数，严禁在单测中直接 import `.vue` 组件；若单测被测模块引用了 `@/config/axios`，必须在测试文件顶层显式声明 `vi.mock('@/config/axios', ...)`，防止级联执行路由 `createWebHistory` 触发 `window is not defined` 报错。

## 后端项目
- 在luna-srv项目中
- 技术实现：springboot + postgresql +  MyBatis Plus + Redis
- 控制器与类文件定位：业务控制器统一位于 `com.luna.lunasrv.module.lunaerp.controller.<domain>`（无 `admin/` 中间层，区别于底座 yudao 系统管理模块的 `controller/admin/`）；定位 Controller/Service/VO/DTO 一律使用 `git ls-files "*<Keyword>*"`，严禁凭经验拼接类路径。
- 单测执行：`mvn test "-Dtest=TargetTest"` 或指定模块 `mvn test -pl lunasrv-module-lunaerp "-Dtest=TargetTest"`（多类测试在 PowerShell 下务必使用双引号包裹 `"-Dtest=TestA,TestB"`）
- 框架依赖刷新：修改或依赖刚刚调整过的 `lunasrv-framework` 基础组件时，需先执行 `mvn -pl lunasrv-framework/<module-name> install -DskipTests`（如 `lunasrv-spring-boot-starter-mybatis`）将最新 jar 安装至本地仓库，避免子模块 `-pl` 测试时读取 `~/.m2` 旧快照。
- 数据库脚本与发号校验规约：35 个增量脚本的执行状态见 `luna-srv/sql/postgresql/README.md`；PostgreSQL 幂等发号见 `docs/agents/coding-standards.md` 第 8 节；按日取号顺延重试见第 9 节；复合更新 DTO 校验与单测打桩边界见第 10 节。