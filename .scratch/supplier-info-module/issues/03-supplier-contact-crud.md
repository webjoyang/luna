# 03: 供应商联系人端到端（独立子资源）

**What to build:** 采购人员在供应商工作台的"联系人"Tab 里，可以为每家供应商维护多名对接人（接单、财务、档口等）：内联看到联系人列表，通过弹窗单条新增、编辑、删除，并可标记主联系人。联系人是独立子资源——每条操作即时调自己的接口保存，不随供应商基础档案表单整体提交替换（与客户联系人模式同构，ADR-0002）。

**Blocked by:** 02 供应商主档案端到端（建表/编号 → CRUD → 列表页 → 基础档案工作台）

**Status:** ready-for-human

- [x] 部署 SQL 可重复执行：建供应商联系人表与序列（supplier_id/name 必填，position/phone/email/remark/is_primary，全列注释、supplier_id 索引）；注册供应商联系人隐藏权限节点与增改删按钮（导航树不显示，角色管理可分配）
- [x] 联系人分页接口必须带 supplierId，返回该供应商联系人且主联系人优先排序；详情/新建/修改/删除五个端点行为正确
- [x] 新建校验 supplierId 与姓名必填；操作不存在的联系人报 1-030-005-002；删除为直接逻辑删除，不联动供应商状态
- [x] 同一供应商允许多条 isPrimary=1，后端不建唯一约束；不随供应商保存载荷整体替换
- [x] 五个端点均挂权限码 lunaerp:baseinfo:supplier-contact:{query|create|update|delete}，无权限访问被拦截；多租户自动隔离
- [x] 工作台"联系人"Tab 激活：进入即按 supplierId 加载列表（姓名/职位/电话/邮箱/主联系人标记/备注），提供新增、编辑、单条删除（二次确认）弹窗，保存后列表即时刷新
- [x] 前端联系人 API 与类型定义独立于供应商主 API；未选/无供应商上下文时 Tab 不发出无 supplierId 的请求
- [x] 新增供应商联系人 Service 纯 Mockito 单元测试（镜像客户联系人测试）：supplierId/姓名必填校验、不存在错误码 1-030-005-002、逻辑删除、主联系人排序参数透传

## Comments

### 2026-09-19 实现完成（ready-for-human）

后端（DO/VO/Mapper/Service/Controller/错误码/SQL）与独立前端联系人 API 在票 02 中已预先落地并通过核对，本票补齐工作台前端闭环并完成全量验证：

- 新增 `luna-ui/src/views/lunaerp-elmui/baseinfo/supplier/workspace/ContactsTab.vue`：镜像客户 ContactsTab——supplierId 空态（create 模式不发请求）、列表（姓名/职位/电话/邮箱/主联系人标记/备注/操作）、新增/编辑弹窗（6 字段、姓名必填）、删除 `delConfirm` 二次确认、保存与删除后即时刷新；按钮挂 `lunaerp:baseinfo:supplier-contact:{create|update|delete}` 权限指令。
- 工作台 `workspace/index.vue` 联系人 Tab 由 PlaceholderTab 切换为 `<ContactsTab :supplier-id="supplierId" />`。
- 独立 API：`luna-ui/src/api/lunaerp-elmui/baseinfo/supplier/contact.ts`（五端点 + SupplierContactVO/SupplierContactPageParam，与供应商主 API 无耦合）。
- 后端核对：五端点权限码齐全；分页 supplierId `@NotNull` + `orderByDesc(isPrimary, createTime)` 主联系人优先；不存在统一抛 1_030_005_002；删除仅 `deleteById` 逻辑删除无状态联动；DO 继承 TenantBaseDO；SQL 第 5/6 节可重复执行（表/序列/14 列注释/supplier_id 索引/隐藏权限节点 10060-10063）。
- 验证：lunaerp 模块全量 87 个单测通过（其中供应商联系人 14 个：8 主用例 + 6 VO 校验）；前端 `pnpm build:local` 成功；新文件 VS Code 诊断 0 错误（`pnpm ts:check` 仅有 mall/thinking-orb/fms/pms 等无关存量文件的历史报错）。
- 代码审查：无阻断/严重/一般问题；3 条低级观察（更新不校验 supplierId 归属、新建不校验供应商存在、列表固定前 20 条无分页器）均为客户联系人同构共性，建议客户/供应商两侧后续统一收口，不阻塞本票。
