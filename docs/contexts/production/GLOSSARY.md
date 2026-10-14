# 生产管理域术语表（production）

本文件是 Luna ERP **生产管理域（production）**的权威术语表，收录打色指示单、胚布单、染色单、纱线采购、检验单、质检确认、织造执行、进度反馈、生产进度、排产与齐套检查的领域概念。
仓库领域文档总入口见根目录 `GLOSSARY-MAP.md`；跨域平台约定（租户、软删除、前后端模块约定、错误码段位总表）见 `docs/contexts/shared/GLOSSARY.md`。
当对话或代码中含糊使用某个词时，以本文件为准。

## 生产管理：打色指示单

**打色指示单 (ColorInstruction)**：
客户大货投产前，按客户提供的色号/色卡/参考样打出小样并对色确认的指示单据，记载客户/产品/规格快照、打色承接方、逐颜色的灯源与确认结果，是生产管理域时序最早的单据（在胚布单之前）。打色单**独立建档**，不依赖样品链、不挂样品菜单；一张打色单服务一个客户的一个产品；不产生库存/账务副作用。通过确认的打色单可作为胚布单/染色单的颜色来源单据。
_避免_：打色单（口语可用，正式名称为"打色指示单"）、LabDipInstruction（旧系统名称，保留以追溯）、打样单（打样含寄样/选样等更广语义，二期）。

**打色单号 (ColorInstructionNo)**：
形如 `DS20261007001`：`DS` + 年月日 8 位 + 3 位日流水（超 999 自然扩位），按天取号，租户内全量唯一、软删不释放（仿 ADR-0006 胚布单号）。可手填，撞号报错。

**打色颜色行 (ColorInstructionColor)**：
一张打色指示单中按颜色逐行登记打色要求与结果的条目，1~N 行，随单聚合、无独立操作界面（同 ADR-0003）。颜色身份字段与染色颜色明细、发货明细**全域同口径**：`customerColorNo`（客户色号）+ `colorName`（颜色名称），另含 `factoryColorNo`（厂内色号）、`colorHex`（色值，列表色块/取色器）、`lightPrimary/lightSecondary`（主辅灯源）、`colorCardRef`（色卡参考）、`samplePhotos`（样布照片，JSON 数组）、`copies`（份数）、`round`（打色轮次）、行状态。保存为差异保存 + 行状态守卫（非删旧插新——须保留驳回轮次）。
_避免_：colors（旧系统 JSON 字段名）、颜色明细（口语，正式为"打色颜色行"）。

**客户色号 (CustomerColorNo)**：
客户对某一颜色给定的编号，是颜色在全域的权威身份字段。打色颜色行、染色颜色明细、发货明细统一使用 `customerColorNo` 同一字段口径；合同明细的弱文本字段 `customerColor` 不沿用。

**灯源 (LightSource)**：
对色时使用的标准照明光源，字典 `lunaerp_color_light_source`：D65（标准日光）/ TL84（三基色荧光）/ CWF（冷白荧光）/ A（白炽灯）/ UV（紫外）/ TL83（暖白荧光）。打色颜色行上主灯源必填、辅灯源可空——灯源是比色硬口径，同一颜色在不同灯源下的评价可能不同。

**色卡参考 (ColorCardRef)**：
客户给出颜色依据的色卡编号描述（如 `Pantone TPX 19-4052`），打色颜色行上的自由文本，可空。区别于客户色号：色卡参考指向通用色卡体系，客户色号是客户自有编号。

**打色轮次 (ColorRound)**：
同一颜色被驳回后重新打色的次数标识，首轮 round=1，每"驳回重打"一次 round+1。驳回重打时旧行置 REJECTED 冻结留痕（记录驳回意见），克隆新行继承颜色与灯源等字段，**样布照片不继承**（上一轮结果物）。打印件输出全部轮次行。

**颜色行状态 (ColorLineStatus)**：
字典 `lunaerp_color_line_status`：`PENDING` 待结果 / `APPROVED` 通过 / `REJECTED` 驳回。仅 PENDING 行可改可删；冻结行不可改不可删。允许对 PENDING 或 APPROVED 行执行"驳回重打"（覆盖先通过后改口场景）。

**打色承接方 (LabFactory)**：
承担打色任务的一方，引用供应商档案并保存公司名快照，**可空——空即本厂打色房自做**（不使用占位供应商，同 ADR-0022 自织先例）。

**打色单状态 (ColorInstructionStatus)**：
五态字典 `lunaerp_color_instruction_status`：`PENDING` 待打色（创建缺省）/ `IN_PROGRESS` 打色中 / `CONFIRMED` 已确认 / `IN_PRODUCTION` 已投产 / `VOID` 已作废。确认须选择确认类型 `confirmType=CUSTOMER/INTERNAL`（客户确认/内部确认，内部枚举不入字典）；确认守卫：无 PENDING 行且至少一行 APPROVED。CONFIRMED 在无下游引用时可回退；投产由下游胚布单/染色单引用**自动触发**（胚布单保存引用即触发，染色单下达 ACTIVE 时触发，幂等条件更新），IN_PRODUCTION 与 VOID 为终态。仅 PENDING 可软删进回收站，CONFIRMED 后取消走作废；下游作废不回退投产状态。

## 生产管理：胚布单

**胚布单 (GreigeOrder)**：
织布生产的源头单据，记载品种规格、投胚/成品数量、损耗与交期，是继打色指示单之后织造环节的第一个单据。通过 `productionMode` 区分两种执行方式：`OUTSOURCE` 外发（向**外部坯布厂**下达织造委托，缺省）/ `SELF` 自织（由本厂车间织造，作为"织造单"的上游自产计划）。两种模式同表承载，存量单据默认 OUTSOURCE 语义不变（见 ADR-0022）。
_避免_：胚布指示单、GreigeInstruction（旧系统名称，保留以追溯）、订单。

**生产模式 (ProductionMode)**：
胚布单的执行方式，字典 `lunaerp_greige_production_mode`：`SELF` 自织 / `OUTSOURCE` 外发（缺省）。仅 `SELF` 态胚布单可作为织造单的来源。自织胚布单的实际织造进度在"织造单"侧体现：胚布单详情实时聚合展示"已织匹数/计划匹数"只读进度，不回写胚布单状态（零回写惯例）。

**颜色投胚分配 (GreigeOrderColor)**：
一张胚布单中按颜色逐行拆分成品下单量与损耗，并据此折算投胚需求的条目。一份胚布单含 0~N 行，随单聚合保存、级联删除，没有独立操作界面（同合同明细模式，见 ADR-0003）。
_避免_：颜色明细（口语）、sourceColors（旧系统 JSON 字段名）。

**承织坯布厂 (GreigeFactory)**：
承担本单织造任务的供应商；**外发（OUTSOURCE）时必填，自织（SELF）时可空**；一张胚布单一个承织方。引用供应商档案并保存公司名快照，供应商改名不回写。自织时不使用"本厂"占位供应商——避免污染供应商档案与外发统计口径（ADR-0022）。

**指定染厂 (DyeingFactory)**：
本单预设的后续染色供应商，可空——下单时染厂可能尚未确定。同样引用供应商档案并保存名称快照。

**计划投胚量 (PlannedQty)**：
下达给坯布厂的总投胚量，属工厂经验口径。它与颜色投胚分配按成品量折算的投胚需求之和**刻意不强制相等**：两者口径不同，强一致会挡住正常单据。

**发染记录 (GreigeDispatchRecord)**：
胚布单发往染厂的轻量流转记录，补齐"胚布→染厂"的物流血缘环节。挂胚布单（`greigeOrderId` 必填），可空指向染色单（`dyeingOrderId`，可后续补填）与承染染厂（供应商 ID+名称快照），记录发染日期、数量 KG、匹数与产品快照。创建即生效并写库存流水 `PROCESS_OUT`（扣胚布库存），删除写 `PROCESS_OUT_REVERSAL` 红冲；零回写胚布单与染色单状态（同进度反馈零回写惯例）。无业务编号（同染色单先例），靠"胚布单+日期"识别。双入口：胚布单工作台与染色单工作台各设"发染记录"页签，复用同一套接口；染色单侧只读胚布单信息、不回写。
_避免_：拉胚记录、FabricPullRecord（旧系统名称，保留以追溯）、发胚布（口语）。

**胚布单号 (GreigeOrderNo)**：
形如 `PB0919001`：`PB` + 月日 + 3 位顺序号（超 999 自然扩位），**按天取号**，租户内全量唯一、软删不释放（见 ADR-0006）。新建缺省由服务端生成、也可手填（撞号报错）。

