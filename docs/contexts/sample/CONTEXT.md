# 样品管理域术语表（sample）

本文件是 Luna ERP **样品管理域（sample）**的权威术语表，收录样品分析及未来样品系列其他对象的领域概念。
仓库领域文档总入口见根目录 `CONTEXT-MAP.md`；跨域平台约定（租户、软删除、前后端模块约定、错误码段位总表）见 `docs/contexts/shared/CONTEXT.md`。
当对话或代码中含糊使用某个词时，以本文件为准。

## 样品管理：样品分析

**样品分析 (SampleAnalysis)**：
客户寄来针织布样后，业务员/分析员登记来样信息、反推针织工艺（用纱结构、机台参数、大圆机三角排列等）、估算成本与对外报价，并按条收取月结分析费的**分析记录/报告**，是样品管理域的第一个对象。它不是流转单据：无审批流、无过账、无状态机、不驱动也不联动任何下游单据（区别于合同/胚布单/染色单/入库单）。旧系统（linaagi，仓内 `oldserver`/`oldweb`）对应 Prisma 模型 `SampleAnalysis`；旧系统另有字段约 95% 同构的金熊工艺单（`KnittingCraftOrder`，KC 编号，服务车间工艺单场景），本期**不迁移**、不在本表预留 entryMode/类型字段。
_避免_：来样分析、SampleReport、工艺单（指金熊工艺单时需说全称）。

**样品管理域 (Sample)**：
继 baseinfo / business / production / stock 之后的第五个顶级业务域。MVP 仅含样品分析；旧"样品系列"中的样品库存台账、寄样登记未来归入本域，本期不建表、不建菜单。

**样品编号 (SampleAnalysisNo)**：
形如 `SA20260001`：`SA` + 年份 + 4 位顺序号（超 9999 自然扩位），租户内全量唯一（含软删行、不重用），新建缺省由服务端生成、也可手填（撞号报错），提供只读"预览下一个编号"接口。规则同供应商编号（ADR-0005）/合同编号（ADR-0004）。
_避免_：sampleNo（旧字段名；新系统列名 `sample_analysis_no`）。

**样品分析客户关联**：
`customerId`（可空，逻辑关联客户档案、不建数据库外键）+ `clientName` 名称快照；散客/临时来样允许不挂档案、纯手填名称。联系人 `clientContact` 与客户款号 `clientStyleNo` 均为自由文本——来样联系人常为一次性对接（旧系统全为文本），不关联 CustomerContact。

**样品分析无状态**：
不设业务状态字段、不做状态机。旧系统 `status`（ACTIVE，后期审批终态回写落点）不迁移；唯一的生命周期信号是独立的布尔付款状态（见"月结收费"）。

**来样核心属性**：
主表上描述来样与成品规格的一组字段：`productName` 产品名称（**唯一必填项**，自由文本，不关联产品档案）、`composition` 成分、`width` 门幅（CM，数值）、`weight` 克重（GSM，数值）、`meterLength` 米长（服务端计算，只读）。门幅/克重/成分是对未知来样的反推结果，多数样品永远不会变成在售产品，因此**不存 productId**；前端可基于产品档案做名称联想填充，只填字、不存 ID。

**米长 (MeterLength)**：
每公斤布的米数，`meterLength = 100000 / (width × weight)`，保存时由服务端计算落库，保留 2 位小数；门幅或克重为空、零或非正则为 NULL（前端显示 `-`）。公式原样搬迁自旧系统。

**工序 / 双染工艺 / 机型**：
来样反推出的三项工艺分类，均为**字典下拉单选、存字典 value（英文业务编码）、不存 dict_data 数字 id**（全项目字典字段惯例）：
- 工序 `process`，字典 `lunaerp_sample_process`：`DYEING` 染色 / `YARN_DYED` 色织 / `PRINTING` 印花 / `NAPPING` 刺毛；
- 双染工艺 `dyeingProcess`，字典 `lunaerp_sample_dyeing_process`，13 项：`SINGLE_T/C/R/N/M` 单染 T/C/R/N/M、`SINGLE_TENCEL` 单染天丝、`DOUBLE_TR/CVC/TC/NR/TN/TM` 双染六种、`TRIPLE_NTR` 三染 NTR；
- 机型 `machineType`，字典 `lunaerp_sample_machine_type`：`WATER_JET` 喷水织机 / `CIRCULAR_KNIT` 大圆机 / `WARP_KNIT` 经编机 / `RAPIER` 剑杆织机。
三者均可空。旧系统这三项是前端常量且允许 AI 回填列表外自由值；新系统不提供自由录入（AI 识别整体延后），干净走字典。

