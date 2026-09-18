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

**租户 (Tenant)**：
多租户通过后端 `TenantBaseDO` 强制（自动注入 `tenant_id` 列、自动过滤查询）。Customer 与 CustomerContact 都继承 `TenantBaseDO`。

**软删除 (Soft delete)**：
记录通过 `BaseDO`/`TenantBaseDO` 继承的 `deleted` int2 列做逻辑删除，查询自动过滤 `deleted = 0`。客户删除时 service 层先置 `status = 'INACTIVE'` 再走逻辑删除（与 product 模块模式一致），便于忽略 `deleted` 的审计/列表查询仍能看到生命周期变化。

## 字典

**客户字典 (Customer dictionaries)**：
客户的 `clientType` / `industry` / `scale` / `source` / `cooperationStatus` 五个枚举性质字段统一接入 yudao 框架的 `system_dict_*` 机制，字典 type 命名前缀 `lunaerp_customer_`：`lunaerp_customer_client_type` / `lunaerp_customer_industry` / `lunaerp_customer_scale` / `lunaerp_customer_source` / `lunaerp_customer_cooperation_status`。前端用 `<DictTag>` 渲染。

## 错误码

**客户模块错误码段位**：`1-030-002-xxx`（与产品模块 `1-030-001-xxx` 平级，customer 与 customer-contact 共用此段）。
