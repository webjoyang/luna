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

### 代码检索与第一击门禁（CodeGraph 优先 + git grep 边界白名单）

- 混合工作区文件定标优先使用 `git ls-files "*<keyword>*"`，严禁未排除 `node_modules` / `target` 进行递归深搜。
- **Java 符号检索第一击门禁（First-Strike Gate）**：
  - 凡是查询 Java 类名（大驼峰）、方法名（小驼峰）、接口、DO 实体字段属性、或分析上下游调用链路（如 `RecycleBinHandler`、`InspectionConfirmation`、`getPayrollSummary` 等），**第一击必须且只能使用 `codegraph explore "<symbol>"`（或 MCP 工具 `codegraph_explore`）**。单次调用即可获取完整符号实现、行号及调用拓扑，严禁用 `git grep` 盲目试探代码符号。
- **git grep 仅限以下非代码文本白名单**：
  1. Markdown 需求文档与 Spec 文本检索（如 `.scratch/`、`docs/`）；
  2. 纯非符号文本常量（如错误码前缀 `025_`、权限标识字符串 `lunaerp:finance:...`、SQL 物理列名）；
  3. 特性开发中刚创建未暂存、尚未被 CodeGraph 索引的新建文件（附加 `--untracked`，如 `git grep --untracked "<pattern>"`）。
  - **执行安全禁令**：严禁在 PowerShell 下使用 `xargs` / Linux `grep` 管道；混合工作区根目录无 `src/`，必须显式指定子项目路径（如 `git grep "<pattern>" luna-ui/ luna-srv/`）。
- 严禁在 PowerShell 下将 `vue-tsc`、`mvn` 等长生命周期或海量输出的命令挂接管道传递给 `Select-String`（子进程标准流缓冲会导致进程假死 hang 住）。
- Windows PowerShell 5.1 语法守卫：多命令串联统一使用分号 `;` 分隔连接（例如 `cmd1; cmd2`）或拆解为独立执行步骤，严格保持与 Windows PowerShell 5.1 脚本引擎语法兼容。
- 复杂文本与日志过滤：在 Windows PowerShell 5.1 下，针对长日志或 JSON 结构的流式过滤与统计，避免编写含复杂引号转义（如 `$_`）的脆弱命令行管道，优先使用 `node -e "..."` 或在会话 `scratch/` 目录编写一次性 Node.js 脚本。

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
- 单测执行与快速预检：`Cwd` 切换至 `luna-srv` 目录，执行 `mvn test "-Dtest=TargetTest"` 或指定模块 `mvn test -pl lunasrv-module-lunaerp "-Dtest=TargetTest"`（多类测试在 PowerShell 下务必使用双引号包裹 `"-Dtest=TestA,TestB"`）；凡涉及 `finance`、`business` 等域新增/修改 Controller/Mapper/DO/Handler 时，必须捆绑架构守护单测 `"-Dtest=ArchitectureStandardsTest,TargetTest"`，前置自动检验脱敏注解、可空更新策略、跨域 Mapper 直写拦截与 Mapper 纯洁性（接口严禁 `default` 方法，区别于底座 yudao 习惯）。新增或大幅重构单测后，可先执行 `mvn test-compile -pl lunasrv-module-lunaerp -q` 实施秒级语法与类型预检，杜绝低级编译错误直接进入耗时测试流程。
- 框架依赖刷新：修改或依赖刚刚调整过的 `lunasrv-framework` 基础组件时，需先执行 `mvn -pl lunasrv-framework/<module-name> install -DskipTests`（如 `lunasrv-spring-boot-starter-mybatis`）将最新 jar 安装至本地仓库，避免子模块 `-pl` 测试时读取 `~/.m2` 旧快照。
- 编码安全与 import 维护：使用 `replace_file_content` 增删 import 时，`TargetContent` 必须仅定位纯 ASCII 单行 import 语句，严禁跨越 package、Javadoc、类注释或非 ASCII 文本行；超过 3 行的大批量 import 整理优先编写 Node.js 脚本或单行增删。涉及头-行-卷或主子表跨层数据映射时，编码前必须通过 `view_file` 切片查阅 DO/VO 物理属性定义，严禁凭经验臆测字段挂载层级（如缸号/匹号/卷号归属）。
- 数据库脚本与领域规约：增量脚本执行状态见 `luna-srv/sql/postgresql/README.md`；PostgreSQL 幂等发号、顺延重试、DTO 校验、行级悲观锁、范围守卫、可空列清空、重构检索、审批流回调单写、伴生单据协同作废、质检中立初始态、跨单位物理量、入库 placement、跨域记账监听器 Fail-Fast 与源码 import 纯洁性规约见 `docs/agents/coding-standards.md` 第 8~26 节。



