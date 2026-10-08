# 08-release-production-workbench: 合同「下达生产」工作台

Type: grilling
Status: resolved
Blocked by: 06

## Question

设计合同→生产唯一入口并产出 spec（W2，Q7=A），grilling 时先调 Skill "grilling" 与 "domain-modeling"：

1. 一屏工作台：合同详情页内按合同行选择颜色/数量，一次生成打色单、胚布单、染色单、检验单（底层沿用现有四类独立单据，不建聚合实体 Q7=A）；
2. 拆单规则：颜色-缸次拆分、数量分配（含损耗系数？）、多合同行合并下达；
3. 关联回写：各单 ID 关联到合同行，合同行聚合查询生产进度（现有 production-progress 聚合的衔接）；
4. 幂等与防护：重复下达检测、部分下达/追加下达、合同变更后的处理；
5. 联动规则：生成后各单可否脱离合同独立修改；合同终止/取消时在途单据的联动（作废/继续）；
6. 吸收老系统教训：只此一个入口，不做第二套拆单路径（老系统三套入口并存的教训写进 spec 的非目标）。

产出：`.scratch/release-production/spec.md`（按统一 DoD）。

## Answer

grilling 一轮十二问全部按推荐锁定，spec 已落 `.scratch/release-production/spec.md`，ADR-0040 已落 `docs/adr/`。核心决策：

1. **入口**：合同详情页新增「生产」Tab 内嵌工作台（不弹窗/不跳页）；唯一入口，不做第二套拆单路径。
2. **生成四类单**：下达时创建 胚布单(ACTIVE) + 染色单(ACTIVE) + 胚布检验单(GREIGE/DRAFT)；打色单可选——选已有 CONFIRMED 则引用，新建 PENDING 则胚布/染色不引用（待确认后手动补填，零回写）。成品检验不在下达时生成，染色单 COMPLETED 后手动按钮生成（尊重检验单无自动触发边界）。
3. **合同行血缘**：`greige_order_color`、`dyeing_color` 加 `contract_line_id` + `contract_line_no`；检验明细不加（逐卷可跨多行）。
4. **数量/拆分/合并**：一行默认全量下达可改小；同合同同产品多行合并到同一张胚布/染色单；缸次一期不做（拆多张染色单代替）。
5. **损耗**：缺省 0（产品无单一默认损耗字段），逐行可覆盖。
6. **幂等/超下达**：合同行加 `released_qty`，下达时条件更新累加（`WHERE released_qty + delta <= quantity`），影响行数 0 即拦截并事务回滚；下游单据修改/删除不回写 released_qty（ADR-0040）。
7. **联动**：合同变更不影响已下达单据（只约束未来）；合同取消不自动联动在途单，提示+人工处理；生成的四类单完全独立可改可删。

错误码段位 `1-030-035`；不新建表，3 张表加列；权限 `lunaerp:production:release-production:query/:release`，按钮挂销售合同菜单 10114。
