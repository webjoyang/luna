# 18-yudao-capability-research: 芋道底座可复用能力盘点

Type: research
Status: resolved
Blocked by: 

## Question

盘点 luna-srv 当前芋道底座（`luna-srv/` 下依赖与模块）可复用/未迁入能力，为 W0 的 01–04 号决策提供事实（AFK，research 子代理直接解决）：

1. 租户套餐 TenantPackage：现依赖里是否已含租户与套餐模型，能否直接充当模块开通网关（对照 03 号 ticket）；
2. 数据权限 DataPermission：部门/自定义数据范围的现状（对照 Q9 的 4 档数据范围）；
3. 站内信/通知 notify：站内消息+待办角标的可复用点（对照 04 号）；
4. EasyExcel 导入：现有导入基础设施（对照 04 号）；
5. 操作日志/审计：现有审计能力；
6. 脱敏设施：注解级脱敏是否现成（对照 04 号）；
7. BPM 模块：Flowable 表结构与审批记录的兼容性参考（对照 02 号），以及当前 pom 是否已引入；
8. 错误码段位：现有各模块错误码段位约定（对照 01 号）；
9. 字典/多租户隔离的默认行为（对照统一 DoD）。

产出：在 `## Answer` 中给出"能力 × 结论（直接用 / 需扩展 / 未迁入+迁入成本）"清单，注明每条的代码位置证据。

## Answer

### a. 租户套餐 TenantPackage —— **直接用（菜单级网关现成）**
- 模型/服务/接口全链路已迁入：`TenantPackageDO`（`menuIds Set<Long>`，表 `system_tenant_package`）、`TenantPackageService/Impl`、`TenantPackageController`，均在 `luna-srv/lunasrv-module-system/.../system/{dal/dataobject,service,controller/admin}/tenant/`。
- 网关机制：`TenantServiceImpl#handleTenantMenu`（`service/tenant/TenantServiceImpl.java:295-310`）非 system 套餐取 `package.menuIds`；`MenuServiceImpl.java:135` 取菜单列表时按套餐过滤；`PermissionController.java:46` 给角色分配菜单时按套餐裁剪；租户创建/改套餐时自动同步租户管理员角色菜单（`TenantServiceImpl#createRole/updateTenantRoleMenu`，:131-210）。`system_tenant_package` 建表见 `sql/postgresql/ruoyi-vue-pro.sql:5456`。
- 结论：**按模块开通=按菜单开通**可直接用（菜单含 permission 字符串，接口权限被间接约束）；若需要"不挂菜单的纯接口级开关"或租户级灰度，需在其上扩展（成本小：新增无前端页面的"虚拟菜单"或加一张 tenant-feature 表）。

### b. 数据权限 DataPermission —— **直接用（lunaerp 接入需加列/注册映射）**
- starter 完整迁入：`lunasrv-framework/lunasrv-spring-boot-starter-biz-data-permission`（`DeptDataPermissionRule` 按 `dept_id`/`user_id` 列拼 SQL、`DataPermissionRuleFactory`、`@DataPermission` 注解、`DataPermissionUtils`）。
- 5 档数据范围现成：`DataScopeEnum`（ALL/DEPT_CUSTOM/DEPT_ONLY/DEPT_AND_CHILD/SELF，`module-system/.../enums/permission/DataScopeEnum.java:20-26`），`PermissionServiceImpl.java:297-330` 计算用户数据范围。
- 扩展点：lunaerp 各域表目前未注册 dept/user 列映射（`DeptDataPermissionRuleCustomizer` 无人调用），接入需给业务表加 `dept_id`/`user_id` 并 customize；lunaerp 已依赖该 starter（`lunasrv-module-lunaerp/pom.xml:30`）。

### c. 站内信/通知 notify —— **直接用**
- `module-system/.../notify`：`NotifyMessageDO`/`NotifyTemplateDO`、`NotifySendService`（模板渲染单发）、`NotifyMessageSendApi`（跨模块 DTO：`api/notify/dto/NotifySendSingleToUserReqDTO.java`），表 `system_notify_message`（`ruoyi-vue-pro.sql:3552`）。待办角标推送配套 `lunasrv-spring-boot-starter-websocket`（application.yaml:298 enable: true）。审批待办可复用此通道，无需新表。

