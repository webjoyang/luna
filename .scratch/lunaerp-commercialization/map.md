# Map: lunaerp 商业化完整版决策地图

Type: wayfinder:map
Status: active

## Destination

新栈（luna-worktree-return）从当前约 20 个 MVP 模块扩展为可对外售卖的商业化纺织 SAAS（一期私有化买断交付）。地图走完的标志：W0–W4 全部模块的关键决策 resolved、每个缺口模块落成 `.scratch/<feature>/spec.md`（沿用现有 spec 模式）、商业化验收清单成型；此后编码按 spec 分 session 推进，不再需要地图上的决策。

## Notes

- 域：纺织 ERP（坯布/染色/织造/贸易，工厂+贸易一体客户为主）。新栈：luna-ui（Vue3+TS+ElementPlus）+ luna-srv（SpringBoot+PostgreSQL+MyBatis Plus+Redis，芋道底座）。老系统参照：`D:\work\luna\go0829\linaagi`（106 模块注册表 `web/app/staff/config/module-registry.ts`，316 数据模型；商业化方案见其 `docs/商业化方案/`）。
- 技能：每张 grilling ticket 先调 Skill "grilling" 与 "domain-modeling"；research ticket 调 Skill "research"；涉及"长什么样"的 ticket 调 Skill "prototype"。
- 术语进 `docs/contexts/<域>/GLOSSARY.md`；ADR 全局连续编号 `docs/adr/`；错误码入段位总表。
- 本图 tickets 在 `issues/` 下按编号推进；`Blocked by:` 行表达依赖，全部 resolved 即畅通；认领 = `Status: claimed`。
- 每模块 spec 必须满足统一 DoD（charting 决策 Q16）：状态机白名单+终态守卫+并发条件更新；芋道权限+脱敏；列表/详情/删除进回收站；打印+操作/审批日志；核心状态流转与库存/账务副作用测试（金额逻辑必有测试）；前端验收清单+波次冒烟；术语与错误码归位。性能/安全硬指标：常规列表 P95<1s（万级）、写接口租户隔离测试、横向/纵向越权必测。
- 决策快照（charting 会话 grilling 达成，Q1–Q18 全部按推荐锁定）：
  - 目的地 = 决策地图 + 每模块可执行 spec（Q1=A）；一期私有化买断，租户隔离铺路，不做运营计费端（Q3=A）
  - 核心包 = 成本报价→合同→生产指令拆分（含打色）→排产齐套→织染检入库（已有）→发货（已有）→退货→客户对账开票收款+供应商对账付款+经营财务；出口单证（PI/CI/装箱单）入核心包；CRM/样品扩展/HR/营销/生成式 AI = 二期增值包（Q2）
  - 财务 = 经营账，预留金蝶/用友对接接口（Q4=C）；现有 `views/fms`（借贷会计脚手架）不挂菜单、不迁后端、保留代码（Q11=A）
  - AI 一期不做生成式，仅规则型智能（Q5=A）
  - 车间移动端 = H5（Q6=A）；合同→生产唯一入口 = 合同详情页「下达生产」工作台，底层沿用四类独立单据，不建聚合实体（Q7=A）
  - 审批 = 轻量自研固定审批链（逐级/或签/会签/驳回），记录表兼容未来 BPM（Q8=A）；权限 = 芋道 RBAC + 租户模块开通网关 + 固定字段脱敏，不做字段配置中心（Q9=A）
  - 排产 = 轻量排产 + 齐套检查，不做 AI 优化排程（Q10=A）
  - 打印 = 前端固定套打模板 + 二维码（布票/发货单/对账单/出口单证），不做模板设计器与报关对接（Q12=A）
  - 门户 = 一期客户门户最小集（发货查询+签收确认+对账单查看确认），供应商门户二期（Q13=A）
  - Q14 取舍：员工复用芋道 system 用户/部门/岗位；工资初稿 = 质检次品扣款+手工计件（不含考勤算薪）；退货一期仅销售退货（纱线采购退货二期）；报销申请+供应商付款申请走审批；统一回收站（软删+恢复链式补回）；主数据标准 Excel 导入；站内消息+审批待办角标（芋道通知）；打色单一期（寄样/选样/调样/剪样二期）；车间移动 H5 一期（布票扫码报工/卷级扫码质检/领纱确认/进度拍照）
  - 波次：W0 商业地基 → W1 售前链 → W2 合同→生产 → W3 资金闭环 → W4 触点与验收（Q15）
  - 老系统数据不迁移，新栈服务新客户；迁移二期单独立项（Q17=A）
  - 菜单一级分组（首页/基础信息/销售/生产/库存/样品/财务/审批中心/系统管理，03 号决议取消"供应链"分组，纱线采购与发染归生产、供应商信息留基础信息、入库管理留库存）（Q18）

