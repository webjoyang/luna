## Agent skills

### Issue tracker

问题（issue）以 Markdown 文件形式跟踪在 `.scratch/<feature>/` 下。见 `docs/agents/issue-tracker.md`。

- 工单范围守卫：坚持 Spec 最小契约原则。
  - **【契约最小化】**：纯前端或纯后端工单若因联调需要扩展对侧契约，必须在实现前显式声明向前兼容扩展点或增补 issue，严禁静默进行未经声明的跨端范围蔓延（Scope Creep）；严禁在特性工单中顺带扩展未在 Spec 声明的查询维度（如非必须的综合搜索关键词扩展）或修补非本工单归属的遗留字段；
  - **【详情不入列表】**：Spec 声明在详情页（Detail/Workspace）的只读拓展字段（如审批伴生的下游单据号/状态、审批历史链路、时序时间线等），严禁顺带扩展至分页列表 VO 中，亦严禁在分页查询实现中发起额外的批量关联查询（如 `selectBatchIds`），杜绝非必要跨域蔓延与查询负载；
  - **【留痕严格归位】**：详情页与工作台表单及视图仅渲染本工单 Spec 明确声明的字段与留痕（如财务确认仅渲染 `postedBy/postedAt`），严禁超前或附带渲染归属于其他工单的生命周期留痕字段（如 `arrivedBy/At`、`cancelledBy/At`）；
  - **【跨域实体守卫】**：严禁在特性工单中静默修改归属于后续 Issue（如未到开发阶段的关联域实体或表结构）的主实体，必须通过安全空值兜底、视图快照或显式前置依赖扩展，杜绝无序跨域蔓延。
- 特性开发前置工作区干净度检查：开启新工单实现前，必须先执行 `git status -s` 确认工作区无无关改动；若存在前序环境同步、数据库脚本重跑等遗留文件，必须先独立 commit 或 stash，严禁将历史脏工作区带入特性工单引发 Diff 审查与范围蔓延判定噪声。

### Triage labels

状态标签映射：`needs-triage`→`bug:triage`、`needs-info`→`needs-info`、`ready-for-agent`→`ready-for-agent`、`ready-for-human`→`dev:todo`、`wontfix`→`wontfix`；已完成闭环工单使用 `closed`（已开发完结）/`resolved`（已决）。见 `docs/agents/triage-labels.md`。

### Domain docs

多上下文布局：根目录 `GLOSSARY-MAP.md` 路由到 `docs/contexts/` 下各上下文的 `GLOSSARY.md`（shared + baseinfo/business/production/stock/sample/finance）；ADR 在 `docs/adr/` 全局连续编号。见 `docs/agents/domain.md`。

### Coding standards

开发规约与 Reviewer 审查基准（集合空安全、可空日期相交、防 N+1 批量快照、只读聚合、单测匹配、Wrapper 链式调用、前端权限与错误码）：见 `docs/agents/coding-standards.md`。

### 异步任务与工具经济性 (Zero-Poll &amp; Zero-Tool Yield 纪律)

系统具备反应式自动唤醒（Reactive Wakeup）。派发后台耗时任务（如 Maven 测试、构建命令）或通过 `invoke_subagent` 派发并行 Reviewer 子代理后：

1. **Zero-Tool Yield 轮次让渡**：派发后本轮次必须**仅输出纯文本确认或直接停止调用工具（不得附加任何辅助工具调用）以交出控制权**。会话中**严禁使用 `manage_task(status)` 或 `manage_subagents(list)` 自旋轮询，亦严禁额外调用 `schedule` 挂载短时定时器**。系统在子代理或任务结束时会自动注入消息唤醒，轮次内调工具会阻断系统的后台反应式睡眠并引发死循环。
2. **只读审查工作区继承**：通过 `invoke_subagent` 派发 Standards / Spec 等只读性质的 Reviewer 子代理时，一律使用 `Workspace: "inherit"` 继承父工作区，严禁无故开启 `Workspace: "branch"` 产生额外分支与磁盘清理开销。

### 代码检索与命令行管道安全

