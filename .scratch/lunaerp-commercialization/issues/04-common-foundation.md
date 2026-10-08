# 04-common-foundation: 商业地基通用件

Type: grilling
Status: resolved
Blocked by: 18

## Question

设计 W0 通用件并产出 spec（对应 Q12/Q14），grilling 时先调 Skill "grilling" 与 "domain-modeling"。五件事各自独立 spec 还是合并一个 foundation spec，由本 ticket 决定：

1. 统一打印：Vue 套打模板组件 + qrcode 生成；模板组织方式（luna-ui 内打印组件库目录、模板注册表）；覆盖布票二维码（现有 shipment 07-print 伏笔对齐）、发货单、对账单、出口单证；
2. 统一回收站：软删列表+恢复；恢复时链式补回库存/账务的通用规则（对照 ADR-0017 反冲流水模式：删除已审核单据时的处理策略）；
3. 标准 Excel 导入：客户/供应商/产品/纱线规格四类主数据的模板定义、校验规则、错误回报方式（用芋道 EasyExcel，见 18 号研究）；
4. 固定敏感字段脱敏：金额/手机号/联系人等字段的注解级脱敏清单与作用角色范围（谁看全量、谁看脱敏）；
5. 站内消息+审批待办角标：复用芋道 notify/站内信（见 18 号研究），消息触发点清单（单据状态变化、审批到达、账期预警）。

产出：spec 文件（位置由本 ticket 决定，按统一 DoD）。

## Answer

grilling 一轮六问全部按推荐锁定，产出：

1. **spec 落 `.scratch/common-foundation/spec.md`（一份合并 spec，横切子包 `lunaerp.common`）**，五章对应五件事，实施按章拆 session。全程**不新建 lunaerp 业务表**。
2. **统一回收站 = 零新表 + `RecycleBinHandler` SPI 注册表**：手写 XML 查 `deleted=1`；收纳范围限主数据 + DRAFT 草稿（W0 覆盖 12 个 bizType），已过账单据只走红冲取消（ADR-0016/0017）永不进站；恢复只复活软删行与级联子表、**不补任何库存/账务**；主数据恢复 INACTIVE、草稿恢复 DRAFT；引用缺失阻断；物理删除独立权限+引用守卫、无自动清理。菜单落系统管理→回收站（10900）。边界落 **ADR-0038**。
3. **统一打印 = 公共内核（提取现有 iframe 打印逻辑）+ 就近模板 + 静态注册表分发**：W0 新建布票 100×60mm 标签模板并迁移发货单/装箱清单；二维码冻结码格式（布票→`{luna.h5.base-url}/h5/scan/weaving-ticket?code=`，发货单→门户 URL，15/14 号 ticket 接页面）；打印走独立 print-data 接口（豁免脱敏）+ `@LogRecord` 留痕；对账单/出口单证仅冻结契约位（10/07 号）。
4. **Excel 导入 = FastExcel 扁平核心字段 + 字典下拉 + 全量校验整批入库 + 错误 Excel（末列错误原因）+ 仅新增**，同步 5000 行上限；产品 JSON 字段（成本项/纱线配比）不进模板；四类模板列/校验已在 spec 列明。
5. **脱敏 = 自研 `@Sensitive(seePermission, type)` 条件注解**：两个固定权限位 `lunaerp:sensitive:amount` / `:contact`（底座 handler + 新增 AMOUNT 类型输出 `****`）；列表/详情/导出同口径，打印独立 VO 豁免；W0 标现有客户/供应商联系人、产品成本、纱线 refPrice 字段，后续模块按契约补标。
6. **消息 = 全复用芋道 notify（零新表零 DDL，跳转参数随 templateParams JSON）+ `LunaNotifyHelper`**：保持单个铃铛；W0 实装 `LUNA_APPROVAL_TODO` / `LUNA_APPROVAL_APPROVED` / `LUNA_APPROVAL_REJECTED` 三类（消息失败仅日志不回滚审批事务），合同生效/发货签收/账期预警只冻结 templateCode 契约随模块 ticket 落地。
7. 错误码取 `1-030-031`（回收站 000–099 / 导入 100–199），术语（回收站/恢复/彻底删除/删除 vs 取消/套打）与段位已入 shared GLOSSARY。

后续 ticket 挂账：07（出口单证模板注册）、10（对账单模板+AR 预警 job+财务字段脱敏）、14（门户签收页接发货单二维码）、15（H5 报工页接布票二维码）；W1–W4 各模块 spec 统一 DoD 增"声明 RecycleBinHandler（红冲类除外）"条目。
