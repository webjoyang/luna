# 04: 前端表单重构 — 5 分区布局 + 删除字段 + 字典下拉 + 梭织参数

**What to build:** 重构 ProductEditModal.vue 为 5 个分区（按顺序）：图片（封面+详情多图）、基础信息（产品编号/品名/类别下拉/供应商样号/成分/备注/产品标签/对外展示/首页推荐）、技术规格（门幅/克重/颜色/工艺/机型/布种下拉/胚布品种/标准米长/梭织参数5字段）、纱线规格（占位，下个 ticket 填充）、成本结构明细（占位，下个 ticket 填充）。删除产量/花型号/临时编号/算长系数 4 个字段。类别字段改为 DictSelect（字典 lunaerp_product_category，存 dict_value + dict_label 冗余）。布种字段改为 DictSelect（字典 lunaerp_fabric_type，存 dict_value + dict_label 冗余）。技术规格分区新增 5 个梭织参数输入框（总经根数/筘号/经密/纬密/上机纬密，全选填）。supplierCode 标签改为"供应商样号"。

**Blocked by:** 02 (Backend DO/VO alignment)

**Status:** ready-for-agent

- [ ] ProductEditModal.vue 表单按 5 分区顺序排列（图片→基础信息→技术规格→纱线规格→成本结构明细）
- [ ] 删除产量(output)/花型号(patternNo)/临时编号(tempCode)/算长系数(conversionRate) 4 个表单字段
- [ ] 类别字段使用 DictSelect 组件，从 lunaerp_product_category 字典取数，选中存 dict_value + dict_label
- [ ] 布种字段使用 DictSelect 组件，从 lunaerp_fabric_type 字典取数，选中存 dict_value + dict_label
- [ ] 技术规格分区新增 5 个梭织参数输入框（总经根数/筘号/经密/纬密/上机纬密）
- [ ] supplierCode 字段标签显示为"供应商样号"
- [ ] 纱线规格和成本结构明细分区有占位区域，不报错
- [ ] 保存产品时新字段（梭织参数/布种/类别标签）正确提交到后端
- [ ] 编辑已有产品时所有新字段正确回显
