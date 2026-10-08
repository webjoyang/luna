# 21-data-permission-wiring: 销售归属链接通芋道 4 档数据范围

Type: grilling
Status: resolved
Blocked by: 03, 04

## Question

16 号验收清单（已 resolved）Q4=B 定稿引出的施工票：把芋道 DataPermission 5 档底座收敛为商业化 4 档（**全部/本部门及以下/本部门/本人**，弃用并在 UI 隐藏"自定义部门"档），仅给**销售归属链 5 表**接通行级数据范围，产 `.scratch/data-permission-wiring/spec.md`：

1. 5 表（客户、客户联系人、报价单、销售合同、发货单）加 `owner_user_id`/`dept_id` 的方案：列放哪张表（联系人是否继承客户归属而非自挂）、新值默认归属（建档人/建档部门）、存量行回填口径（租户 1 存量数据归 admin/根部门？）、Mapper XML 手写查询与租户拦截器一样需显式补规则的排查清单；
2. `DeptDataPermissionRuleCustomizer` 注册映射（user_id/dept_id 列名）与 @DataPermission 忽略点；报价转合同、发货从合同带单、合同未发量聚合等跨表查询的归属继承与性能（万级 P95 不回退）；
3. 4 档在角色编辑 UI 的收敛方式（隐藏自定义档是前端隐藏还是字典/枚举层裁剪）；11 个 seed 角色的 data_scope 默认值写入（boss/finance=ALL、sales_manager=DEPT_AND_CHILD、salesperson=SELF，其余 ALL，H5 两角色不适用）；
4. 四档走查用例（16 号 spec §6.2 为验收基准）全部可过；列表/详情 id/写操作/导出四入口一致"不存在"文案，不泄漏存在性；
5. 明确非目标：5 表以外单据一期恒全部档、不铺 dept_id/user_id；自定义档、字段级配置不做；术语（数据范围四档/归属人/归属部门）进 shared GLOSSARY；菜单/错误码/字典按需占位；`_order.md` 插在 approval-engine 之后、export-docs 之前（序号重排）。

产出：`.scratch/data-permission-wiring/spec.md`（按统一 DoD），解阻塞 16 号验收 gate G2 中该行。

## Answer（2026-10-08，三轮 grilling Q1–Q18 全部按推荐锁定）

产出已落盘：

- spec：`.scratch/data-permission-wiring/spec.md`（决策记录/归属写入/DDL 回填/规则注册与系统侧忽略契约/调岗切面/4 档收口/测试矩阵/施工顺序/非目标）
- **ADR-0046**：联系人零列 EXISTS 继承、调岗同事务刷 dept_id、自定义档"代码不删三闸收口"三取舍
- SQL：`luna-srv/sql/postgresql/lunaerp-data-permission-wiring.sql`（客户 +2 列、合同/发货各 +1 列、6 个 dp 部分索引、按租户只填空回填、common/test-dp 迁移、11 角色 data_scope 幂等对齐；全程可重跑）
- 术语：shared GLOSSARY 新增"数据范围四档/归属人/归属部门/联系人继承归属"；041 段评估后不占用（历史避让记录已注）
- spec 增量：05 票报价主表补 dept_id+dp 索引+转合同继承；04 票客户导入归属注记+回收站 handler 关闭数据权限挂账
- `_order.md`：本票插 21 位，export-docs 起顺延至 32

核心定稿：

1. 4 注册表（客户/报价/合同/发货）统一 `salesperson_user_id`+`dept_id`；联系人零列、EXISTS 子查询实时继承客户，四入口统一"客户联系人不存在"。
2. 新建 owner 缺省登录用户可改，dept_id 服务端按 owner 当前部门推导、不信前端；带单归属只向前继承一次；调岗切面同事务刷 4 表（满足 16 §6.2 即时变化）。
3. customizer 注册 4 表映射；冻结"用户侧开启/系统侧关闭"契约——取号扫描、未发量聚合、生产进度、审批回写、财务过账、回收站、引用守卫必须显式 @DataPermission(enable=false)。
4. 自定义档四层收口（前端过滤 value=2、API 拒绝并新增 system 错误码 1_002_002_006、底座代码不删、common→SELF/test-dp→ALL）；零新字典/菜单；lunaerp 041 段不占。
5. 性能：每表 `(tenant_id, dept_id)`/`(tenant_id, salesperson_user_id) WHERE deleted=0` 两部分索引，受限档 BitmapOr；5 表万行 P95<1s、无 seq scan，纳入 16 号 perf 清单。