- 混合工作区文件定标优先使用 `git ls-files "*<keyword>*"`，严禁未排除 `node_modules` / `target` 进行递归深搜。
- 内容/代码符号检索一律使用跨平台原生的 `git grep "<pattern>"`，严禁在 PowerShell 下使用 `xargs` / Linux `grep` 管道；混合工作区根目录无 `src/`，检索时必须显式指定子项目路径（如 `git grep "<pattern>" luna-ui/ luna-srv/`）；特性开发中检索可能包含刚创建未暂存的文件时，必须附加 `--untracked`（如 `git grep --untracked "<pattern>"`），防止漏检。
- 严禁在 PowerShell 下将 `vue-tsc`、`mvn` 等长生命周期或海量输出的命令挂接管道传递给 `Select-String`（子进程标准流缓冲会导致进程假死 hang 住）。
- Windows PowerShell 5.1 语法守卫：命令串联必须使用分号 `;` 分隔或分步独立执行，严禁使用 Bash/PowerShell 7 的 `&&` 连接符（会抛出 InvalidEndOfLine 语法解析错误）。
- 复杂文本与日志过滤：在 Windows PowerShell 5.1 下，针对长日志或 JSON 结构的流式过滤与统计，避免编写含复杂引号转义（如 `$_`）的脆弱命令行管道，优先使用 `node -e "..."` 或在会话 `scratch/` 目录编写一次性 Node.js 脚本。

### 以上文档请尽量用中文编写

## 前端项目

- 在luna-ui项目中
- 技术实现：Vue3 + TypeScript + ElementPlus
- 目录导航：业务视图集中在 `src/views/lunaerp-elmui/`（Element Plus 版），接口集中在 `src/api/lunaerp-elmui/`
- 前端单文件闭环指令：修改文件后直接执行增量检查：
  1. 权限守卫：`pnpm lint:perms <files>`（修改 `.vue` 文件后必跑，根目录 Git pre-commit hook 亦会自动执行拦截）
  2. 格式与风格：针对修改文件精准执行 `eslint` 与 `npx prettier --write <files>`（大文件如 `dict.ts`、`remaining.ts` 严禁整文件格式化，仅局部精细修改）
  3. 单测执行：`Cwd` 切换至 `luna-ui` 目录，直接执行 `pnpm test <spec-path>`（如 `pnpm test src/views/lunaerp-elmui/finance/payment/__tests__/payment.spec.ts`；非 monorepo workspace，勿使用 `--filter`）
  4. 免跑全量：存量历史类型报错存在，特性工单免跑阻塞式的全量 `vue-tsc` / `pnpm ts:check`
- Vitest 纯 Node 环境守卫：`vitest.config.ts` 采用纯 Node 环境且未加载 Vue 编译插件与 JSDOM，单测仅覆盖纯 TS 领域模型（`model/*.ts`）与工具函数，严禁在单测中直接 import `.vue` 组件；若单测被测模块引用了 `@/config/axios`，必须在测试文件顶层显式声明 `vi.mock('@/config/axios', ...)`，防止级联执行路由 `createWebHistory` 触发 `window is not defined` 报错。

## 后端项目

- 在luna-srv项目中
- 技术实现：springboot + postgresql +  MyBatis Plus + Redis
- 控制器与类文件定位：业务控制器统一位于 `com.luna.lunasrv.module.lunaerp.controller.<domain>`（无 `admin/` 中间层，区别于底座 yudao 系统管理模块的 `controller/admin/`）；定位 Controller/Service/VO/DTO 一律使用 `git ls-files "*<Keyword>*"`，严禁凭经验拼接类路径。
- 单测执行：`Cwd` 切换至 `luna-srv` 目录，执行 `mvn test "-Dtest=TargetTest"` 或指定模块 `mvn test -pl lunasrv-module-lunaerp "-Dtest=TargetTest"`（多类测试在 PowerShell 下务必使用双引号包裹 `"-Dtest=TestA,TestB"`）；凡涉及 `finance`、`business` 等域新增/修改 Controller/Mapper/DO/Handler 时，必须捆绑架构守护单测 `"-Dtest=ArchitectureStandardsTest,TargetTest"`，前置自动检验脱敏注解、可空更新策略、跨域 Mapper 直写拦截与 Mapper 纯洁性（接口严禁 `default` 方法，区别于底座 yudao 习惯）。
- 框架依赖刷新：修改或依赖刚刚调整过的 `lunasrv-framework` 基础组件时，需先执行 `mvn -pl lunasrv-framework/<module-name> install -DskipTests`（如 `lunasrv-spring-boot-starter-mybatis`）将最新 jar 安装至本地仓库，避免子模块 `-pl` 测试时读取 `~/.m2` 旧快照。
- 编码安全与实体层级核验：Java/TS 修改 import 严禁大块框选临近 import 粗暴替换，必须单行精细追加/替换；涉及头-行-卷或主子表跨层数据映射时，编码前必须先执行 `view_file` 查阅 DO/VO 物理属性定义，严禁凭经验臆测字段挂载层级（如缸号/匹号/卷号归属）。
- 数据库脚本与领域规约：增量脚本执行状态见 `luna-srv/sql/postgresql/README.md`；PostgreSQL 幂等发号、顺延重试、DTO 校验、行级悲观锁、范围守卫、可空列清空、重构检索、审批流回调单写、伴生单据协同作废、质检中立初始态、跨单位物理量、入库 placement 与跨域记账监听器 Fail-Fast 规约见 `docs/agents/coding-standards.md` 第 8~25 节。



