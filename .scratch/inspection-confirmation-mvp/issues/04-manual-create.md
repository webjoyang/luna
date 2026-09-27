# 04: 手工登记闭环（文员代录，提交即已认定，第一条可造数纵向链路）

**What to build:** 文员在质检确认列表页点"手工登记"（:create 权限），在弹窗填写卷号、等级、责任类型（设备故障/操作失误/免扣三选一）及选填信息（缸号/机台/产品/颜色/重量/米数/关联检验单/备注），可把认定时间改为过去时间补录；提交后系统直接产生一条"已认定"记录（来源=手工登记），列表刷新即见。选"操作失误"时必须从挡车工下拉选人，否则前后端均拦截。本票完成即可端到端演示"登记 → 可查"闭环。

**Blocked by:** 03 (列表只读切片)

**Status:** closed

- [x] `POST /lunaerp/production/inspection-confirmation/manual-create`（:create 权限）
- [x] 必填校验：rollNo、grade（A/B/C/DEFECTIVE）、responsibility（三值）；responsibility=OPERATOR 时 workerUserId 必填——缺项分别报 1_030_014_006 / 1_030_014_005
- [x] OPERATOR 时经 AdminUserApi.validateUser 校验挡车工有效并写 worker_name 昵称快照；EQUIPMENT/WAIVED 时挡车工可空
- [x] 落库：source=MANUAL、status=CONFIRMED、新 chain_key、confirmed_by=当前登录用户（id+姓名快照）、confirmedAt 缺省服务端当前时间、允许传过去时间补录
- [x] inspectionOrderId 可选：传则校验检验单存在并快照 code（不存在报 1_030_014_002），不校验卷是否在该单内
- [x] 非法 responsibility 报 1_030_014_004
- [x] 前端手工登记对话框：字段与校验同后端；责任三选一单选，挡车工下拉仅 OPERATOR 显示且必填，认定时间日期时间选择（缺省现在、不允许未来）；成功后关闭弹窗、刷新列表、ElMessage 成功
- [x] 列表"手工登记"按钮受 :create 权限控制；新出现的行来源列显示"手工登记"标签、状态为已认定
- [x] Service 单测增补：缺必填三分支、OPERATOR 缺挡车工、成功落 CONFIRMED/MANUAL/新 chain_key/认定人、补录时间采用、关联检验单不存在报错
