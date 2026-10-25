# 横切基础 - W0 商业地基通用件（统一打印 / 统一回收站 / Excel 导入 / 字段脱敏 / 站内消息）

Status: ready-for-agent（grilling 定稿，源自 `.scratch/lunaerp-commercialization/issues/04-common-foundation.md`，一轮六问全部按推荐锁定）

## Problem Statement

商业化完整版所有业务模块都依赖五件横切能力，必须在 W0 波次先铺好地基，后续 W1–W4 模块只做"注册/接入"，不再各造一套：

1. **统一打印**：布票二维码（W4 车间 H5 扫码报工的入口）、发货单/装箱清单、对账单、出口单证都需要前端固定套打模板；现状是发货单有一份孤立实现 `PrintShipmentDialog.vue`（隐藏 iframe + `contentWindow.print()`），无模板组织约定、无二维码。
2. **统一回收站**：统一 DoD 要求"列表/详情/删除进回收站"。现状所有表走 `deleted` 逻辑删除但全仓无恢复入口；而已审核单据按 ADR-0016/0017 只能红冲取消（单据保留为 CANCELLED），恢复时"链式补回库存/账务"的边界必须讲清。
3. **标准 Excel 导入**：客户/供应商/产品/纱线规格四类主数据的初始化导入。底座 FastExcel 设施齐全（`ExcelUtils`、`DictConvert`、下拉 sheet），lunaerp 尚无任何导入实现。
4. **固定敏感字段脱敏**：金额/联系人等字段按角色脱敏（谁看全量、谁看脱敏）。底座注解（`@MobileDesensitize` 等）加了就永远脱敏，**不做角色判定、也无金额注解**，需自研条件脱敏。
5. **站内消息 + 待办角标**：单据状态变化、审批到达、账期预警需要消息触达。底座 notify 全链路现成（模板表、发送 API、头部铃铛未读数、WebSocket），lunaerp 零调用，需要统一封装与模板/触发点契约。

**本 spec 不新建任何 lunaerp 业务表**：回收站复用各表现有 `deleted` 列；消息复用 `system_notify_message`（该表已持久化 `template_params` JSON，点击跳转信息随参数携带，零 DDL）；打印留痕复用 `system_operate_log`。

## Solution 总览

grilling 六问锁定：

1. **一份合并 spec + 一个横切子包**：后端在 `lunasrv-module-lunaerp` 新增与 `approval` 平级的横切子包 `com.luna.lunasrv.module.lunaerp.common`，下分 `recycle` / `dataimport` / `notify` / `desensitize` 四个子包（打印纯前端）。实施按章节拆 session。
2. **回收站 = 零新表 + handler 注册表**：恢复对象仅限"主数据 + DRAFT 草稿单据"，恢复不补账（草稿与主数据从未记账）；主数据恢复为 INACTIVE、草稿恢复为 DRAFT；级联子表父→子恢复，引用缺失阻断；提供独立权限的彻底删除。边界落 **ADR-0038**。
3. **打印 = 公共内核 + 就近模板 + 模板注册表**：W0 交付布票标签模板与发货单/装箱清单迁移；二维码冻结码格式（完整 H5/门户 URL，域名走 infra_config）；对账单（10 号）、出口单证（07 号）只挂契约，随模块 ticket 注册。
4. **Excel = 核心扁平字段 + 字典下拉 + 全量校验整批入库 + 错误 Excel 回报 + 仅新增**，同步、单文件 5000 行上限；复杂 JSON 字段（产品成本项/纱线配比）不进导入。
5. **脱敏 = 自研权限条件注解 + 两个固定权限位**（`lunaerp:sensitive:amount` / `lunaerp:sensitive:contact`）；打印取数走独立 print-data 接口与打印 VO 豁免；Excel 导出同受脱敏约束。
6. **消息 = 零新表全复用 + `LunaNotifyHelper` 封装 + 保持单个铃铛角标**；W0 实装审批到达/通过/驳回三类，其余触发点冻结 templateCode 契约随模块 ticket 落地。

