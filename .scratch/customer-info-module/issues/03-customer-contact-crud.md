# 03: CustomerContact CRUD end-to-end

**What to build:** 用户在客户工作台切到"联系人" Tab，看到当前客户的联系人列表（el-table），能点"新增联系人"打开弹窗填 6 字段（name/position/phone/email/remark/isPrimary）保存，能点行内"编辑"修改单条联系人，能点行内"删除"二次确认后删除。所有操作在 Tab 内联完成，不需要跳路由。联系人作为独立子资源，有自己的权限码。

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] SQL 脚本 `luna-srv/sql/postgresql/lunaerp-baseinfo-customer.sql` 追加 customer_contact 表 DDL（CREATE TABLE IF NOT EXISTS + 主键 DO 块 + 序列 `lunaerp_baseinfo_customer_contact_seq` + 全列 COMMENT）+ system_menu 事务性 INSERT：10040 customer-contact 隐藏菜单（type=2，visible='0'，parent_id=10030，作为权限节点用，无 component）+ 10041-10043 三个按钮权限节点（query/create/update/delete）
- [ ] 后端新增 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.baseinfo.customercontact` 子包；`controller.baseinfo.customercontact.vo` 含 `CustomerContactSaveReqVO`/`CustomerContactPageReqVO`/`CustomerContactRespVO`
- [ ] `CustomerContactDO` 继承 `TenantBaseDO`，`@TableName("lunaerp_baseinfo_customer_contact")`，`@KeySequence("lunaerp_baseinfo_customer_contact_seq")`，字段：id / customerId(Long) / name / position / phone / email / remark / isPrimary(int2)。customerId 直接 Long，无历史包袱（不学 product-image 的 String productId）
- [ ] `CustomerContactMapper` 继承 `BaseMapperX<CustomerContactDO>`，含 `selectPage(PageReqVO)` 按 customerId 过滤
- [ ] `CustomerContactService` 接口与 Impl 含 `createCustomerContact` / `updateCustomerContact` / `deleteCustomerContact` / `getCustomerContact` / `getCustomerContactPage` + `validateCustomerContactExists(id)` 私有方法（不存在抛 1_030_002_001）；create/update 校验 customerId 与 name 必填；无 isPrimary 单点约束（业务允许同客户多条主联系人，前端管理）
- [ ] `CustomerContactController` `@RequestMapping("/admin-api/lunaerp/baseinfo/customer-contact")`，`GET /page`（参数 customerId 必填）/ `GET /get?id=` / `POST /create` / `PUT /update` / `DELETE /delete?id=`；`@PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer-contact:query|create|update|delete')")`
- [ ] `ErrorCodeConstants.java` 追加 `1_030_002_001` CUSTOMER_CONTACT_NOT_EXISTS
- [ ] 前端 `luna-ui/src/api/lunaerp-elmui/baseinfo/customer/contact.ts` 含 `getCustomerContactPage` / `getCustomerContact` / `createCustomerContact` / `updateCustomerContact` / `deleteCustomerContact`
- [ ] 前端 `luna-ui/src/views/lunaerp-elmui/baseinfo/customer/workspace/ContactsTab.vue` 联系人 Tab：从 props 接收 `customerId`；onMounted 调 `getCustomerContactPage({customerId, pageNo:1, pageSize:20})` 加载列表；el-table 列：name / position / phone / email / isPrimary（el-tag "主联系人"）/ 操作列（编辑/删除）；表格上方"新增联系人"按钮（v-hasPermi=`['lunaerp:baseinfo:customer-contact:create']`）；新增/编辑用 el-dialog 含 6 字段 el-form，name 必填校验；删除 ElMessageBox.confirm 二次确认
- [ ] 前端 `workspace/index.vue` 替换联系人 Tab 的占位为 `<ContactsTab :customer-id="customerId" />`
- [ ] 后端测试 `luna-srv/.../src/test/java/.../lunaerp/baseinfo/customercontact/CustomerContactControllerTest.java` 覆盖：`GET /page?customerId=` 按 customerId 过滤（验证空列表与有数据两种）+ customerId 缺失报错；`POST /create` customerId 必填 + name 必填 + 成功返回 id；`PUT /update` 不存在 id 报错 1_030_002_001 + 成功更新；`DELETE /delete?id=` 成功逻辑删除；权限码缺失返回 403（若框架测试支持）
- [ ] 验收：在 01/02 票建好的客户工作台切到"联系人" Tab，新增 2-3 个联系人（一个勾选 isPrimary），列表显示；编辑某个联系人改电话保存，列表更新；删除一个联系人二次确认后消失；导航树 customer-contact 隐藏菜单不显示但权限节点生效（在角色管理里能看到 `lunaerp:baseinfo:customer-contact:*` 权限项可分配）