**胚布单状态 (GreigeOrderStatus)**：
`ACTIVE` 进行中（新建缺省）/ `COMPLETED` 已完成 / `CANCELLED` 已取消，与销售合同状态口径一致。MVP 无审批流，由编辑页手动切换，不做流转约束。

## 生产管理：染色单

**染色单 (DyeingOrder)**：
向染厂下达的染色委托单，记载客户/合同/产品规格/染色要求与颜色明细，是生产管理域的第二个单据（继胚布单之后）。MVP 不接审批流、不做坯布库存扣减、不联动染缸批次卡与检验单。无业务编号（与胚布单/合同不同，染色单不带 order_no）。关联字段全部存 ID + 名称快照：`salesContractId`+`contractNo`（合同）、`customerId`+`clientName`（客户，从合同带出）、`dyeingFactoryId`+`dyeingFactoryName`（承染方，供应商快照），与项目其他单据惯例一致。
_避免_：染色指示单、DyeingInstruction（旧系统名称，保留以追溯）、染单（口语）。

**染色颜色明细 (DyeingColor)**：
一张染色单中按颜色逐行拆分染色要求与数量的条目。一份染色单含 0~N 行，随单聚合保存、级联删除，没有独立操作界面（同合同明细与胚布单颜色分配模式，见 ADR-0003）。明细字段：customerColorNo（客户色号，从合同明细 customerColor 带出可改）、colorName（颜色名称，手填）、quantity（本单染色数量，成品口径）、pieces（匹数）、dyeingInputQty（投染数，含损耗的实际投入量）、dyeingInputUnit（投染数单位，复用字典 lunaerp_sales_contract_unit：KG/M）、contractQty（合同数量，从合同明细 quantity 带出只读对比）。
_避免_：颜色明细（口语）、colors（旧系统 JSON 字段名）。

**染色技术规格与色牢度 (Dyeing technical specs & fastness)**：
染色单主表上的一组工艺/质量要求字段，与参考项目 DyeingInstruction 对齐：loss（损耗率）、ph（pH 值）、shrinkage（缩水率）、fastnessWash（水洗牢度）、fastnessSoap（皂洗牢度）、fastnessDryRub（干摩擦牢度）、fastnessWetRub（湿摩擦牢度）、fastnessLight（日晒牢度）、fastnessPerspiration（汗渍牢度）、fastnessDryClean（干洗牢度）、fastnessSixFiber（六千沾色牢度）、otherReq（其他要求）。均为可空文本，不建字典。

**承染方 (DyeingFactory)**：
承担本单染色任务的供应商，必填；一张染色单一个承染方。引用供应商档案并保存公司名快照。与胚布单的"指定染厂"是同一概念的复用——胚布单下单时若已确定染厂则填指定染厂，染色单下单时落实为本单的承染方。

**染色单状态 (DyeingOrderStatus)**：
四态：`DRAFT` 草稿（缺省）/ `ACTIVE` 进行中 / `COMPLETED` 已完成 / `CANCELLED` 已取消。比销售合同/胚布单多一个 `DRAFT` 态，因为染色单业务上存在"先打草稿反复确认、再下达给染厂"的形态，这是与合同/胚布单的刻意不一致。保存时默认为草稿，点"下达"转 ACTIVE；编辑页可手动切换 COMPLETED/CANCELLED。

## 生产管理：纱线采购

**纱线材质 (YarnType)**：
纱线按材质类型归档的第一级主数据，是纱线规格的父级。`lunaerp_production_yarn_type` 主表仅含 name/sort/status + 框架列，**不内置 seed**，主数据由纱线档案页面创建维护（如棉/涤/人棉/氨纶/锦纶/粘胶/混纺）。**材质类型不入字典**（见 ADR-0011）。
_避免_：纱线类型、YarnCategory（旧系统术语保留追溯）。

**纱线规格 (YarnSpec)**：
具体可采购的纱线规格，挂在材质之下，是采购明细行直接关联的实体。`lunaerp_production_yarn_spec` 子表含 yarn_type_id 关联 + spec_name（如"32S 棉"）+ spin_method（纺纱工艺）+ luster（光泽）+ standard_name（标准名称，唯一索引 [tenant_id, yarn_type_id, standard_name]）+ ref_price（参考单价）+ remark/status。**ref_price 是纱价表无生效记录时的取价兜底来源**（ADR-0011；纱价表上线后语义降级为"最后兜底"，取价优先级见"纱价"，ADR-0039）。

**纱价 (YarnPrice)**：
挂在某一纱线规格上、按价类与生效日期维护的价格主数据，单位元/KG。`lunaerp_production_yarn_price` 含 yarn_spec_id（+yarn_type_id/spec_name 冗余快照）+ price_type（字典 `lunaerp_yarn_price_type`：PURCHASE 采购参考价（缺省）/MARKET 市场参考价）+ price + effective_date 生效日期 + source（HAND 手工/EXCEL 导入）+ remark。同规格同价类同一生效日期仅一条（部分唯一索引 WHERE deleted=0，删错可重录），保留完整历史。**取价规则**：不晚于当日的最近生效价；无记录回退规格 ref_price（响应标记 SPEC_REF_FALLBACK，不计缺价）；两者皆无才算缺价。维护入口是纱线档案页规格行的「价格记录」抽屉（不设一级菜单），支持 W0 Excel 导入；纱线采购实际单价只做"最近采购价"只读聚合参考，一期不自动回写（ADR-0039）。
_避免_：纱线行情（爬虫行情二期）、在规格表上加价格列（不留历史，已否决）。

**纱线配比 BOM (YarnRecipeBom)**：
某产品品种生产时所需的纱线配比清单，是纱线需求量测算与成本卡算价的**唯一权威源**。`lunaerp_production_yarn_recipe_bom` 主表通过 product_id 强关联产品档案（含 product_code/product_name 快照），明细表 `lunaerp_production_yarn_recipe_bom_item` 含 yarn_type_id + yarn_spec_id + ratio（**百分比数字，49 = 49%**）+ spin_method + remark + ref_price；成本卡落地时补齐五个测算列（均可空）：waste_rate 投入损耗率%、direction（GENERAL/WARP/WEFT）、density 密度、crimp_rate 织缩率%、consumption_kg_per_meter 每米用量（ADR-0039，承接原仅存在于产品 yarn_specs JSON 的测算参数）。
_避免_：复用产品档案 yarn_specs 字段（旧系统方案，已废弃，见 ADR-0011；ADR-0039 完成双源收敛，产品 JSON 与前端算价函数冻结）。配比权威源独立成表是因为配比与产品档案生命周期不同，且需单独维护与版本化。

**纱线采购单 (YarnOrder)**：
向纱线供应商下达的纱线采购委托单，记载一批胚布指示单共同驱动的纱线采购需求。`lunaerp_production_yarn_order` 主表通过 `source_instructions`（Text JSON 数组）关联多张胚布指示单 ID，多供应商挂在明细行（不在主表），含 order_date/delivery_date/loss_rate（**百分比数字，5 = 5%**）/status/remark。命名 `yarnorder`（不使用 yarnpurchase，见用户决策）。
_避免_：纱线采购指令、YarnPurchase（命名已纠正）。

**纱线采购明细 (YarnOrderItem)**：
采购单中按纱线规格逐行展开的采购条目。`lunaerp_production_yarn_order_item` 明细表关联 yarn_type_id + yarn_spec_id + supplier_id（多供应商挂在明细行 + 公司名快照），含三量字段 `required_qty`/`stock_qty`/`purchase_qty`（需求量/在库量/实采量）+ ratio（百分比数字）+ loss_rate（百分比数字）+ base_qty（基础量）+ unit_price + total_amount + ref_unit_price（历史价回填）+ remark。**无 line_no 行号字段**（用户决策）。明细随主表聚合保存、级联逻辑删除（同 ADR-0003 模式）。

**纱线采购单号 (YarnOrderNo)**：
形如 `YP20260920001`：`YP` + 年月日 + 3 位日流水（超 999 自然扩位），**按天取号**，租户内全量唯一、软删不释放（仿胚布单 ADR-0006 模式）。新建缺省由服务端生成、也可手填（撞号报错）。

**纱线采购单状态 (YarnOrderStatus)**：
四态：`DRAFT` 草稿（缺省）/ `ACTIVE` 进行中 / `COMPLETED` 已完成 / `CANCELLED` 已取消，仿染色单 ADR-0008。MVP 无审批流。（原规划完成时写 STOCK_IN 台账已于 2026-09-21 撤销回退，库存台账暂缓开发）。

