# 16: 录检行 ROLL 保存 + (缸号,卷号) 查重 + 缸号记忆 + 重复卷定位改录

**What to build:** 落成成品 ROLL 录检行保存端点与成品逐卷录检前端。POST `/lunaerp/h5/workshop/inspection/item/save` ROLL 路径（`@PreAuthorize lunaerp:workshop:h5:inspection`；`@LogRecord`）。规则：成品 ROLL（来自检验单内逐卷录）：vatNo、rollNo 必填（依赖 05 的条件校验：FINISHED/ROLL 时 vatNo 必填）；保存前按 `(inspectionOrderId, vatNo, rollNo)` 查重，命中已存在行 → **不覆盖**，返回 039-005 + 该行数据，前端定位改录；公共字段同 15（grade/measuredWeight/lengthM/remarks/photos ≤6）；改录仅 DRAFT/ACTIVE 允许；布票 INSPECTED 回写、入库链路零改动。前端 `src/views/h5/inspection/order.vue` ROLL 模式 UI（进单逐卷一行；扫码/手输卷号定位，缸号记住上次值 localStorage，`(缸号,卷号)` 重复定位到已有行改录而非新建）。可 demo：进 DRAFT/ACTIVE 单→逐卷扫码/手输→缸号记忆→重复卷定位改录。

**Blocked by:** 15（PIECE 保存端点框架已就位，ROLL 复用同一端点分支）, 05（缸号条件校验放宽已落，ROLL 时 vatNo 必填不变）

**Status:** ready-for-agent

- [ ] POST `/lunaerp/h5/workshop/inspection/item/save` ROLL 路径
- [ ] 成品 ROLL：vatNo、rollNo 必填（FINISHED/ROLL 时 vatNo 必填不变）
- [ ] 保存前按 `(inspectionOrderId, vatNo, rollNo)` 查重，命中已存在行 → 不覆盖，返回 039-005 + 该行数据
- [ ] 公共字段同 15：grade（字典 2232）、measuredWeight、lengthM、remarks、photos（≤6）
- [ ] 改录仅 DRAFT/ACTIVE 允许；布票 INSPECTED 回写、入库链路零改动
- [ ] `@PreAuthorize lunaerp:workshop:h5:inspection`；`@LogRecord` 留痕
- [ ] 前端 `src/views/h5/inspection/order.vue` ROLL 模式 UI（进单逐卷一行；扫码/手输卷号；缸号记住上次值 localStorage；(缸号,卷号) 重复定位到已有行改录而非新建）
- [ ] 测试：ROLL 查重同 (缸号,卷号) 二次保存 → 039-005 且不产生新行；FINISHED/ROLL 空 vatNo → 校验错
- [ ] demo：进 DRAFT/ACTIVE 单→逐卷扫码/手输→缸号记忆→重复卷定位改录
