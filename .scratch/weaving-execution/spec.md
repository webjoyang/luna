# 生产管理 - 织造执行（胚布单自织改造 + 织造三层对象 + 领纱发纱 + 检验/入库联动 + IoT）

Status: ready-for-agent

## Problem Statement

业务侧需要在新 Luna ERP 系统中管理**自有织造车间的生产执行**。旧系统（linaagi，Prisma + Express + React）已有完整链路：

```
GreigeInstruction（胚布指示单/自产计划）
  └─ WeavingBatch（织造批次）
       └─ MachineTask（机台任务：排产到织机）
            └─ WeavingTicket（布票：每匹一码，扫码开工/完工/异常）
```

并附带织机档案、领纱单/发纱（扣纱线库存）、IoT 设备数据接入（厂商凭证/设备绑定/实时快照）、完工交接胚布检验、检验后胚布入库等环节。旧系统与新系统（Spring Boot + MyBatis-Plus + PostgreSQL + Vue3 + Element Plus）技术栈不兼容，需要按新项目规范重新实现。

## Solution

总体方案（设计依据：ADR-0022 ~ ADR-0028；术语以根 GLOSSARY.md 为准）：

1. **胚布单扩展自织模式**（ADR-0022）：加 `productionMode`（SELF 自织 / OUTSOURCE 外发，缺省），自织时承织坯布厂可空；自织胚布单是织造单的唯一合法来源。
2. **织造三层对象**（ADR-0023）：织造单 `WeavingOrder`（从自织胚布单创建）→ 机台任务 `MachineTask`（排产：哪台机织多少匹）→ 布票 `WeavingTicket`（**每匹一码**，票号=二维码内容）。数量超量一律软校验（警告不拦截）。
3. **状态机**（ADR-0024）：布票七态（GENERATED/STARTED/COMPLETED/INSPECTED/STOCKED/EXCEPTION/VOID），扫码动作白名单守卫；机台任务与织造单状态由布票在同事务自动推导；任务 HALTED 为人工终态须手动恢复。
4. **检验单卷/匹双模式**（ADR-0025）：加 `packageMode` 与明细 `pieceNo`；交接检验只 find-or-create 匹装表头、明细检验时逐匹产生；检验完成后可显式生成胚布入库单草稿。
5. **纱线入通用库存**（ADR-0026）：入库明细/流水加 `itemType`（PRODUCT/YARN_SPEC）+ `yarnSpecId`，与 product 互斥；余额聚合键扩展。
6. **领纱发纱**（ADR-0027）：领纱单挂织造单、明细独立子资源、按配比 BOM 后端测算；发纱写 YARN_OUT、不足拦截、支持撤销红冲。
7. **IoT**（ADR-0028）：厂商凭证（密码加密）/设备绑定/实时快照三表先行；`LoomDataCollector` 接口 + 手工录入/模拟实现，真实采集后置；快照不回写织机状态。

## 分期与里程碑

| 期 | 范围 | 对应 issues | 验收标志 |
| --- | --- | --- | --- |
| **P1** | 胚布单自织改造 + 织机档案 + 织造单/机台任务/布票核心 + 前端工作台 + 检验单匹装改造 | 01-07 | 自织胚布单→排产→出票→逐匹开工/完工，全链路在 Web 端可操作 |
| **P2** | 库存 itemType 扩展 + 领纱单/BOM 测算 + 发纱/红冲 | 08-10 | 纱线可入库，可按织造单领纱、仓库发纱，库存查询可见纱线余额与流水 |
| **P3** | 完工交接胚布检验 + 检验生成胚布入库单 + IoT 模型先行 | 11-12 | 完工匹票可交接并逐匹检验，合格明细可生成生产入库单；织机页可管理 IoT 凭证/绑定/快照 |

## 全局约定（实现时遵循）

- 后端落 `lunasrv-module-lunaerp`：`production.{loom,weavingorder[.task|.ticket|.requisition],iot}`；
  胚布单/检验单/库存的改造在各自既有包内进行。
- 前端落 `luna-ui/src/views/lunaerp-elmui/production/{weaving-order,loom}/`，API 镜像于 `src/api/lunaerp-elmui/production/`。
- 菜单：10280 织造单（10281-10284 按钮）、10285 织机档案（10286-10288）；领纱并入织造单工作台 Tab，IoT 并入织机档案页 Tab。
- 错误码段：织造域 `1-030-015`（子资源分段）；改造域复用各自段位。
- 字典 id：2238-2244（详见 GLOSSARY.md）。
- 全部新实体继承 `TenantBaseDO`；关联存 ID + 名称快照，不建物理外键（沿用项目惯例）。