## 上下文与终端防膨胀纪律（Context Hygiene）

> **作用域与防套娃声明：**
> 1. **主会话（Main / Orchestrator）：** 严格受限于下述所有阅读上限、重型命令禁令与熔断派发规则，核心目标是确保主上下文始终保持在 150k Token 聪明区以内，绝不触发有损自动压缩。
> 2. **子代理（Sub-agent：Implementer / Fixer / Research / Diagnosing）：** 在各自独立上下文中负责具体闭环，**不受“阅读 3 个文件上限”及“零代码编辑”限制**，但必须严格遵守【终端与 CLI 降噪规范】，且**严禁子代理再次嵌套调用 `invoke_subagent`**。

### 1. 终端与 CLI 降噪规范（主会话与子代理通用）
- **无错即静默（Silent on Success）：** 任何可能产生长输出的构建、测试或脚本命令，严禁直接裸跑刷屏。必须确保 `.scratch/logs/` 目录存在并将输出重定向至 `.scratch/logs/<name>.log`，同时保留真实退出码：
  * **执行准则：** 成功仅输出单行 `✓ OK`；失败仅输出 `单行摘要 + 日志路径 + 末尾 30 行`，并确保返回非零退出码（参考范式：`mkdir -p .scratch/logs && cmd > .scratch/logs/<name>.log 2>&1 && echo "✓ OK" || { rc=$?; echo "✗ FAILED (exit $rc) - log: .scratch/logs/<name>.log"; tail -n 30 .scratch/logs/<name>.log; exit $rc; }`）。
  * **Git 隔离：** `.scratch/` 目录仅作临时工作区，严禁提交至版本库（执行 `git add` 时严禁包含 `.scratch/`）。
- **CLI 强制紧凑参数：**
  * **Git：** 查状态必用 `git status -s`；查历史必用 `git log --oneline -n 10`；查全局变更概览必用 `git diff --stat`（仅在需要审查具体代码行差异时，才允许针对单一文件执行 `git diff -- <file_path>`，严禁裸跑全量 `git diff`）。
  * **Maven：** 强制追加静默参数 `mvn <goal> -q`；调试单测时必须精确到单类或单方法（`mvn test -q -Dtest=ClassName#methodName`），严禁在调试期反复跑全量 `mvn test`。
  * **前端（pnpm / Vitest）：** 运行脚本必加 `pnpm <cmd> --silent`；运行 Vitest 单测强制使用 `pnpm vitest run --reporter=dot --bail=1`（遇首个错误即停，杜绝连环报错刷屏）。
  * **调试例外原则：** 仅日常命令强制启用紧凑参数；深度调试需要完整上下文时允许去掉 `-q` / `--silent`，但**详细输出必须重定向至 `.scratch/logs/<name>.log` 按需检索，严禁直接打印到终端刷屏**。