### 工程落点

**后端**（`lunasrv-module-lunaerp`）：

```
com.luna.lunasrv.module.lunaerp.common
├── recycle/        # 回收站：SPI 接口、注册表、Controller、通用分页 VO
├── dataimport/     # 导入：通用校验/错误收集工具、四类主数据 ImportVO 与 Service
├── notify/         # LunaNotifyHelper、templateCode 枚举、bizType 跳转约定
└── desensitize/    # @Sensitive 注解、条件序列化器、AMOUNT handler
```

各业务域在自己包内实现 SPI（如 `service/baseinfo/customer/CustomerRecycleHandler.java`），Spring 自动收集进注册表。

**前端**（`luna-ui`）：

```
src/components/Print/
├── src/PrintDispatcher.vue     # 统一打印入口：按 bizType+templateCode 分发
├── src/PrintPaper.vue          # 纸张/边距/打印按钮外壳（提取自 PrintShipmentDialog）
├── src/QrSlot.vue              # 二维码槽位（包装现有 components/Qrcode）
├── composables/usePrintHtml.ts # 隐藏 iframe + contentWindow.print()（提取自 PrintShipmentDialog:504-669）
└── registry.ts                 # 模板注册表（静态数组，模块 ticket 追加）
src/views/lunaerp/common/recycle/index.vue   # 回收站页（按 bizType 分 tab）
src/api/lunaerp/common/{recycle,dataimport}/index.ts
```

打印模板就近放各业务模块 `components/print/`，注册表只登记位置与取数 API。

---

## 1. 统一打印

### 1.1 模板注册表

`registry.ts` 维护静态注册表（不做自动发现、不做模板设计器，Q12=A）：

```ts
interface PrintTemplateReg {
  bizType: string            // 业务对象类型，与回收站/消息的 bizType 同字典
  templateCode: string       // 模板编码，如 SHIPMENT_DOC / PACKING_LIST / WEAVING_TICKET_LABEL
  name: string               // 版式名（按钮文案）
  paper: 'A4' | 'LABEL-100x60'
  permission: string         // v-hasPermi 权限码
  component: Component       # 模板组件
  fetchApi: (bizId: string|number) => Promise<any>  // 后端 print-data 取数
}
```

列表页"打印"按钮统一调 `<PrintDispatcher :biz-type="'SHIPMENT'" :biz-id="row.id" />`，一对象多模板（发货单/装箱清单）时 Dispatcher 弹版式切换。

### 1.2 二维码码格式冻结（W0 定契约，15/14 号 ticket 接页面）

| 二维码 | 编码内容 | 域名配置 | 扫码落点 |
|---|---|---|---|
| 布票 | `{luna.h5.base-url}/h5/scan/weaving-ticket?code={布票code}` | infra_config 键 `luna.h5.base-url` | W4 车间 H5 报工页（15 号） |
| 发货单 | `{luna.portal.base-url}/portal/shipment?no={发货单号}` | infra_config 键 `luna.portal.base-url` | 客户门户签收页（14 号；W0 只印，链接可暂不通） |

规则：编码**完整 URL**（私有化单域名，不加签名，鉴权交给 H5/门户登录态）；配置缺失时打印接口报错提示管理员补配置，不允许打出空域名二维码；布票 code 用现有 `WeavingTicketDO.code`（租户级唯一，错误码 1_030_015_116）。

### 1.3 W0 模板交付范围

| 模板 | 状态 | 说明 |
|---|---|---|
| `WEAVING_TICKET_LABEL` 布票标签 | 新建 | 不干胶 100×60mm 固定版式（`@page { size: 100mm 60mm }`），内容：布票 code 二维码 + 布票号 + 织造单号 + 机台任务号 + 品名/颜色/匹序/数量；织造单工作台机台任务页提供"打印布票（批量勾选）" |
| `SHIPMENT_DOC` 发货单 / `PACKING_LIST` 装箱清单 | 迁移 | 将 `PrintShipmentDialog.vue` 两套版式拆为 `business/shipment/components/print/` 下两个模板组件，打印内核替换为公共 composable，行为/版式不变 |
| 对账单、出口单证（PI/CI/PL） | 仅登记契约位 | 模板随 10 号客户对账、07 号出口单证 spec 实现并注册到同一注册表 |

