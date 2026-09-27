# 05: 从检验单幂等生成质检确认（检验单工作台按钮）

**What to build:** 质检员/领班在一张录检中（ACTIVE）或录检完成（COMPLETED）的检验单工作台点"生成质检确认"，系统扫描该单录检明细，把二等品/三等品/等外品（B/C/DEFECTIVE）的卷逐卷生成待确认记录并完整快照卷信息；一等品（A）与无等级卷不生成；重复点击安全——已存在有效确认行的卷跳过，只补新卷，点击后提示"新增 X 卷 / 跳过 Y 卷"。本票完成后质检确认列表出现检验单引入的待确认行。

**Blocked by:** 03 (列表只读切片；不强制等 04，可并行)

**Status:** closed

- [x] `POST /lunaerp/production/inspection-confirmation/generate-from-order`（:generate 权限），入参 inspectionOrderId，返回 {generatedCount, skippedCount}
- [x] 检验单不存在报 1_030_014_002；状态非 ACTIVE/COMPLETED（DRAFT/CANCELLED）报 1_030_014_003
- [x] 仅 grade ∈ {B,C,DEFECTIVE} 的录检卷生成；A 与 grade 为空跳过
- [x] 幂等：查本表该检验单下已有有效行（status ∈ PENDING,CONFIRMED）的 item_id 集合，命中跳过；REVOKED 行不阻挡（撤销克隆出的 PENDING 已在有效集合中）
- [x] 新行字段：source=INSPECTION、status=PENDING、inspection_order_id/code、inspection_order_item_id、卷快照（rollNo/vatNo/grade/machineNo/product 编码名称/color/colorNo/styleNo/measuredWeight/lengthM）逐字段从录检明细与检验单复制；每行预生成 chain_key
- [x] 整体一个事务；跳过不报错
- [x] 前端检验单工作台"录检明细"Tab 工具栏新增"生成质检确认"按钮：受 `lunaerp:production:inspection-confirmation:generate` 控制、仅 ACTIVE/COMPLETED 态可点（其余隐藏或禁用）；成功 ElMessageBox 提示新增/跳过计数并可跳转质检确认列表
- [x] Service 单测增补：B/C/DEFECTIVE 各生成且快照完整、A/无等级跳过、重复调用幂等计数正确、REVOKED 链场景仍跳过、检验单不存在报 002、DRAFT/CANCELLED 报 003、ACTIVE/COMPLETED 放行
