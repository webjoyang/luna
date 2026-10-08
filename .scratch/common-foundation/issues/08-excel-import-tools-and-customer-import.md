# 08: Excel 导入通用工具 + 客户导入

**What to build:** 实施人员私有化交付后用标准模板一次性导入几千条客户档案，字典列有下拉不会填错；导入文件有 3 行错误时系统不写入任何数据，给一份标好每行错误原因的 Excel，改完重新导入即可。交付通用导入工具（FastExcel `ExcelUtils.read` + `DictConvert` + `@ExcelColumnSelect` 下拉 sheet，参照芋道用户导入）：逐行校验收集全部错误，有任一错误整批不写库返回错误 Excel，全部通过单事务批量插入返回成功条数；错误 Excel 原始列原样保留末列追加"错误原因"（一行多原因用 `;` 分隔）`.xlsx` 流下载；同步导入单文件 5000 行上限（报 1_030_031_100）、空文件报 1_030_031_101；字典列值不在字典按行错误；仅新增（唯一键冲突按行报错，不做 upsert）。以客户导入为 tracer bullet：客户模板（企业名称* 简称 客户类型 行业/规模/来源/合作状态 统一社会信用代码 英文名称/英文地址 备注），导入行 status 默认 ACTIVE，归属人=登录用户、归属部门=其当前部门（服务端推导，不支持 Excel 指定归属人，与页面建档同路径）。

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] 通用导入工具落在 `common/dataimport` 子包：逐行校验 + 全量错误收集、错误 Excel 生成（末列"错误原因" `;` 分隔）、5000 行上限、空文件校验、字典列校验
- [ ] 客户 ImportVO + Service：列定义按 spec（企业名称* 重名检查、简称空则用企业名称回填、客户类型/行业/规模/来源/合作状态字典列、统一社会信用代码 18 位格式校验、英文名称/英文地址写 name_en/address_en、备注）
- [ ] 仅新增：唯一键（企业名称）冲突按行报错；导入行 status 默认 ACTIVE
- [ ] 归属人=执行导入的登录用户、归属部门=其当前部门（服务端推导，无 Excel 归属列，与页面建档同路径——ADR-0046）
- [ ] 端点：`GET /lunaerp/baseinfo/customer/import-template`（模板下载）+ `POST /lunaerp/baseinfo/customer/import`（multipart），权限 `lunaerp:baseinfo:customer:import`
- [ ] 客户列表工具栏新增"下载模板 / 导入"按钮
- [ ] 测试：成功路径、错误 Excel 回报（一行多原因 `;`）、重复企业名称逐行拦下、整批回滚、5000 行上限、空文件、字典列错误