### d. EasyExcel 导入 —— **直接用（lunaerp 各域导入模板需自写）**
- 底座用的是 **FastExcel 1.3.0**（`cn.idev.excel:fastexcel`，EasyExcel 原作者延续、API 兼容）：`lunasrv-dependencies/pom.xml:60,509`、`lunasrv-spring-boot-starter-excel/pom.xml:46`。设施齐全：`ExcelUtils` 读写、`DictConvert`/`MultiDictConvert` 字典转换、`@ExcelColumnSelect` 下拉框、`@InDict` 校验。
- 先例：用户导入 `UserController.java:198`（`ExcelUtils.read`）；lunaerp 已依赖 excel starter（`lunasrv-module-lunaerp/pom.xml:62`）但**尚无任何导入实现**——各域导入 VO/模板属于业务工作，非底座缺口。

### e. 操作日志/审计 —— **直接用**
- 注解式：`io.github.mouzt:bizlog-sdk`（mzt-biz-log，`lunasrv-spring-boot-starter-security/pom.xml:60`），`@EnableLogRecord` 于 `framework/operatelog/config/LunasrvOperateLogConfiguration.java`，`LogRecordServiceImpl` 桥接到 `OperateLogCommonApi` → `system_operate_log` 表（`ruoyi-vue-pro.sql:3915`）。另有 API 访问/错误日志（starter-web apilog → infra 模块）。审批"操作留痕"可直接复用 `@LogRecord`。

### f. 脱敏设施 —— **直接用（注解级现成；敏感词未迁入）**
- `lunasrv-spring-boot-starter-web/.../framework/desensitize/`：完整注解级脱敏——`@MobileDesensitize`、`@IdCardDesensitize`、`@BankCardDesensitize`、`@EmailDesensitize`、`@ChineseNameDesensitize`、通用 `@SliderDesensitize`/`@RegexDesensitize`，Jackson 序列化器 `StringDesensitizeSerializer`。VO 字段上加注解即生效。
- 注意：敏感词过滤（yudao SensitiveWord 模块）未迁入，若商业化需要内容审核需另行迁入（成本小-中）。

### g. BPM 模块 —— **未迁入（迁入成本：中-高）**
- 根 `luna-srv/pom.xml:20` `<module>lunasrv-module-bpm</module>` 被注释，`lunasrv-server/pom.xml:56` 依赖被注释——**模块源码不在本仓库**。
- 但 `lunasrv-dependencies/pom.xml:50` 仍管理 `flowable.version=8.0.0` 并声明 `flowable-spring-boot-starter-process/actuator`（:438-445），版本已就绪；`sql/postgresql/ruoyi-vue-pro.sql` 中**无任何 `bpm_` 建表**。
- 结论：自研审批记录表（对照 02 号）与回迁 yudao BPM 二选一；若回迁，Flowable 的 `ACT_RU_*/ACT_HI_*` 表模型（运行时任务/历史实例两态）可作为自研表 design 参考。

### h. 错误码段位 —— **约定清晰，新模块需续段**
- infra=`1-001-000-000` 段、system=`1-002-000-000` 段（子模块再按千位分段：AUTH/菜单/角色/用户…，见各自 `enums/ErrorCodeConstants.java` 头注释）。
- lunaerp=`1-030-001~019-xxx` 按域分段（产品/胚布品种/销售合同/客户/供应商/胚布单/染色单/纱线档案/纱线采购/进度反馈/发染/生产进度，见 `lunasrv-module-lunaerp/.../enums/ErrorCodeConstants.java:8-19`；002 段被胚布品种占用、003 起为后补域）。
- 建议：审批/模块网关若挂 lunaerp 继续在 1-030 内取未用段；若独立产品模块取 `1-031-xxx`。

### i. 字典与多租户隔离默认行为 —— **默认开、按 DO 声明判定**
- 开关：`lunasrv.tenant.enable=true`（`application.yaml:326-343`；ignore-urls 仅 `/jmreport/*`，ignore-tables 为空）。
- 判定机制：`TenantDatabaseInterceptor#computeIgnoreTable`（`starter-biz-tenant/.../core/db/TenantDatabaseInterceptor.java:68-81`）——DO 继承 `TenantBaseDO` → 自动拼 `tenant_id`；标 `@TenantIgnore` → 忽略（如 `TenantPackageDO`）；非项目表忽略。新增业务表只要 DO 继承 `TenantBaseDO` 即自动隔离，无需配置。
- 字典：`DictDataDO extends BaseDO`（`module-system/.../dal/dataobject/dict/DictDataDO.java:20`），`system_dict_data` 表无 `tenant_id`（`ruoyi-vue-pro.sql:707-722`）→ **字典全局共享、不做租户隔离**（商业化若需租户自定义字典需改造为 TenantBaseDO）。
- 缓存：Redis 缓存默认按租户前缀隔离（`TenantRedisCacheManager`），白名单在 `ignore-caches`（user_role_ids/permission_menu_ids/notify_template 等，`application.yaml:334-343`）。
