# 06: 轻量签收登记（已发货单补录签收状态/签收人/日期）

**What to build:** 在已发货（COMPLETED）发货单的"基本信息"Tab 签收信息区，文员可登记或修改签收：签收状态（待签收 PENDING/已签收 SIGNED/拒签 REFUSED）、签收人、签收日期、备注。新建单签收状态缺省待签收。仅 COMPLETED 可登记/修改，草稿与已取消单登记报 ...016_002；签收状态非法报 ...016_006。拒签只登记状态，**不自动红冲库存**，退货由人执行取消。列表与工作台以独立标签展示签收状态。

**Blocked by:** 04 (审核出库过账与红冲)

**Status:** ready-for-agent

- [ ] update-sign 端点：仅 COMPLETED 可登记/修改 signStatus/signedByName/signedAt/signRemark，鉴权复用 `:update`
- [ ] 状态守卫：DRAFT/CANCELLED 登记报 ...016_002；签收状态非法报 ...016_006
- [ ] 拒签只登记、不自动红冲、不触发任何库存动作
- [ ] 新建单签收状态缺省 PENDING
- [ ] 前端签收信息区（仅 COMPLETED 可编辑）+ 列表签收状态独立标签
- [ ] Service 层 Mockito 单测：签收守卫、非法状态、拒签不产生流水
