# 16-acceptance-checklist: 商业化验收清单

Type: grilling
Status: resolved
Blocked by: 05, 06, 20, 08, 09, 10, 11, 12, 13, 14, 15

## Question

设计商业化验收清单并产出 spec（W4 收口，Q16），grilling 时先调 Skill "grilling" 与 "domain-modeling"：

1. 全模块 DoD 核对表：charting 决策 Q16 七条 + 性能/安全硬指标（列表 P95<1s@万级、写接口租户隔离测试、横向/纵向越权必测、敏感字段脱敏生效）；
2. 波次 E2E 冒烟脚本清单：主链路 = 报价→合同→下达生产→织造/染整/质检→入库→发货→签收→对账→收款；辅链路 = 退货→应收冲减；采购链 = 纱线采购→入库→齐套→领纱；资金链 = 付款/报销/工资；每条链路给出可脚本化的步骤与断言点；
3. 权限/越权走查用例集：角色矩阵、数据范围（4 档）、脱敏抽查；
4. 交付物清单：部署文档、初始化脚本、默认数据（字典/角色/示例模块开通）、升级规范——把 map 中"Not yet specified"的交付物细节在此澄清；
5. 验收签字口径：什么叫"达到可对外售卖"——逐条可勾选。

产出：`.scratch/commercialization-acceptance/spec.md`（按统一 DoD）。

## Answer

两轮 grilling 15 问全部按推荐锁定（2026-10-08），spec 落 `.scratch/commercialization-acceptance/spec.md`：

1. **DoD 总表**：30 行（15 存量 MVP+W0 横切 3+W1 四+W2 一+W3 四+W4 两端+新票 data-permission-wiring），逐行 11 格（Q16 七条+P95/租户/横向/纵向），存量 MVP 硬指标同标准补测无祖父条款；横切工程并入宿主行。
2. **P95 统一测量法**：验收库每列表主表 1 万行（双租户 9000:1000 含软删），100 次请求去预热取 P95<1s，EXPLAIN 走索引无 N+1，灌数/计时/报告脚本化。
3. **冒烟双轨**：API 级 JUnit 四链自动冒烟（断言单据状态+库存台账 STOCK_IN/SHIPMENT_OUT/YARN_OUT 及红冲+分录 category/金额）+ UI 手工清单（打印、五终端 H5、375px 门户）；波次出口冒烟 W0–W3 不过闸不进波；主链成品入库按现状手工入库单、签收 API 走 PC 登记。
4. **权限走查**：seed 11 标准角色+菜单/数据范围/amount/contact 矩阵；**数据范围 4 档=全部/本部门及以下/本部门/本人（弃用自定义档）**，一期仅销售归属链 5 表（客户/联系人/报价/合同/发货）接通——施工新开 **21-data-permission-wiring** 票（Blocked by 03,04，产 spec，插 _order 在 approval-engine 后）；横/纵向越权与租户隔离全域用例+脱敏六类抽查。
5. **交付物**：重写 docker 私有化 compose（PG15+Redis7+server+luna-ui 四服务，干净机一键全绿）替换 MySQL/Vue2 遗物；`docs/deployment/{install,upgrade,acceptance-runbook}.md`；`sql/postgresql/install/` 编号幂等总入口（空库/存量双库重跑实证，不引 flyway、无 down 脚本、回滚靠备份）；默认数据最小全集（菜单/字典 2255–2287/11 角色/核心包套餐/六类默认审批流/config 键），不灌业务演示数据另发可清理 demo-seed。
6. **签字 gate**：G1 四链冒烟/G2 DoD 总表/G3 性能安全/G4 交付物/G5 账实零差异 五个一票否决，P0/P1 清零、P2≤10 书面知悉，供方研发+交付、客户老板+实施四方签字；签字单即 spec §8.4 附录。

map fog「交付物细节」随本票澄清清出；术语（4 档数据范围/归属人/归属部门）留给 21 号票进 shared GLOSSARY。
