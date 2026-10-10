# 10: 转合同预填 + 报价套打 + W0 打印注册 + 合同来源列接线

**What to build:** 用户在最新 CONFIRMED 报价单上点「生成销售合同」：系统校验状态与版本（仅最新 CONFIRMED 可调，非最新版报 `1_030_034_103`）后，把报价头（客户/联系人/币种/贸易类型缺省内销）与明细（产品编号/品名/色号/成分/门幅/克重/数量/报价单价）预填进销售合同新建页（合同页币种锁定不可改、只能改价与量），保存时服务端校验：报价存在、为最新 CONFIRMED、币种一致（不一致报 `1_030_034_107`），写入 `source_quotation_id`/`source_quotation_no` 两列；不回写报价单状态、不锁数量、不追踪已转量；允许多次预填多份合同。过期报价（「已失效」派生态）转合同时弹警告但不硬锁。合同详情可见来源报价号、可从合同侧跳回报价详情。`GET /print-data?id=` 返回白诺 VO（结构保证不含任何成本/毛利字段，后端白诺字段非依赖脱敏），W0 打印模板注册名 `quotation-default`（A4 固定套打：客户抬头/报价号-版本/有效期/币种/产品规格行/数量/单价/金额/备注/签章区），每次调用 `/print-data` 写打印操作日志。`SalesContractSaveReqVO` 加可空 `sourceQuotationId`/`sourceQuotationNo`；合同新建页带 `quotationId` 参数时先调 `/contract-prefill` 预填。「生成销售合同」草稿 owner 继承报价单 owner（业务员已停用则回退登录用户），合同 `dept_id` 按其 owner 重新推导不继承快照；归属向前继承一次，此后各自独立。

**Blocked by:** 08（CONFIRMED 状态 + `version_no` 基础，使「最新版本」判定可工作）

**Status:** resolved

- [ ] 报价 Controller 加 `GET /contract-prefill?id=`（仅最新 CONFIRMED 可调，非最新版报 `1_030_034_103`；失效加 warning 标志不拦截）；权限复用 query
- [ ] 报价 Controller 加 `GET /print-data?id=`（白诺 VO 保证不含成本/毛利字段）；每次调用写打印操作日志；权限 `lunaerp:business:quotation:print`
- [ ] W0 打印模板注册：模板名 `quotation-default`，A4 固定套打，落 `views/lunaerp/business/quotation/print/`
- [ ] `SalesContractSaveReqVO` 加可空 `sourceQuotationId`/`sourceQuotationNo`；保存时服务端校验：报价存在、为最新 CONFIRMED、币种一致（不一致报 `1_030_034_107`），写入两来源列；不回写报价状态
- [ ] 销售合同新建页带 `quotationId` 参数时先调 `/contract-prefill` 预填（头映射 + items 映射）；合同页币种锁定不可改
- [ ] 「生成销售合同」草稿 owner 继承报价单 owner（业务员已停用则回退登录用户）；合同 `dept_id` 按其 owner 重新推导不继承快照；归属向前继承一次
- [ ] 过期报价转合同弹警告不硬锁
- [ ] 合同详情可见来源报价号；从合同侧可跳回报价详情
- [ ] 前端：报价详情/列表「生成销售合同」按钮（仅最新 CONFIRMED 启用），跳转合同新建页预填；套打预览（无任何成本/毛利字样）
- [ ] `QuotationServiceImplTest` 单测：contract-prefill 非最新版/非确认态报 103、失效仅 warning、币种一致校验报 107、来源列回写、可多次转；print-data 响应不含成本/毛利字段；操作日志写入
- [ ] 验收：从报价侧点「生成销售合同」跳转合同新建页预填、币种锁定、保存后合同详情可见来源报价号；用同报价再转第二张合同成功；套打预览无成本/毛利字样；过期报价转合同弹警告但可继续
