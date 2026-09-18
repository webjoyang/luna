# 02: Customer update + delete（completes CRUD）

**What to build:** 用户能在工作台"基础档案" Tab 加载已有客户数据、修改字段并保存（update），能从列表行点"编辑"直接进工作台基础档案 Tab 编辑模式，能从列表行点"删除"或工具栏勾选多行后点"批量删除"删除客户（带二次确认），删除后客户 status 置 INACTIVE 且 deleted=1，列表与 `GET /get` 不再返回该客户。客户模块 CRUD 完整闭环。

**Blocked by:** 01

**Status:** resolved

- [x] 后端 `CustomerService` 接口与 Impl 追加 `updateCustomer(SaveReqVO)` / `deleteCustomer(id)` / `deleteCustomerList(ids)` + `validateCustomerExists(id)` 私有方法（不存在抛 1_030_002_000）；update 时 shortName 同 create 走回填逻辑（抽 `backfillShortName` 共用）；delete 时先 `UPDATE status='INACTIVE'` 再走 `mapper.deleteByIds`（逻辑删除，与 product 模块模式一致）
- [x] `CustomerController` 追加 `PUT /update` + `DELETE /delete?id=` + `DELETE /delete-list?ids=1,2,3`；`@PreAuthorize` 加 `lunaerp:baseinfo:customer:update|delete`
- [x] 前端 API 追加 `updateCustomer` / `deleteCustomer` / `deleteCustomerList`
- [x] 前端 `BasicProfileTab.vue` 编辑模式：根据路由 `:id` 是否存在区分 create/edit 模式；edit 模式 onMounted 调 `getCustomer(id)` 回填表单；保存按钮调 `updateCustomer`；保存成功 ElMessage 提示 + 回列表
- [x] 前端列表页 `index.vue` 操作列接"编辑"（push 到 `customer/workspace/${row.id}` 工作台自动进基础档案 Tab）+ "删除"（delConfirm 二次确认 → 调 `deleteCustomer` → 刷新列表）；工具栏追加"批量删除"按钮（v-hasPermi=`['lunaerp:baseinfo:customer:delete']`），表格开启 selection 列，勾选后批量删除二次确认 → 调 `deleteCustomerList(ids)` → 刷新列表
- [x] 前端所有 create/update/delete 按钮加 `v-hasPermi` 权限指令
- [x] 后端测试追加（落在既有的纯 Mockito `CustomerServiceImplTest`，本模块无 MockMvc/DB 测试基建，同 01 票与 product 模块）：update 不存在 id 报错 1_030_002_000 + 成功更新并验证 shortName 回填在 update 时也生效；delete 不存在 id 报错 + 成功删除验证先 updateById(INACTIVE) 再 deleteByIds（DB status=INACTIVE/deleted=1 语义以调用验证代替）；delete-list 批量删除逐条断言 INACTIVE
- [ ] 验收（待联调环境手动）：在 01 票建好的客户上，点列表"编辑"进工作台修改字段保存，列表显示更新；点"删除"二次确认后客户从列表消失，DB 验证 status=INACTIVE deleted=1；勾选多条批量删除生效；无 update 权限的用户编辑按钮不显示

## Comments

- 2026-09-18：实现完成，`CustomerServiceImplTest` 9 个用例全绿；前端 vue-tsc 客户相关文件 0 错误（仓内 mall/bpm/fms 既有错误与本票无关）。
- 2026-09-18：规格要求的是 `CustomerControllerTest`（HTTP 层），但 lunaerp 模块当前无 MockMvc + security + 测试库基建（product 模块亦无任何测试），按 01 票既定决策以纯 Mockito Service 单测覆盖同等业务语义；待测试基建具备后可补 HTTP 层用例。
- 2026-09-18（需决策，不阻塞本票）：错误码 `1_030_002_xxx` 段与既有 `GreigeErrorCodeConstants`（胚布品种，已占用 1_030_002_000/001/002）重号。本票按 spec.md L123-126 与 ADR-0002 的明确规定沿用 1_030_002_000；框架无错误码唯一校验、功能不受影响，但 03 票计划给联系人用的 1_030_002_001 也会重号，建议 03 票前统一改段（如客户迁 1-030-003-xxx）或胚布迁段。