### 1.4 后端 print-data 接口与留痕

打印取数必须走**独立 print-data 接口**，不复用列表/详情 VO（原因见第 4 章：打印件需要全量联系人字段，不能走脱敏 VO）：

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/lunaerp/production/weaving-ticket/{id}/print-data` | `lunaerp:production:weaving-ticket:print` | 布票标签 VO（含拼好的 qrUrl） |
| GET | `/lunaerp/business/shipment/{id}/print-data` | `lunaerp:business:shipment:print` | 发货单/装箱清单打印 VO（沿用现有权限码） |

两个接口方法上加 `@LogRecord(type="打印", success="打印了{PrintLogName} {{#no}}")`，写入 `system_operate_log`，**不新建打印日志表**。后续模块打印接口同样遵循"独立 print-data + `@LogRecord`"约定。

### 1.5 User Story（打印）

1. 作为车间管理员，我想在织造单工作台勾选多张布票一次打印不干胶标签，贴在每匹布上，工人用手机扫码直接进报工页。
2. 作为销售内勤，我想在发货单上点打印，选择发货单或装箱清单版式，预览后送打印机（行为与现状一致）。
3. 作为客户门户的最终用户（二期链路），扫发货单二维码可查看发货与签收信息。
4. 作为管理员，所有打印行为在操作日志中有据可查。

---

## 2. 统一回收站（边界详见 ADR-0038）

### 2.1 回收什么、不回收什么（核心边界）

- **进回收站**：主数据（客户/供应商/产品/纱线材质/纱线规格）；**仅 DRAFT 状态**的草稿单据（销售合同/胚布单/染色单/纱线采购单/入库单/织造单/发货单）。这些数据从未产生过库存/账务副作用。
- **永不进回收站**：已审核/已生效单据。按 ADR-0016/0017，已过账单据只能**红冲取消**，单据行保留为 CANCELLED；取消是追加反向流水的业务动作，不是删除，也不从任何列表"消失"。
- 因此**恢复动作不补任何库存/账务**；"链式补回"只指级联软删的子表行（明细/放置记录/机台任务/布票等）随父单一并复活。

### 2.2 SPI 设计（零新表）

```java
public interface RecycleBinHandler {
    String getBizType();                                  // lunaerp_recycle_biz_type 字典值
    PageResult<RecycleItemRespVO> pageDeleted(RecyclePageReqVO req);  // 自定义 XML：显式 WHERE deleted=1，绕开 MP 逻辑删除
    void validateRestore(Long id);                       // 引用检查：引用的主数据也在回收站则抛 1_030_031_002
    void restore(Long id);                               // 自定义 UPDATE ... SET deleted=0（MP 内置 update 无法命中已删行），同事务父→子复活
    boolean hasActiveReference(Long id);                 // 彻底删除前的引用守卫
    void purge(Long id);                                 // 物理删除（自定义 DELETE），同事务级联
}
```

- `RecycleItemRespVO`：bizType / bizId / displayName（单号或名称）/ deletedBy / deletedTime（取 BaseDO 的 deleted 行仍带 updater/update_time，删除即最后更新）。
- 注册表 `RecycleBinHandlerRegistry` 启动时收集所有 Bean，按 bizType 路由；未注册类型调接口报 1_030_031_000。
- 查询/复活/物理删除的 Mapper 语句全部走 **Mapper XML 手写 SQL**（显式 `deleted = 1` 条件 + 租户隔离由 TenantBaseDO 机制在 XML 中同样需要显式 `tenant_id`——XML 不经租户拦截器自动改写，必须手写租户条件并加测试守护）。

### 2.3 恢复规则

1. 主数据恢复后 `status = INACTIVE`（与删除前置 INACTIVE 的惯例衔接），用户在列表手动启用；草稿单据恢复为 DRAFT。
2. 级联子表随父单恢复：按现有聚合关系父→子顺序批量 `deleted=0`（织造单含机台任务/布票/领纱，参考现有 `WeavingOrderCascadeDeleteListener` 的级联范围做逆向）。
3. **引用阻断**：草稿单据引用的主数据仍在回收站时，恢复报 1_030_031_002，错误信息按依赖链列出需先恢复的对象名称；主数据之间存在父子依赖（纱线规格→纱线材质）同理，先父后子。
4. 编号不冲突：编号唯一索引显式包含已删行（shared GLOSSARY 既定约定），复活的是原行本身，不存在重号。
5. 恢复操作幂等：仅 `deleted=1` 的行可恢复，否则报 1_030_031_001。

### 2.4 彻底删除（物理删除）

- 回收站页"彻底删除"独立权限码；主数据存在 `deleted=0` 的业务引用时禁止（`hasActiveReference` 报 1_030_031_003）；草稿单据按聚合物理删除全部子表。
- **不做**定期自动清空、不做批量彻底删除（一期逐条，防误操作）。

### 2.5 W0 handler 覆盖

| bizType | 对象 | 级联复活范围 |
|---|---|---|
| CUSTOMER | 客户 | 客户联系人 |
| SUPPLIER | 供应商 | 供应商联系人 |
| PRODUCT | 产品 | 标签等子表 |
| YARN_TYPE | 纱线材质 | —（有规格引用时阻断删除/提示） |
| YARN_SPEC | 纱线规格 | — |
| SALES_CONTRACT | 销售合同（DRAFT） | 合同明细 |
| GREIGE_ORDER | 胚布单（DRAFT） | 颜色行等 |
| DYEING_ORDER | 染色单（DRAFT） | 颜色明细 |
| YARN_PURCHASE_ORDER | 纱线采购单（DRAFT） | 采购明细 |
| STOCK_IN | 入库单（DRAFT） | 明细/放置记录 |
| WEAVING_ORDER | 织造单（DRAFT） | 机台任务/布票/领纱单及明细 |
| SHIPMENT | 发货单（DRAFT） | 发货明细/卷清单 |

W1–W4 新模块的 spec 必须声明自己的 `RecycleBinHandler`（统一 DoD 条目），DRAFT 可删单据强制注册；红冲类单据不注册。

> 数据范围（21 号票接线，ADR-0046）：CUSTOMER / SALES_CONTRACT / SHIPMENT（及 05 票 QUOTATION）handler 查询 deleted=1 行与恢复引用校验属于**系统侧调用**，Service 方法必须显式 `@DataPermission(enable=false)`（租户条件照加）——回收站是管理视图，不能因登录人数据范围漏掉软删行。

### 2.6 API 与菜单

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/lunaerp/common/recycle/page`（bizType 必填） | `lunaerp:common:recycle:query` |
| PUT | `/lunaerp/common/recycle/restore`（body: bizType, bizId） | `lunaerp:common:recycle:restore` |
| DELETE | `/lunaerp/common/recycle/purge`（bizType, bizId） | `lunaerp:common:recycle:purge` |