**纱线库存台账 (StockLedger)**：
（已于 2026-09-21 撤销回退，暂不开发；前后端程序与表全部取消）。

**测算口径 (Yarn demand calculation)**：
按"勾选胚布指示单 → 查 yarn_recipe_bom 拿配比 → 按纱线规格合并多单需求 → 算实采量"的核心业务流测算。算法：
- `baseQty = Σ(胚布单成品量 × ratio/100)`，按 yarn_spec_id 合并多张胚布单
- `requiredQty = baseQty × (1 + lossRate/100)`（lossRate 为百分比数字 5 = 5%）
- `purchaseQty = requiredQty`（库存台账未启用，在库量 stockQty 缺省 0）
- `totalAmount = unitPrice × purchaseQty`（金额服务端权威重算）

**与胚布单 loss_rate 小数+除法公式刻意不一致**：胚布单用 `demand_qty = finished_qty ÷ (1 - loss_rate)`（loss_rate 为小数 0.05 = 5%）。这是两个单据业务语义不同（胚布单是投胚需求折算、纱线采购单是含损耗的采购量），刻意不一致（ADR-0010）。

测算接口为后端 `POST /lunaerp/production/yarn-order/calculate`，前端只展示结果不做计算（ADR-0010）。

**入库来源 (StockInSource)**：
（已于 2026-09-21 撤销回退，暂不开发）。

## 生产管理：检验单

**检验单 (InspectionOrder)**：
对胚布或成品布逐卷/逐匹检验的委托单，记载目标量、承检方与录检结果，是生产管理域的检验环节单据。除手工新建外，胚布检验单（`type=GREIGE, packageMode=PIECE`）可由织造单"交接检验"find-or-create 生成（ADR-0023/0025）。`type` 区分两类：`GREIGE` 胚布检验（关联 `greigeOrderId`）/ `FINISHED` 成品检验（关联 `dyeingOrderId`）。
_避免_：检验指示单、InspectionInstruction（旧系统名称，保留以追溯）。

**包装模式 (PackageMode)**：
检验单录入对象的物理形态，字典 `lunaerp_inspection_package_mode`：`ROLL` 卷装（缺省，存量无感）/ `PIECE` 匹装（每匹独立录入）。由织造布票交接生成的胚布检验单为 `PIECE`——布票每匹一码（见 ADR-0023/0025）。模式决定录检明细的编号字段与唯一性规则（见下）。

**录检明细 (InspectionOrderItem)**：
检验单下逐条录入的检验结果条目。公共字段：`vatNo`（缸号，胚布场景可空）、`measuredWeight`（实称重量 KG）、`lengthM`（米数）、`machineNo`（机台号，可空）、`grade`（等级，字典 `lunaerp_inspection_grade`，可空）、`colorName`/`colorNo`/`styleNo`（颜色/色号/款号，可空）、`productCode`+`productName`（冗余快照）、`pieceCount`（匹数）、`photos`（疵点照片 URL JSON 数组，≤6，W4 车间 H5 录入，PC 只读）、`remarks`。编号字段随包装模式分化（ADR-0025）：
- **ROLL 卷装**：`rollNo`（卷号）必填、`pieceNo` 禁填；`pieceCount` 可 >1（一卷含 N 匹），缺省 1；单内 `(vatNo, rollNo)` 唯一。
- **PIECE 匹装**：`pieceNo`（匹号）必填——一卷拆多匹时可在多行填同一个 `rollNo`（**允许 rollNo 重复**，rollNo 可空可填），`pieceCount` 恒 1 并隐藏（每匹一行，匹数=行数；防统计翻倍与无身份匹）；单内 `(vatNo, pieceNo)` 唯一。

明细加 `weavingTicketId`（可空）：织造布票交接后由检验环节产生明细时回填，承载"布票↔检验明细"血缘（布票自身不存检验编号，见 ADR-0023）。本期不做 `defects` 疵点数组。**作为独立子资源处理**，有自己的 CRUD 端点与权限码（刻意不一致 ADR-0003 聚合保存模式，理由见 ADR-0014），逐条增删改；删主表时级联逻辑删除明细。单内唯一性由 service 层校验，不建数据库唯一约束（用户决策）。

**承检方 (InspectionFactory)**：
承担本单检验任务的供应商，可空；引用供应商档案并保存公司名快照（`inspectionFactoryId`+`inspectionFactoryName`）。命名 `inspectionFactory` 而非 `dyeingFactory`，因 `type=GREIGE` 胚布检验时承检方可能是坯布厂/检验仓库/检测中心（供应商字典含这些类型），未必是染厂，`dyeingFactory` 在胚布检验场景下有命名歧义。

**检验单号 (InspectionOrderNo)**：
形如 `INS-20260921-001`：`INS` + `-` + 年月日 8 位 + `-` + 3 位日流水（超 999 自然扩位），**按天取号**，租户内全量唯一、软删不释放。**带连字符分隔**，与项目其他单据编号（合同/胚布单/纱线采购单/供应商均无连字符）刻意不一致，理由见 ADR-0015。新建缺省由服务端生成、也可手填（撞号报错）。

**检验单状态 (InspectionOrderStatus)**：
四态：`DRAFT` 草稿（新建缺省）/ `ACTIVE` 录检中 / `COMPLETED` 录检完成 / `CANCELLED` 已取消，仿染色单/纱线采购单四态。MVP 无审批流，编辑页手动切换，不做流转约束（与合同/胚布单/染色单 MVP 惯例一致）。第一卷录检保存时由后端便利地把 DRAFT 转 ACTIVE（不强约束）。**不含 `SYNCED` 态**——同步入库功能本期延后（ADR-0013），`SYNCED` 留给同步上线时插入，避免出现不可达死状态。

**检验等级 (InspectionGrade)**：
字典 `lunaerp_inspection_grade`（id=2232），4 项：`A` 一等品 / `B` 二等品 / `C` 三等品 / `DEFECTIVE` 等外品。本期无同步入库，grade 仅作记录与列表筛选，不映射到入库卷状态（rollStatus 是同步入库的概念，本期不入字典）。

## 生产管理：质检确认

**质检确认 (InspectionConfirmation)**：
对检验录检发现的非一等品卷（等级 B/C/DEFECTIVE）逐卷**认定质量责任**的记录，回答"这卷降等/次品是谁的责任、扣不扣挡车工的钱"。检验单负责录结果，质检确认负责定责任，两者分工不重叠。来源有二：检验单工作台幂等生成（`INSPECTION`，关联检验单+录检明细并快照卷信息）与文员手工登记（`MANUAL`，关联可空、快照手填、提交即已认定、可补录认定时间）。**无业务编号**（同染色单 ADR-0009 口径），靠"检验单号+卷号+认定链"识别。**以检验单录检明细为源而非入库明细、显式幂等生成**，详见 ADR-0019。
_避免_：质检审核、质量复核、QualityCheck、InspectionApprove。

**责任类型 (Responsibility)**：
三选一：`EQUIPMENT` 设备故障（免扣）/ `OPERATOR` 操作失误（挡车工扣款，**挡车工必填**）/ `WAIVED` 免扣（不追责）。字典 `lunaerp_inspection_responsibility`。仅 OPERATOR 且已认定未撤销的记录进入工资扣款汇总口径。

**质检确认状态 (InspectionConfirmationStatus)**：
`PENDING` 待确认（生成或撤销克隆后的缺省态）/ `CONFIRMED` 已认定 / `REVOKED` 已撤销（冻结留痕，不进主列表，仅在同链详情可见）。撤销 = 旧行 REVOKED 冻结（字段不清空）+ 按同一 `chainKey` 克隆一条新 PENDING；**所有记录一律不可删除**，误生成的待确认只能以 WAIVED 认定结案。详见 ADR-0020。

**挡车工 (Worker)**：
对该卷生产操作负责的车间工人，是操作失误扣款的主体。**复用 system 域人员体系**：`system_post` 中 seed"挡车工"岗位，挡车工即关联该岗位的 `system_users` 用户；记录存 `workerUserId` + 姓名快照，不新建员工表（ADR-0021，仿销售合同业务员先例）。自动生成时无机台/任务数据可反查挡车工，由认定人在确认时选择；仅责任=OPERATOR 必填。
_避免_：员工（本项目不建独立员工表）、操作工、workerName 裸文本。

