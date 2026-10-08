# 09: 供应商/产品/纱线规格导入

**What to build:** 三类主数据标准导入，字典列有下拉；重复编号逐行拦下不产生重复档案；纱线规格参考价列受 `lunaerp:sensitive:amount` 管控——导入人无金额权限则按行报错提示无权限。在 08 号通用工具基础上落地 3 个 ImportVO + Service + 端点 + 列表按钮。供应商（编号*租户内唯一含已删行口径/企业名称*/简称/供应商类型字典/机型/备注）；产品（产品编号*租户内唯一错误码 1_030_001_001 口径/产品名称*/类型/面料分类/织物组织/计价方式字典/单位/状态等扁平核心字段，**不含**成本项/纱线规格/标签/历史关联 UUID 字段——导入后 UI 维护）；纱线规格（材质名称*精确匹配启用状态材质匹配不到按行错误/规格名称*/标准名*租户+材质内唯一冲突报 1_030_008_003/纺纱方式/光泽字典/参考价非负金额受 `lunaerp:sensitive:amount` 管控/备注）。

**Blocked by:** 08, 02

**Status:** ready-for-agent

- [ ] 供应商 ImportVO + Service：编号*租户内唯一（含已删行口径）冲突报错、企业名称*、简称、供应商类型字典 `lunaerp_supplier_type`、机型、备注
- [ ] 产品 ImportVO + Service：产品编号*租户内唯一（1_030_001_001 口径）、产品名称*、类型/面料分类/织物组织/计价方式字典列、单位、状态等扁平核心字段；**排除** costItems/yarnSpecs JSON、标签、greigeTypeId/supplierId 历史 UUID 字段
- [ ] 纱线规格 ImportVO + Service：材质名称*（精确匹配启用状态纱线材质，匹配不到按行错误）、规格名称*、标准名*（租户+材质内唯一，冲突报 1_030_008_003）、纺纱方式/光泽字典、参考价（非负金额，受 `lunaerp:sensitive:amount` 管控——导入人需有金额权限否则按行错误 1_030_031_102）、备注
- [ ] 3 个端点（import-template / import），路径前缀替换为各自模块路径，权限 `lunaerp:baseinfo:supplier:import` / `lunaerp:baseinfo:product:import` / `lunaerp:production:yarn-spec:import`
- [ ] 3 个列表工具栏新增"下载模板 / 导入"按钮（产品在正式目录 `views/lunaerp/`，客户/供应商以各自当前落点为准）
- [ ] 测试：三类成功路径、错误 Excel、重复编号逐行拦下、整批回滚、5000 上限、纱线规格材质匹配/标准名唯一/金额权限校验、字典列校验