**机台参数 (Machine Parameters)**：
旧界面"机台生产工艺参数"卡共 15 个字段，新系统全部拍平为主表可空列、不建子表、不用 JSON：上述工序/双染工艺/机型 + `output` 实测产量（KG/天，文本）四个主级字段，加 11 个专用参数：`flowerNo` 花号、`whiteWidth` 白胚门幅、`whiteWeight` 白胚克重、`flowerWidth` 花宽、`flowerHeight` 花高、`gear` 齿轮、`clothRack` 布架、`startSpeed` 开机转数、`feederDisk` 送纱盘、`theoreticalOutput` 理论产量、`stitchLength` 线长（如 9.6cm/50针）。11 个专用参数均为自由文本（旧系统即文本，含单位/分数等混合写法）。

**纱线规格明细 (SampleAnalysisYarnSpec)**：
样品分析中逐行登记的用纱结构条目，**独立子表** `lunaerp_sample_analysis_yarn_spec`，随主表聚合差异保存、级联软删，无独立端点/权限码（ADR-0003 聚合模式）。每行**必须关联纱线规格档案**（`yarnSpecId` 必填，逻辑关联无数据库外键）：材质→规格两级下拉复用既有 `GET /lunaerp/production/yarn-spec/cascade-options`；选中后快照 `yarnTypeId`+`yarnTypeName`（材质）、`specName`（规格名）、`spinMethod`（纺法）。行字段另含 `ratio` 比例（百分比数字，如 30 表示 30%，>0）、`unitPrice` 单价（元/KG，缺省带档案 `refPrice`、可改）、`remark`。**不提供自由文本规格行**；比例不强制合计 100%，前端仅在合计 ≠100% 时文字提示（不足/超标），不拦截保存。
_避免_：yarnSpecs（旧 JSON 字段名）。

**纺法 (Spin Method)**：
纱线的纺纱工艺。样品分析下拉使用字典 `lunaerp_sample_spin_method`（id=2254）：环锭纺 / 赛络纺 / 紧密纺 / 紧密赛络纺 / 气流纺 / 涡流纺——与旧系统 `SPINNING_METHOD` 字典 6 项保持一致。该字典 **value 直接存中文**（非英文编码），因为纱线规格档案的 `spinMethod` 是自由文本且存量即中文（ADR-0011），明细行存的是档案快照值，value 用中文才能与档案、`<DictTag>` 渲染天然一致；快照值若不在字典内（档案自由文本特例）照常存储展示，不做字典强校验。纺法随规格选择自动带出、不可在手填行修改（无自由行）。

**绕纱行 (SampleAnalysisYarnArrange)**：
"纱线排列绕纱工艺"的逐行描述，**独立子表** `lunaerp_sample_analysis_yarn_arrange`，随主表聚合差异保存、级联软删。行字段均为可空自由文本：`code` 路号、`draftText` 绕纱表述（如 25.1cm/1花宽）、`borderStatus` 对针（如 第一路对针）、`description` 描述（如 开幅线；列名避让 SQL 保留词 desc）。不设行号，按创建顺序展示（同纱线采购明细先例）。
_避免_：yarnArrange（旧 JSON 字段名）。

**三角排列画板 (StitchBoard)**：
大圆机针盘三角排列的 5 态网格：默认 4 行 × 24 列，每格点击循环 5 态——空 → `v` 成圈 → `-` 集圈 → `+` 浮线 → `=` 其它；支持每次 +4/-4 列与清空。整块网格作为**单个 JSONB 列** `stitchArrange` 存主表（结构 `{columns, rows, data: string[][]}`），不建子表——它是一整块不可拆分的工艺图、无逐格查询/计算需求。前端用 Element Plus 按钮网格实现，不做手绘 canvas，不迁移旧版混纺彩色条装饰组件。
_避免_：stitchArrange（旧字段名在对话中可接受，正式列名为蛇形 `stitch_arrange`）。

**成本核算 (Cost Calculation)**：
服务端权威重算、前端只展示（同 ADR-0010 计算口径先例）。主表费用项：`productionCost` 生产工费、`dyeingCost` 染整费、`otherCost` 其他费用（旧 UI 有字段但旧公式漏算，**新系统计入总成本**，属搬家时修正）、`lossRate` 损耗率（**百分比数字 5=5%**，API 硬校验 0≤lossRate<100，越界报错；不继承旧系统"越界走加法兜底"hack）。公式：

- 纱线成本 = Σ（明细单价 × 比例/100）；明细单价 `>1000` 时自动 ÷1000（防元/吨误填，保留旧系统容错，服务端统一处理）；
- `totalCost = (纱线成本 + 生产工费 + 染整费 + 其他费用) ÷ (1 − 损耗率/100)`，四舍五入 2 位。

旧系统 `processingFee` 与 `costData.productionCost` 双字段合并为"生产工费"一个字段，旧的"工费为空用 processingFee 兜底"规则随之消失。

