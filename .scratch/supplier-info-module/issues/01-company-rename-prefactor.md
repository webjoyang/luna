# 01: 公司名字段统一正名 company→companyName（先行重构）

**What to build:** 在不改变客户信息与销售合同任何现有功能行为的前提下，把客户的"公司名称"字段在全链路统一正名为 `companyName`（数据库列 `company_name`），为供应商模块（同样使用 companyName）确立统一词汇。重构后：客户建档/编辑/列表/搜索/简称回填行为不变；销售合同上的客户名称快照(clientName)回填结果不变；全新库与已安装客户模块的存量库都能靠 SQL 脚本平滑升级。

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

- [x] 客户 DO/请求 VO/响应 VO 的公司名字段为 companyName；保存时公司名必填校验仍生效
- [x] 客户分页搜索的智能搜索词仍命中公司名（列 company_name）、简称、电话、信用代码，搜索结果与重构前一致
- [x] 客户新建/编辑时简称留空仍自动用公司名回填，填了简称则保留
- [x] 销售合同新建/编辑时客户名称快照仍正确取客户公司名回填（仅取值入口改名，快照字段 clientName 不变）
- [x] 前端客户列表的公司名列、简称副标题、基础档案 Tab 表单字段与校验、客户类型定义均使用 companyName，页面表现不变
- [x] 前端销售合同编辑页的客户选择器选项仍正确显示客户公司名
- [ ] 客户建表脚本的列定义与列注释为 company_name；脚本内含可重复执行的列迁移逻辑（information_schema 判存后 RENAME COLUMN company→company_name），在空库与存量库各重跑一次均成功且不丢数据（迁移块已编写并静态自查三态幂等；本机无 docker/psql，未做真实 PG 双库重跑实证）
- [x] 客户与销售合同两个既有 Service 单元测试同步改为 companyName 并全部通过；后端模块全量编译、测试为绿，前端 typecheck/build 为绿（lunaerp 52 例全绿、vite build 绿；ts:check 存量报错均在无关遗留模块，改动文件零报错）
- [x] 全仓库不再残留客户语义的 company 字段/列引用（供应商开工前的基线检查；迁移脚本中保留的旧列名 company 为迁移逻辑本身，产品模块 supplierCompany 为供应商占位字段）

## Comments

- 2026-09-19 实现完成，提交 7490a32（16 文件，+73/-53）：后端 DO/VO/Mapper/Service 与销售合同两处快照取值改名、SQL 列改名 + information_schema 判存的可重复执行 RENAME 迁移块、前端类型/视图/选择器联动、两个 Service 测试同步。
- 验证：`mvn test -pl lunasrv-module-lunaerp` 52 例全绿（Customer 9 / CustomerContact 14 / SalesContract 29）；`pnpm build:local` 两次通过；全仓基线 grep 无客户语义 company 残留。
- 遗留：SQL 空库/存量库真实重跑待具备 PostgreSQL 环境时补验（块逻辑为标准 PL/pgSQL DO $$ + information_schema 判存，位于 CREATE TABLE 之后、COMMENT 之前）。
