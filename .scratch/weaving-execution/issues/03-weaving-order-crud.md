# 03: 织造单主表 CRUD（从自织胚布单创建 + 派生编号 + 数量软校验）

**What to build:** 新建 `production.weavingorder` 子包与 `lunaerp_production_weaving_order` 表，全套 CRUD（分页/详情/新建/更新/删除/取消）。字段：`code`、`greigeOrderId`（必填，限 productionMode=SELF 且未取消）、`contractNo`、`productId`+`productCode`+`productName`、`composition`、`gsm`、`width`、`plannedQty`（计划匹数，正整数）、`plannedWeight`（KG）、`status`（DRAFT/ACTIVE/COMPLETED/CANCELLED，缺省 DRAFT）、`remark` + TenantBaseDO。创建时校验胚布单存在且为 SELF（否则 1-030-015-003），从胚布单/产品带快照；聚合该胚布单已有织造单 plannedQty，超量**警告但允许**（返回 warning 字段）。编号**派生式**：`ZZ-{胚布单号去符号}-{该胚布单内 2 位序号}`，服务端生成、可手填（撞号 001）、软删不释放；并发撞唯一键顺延重试。DRAFT 可改可删；删除级联逻辑删除任务/布票；CANCELLED 手动终态。状态自动推导（DRAFT→ACTIVE→COMPLETED）在 issue 05 随布票操作落地，本票先实现手动取消。建表 SQL 文件：`luna-srv/sql/postgresql/lunaerp-production-weaving.sql`（本 issue 建文件，含织造单表；任务/布票表在 04/05 追加）。菜单 10280 + 10281-10284。

**Blocked by:** 01

**Status:** closed

- [x] 织造单表 + CRUD/取消端点；SELF 胚布单存在性校验（003）
- [x] 派生编号 ZZ-{胚布单}-{2位}，手填/重试
- [x] 超量软警告；快照从胚布单/产品带入
- [x] DRAFT 可删且级联任务/布票逻辑删除
- [x] SQL 文件 + 菜单/权限（10280-10284，字典 id=2238）
- [x] 单测：编号生成/撞号、SELF 校验、级联删除