**供应商报价登记 (SampleAnalysisSupplierQuote)**：
成本区的参考登记子表 `lunaerp_sample_analysis_supplier_quote`，随主表聚合差异保存、级联软删。行字段：`supplierId`（可空，逻辑关联供应商档案）+ `supplierName` 名称快照（联想搜索、也允许纯手填）、`greigePrice` 胚布报价（元/KG）、`remark`。旧 JSON 中的 lossRate/amount 无录入入口，不迁移。报价行**不参与**总成本计算；不做旧系统"供应商必须 ACTIVE"守卫（停用供应商不影响历史登记）。

**对外报价 (QuotedPrice)**：
面向客户的实际报价 `quotedPrice`（元/米，可空）+ `quotedRemark` 报价备注（如"含税开票不含运费"），自由登记，与内部成本估算、月结收费互不参与计算。

**月结收费 (Monthly Billing)**：
样品分析按条向客户收取分析费的内部登记。字段：`isPaid` 付款状态（布尔，缺省 false 未付）、`unitPrice` 月结单价（元/条，缺省 30，可改）。列表支持付款三态筛选（全部/未付/已付）、单条切换与多选批量标记付款（同一端点，需独立 `:pay` 权限，对齐发货单 `:audit` 独立分权先例）。收费统计端点返回三张卡口径：未付金额 = Σ unitPrice（isPaid=false）、已付金额 = Σ unitPrice（isPaid=true）、任务总数 = count；对外报价不计入收费统计。不接财务系统、不做账期/账单。

**样品照片 (SamplePhoto)**：
来样实物照片，最多 9 张，主表 `photos` JSONB 列存框架 infra 文件服务返回的 URL **有序数组**（不沿用旧系统 base64 入库）；列表取第一张作缩略图，工作台支持上传/删除/预览。删除照片只移除主表内 URL 关联，**不物理删除 infra 文件**（避免误删被引用文件）。

**登记人 (Creator)**：
创建样品分析的内部用户，建单时写入 `creatorId`（逻辑关联 system_users）+ `creatorName` 姓名快照，之后不随编辑改变（模式同销售合同业务员、挡车工快照先例，不使用框架 creator 字段裸值）。

## 模块边界

样品分析落新建的 `sample` 顶级子包（与 baseinfo/business/production/stock 平级），后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.sample.sampleanalysis[.yarnspec|.yarnarrange|.supplierquote]`（主表一个 Controller，三个子表仅 DO/Mapper/Service 内部协作、无独立 Controller）；前端视图 `luna-ui/src/views/lunaerp-elmui/sample/sample-analysis/`（列表 index + workspace 工作台三 Tab）、前端 API 镜像于 `luna-ui/src/api/lunaerp-elmui/sample/sample-analysis/`。URL 前缀 `/lunaerp/sample/sample-analysis`，权限码前缀 `lunaerp:sample:sample-analysis:`，五个动作：`query`（菜单承载）/ `create` / `update` / `delete` / `pay`（单条与批量付款切换）；错误码段 `1-030-018-xxx`。菜单新建 **10600 样品管理**顶级目录（sort 104，path `/sample`）：**10610 样品分析**菜单 + 10611 创建 / 10612 修改 / 10613 删除 / 10614 收费四个按钮位。建表 SQL：`luna-srv/sql/postgresql/lunaerp-sample-analysis.sql`（1 主表 + 3 子表 + 4 字典 + 菜单，可重复执行）。

样品分析是**消费方**：只读引用客户档案（baseinfo）、供应商档案（baseinfo）、纱线材质/规格档案（production），一律 ID 逻辑关联 + 名称快照、零回写、零联动；删除客户/供应商/纱线规格不级联、不硬阻止（历史分析以快照留存）。

## 字典

样品分析域建四个字典（type id 2251-2254，避让产品模块 2249/2250；data id 自 3709 起）：

| type id | dict_type | 项 |
|---|---|---|
| 2251 | `lunaerp_sample_process` | DYEING 染色 / YARN_DYED 色织 / PRINTING 印花 / NAPPING 刺毛 |
| 2252 | `lunaerp_sample_dyeing_process` | 13 项双染工艺（见"工序/双染工艺/机型"术语） |
| 2253 | `lunaerp_sample_machine_type` | WATER_JET / CIRCULAR_KNIT / WARP_KNIT / RAPIER |
| 2254 | `lunaerp_sample_spin_method` | 环锭纺 / 赛络纺 / 紧密纺 / 紧密赛络纺 / 气流纺 / 涡流纺（**value 存中文**，对齐纱线档案自由文本） |

付款状态为布尔字段不建字典；机台 11 个专用参数为自由文本不建字典。

## 错误码

`1-030-018-xxx` 样品分析（主表 + 三个聚合子表共用）：

- 000 样品分析不存在
- 001 样品编号重复
- 002 关联引用不存在（客户 / 供应商 / 纱线规格）
- 003 损耗率取值非法（必须 ≥ 0 且 < 100）
- 100 纱线规格不存在或已停用（明细行 yarnSpecId 校验）

子表随主表聚合保存、无独立端点，不单独分配错误码段。