菜单位于 **系统管理 → 回收站**（菜单 id 10900，挂芋道内置系统管理目录，所有租户必含；随系统管理模块开通，不参与模块套餐网关的独立模块判定）。回收站页按 bizType 分 tab，列：名称/单号、删除人、删除时间，操作：恢复 / 彻底删除。

### 2.7 User Story（回收站）

5. 作为仓管，我误删了一张草稿入库单，能在回收站找到并一键恢复，明细行完整回来且仍是草稿态。
6. 作为业务员，我删除的客户在回收站可见；恢复后客户为停用状态，确认无误后手动启用。
7. 作为用户，恢复一张草稿胚布单但其客户也在回收站时，系统提示我先恢复客户而不是恢复出一个引用悬空的单子。
8. 作为管理员，我可以对确认无用的记录彻底删除，但已被业务引用的主数据不允许物理删除。
9. 已审核的任何单据在回收站里永远看不到——取消它们走单据上的"红冲取消"。

---

## 3. 标准 Excel 导入（四类主数据）

### 3.1 通用规则

- 设施：FastExcel（`ExcelUtils.read`）、字典列 `DictConvert` + `@ExcelColumnSelect` 下拉 sheet，参照芋道用户导入实现。
- **仅新增**：唯一键冲突即按行报错，不做 upsert/更新（更新走 UI，防误覆盖）。
- **全量校验、整批入库**：先逐行校验收集全部错误；有任一错误→整批不写库，返回错误 Excel；全部通过→单事务批量插入，返回成功条数。
- 错误 Excel：原始列原样保留，**末列追加"错误原因"**（一行多原因用；分隔），`.xlsx` 文件流下载。
- 同步导入，单文件上限 **5000 行**（超出报 1_030_031_100），空文件报 1_030_031_101。
- 字典列校验：值不在字典中按行错误；下拉只是辅助，后端以校验为准。
- 复杂字段不进模板：产品 `yarnSpecs/costItems` JSON、`greigeTypeId/supplierId` 历史 UUID 字段一律排除，导入后在 UI 维护。

