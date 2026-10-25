# 调度官（Orchestrator）极简派发模板

本文件定义主会话在调度 `/implement-spec` 时派发各类子 Agent（`invoke_subagent`）的标准模板。
核心原则：**极简指针化（Context Pointers Only），严禁内联粘贴需求正文、DDL、VO 实体或代码。**

---

## 1. Implementer 派发标准模板（< 10 行）

用于派发开发具体 Ticket 的子 Agent。在独立分支与独立 Worktree 中自闭环运行 `/tdd`。

```markdown
You are Implementer for Ticket {NN}: {slug}.
Context pointers:
- Spec: .scratch/{feature}/spec.md
- Ticket: .scratch/{feature}/issues/{NN}-{slug}.md
- Integration Branch: feat/{feature}
- Worktree Branch: ticket-{NN}
- Exploration Notes: .scratch/{feature}/exploration.md (if exists)
Rule: If .codegraph exists, explore symbols using `codegraph explore "<symbol>"` FIRST (fallback to git grep if unideal).
Execute /tdd in your isolated worktree, commit with standard message "feat({feature}): ... (Issue {NN})", merge integration branch tip, and report single-line summary when done.
```

---

## 2. Fixer / Review-Fixer 派发标准模板（< 10 行）

用于单测挂掉、编译报错、或 `/code-review` 发现问题时的救火与修复。主会话严禁亲自下场改代码。

```markdown
You are Fixer for Ticket {NN}: {slug}.
Context pointers:
- Worktree Branch: ticket-{NN}
- Failure Log / Review Points: {log_file_path_or_review_report_pointer}
- Spec: .scratch/{feature}/spec.md
- Ticket: .scratch/{feature}/issues/{NN}-{slug}.md
Rule: Prefer `codegraph explore "<symbol>"` to inspect call sites and blast radius if modifying shared code (fallback to git grep if unideal).
Fix the issue in your isolated worktree, verify tests pass silently, commit with "fix({feature}): ...", and report single-line summary.
```

---

## 3. Merger 派发标准模板（< 10 行）

用于将完成的 Ticket 分支合并回集成特性分支（Integration Branch）并更新 Issue 状态。

```markdown
You are Merger for Ticket {NN}: {slug}.
Task:
1. Checkout integration branch `feat/{feature}`.
2. Merge `ticket-{NN}` with `--no-ff` (message: "merge: feat({feature}): ... (Issue {NN})").
3. Verify test suite passes silently.
4. Update `.scratch/{feature}/issues/{NN}-{slug}.md` (Status: resolved, mark [x], append ## Answer).
5. Commit doc update, delete `ticket-{NN}` branch and worktree, report single-line commit hash.
```

---

## 4. Exploration（调研摸底）子代理模板（< 10 行）

在实现前若需要摸排架构或旧业务代码，派发只读 `research` 子代理，严禁主会话亲自调用 `view_file` 通读。

```markdown
You are Exploration Agent for feature {feature}.
Task:
1. Explore existing codebase related to {module/feature} (prefer `codegraph explore "<symbol>"` if .codegraph exists; fallback to git grep if unideal).
2. Summarize findings, existing APIs, service patterns, and relevant files into `.scratch/{feature}/exploration.md`.
3. Report only the saved file path and a 2-line summary. Do not output raw code files in your report.
```

## 5. Reviewer（代码审查）子代理派发标准模板（< 12 行）

用于特性工单实现完毕后的 Standards / Spec 并行 Review。Reviewer 子代理必须以只读 `Workspace: "inherit"` 继承父工作区。

> [!IMPORTANT]
> **Diff 优先审查规约 (Diff-First Review Guard)**：Reviewer 必须首先通过 `git diff <base_commit>..HEAD`（或针对特定文件 `git diff <base_commit>..HEAD -- <path>`）查阅实际代码变更差异；**严禁对所有新增或修改文件无脑调用 `view_file` 全量通读**（极易引发数十次冗余文件读取与上下文膨胀，导致单次审查超时 10+ 分钟）；仅在 Diff 中发现疑似问题需要上下文确认时，才允许针对性对特定代码块做切片读取（Slice Read）。

### Standards Reviewer 模板：
```markdown
You are Standards Reviewer for Ticket {NN}: {slug}.
Context pointers:
- Diff: git diff {base_commit}..HEAD
- Standards: docs/agents/coding-standards.md
- Arch Test: cd luna-srv; mvn test -q -pl lunasrv-module-lunaerp "-Dtest=ArchitectureStandardsTest"
Rule: Review git diff FIRST. Prohibit full-file view_file traversals. Use slice read only when investigating specific diff hunks.
Report: Concise pass/fail summary with violated standard rules (if any), zero raw code dumps.
```

### Spec Reviewer 模板：
```markdown
You are Spec Reviewer for Ticket {NN}: {slug}.
Context pointers:
- Diff: git diff {base_commit}..HEAD
- Ticket: .scratch/{feature}/issues/{NN}-{slug}.md
- Spec: .scratch/{feature}/spec.md
- Verify Test: {compact_verification_command}
Rule: Review git diff FIRST against Ticket checklist. Prohibit full-file view_file traversals. Use slice read only when investigating specific diff hunks.
Report: Compact checklist mapping (pass/fail per item), zero raw code dumps.
```

---

## 6. 对比示范

### ❌ 坏示范（Prompt 臃肿，吞噬 10k+ Tokens）：
```markdown
请帮我实现 Ticket 01。需求如下：
我们需要创建表 system_menu，ID 是 10150，权限是 lunaerp:baseinfo:customer:portal-token。
接口定义在 CustomerPortalTokenController，有三个方法：
1. GET /admin-api/lunaerp/baseinfo/customer/portal-token
2. POST /admin-api/lunaerp/baseinfo/customer/portal-token/generate
3. DELETE /admin-api/lunaerp/baseinfo/customer/portal-token
生成的 token 用 SecureRandom 32 字节 Base64URL 编码，存在 Redis 里面，主键是 luna_portal:token:{token}...（后略 2000 字）
```

### ✅ 好示范（极简指针，仅 80 Tokens）：
```markdown
You are Implementer for Ticket 01: admin-token-issuance.
Context pointers:
- Spec: .scratch/customer-portal/spec.md
- Ticket: .scratch/customer-portal/issues/01-admin-token-issuance.md
- Integration Branch: feat/customer-portal
- Worktree Branch: ticket-01
Execute /tdd in your isolated worktree, commit with standard message, merge integration branch tip, and report done.
```
