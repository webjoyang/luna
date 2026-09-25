# 03: CustomerContact CRUD end-to-end

**What to build:** 用户在客户工作台切到"联系人" Tab，看到当前客户的联系人列表（el-table），能点"新增联系人"打开弹窗填 6 字段（name/position/phone/email/remark/isPrimary）保存，能点行内"编辑"修改单条联系人，能点行内"删除"二次确认后删除。所有操作在 Tab 内联完成，不需要跳路由。联系人作为独立子资源，有自己的权限码。

**Blocked by:** 02

**Status:** resolved

- [x] SQL 脚本 `luna-srv/sql/postgresql/lunaerp-baseinfo-customer.sql` 追加 customer_contact 表 DDL（CREATE TABLE IF NOT EXISTS + 主键 DO 块 + 序列 `lunaerp_baseinfo_customer_contact_seq` + 全列 COMMENT）+ system_menu 事务性 INSERT：10040 customer-contact 隐藏菜单（type=2，visible='0'，parent_id=10030，作为权限节点用，无 component）+ 10041-10043 三个按钮权限节点（query/create/update/delete）
- [x] 后端新增 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.baseinfo.customercontact` 子包；`controller.baseinfo.customercontact.vo` 含 `CustomerContactSaveReqVO`/`CustomerContactPageReqVO`/`CustomerContactRespVO`
- [x] `CustomerContactDO` 继承 `TenantBaseDO`，`@TableName("lunaerp_baseinfo_customer_contact")`，`@KeySequence("lunaerp_baseinfo_customer_contact_seq")`，字段：id / customerId(Long) / name / position / phone / email / remark / isPrimary(int2)。customerId 直接 Long，无历史包袱（不学 product-image 的 String productId）
- [x] `CustomerContactMapper` 继承 `BaseMapperX<CustomerContactDO>`，含 `selectPage(PageReqVO)` 按 customerId 过滤
- [x] `CustomerContactService` 接口与 Impl 含 `createCustomerContact` / `updateCustomerContact` / `deleteCustomerContact` / `getCustomerContact` / `getCustomerContactPage` + `validateCustomerContactExists(id)` 私有方法（不存在抛 1_030_002_001）；create/update 校验 customerId 与 name 必填；无 isPrimary 单点约束（业务允许同客户多条主联系人，前端管理）
- [x] `CustomerContactController` `@RequestMapping("/lunaerp/baseinfo/customer-contact")`，`GET /page`（参数 customerId 必填）/ `GET /get?id=` / `POST /create` / `PUT /update` / `DELETE /delete?id=`；`@PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer-contact:query|create|update|delete')")`
- [x] `ErrorCodeConstants.java` 追加 `1_030_002_001` CUSTOMER_CONTACT_NOT_EXISTS
- [x] 前端 `luna-ui/src/api/lunaerp-elmui/baseinfo/customer/contact.ts` 含 `getCustomerContactPage` / `getCustomerContact` / `createCustomerContact` / `updateCustomerContact` / `deleteCustomerContact`
- [x] 前端 `luna-ui/src/views/lunaerp-elmui/baseinfo/customer/workspace/ContactsTab.vue` 联系人 Tab：从 props 接收 `customerId`；onMounted 调 `getCustomerContactPage({customerId, pageNo:1, pageSize:20})` 加载列表；el-table 列：name / position / phone / email / isPrimary（el-tag "主联系人"）/ 操作列（编辑/删除）；表格上方"新增联系人"按钮（v-hasPermi=`['lunaerp:baseinfo:customer-contact:create']`）；新增/编辑用 el-dialog 含 6 字段 el-form，name 必填校验；删除 ElMessageBox.confirm 二次确认
- [x] 前端 `workspace/index.vue` 替换联系人 Tab 的占位为 `<ContactsTab :customer-id="customerId" />`
- [x] 后端测试 `luna-srv/.../src/test/java/.../lunaerp/baseinfo/customercontact/CustomerContactControllerTest.java` 覆盖：`GET /page?customerId=` 按 customerId 过滤（验证空列表与有数据两种）+ customerId 缺失报错；`POST /create` customerId 必填 + name 必填 + 成功返回 id；`PUT /update` 不存在 id 报错 1_030_002_001 + 成功更新；`DELETE /delete?id=` 成功逻辑删除；权限码缺失返回 403（若框架测试支持）
- [ ] 验收（待联调环境手动）：在 01/02 票建好的客户工作台切到"联系人" Tab，新增 2-3 个联系人（一个勾选 isPrimary），列表显示；编辑某个联系人改电话保存，列表更新；删除一个联系人二次确认后消失；导航树 customer-contact 隐藏菜单不显示但权限节点生效（在角色管理里能看到 `lunaerp:baseinfo:customer-contact:*` 权限项可分配）

## Comments

- 2026-09-18：实现完成。`CustomerContactServiceImplTest` 14 个用例全绿（8 Service + 6 VO Bean Validation，覆盖 page 空/有数据、create 返 id、update 不存在报 1_030_002_001/成功、delete 逻辑删除、customerId/name 必填、email 格式）；前端 vue-tsc 本票文件 0 错误（仓内 mall/bpm/fms 等既有错误与本票无关）。
- 2026-09-18：规格写的是 `CustomerContactControllerTest`（HTTP 层），lunaerp 模块仍无 MockMvc + security + 测试库基建（product 模块亦无测试），沿用 01/02 票既定决策落纯 Mockito 的 `CustomerContactServiceImplTest`，Controller 必填语义改用内嵌 `ReqVOValidationTest`（直接跑 Hibernate Validator）等价覆盖；权限码缺失 403 用例按票面"若框架测试支持"豁免，待测试基建具备后补 HTTP 层用例。
- 2026-09-18：code-review 双轴后已修：isPrimary 从姓名列拆出为独立列（规格逐条对齐）；update/delete 补 `@Transactional(rollbackFor=Exception.class)` 与 CustomerServiceImpl 对齐；`openFormDialog` 双分支 Object.assign 合并去重。未采纳项：create/update 不校验父客户存在与归属（票面只要求 customerId、name 必填，未要求跨资源存在性校验）；不加分页器（票面固定 pageNo=1/pageSize=20）。
- 2026-09-18（沿 02 票遗留，需决策，不阻塞本票）：错误码 `1_030_002_001` 仍与 `GreigeErrorCodeConstants.GREIGE_TYPE_CODE_DUPLICATE` 同号；本票按票面明确指定使用该码（spec/ADR 规定客户占 1-030-002 段），框架无错误码唯一校验、运行期各自携带 message 不影响功能，但客户与胚布品种的整段重号（000/001/002）需统一改段（客户迁 1-030-003 或胚布迁段），建议另开一票处理。
- 2026-09-18（已解决）：重号问题统一处理，客户模块整体迁至 `1-030-004-xxx`，CUSTOMER_CONTACT_NOT_EXISTS=`1_030_004_001`（CUSTOMER_NOT_EXISTS=`1_030_004_000`）。选择依据：胚布品种随 `985a7ec` 先行上线拥有 002 段；003 段曾被销售合同 WIP 占用，跳号至 004 对两侧 WIP 都免疫。已同步：ErrorCodeConstants 类注释+内联说明、两个 ServiceImpl 注释、两个测试类的断言/方法名/Javadoc、spec.md（新增「错误码段重号修订」小节）、CONTEXT.md 段位全表。全模块 23 个测试全绿。
