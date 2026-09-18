# 02: 商品明细聚合端到端（子表 + 服务端重算 + 级联删除）

**What to build:** 用户在销售合同整页表单里可以维护多行商品明细——增删行、录入品种信息与数量价格，行金额与合同总数量/总金额在表单底部实时汇总；保存后重新打开完整回显，再次编辑时整表替换；可以保存没有明细的合同（汇总为 0）；删除合同时明细一并消失。明细是合同聚合的一部分，没有独立菜单、独立端点或独立权限（ADR-0003，刻意与客户联系人独立 Controller 相反）。

**Blocked by:** 01: 销售合同骨架端到端（主表 CRUD 跑通）

**Status:** ready-for-agent

- [x] 在 01 票的 SQL 脚本中追加明细子表 `lunaerp_business_sales_contract_item`（TenantBaseDO 标准列）：contract_id int8 必填、line_no int4、product_code varchar(64)、product_name varchar(128)、customer_color varchar(64)、composition varchar(128)、width varchar(64)、weight varchar(64)、quantity/unit_price/other_fees/amount numeric(18,2)、remark varchar(256)；普通索引 `(contract_id)`；序列 `lunaerp_business_sales_contract_item_seq`；脚本保持可重复执行
- [x] 后端新增 `SalesContractItemDO` + `SalesContractItemMapper`（继承 BaseMapperX），与合同同包或 `.item` 子包；**不建** item 的 Controller/Service/权限码/错误码
- [x] SaveReqVO 内嵌 `items: List<SalesContractItemSaveReqVO>`；RespVO 内嵌 items 详情（按 line_no 排序）；Page 列表不返回明细
- [x] create 与 update 在同一事务内：保存主表 → 按 contract_id 逻辑删除旧明细（update 时）→ 插入本次提交的全部明细；保存时按数组顺序重排 line_no
- [x] 服务端权威重算（请求体传入的主表 totalQuantity/totalAmount 与行 amount 一律忽略覆盖）：逐行 `amount = quantity × unit_price + other_fees`（null 数值按 0）；主表 `total_quantity = Σ quantity`、`total_amount = Σ amount`；零明细合法且汇总为 0
- [x] `GET /get` 返回嵌套 items；`DELETE /delete` 删除主表时按 contract_id 级联逻辑删除全部明细
- [x] `GET /page` 的 customerId 预留参数在本票确认透传可用（01 票已建查询构造，本票补测试/或前端外部调用验证即可，不新增 UI）
- [x] 前端编辑表单新增"商品明细"分区：el-table 可编辑行（产品编号/品名/客户色号/成分/门幅/克重为文本输入；数量/单价/其他费用 el-input-number 两位小数；行金额只读；行备注；行删除按钮）+ 底部"添加明细行"；本票产品编号为**普通手填输入**（远程搜索产品库回填属 03 票）
- [x] 前端行金额 = 数量 × 单价 + 其他费用实时计算，底部汇总栏实时预览总数量/总金额（前端仅预览，提交不阻断服务端重算）；支持空明细提交；数值用 decimal 语义传输避免浮点误差
- [x] 编辑/只读模式打开时明细完整回显、按 line_no 排序；只读模式明细同样禁用
- [x] 纯 Mockito 单元测试（在 SalesContractServiceImplTest 中追加，同 seam）：多行明细聚合 insert 并重排 line_no、update 先逻辑删旧再插新、行金额公式（含 null 按 0）、totalQuantity/totalAmount 服务端重算且覆盖请求值、零明细汇总 0、删除合同级联逻辑删除明细、get 返回嵌套 items、customerId 透传
- [ ] 验收：新建含 3 行明细的合同，行金额与底部汇总随输入变化；保存后重开三行完整回显且顺序一致；编辑删掉 1 行、改 1 行数量后保存，重开只剩修改后的 2 行且主表汇总同步变化；手工构造"前端传入错误汇总"的请求，落库汇总仍等于明细重算值；保存零明细合同成功且汇总为 0；删除合同后其明细行均为 deleted=1；全程无任何 item 独立 URL/菜单/权限点