### 3.2 四类模板列定义

**客户**（`lunaerp:baseinfo:customer:import`，模板下载同权限）

| 列 | 必填 | 校验 |
|---|---|---|
| 企业名称 | 是 | 租户内重名检查（当前服务层无强唯一约束，导入按企业名称查重，冲突报错） |
| 简称 | 否 | 为空时后端按现有逻辑用企业名称回填 |
| 客户类型 | 否 | 字典（以 CustomerSaveReqVO 的 clientType 字典码为准） |
| 行业 / 规模 / 来源 / 合作状态 | 否 | 均字典列 |
| 统一社会信用代码 | 否 | 格式校验（18 位） |
| 英文名称 / 英文地址 | 否 | 写入客户档案 name_en/address_en（20 号出口单证定稿新增两可空列，供出口单证买方抬头带出） |
| 备注 | 否 | |

导入行 `status` 默认 ACTIVE。

> 归属（21 号数据范围接线，ADR-0046）：客户模板**无归属列**，导入行归属人=执行导入的登录用户、归属部门=其当前部门（服务端推导，与页面建档同路径）；不支持按 Excel 指定归属人。

**供应商**（`lunaerp:baseinfo:supplier:import`）

| 列 | 必填 | 校验 |
|---|---|---|
| 供应商编号 | 是 | 租户内唯一（含已删行口径），冲突报错 |
| 企业名称 | 是 | 非空 |
| 简称 | 否 | |
| 供应商类型 | 否 | 字典 `lunaerp_supplier_type` |
| 机型 | 否 | |
| 备注 | 否 | |

**产品**（`lunaerp:baseinfo:product:import`）：产品编号*、产品名称*、类型/面料分类/织物组织/计价方式（字典列）、单位、状态等扁平核心字段；编号租户内唯一（错误码 1_030_001_001 口径）。**不含**成本项、纱线规格、标签、历史关联 UUID 字段。

**纱线规格**（`lunaerp:production:yarn-spec:import`）：材质名称*（精确匹配启用状态的纱线材质，匹配不到按行错误）、规格名称*、标准名*（租户+材质内唯一，冲突报错，对应 1_030_008_003）、纺纱方式/光泽（字典列）、参考价（非负金额，受 `lunaerp:sensitive:amount` 管控——导入人需有金额权限，否则按行错误提示无权限）、备注。