**质检确认来源 (ConfirmationSource)**：
`INSPECTION` 检验单引入（关联检验单 ID/单号 + 录检明细 ID，生成时快照卷号/缸号/等级/机台/产品/颜色/重量/米数）/ `MANUAL` 手工登记（文员代录、可跨天补录认定时间，源单关联可空、快照字段手填，提交即为 CONFIRMED）。仅后端枚举使用，不入字典。

**认定日期双口径**：
批量打印签字单按 `createdAt`（**发现日**：卷被录检为非一等品、待确认记录生成的日期）；工资扣款汇总按 `confirmedAt`（**确认日**：领班认定责任的日期）。两套口径刻意不同、不可混用（沿参考系统）。工资扣款汇总为**只读查询**：仅统计 CONFIRMED + OPERATOR + 未撤销记录，按挡车工（userId）聚合卷数并列明细；本期不做同步标记、不写工资系统（工资模块尚不存在）。

## 生产管理：织造执行

**织造单 (WeavingOrder)**：
本厂车间一次织造执行的主单据，从自织（`productionMode=SELF`）胚布单创建（一个胚布单可分批创建 N 张织造单），记载品种规格快照、计划匹数（整数）、计划总重量与执行状态。字段：`code`、`greigeOrderId`（必填）、`contractNo`、`productId`+`productCode`+`productName`、`composition`、`gsm`、`width`、`plannedQty`（计划匹数）、`plannedWeight`（KG）、`status`、`remark`。创建时聚合该胚布单已有织造单计划匹数，超量**警告但允许**（软校验，临时加单是常态）。
_避免_：织造批次（旧系统 WeavingBatch 名称，保留追溯）、织造指示单。

**织造单状态 (WeavingOrderStatus)**：
字典 `lunaerp_weaving_order_status`，四态：`DRAFT` 待排产（新建缺省）/ `ACTIVE` 生产中 / `COMPLETED` 已完工 / `CANCELLED` 已取消。DRAFT→ACTIVE→COMPLETED 由下属布票状态**自动推导**（首个布票开工→ACTIVE；全部有效票完工→COMPLETED），CANCELLED 手动。不留"部分完工"态（ACTIVE 覆盖）。状态机守卫细节见 ADR-0024。

**机台任务 (MachineTask)**：
织造单排产到某台织机的派工任务——"哪台机织多少匹"的载体，也是**织造排产唯一的机台占用事实源**（ADR-0041：排产不另建实体）。字段：`code`、`weavingOrderId`、`loomId`+`loomNo`（必填+快照）、`workerUserId`+`workerName`（挡车工，关联 system User，可空，模式见 ADR-0021）、`plannedQty`、`actualQty`（布票聚合，只读）、`plannedStart`/`plannedEnd`（普通日期，可空）、`status`、`haltReason`、`haltAt`、`remark`。单内任务计划匹数合计超织造单时警告不拦截；**同织机计划时段闭区间相交（同日即冲突）硬拦截**（仅与 PENDING/IN_PROGRESS/HALTED 他任务比，COMPLETED 豁免，无日期不占时段，ADR-0041）。
_避免_：派工单、工单。

**机台任务状态 (MachineTaskStatus)**：
字典 `lunaerp_weaving_task_status`，四态：`PENDING` 待开始（缺省）/ `IN_PROGRESS` 生产中 / `HALTED` 暂停异常 / `COMPLETED` 已完工。生产中/已完工由布票状态推导（首个在产→生产中；全部有效票完工→已完工，actualQty 同步聚合）；`HALTED` 是**人工终态**：自动推导只更新 actualQty、不覆盖 HALTED，须手动"恢复"才能解除；已完工任务不得再标停机（ADR-0024）。

**布票 (WeavingTicket)**：
织造执行中**每匹布**的二维码生产凭证（一**匹**一码，票号=二维码内容），挂在织造单上、排产归属机台任务。字段：`code`、`weavingOrderId`、`machineTaskId`、`plannedMachine`（建议机台，生成时带入）、`actualMachine`（开工时写入=真相）、`plannedWeight`（单匹计划重量）、`workerUserId`+`workerName`（开工写入）、`materialBatchNo`（纱批号/原料批号，可空，区别于生产批次）、`status`、`exceptionType`+`exceptionNote`+`exceptionPhotos`（异常现场照片 URL JSON 数组，≤6，W4 车间 H5 录入）、`startedAt`/`completedAt`、`voidReason`/`voidedAt`。布票按机台任务生成，**票号流水按织造单（批次）维度计数**——同单内票号全局不撞；作废票占号不重用。Web 端保留勾选操作 + 布票号输入框回车（兼容扫码枪键盘输入）；W4 起新增**车间移动 H5** 扫码报工入口（扫票开工/完工/异常登记，异常可挂 ≤6 张现场照片，见"车间移动 H5"），布票二维码 `/h5/scan/weaving-ticket?code=` 码格式由 04 号统一打印冻结。
_避免_：匹条、落布票、卷票。

**布票状态 (WeavingTicketStatus)**：
字典 `lunaerp_weaving_ticket_status`，七态：`GENERATED` 已生成（缺省）/ `STARTED` 已开工 / `COMPLETED` 已完工 / `INSPECTED` 已检验 / `STOCKED` 已入库 / `EXCEPTION` 异常 / `VOID` 作废。流转只能向前走且有守卫（ADR-0024）：开工允许从 GENERATED/STARTED/EXCEPTION 进入（异常恢复、转机重扫更新 actualMachine 均为设计内恢复路径）；完工允许从 STARTED 进入；异常可从 GENERATED/STARTED 进入；作废为人工终态。检验明细按 `weavingTicketId` 审核保存时回写 INSPECTED；胚布入库审核过账时回写 STOCKED。

**异常类型 (WeavingExceptionType)**：
布票异常分类，字典 `lunaerp_weaving_exception_type`：`YARN_BREAK` 断纱 / `CLOTH_DAMAGE` 坏布 / `YARN_SHORTAGE` 缺纱停机 / `MACHINE_CHANGE` 换机 / `WORKER_CHANGE` 换人。配合布票上的 `exceptionNote` 自由说明。

**织机 (LoomMachine)**：
织造车间的织机设备档案（本模块自建的轻量机台主数据，尚无更上层设备域）。字段：`loomNo`（机台号，租户内唯一含软删行）、`model`（型号）、`status`（ACTIVE/INACTIVE，不建字典）、`remark`。不提供物理删除：被机台任务引用时禁删、引导停用（仿仓库删除守卫）。
_避免_：织布机、大圆机（口语）。

**领纱单 (YarnRequisition)**：
织造单领用纱线的申请单，挂织造单（一单一申请，可多次申请）。字段：`code`、`weavingOrderId`、`status`、`remark`。状态字典 `lunaerp_yarn_requisition_status`，五态：`DRAFT` 草稿（缺省）/ `ACTIVE` 待发纱 / `PARTIAL` 部分发纱 / `DISPATCHED` 已发纱 / `CLOSED` 已关闭。首次发纱→PARTIAL，发齐→DISPATCHED（由发纱进度推导），CLOSED 手动。
_避免_：领料单、领纱指示单。

**领纱明细 (YarnRequisitionItem)**：
领纱单中按纱线规格逐行的需求条目，**独立子资源**（逐行发纱、有独立端点，仿 ADR-0014）。字段：`yarnTypeId`+`yarnSpecId`+`yarnSpecName`（快照）、`requiredQty`（需求量）、`dispatchedQty`（已发量，缺省 0）、`unit`（KG）、`remark`。需求可由后端按配比 BOM 测算填充（可手改）；发纱写库存流水 `YARN_OUT`，库存不足时**拦截报错**（由人决定改量或分批，不出现负库存）；支持撤销发纱红冲（`YARN_OUT_REVERSAL`），见 ADR-0027。

**领纱测算 (Yarn requisition calculation)**：
后端 `calculate` 端点（前端不计算，同 ADR-0010 先例）：`某纱线规格需求 KG = 织造单计划匹数 × 单匹计划重量 × ratio/100`，按 `yarnSpecId` 合并该产品配比 BOM（`yarn_recipe_bom`，ratio 为百分比数字 49=49%）；单匹计划重量 = 织造单 `plannedWeight / plannedQty`；无 BOM 时提示手工填，结果可改。

**IoT 厂商凭证 (VendorCredential)**：
设备数据接入中每客户/采购批次持有的一组厂商 API 凭证。字段：`vendorAccount`、`vendorPasswordEnc`（**只存加密值，禁止明文**）、`scope`（覆盖设备范围说明）、`status`（启用/停用）、`lastSyncAt`。租户内账号唯一。