## Decisions so far

- [07-export-docs: 出口单证研究](issues/07-export-docs.md)：老系统以"单证套"（共享抬头+明细，PI/CI/PL 叠加 meta JSON）为核心；新栈沿用此模式，spec 草案已落 `.scratch/export-docs/spec.md`；缺口不致命，唯一前置是租户卖方出口配置（infra_config 键，零新表）。
- [17-legacy-finance-research: 老系统财务深挖](issues/17-legacy-finance-research.md)：老系统三本账冗余是 P0 根因，新栈只留一种 append-only 分录账永不重建；客户对账取数=发货单明细白名单、供应商侧=收货四层匹配；ReturnOrder/发票分摊/结汇加权汇率/FIFO 账龄可直接照抄，预收预付与工资扣款明细需重设计。
- [18-yudao-capability-research: 芋道能力盘点](issues/18-yudao-capability-research.md)：TenantPackage 可直接充当模块开通网关（菜单级现成、接口级小幅扩展）；BPM 模块未迁入（自研审批表可参考 ACT_RU/HI 两态模型）；注解级脱敏 starter 现成；字典表不租户隔离需注意 DO 继承。
- [01-finance-domain-adr: finance 域落地决策与 ADR](issues/01-finance-domain-adr.md)：ADR-0037 落定——经营账独立成第六域、不做借贷会计、views/fms 三不（不迁/不挂/保留）、金蝶用友只预留分录账出口；包骨架 `**.finance.*`（9 子模块+external 占名）、错误码预留 1-030-020~029（盘点发现 018 发染/样品重号待清理）、菜单 10700 财务+10710~10770 七子菜单；finance GLOSSARY 骨架与全域路由已更新，10/11/12 号解除阻塞。
- [02-approval-engine: 轻量审批引擎设计](issues/02-approval-engine.md)：lunaerp 横切 `approval` 子包 5 表（config/config_node/instance/task/record），TenantBaseDO 租户隔离；逐级/或签/会签（一票否决），审批人发起时全流程快照；`approval_status` 五态跨切面字段，APPROVED 发 `ApprovalCompletedEvent`（BEFORE_COMMIT 同事务）由各单据 ApprovalHandler 自实现过账，引擎不碰业务账目（对齐 ADR-0017）；预留 flowable_* 列兼容 BPM 回迁；新建一级审批中心菜单（与芋道 1185/1200 解耦）；错误码 1-030-030；spec 落 `.scratch/approval-engine/spec.md`，术语进 shared GLOSSARY；解阻塞 13 号销售退货。
- [03-module-gateway: 模块套餐开通网关与菜单 IA](issues/03-module-gateway.md)：复用芋道 TenantPackage 不另建模块表，模块=一级菜单目录、套餐 menuIds 勾目录即开通整树；不加后端拦截器，前端菜单裁剪+后端 @PreAuthorize 403 双层（前端 403 提示"模块未开通"）；停用=隐藏（移除菜单树、数据保留不归档）；菜单 IA 迁移为 8 分组（基础信息/销售/生产/库存/样品/财务/审批中心+系统管理+首页），不设供应链，纱线采购/发染归生产；spec 落 `.scratch/module-gateway/spec.md`。
- [04-common-foundation: W0 商业地基通用件](issues/04-common-foundation.md)：五件事合并一份 spec（`.scratch/common-foundation/spec.md`）+ 横切子包 `lunaerp.common`，零新表；回收站=handler SPI 只收主数据+DRAFT、恢复不补账（ADR-0038）；打印=内核+就近模板+注册表，W0 交付布票标签/发货单迁移并冻结二维码码格式；Excel 导入=仅新增/全量校验/错误 Excel/5000 行；脱敏=自研 `@Sensitive` 两权限位（amount/contact）、打印独立 VO 豁免；消息=全复用芋道 notify+Helper，W0 实装审批三类模板；错误码 1-030-031；挂账 07/10/14/15 号接契约。
- [20-export-docs-spec-grilling: 出口单证 spec 定稿](issues/20-export-docs-spec-grilling.md)：`.scratch/export-docs/spec.md` 转 ready-for-agent。合同 +4 外贸条款列（仅 EXPORT 显示）、客户档案 +2 英文列（同步 04 号 Excel 模板）、买方银行留单证套手填；卖方配置弃用 @TenantIgnore 的 infra_config，改租户单行表（列表页弹窗维护）；三模板接 04 打印内核 + 完整门户 URL 二维码（一期链不通）；补 resync 只填空端点、定稿明细非空、草稿可打印、RecycleBinHandler(EXPORT_DOC)；避让并行 06 号会话，错误码取 1-030-033、字典 2258–2261、菜单 10126–10131。
- [06-color-instruction: 打色指示单](issues/06-color-instruction.md)：spec 落 `.scratch/color-instruction/spec.md`；两表（主表+颜色行）差异保存+行状态守卫；客户色号/颜色名与染色发货明细同口径，灯源走字典；单据五态、确认守卫、CONFIRMED 可回退、投产由下游引用自动触发；胚布单/染色单各加 color_instruction_id+no 显式列；菜单 10230、A4 套打本期实现、回收站仅收 PENDING；错误码 1-030-032、字典 2255-2257；术语进 production GLOSSARY。
- [05-cost-card-quotation: 成本卡与智能报价](issues/05-cost-card-quotation.md)：spec 落 `.scratch/cost-card-quotation/spec.md` + ADR-0039。新增 6 表：纱价主档（生产域，规格+价类+生效日期历史，取价回退 ref_price）、成本卡三表（business 域，DRAFT/EFFECTIVE/EXPIRED，同品一生效部分唯一索引，冻结+复制新建，双口径 KG/M 不含税/含税，结构化纱线行+六项费用行）、报价单两表（同号多版本 QT…-vN，四态+EXPIRED 派生不入库，真毛利率反算，外币快照汇率，成本快照 JSON，W0 amount 位脱敏，打印白诺无底牌）；转合同=预填草稿+来源两列、多次转不占量；配比源收敛 BOM（item 加 5 列）、三模式算价移植后端纯函数+/trial、产品 yarn_specs JSON 与前端 TS 冻结（修订 ADR-0035 算价位置）；客户加默认毛利率；不做审批/核价申请/协议价表/工序费主档/自动回写采购价（二期）。错误码 1-030-034（避让打色 032/出口单证 033）、字典 2262–2268、菜单 10132/10136（避让出口单证 10126–10131）；术语进 business/production/baseinfo GLOSSARY；`_order.md` 序号 23。
- [08-release-production-workbench: 合同「下达生产」工作台](issues/08-release-production-workbench.md)：spec 落 `.scratch/release-production/spec.md` + ADR-0040。合同详情页「生产」Tab 内嵌唯一入口；下达一次生成胚布单(ACTIVE)+染色单(ACTIVE)+胚布检验单(GREIGE/DRAFT)，打色单可选（选已有 CONFIRMED 则引用，新建 PENDING 则不引用待手动补填），成品检验由染色单完成后手动按钮生成；`greige_order_color`/`dyeing_color` 加 `contract_line_id`+`contract_line_no`；合同行加 `released_qty` 条件更新累加（超下达/并发拦截，下游修改删除零回写）；同合同同产品合并、一行默认全量可改小、缸次一期不做；错误码 1-030-035、按钮 10114、零新表；`_order.md` 序号 24。
- [11-vendor-reconciliation: 供应商对账/付款申请](issues/11-vendor-reconciliation.md)：spec 落 `.scratch/vendor-reconciliation/spec.md`。加工对账取数源改判 **COMPLETED 检验单**（承检方+Σ实称重量/米数，全等级计入，织费缺省按米/染费按公斤可改），口径收敛唯一取数服务+源指纹防重；加工单价一期对账行手填（生产域零改动）、纱线采购对账=供应商维度 PURCHASE 入库明细+入库单加纱线采购单可选引用；对账单三态 DRAFT/CONFIRMED/CANCELLED、三类行（检验单汇总/入库明细/调整项）、快照零回写、已申请已付金额实时聚合不落字段；付款两步制：申请（RECON/PREPAY，02 号审批，ApprovalHandler 空实现）→ 付款登记（出纳显式扣款、部分付款、多申请核销、预付抵扣）；预付余额独立实时聚合不复用负余额；质保金/外币/价格主档/红冲进非目标；错误码 022 独占+023 共享段先占、字典 2269-2271、菜单 10730/10740；与 10/12 号 8 项共享件冲突清单随 spec 合并收口。
- [09-scheduling-and-material-check: 排产与齐套检查](issues/09-scheduling-and-material-check.md)：spec 落 `.scratch/scheduling-material-check/spec.md` + ADR-0041。**零新表零加列**：织造排产=现有机台任务上的只读视图（甘特/待排池/跨单列表，vue-ganttastic MIT 库）+ Service 同织机时段闭区间冲突硬拦截（COMPLETED 豁免、无日期进待排池、存量红标不回刷，不做排他约束）；不建染缸台账，排缸日历=进度反馈页内月历视图（点击改期非拖拽）；齐套=时点只读试算（计划重量×ratio − 台账余额，在途=ACTIVE 纱线采购单实采量近似口径，不冻结库存、不自动建采购单）；排产状态前端派生不改织造单状态机；菜单 10510 排产中心/10520 齐套检查、错误码仅 1-030-036-000、无新字典；AI 排程/能力矩阵/库存冻结进非目标；术语（排产/待排池/齐套/在途纱线/排缸日历/缸次）进 production GLOSSARY；`_order.md` 序号 25。
- [10-customer-reconciliation: 客户对账/开票申请/收款核销](issues/10-customer-reconciliation.md)：spec 落 `.scratch/customer-reconciliation/spec.md`（三轮 17 问定稿）。应收成立点=**发货审核**（Q1=A：shipment 审核/红冲同事务发事件、finance 记 AR_INC/AR_INC_REVERSAL 逐明细行+运费，前置 session 承载）；对账单=期间汇总+确认+打印凭证（五态、行级占用、多币种分单、签收前可对账、门户 4 端点契约冻结给 14 号+代客确认兜底）；收款单走 02 审批（CUSTOMER_RECEIPT）一步过账，核销锚定发货明细分录（严格同币种/FIFO 自动分配/预收 ADVANCE 分型转核销/红冲重开）；账龄五桶 FIFO 实时算+credit_days 逾期筛选（**预警 job 不做**，LUNA_AR_PERIOD_WARNING 一期不接线）；发票申请四态挂发货单+累计开票守卫（数电票字段、单头单税率、不做打印）；汇率主档归 10 号（SPOT/发货日汇率/核销同币种）；域公共表 ledger_entry/account/exchange_rate DDL 由 10 号先行、11/12 消费扩列；payment 共表 IN 侧扩列收口 11 号冲突清单；13 号加 Blocked by 10 并冻结 recordArReturnReversal；错误码 020/021/023(100–199)/026、字典 2272–2278、菜单 10720/10740/10770 按钮+**新增 10780 汇率**（ADR-0037 增补）；前端落 lunaerp-elmui/finance/；`_order.md` 序号 27。
- [13-sales-return: 客户销售退货](issues/13-sales-return.md)：spec 落 `.scratch/sales-return/spec.md` + ADR-0042。**库存回冲复用入库单不新增台账类型**（审批通过生成 inType=RETURN 的 DRAFT 入库单+stock_in 加 source_doc_type/source_doc_id 通用回指，到货质检审核写 STOCK_IN；全废自动取消入库单）；业务五态+02 审批五态双列（DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED，过账不可逆）；严格一单对一 COMPLETED 发货单、行血缘强制卷可选、行级原因、逐卷 RESTOCK/SCRAP（废品不入库）、数量+金额双维度防超退（APPROVING 即占量，行锁串行）、入库金额恒 0；应收冲减遵 10 号冻结接口（POSTED 发 SalesReturnPostedEvent、finance 写 AR_RETURN_REVERSAL、汇率 finance 按原发货分录解析），不建红字对账单/退款单/红字发票；错误码 1-030-037、字典 2279–2281、菜单 10141+10142–10149、单号 RT-；采购退货/过账后红冲/废品仓/退货成本价进非目标；`_order.md` 序号 28。
- [14-customer-portal: 客户门户 H5](issues/14-customer-portal.md)：spec 落 `.scratch/customer-portal/spec.md` + ADR-0044。免登=token 即身份直绑 customerId（不建门户用户实体，签收/确认人手填姓名）、单客户单 token 重发即吊销、128bit 防遍历、Redis 双键零新表、30 天滑动、URL token 即 API 凭证；端点挂 `/app-api/lunaerp/portal/**`（@PermitAll+自研 Filter+60/min 限流），10 号 4 端点契约仅替换身份来源；前端=luna-ui 内 `/portal/*` 免登路由区（`src/views/portal/` 正式目录、PortalLayout 移动布局不引 vant、constantRoutes 静态注册），发货单整单签收复用既有守卫、金额透明、PortalVO 白名单透出；04/10 号二维码 `?no=` 冻结 URL 不回改同组件兼容；错误码 038、按钮 10150；供应商门户/门户用户体系/短信自动发送/门户打印/出口单证查看页进非目标；15 号车间 H5 引用本票工程模式；`_order.md` 序号 30。
- [12-treasury-expense-payroll: 资金账户/收支总览/报销/工资初稿](issues/12-treasury-expense-payroll.md)：spec 落 `.scratch/treasury-expense-payroll/spec.md` + ADR-0043。一张 ledger_entry 承载资金流水+往来分录（10 号公共 3 表定稿基础上 ALTER 扩 5 列），唯一过账服务 FinancePostingService（条件更新 current_balance+INSERT 同事务，余额不足 023_004）——Q18 仲裁接受 10 号物化余额（放弃纯聚合）、Q19 仲裁汇率主档归 10 号 10780（12 号纯消费）；9 新表：转账 ZZ/其他收支 QT（登记即终态）、报销 BX（一步过账，附件≤10）、工资五表（一月一单、统一款项行+逐卷次品引用指纹不回写质检、APPROVED 空 Handler、发放批次 GF 可多次派生已发清、零批次可作废）；收支总览新菜单 10715（资金视角+毛利参考=AR_INC 净额−供应商 confirmed 对账，标注非法定报表）；错误码 020_105–110+024/025，字典 2282–2287（避让 13 号 2279–2281），10780 sort 校正 9；红冲/结汇/考勤算薪/借支/OCR 进非目标；`_order.md` 序号 29。
- [15-workshop-h5: 车间移动 H5](issues/15-workshop-h5.md)：spec 落 `.scratch/workshop-h5/spec.md` + ADR-0045（与 14 号门户 ADR-0044 互引）。**零新表 2 ALTER**（布票 exception_photos/录检明细 photos 各 jsonb ≤6）；报工=扫布票复用 start/complete/exception（修正 ticket 原文 progress-feedback 误用——外发染厂覆盖层不在范围），未派工扫票自助认领（worker IS NULL 条件更新）；胚布扫码质检内部幂等调 handoffInspection 自动交接+按 weavingTicketId 建行（pieceNo 缺省=票号、胚布 vatNo 放宽可空），成品 ROLL 进单逐卷录、重复定位改录；领纱=移动发纱台复用 dispatch，requestId+@Idempotent 防重复扣库存；员工 /admin-api 账号密码（非门户令牌）、企微钉钉一期仅 WebView 适配；工程=luna-ui 内独立 /h5 路由树（与门户同栖单工程）但引 Vant 4+html5-qrcode，键盘楔入一等公民、摄像头限 HTTPS；隐藏菜单 10530+10531–10533、seed workshop_worker/workshop_inspector 两角色；错误码顺延 **1-030-039**（避让并行 14 号已占 038）、零新字典、`luna.workshop.default-warehouse-id` 配置键；五终端×四流程冒烟；`_order.md` 序号 31。
- [16-acceptance-checklist: 商业化验收清单](issues/16-acceptance-checklist.md)：spec 落 `.scratch/commercialization-acceptance/spec.md`（`_order.md` 序号 32）。30 行 DoD 总表（11 格：七条+P95/租户/横纵向越权，存量 MVP 同标准补测无祖父条款）；P95 统一测法（万行双租户 9000:1000 含软删、100 次去预热、EXPLAIN 走索引）；冒烟双轨=JUnit 四链 API 自动冒烟（断言单据状态+库存台账/经营分录 category 金额）+UI 手工清单，W0–W3 波次出口闸；**数据范围 4 档=全部/本部门及以下/本部门/本人（弃自定义档），一期仅销售归属链 5 表接通**，施工衍生新票 [21-data-permission-wiring](issues/21-data-permission-wiring.md)（Blocked by 03,04）；seed 11 标准角色+权限/脱敏矩阵；交付物=重写 PG15+Redis7 四服务 compose 干净机一键、`docs/deployment/` 三文档、`sql/postgresql/install/` 幂等总入口（不引 flyway、回滚靠备份）、默认数据最小全集+可选 demo-seed；签字=G1 四链/G2 DoD/G3 性能安全/G4 交付物/G5 账实零差异 五 gate 一票否决，P0/P1 清零 P2≤10 书面知悉、四方签字。fog「交付物细节」清出。
- [19-errorcode-018-dedup: 错误码 018 段重号清理](issues/19-errorcode-018-dedup.md)：样品分析 8 码由 `1-030-018` 迁至 `1-030-040`（票内候选 030 已被审批引擎占用，取 039 后首个空闲 040；018 恢复为发染记录 ADR-0033 独占）；改 `SampleErrorCodeConstants` 全部码值 + `ErrorCodeConstantsTest`/`SampleAnalysisServiceImplTest` 断言与 DisplayName + 服务注释 + shared/sample GLOSSARY + ADR-0036 + MVP spec；金额/逻辑零改动；编译通过、ErrorCodeConstantsTest 2/2 + SampleAnalysisServiceImplTest 58/58 通过。
- [21-data-permission-wiring: 销售归属链接通 4 档数据范围](issues/21-data-permission-wiring.md)：spec 落 `.scratch/data-permission-wiring/spec.md` + ADR-0046 + 迁移脚本 `sql/postgresql/lunaerp-data-permission-wiring.sql`。4 注册表（客户/报价/合同/发货）统一 `salesperson_user_id`+`dept_id`（客户 +2 列、合同/发货各补 dept_id，报价随 05 票建表自带），联系人零列 EXISTS 实时继承客户、四入口统一"客户联系人不存在"；owner 缺省登录人可改、dept_id 服务端推导不信前端、带单只向前继承一次、调岗切面同事务刷 4 表（满足 16 号 §6.2 即时变化）；冻结"用户侧开启/系统侧 @DataPermission(enable=false)"契约（取号扫描/未发量/进度聚合/审批回写/财务过账/回收站/引用守卫）；自定义档三闸收口（前端过滤+API 拒绝 system 1_002_002_006+底座代码不删，common→SELF/test-dp→ALL）；11 seed 角色基线（ALL/4/5）；6 个 `(tenant_id,dept_id|salesperson_user_id) WHERE deleted=0` 部分索引保万行 P95；存量按租户只填空回填（creator→租户管理员/根部门兜底不写死）；零新字典/菜单、lunaerp 041 段不占；05/04 spec 已补增量；`_order.md` 序号 21，解阻 16 号 G2。