&nbsp;

## 上下文与终端防膨胀纪律（Context Hygiene）

1. **大命令下沉**：编译打包（如 `pnpm build`）、跑完整单测（如 `mvn test`）等长输出任务，一律由 Implementer 子 Agent  中执行并自闭环，主会话只接收单行成功/失败摘要，严禁在主会话直接运行重型构建与测试命令。
2. **无错即静默**：需要捕获结果的命令优先遵循“成功静默、失败截取”范式（通过输出重定向至临时文件并结合退出码，执行成功仅输出单行 `✓ OK`，失败时才提取末尾 30 行错误堆栈）。
3. **CLI 紧凑开关**：主会话与 Agent 运行日常命令强制启用紧凑/静默参数，杜绝冗长文本刷屏：
   - Git 命令必加紧凑参数：`git status -s`、`git log --oneline -n <N>`、`git diff --stat`。
   - Maven 命令启用静默选项：`mvn test -q`。
   - 前端命令启用静默模式：`pnpm <cmd> --silent`，Vitest 采用 `--reporter=basic`。
4. **调用 skill 工具 "implement" 期间**，为防止主会话超出 150k 聪明区与触发有损自动压缩，强制执行以下派发与熔断规则：
   - **两遍试错熔断（Rule of Two Failures）**：单测红灯或编译报错时，在主会话最多只允许尝试修改 1 次；若第 2 次执行依然报错，**严禁在主会话继续盲目试错**，必须立即调用 `invoke_subagent` 派发专职诊断子代理（`diagnosing-bugs` 或 `self`）。主会话仅向子代理传入最小复现单测命令与错误调用栈指针，子代理在独立上下文定位根因并输出精简 Diff 建议后返回，主会话直接应用该方案，杜绝主会话因长日志与重复试错撑爆 150k 上下文聪明区。
   - **跨域探索隔离（Read-Heavy Exploration）**：凡需查阅 &gt;3 个非直接关联文件以探查系统架构/模式，或排查第三方库及未知深层链路时，严禁在主会话直接进行大批量文件阅读；必须派发 `research` 只读子代理（`Workspace: "inherit"`），由其查阅并仅带回关键结论（&lt;200字）或保存至 `.scratch/` 笔记中。
   - **轻量契约派发（Context Pointer 协议）**：派发排错或调研子代理时，严禁在 Prompt 中倾倒整段主会话历史或长日志，严格遵守四要素契约：① 目标问题简述、② 核心源码及异常行号指针（`file:///...`）、③ 唯一确定性复现指令（单测/脚本）、④ 期望输出契约（严格限制输出字数或代码块，禁止擅自修改非目标文件）。
5. **探索与调研下沉**：主会话严禁直接调用 `view_file` 大面积通读业务代码（单会话累积阅读代码不得超过 3 个文件）。需要摸底架构与接口时，必须派发只读 `research` 子 Agent 沉淀到外部 Markdown 文件，主会话只读其摘要或保留路径指针。
6. **派发强制指针化（Context Pointers Only）**：主会话在 `/implement-spec，`主会话派发子 Agent（`invoke_subagent`）时，Prompt 长度严格限制在 15 行以内，严禁在 Prompt 中内联粘贴长篇需求、DDL、VO 实体或测试要求。必须且仅传递文件指针（Spec 路径、Ticket 路径、Base Commit、分支名，见 `docs/agents/orchestrator-templates.md`），由子 Agent 自行读取。
7. **调度官绝对零代码（Zero Direct Code Editing）**：主会话在 `/implement-spec` 阶段严格定位为纯调度与门禁把关者，严禁使用 `write_to_file` 或 `replace_file_content` 编写或修补业务代码。遇到编译报错、测试失败或 Review 问题时，一律派发 Fixer 子 Agent 在独立分支自闭环修复，绝不亲自下场改代码。