**IoT 设备绑定 (DeviceBinding)**：
厂商设备 → 本系统织机的归属映射（归属权威路由，防跨租户污染）。字段：`vendorDeviceId`（厂商侧设备唯一标识）、`loomId`（绑定本系统织机）、`status`、`lastSyncAt`。

**IoT 实时快照 (MachineRealtime)**：
采集的设备最新值，按 `(租户, vendorDeviceId)` 唯一。字段：`status`（running/idle/abnormal/disabled/offline）、`runEfficiencyPct`（运转效率）、`minuteCircleSpeed`（分钟圈速）、`shiftCircles`（班次圈数）、`limitSpeed`、`runMinutes`（运转分钟）、`shiftMinutes`（在班分钟）、`staffOnShift`、`deviceSn`、`fetchedAt`。本期**模型与展示先行、真实采集后置**（ADR-0028）：采集层定义 `LoomDataCollector` 接口，提供手工录入端点+模拟实现，真实定时对接待厂商协议/凭证/现场网络就绪后补；快照不回写织机档案 status（两个信息源不互相污染）。

## 生产管理：进度反馈

**进度反馈 (ProgressFeedback)**：
对**外发给染厂**的染色订单进行生产进度跟单的记录体系。本期仅覆盖外发染色环节：自织织造进度已由布票体系自动推导（见织造执行），不重复建设；外发胚布进度不在本期。进度反馈是染色单之上的**状态覆盖层（Overlay）**：以染色颜色明细行为骨架、以"缸"为最小反馈粒度，只记录执行状态与沟通过程，**不回写染色单任何数据**（零回写惯例），也不替代染色单制单。进度由内部跟单员依据染厂电话/微信汇报在 Web 端代录；MVP 不设染厂外部账号、不做移动端。架构取舍见 ADR-0031。
_避免_：进度上报、进度汇报。与**生产进度**（全链路聚合视图，见下节）区分：进度反馈只是生产进度中染色环节数据的来源，两者不是同义词。

**反馈缸 (ProgressVat)**：
进度反馈的最小记录单位——某张染色单某个颜色行下投染的"一缸"。一个颜色行可分多缸，每缸一条反馈缸记录。字段：`dyeingOrderId` + `dyeingColorId`（稳定关联；颜色行 ID 的稳定性由聚合子表差异保存保证，见 ADR-0032）、`vatNo` 缸号（必填自由文本，同单同色下唯一，由 service 层校验、不建数据库唯一约束）、`quantity` 本缸数量（>0，单位跟随颜色行 KG/M）、`pieces` 匹数（可空）、状态与五个阶段日期戳、`delayReason` 延期原因、`remark`。**不带业务编号**（同染色单 ADR-0009 先例），靠"染色单+颜色+缸号"识别。**零展示快照**：合同号/客户/染厂/产品/颜色等展示信息不落库，列表与详情全部经染色单主表与颜色行子查询实时取出（刻意不一致全项目 ID+名称快照惯例，见 ADR-0031）。
_避免_：缸卡（指反馈缸的 UI 呈现，非领域对象）、染缸（染厂设备语义）。

**进度状态 (ProgressFeedbackStatus)**：
固定六工序链，字典 `lunaerp_progress_feedback_status`（id=2247）：`NOT_SCHEDULED` 未排缸（覆盖层虚拟态，对应没有任何反馈缸记录的颜色行）/ `SCHEDULED` 已排缸（建缸时的缺省态）/ `DYEING` 染色中 / `FIXING` 固色中 / `QUALITY_CHECK` 质检中 / `COMPLETED` 已完成。状态只能逐格前进，"推进"动作自动在新阶段盖当天日期戳；编辑时可修正各阶段日期、可回退状态（需二次确认，录错纠偏是刚需）。无"取消/作废"态：录错的缸直接删除，颜色行回到虚拟未排缸，不影响染色单。不引入参考系统的自定义工序模板/工序组合器——不同染厂的工艺差异本期以固定六步承载。

**阶段日期戳 (StageDate)**：
五个阶段日期：排缸日、染色开始日、固色开始日、质检日、完成日，分别对应六态间的五次推进；由后端在推进时自动写入，可人工修正。

**反馈记录 (ProgressNote)**：
反馈缸下的跟单沟通流水，作为**独立子资源**处理（有独立的增删端点与 `:note` 权限，模式同检验录检明细 ADR-0014）。字段：`content` 文字内容、`issueFlag` 是否问题、`resolvedAt` 闭环时间、记录人与记录时间。流水只增不改；标记为问题的记录闭环后不可删除；删缸时级联逻辑删除其全部记录。MVP **不含照片/视频附件**——lunaerp 业务侧尚无文件上传使用先例，现场信息先以文字承载，附件随框架上传能力后续再补。
_避免_：进度日志、评论。

**延期 (ProgressDelay)**：
缸的交期取染色单交货日期（`deliveryDate`）；当前日期晚于交期且缸状态非 COMPLETED 即判定延期，列表红色标记并计入延期统计；延期原因在缸上以自由文本登记。不做"单工序滞留 N 小时"类预警（依赖自定义工序时间戳，固定六态下价值有限）。

**排产覆盖率 (VatCoverage)**：
同一染色颜色行下全部反馈缸数量合计 ÷ 该颜色行染色数量，只读动态计算（同"合同未发量"先例，不落库）；已建缸合计超过颜色行染色数量时**警告但允许**（临时加缸是常态，同织造单软校验先例）。不做投入/产出/损耗统计（本期没有成品回产量数据）。

**缸卡墙 (VatWall)**：
进度反馈的主交互界面：以颜色行为骨架分节（节头展示合同/客户/染厂/产品/颜色、行需求量、已建缸合计与排产覆盖率、状态汇总），节内每缸一张卡片网格横排；零缸颜色行以虚线"＋建缸"占位卡呈现虚拟未排缸行。技术实现为 Vue3 + Element Plus 桌面宽屏版（el-card/el-dialog/el-drawer/el-tag/el-progress + scoped SCSS），只复刻参考系统缸卡墙的信息架构与交互流，不引入其移动端视觉、矩阵视图、实时推送、AI 洞察与语音录入。

**进度反馈双入口**：
"生产管理 → 进度反馈"独立菜单承载覆盖层缸卡墙列表与建缸；染色单整页编辑页另设"进度反馈"页签，按单查看该单缸卡并可直接建缸。两入口复用同一建缸弹窗与同一套接口；染色单侧只读、零回写。

## 生产管理：生产进度

**生产进度 (ProductionProgress)**：
以销售合同为锚点的全链路生产进度**只读聚合视图**——不是表、不是单据，而是从下游执行数据实时推导的投影（旧系统 OrderProgress 手动填进度字符串的根除）。聚合环节：胚布（计划投胚/胚布入库/已发染量/自织进度）、染色（投染量/排产覆盖率/缸状态汇总/延期缸数）、检验（录检明细数/等级汇总）、入库（胚布+成品入库量）、发货（已发/未发量），外加预警。染色环节数据来自进度反馈覆盖层（外发黑盒、手动代录），其余环节从执行单据与库存流水自动聚合；合同侧不加任何进度字段（零回写惯例）。
_避免_：订单进度、OrderProgress（旧系统名称，保留以追溯）、合同进度（命名决策采用"生产进度"）、进度跟踪单（进度不是"另一张单"）。

**生产进度预警 (ProgressAlert)**：
生产进度视图上的只读风险标记，MVP 三条规则：① 临期——交期距今 ≤7 天且仍有未发量；② 超期——交期已过且仍有未发量；③ 延期缸——该合同染色单下存在延期反馈缸（延期的判定见进度反馈"延期"）。规则后续按需扩展（排产超量等），不在视图上手工维护。

## 生产管理：排产与齐套检查

**织造排产 (Scheduling)**：
把织造单的机台任务在"织机 × 计划时段"上排布并防止机台双占的车间计划活动。一期排产**不是单据、不是表**：机台任务（见织造执行）是唯一机台占用事实源，排产中心只是它之上的实时只读视图（甘特、待排池、跨单列表）+ Service 层冲突硬校验（ADR-0041）。不建排产计划/版本实体，不做 AI 优化排程（charting Q10=A）。
_避免_：排程单、派工单（派工是动作，载体仍是机台任务）、生产计划（二期产能/工作日历语义）。

**待排池 (Pending Pool)**：
排产中心中聚合"还没排上机台/时间"工作的队列，两组：① 无计划日期的 PENDING 机台任务；② 未排满的 DRAFT/ACTIVE 织造单（占用态任务计划匹数合计 < 织造单计划匹数），按剩余匹数展示。全部实时聚合、不落库。

