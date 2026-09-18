# 01: 销售合同骨架端到端（主表 CRUD 跑通）

**What to build:** 用户能在新菜单"业务管理 → 销售合同"里完成一份**只有基本信息**（暂不含商品明细）的销售合同的新建、列表检索、查看、编辑、删除闭环。整页新建表单中客户为必选（从客户档案远程搜索），业务员可从系统用户远程选择，合同编号缺省自动生成（形如 CH20260001）也能手填，枚举字段全部走系统字典。保存后合同出现在列表，可按关键词/状态/贸易类型筛选、分页、默认签订日期倒序。

**Blocked by:** None（can start immediately）

**Status:** ready-for-human（代码实现与单测完成，运行时验收待人工执行，见 Comments）

- [x] 新建可重复执行的 PostgreSQL 脚本 `luna-srv/sql/postgresql/lunaerp-business-sales-contract.sql`（体例对齐 lunaerp-baseinfo-customer.sql：CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 序列 + 全列 COMMENT + 事务内先 DELETE 再 INSERT 菜单/字典）
- [x] 合同主表 `lunaerp_business_sales_contract` DDL（继承 TenantBaseDO 的标准列）：contract_no varchar(32) 必填、customer_id int8 必填、client_name varchar(128) 必填（快照）、client_contact、client_address、client_contract_no、salesperson_user_id int8 可空、sign_date date 必填、delivery_date date 可空、unit 必填缺省 KG、currency 必填缺省 RMB、trade_type 必填缺省 DOMESTIC、order_type 必填缺省 BULK、total_quantity/total_amount numeric(18,2) 必填缺省 0（本票先固定为 0，02 票接管重算）、remarks varchar(1000)、status 必填缺省 ACTIVE
- [x] 主表索引：普通唯一索引 `(tenant_id, contract_no)`——**不带 WHERE deleted=0**（ADR-0004，软删不释放编号）；辅助索引 `(status)`、`(sign_date)`；序列 `lunaerp_business_sales_contract_seq`
- [x] 菜单：10100 `业务管理` 目录（type=1，path `/business`，icon `ep:briefcase`，与 10000 基本信息平级）；10110 `销售合同` 菜单（type=2，parent=10100，component `lunaerp-elmui/business/sales-contract/index`，componentName `LunaerpElmuiBusinessSalesContract`，承载 query 权限）；10111/10112/10113 创建/修改/删除按钮
- [x] 5 个字典初始化：`lunaerp_sales_contract_status`（ACTIVE 进行中 success / COMPLETED 已完成 info / CANCELLED 已取消 danger）、`lunaerp_sales_contract_trade_type`（DOMESTIC 内销 / EXPORT 外销）、`lunaerp_sales_contract_order_type`（BULK 大货 / SAMPLE 样布）、`lunaerp_sales_contract_unit`（KG 千克 / M 米）、`lunaerp_sales_contract_currency`（RMB/USD/EUR/HKD）；dict_type id 2210-2214、dict_data id 从 3530 起
- [x] 后端新增子包 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.business.salescontract`：`SalesContractDO`（继承 TenantBaseDO，@TableName + @KeySequence）、`SalesContractMapper`（继承 BaseMapperX）、Service/ServiceImpl、Controller；VO 含 SaveReqVO / PageReqVO / RespVO，带 Swagger 与校验注解，风格对齐 customer 模块
- [x] Controller 基础路径 `/admin-api/lunaerp/business/sales-contract`，本票实现 `GET /page`、`GET /get`、`POST /create`、`PUT /update`、`DELETE /delete`；全部带 `@PreAuthorize("@ss.hasPermission('lunaerp:business:sales-contract:<action>')")`
- [x] `GET /page` 参数 pageNo/pageSize/keyword/status/tradeType/**customerId（预留，本票只需透传到查询构造，无 UI 入口）**；keyword 对 contract_no / client_name / client_contract_no 做 OR 模糊（LOWER+LIKE）；默认 sign_date DESC；RespVO 不含明细
- [x] 编号生成（沿用产品模块 getNextCode 数字后缀扫描方式，**不引 Redis**）：contractNo 为空时生成 `CH` + 当前年份 + 4 位顺序号（超 9999 自然扩位）；取号扫描与手填查重**必须包含已删除行**（绕过逻辑删除自动过滤的自定义查询，代码处注释 ADR-0004 意图）；手填撞号（含已删除合同）报 1_030_003_001，不顺延；自动号撞唯一索引有限次顺延重试
- [x] 校验与默认值：customerId 必填；客户存在性校验（引用客户库）；signDate 缺省今天；status/tradeType/orderType/unit/currency 缺省值如上；更新/删除/获取不存在合同报 1_030_003_000
- [x] 错误码追加到 lunaerp 模块 ErrorCodeConstants：`1_030_003_000` SALES_CONTRACT_NOT_EXISTS、`1_030_003_001` SALES_CONTRACT_NO_DUPLICATE
- [x] 删除即逻辑删除主表（本票无明细）；不做状态联动、不区分当前状态
- [x] 前端 API 与类型 `api/lunaerp-elmui/business/sales-contract/index.ts`：page/get/create/update/del 五个方法；`utils/dict.ts` 增加 5 个 DICT_TYPE 枚举
- [x] 列表页 `views/lunaerp-elmui/business/sales-contract/index.vue`（useTable + useCrudSchemas 模式对齐 customer）：搜索区关键词 + 状态 + 贸易类型；7 列——合同号 / 客户 / 贸易类型(DictTag) / 签订日期 / 总数量 / 总金额（币种+千分位两位小数）/ 状态(DictTag) + 操作列（查看/编辑/删除，删除二次确认）；新建/删除按钮带 v-hasPermi；订单属性不进列表
- [x] 整页表单 `views/lunaerp-elmui/business/sales-contract/edit/index.vue`：基本信息分区（客户远程 el-select 必选、业务员远程下拉可空、编号手填+自动占位、签订/交货日期、单位/币种/贸易类型/订单属性/状态字典下拉、客户合同号、联系人、地址、备注）；remaining.ts 注册隐藏路由 `/business/sales-contract/edit/:id?`（name `LunaerpElmuiBusinessSalesContractEdit`，无 id 新建/有 id 编辑，"查看"以同一页只读模式打开，全部控件 disabled、隐藏保存）
- [x] 本票客户远程选择只要求能搜索选中并保存 customerId + clientName 快照；"选中后自动带出联系人/地址"的回填增强属于 03 票，本票不做
- [x] Service 层纯 Mockito 单元测试 `SalesContractServiceImplTest`（对齐 CustomerServiceImplTest 写法，不启 Spring/不连库）：自动号格式与序号 +1、扫描入口包含已删除行、手填号撞已删除合同抛 1_030_003_001、更新/删除不存在抛 1_030_003_000、customerId 必填、signDate/status/tradeType/orderType/unit/currency 默认值、keyword/status/tradeType/customerId 透传查询构造
- [ ] 验收：重跑 SQL 脚本不报错且菜单/字典不重复；启动前后端，导航树出现"业务管理 > 销售合同"；新建合同（留空编号）保存成功且编号为 CH 当年 0001 形态，列表可见、三种筛选与分页生效、查看只读、编辑改字段生效、删除后列表消失且该编号无法被新合同重用；无对应权限的角色看不到按钮/接口被拒

## Comments

- 2026-09-18（agent）：代码实现完成。落点：SQL 脚本、后端 `business.salescontract` 四层 + Controller 五接口 + 错误码、前端 API/dict/列表页/整页表单/隐藏路由、`SalesContractServiceImplTest` 19 例全部通过；lunaerp 模块全量单测（42 例）通过；前端 vue-tsc 对新增文件零错误（存量错误属其他模块，不属本票范围）。取号/查重用 `@Select` 手写 SQL 绕过逻辑删除过滤以包含已删除行（ADR-0004，代码处已注释）。列表页实际对齐 customer 现行实现（el-table + Pagination，customer 未用 useCrudSchemas）。"查看"经 `?mode=view` 同页只读。运行时验收（SQL 重跑、启服务走 UI 闭环、权限拒绝）待人工执行后勾选末项。
