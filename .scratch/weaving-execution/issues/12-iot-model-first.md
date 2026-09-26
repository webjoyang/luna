# 12: IoT 模型先行（凭证/绑定/快照三表 + LoomDataCollector + 手工/模拟采集）

**What to build:** 新建 `production.iot` 子包，落三表（ADR-0028）：`lunaerp_production_vendor_credential`（vendorAccount 租户唯一、**vendorPasswordEnc 加密存储**、scope、status、lastSyncAt）、`lunaerp_production_device_binding`（vendorDeviceId、loomId、status、lastSyncAt，归属权威）、`lunaerp_production_machine_realtime`（(tenant,vendorDeviceId) 唯一：status running/idle/abnormal/disabled/offline、效率/圈速/运转分钟/在班人员/deviceSn、fetchedAt）。端点：凭证 CRUD（密码写入即加密，不回显明文）、绑定 CRUD（同设备重复绑定报 411）、快照分页/最新查询、**手工录入快照**；定义 `LoomDataCollector` 接口并提供模拟实现（按绑定生成模拟值），不启用真实定时任务。前端不单独开菜单：织机档案页内三个 Tab（设备接入凭证/设备绑定/实时快照，快照为辅助列、**不回写 loom.status**）。SQL 文件 `lunaerp-production-iot.sql`；权限码 `lunaerp:production:iot:`（错误码 1-030-015 段 400-420）。

**Blocked by:** 02

**Status:** closed

- [x] 三表 DDL + 凭证/绑定/快照端点
- [x] 凭证密码加密、不回显；绑定重复守卫（411）
- [x] LoomDataCollector 接口 + 模拟实现 + 手工录入
- [x] 织机档案页 IoT 三 Tab；快照不回写织机态
- [x] 单测：加密存储、租户隔离/归属、快照更新
