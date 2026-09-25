# 05: 审核过账与红冲（三态状态机 + 库存流水写入）

**What to build:** 入库单引入严格三态状态机。草稿态点"审核"（需 `:audit` 权限）→ 单据转已审核，同事务按库位分配展开写正数 STOCK_IN 流水（有放置按放置逐条、无放置按明细行一条且库位为空）；已审核单点"取消"（需 `:audit` 权限）→ 红冲，同事务按关联单号反查原流水逐条插入负数反向行（STOCK_IN_REVERSAL），原流水永不物理删除，单据转已取消终态；草稿态点"取消"（需 `:update` 权限）→ 仅作废，不写流水。非草稿调用编辑/删除报"当前状态不允许该操作"（002）；零明细审核报"入库单没有明细"（005）。前端操作栏按状态+权限渲染按钮，红冲需二次确认并提示库存影响。

**Blocked by:** 04 (库位分配)

**Status:** done

- [x] 三态状态机：DRAFT/COMPLETED/CANCELLED 严格守卫，非草稿禁改禁删
- [x] audit 端点：DRAFT→COMPLETED，至少一条明细，同事务写正数流水（按 placement 展开）
- [x] cancel 端点：DRAFT→CANCELLED 不写流水；COMPLETED→CANCELLED 红冲（反查原流水逐条写负数）
- [x] 权限分权：审核/红冲走 `:audit`，草稿作废走 `:update`
- [x] 前端操作栏按状态+权限渲染，红冲二次确认提示
- [x] StockInServiceImplTest 增补状态机/过账/红冲用例
