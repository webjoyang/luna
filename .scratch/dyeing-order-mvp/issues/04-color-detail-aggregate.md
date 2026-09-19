# 04: 染色颜色明细聚合

**What to build:** 用户在染色单工作台维护多行颜色明细，每行包含：客户色号（从合同明细带出可改）、颜色名称（手填）、本单染色数量（成品口径）、匹数、投染数（含损耗的实际投入量）、投染数单位（复用合同单位字典 KG/M）、合同数量（从合同明细带出只读对比）。选销售合同时自动把该合同全部明细行拷到染色单明细（产品编码/产品名称/客户色号/合同数量），用户可删行、改字段。颜色明细随单整体保存、级联删除，无独立端点。零明细合法。

**Blocked by:** 03 查看与修改染色单

**Status:** ready-for-agent

- [ ] 染色颜色明细子表已建（含主键序列、dyeing_order_id 索引、全列注释）
- [ ] 保存载荷内嵌颜色行，无独立端点；事务内按 dyeing_order_id 删旧行、插新行并重排 line_no
- [ ] 每行字段：customer_color_no、color_name、quantity（本单染色数量）、pieces（匹数）、dyeing_input_qty（投染数）、dyeing_input_unit（投染数单位，复用 lunaerp_sales_contract_unit 字典）、contract_qty（合同数量）
- [ ] 选销售合同后前端自动带出合同全部明细行：product_code/product_name/customer_color→customer_color_no、quantity→contract_qty
- [ ] 工作台"基础信息"Tab 内新增颜色明细可编辑表格：新增/删除行、投染数单位下拉（KG/M）、合同数量列只读
- [ ] 查看模式下颜色行只读；零颜色单据可正常保存
- [ ] 详情接口嵌套返回颜色行（按 line_no 排序），列表不返回
- [ ] 测试覆盖：聚合保存（删旧插新/重排行号）、零颜色合法、详情嵌套返回
