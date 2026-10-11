# 04: 成品检验单便利生成

**What to build:** 染色单 COMPLETED 后，操作员在染色单工作台点「生成成品检验单」，系统从染色单带出产品/客户/合同信息创建一张 FINISHED 类型、DRAFT 状态的成品检验单；同一染色单已存在成品检验单时不重复创建。手动触发，非状态变更自动触发。

**Blocked by:** 01 (SQL 基建 + DO 字段变更/按钮权限)

**Status:** complete

- [x] `POST /lunaerp/production/release-production/create-finished-inspection?dyeingOrderId=`：
  - 从染色单带出 productId、customerId、contractId、dyeingOrderId
  - 创建 type=FINISHED、status=DRAFT 检验单，targetWeight=染色单数量合计
  - 已存在同 dyeingOrderId 的 FINISHED 非删除检验单则报 `1_030_035_005`，不重复创建
- [x] 染色单工作台 COMPLETED 态操作栏加「生成成品检验单」按钮，成功后跳转检验单工作台
- [x] `@LogRecord` 记录生成操作
- [x] 权限受 `lunaerp:production:release-production:release` 控制，无权限按钮不显示
- [x] Service 单测覆盖：正常生成带出字段、重复创建报 005、染色单不存在/非 COMPLETED 的守卫
