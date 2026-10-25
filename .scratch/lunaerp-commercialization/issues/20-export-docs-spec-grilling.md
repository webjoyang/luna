# 20-export-docs-spec-grilling: 出口单证 spec 定稿 grilling

Type: grilling
Status: resolved
Blocked by: 07

## Question

07 号研究已产出 spec 草案 `.scratch/export-docs/spec.md`（Status: draft），本 ticket 通过 HITL grilling 将其定稿（转 ready-for-agent）。grilling 时先调 Skill "grilling" 与 "domain-modeling"。

07 号 Answer 点名的三个必决项：

1. **G1 合同外贸字段回补**：销售合同 MVP 落地时刻意砍掉的 7 个外贸字段（buyerNameEn/buyerAddressEn/buyerBankInfo/incoterm/portOfLoading/portOfDischarge/paymentTerm）是否一期回补？动已落地模块（DDL 加列 + 表单按 trade_type=EXPORT 条件显示）的成本 vs 每张单证重复录入条款的代价；
2. **卖方出口配置落点（G2）**：草案拟用芋道 `infra_config` 键 `lunaerp.export.info`，但代码核查发现 `ConfigDO` 标注 `@TenantIgnore`（全局配置、不做租户隔离），与"每租户一个卖方身份"冲突，需要重新定点；
3. **错误码段位**：从段位总表取未占用段（012 已预留库存台账，020–029 finance，030 审批，031 common）。

grilling 中还应对齐 04 号 common-foundation 已定稿契约（07 号研究早于/并行于其定稿，草案有偏差）：

4. 打印必须接入 W0 统一打印内核（PrintDispatcher + 模板注册表 + 独立 print-data 接口豁免脱敏 + @LogRecord 留痕），而非草案写的 inspection-confirmation 老 PrintDialog 先例；三模板注册 EXPORT_DOC_PI/CI/PL；
5. 二维码码格式遵守冻结契约"完整 URL + 域名走 infra_config"，而非草案的裸码 `ED:{doc_no}`——门户一期无单证查看页（Q13=A 最小集不含），需定一期落点；
6. 补声明 RecycleBinHandler（W1+ 模块统一 DoD 挂账项）；定稿守卫/草稿打印/resync（老系统"只填空字段"核心工作模式，草案 API 无对应端点）等草案悬置项一并收口。

产出：`.scratch/export-docs/spec.md` 转 ready-for-agent（字段模型/API/状态机/打印契约/错误码段位/菜单/验收全部可执行），术语归 business GLOSSARY，段位入 shared GLOSSARY 总表。

## Answer

两轮 grilling 共 11 问，用户全部采纳推荐，spec 已定稿 `.scratch/export-docs/spec.md`（ready-for-agent）。结论：

1. **G1 七字段分三处落**：销售合同 +4 可空外贸条款列（incoterm/起运港/目的港/payment_term，表单仅 EXPORT 显示）；客户档案 +2 可空列（name_en/address_en，买方英文名址是客户属性）；买方银行信息随单变化，留单证套手填。跨模块改动由本 spec"前置改动"章承载为第 0 个前置 session，不回改历史 spec（Q9）。
2. **G2 卖方配置弃用 infra_config**（代码核查发现 ConfigDO @TenantIgnore 全局不隔离），新建租户单行表 `lunaerp_business_export_seller_config`（tenant_id 唯一、upsert、无删除端点），出口单证列表页弹窗维护 + `:seller-config` 权限位（菜单 10131）。
3. **打印接入 04 号统一内核**：EXPORT_DOC_PI/CI/PL 注册模板注册表，print-data 独立 VO 豁免脱敏 + @LogRecord；二维码印完整 URL `{luna.portal.base-url}/portal/export-doc?no={doc_no}`，一期门户无页链接可不通、未配置不阻断打印。
4. **补 resync**（只填空不覆盖，优先级 发货单>合同>卖方配置，MANUAL 不可用）；定稿守卫明细非空；DRAFT 可打印；声明 RecycleBinHandler(EXPORT_DOC) 仅收草稿；套号 ED+年+4 位流水照 ADR-0004。
5. 客户 Excel 导入同步加英文名称/英文地址两列（已小改 common-foundation spec 客户模板表，W0 生效）。
6. **并行会话避让（wayfinder 并发编辑的实物教学）**：grilling 当口推荐错误码 032、字典 2249–2252，落稿核查段位总表时发现 06 打色指示单（claimed 中）已登记 032，且 2249/2250（product-info-align）、2251–2254（sample-analysis）、2255–2257（color-instruction）连续被占；出口单证顺延 **1-030-033**、字典 **2258–2261**、dict_data **3750 起**，避让记录已追加 shared GLOSSARY。
7. 菜单 10126–10131（销售 10100 下，sort 3）；术语（出口单证套/PI/CI/PL/单证套号/定稿/resync/卖方出口配置/唛头）入 business GLOSSARY；16 号阻塞由 07 替换为 20，本票 resolve 后 16 号恢复就绪。

## Prototype（resync + 状态机逻辑自验）

**原型**：`.scratch/export-docs/prototype-resync-logic.html`（单文件、双击运行、LOGIC 分支）。
**问题**：spec 的 resync（只填空不覆盖、三层 priority 发货单>合同>卖方配置）+ DRAFT/FINALIZED 守卫矩阵 + 多源字段映射优先级是否成立。
**结论**：成立。43 条断言全过（8 个引导场景 + 边角守卫 + 幂等）。纯逻辑模块 `Logic` 无 DOM 引用，可原样 lift 进后端 `ExportDocServiceImpl` 的 resync/finalize/revoke/softDelete/restore 方法体——状态机守卫顺序（NOT_EXISTS → DELETED → 状态白名单 → 业务守卫）、resync 三层 fillEmpty（抬头 hdrMap → meta metaMap → 明细按 lineNo 匹配）、PL TOTAL meta手填优先 else 求和、卷清单净重 Σ 聚合、实发客户名优先于合同抬头，全部按 spec 行为执行。无需改 spec。
**保留**：原型 HTML 留在 `.scratch/export-docs/` 作为 primary source（已是 scratch 区，不入 main、不进 git；spec.md 即为吸收后的最终决策）。
