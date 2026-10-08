# 02: 审批链配置 CRUD（后端）

**What to build:** 管理员可通过后端 API 对审批流程定义及其节点做完整 CRUD：新建带节点的流程、整体替换更新节点、启用/停用切换、删除未被引用的流程。同 `biz_type` 同一时刻只允许一条启用流程；已产生审批实例的流程不允许物理删除，只能停用。

**Blocked by:** 01 (SQL 基建)

**Status:** ready-for-agent

- [ ] 后端包 `com.luna.lunasrv.module.lunaerp.approval` 下 config 子包：`ApprovalConfigDO` / `ApprovalConfigNodeDO`、Mapper、Service、Controller、VO（SaveReqVO 含节点列表整体替换）
- [ ] `GET /lunaerp/approval/config/page` 分页查询（按 biz_type 筛选）
- [ ] `GET /lunaerp/approval/config/{id}` 详情（含节点列表按 sort 排序）
- [ ] `POST /lunaerp/approval/config` 新建流程（含节点）
- [ ] `PUT /lunaerp/approval/config/{id}` 更新流程（节点整体替换：删旧插新）
- [ ] `PUT /lunaerp/approval/config/{id}/status` 启用/停用
- [ ] `DELETE /lunaerp/approval/config/{id}` 删除（仅未被实例引用）
- [ ] 业务守卫：启用时若同 biz_type 已有 ENABLED 流程，报 `1_030_030_007`；删除时若已有实例引用，报 `1_030_030_008`
- [ ] 错误码常量类 `ApprovalErrorCodeConstants` 注册 `1_030_030_000`~`009`（本票用到 000/007/008，其余预留占位）
- [ ] 全部表继承 TenantBaseDO，写接口租户隔离（租户隔离测试覆盖 config CRUD）
- [ ] 列表/详情/删除进回收站：config 软删，不物理删