## Spec 执行波次与决策票对照

> 波次为规划分组（Q15）；**实际施工顺序以 [`_order.md`](../_order.md) 全局序号为准**（依赖驱动，序号可早于所属波次，如打色单因 24 号依赖其回指列而排 05）。全部 21 张决策票已 resolved，项目进入按 spec 施工阶段；决策研究票 17（老系统财务深挖）、18（芋道能力盘点）无独立 spec，分别为 W3 各 spec 与 W0 决策提供事实输入。

| 波次 | 全局序号 | Spec | Wayfinder 决策票 | 备注 |
|---|---|---|---|---|
| W0 商业地基 | — | —（无独立 spec） | [01-finance-domain-adr](issues/01-finance-domain-adr.md) | 已完成：ADR-0037 + finance GLOSSARY/菜单 10700 骨架/错误码段位，供 26–29 号消费 |
| W0 商业地基 | 18 | [common-foundation](../common-foundation/spec.md) | [04-common-foundation](issues/04-common-foundation.md) | 通用件（回收站/打印/导入/脱敏/消息），横切 lunaerp.common，零新表 |
| W0 商业地基 | 19 | [module-gateway](../module-gateway/spec.md) | [03-module-gateway](issues/03-module-gateway.md) | TenantPackage 模块开通 + 8 分组菜单 IA |
| W0 商业地基 | 20 | [approval-engine](../approval-engine/spec.md) | [02-approval-engine](issues/02-approval-engine.md) | 审批横切 5 表，ApprovalCompletedEvent 由各单据 Handler 自实现过账 |
| W0 商业地基 | 21 | [data-permission-wiring](../data-permission-wiring/spec.md) | [21-data-permission-wiring](issues/21-data-permission-wiring.md) | 派生票（Blocked by 03/04）：销售归属链 4 档数据范围，解阻 32 号 G2 |
| W0 商业地基 | — | —（代码修复，无 spec） | [19-errorcode-018-dedup](issues/19-errorcode-018-dedup.md) | 已完成：样品 8 码迁 1-030-040，018 恢复发染独占 |
| W1 售前链 | 22 | [export-docs](../export-docs/spec.md) | [07-export-docs](issues/07-export-docs.md)（研究）→ [20-export-docs-spec-grilling](issues/20-export-docs-spec-grilling.md)（定稿） | 单证套模式；合同 +4 外贸列、客户 +2 英文列、租户卖方出口配置单行表 |
| W1 售前链 | 23 | [cost-card-quotation](../cost-card-quotation/spec.md) | [05-cost-card-quotation](issues/05-cost-card-quotation.md) | 6 新表（纱价/成本卡 3/报价 2）；ADR-0039，修订 ADR-0035 算法位置 |
| W1 售前链 | 05 | [color-instruction](../color-instruction/spec.md) | [06-color-instruction](issues/06-color-instruction.md) | 序号前置：24 号依赖 color_instruction_id/no 回指列；错误码 1-030-032 |
| W2 合同→生产 | 24 | [release-production](../release-production/spec.md) | [08-release-production-workbench](issues/08-release-production-workbench.md) | 合同→生产唯一入口；ADR-0040；released_qty 条件更新，零新表 |
| W2 合同→生产 | 25 | [scheduling-material-check](../scheduling-material-check/spec.md) | [09-scheduling-and-material-check](issues/09-scheduling-and-material-check.md) | 排产只读视图+齐套试算；ADR-0041；零新表零加列 |
| W3 资金闭环 | 26 | [vendor-reconciliation](../vendor-reconciliation/spec.md) | [11-vendor-reconciliation](issues/11-vendor-reconciliation.md) | 加工对账取 COMPLETED 检验单；付款两步制；payment 共表与 27 号合并收口 |
| W3 资金闭环 | 27 | [customer-reconciliation](../customer-reconciliation/spec.md) | [10-customer-reconciliation](issues/10-customer-reconciliation.md) | 域公共 3 表（ledger/account/exchange_rate）DDL 先行、11/12 号消费扩列；前置=发货事件化 |
| W3 资金闭环 | 28 | [sales-return](../sales-return/spec.md) | [13-sales-return](issues/13-sales-return.md) | ADR-0042；复用入库单回冲（inType=RETURN）；冲减遵 27 号冻结接口，建议与 29 号同批施工 |
| W3 资金闭环 | 29 | [treasury-expense-payroll](../treasury-expense-payroll/spec.md) | [12-treasury-expense-payroll](issues/12-treasury-expense-payroll.md) | ADR-0043；唯一过账服务 FinancePostingService；9 新表+2 ALTER |
| W4 触点与验收 | 30 | [customer-portal](../customer-portal/spec.md) | [14-customer-portal](issues/14-customer-portal.md) | ADR-0044；token 免登零新表；依赖 27 号门户 4 端点契约 |
| W4 触点与验收 | 31 | [workshop-h5](../workshop-h5/spec.md) | [15-workshop-h5](issues/15-workshop-h5.md) | ADR-0045（与 30 号互引）；零新表 2 ALTER；与门户同栖 luna-ui 单工程 |
| W4 触点与验收 | 32 | [commercialization-acceptance](../commercialization-acceptance/spec.md) | [16-acceptance-checklist](issues/16-acceptance-checklist.md) | 收口验收 gate（G1–G5 一票否决），依赖 01–31 全部 |

