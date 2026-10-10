# 03: 打色单查看与修改（含差异保存与行状态守卫）

**What to build:** 用户可在列表点编辑进入打色单详情/编辑页，查看单据头 + 颜色行（按 lineNo 排序，含历史轮次行），在 PENDING/IN_PROGRESS 态整体编辑保存。颜色行改为**差异保存**：未改行保留原 id（供后续行级动作与打印稳定关联），新增行插入（round=1、status=PENDING），PENDING 行可改可删，APPROVED/REJECTED 冻结行提交任何字段变更报 1_030_032_006。服务端按 `customerColorNo` 分组重排 lineNo（同色号多轮相邻、最新轮在前、从 1 连续编号）。CONFIRMED 后须先回退（07 票）再改。

**Blocked by:** 02 打色单新建、单号取号与软删

**Status:** resolved

- [x] 详情接口 GET `/get?id=`（`:query` 权限）嵌套返回单据头 + 颜色行（按 lineNo 排序，含 REJECTED 历史轮次行）
- [x] 更新接口 PUT `/update`（`:update` 权限）实现差异保存：保存 VO 颜色行携带可空 id；服务端同事务内三分流——无 id 行插入（round=1、status=PENDING）、带 id 行更新（仅 PENDING 行可改）、请求缺失的持久化行删除（仅 PENDING 行可删）
- [x] 行状态守卫：对 APPROVED/REJECTED 行提交任何字段变更报 1_030_032_006；冻结行缺失不处理（不报错）；显式提交删除冻结行报 1_030_032_006
- [x] lineNo 重排：按 `customerColorNo` 分组、同色号多轮相邻、最新轮在前、从 1 连续编号
- [x] 编辑守卫：仅 PENDING/IN_PROGRESS 可整体编辑；CONFIRMED/IN_PRODUCTION/VOID 报 1_030_032_002（终态守卫代码可前置，运行时回归依赖 06b/07 落地）
- [x] 跨父非法 id（提交的行 id 不属本单）报错
- [x] 前端编辑页查看/编辑双模式：颜色行稳定 row-key（用 id），加载→改行→加色→删色→调序→保存→重开 行 id 稳定、数据不串；冻结行（APPROVED/REJECTED）置灰只读、显示驳回意见
- [x] 前端列表"编辑"按钮（`v-hasPermi` update 码）；非可编辑态点编辑进入只读模式
- [x] 测试覆盖：差异保存三分流（未改行 id 与审计字段不变、新增行插入、删除行级联）、lineNo 重排（同色号多轮相邻、最新轮在前）、冻结行守卫（改/删均报 1_030_032_006）、跨父非法 id、编辑守卫（终态报错）
