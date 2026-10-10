# 06: 重新同步（resync-from-source）

**What to build:** 落地 `PUT /resync-from-source?id=`，让用户在源单据（合同/发货单）更新后把变化补充进已存在的草稿单证套。规则（grilling Q6 定稿）：仅 DRAFT 且 source_type 非 MANUAL 可用（MANUAL 来源拒绝 EXPORT_DOC_SOURCE_REQUIRED，非 DRAFT 拒绝 EXPORT_DOC_STATUS_NOT_ALLOW）；**只填当前为空的字段、永不覆盖已编辑值**；优先级 发货单 > 合同 > 卖方配置；明细行按来源行匹配补空（空字段补、已填不动）。三层各有"只填空不覆盖"用例：共享抬头层、单据 meta 层、明细行层。前端工作台放"重新同步"按钮 + 二次确认提示"仅补充空白内容，不覆盖已填内容"（前端入口待 09 接，本票只交付后端端点）。本票复用 05 的字段映射逻辑。单测覆盖抬头/meta/行三层只填空不覆盖 + MANUAL 拒绝 + 非 DRAFT 拒绝。

**Blocked by:** 05（复用 from-contract/from-shipment 的映射逻辑）

**Status:** ready-for-human

- [x] `PUT /resync-from-source?id=`：仅 DRAFT 且 source 非 MANUAL 可用
- [x] 只填当前为空字段、永不覆盖已编辑值；明细行按来源行匹配补空
- [x] 优先级 发货单 > 合同 > 卖方配置
- [x] MANUAL 来源拒绝 EXPORT_DOC_SOURCE_REQUIRED；非 DRAFT 拒绝 EXPORT_DOC_STATUS_NOT_ALLOW
- [x] 权限复用 `lunaerp:business:export-doc:update`
- [x] 单测：抬头/meta/行三层只填空不覆盖（每层各有用例）+ MANUAL 拒绝 + 非 DRAFT 拒绝