**排产覆盖状态 (Schedule Coverage)**：
织造单维度的实时派生标记：未排（无任务）/ 部分排（任务匹数 < 计划）/ 已排满 / 生产中（有 IN_PROGRESS 任务）/ 已完工。前端计算、不入字典、不落库（同生产进度预警口径）；织造单状态机不因此改动。

**齐套检查 (Material Check)**：
开工前按"配比 BOM × 织造单计划量 − 可用库存"核算纱线是否备齐的**时点只读试算**，不落检查记录表、不预留/冻结库存、不自动生成采购单（ADR-0041）。需求口径复用领纱测算（织造单计划重量 × ratio/100，按纱线规格聚合）；可用量取库存台账实时余额。输出需求量/可用库存/在途量/两个口径缺口/建议采购量；无配比或无计划重量的织造单列入无法试算区。
_避免_：物料核算（口语）、BOM 核算、齐套单（齐套不是单据）。

**在途纱线 (Yarn In Transit)**：
齐套检查的近似口径：处于 `ACTIVE`（进行中）状态的纱线采购单明细实采量（purchase_qty）按纱线规格的合计。DRAFT 未正式下达不计、COMPLETED 视为已完结、CANCELLED 排除。因入库单不关联采购单，部分到货无法逐单核销，该口径依赖人工及时完结采购单（已知限制，精确未到货量留待采购链二期）。齐套页面默认"不含在途"，缺口两列（含/不含在途）常驻展示。

**排缸日历 (Vat Calendar)**：
进度反馈页内反馈缸（见进度反馈）的月历视图——以日期格组织缸点，回答"某天哪些缸在染、排缸是否拥挤"。与缸卡墙是同一反馈缸数据的两种视图（不建表、不单独开菜单）；点击缸点可改排缸日（非 COMPLETED 缸，复用反馈缸更新），刻意不做跨格拖拽。
_避免_：染缸排程表、染缸台账（外发模式下染缸是染厂资源，本厂不建设备主数据，ADR-0041）。

**缸次 (Vat Run)**：
外发染色中"一缸布"的生产批次口径——某张染色单某颜色行一次投染即为一个缸次，反馈缸记录是缸次的载体（同单同色缸号唯一）。缸次不是染缸设备：一个染缸设备可连续承载多个缸次。一期"建缸"即登记一个缸次。
_避免_：染缸（设备语义）、缸（口语可用）。

## 生产管理：车间移动 H5

**车间移动 H5 (WorkshopH5)**：
车间现场员工用手机/PDA 在浏览器或企微/钉钉 WebView 内使用的移动触点，承载三类现场动作：**扫码报工**（挡车工扫布票二维码开工/完工/异常登记）、**移动质检**（检验员胚布扫布票码、成品按检验单逐卷录检）、**移动发纱**（机台领纱单逐行确认发纱）。身份复用 system 员工账号（账号密码登录，非外部免登）；数据范围按登录人硬过滤——挡车工只能操作派给自己的机台任务与布票，未派工任务允许扫票**自助认领**；检验员不受机台归属限制。H5 是既有单据的接入通道，不产生新单据/新状态机：报工写布票、录检写检验单明细、发纱写领纱明细与库存流水，业务规则全部复用既有 Service。异常照片挂布票、疵点照片挂录检明细（各 ≤6 张 infra 文件 URL）。工程形态见 ADR-0045（luna-ui 内独立 `/h5/**` 路由树+Vant，与 14 号客户门户同栖单工程但凭证机制不同）。
_避免_：车间 App、小程序（Q6=A 否决原生端）、扫码报工系统（它是通道不是独立系统）。

**自助认领 (Self-claim)**：
挡车工扫到一张派工对象为空（机台任务 `workerUserId` 为空）的布票并点开工时，系统把该任务的挡车工条件更新为登录人（`WHERE worker_user_id IS NULL`，并发只一人成功），随即完成开工的派工动作。任务已派给他人时他人扫码被拦截，改派必须由领班在 PC 操作。自助认领只解决"派工没录全"的现场现实，不改变机台任务作为派工载体的模型。

**胚布扫码自动交接 (Scan-triggered handoff)**：
检验员在 H5 扫完工布票录检时，若该票所属织造单尚未在 PC 做"完工交接检验"，H5 后端以服务内调用幂等完成 find-or-create 胚布检验单表头（规则同 ADR-0025 交接：按胚布单归一、要求有完工票），再按 `weavingTicketId` 定位或新建录检行（PIECE 匹号缺省取布票完整 code，胚布缸号可空）。移动端点不要求检验员持有 PC 的交接权限码；交接仍是织造单侧的幂等动作，H5 只是触发者。

## 模块边界

打色指示单落 `production` 子包（与胚布单/染色单同层）：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.production.colorinstruction[.color]`；前端视图 `luna-ui/src/views/lunaerp-elmui/production/color-instruction/`（列表+编辑页+就近打印模板）、前端 API 目录镜像。URL 前缀 `/lunaerp/production/color-instruction`，权限码前缀 `lunaerp:production:color-instruction:`，错误码段 `1-030-032-xxx`（主表与颜色行共用）。菜单挂 10200 生产管理：**10230 打色指示单 + 10231-10235 五个按钮位**（create/update/delete/confirm/print）。胚布单、染色单主表各加 `color_instruction_id`+`color_instruction_no` 可空显式列引用打色单。建表 SQL：`luna-srv/sql/postgresql/lunaerp-production-color-instruction.sql`。spec 见 `.scratch/color-instruction/spec.md`。

胚布单落在 `production`（生产管理）子包，与 `baseinfo` / `business` 平级：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.production.greigeorder[.color]`，前端 `lunaerp-elmui/production/greige-order/`（视图与 API 目录镜像）。

纱线采购域同样落在 `production` 子包下，包含 5 个并列子包：`production.yarntype`（材质 CRUD，无字典）、`production.yarnspec`（规格 CRUD + cascade-options 两级级联数据源）、`production.yarnprice`（纱价主数据 CRUD + getEffectivePrice 取价服务，ADR-0039）、`production.yarnrecipebom`（配比 BOM 主从表 CRUD + get-by-product-id；明细含五测算列）、`production.yarnorder`（采购单主从表 CRUD + 状态操作 + calculate 测算接口；原 stockledger 台账已于 2026-09-21 撤销回退）。前端视图 `lunaerp-elmui/production/yarn-master/`（材质+规格合并管理页，规格行挂「价格记录」抽屉）、`yarn-recipe-bom/`（配比 BOM 管理页）、`yarn-order/`（采购单列表+编辑页）。URL 前缀 `/lunaerp/production/{yarn-type|yarn-spec|yarn-price|yarn-recipe-bom|yarn-order}`，权限码前缀 `lunaerp:production:<子域>:`（纱价四按钮 10245-10248 挂纱线档案菜单下，不开独立菜单）。详见 ADR-0011、ADR-0039。

检验单落 `production` 子包（与胚布单/染色单/纱线同层）：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.production.inspectionorder[.item]`；前端视图 `luna-ui/src/views/lunaerp-elmui/production/inspection-order/`、前端 API `luna-ui/src/api/lunaerp-elmui/production/inspection-order/`（含 inspection-order-item）。URL 前缀 `/lunaerp/production/inspection-order[-item]`，权限码前缀 `lunaerp:production:inspection-order[-item]:`，错误码段 `1-030-010-xxx`（主表与明细共用）。**录检明细作为独立子资源**（刻意不一致 ADR-0003，理由见 ADR-0014）。菜单挂 10200 生产管理目录下：10260 检验单菜单 + 10261-10263 主表按钮 + 10264-10267 明细按钮（仿供应商 10050-53/10060-63 同构）。

质检确认同样落 `production` 子包：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.production.inspectionconfirmation`（**单包单实体，无独立明细子资源**——认定留痕链行同表，非主从结构）；前端视图 `luna-ui/src/views/lunaerp-elmui/production/inspection-confirmation/`（单列表页 + 页内弹窗：认定对话框、手工登记、按日打印、工资扣款汇总、认定链详情）、前端 API 目录镜像。URL 前缀 `/lunaerp/production/inspection-confirmation`，权限码前缀 `lunaerp:production:inspection-confirmation:`，动作码六个：`query`（菜单承载）/`create`（手工登记）/`generate`（检验单工作台生成）/`confirm`（认定与撤销）/`print`/`payroll`；错误码段 `1-030-014-xxx`。菜单挂 10200 生产管理：10270 质检确认（承载 query）+ 10271-10275 五个按钮位（create/generate/confirm/print/payroll）。"生成质检确认"按钮物理上在检验单工作台，但鉴权用本域 `:generate` 码（创建的是质检确认数据，不挂检验单更新权限，ADR-0019）。建表 SQL：`luna-srv/sql/postgresql/lunaerp-production-inspection-confirmation.sql`。

