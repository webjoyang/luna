# 07: 定稿/回退 + 回收站 Handler

**What to build:** 落地单证套的两态状态流转与生命周期回收。`PUT /finalize?id=` DRAFT→FINALIZED，守卫明细非空（EXPORT_DOC_ITEM_EMPTY，对齐 SHIPMENT_AUDIT_NO_ITEMS/STOCK_IN_ITEM_EMPTY 惯例），条件 UPDATE 防并发；`PUT /revoke?id=` FINALIZED→DRAFT，条件 UPDATE。FINALIZED 只锁编辑/删除不锁打印（PI 草稿常需先发客户确认定金，DRAFT/FINALIZED 均可打印）。回收站：实现并注册 `RecycleBinHandler`，bizType=`EXPORT_DOC`，**仅 DRAFT 进站**（FINALIZED 须先 revoke），级联恢复 items，**不补账**（本模块无库存/账务副作用，与 ADR-0038 回收站边界一致）。恢复级联复活 items。回收站 SPI 接口由 common-foundation 05 提供（RecycleBinHandler SPI + 注册表），本票只实现 EXPORT_DOC 的 handler 并注册。单测定稿守卫 + 条件更新 + 回收站级联恢复 + FINALIZED 不可删。

**Blocked by:** 03（Service 骨架 + 状态守卫）, common-foundation 05（RecycleBinHandler SPI + 注册表）

**Status:** ready-for-human

- [x] `PUT /finalize?id=` DRAFT→FINALIZED，守卫明细非空 EXPORT_DOC_ITEM_EMPTY，条件 UPDATE 防并发
- [x] `PUT /revoke?id=` FINALIZED→DRAFT，条件 UPDATE
- [x] FINALIZED 只锁编辑/删除不锁打印（DRAFT/FINALIZED 均可打印）
- [x] RecycleBinHandler(EXPORT_DOC) 注册：仅 DRAFT 进站、级联恢复 items、不补账、FINALIZED 须先 revoke
- [x] 权限 finalize/revoke 复用 `lunaerp:business:export-doc:update`
- [x] 单测：定稿守卫（明细空拒绝）、条件更新防并发、回收站级联恢复 items、FINALIZED 不可删
