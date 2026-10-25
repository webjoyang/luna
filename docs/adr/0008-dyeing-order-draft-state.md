# 0008 — 染色单比其他单据多一个 DRAFT 草稿态

## 状态

accepted

## 上下文与决定

染色单状态为四态：`DRAFT` 草稿（新建缺省）/ `ACTIVE` 进行中 / `COMPLETED` 已完成 / `CANCELLED` 已取消。销售合同与胚布单均为三态 `ACTIVE` / `COMPLETED` / `CANCELLED`（新建缺省 ACTIVE，无草稿态）。染色单刻意多一个 `DRAFT`，且保存时默认为 DRAFT，需点"下达"才转 ACTIVE。

状态字段类型与其他单据一致：`varchar(16)` 字符串枚举，走字典 `lunaerp_dyeing_order_status`（DRAFT/ACTIVE/COMPLETED/CANCELLED）。

状态约束：
- 编辑：仅 DRAFT 可编辑，ACTIVE/COMPLETED/CANCELLED 只读
- 删除：仅 DRAFT 可删；ACTIVE 不可删，只能"取消"转 CANCELLED；COMPLETED/CANCELLED 不可删
- 流转：DRAFT → ACTIVE（下达）/ ACTIVE → COMPLETED（完成）/ ACTIVE → CANCELLED（取消）

## 理由

1. **业务形态不同**：染色单是下达给染厂的生产指令，业务上存在"先打草稿反复确认、再正式下达"的需求——草稿可以反复修改字段与明细，下达后即锁定不可改。销售合同/胚布单新建即生效（ACTIVE），是因为它们一经录入就视为有效单据，不需要"草稿→正式"的二次确认动作。
2. **与参考项目对齐**：参考项目（linaagi）DyeingInstruction 虽无显式草稿态，但其审批流的 draft/submitted 语义本质上就是"草稿→正式"。MVP 不接审批流，用 DRAFT 态承载"未正式下达"的语义，为后续接审批流留了字段口子。
3. **数据安全**：草稿态可编辑可删除，避免用户误操作创建的半成品单据污染"已下达"数据；正式下达后锁定，防止已发给染厂的指令被篡改。

## 考虑过的备选

- **三态 ACTIVE/COMPLETED/CANCELLED（与合同/胚布单完全一致）**：全系统状态枚举统一，认知成本低。但染色单的"草稿反复修改"需求无法表达——新建即 ACTIVE 意味着一保存就锁定，用户无法在确认前修改。若不加草稿态，用户只能通过"创建→删除→重建"来调整，体验差。
- **用字段 `isDraft` 布尔标记而非状态枚举值**：可复用 ACTIVE/COMPLETED/CANCELLED 三态，加一个独立布尔字段区分草稿。但状态语义被拆成两个字段（status + isDraft），查询"所有草稿"需要 `isDraft = true` 而非 `status = 'DRAFT'`，不直观；且状态流转约束需要同时判断两个字段，逻辑更碎。用独立的 DRAFT 枚举值更直接。

## 后果

- 全系统出现两套状态枚举（三态与四态），GLOSSARY.md 已显式记录该刻意不一致，未来工程师看到染色单有 DRAFT 而其他单据没有，**不应**将其"修正"为三态。
- 前端编辑页需根据状态切换按钮显示与只读模式：DRAFT 显示"保存/下达/删除"，ACTIVE 显示"完成/取消"，COMPLETED/CANCELLED 全部只读。
- 后端 `update` / `delete` 接口需校验状态，非 DRAFT 拒绝操作并返回 `1_030_007_001`（状态不允许该操作）。