织造执行域落 `production` 子包（与胚布单/检验单/质检确认同层），含 5 组并列子包：`production.loom`（织机档案 CRUD）、`production.weavingorder`（织造单主表）、`production.weavingorder.task`（机台任务）、`production.weavingorder.ticket`（布票）、`production.weavingorder.requisition`（领纱单+明细，独立子资源）；IoT 落 `production.iot`（厂商凭证/设备绑定/实时快照三 DO，采集层接口+手工录入端点）；发染记录落 `production.greigedispatch`。前端视图 `lunaerp-elmui/production/weaving-order/`（列表 + 工作台 5 Tab：基本信息/机台任务/布票/领纱/统计）、`lunaerp-elmui/production/loom/`（织机档案页，IoT 接入与快照作为页内 Tab，不单独开菜单）；前端 API 镜像于 `api/lunaerp-elmui/production/`。胚布单详情（自织单）加"织造进度"区+生成织造单入口；检验单编辑页加卷/匹模式切换。URL 前缀 `/lunaerp/production/{loom|weaving-order[-task|-ticket|-requisition]|iot}`，权限码前缀 `lunaerp:production:<子域>:`，错误码段 **`1-030-015`**（全织造域共用，按子资源分段；避让已占用的质检确认 014 段）。菜单挂 10200 生产管理目录下：**10280 织造单菜单 + 10281-10284 主表按钮；10285 织机档案 + 10286-10288 按钮**（领纱并入织造单工作台 Tab，不开独立菜单）。开发分三期：P1 胚布单自织改造+织造核心+检验单匹装改造；P2 纱线库存+领纱发纱；P3 转检验/入库联动+IoT（见 `.scratch/weaving-execution/`）。

