# 04: 生产进度独立菜单页——列表 + 单合同抽屉

**What to build:** "生产管理 → 生产进度"页面上线，MVP 闭环完成：跨合同列表展示 spec 定义的列（合同号/客户/业务员/交期/状态/各环节关键量/未发量/预警数），默认按交期升序，支持关键字、交期范围、状态（默认排除已取消可切换）、仅看有预警筛选；点击行打开单合同抽屉——头部合同信息 + 预警标签、六环节分区卡片（进度对照）、发染记录列表与缸状态下钻（复用 02 的发染 API 封装与 03 的 get 接口）。含菜单/路由/生产进度权限 SQL。

**Blocked by:** 02: 发染记录双入口前端；03: 生产进度查询服务——page + get 聚合口径

**Status:** ready-for-human

**参考**：本目录上级 spec.md（列表列/筛选/抽屉结构定义）；ADR-0034 §1（消费方）

- [x] 菜单出现在"生产管理"目录下，权限控制生效
- [x] 列表列与筛选按 spec 定义齐全，默认排序与默认排除已取消生效
- [x] 抽屉六环节数量与各单据侧展示一致（口径唯一验证）
- [x] 预警在列表行与抽屉头部均可见
- [x] 抽屉内发染记录/缸状态下钻可用且数据一致
- [ ] 按设计文档"验证方式"清单完成手工验收（含 M 单位不进合计、删除发染后进度即时反映）

---

## 实现说明（供人工验收对照）

**落地文件**

| 文件 | 作用 |
|------|------|
| `luna-ui/src/api/lunaerp-elmui/production/production-progress/index.ts` | 03 的 page/get 接口封装 + VO 类型 + `NEAR_DELIVERY_DAYS` 阈值（镜像后端，供列表与抽屉共用） |
| `luna-ui/src/views/lunaerp-elmui/production/production-progress/index.vue` | 跨合同列表页（组件名 `LunaerpElmuiProductionProgress`） |
| `.../production-progress/utils.ts` | 列表与抽屉共用的展示口径（`formatKg` / 距交期天数 / 交期着色 / 预警配色） |
| `.../production-progress/components/ProgressDetailDrawer.vue` | 单合同全链路抽屉 |
| `luna-srv/sql/postgresql/lunaerp-production-progress.sql` | 菜单 10410 挂 10200「生产管理」，权限 `lunaerp:production-progress:query`；无表结构变更 |

**验收要点与代码位置**

- 菜单/路由：菜单记录 `component = lunaerp-elmui/production/production-progress/index`，经 `routerHelper.ts` 的 `import.meta.glob('../views/**')` 匹配到页面；`component_name` 与 `defineOptions({ name })` 同值以支持 keep-alive。路由为动态菜单路由，`remaining.ts` 无需改动（本页用抽屉不用整页工作台）。
- 默认排序/默认排除已取消：排序在后端（交期升序、无交期排最后，已由 03 单测覆盖），前端不再二次排序；`includeCancelled` 缺省 `false`，且选定具体状态时开关禁用（后端此时以 status 等值筛选为准）。
- 口径唯一：抽屉六环节全部直渲 `get` 返回值，前端不做任何二次聚合；唯一的前端求和是发染记录合计，它与 `greige.sentToDyeingKg` 在后端同一个循环里同源产出（`ProductionProgressQueryServiceImpl#aggregateDispatch`），页面上直接把两者并列展示以便对账。
- 六环节 = 胚布 / 织造（自织） / 染色 / 检验 / 入库 / 出货。织造环节在 `selfWeaving = false` 时显示「外发胚布无自织进度」而非假 0%。
- M 单位（按后端实际口径逐项核对过）：`demandKg` 仅在 `contract.unit = KG` 时累加，`unshippedKg = demandKg - shippedKg`，因此 M 合同这两项在 KG 口径下无定义（0 与负数）——列表与抽屉均显示「—」；而胚布计划/已发染/入出库/已发货来自执行单据与 `ledger.unit = KG` 流水，是真实聚合值，原样展示不抹除（否则就是前端自搞第二口径）。以 `demandKg` 为分母的对照进度条在 M 合同不渲染，避免假 0%。列表合同号旁挂 `M` 标记带悬浮说明，抽屉头部给提示条。
- 只读边界：抽屉无新增/删除入口（本页只有 query 权限），发染记录的增删仍走 02 的胚布单抽屉与染色单页签。
- 展示口径不双写：`formatKg`、距交期天数、交期着色（逾期红/临期橙/无交期灰）、预警配色只存于 `utils.ts` 一份，列表行与抽屉头部共用——否则「口径唯一」会在最后一米被前端自己破坏。`NEAR_DELIVERY_DAYS` 放在 API 模块（属于后端契约镜像），utils 引用它。

**已完成的自动验证**

- `pnpm ts:check`：新增文件零错误（仓库存量错误未变动）
- `eslint` / `prettier --check` / `stylelint`：新增文件全部通过
- `pnpm build:local`：构建成功，产出 `production-progress` 与 `ProgressDetailDrawer` 独立 chunk
- `mvn test`（lunaerp 模块）：`ProductionProgressQueryServiceImplTest` 15 + `GreigeDispatchRecordServiceImplTest` 11 = 26 项全绿
- `/code-review` 双轴复查：Spec 轴 0 项发现；Standards 轴 1 项「疑似 Duplicated Code」（两文件重复 `formatKg`/`alertTagType`/交期计算），已抽出 `utils.ts` 修复后重跑全部校验

**待人工验收（最后一项未勾选）**

需要连库执行 `lunaerp-production-progress.sql` 后在真实环境走设计文档「验证方式」清单：菜单可见性与权限、六环节数值与各单据侧逐一对照、M 单位合同不进 KG 合计、删除发染记录后进度即时反映、双入口数据一致。SQL 文件头已注明「禁止在共享远程库直接执行」，故未由 agent 代跑。

