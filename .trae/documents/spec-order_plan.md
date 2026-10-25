# Spec 实现顺序索引（.scratch/_order.md）实现计划

## 调研结论

- `.scratch/` 下共 22 个目录，其中 **20 个含 `spec.md`**，纳入索引；另 2 个不含 spec，排除：
  - `lunaerp-commercialization/`：wayfinder 决策地图（`map.md`），不是 feature spec
  - `partial-update-column-wipe/`：单个修复工单，无 spec
- 完成状态以各 feature `issues/*.md` 中勾选框机械判定：
  - **已完成（10 个，`[ ]` 残留 = 0）**：product-info-align、greige-order-mvp、dyeing-order-mvp、stock-in-mvp、inspection-order-mvp、weaving-execution、inspection-confirmation-mvp、shipment-mvp、sample-analysis-mvp（共 9 个全清；第 10 个见下"边界情况"）
  - **进行中（7 个，有残留）**：customer-info-module（17）、supplier-info-module（8）、aggregate-differential-save（7）、sales-contract-mvp（3）、yarn-order-mvp（1）、production-progress（1）、progress-feedback-mvp（1）
  - **待实现（4 个 draft spec，未拆工单）**：common-foundation、module-gateway、approval-engine（均 W0）、export-docs（W1，待 grilling 定稿）
- 顺序依据：已完成部分按依赖脉络（基础信息 → 合同 → 生产链 → 库存/检验 → 进度叠加 → 发货 → 样品）；进行中的存量 MVP 排在商业化 W0 之前（先收尾再开新波次）；新 spec 按 wayfinder 地图波次 W0→W1 排。

## 变更文件

- **新建 `.scratch/_order.md`**：实现顺序索引 + 文件头维护约定
- **修改 `docs/agents/issue-tracker.md`**：在 Conventions 增加一条，并新增小节说明 `_order.md` 的维护规则，使 `to-spec` / `grill-with-docs` 等技能产生新 spec 时有章可循

## `.scratch/_order.md` 初始内容

```markdown
# Spec 实现顺序

> 维护约定：
> - 每次新 spec 落盘（.scratch/<feature>/spec.md）时，**按依赖关系插入**到正确位置并分配全局序号，不是追加到末尾。
> - 序号只表达实现顺序、可重排（插入/重排后重新连续编号）；feature 的稳定身份是目录名（slug），不是序号。
> - 勾选状态 = 该 feature 的 issues 是否全部完成（全部 [x] 打勾；仍有 [ ] 不打勾）。
> - 无 spec 的目录不入本索引（wayfinder 地图、纯修复工单等）。

- [ ] 01，customer-info-module，[customer-info-module/spec.md](customer-info-module/spec.md)
- [ ] 02，supplier-info-module，[supplier-info-module/spec.md](supplier-info-module/spec.md)
- [x] 03，product-info-align，[product-info-align/spec.md](product-info-align/spec.md)
- [ ] 04，sales-contract-mvp，[sales-contract-mvp/spec.md](sales-contract-mvp/spec.md)
- [ ] 05，yarn-order-mvp，[yarn-order-mvp/spec.md](yarn-order-mvp/spec.md)
- [x] 06，greige-order-mvp，[greige-order-mvp/spec.md](greige-order-mvp/spec.md)
- [x] 07，dyeing-order-mvp，[dyeing-order-mvp/spec.md](dyeing-order-mvp/spec.md)
- [x] 08，stock-in-mvp，[stock-in-mvp/spec.md](stock-in-mvp/spec.md)
- [x] 09，inspection-order-mvp，[inspection-order-mvp/spec.md](inspection-order-mvp/spec.md)
- [x] 10，weaving-execution，[weaving-execution/spec.md](weaving-execution/spec.md)
- [x] 11，inspection-confirmation-mvp，[inspection-confirmation-mvp/spec.md](inspection-confirmation-mvp/spec.md)
- [ ] 12，aggregate-differential-save，[aggregate-differential-save/spec.md](aggregate-differential-save/spec.md)
- [ ] 13，production-progress，[production-progress/spec.md](production-progress/spec.md)
- [ ] 14，progress-feedback-mvp，[progress-feedback-mvp/spec.md](progress-feedback-mvp/spec.md)
- [x] 15，shipment-mvp，[shipment-mvp/spec.md](shipment-mvp/spec.md)
- [x] 16，sample-analysis-mvp，[sample-analysis-mvp/spec.md](sample-analysis-mvp/spec.md)
- [ ] 17，common-foundation，[common-foundation/spec.md](common-foundation/spec.md)
- [ ] 18，module-gateway，[module-gateway/spec.md](module-gateway/spec.md)
- [ ] 19，approval-engine，[approval-engine/spec.md](approval-engine/spec.md)
- [ ] 20，export-docs，[export-docs/spec.md](export-docs/spec.md)
```

## issue-tracker.md 新增约定（措辞草案）

Conventions 列表增加一条：

> - `.scratch/_order.md` is the implementation-order index: one line per feature spec, globally sequenced by dependency. Update it whenever a new spec lands (insert by dependency, renumber, keep the checkbox in sync with issue completion).

并新增小节：

```markdown
## Implementation order (`.scratch/_order.md`)

- One line per feature that has a `spec.md`: `- [ ] NN，<feature-slug>，[<slug>/spec.md](<slug>/spec.md)`
- On creating a new spec, insert the line at the dependency-correct position and renumber sequentially. Numbers are ordering, not identity; the slug is identity.
- Check the box only when every issue under that feature is complete (`[x]`).
- Spec-less directories (wayfinder maps, one-off fix tickets) are not listed.
```

（正文按仓库惯例可用中文；最终与该文件现有英文风格保持一致，采用英文。）

## 实施步骤

1. 新建 `.scratch/_order.md`，写入文件头约定 + 20 行初始索引
2. 编辑 `docs/agents/issue-tracker.md`：Conventions 增条 + 新增"Implementation order"小节
3. 不改动任何 spec / issue 文件

## 边界情况（请审批时一并确认）

- **yarn-order-mvp（217 勾 / 1 未勾）、production-progress（25/1）、progress-feedback-mvp（60/1）**：按机械规则未打勾。若这 3 个实际已收尾，审批时告知，我直接打 `[x]`。
- 初始顺序中"已完成"项的先后按依赖脉络重建，并非严格按提交日期；如与你心中的历史顺序不符，可指出调整。

## 验证

- 20 行链接逐一解析存在（脚本校验，broken: none）
- 序号连续 01–20 无重复
- 每个含 spec.md 的目录恰好出现一次，不含 spec 的两个目录不出现

## 风险

- **勾选状态与实际偏差**：工单勾选框可能滞后于代码（commit 了但没勾）。缓解：本次机械统计并明确列出边界情况，由你确认；日常维护以 issues 勾选为准。
- **插入重排导致序号变动**：序号刻意不承担身份语义，引用某 feature 时用 slug 而非序号，已写入文件头约定。