进度反馈落 `production` 子包（与染色单/检验单/织造域同层）：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.production.progressfeedback[.note]`（反馈缸主表 + 反馈记录独立子资源）；生产进度聚合视图落 `production.productionprogress`（只读查询，无独立表）。前端视图 `luna-ui/src/views/lunaerp-elmui/production/progress-feedback/`（缸卡墙列表 + 建缸/推进/反馈弹窗 + 缸详情抽屉）、前端 API 镜像；染色单工作台加"进度反馈"页签（双入口，零回写）。URL 前缀 `/lunaerp/production/progress-feedback[-note]`，权限码前缀 `lunaerp:production:progress-feedback:`，六个动作：`query`（菜单承载）/ `create`（建缸）/ `update`（缸信息与阶段日期）/ `advance`（推进状态）/ `note`（反馈记录增删与问题闭环）/ `delete`（删缸，级联软删记录）；错误码段 `1-030-017-xxx`。菜单挂 10200 生产管理：**10400 进度反馈菜单 + 10401-10405 五个按钮位**（create/update/advance/delete/note；跳号原因：10290-10298 已被织造领纱按钮占用、10300 起为库存管理目录）。建表 SQL：`luna-srv/sql/postgresql/lunaerp-production-progress-feedback.sql`（两表 `lunaerp_production_progress_feedback` / `lunaerp_production_progress_feedback_note`）。依赖 ADR-0032（聚合子表差异保存）先行落地，分两阶段交付：阶段一差异保存改造，阶段二进度反馈。

排产与齐套落 `production` 子包，零新表（ADR-0041）：后端新增 `production.scheduling`（甘特/待排池/跨单分页只读查询，复用机台任务/织机/织造单 Mapper；冲突校验写在既有 `production.weavingorder.task` 的 MachineTaskService 内）与 `production.materialcheck`（齐套只读试算，复用配比 BOM、库存台账、纱线采购单聚合）；排缸日历为 `production.progressfeedback` 内新增只读端点。前端视图 `luna-ui/src/views/lunaerp-elmui/production/scheduling/`（排产中心：甘特+待排池+跨单任务列表，甘特用 MIT 库 vue-ganttastic）、`production/material-check/`（齐套检查页）；排缸日历组件内嵌 `production/progress-feedback/`。菜单挂 10200 生产管理：**10510 排产中心**（query 复用 `lunaerp:production:weaving-order-task:query`，不另设按钮位，派工/改期复用机台任务 create/update）、**10520 齐套检查**（权限码 `lunaerp:production:material-check:query`，纯只读单菜单）；排缸日历不占菜单不加权限。URL 前缀 `/lunaerp/production/{weaving-order-task/gantt|weaving-order-task/pending-pool|material-check|progress-feedback/calendar}`。SQL 仅菜单授权：`luna-srv/sql/postgresql/lunaerp-production-scheduling-material-check.sql`。spec 见 `.scratch/scheduling-material-check/spec.md`。

车间移动 H5 落**横切接入层**而非生产域新子包：后端 `com.luna.lunasrv.module.lunaerp.{controller.admin.h5.workshop, service.h5.workshop}`（H5 编排 Service + H5 专属 VO，写库全部调既有生产域 Service，零业务规则复制）；前端视图 `src/views/h5/`（Vant 4 + H5Layout，非 elmui 临时区）、API `src/api/h5/workshop/`，路由 `/h5/**` 独立移动路由树。URL 前缀 `/lunaerp/h5/workshop`（网关后 `/admin-api/lunaerp/h5/workshop/**`，标准员工 admin 鉴权），权限码三个：`lunaerp:workshop:h5:report`（移动报工）/ `lunaerp:workshop:h5:inspection`（移动质检）/ `lunaerp:workshop:h5:dispatch`（移动发纱）；错误码段 `1-030-039-xxx`；零新表（布票加 `exception_photos`、录检明细加 `photos` 两 jsonb 列），零新字典；SQL（隐藏菜单 10530 容器+10531–10533 按钮、seed `workshop_worker`/`workshop_inspector` 两角色、2 处 ALTER）：`luna-srv/sql/postgresql/lunaerp-workshop-h5.sql`。工程形态见 ADR-0045；spec 见 `.scratch/workshop-h5/spec.md`。

## 字典

**胚布单字典 (Greige order dictionaries)**：
胚布单 MVP 只有一个枚举字段走字典：`lunaerp_greige_order_status`（ACTIVE/COMPLETED/CANCELLED）。数量单位统一 KG，不建单位字典。

**纱线采购域字典 (Yarn domain dictionaries)**：
本域两个字典：`lunaerp_yarn_order_status`（DRAFT/ACTIVE/COMPLETED/CANCELLED，仿染色单）、`lunaerp_yarn_price_type`（id=2266，PURCHASE 采购参考价 / MARKET 市场参考价，ADR-0039）。**纱线材质不入字典**（主数据由纱线档案页面维护，不内置 seed，见 ADR-0011）。纱线规格的 spin_method/luster 等均为自由文本不建字典。库存台账 type 枚举不入字典（仅后端服务内部使用）。

**检验单字典 (Inspection order dictionaries)**：
检验单域建两个字典：`lunaerp_inspection_order_status`（id=2231，DRAFT/ACTIVE/COMPLETED/CANCELLED，仿染色单/纱线采购单四态）、`lunaerp_inspection_grade`（id=2232，A 一等品 / B 二等品 / C 三等品 / DEFECTIVE 等外品，4 项）。type 字段（GREIGE/FINISHED）不入字典，仅后端枚举使用。本期无同步入库，不建入库卷状态（rollStatus）字典。

**质检确认字典 (Inspection confirmation dictionaries)**：
质检确认域建两个字典：`lunaerp_inspection_confirmation_status`（id=2236，PENDING 待确认 / CONFIRMED 已认定 / REVOKED 已撤销）、`lunaerp_inspection_responsibility`（id=2237，EQUIPMENT 设备故障（免扣）/ OPERATOR 操作失误（扣款）/ WAIVED 免扣）。来源 INSPECTION/MANUAL 为后端内部枚举不入字典（同检验单 type、库存流水 txn_type 先例）；等级快照值直接复用 `lunaerp_inspection_grade` 不另建。"挡车工"是 `system_post` 表 seed 的一条岗位数据（system 域），不是 lunaerp 字典。

**织造执行字典 (Weaving execution dictionaries)**：
织造执行域建七个字典（id 自 2238 起，避让质检确认 2236/2237）：`lunaerp_weaving_order_status`（id=2238，DRAFT 待排产/ACTIVE 生产中/COMPLETED 已完工/CANCELLED 已取消）、`lunaerp_weaving_task_status`（id=2239，PENDING 待开始/IN_PROGRESS 生产中/HALTED 暂停异常/COMPLETED 已完工）、`lunaerp_weaving_ticket_status`（id=2240，GENERATED 已生成/STARTED 已开工/COMPLETED 已完工/INSPECTED 已检验/STOCKED 已入库/EXCEPTION 异常/VOID 作废）、`lunaerp_greige_production_mode`（id=2241，SELF 自织/OUTSOURCE 外发）、`lunaerp_inspection_package_mode`（id=2242，ROLL 卷装/PIECE 匹装）、`lunaerp_yarn_requisition_status`（id=2243，DRAFT 草稿/ACTIVE 待发纱/PARTIAL 部分发纱/DISPATCHED 已发纱/CLOSED 已关闭）、`lunaerp_weaving_exception_type`（id=2244，YARN_BREAK 断纱/CLOTH_DAMAGE 坏布/YARN_SHORTAGE 缺纱停机/MACHINE_CHANGE 换机/WORKER_CHANGE 换人）。织机 ACTIVE/INACTIVE 不建字典（对齐仓库/客户/供应商）；IoT 凭证/绑定状态与设备快照状态为后端内部枚举不入字典（同 txn_type 先例）。

**进度反馈字典 (Progress feedback dictionaries)**：
进度反馈域建一个字典：`lunaerp_progress_feedback_status`（id=2247，NOT_SCHEDULED 未排缸 / SCHEDULED 已排缸 / DYEING 染色中 / FIXING 固色中 / QUALITY_CHECK 质检中 / COMPLETED 已完成，固定六工序链）。问题标记为布尔字段不建字典；缸数量单位跟随颜色行，复用 `lunaerp_sales_contract_unit`（KG/M）。

**打色指示单字典 (Color instruction dictionaries)**：
建三个字典（dict_type id 2255-2257，避让库存 2248、产品 2249/2250、样品 2251-2254；dict_data id 自 3736 起）：`lunaerp_color_light_source`（2255，灯源：D65/TL84/CWF/A/UV/TL83）、`lunaerp_color_line_status`（2256，颜色行状态：PENDING/APPROVED/REJECTED）、`lunaerp_color_instruction_status`（2257，单据五态：PENDING/IN_PROGRESS/CONFIRMED/IN_PRODUCTION/VOID）。确认类型 confirmType（CUSTOMER/INTERNAL）为后端内部枚举不入字典（同各单 type 先例）。

**排产与齐套字典 (Scheduling & material check dictionaries)**：
**不新增字典**。排产覆盖状态（未排/部分排/已排满/生产中/已完工）与齐套状态（齐套/在途可覆盖/缺料）均为前端实时派生标签，不持久化、不入字典（同生产进度预警 ADR-0034 口径）；机台任务、反馈缸状态复用既有 2239/2247 字典。

## 错误码

错误码段位总表与新模块取段规则见 `docs/contexts/shared/GLOSSARY.md`。本域占用段位：

- `1-030-006-xxx` 胚布单（主表与颜色分配共用：000 胚布单不存在 / 001 胚布单号重复 / 002 关联引用不存在）
- `1-030-007-xxx` 染色单（主表与颜色明细共用：000 染色单不存在 / 001 状态不允许该操作 / 002 关联引用不存在）
- `1-030-008-xxx` 纱线档案（材质+规格+配比BOM+库存台账+纱价共用：000 材质不存在 / 001 材质名重复 / 002 规格不存在 / 003 规格名重复 / 004 配比BOM不存在 / 005 配比BOM明细不存在 / 006 库存台账不存在 / 007 该产品已存在纱线配比BOM / 008 纱价记录不存在 / 009 同规格同价类生效日期重复，ADR-0039）
- `1-030-009-xxx` 纱线采购单（主表+明细共用：000 采购单不存在 / 001 采购单号重复 / 002 状态不允许该操作 / 003 关联引用不存在）
- `1-030-010-xxx` 检验单（主表与明细共用，本期新增）：000 检验单不存在 / 001 检验单号重复 / 002 状态不允许该操作 / 003 关联引用不存在 / 004 单内卷号缸号重复 / 100 录检明细不存在（主表段 000-004，明细段从 100 起避开主表）
- `1-030-014-xxx` 质检确认（ADR-0019/0020/0021）：000 质检确认记录不存在 / 001 状态不允许该操作（如重复认定、撤销未认定记录）/ 002 检验单不存在 / 003 检验单状态不允许生成（仅 ACTIVE/COMPLETED）/ 004 责任类型非法 / 005 操作失误必须指定挡车工 / 006 手工登记必填项缺失（卷号/等级/责任类型等）。幂等生成跳过已存在卷不报错（接口返回新增/跳过计数），故不设"重复生成"错误码。
- `1-030-015-xxx` 织造执行域（主表/机台任务/布票/织机/领纱/IoT 共用，按子资源分段）：
  - 织造单（000-009）：000 织造单不存在 / 001 织造单号重复 / 002 状态不允许该操作 / 003 关联胚布单不存在或非自织态 / 004 计划匹数非法。
  - 机台任务（100-109）：100 机台任务不存在 / 101 任务状态不允许该操作（如已完工标停机）/ 102 关联织机不存在。
  - 布票（110-129）：110 布票不存在 / 111 布票当前状态不允许该扫码动作 / 112 布票已作废 / 113 异常类型非法。
  - 织机（200-209）：200 织机不存在 / 201 织机号重复 / 202 织机已被机台任务引用不能删除。
  - 领纱（300-319）：300 领纱单不存在 / 301 领纱单号重复 / 310 领纱明细不存在 / 311 纱线库存不足（返回需求量/可用量）/ 312 领纱单状态不允许发纱。
  - IoT（400-419）：400 厂商凭证不存在 / 401 厂商账号重复 / 410 设备绑定不存在 / 411 设备已绑定 / 420 快照不存在。

  胚布单自织改造复用 `1-030-006` 段，检验单匹装改造复用 `1-030-010` 段，库存扩展复用 `1-030-011` 段，不新增错误码。

- `1-030-017-xxx` 进度反馈（反馈缸/反馈记录共用）：000 反馈缸不存在 / 001 同单同色缸号重复 / 002 染色单不存在或状态不允许建缸（仅 ACTIVE/COMPLETED 单的颜色行可建缸）/ 003 染色颜色行不存在 / 004 颜色行下存在反馈缸不能移除（须先删除缸反馈；染色单差异保存删除守卫跨模块使用本段）/ 100 反馈记录不存在 / 101 问题已闭环不可操作。

- `1-030-032-xxx` 打色指示单（主表与颜色行共用）：000 打色单不存在 / 001 打色单号重复 / 002 单据状态不允许该操作 / 003 关联引用不存在（客户/产品/供应商）/ 004 存在未决颜色行或无通过行，不能确认 / 005 已被下游有效单据引用，不能作废或删除 / 006 颜色行状态不允许修改或删除 / 007 灯源或确认类型非法 / 100 颜色行不存在。
- `1-030-035-xxx` 下达生产（release-production 编排，ADR-0040）：000 销售合同不存在 / 001 合同已取消，不能下达生产 / 002 合同行已下达量超出合同数量（超下达或并发冲突）/ 003 打色单状态不允许被引用（须 CONFIRMED/IN_PRODUCTION）/ 004 打色单颜色行主灯源不能为空 / 005 存在相同染色单的成品检验单，请勿重复创建。
- `1-030-036-xxx` 排产与齐套（零新表模块，冲突校验从机台任务 Service 抛出，跨模块使用本段；齐套/甘特/日历为只读查询不设业务码）：000 织机计划时段冲突（返回冲突任务号与日期，ADR-0041）。001+ 预留。
- `1-030-039-xxx` 车间移动 H5 接入层（ADR-0045；业务守卫复用 015/010 段，本段仅新增 H5 场景码）：001 布票不存在或已作废（含跨租户落空，统一文案防枚举）/ 002 机台任务已派他人（含自助认领并发落败）/ 003 布票未完工不能扫码质检 / 004 布票当前状态不可录检 / 005 缸号+卷号录检记录已存在（返回已存在行供改录，非致命）/ 006 领纱单不属于本人机台任务 / 007 默认发纱仓库未配置 / 008 照片超 6 张或非图片。009+ 预留。