## Not yet specified

- 各波次内并行 session 的施工分配策略（W0 spec 已齐；并行度与认领方式按 `_order.md` 序号前沿推进）
- 二期增值包边界清单（CRM/样品扩展/AI/小程序/HR/BI/结汇）——本期只记录不展开；另：门户出口单证查看页（20 号已冻结二维码 URL `/portal/export-doc?no=`，14 号一期最小集不含，二期门户扩展时接页）

## Out of scope

- 老系统存量数据迁移与切换工具（Q17=A，二期单独立项开新图）
- SaaS 平台运营端：租户自助注册、在线计费、效率点、平台监控驾驶舱（Q3=A）
- 借贷会计凭证、科目、三大报表（Q4=C）；`views/fms` 代码保持不动、不挂菜单
- 生成式 AI 全部功能（光球/OCR/语音/AI算料/AI邮箱）（Q5=A）；AI 优化排程（Q10=A）
- 微信小程序原生端（Q6=A，移动端走 H5）
- 供应商门户、租户自助字段配置中心、可视化打印模板设计器、报关/电子口岸对接、CRM（公海/商机漏斗）、寄样/选样/调样/门市剪样、HR/考勤/行政套件、营销模块（Q2/Q9/Q12/Q13/Q14，全部二期或增值包）
