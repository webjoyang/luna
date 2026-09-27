# 06: 责任认定 + 撤销留痕链（含认定链详情抽屉）

**What to build:** 领班在列表对每条待确认行做责任认定——对话框只读展示卷快照，三键选择设备故障（免扣）/操作失误（挡车工扣款，必选挡车工）/免扣不追责，可填原因备注，提交后记录变为已认定并留下认定人与认定时间；已认定记录不能被重复认定。对已认定行可"撤销重录"：原记录冻结为已撤销（所有字段原样保留、记录撤销人与时间），同一卷克隆出一条新的待确认行（卷快照与挡车工沿用、责任与认定字段清空、同挂一条认定链）。点开认定链详情可看到该卷全部历史轮次的时间线。这是扣款凭据链的核心闭环。

**Blocked by:** 04 (手工登记闭环)、05 (从检验单幂等生成)

**Status:** closed

- [x] `POST /confirm`（:confirm 权限）：行不存在报 1_030_014_000；status≠PENDING 报 1_030_014_001（含已认定须先撤销、已撤销不可认定）；非法责任报 1_030_014_004；OPERATOR 缺挡车工报 1_030_014_005
- [x] 认定写 responsibility、worker_user_id/name（经 AdminUserApi 校验+昵称快照）、confirmed_by_user_id/name（当前登录用户）、confirmed_at=服务端当前时间（不接受请求传入）、note、status=CONFIRMED；EQUIPMENT/WAIVED 挡车工可空
- [x] `POST /revoke`（:confirm 权限）：status≠CONFIRMED 报 1_030_014_001（待确认无需撤销/已撤销不可重复撤销）
- [x] 撤销同事务两步：旧行置 REVOKED 且责任/挡车工/认定/备注字段原样不清空、写 revoked_by/name/revoked_at；插入克隆新 PENDING 行（同 chain_key、source/来源关联/卷快照/挡车工沿用、责任与 confirmed_*/note/revoked_* 清空）；返回 {revokedId, freshId}
- [x] `GET /get?id=` 响应在记录字段外附带 chainRecords：同 chain_key 全部行按创建时间正序（含 REVOKED 冻结行）
- [x] 前端认定对话框：卷快照只读区 + 责任三键单选 + 挡车工 el-select（worker-options，仅 OPERATOR 必填）+ 备注；提交后刷新列表
- [x] 操作列：待确认行[认定]；已认定行[查看链][撤销重录]（撤销前二次确认，告知原记录留痕并生成新待确认）；已撤销行[查看链]；均受 :confirm 权限控制
- [x] 认定链详情抽屉：当前有效行摘要 + 时间线列出同链各行（责任/挡车工/认定人/认定时间/撤销人/撤销时间/备注，REVOKED 行置灰）
- [x] Service 单测增补：三类责任认定成功路径与快照、已认定/已撤销再认定报 001、非法责任 004、OPERATOR 无挡车工 005；撤销冻结字段不丢+克隆字段正确+chain_key 一致、PENDING/REVOKED 撤销报 001；get 返回 chainRecords 完整且有序
