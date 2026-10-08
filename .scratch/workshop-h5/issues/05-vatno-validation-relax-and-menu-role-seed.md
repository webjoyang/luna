# 05: 缸号校验放宽 + 菜单/角色 seed

**What to build:** 落盘 H5 权限容器与角色种子，以及录检明细缸号校验的行为放宽。`InspectionOrderItemSaveReqVO.vatNo` 现为 `@NotBlank`，与术语表"胚布场景缸号可空"矛盾，改为 Service 层条件校验：检验单 `type=FINISHED` 或 `packageMode=ROLL` 时 vatNo 必填（不变）；`type=GREIGE AND packageMode=PIECE` 时 vatNo 可空（H5 胚布扫码录检不带缸号；PC 同步放宽，保存空缸号不再报错）。SQL `lunaerp-workshop-h5.sql` 可重复执行：菜单 10530（`visible=0` 隐藏容器，`parent=10200`，无 component/path 占位）+ 10531-10533 三按钮（`lunaerp:workshop:h5:report`/`inspection`/`dispatch`）+ 角色 `workshop_worker`（10531+10533）/ `workshop_inspector`（10532）DO 块按 code 幂等 upsert（不固定 id，`tenant_id=1`，仿 LUNA_WORKER 岗位 seed）+ `super_admin`/`admin` role_menu 同步（既有无视菜单授权逻辑，保持 get-permission-info 一致）。本票不交付用户行为，是 06 路由守卫权限码判空与 16 ROLL 校验的前置。

**Blocked by:** 01（错误码已落）

**Status:** ready-for-agent

- [ ] `InspectionOrderItemSaveReqVO.vatNo` 去掉 `@NotBlank` 注解
- [ ] Service 层条件校验：检验单 `type=FINISHED` 或 `packageMode=ROLL` 时 vatNo 必填（不变）；`type=GREIGE AND packageMode=PIECE` 时 vatNo 可空
- [ ] PC 同步生效：保存空缸号不再报错（GREIGE+PIECE 场景）
- [ ] SQL `luna-srv/sql/postgresql/lunaerp-workshop-h5.sql` 可重复执行
- [ ] 菜单 10530（`visible=0` 隐藏容器，`parent=10200`，无 component/path 占位）
- [ ] 菜单 10531 按钮 `lunaerp:workshop:h5:report`（移动报工）
- [ ] 菜单 10532 按钮 `lunaerp:workshop:h5:inspection`（移动质检）
- [ ] 菜单 10533 按钮 `lunaerp:workshop:h5:dispatch`（移动发纱）
- [ ] 角色 `workshop_worker`（车间工人，授权 10531+10533）DO 块按 code 幂等 upsert，不固定 id，`tenant_id=1`
- [ ] 角色 `workshop_inspector`（车间检验员，授权 10532）DO 块按 code 幂等 upsert，不固定 id，`tenant_id=1`
- [ ] `super_admin`/`admin` 自动获全部 H5 权限（既有无视菜单授权逻辑，SQL 同步插 role_menu 保持 get-permission-info 一致）
- [ ] 一人可双角色；挡车工身份仍由机台任务 `worker_user_id` 关联（system_users + LUNA_WORKER 岗位，ADR-0021），岗位与角色正交
- [ ] 多租户环境：seed 仅租户 1；新租户由租户管理员在系统管理复制角色或自行创建（一期私有化买断单租户为主，不做租户模板自动化）
- [ ] 脚本幂等可重复执行（事务内 DELETE 再 INSERT 或 ON CONFLICT）
