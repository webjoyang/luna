# 10: 打色指示单套打

**What to build:** 注册统一打印注册表项（bizType=COLOR_INSTRUCTION，templateCode=COLOR_INSTRUCTION_SHEET，paper=A4，permission=`lunaerp:production:color-instruction:print`）；独立 print-data 接口返回打印 VO（不脱敏——无金额字段；联系人按打印豁免惯例全量），`@LogRecord(type="打印")` 留痕。前端 `ColorInstructionSheet.vue` A4 固定版式：抬头（单号/单据日期/客户/联系人/产品编号名称/成分幅宽克重/打色承接方/要求完成日）+ 颜色行表（行号/客户色号/厂内色号/颜色名/色块/主灯源/辅灯源/色卡参考/份数/轮次/行状态）+ 客户反馈/备注 + 签字位（制单/打色/客户确认）。**全部轮次行均输出**，REJECTED 行划线标注轮次与行状态。列表/详情"打印"按钮统一走 `<PrintDispatcher biz-type="COLOR_INSTRUCTION" :biz-id="row.id" />`。

**Blocked by:** 04 行级动作：通过 / 驳回重打（含开始打色）

**Status:** ready-for-agent

- [ ] 注册统一打印注册表项：`{ bizType: 'COLOR_INSTRUCTION', templateCode: 'COLOR_INSTRUCTION_SHEET', name: '打色指示单', paper: 'A4', permission: 'lunaerp:production:color-instruction:print', component: ColorInstructionSheet, fetchApi: (id) => colorInstructionApi.getPrintData(id) }`（对照 `.scratch/common-foundation` 统一打印契约）
- [ ] print-data 独立接口 GET `/{id}/print-data`（`:print` 权限）：返回独立打印 VO（不脱敏，无金额字段；联系人按打印豁免全量），结构与 A4 版式对齐——单据头 + 颜色行（全部轮次，按 lineNo 排序）+ 客户反馈/备注
- [ ] `@LogRecord(type="打印")` 入 `system_operate_log`
- [ ] 前端 `ColorInstructionSheet.vue` A4 固定版式：抬头区 + 颜色行表（行号/客户色号/厂内色号/颜色名/色块 colorHex 渲染/主灯源/辅灯源/色卡参考/份数/轮次/行状态 DictTag）+ 客户反馈/备注 + 签字位（制单/打色/客户确认）
- [ ] 全部轮次行均输出（含 REJECTED 历史轮次行）；REJECTED 行划线标注、显示轮次与行状态、显示驳回意见
- [ ] 前端列表/详情"打印"按钮统一走 `<PrintDispatcher biz-type="COLOR_INSTRUCTION" :biz-id="row.id" />`（`v-hasPermi` print 码）
- [ ] 测试覆盖：print-data 返回结构（单据头 + 全部轮次颜色行）、REJECTED 行标注与轮次正确、`@LogRecord` 留痕、无 print 权限 403