### 3.3 API

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/lunaerp/baseinfo/customer/import-template` | `lunaerp:baseinfo:customer:import` |
| POST | `/lunaerp/baseinfo/customer/import`（multipart） | 同上 |
| 供应商/产品/纱线规格同构，路径前缀替换为各自模块路径 | | |

四个列表现有列表页工具栏新增"下载模板 / 导入"按钮（产品在正式目录 `views/lunaerp/`，客户/供应商以各自当前落点为准）。

### 3.4 User Story（导入）

10. 作为实施人员，私有化交付后我用标准模板一次性导入几千条客户/供应商/产品档案，字典列有下拉不会填错。
11. 作为用户，导入文件有 3 行错误时，系统不写入任何数据，给我一份标好每行错误原因的 Excel，改完重新导入即可。
12. 作为用户，重复导入相同编号的数据会被逐行拦下，不会产生重复档案。

---

## 4. 固定敏感字段脱敏

### 4.1 机制：权限条件脱敏注解

在 `common/desensitize` 新增：

```java
@Sensitive(seePermission = "lunaerp:sensitive:amount", type = SensitiveType.AMOUNT)
private BigDecimal refPrice;
```

- `@Sensitive(type=...)`：类型枚举 `MOBILE / CHINESE_NAME / BANK_CARD / FIXED_PHONE / EMAIL / AMOUNT`。前五个复用底座 desensitize handler 的脱敏规则；`AMOUNT` 为本 spec 新增 handler：无权限序列化为字符串 `"****"`，有权限返回原值（BigDecimal 数字正常序列化）。
- 序列化器在 Jackson 输出时调用 `SecurityFrameworkService.hasPermission(seePermission)`（与 `@ss` 同源）：**有权限出原文，无权限脱敏**；无登录上下文（定时任务/MQ）按脱敏处理。
- 注解只标在 **RespVO** 上；不建字段配置中心（Q9=A），权限码是代码中固定的两个：

| 权限位 | 范围 | 默认授予建议 |
|---|---|---|
| `lunaerp:sensitive:amount` | 金额/单价/成本价/参考价等一切金额字段 | 租户管理员、财务、老板角色 |
| `lunaerp:sensitive:contact` | 联系人姓名、手机、座机、银行账号、邮箱 | 业务员（配合数据范围）、销售主管、管理员 |

两个权限位以 `type=3`（按钮）菜单注册到菜单 SQL（挂系统管理目录下，无页面），角色管理中可勾选授权。

### 4.2 W0 标注清单（现有 VO）与后续契约

| 字段 | 所在 VO | 类型 |
|---|---|---|
| 参考价 refPrice | 纱线规格 RespVO/导出 VO | AMOUNT |
| 产品成本/价格类字段 | 产品 RespVO/导出 VO（实施时按字段实标，costItems 内单价同标） | AMOUNT |
| 联系人姓名 | 客户/供应商联系人 RespVO | CHINESE_NAME |
| 手机/座机/银行账号/邮箱 | 客户/供应商联系人 RespVO | MOBILE/FIXED_PHONE/BANK_CARD/EMAIL |
| 主体上的联系电话/银行信息（如有） | 客户/供应商 RespVO | 对应类型 |

后续模块（对账/收付款/报销/工资/成本报价等）在各自 spec 与 RespVO 上按同一注解标注，本清单即目标契约；工资明细等强敏感对象默认只用 AMOUNT/CONTACT 两个权限位，不再新增权限位。

### 4.3 列表/详情/导出/打印的口径

- **列表与详情**：同一 RespVO，均按权限脱敏（接口返回即脱敏，前端不做二次处理）。
- **Excel 导出**：复用同一序列化机制，无权限导出的就是脱敏值，不提供"导出全量"旁路。
- **打印例外**：打印件（发货单上的客户电话）走第 1 章独立 print-data 接口与**打印 VO**，持有对应 `:print` 权限即返回全量字段；每次打印 `@LogRecord` 留痕。不允许在脱敏 VO 上加"打印模式"开关绕过。
- **越权测试**：构造有/无两类权限的用户，对列表/详情/导出分别断言；写接口不接受脱敏值回写（`"****"` 入库）——更新接口以 ID 为准且不依赖脱敏字段回传，测试覆盖。

### 4.4 User Story（脱敏）

13. 作为普通业务员，我看客户列表时联系人手机号显示 `138****5678`；主管账号看到全量。
14. 作为车间账号，我在任何界面和导出文件里都看不到单价/金额，只显示 `****`。
15. 作为仓库，我打印发货单时电话完整可拨号，但这次打印在操作日志里留痕。

---

## 5. 站内消息 + 审批待办角标

### 5.1 零新表复用

- 表与通道：`system_notify_template`（模板）、`system_notify_message`（消息，已持久化 `template_params` JSON）、`NotifyMessageSendApi#sendSingleMessageToAdmin`、头部铃铛（`layout/components/Message`，未读数接口与 WebSocket 已启用）。
- lunaerp 只包一层 `LunaNotifyHelper.send(userId, templateCode, params, bizJump)`；**不新增表、不新增第二个角标/红点**（审批待办数只在审批中心页内查询）。
- **点击跳转零 DDL**：消息的 `templateParams` 固定携带 `bizType / bizId / bizNo`；前端铃铛消息点击时按前端维护的 bizType→路由表跳转（审批类 → `/approval/todo` 并带上实例 id），无路由约定的消息仅展示详情。

