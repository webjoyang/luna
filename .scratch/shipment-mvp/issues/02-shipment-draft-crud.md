# 02: 发货单草稿 CRUD（SH 单号 + 关联快照 + 聚合明细，端到端）

**What to build:** 打开"业务管理 → 发货管理"，可查看发货单列表（发货单号、客户、合同号、发货仓库、发货日期、总匹数、货物金额合计、运费、签收状态、单据状态、创建人/时间）、分页、多条件检索（关键词/状态/客户/仓库/合同号/发货日期区间，关键词匹配单号、合同号、客户名、卷号）、新建。点新建进入工作台"基本信息"Tab：客户必选（带出默认联系人与收货地址）、合同可空、发货仓库必选（仅 ACTIVE）、发货/收货地址、承运商、运单号、发货日期/预计到货、运费、业务员、币种、备注；在"发货明细"Tab 以可编辑表格录入明细（产品必选带快照、客户色号、颜色、缸号、数量、匹数、单位 KG/M、单价、金额、来源入库明细选择、备注），随整单聚合保存。草稿可改可删、删除级联逻辑删除明细；操作日志 Tab 占位；隐藏工作台路由。单号 SH-YYYYMMDD-NNN 按天取号、可手填（撞号报错）、提供预览。

**Blocked by:** 01 (发货域 SQL 基建)

**Status:** resolved

- [x] 发货单主表 + 发货明细 DO/Mapper/VO/Service/Controller 就位（主表 + 明细 + 后续卷清单三个 DO 同包，仅主表有 Controller）
- [x] 端点：page/get/get-next-no/create/update/delete 就位，返回 CommonResult/PageResult，VO 带 Swagger 与校验注解
- [x] SH-YYYYMMDD-NNN 按天取号 + 手填撞号报 ...016_001 + get-next-no 预览（复用入库单取号机制）
- [x] 关联校验与快照：客户必填、仓库必填且 ACTIVE；合同/业务员可空但存在性校验；每行产品必填存在；来源入库明细可空存在性校验；回填全部名称快照，违规报 ...016_003
- [x] 聚合保存：明细整表逻辑删除后重插；金额 = quantity×unit_price 与整单 total_amount/total_pieces 服务端权威重算；允许零明细草稿
- [x] 状态守卫：仅 DRAFT 可改可删，非草稿报 ...016_002；删除主表级联逻辑删除明细与卷清单
- [x] 前端列表页（检索/分页/状态与签收标签/操作列按权限显示）+ 工作台基本信息 Tab + 发货明细 Tab + 操作日志占位 Tab + 隐藏路由
- [x] Service 层 Mockito 单测：创建/更新/删除/取号/关联校验/快照回填用例

## Comments

- 后端实体与数据访问层：
  - 数据模型：`ShipmentDO`、`ShipmentItemDO`、`ShipmentRollDO` 落于 `com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment`
  - 数据访问：`ShipmentMapper`、`ShipmentItemMapper`、`ShipmentRollMapper` 落于 `com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment`
- 控制器与 VO：
  - 端点控制器：`ShipmentController`，提供 `/create`、`/update`、`/delete`、`/get`、`/page`、`/get-next-no`
  - VO 套装：`ShipmentSaveReqVO`、`ShipmentRespVO`、`ShipmentPageReqVO`、`ShipmentItemSaveReqVO`、`ShipmentItemRespVO`、`ShipmentRollSaveReqVO`、`ShipmentRollRespVO`
- 业务服务与逻辑：
  - `ShipmentService` 与 `ShipmentServiceImpl`
  - SH-YYYYMMDD-NNN 按天自动取号（超 999 自然扩位、含已删除行防撞与重试机制）+ get-next-no 预览
  - 严格关联校验与快照回填：客户与仓库必须为 ACTIVE；合同、业务员、产品、来源入库明细存在性校验
  - 聚合保存与服务端权威重算：金额 = quantity × unitPrice，整单 totalAmount 与 totalPieces 权威重算；明细与卷整表替换
  - 状态守卫：仅草稿态可编辑/删除，非草稿报 1_030_016_002；删除级联逻辑删除 items 与 rolls
- 单元测试：
  - `ShipmentServiceImplTest` 纯 Mockito 16 个测试用例全部通过，覆盖创建、更新、删除、取号、关联校验、快照回填与异常状态守卫
- 前端页面与路由：
  - API 客户端：`luna-ui/src/api/lunaerp-elmui/business/shipment/index.ts`
  - 隐藏路由：`luna-ui/src/router/modules/remaining.ts` 注册 `/business/shipment/workspace/:id?`
  - 列表页：`luna-ui/src/views/lunaerp-elmui/business/shipment/index.vue`（多条件组合检索、分页、标签渲染、操作按钮权限控制）
  - 工作台：`luna-ui/src/views/lunaerp-elmui/business/shipment/workspace/index.vue`、`BasicInfoTab.vue`、`ItemsTab.vue`、`LogsTab.vue`

