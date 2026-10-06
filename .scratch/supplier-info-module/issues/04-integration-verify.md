# 04: 供应商模块前后端联调与发布验证

**What to build:** 在 02、03 完成后，以真实服务 + 浏览器完成供应商档案 MVP 的端到端验收，确认新模块自身链路通畅、与客户/销售合同的命名重构联动无回归、部署脚本幂等可发布。产出可对外演示的完整流程：建供应商（自动号/手填号）→ 维护联系人 → 筛选查找 → 编辑停用 → 删除验证编号不重用。

**Blocked by:** 02 供应商主档案端到端（建表/编号 → CRUD → 列表页 → 基础档案工作台）、03 供应商联系人端到端（独立子资源）

**Status:** ready-for-human

- [ ] 冒烟-建档：自动生成编号（形如 SUP20260001）建档成功；预览下一号与实际分配一致；手填历史编号建档成功
- [ ] 冒烟-编号边界：手填撞在用编号被拒；删除一家供应商后用其编号新建被拒（软删不重用）；自动号在并发/撞号场景下顺延成功
- [ ] 冒烟-列表：搜索词分别用公司名/简称/编号/电话命中；类型下拉 18 项渲染正确并可筛选；状态筛选与分页、倒序正确
- [ ] 冒烟-工作台：6 个 Tab 齐全，基础档案修改保存生效、改号撞号有提示；联系人 Tab 增删改即时生效、主联系人标识正确；4 个占位 Tab 显示"开发中"；URL 直达可打开
- [ ] 冒烟-删除：单删与批量删除均有二次确认；删除后列表不可见、详情报不存在；停用供应商可正常删除
- [ ] 冒烟-权限：按角色分配供应商与供应商联系人权限码后，按钮显隐与接口访问均符合预期
- [x] 回归-客户模块：客户建档/搜索/简称回填/工作台联系人在 companyName 重命名后行为不变
- [x] 回归-销售合同：新建/编辑合同时选择客户，客户名称快照回填与展示不变
- [x] 后端 lunaerp 模块全量 mvn test 通过（含新增 2 个与修订的 2 个测试类）；前端 typecheck 与生产 build 通过
- [ ] 部署脚本在空库、已安装客户模块的存量库上各完整重跑一次均成功（菜单/字典不重复、列迁移不报错、数据不丢失）
- [x] GLOSSARY.md/ADR-0005 与最终实现一致（编号规则、错误码、字典 value、菜单/字典 ID、Tab 名称），如有落地偏差已回改文档

## Comments

### 2026-09-19 环境受限收尾（ready-for-human）

本机无 docker / psql；唯一可达 PostgreSQL 为团队共享 dev 库，脚本头部明确"禁止在共享远程库直接执行"，且该库尚未执行过供应商脚本（探测接口返回 `relation "lunaerp_baseinfo_supplier" does not exist`，客户表仍为旧 `company` 列）。经确认采用 C 方案收尾——不碰共享库、不强行下载本地库，按静态核查 + 单测 + 生产构建验证，以下为已核实与遗留项。

**已核实（通过）**

- 后端全量单测：`mvn test -pl lunasrv-module-lunaerp` 退出码 0；surefire 汇总 7 类 87 例全绿——Supplier 21、SupplierContact 14（新增）、Customer 9/14、SalesContract 29（companyName 重命名联动修订）。
- 前端生产构建：`pnpm build:prod` 成功（33.85s，输出 dist-prod）；`pnpm ts:check` 仅 32 个报错，全部位于 mall/thinking-orb/fms/pms 等无关存量模块与旧的 `views/lunaerp/product` 正式版，`lunaerp-elmui` 及 customer/supplier/contract 改动文件零报错（与 02/03 结论一致，已比对两个提交的改动文件清单）。
- 静态一致性核查（与 spec/CONTEXT/ADR-0005 逐项对齐）：
  - 编号规则：`SUP`+年份+4 位、超 9999 自然扩位；取号 `selectListBySupplierNoPrefixIncludingDeleted` 与查重 `selectBySupplierNoIncludingDeleted` 均手写 SQL 绕过 MP 逻辑删除过滤，注释标注 ADR-0005 意图；自动号撞号 `MAX_GENERATE_RETRY=3` 顺延，连续撞号抛 `1_030_005_001`；`getNextSupplierNo` 复用扫描入口、不写库；更新时编号留空置 null 跳过列、变更做全量查重排除自身。
  - 错误码：`1_030_005_000/001/002` 三码齐全。
  - 字典：18 项 value 与 label 完全一致（YARN_MILL…FREIGHT），data id 3550–3567、type id 2220。
  - 菜单：10050 供应商信息、10051–10053 增改删；10060 联系人隐藏节点、10061–10063 增改删；权限码 `lunaerp:baseinfo:supplier:{query|create|update|delete}` 与 `lunaerp:baseinfo:supplier-contact:{query|create|update|delete}` 与 Controller `@PreAuthorize` 逐一对应（供应商 7 端点、联系人 5 端点）。
  - DO 字段：Supplier 9 字段 + TenantBaseDO，SupplierContact 7 字段 + TenantBaseDO；联系人 `supplierId` 逻辑关联无外键、`isPrimary` 无唯一约束。
  - 前端：列表页搜索 + 类型/状态下拉 + 7 列 + v-hasPermi；工作台 6 Tab（基础档案/联系人激活，入库/付款/关联品种/位置分布 PlaceholderTab 显示"开发中"）；基础档案含编号预览按钮；联系人 Tab 内联表格 + 弹窗单条增删改 + 主联系人开关；路由 `supplier/workspace` 与 `supplier/workspace/:id` 已登记。
  - GLOSSARY.md 与 ADR-0005 已覆盖编号规则、错误码段、字典 18 项、Tab 名称、companyName 统一命名，与最终实现一致，无需回改。
