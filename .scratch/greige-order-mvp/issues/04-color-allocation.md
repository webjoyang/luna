# 04: 颜色投胚分配

**What to build:** 用户在胚布单表单中按颜色逐行维护成品下单量与损耗，系统实时并在保存时由服务端权威折算投胚需求；颜色行随单据整体保存、级联删除。零颜色合法，计划投胚量与颜色合计不强一致。

**Blocked by:** 03 查看与修改胚布单

**Status:** ready-for-human（代码实现与单测完成，运行时验收待人工执行，见 Comments）

- [x] 颜色投胚分配子表已建（含主键序列、order_id 索引）
- [x] 保存载荷内嵌颜色行，无独立端点；事务内按单删旧行、插新行并重排 line_no
- [x] 每行字段：颜色、成品下单量(KG)、损耗率、投胚需求(KG)、备注
- [x] 行损耗为空时取主单预设损耗、再缺省 0；投胚需求按"成品量÷(1−损耗率)"服务端权威重算，前端传值被覆盖
- [x] 编辑页可编辑颜色表格：新增/删除行、投胚需求只读实时计算、底部展示合计仅供参考
- [x] 计划投胚量独立保存，不与颜色投胚需求合计做一致校验
- [x] 查看模式下颜色行只读；零颜色单据可正常保存
- [x] 详情接口嵌套返回颜色行（按 line_no 排序），列表不返回
- [x] 测试覆盖：聚合保存（删旧插新/重排行号）、投胚需求重算口径、行损耗缺省取主单、null 成品量按 0

## Comments

- 2026-09-19（agent）：代码实现完成。落点：子表 `lunaerp_production_greige_order_color`（DDL 追加进 `lunaerp-production-greige-order.sql`：主键 DO 块判存、`idx_lb_greige_order_color_order_id` 索引、主键序列、列注释），DO `GreigeOrderColorDO`/Mapper `GreigeOrderColorMapper` 落 `production.greigeorder.color` 子包（对齐 CONTEXT 模块落点），行 VO `GreigeOrderColorSaveReqVO`/`GreigeOrderColorRespVO`，主 VO 嵌套 `colors`。服务端无独立端点：创建随主表插入颜色行（insertBatch 回填 order_id），更新在事务内 `deleteByOrderId` 删旧行再插新行（清空颜色也执行删除，零颜色合法）、按数组顺序重排 line_no（从 1 起）；行损耗缺省链 行值→主单预设损耗→0（保存为解析后值，scale=4 对齐 numeric(6,4)）；投胚需求服务端权威重算 = 成品量÷(1−损耗率)（null 成品量按 0，scale=2；前端传值一律覆盖；损耗率 ≥ 100% 除数 ≤ 0 的异常口径按不放大处理避免除零）。`GET /get` 详情嵌套返回颜色行（Mapper 按 line_no 升序），`/page` 不填充。计划投胚量独立保存，不做一致校验。前端 API 增 `GreigeOrderColorVO` 与 `colors` 字段；编辑页新增"颜色投胚分配"区块：可编辑表格（颜色/成品下单量/损耗率百分比/备注、删行、添加按钮），投胚需求列只读实时预览（口径与后端一致：行损耗留空取主单损耗），底部合计仅供参考并标注"不与计划投胚量做一致校验"，查看模式控件 disabled 且隐藏增删；回填时后端 lossRate 小数→百分比展示，提交时反向换算并入载荷。测试：`GreigeOrderServiceImplTest` 新增 9 例（创建重排 line_no 与重算口径 500÷0.95=526.32/300÷0.90=333.33、前端 demandQty 传入被覆盖、行损耗缺省取主单 0.08→108.70、null 成品量按 0、零颜色不插行、更新删旧插新回填 orderId、清空颜色删全部旧行、更新行损耗同样取主单、损耗率≥1 不放大、详情嵌套返回），全类通过；lunaerp 模块全量测试通过；vue-tsc 对改动文件零错误（既有无关模块报错不计）。运行时验收（启服务走查：编辑页增删颜色行保存后详情回显、清空颜色保存、查看模式只读、零颜色新建保存）待人工执行。
