# 05: 染色单状态操作

**What to build:** 用户在染色单工作台可执行状态流转操作：草稿态点"下达"转进行中（DRAFT→ACTIVE）；进行中态点"完成"转已完成（ACTIVE→COMPLETED）或点"取消"转已取消（ACTIVE→CANCELLED）。非法状态流转（如已完成再下达、已取消再完成）报状态不允许错误。非草稿态工作台全只读，操作按钮按当前状态动态显示。

**Blocked by:** 04 染色颜色明细聚合

**Status:** ready-for-human

- [x] 状态操作接口可用：PUT /effect（DRAFT→ACTIVE）、PUT /complete（ACTIVE→COMPLETED）、PUT /cancel（ACTIVE→CANCELLED）
- [x] 非法状态流转报 1-030-007-001（状态不允许该操作）：如 ACTIVE→DRAFT、COMPLETED→ACTIVE、CANCELLED→ACTIVE、COMPLETED→CANCELLED 等
- [x] 状态操作只修改 status 字段，不触碰其他业务字段
- [x] 前端工作台操作栏按状态动态显示按钮：DRAFT 显示[保存草稿][下达][删除]、ACTIVE 显示[完成][取消]、COMPLETED/CANCELLED 无操作按钮
- [x] 非 DRAFT 态工作台全部控件只读（disabled）
- [x] 列表状态列用 DictTag 渲染四态（草稿 warning/进行中 success/已完成 info/已取消 danger）
- [x] 测试覆盖：三条合法流转、非法流转报状态错误、状态操作不改其他字段