### 2. 跨域探索下沉与行号切片（Read-Heavy Isolation & Slice Read）
- **3 文件深度通读红线：** 主会话仅允许使用 `view_file` 全量通读与当前修改直接相关的核心目标文件（单次任务在主会话直接通读的源码文件累积不得超过 3 个）。
- **只读参考行号切片（Slice Read）：** 查阅架构守护测试用例、DDL 物理列定义或现有参考接口/方法签名时，**必须指定 `StartLine` 与 `EndLine` 精确切片查看（单次切片建议 <= 60 行）**。此类精准切片不计入通读红线；**严禁为规避阅读红线而反复执行 `git grep -n -A 20/30` 充当伪 `view_file`**（会导致大量碎片探针、高频交互与上下文污染）。
- **Research 只读代理与前置摸底：** 凡需查阅超过 3 个文件以摸底系统架构、跨模块调用链路或排查第三方库机制时，主会话严禁亲自大面积探索；必须调用 `invoke_subagent` 派发只读 `research` 子代理。对于涉及多领域模型、复杂 DDL 与多表聚合的工单，在进入编码前派发 1 轮轻量 `research` 子代理提取符号表与契约边界，主会话仅接收 200 字以内的核心结论与文件指针。
- **前置符号锚定（Symbol Anchoring）：** 特性开发进入编码前，必须先从 Spec 提取 1~2 个核心上下游实体/接口符号（如 `InspectionConfirmation`），执行一次 `codegraph explore "<symbol>"` 锚定契约边界与行号，严禁在未建立符号全景前发起连续碎片化 grep。

### 3. 子代理派发强制指针化（Context Pointer 协议）
主会话调用 `invoke_subagent`（无论是派发开发、排错还是调研）时，**Prompt 严格限制在 15 行以内**，严禁在 Prompt 中内联粘贴长篇历史对话、完整需求文档、DDL、VO 实体或长报错日志。必须且仅传递以下四要素指针，由子代理自行读取：
1. **目标简述：** 一句话说明子任务边界（如 Ticket ID 或待定位的异常现象）。
2. **上下文指针：** Spec 路径、Ticket 路径、核心源码及异常行号指针（`file:///...`）、报错日志路径（`.scratch/logs/...`）、Git Base Commit 与工作分支名（参见 `docs/agents/orchestrator-templates.md`）。
3. **确定性验证指令：** 唯一用于复现问题或验收通过的最小化单测/构建命令。
4. **返回契约：** 明确要求子代理仅返回单行执行结果摘要、精简根因说明或文件变更统计（严禁子代理在返回结果中倾倒大段源码或全量日志）。

### 4. 分模式执行与熔断纪律（Mode-Specific Execution）

#### 模式 A：`/implement-spec` 多工单编排阶段（主会话 = 纯调度官）
- **绝对零代码（Zero Direct Code Editing）：** 主会话严格定位为纯调度与门禁把关者，**严禁**使用 `write_to_file` 或 `replace_file_content` 编写或修补任何业务代码。
- **大命令与修复全下沉：** 编译打包（如 `pnpm build`）、完整单测（如 `mvn test`）及代码实现一律由 `Implementer` 子代理在独立上下文与对应分支中执行并自闭环；遇到编译报错、测试红灯或 Review 问题时，主会话一律派发 `Fixer` 子代理闭环修复，主会话仅接收单行成功/失败摘要与 Commit Hash。

#### 模式 B：`/implement` 或日常单点任务（主会话 = 受限执行者）
- **重型命令下沉：** 主会话仅允许运行针对单个测试类的轻量验证命令；全量编译打包与全量测试套件必须委派子代理执行。
- **两遍试错熔断（Rule of Two Failures）：** 当编译报错或单测红灯时，主会话最多只允许尝试修改 **1 次**。若第 **2 次**执行验证依然报错，严禁在主会话继续盲目试错！必须立即触发熔断，调用 `invoke_subagent` 派发专职诊断/修复子代理（`diagnosing-bugs` 或 `Fixer`），按【Context Pointer 协议】传入最小复现指令与 `.scratch/logs/` 错误日志指针，由子代理在独立上下文中定位根因并完成修复（或返回最小 Diff 供主会话直接应用）。