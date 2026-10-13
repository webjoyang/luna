# 05: 确认动作与三道守卫

**What to build:** 用户在 IN_PROGRESS 态打色单点"确认"时，服务端校验三道守卫：单据状态为 IN_PROGRESS（CONFIRMED 重复确认报错）、无 PENDING 行（存在未决行报 1_030_032_004）、至少 1 行 APPROVED（全部驳回不能确认，报同一错误码）。通过后弹窗必选确认类型（CUSTOMER 客户确认 / INTERNAL 内部确认）+ 可空客户反馈意见，写入 confirmedBy、confirmedAt，status=CONFIRMED。条件更新防并发覆盖。

**Blocked by:** 04 行级动作：通过 / 驳回重打（含开始打色）

**Status:** completed

- [x] "确认"接口 PUT `/confirm`（`:confirm` 权限，body: id, confirmType, customerFeedback）：三道守卫依次校验——单据 IN_PROGRESS（CONFIRMED 重复确认报 1_030_032_002）、无 PENDING 行 + 至少 1 行 APPROVED（任一不满足报 1_030_032_004）
- [x] 通过后写入 `confirmType`（CUSTOMER/INTERNAL，弹窗必选，后端枚举校验，非法报 1_030_032_007）、`customerFeedback`（可空）、`confirmedBy`（当前用户 id）、`confirmedAt`（服务端 now），`status=CONFIRMED`
- [x] 条件更新 `WHERE status='IN_PROGRESS' AND tenant_id=? AND deleted=0`，影响行数 0 报 1_030_032_002（防并发覆盖与重复确认）
- [x] `confirmType` 为后端内部枚举，不入字典（同各单 source/type 先例）；前端枚举常量定义
- [x] `@LogRecord`（如 `success="确认打色单 {{#instructionNo}}"`）入 `system_operate_log`
- [x] 前端编辑页页头"确认"按钮（IN_PROGRESS 态显示，`v-hasPermi` confirm 码）：弹窗必选确认类型（单选 CUSTOMER/INTERNAL）+ 可空反馈意见 textarea；确认成功后单据转只读（CONFIRMED 态全只读，回退留 08 票）
- [x] 测试覆盖：三道守卫各失败路径（CONFIRMED 重复确认、存在 PENDING 行、全部 REJECTED 无 APPROVED）、确认成功写入四字段、confirmType 非法枚举报 1_030_032_007、条件更新防并发
