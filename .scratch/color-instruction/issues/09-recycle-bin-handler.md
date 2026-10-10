# 09: 打色单回收站

**What to build:** 注册 `COLOR_INSTRUCTION` 的 `RecycleBinHandler`（统一回收站 SPI）。仅 PENDING 单可软删进站（打色单无 DRAFT，PENDING 为"从未生效"等价物，02 票已实现软删）；恢复为 PENDING 并级联复活颜色行（含历史 REJECTED 轮次行）；引用的客户/产品/供应商若在回收站则阻断恢复（1_030_031_002）。CONFIRMED/IN_PRODUCTION/VOID 不进回收站（作废留单是业务事实）。

**Blocked by:** 02 打色单新建、单号取号与软删

**Status:** resolved

- [x] 注册 `ColorInstructionRecycleHandler` 实现 `RecycleBinHandler` SPI（bizType `COLOR_INSTRUCTION`），落点 `service/production/colorinstruction/`
- [x] 软删进站：仅 PENDING 单可软删（02 票已实现 DELETE 接口与级联逻辑删颜色行）；CONFIRMED/IN_PRODUCTION/VOID 不进回收站（CONFIRMED 走作废，作废留单不进站）
- [x] 恢复：主表 status 复位为 PENDING（deleted=0）、级联复活全部颜色行（含历史 REJECTED 轮次行——按 instruction_id 批量 update deleted=0，不区分行状态）
- [x] 主档阻断守卫：引用的客户/产品/供应商若在回收站（deleted=1）则阻断恢复，报 1_030_031_002（沿用统一回收站错误码）
- [x] 回收站列表项展示：单号/客户/产品/单据日期/删除时间/恢复操作按钮
- [x] 测试覆盖：软删进站可查、恢复主表复位 PENDING + 颜色行级联复活（含 REJECTED 历史轮次行）、主档在回收站阻断恢复报 1_030_031_002、CONFIRMED/IN_PRODUCTION/VOID 不进站（软删被 08 票终态守卫拦截）
