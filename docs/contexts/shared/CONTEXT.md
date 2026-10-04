# 跨域共享约定（shared）

本文件收录 Luna ERP **跨所有业务域**的平台约定与共享词汇：多租户、软删除、前后端模块结构约定、错误码段位分配总表。
任何业务域（baseinfo / business / production / stock / sample）的工作开始前都应先读本文件。各业务域自己的术语见根目录 `CONTEXT-MAP.md` 指路的各域 CONTEXT.md。

## 多租户

**租户 (Tenant)**：
多租户通过后端 `TenantBaseDO` 强制（自动注入 `tenant_id` 列、自动过滤查询）。lunaerp 各域主表与子表（如 Customer/CustomerContact、Supplier/SupplierContact 等）都继承 `TenantBaseDO`。

## 软删除

**软删除 (Soft delete)**：
记录通过 `BaseDO`/`TenantBaseDO` 继承的 `deleted` int2 列做逻辑删除，查询自动过滤 `deleted = 0`。客户与供应商删除时 service 层都先置 `status = 'INACTIVE'` 再走逻辑删除（与 product 模块模式一致），便于忽略 `deleted` 的审计/列表查询仍能看到生命周期变化。业务编号（供应商编号、合同编号、胚布单号、发货单号等）的唯一索引与取号扫描显式包含已删除行，软删后编号不释放、不重用——这是该过滤机制的刻意例外（各编号规则见对应业务域 CONTEXT.md 与 ADR-0004/0005/0006/0029）。

## 前后端模块结构约定

**仓库双项目结构**：根目录并列两个项目——`luna-srv`（后端：Spring Boot + PostgreSQL + MyBatis Plus + Redis）与 `luna-ui`（前端：Vue3 + TypeScript + Element Plus）。一个业务域横跨前后端，领域文档统一放在仓库级的 `docs/contexts/<域>/`，不嵌入任一项目内部。

**后端包约定**：lunaerp 业务代码落在 `luna-srv/lunasrv-module-lunaerp`，按"层 → 业务域 → 子模块"组织：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.<域>.<子模块>`，其中业务域固定五个：`baseinfo`（基本信息）、`business`（业务管理）、`production`（生产管理）、`stock`（库存管理）、`sample`（样品管理）。

**临时 UI 区 lunaerp-elmui**：
`luna-ui/src/views/lunaerp-elmui/` 是 `views/` 下的新目录，与既有的 `views/lunaerp/` 平级。它是一个**临时快速实现版**：用 Element Plus 快速搭出业务核心，先跑通前后端服务与关键流程。**未来 lunaerp 业务视图的正式 UI 仍然落到默认目录 `views/lunaerp/`**，`lunaerp-elmui/` 只承担"先行验证"的角色。详见 ADR-0001。产品信息等后建模块已直接落在正式目录 `views/lunaerp/`，各域具体落点以该域 CONTEXT.md 的"模块边界"为准。

## 错误码段位分配总表

lunaerp 错误码统一格式 `1-030-NNN-xxx`，段位（NNN）按业务模块全局分配、不可重号。新增模块取段前先查本表避让：

| 段位 | 所属域 | 模块 |
|---|---|---|
| `1-030-001` | baseinfo | 产品 |
| `1-030-002` | baseinfo | 胚布品种（`GreigeErrorCodeConstants`，随产品模块迁移先行占用） |
| `1-030-003` | business | 销售合同（主表与明细共用） |
| `1-030-004` | baseinfo | 客户（customer 与 customer-contact 共用） |
| `1-030-005` | baseinfo | 供应商（supplier 与 supplier-contact 共用） |
| `1-030-006` | production | 胚布单（主表与颜色分配共用；胚布单自织改造复用本段） |
| `1-030-007` | production | 染色单（主表与颜色明细共用） |
| `1-030-008` | production | 纱线档案（材质+规格+配比BOM+库存台账共用） |
| `1-030-009` | production | 纱线采购单（主表+明细共用） |
| `1-030-010` | production | 检验单（主表与明细共用；检验单匹装改造复用本段） |
| `1-030-011` | stock | 入库单（主表/明细/放置共用；库存扩展复用本段） |
| `1-030-012` | stock | 库存台账/余额（本期只读查询，预留段位） |
| `1-030-013` | stock | 仓库与库位 |
| `1-030-014` | production | 质检确认 |
| `1-030-015` | production | 织造执行域（织造单/机台任务/布票/织机/领纱/IoT 共用，按子资源分段） |
| `1-030-016` | business | 发货单（主表/明细/卷清单共用） |
| `1-030-017` | production | 进度反馈（反馈缸/反馈记录共用） |
| `1-030-018` | sample | 样品分析（主表 + 纱线规格明细/绕纱行/供应商报价三个聚合子表共用） |

各段位的具体错误码明细（000/001/…）见所属业务域 CONTEXT.md 的"错误码"节。

**历史避让记录**：客户最初设计误记为 002 段，落地时发现与胚布品种整段重号，因胚布品种已随主线先行上线，客户（后开发模块）改用 004 段。胚布单设计时误记 005 段空闲，落地前核实发现供应商已占用 005 段，胚布单顺延为 006 段。
