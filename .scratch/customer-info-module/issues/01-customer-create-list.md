# 01: Customer create + list（vertical）

**What to build:** 用户能在客户工作台的"基础档案" Tab 里新建客户档案（12 字段表单，5 个字典下拉，company + clientType 必填，shortName 未填时后端自动用 company 回填，status 缺省 ACTIVE），保存后回到列表页看到该客户，点行能进工作台查看。Workspace 7-Tab 容器就位——基础档案 Tab 激活可编辑，其余 6 个 Tab（联系人/合同/寄样/画像分析/位置分布/拜访记录）渲染"开发中"占位。列表页支持单 search 智能词（命中 company/shortName/phone/unifiedSocialCreditCode 之一）+ status 下拉筛选 + 分页。

**Blocked by:** None（can start immediately）

**Status:** ready-for-agent

- [ ] SQL 脚本 `luna-srv/sql/postgresql/lunaerp-baseinfo-customer.sql` 完成 customer 表 DDL（CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 序列 `lunaerp_baseinfo_customer_seq` + 全列 COMMENT）+ system_menu 事务性 INSERT（先 DELETE 后 INSERT，可重复执行）：10030 客户信息菜单（parent_id=10000，component=`lunaerp-elmui/baseinfo/customer/index`，component_name=`LunaerpElmuiBaseinfoCustomer`，icon=`ep:office-building`）+ 10031-10033 三个按钮权限节点（query/create/update/delete，注意 10031=query, 10032=create, 10033=update+delete 合并或分两个具体看 product 菜单模式）+ 5 个 system_dict_type 与对应 system_dict_data（lunaerp_customer_client_type: 潜在/正式/流失；lunaerp_customer_industry: 纺织/服装/贸易/电商；lunaerp_customer_scale: 大型/中型/小型/微型；lunaerp_customer_source: 主动开发/客户转介绍/展会/线上咨询；lunaerp_customer_cooperation_status: 未合作/合作中/已暂停/已结束）。脚本模式参照 `luna-srv/sql/postgresql/lunaerp-baseinfo-product.sql`
- [ ] 后端 `lunasrv-module-lunaerp` 新增 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.baseinfo.customer` 子包；`controller.baseinfo.customer.vo` 含 `CustomerSaveReqVO`/`CustomerPageReqVO`/`CustomerRespVO`
- [ ] `CustomerDO` 继承 `TenantBaseDO`，`@TableName("lunaerp_baseinfo_customer")`，`@KeySequence("lunaerp_baseinfo_customer_seq")`，12 字段（company/shortName/phone/address/clientType/industry/scale/source/cooperationStatus/unifiedSocialCreditCode/remark/status），无 code 无 businessOwnership 无业务唯一约束
- [ ] `CustomerMapper` 继承 `BaseMapperX<CustomerDO>`，含 `selectPage` 方法支持 `search` 智能词（PostgreSQL 用 `LOWER(...) LIKE LOWER({0})` 对 company/short_name/phone/unified_social_credit_code 做 OR ILIKE）+ `status` 过滤
- [ ] `CustomerService` 接口与 `CustomerServiceImpl` 含 `createCustomer(SaveReqVO)`/`getCustomer(id)`/`getCustomerPage(PageReqVO)`；create 时若 shortName 为空则用 company 回填；status 缺省 ACTIVE
- [ ] `CustomerController` `@RequestMapping("/admin-api/lunaerp/baseinfo/customer")`，`POST /create` + `GET /page` + `GET /get?id=`；`@PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer:query|create')")`
- [ ] `ErrorCodeConstants.java` 末尾追加 `1_030_002_000` CUSTOMER_NOT_EXISTS
- [ ] 前端 `luna-ui/src/api/lunaerp-elmui/baseinfo/customer/index.ts` 含 `getCustomer/getCustomerPage/createCustomer` 三个 API 方法
- [ ] 前端 `luna-ui/src/views/lunaerp-elmui/baseinfo/customer/index.vue` 列表页：5 列精简（company 主显示 + shortName 副标题 / clientType DictTag / phone / cooperationStatus DictTag / createTime）+ 操作列（查看/编辑/删除占位，本票只接"查看"路由跳工作台）+ 搜索区（单 search 输入框 + status 下拉筛选）+ 分页 + 工具栏"新建"按钮（跳 workspace create 模式）；顶部带 `v-hasPermi="['lunaerp:baseinfo:customer:create']"` 权限指令
- [ ] 前端 `luna-ui/src/views/lunaerp-elmui/baseinfo/customer/workspace/index.vue` 工作台容器：7 Tab（基础档案/联系人/合同/寄样/画像分析/位置分布/拜访记录），基础档案 Tab 激活，联系人 Tab 渲染"开发中"占位（本票不实现），后 5 个 Tab 同样"开发中"占位；从路由 `workspace/:id` 取 id 加载 customer 数据；无 id 时为 create 模式
- [ ] 前端 `luna-ui/src/views/lunaerp-elmui/baseinfo/customer/workspace/BasicProfileTab.vue` 基础档案 Tab：el-form 12 字段表单，clientType/industry/scale/source/cooperationStatus 用 `<el-select>` + 字典 options（通过 `useDict` hook 拉取），status 用 ACTIVE/INACTIVE 下拉；create 模式提交调 `createCustomer`，编辑模式提交（02 票接）暂留空；保存成功后 `router.push` 回列表
- [ ] 前端 `luna-ui/src/views/lunaerp-elmui/baseinfo/customer/workspace/PlaceholderTab.vue` 占位组件：el-empty "开发中" 文案
- [ ] 前端路由注册：列表页 path `customer`，name `LunaerpElmuiBaseinfoCustomer`，component `lunaerp-elmui/baseinfo/customer/index`；工作台 path `customer/workspace/:id`，name `LunaerpElmuiBaseinfoCustomerWorkspace`，component `lunaerp-elmui/baseinfo/customer/workspace/index`（参照既有 lunaerp 路由配置位置）
- [ ] 后端测试 `luna-srv/.../src/test/java/.../lunaerp/baseinfo/customer/CustomerControllerTest.java`（若 yudao 有 `BaseDbUnitTest` 之类基类则复用，否则用 `@SpringBootTest` + `MockMvc`）覆盖：`POST /create` 必填校验（company 缺失 + clientType 缺失）+ shortName 为空时自动回填 company + status 缺省 ACTIVE + 成功返回 id；`GET /page` 无数据返回空 + 有数据返回分页 + search 命中 company/shortName/phone/unifiedSocialCreditCode 四种情况 + status 过滤；`GET /get?id=` 存在返回详情 + 不存在返回 1_030_002_000
- [ ] 验收：执行 SQL 脚本无报错（可重复执行）；启动后端 + 前端，登录后导航树出现"基本信息 > 客户信息"菜单；点新建进工作台基础档案 Tab，填表保存成功，列表显示新客户，搜索框按公司名/电话/信用代码均能命中，状态筛选生效
