# 06-color-instruction: 打色指示单

Type: grilling
Status: resolved
Blocked by: 

## Question

设计打色指示单并产出 spec（W1，Q14-h），grilling 时先调 Skill "grilling" 与 "domain-modeling"：

1. 数据模型：客户 + 产品 + 颜色/色号/灯源/色卡参考/样布照片附件；独立建档，不依赖样品链（Q14-h）；与现有染色单颜色明细（dyeing color detail）的字段对齐，避免两套颜色语义；
2. 状态机：待打色→打色中→客户确认（或内部确认）→已投产/作废；终态守卫；
3. 单据引用关系：打色单作为胚布单/染色单的来源单据（来源单据类型+ID 字段约定，对照现有 shipment from-contract 的做法）；
4. 菜单落位："生产"一级菜单下；
5. 打印：打色指示单套打模板（走 04 号统一打印组件的注册方式，可先约定占位）。

产出：`.scratch/color-instruction/spec.md`（按统一 DoD）。术语（打色单/色号/灯源）进对应域 GLOSSARY。

## Answer

grilling 两轮十七问全部按推荐锁定，spec 落 `.scratch/color-instruction/spec.md`。核心决策：

1. **两表**：主表 `lunaerp_production_color_instruction` + 颜色行 `lunaerp_production_color_instruction_color`；行随单聚合、无独立界面；保存为**差异保存 + 行状态守卫**（Q11 修正 Q1 的删旧插新，以保留 REJECTED 轮次留痕）。
2. **颜色语义全域对齐**：`customerColorNo`+`colorName` 与染色颜色明细/发货明细同口径，另加 `factoryColorNo`/`colorHex`；不沿用合同明细弱文本 `customerColor`。灯源字典 `lunaerp_color_light_source`（2255），主灯源必填。色卡参考与样布照片（JSON、infra 文件服务）落颜色行。
3. **单据五态**：PENDING/IN_PROGRESS/CONFIRMED/IN_PRODUCTION/VOID；`confirmType=CUSTOMER/INTERNAL`；确认守卫=无 PENDING 行且 ≥1 APPROVED；CONFIRMED 无下游引用可回退；**投产由下游引用自动触发**（胚布单保存即触发、染色单 ACTIVE 时触发，幂等条件更新），IN_PRODUCTION/VOID 终态；仅 PENDING 可软删。
4. **行三态 + 轮次**：PENDING/APPROVED/REJECTED；"驳回重打"冻结旧行、克隆 round+1（不继承照片）；打印件含全部轮次。
5. **下游引用**：胚布单/染色单各加 `color_instruction_id`+`color_instruction_no` 显式列（不引入通用 sourceType/sourceId）；仅 CONFIRMED/IN_PRODUCTION 可引；被有效下游引用禁作废/删除。
6. 菜单 10230（生产目录 sort=0）+10231-10235；权限码 `lunaerp:production:color-instruction:{query,create,update,delete,confirm,print}`；A4 套打 `COLOR_INSTRUCTION_SHEET` 本期实现并注册统一打印注册表；回收站 handler（仅 PENDING）。
7. 错误码新取段 **`1-030-032`**（000-007 + 行 100），已入 shared GLOSSARY 段位总表；字典 2255-2257（data 3736-3749）。
8. 一期不接审批/消息、不做 Excel 导入导出、不挂样品链；寄样/选样/调样/剪样二期。

术语（打色指示单/客户色号/灯源/色卡参考/打色轮次/打色承接方等）已入 production GLOSSARY，并同步模块边界/字典/错误码节。
