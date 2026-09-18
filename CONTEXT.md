# 项目术语表

本文件是 Luna ERP 仓库的权威术语表。这里只收录领域概念，不写实现细节。
当对话或代码中含糊使用某个词时，以此文件为准。

## 客户与联系人

**客户 (Customer)**：
企业或个人，业务销售/服务的对象。区别于 `User`（系统登录身份）：客户是市场侧实体，User 是系统侧身份。一个客户有 12 个基础档案字段（company、shortName、phone、address、clientType、industry、scale、source、cooperationStatus、unifiedSocialCreditCode、remark、status），并可拥有多条 `客户联系人`。
_避免_：Client、BusinessClient、客户主体。旧系统 Prisma 模型叫 `BusinessClient`，新系统正名为 `customer`，理由是中文"客户"对应 `customer` 更直接，同时避开旧系统 `clientType` 字段被业务部隔离逻辑占用导致的命名冲突。

**客户联系人 (CustomerContact)**：
关联到某个客户的人（如采购、财务、仓管）。P1 含 6 个字段：name、position、phone、email、remark、isPrimary。作为独立子资源处理，有自己的 CRUD 端点与权限码，不嵌在父级客户的保存载荷里整体替换。
_避免_：ClientContact、联系人（无前缀时歧义）。

**客户工作台 (CustomerWorkspace)**：
查看/编辑单个客户的多 Tab 容器。7 个 Tab 与旧系统 tab-config 对齐：基础档案 / 联系人 / 合同 / 寄样 / 画像分析 / 位置分布 / 拜访记录。P1 激活"基础档案"与"联系人"，其余 5 个为"开发中"占位。

## 业务管理：销售合同

**销售合同 (SalesContract)**：
与客户签订的布匹销售约定，记载交易条款与商品明细，是业务管理域的第一个单据。
_避免_：合同（无前缀时与未来的采购合同歧义）、订单。

**合同明细 (SalesContractItem)**：
一份销售合同中，按单个品种（客户色号 / 门幅 / 克重组合）逐行记录的数量、单价、其他费用与金额条目。一份合同含 0~N 条明细，允许先建档后补明细。明细是合同聚合的一部分，随合同整体保存、级联删除，没有独立操作界面（与客户联系人的独立资源模式刻意相反，见 ADR-0003）。
_避免_：合同商品、明细行（口语）。

**贸易类型 (TradeType)**：
`DOMESTIC` 内销（缺省）/ `EXPORT` 外销，区分销售方向。MVP 仅作标记；外贸专属字段（贸易术语、港口、英文抬头等）与出口单证在后续版本补。

**订单属性 (OrderType)**：
`BULK` 大货（缺省）/ `SAMPLE` 样布，区分批量生产订单与打样。

**合同状态 (SalesContractStatus)**：
`ACTIVE` 进行中（新建缺省）/ `COMPLETED` 已完成 / `CANCELLED` 已取消。MVP 无审批流，由编辑页手动切换，不做流转约束。旧系统的审批态（draft/submitted/approved/rejected）、业务进度 progress、回款状态 paymentStatus 三套状态在 MVP 均不引入。

**业务员 (Salesperson)**：
负责该合同的内部销售人员，关联 system `User`，可空。区别于客户侧的客户联系人。

**客户名称快照 (clientName)**：
建单时从客户档案复制到合同上的客户公司名。客户档案后续改名不影响既有合同；客户为必选（`customerId` 逻辑关联，不建数据库外键），联系人与地址选客户时带出但可手工修改。

**合同编号 (ContractNo)**：
形如 `CH20260001`：`CH` + 年份 + 4 位顺序号（超 9999 自然扩位），租户内唯一，新建缺省由服务端生成、也可手填（撞号报错）。编号在软删除后也不释放、不重用（见 ADR-0004）。

## 状态与生命周期

**客户状态 (Status)**：
两态：`ACTIVE`（启用，新建缺省） / `INACTIVE`（停用）。旧系统的 `PENDING_APPROVAL` / `REJECTED` 审批态在 P1 不引入（无审批流），后续上审批流时再追加。

**客户简称 (shortName)**：
客户的展示缩写。用户未填时由后端自动用 `company` 回填。列表展示优先用 `shortName || company`。

## 模块边界

**模块落点 (Module placement)**：
客户功能落在 `lunaerp` 模块的 `baseinfo`（基本信息）子包下，前后端一致：

- 后端：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.baseinfo.customer[.contact]`
- 前端视图：`luna-ui/src/views/lunaerp-elmui/baseinfo/customer/`
- 前端 API：`luna-ui/src/api/lunaerp-elmui/baseinfo/customer/`

`lunaerp-elmui`（lunaerp 的 Element-Plus UI 区块）是 `views/` 下的新目录，与既有的 `views/lunaerp/` 平级。它是一个**临时快速实现版**：用 Element Plus 快速搭出业务核心，先跑通前后端服务与关键流程。**未来 lunaerp 业务视图的正式 UI 仍然落到默认目录 `views/lunaerp/`**，`lunaerp-elmui/` 只承担"先行验证"的角色。详见 ADR-0001。

销售合同落在 `business`（业务管理）子包，与 `baseinfo` 平级：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.business.salescontract[.item]`，前端 `lunaerp-elmui/business/sales-contract/`（视图与 API 目录镜像）。

**租户 (Tenant)**：
多租户通过后端 `TenantBaseDO` 强制（自动注入 `tenant_id` 列、自动过滤查询）。Customer 与 CustomerContact 都继承 `TenantBaseDO`。

**软删除 (Soft delete)**：
记录通过 `BaseDO`/`TenantBaseDO` 继承的 `deleted` int2 列做逻辑删除，查询自动过滤 `deleted = 0`。客户删除时 service 层先置 `status = 'INACTIVE'` 再走逻辑删除（与 product 模块模式一致），便于忽略 `deleted` 的审计/列表查询仍能看到生命周期变化。

## 字典

**客户字典 (Customer dictionaries)**：
客户的 `clientType` / `industry` / `scale` / `source` / `cooperationStatus` 五个枚举性质字段统一接入 yudao 框架的 `system_dict_*` 机制，字典 type 命名前缀 `lunaerp_customer_`：`lunaerp_customer_client_type` / `lunaerp_customer_industry` / `lunaerp_customer_scale` / `lunaerp_customer_source` / `lunaerp_customer_cooperation_status`。前端用 `<DictTag>` 渲染。

**销售合同字典 (Sales contract dictionaries)**：
合同的枚举字段同样走 `system_dict_*`，前缀 `lunaerp_sales_contract_`：`lunaerp_sales_contract_status`（ACTIVE/COMPLETED/CANCELLED）、`lunaerp_sales_contract_trade_type`（DOMESTIC/EXPORT）、`lunaerp_sales_contract_order_type`（BULK/SAMPLE）、`lunaerp_sales_contract_unit`（KG/M）、`lunaerp_sales_contract_currency`（RMB/USD/EUR/HKD）。

## 错误码

**客户模块错误码段位**：`1-030-004-xxx`（customer 与 customer-contact 共用此段）。段位全表：`1-030-001-xxx` 产品、`1-030-002-xxx` 胚布品种（`GreigeErrorCodeConstants`，随产品模块迁移先行占用）、`1-030-003-xxx` 销售合同（主表与明细共用）、`1-030-004-xxx` 客户。客户最初设计误记为 002 段，落地时发现与胚布品种整段重号，因胚布品种已随主线先行上线，客户（后开发模块）改用 004 段。