### 5.2 模板与触发点契约

| templateCode | 触发点 | 接收人 | W0 |
|---|---|---|---|
| `LUNA_APPROVAL_TODO` | 审批任务创建（节点到达，02 号引擎） | 节点各审批人 | 实装 |
| `LUNA_APPROVAL_APPROVED` | 实例通过 | 发起人 | 实装 |
| `LUNA_APPROVAL_REJECTED` | 实例驳回（内容带驳回意见） | 发起人 | 实装 |
| `LUNA_SALES_CONTRACT_ACTIVE` | 合同审批通过生效 | 销售员/销售主管 | 冻结，随 W1 合同接入 |
| `LUNA_SHIPMENT_SIGNED` | 发货单签收 | 销售员 | 冻结，随发货/门户接入 |
| `LUNA_AR_PERIOD_WARNING` | 应收账期逾期/即将到期定时预警 | 财务/业务员 | 冻结，**一期不接线**（10 号 grilling 决议不做预警 job；模板文案保留待二期，逾期仅账龄页静态筛选） |

模板内容用芋道占位符（如 `您有新的审批待办：{bizName} 单号 {bizNo}，提交人 {submitterName}`），由 SQL 种子写入 `system_notify_template`，可重复执行（ON CONFLICT 更新）。

### 5.3 与审批引擎的接线

02 号 spec 已约定"创建 task 时发站内信"：审批引擎在创建 task / 实例终态处调用 `LunaNotifyHelper`（同事务 BEFORE_COMMIT 语义内，消息发送失败仅记 error 日志、**不回滚业务事务**——通知是旁路，失败可容忍，避免消息服务故障阻塞审批）。会签场景给节点每个审批人各发一条；或签有人处理后其余待办自动关闭，已发消息不撤回（铃铛自然沉淀为已读/忽略）。

### 5.4 User Story（消息）

16. 作为审批人，单据到达我这一节点时铃铛立即出现未读，点开直达审批工作台。
17. 作为发起人，审批通过/驳回后我收到消息，驳回消息里能看到驳回意见。
18. 作为财务，未来账期预警消息直接把我带到对应客户的对账页。

---

## 菜单与权限汇总

| 菜单/权限 | 类型 | 位置 |
|---|---|---|
| 回收站（10900，path `recycle-bin`） | 菜单 | 系统管理目录下 |
| `lunaerp:common:recycle:query/restore/purge` | 按钮 | 回收站页 |
| `lunaerp:sensitive:amount`、`lunaerp:sensitive:contact` | 按钮（无页面） | 系统管理目录下，供角色授权 |
| 四类主数据 `:import` | 按钮 | 各自列表工具栏 |
| `lunaerp:production:weaving-ticket:print` | 按钮 | 织造单工作台 |
| `lunaerp:business:shipment:print` | 按钮（已存在） | 发货单 |

## 配置项（infra_config 种子）