- 回归核查（静态 + 单测，dev 库因列未迁移无法运行时回放）：
  - 客户：CustomerMapper search 命中 `company_name`；CustomerServiceImpl shortName 回填走 `getCompanyName()`；前端客户 API/列表/基础档案/工作台全部 `companyName`；CustomerServiceImplTest 9 例、CustomerContactServiceImplTest 14 例通过。
  - 销售合同：SalesContractServiceImpl 两处 `setClientName(customer.getCompanyName())` 快照（仅取值入口改名，clientName 字段不变）；前端合同编辑页选择器 `:label="item.companyName"`、快照回填 `formData.clientName = customer?.companyName`；SalesContractServiceImplTest 29 例通过。
- SQL 幂等性静态核查：supplier 脚本全程 `IF NOT EXISTS` + 主键/索引 `DO` 块判存 + 菜单字典先 DELETE 后 INSERT；customer 脚本 `company → company_name` 迁移块三态幂等（旧列存在且新列不存在才 RENAME，空库新表与已升级库重跑均跳过，RENAME 保留数据与约束）。

**环境受限遗留（未实证，待有 PostgreSQL 环境补验）**

- 冒烟-建档/编号边界/列表/工作台/删除/权限 共 6 项真实服务 + 浏览器 E2E 未执行。原因：dev 库未部署供应商 schema，且禁止对共享库执行 DDL；已通过代码路径静态走读 + SupplierServiceImplTest 21 例（含取号/软删撞号/顺延重试/预览不占号/回填/缺省状态/删除先停用/批量删除/分页条件）覆盖关键业务语义。
- 部署脚本空库 + 已安装客户模块存量库双库完整重跑未实证（同票 01 的 company_name 迁移遗留）。脚本为标准 PL/pgSQL DO 块 + information_schema 判存，三态逻辑已静态核查，但无本机 PG 双库实证。
- 浏览器侧 6 Tab 渲染、DictTag 18 项、v-hasPermi 显隐、二次确认弹窗等纯前端交互未在浏览器实测。

**建议后续补验步骤**（具备 PG 环境后）：在空库与含旧客户表的存量库各执行一次 customer 脚本 + supplier 脚本 → 启动服务 → 走建供应商（自动号/手填号）→ 联系人增删改 → 搜索筛选 → 编辑撞号 → 停用 → 删除 → 重用旧编号被拒全流程 → 角色权限码验证 → 脚本第二次重跑验证幂等。

### 2026-09-19 缺陷修复：客户信息/供应商信息被渲染成带子菜单的父菜单

- 现象：侧边栏"客户信息""供应商信息"右侧出现空展开箭头，变成可展开父菜单（展开无子项）；产品信息/胚布品种/销售合同正常为叶子菜单。
- 根因：联系人权限承载节点 10040/10060 原设计为 `type=2、visible='0'` 隐藏菜单，后端路由菜单接口只过滤 type=3 按钮，type=2 隐藏节点仍被前端 `generateRoute` 生成为子路由；页面菜单自身 `always_show='1'`，`useRenderMenuItem` 在"有子路由 + alwaysShow"时强制渲染 ElSubMenu，于是出现空展开箭头。
- 修复（纯 SQL/文档，无 Java/前端代码变更）：10040-10043、10060-10063 全部改为 type=3 按钮、path/icon/component 置空，四个联系人权限节点（query/create/update/delete）平铺直接挂页面菜单 10030/10050（sort 10-13，排在自身增改删按钮之后）；平铺而非按钮挂按钮，以满足后端 `validateParentMenu`"按钮父级必须是目录/菜单"约束，菜单管理界面后续编辑不会报错。type=3 不进路由树，侧边栏恢复叶子直达；角色管理菜单树仍可勾选全部联系人权限码。
- 幂等：脚本对应事务段为先 DELETE 后 INSERT，存量库重跑两个脚本的菜单段即可修正，不影响角色已分配数据（system_role_menu 按菜单 id 关联，id 不变）。
- 同步回改：customer/supplier 两个 spec 的菜单 ID 分配小节。
- 验证：前端代码走读闭合（产品信息仅有 type=3 子节点、线上为叶子菜单为同构实证）；浏览器实测待菜单数据更新后刷新确认。
