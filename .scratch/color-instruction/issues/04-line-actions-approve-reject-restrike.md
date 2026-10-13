# 04: 行级动作：通过 / 驳回重打（含开始打色）

**What to build:** 用户在打色单工作台执行状态流转：点"开始打色"将单据 PENDING→IN_PROGRESS；对 PENDING 颜色行点"通过"置 APPROVED（单据仍 PENDING 时自动转 IN_PROGRESS 便利规则）；对 PENDING 或 APPROVED 行点"驳回重打"——旧行置 REJECTED 冻结并记录驳回意见，同事务克隆新行继承色号/颜色名/厂内色号/色值/主辅灯源/色卡参考/份数、`round=旧行 round+1`、`status=PENDING`、**不继承样布照片**。REJECTED 行不可再"重打"（如需三轮，对最新 PENDING 行继续驳回重打）。所有状态更新走条件更新（带 `status=? AND tenant_id=? AND deleted=0`），影响行数 0 报 1_030_032_002，天然防并发覆盖。前端按色组展示多轮行，REJECTED 行置灰划线标注轮次与驳回意见。

**Blocked by:** 03 打色单查看与修改（含差异保存与行状态守卫）

**Status:** completed

- [x] "开始打色"接口 PUT `/start`（`:update` 权限）：PENDING→IN_PROGRESS，条件更新 `WHERE status='PENDING' AND tenant_id=? AND deleted=0`，影响行数 0 报 1_030_032_002
- [x] "通过"接口 PUT `/color/approve`（`:confirm` 权限，body: 行 id）：行 PENDING→APPROVED；便利规则——单据仍 PENDING 时自动转 IN_PROGRESS（同事务或同进程内级联条件更新）
- [x] "驳回重打"接口 PUT `/color/reject-restrike`（`:confirm` 权限，body: 行 id, rejectReason）：允许对 PENDING 或 APPROVED 行使用；旧行置 REJECTED 冻结 + 记录 rejectReason；同事务克隆新行继承 customerColorNo/colorName/factoryColorNo/colorHex/lightPrimary/lightSecondary/colorCardRef/copies，`round=旧行 round+1`、`status=PENDING`，**samplePhotos 不继承**；服务端重排 lineNo（同色号多轮相邻、最新轮在前）
- [x] REJECTED 行不可再"驳回重打"（报 1_030_032_006 行状态不允许）；如需三轮，对最新 PENDING 行继续驳回重打
- [x] 全部状态动作走条件更新，影响行数 0 报 1_030_032_002；行级动作幂等性（重复 approve 报状态错误）
- [x] 全部写动作加 `@LogRecord`（如 `success="通过打色行 {{#customerColorNo}}"`），入 `system_operate_log`
- [x] 前端编辑页页头"开始打色"按钮（PENDING 态显示）；颜色行操作列"通过"/"驳回重打"按钮（`v-hasPermi` confirm 码，驳回弹窗填 rejectReason）；按色组展示多轮行（REJECTED 行置灰、划线、显示轮次与驳回意见）
- [x] 测试覆盖：三条合法行流转（PENDING→APPROVED、PENDING→REJECTED+克隆、APPROVED→REJECTED+克隆）、驳回重打克隆内容与轮次（round+1、samplePhotos 不继承）、REJECTED 不可再重打、便利规则（approve 时单据 PENDING 自动转 IN_PROGRESS）、条件更新防并发（影响行数 0 报错）、@LogRecord 留痕