| 键 | 用途 | 默认 |
|---|---|---|
| `luna.h5.base-url` | 布票二维码 H5 基址 | 空（打印时强校验） |
| `luna.portal.base-url` | 发货单二维码门户基址 | 空（14 号门户上线前可空，发货单打印不阻断，仅二维码提示） |

## 错误码（段位 `1-030-031` 横切 common，shared GLOSSARY 段位表追加）

段内分配：000–099 回收站；100–199 Excel 导入；脱敏/消息/打印复用通用错误码不另占。

| 码 | 说明 |
|---|---|
| 1_030_031_000 | 不支持的回收站数据类型【{}】 |
| 1_030_031_001 | 记录不存在或不在回收站中 |
| 1_030_031_002 | 引用的【{}】仍在回收站，请先恢复后再恢复本单据 |
| 1_030_031_003 | 该数据已被业务引用，无法彻底删除 |
| 1_030_031_100 | 导入数据超过单次上限 {} 行，请拆分后导入 |
| 1_030_031_101 | 导入文件为空或无有效数据行 |
| 1_030_031_102 | 导入人缺少【{}】权限，无法导入含该字段的数据 |

## 统一 DoD 对照

- [x] 状态机白名单+终态守卫：仅 DRAFT 单据可删/可恢复；已过账单据回收站不可见（ADR-0038 边界），恢复非回收站行报 001
- [x] 并发条件更新：恢复/彻底删除 SQL 带 `deleted=1 AND tenant_id=?` 条件，影响行数为 0 即报错
- [x] 芋道权限+脱敏：恢复/彻底删除/导入/打印/敏感查看全部独立权限码；金额与联系人按两类固定权限位脱敏
- [x] 列表/详情/删除进回收站：12 个 W0 handler 覆盖现有主数据与草稿单据；新模块注册 handler 写入统一 DoD
- [x] 打印+操作/审批日志：打印统一 `@LogRecord` 入 `system_operate_log`；恢复/彻底删除/导入同加 `@LogRecord`
- [x] 金额逻辑测试：脱敏不改变库内值，更新接口不接受 `"****"` 回写；参考价导入非负校验
- [x] 核心副作用测试：恢复（含级联/引用阻断/幂等）、彻底删除引用守卫、整批导入回滚、条件序列化（有权限/无权限/未登录）
- [x] 前端验收清单+波次冒烟：回收站三个操作、四模板下载/导入成功与错误回报、布票批量打印+二维码扫码跳转占位页、铃铛审批消息点击跳转、两类角色脱敏肉眼核对
- [x] 性能：回收站手写 SQL 按 `(tenant_id, deleted)` + 更新时间分页，万级已删行 P95<1s；导入 5000 行同步超时内完成（批量 insert，单事务）
- [x] 租户隔离：回收站 XML 手写 tenant_id 条件并专测；导入与消息均带租户上下文；横向越权（他租户 bizId）必测
- [x] 纵向越权：无 `:restore`/`:purge`/`:import`/`:print`/sensitive 权限分别 403 或脱敏
- [x] 术语与错误码：回收站/恢复/彻底删除/套打进 shared GLOSSARY；1-030-031 入段位总表；边界落 ADR-0038

## Out of Scope

- 可视化打印模板设计器、服务端 PDF 生成、打印模板租户级自定义（Q12=A，二期）
- 报关/电子口岸对接、出口单证模板实现（07 号 ticket）、对账单模板实现（10 号 ticket）
- 已审核/已生效单据进回收站、恢复时补回库存或账务（红冲取消是唯一路径，ADR-0016/0017/0038）
- 回收站自动清空、批量彻底删除、回收站数据导出
- Excel 更新/upsert、异步导入任务、超过 5000 行大数据导入、复杂 JSON 字段（成本项/纱线配比）导入
- 字段级脱敏配置中心、更多敏感权限位、敏感词过滤（18 号研究：未迁入）
- 第二个待办角标、审批待办独立红点、短信/邮件/企微/钉钉外发通道、消息偏好设置
- 消息触达失败的可靠消息/重试队列（一期失败仅日志，不阻塞业务事务）
