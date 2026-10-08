# 09: 报价单修订版本端到端（revise / 版本历史 / 前端版本抽屉）

**What to build:** 用户能对最新 CONFIRMED 报价单「修订」：同事务把旧版条件更新为 SUPERSEDED（只读终态）、插入 `version_no+1` 的 DRAFT（头行复制、有效期重置为今天起/至空、成本快照保留并标记「复制自 vN，可重新取价」）；同号草稿唯一性由部分唯一索引兜底（冲突报 `1_030_034_104` 提示「该报价号已存在草稿版本，请在草稿上继续编辑」）；只有「同号最大版本且 = CONFIRMED」可修订（`SELECT FOR UPDATE` 同号行或唯一索引串行化，非最新版报 `1_030_034_103`）；DRAFT 新版 confirm 后成为新最新版；DRAFT 允许删除（进回收站；删除后该号止于 SUPERSEDED，如需继续报价开新号，刻意不做「复活」旧版）。页面把编号展示为 `QT20260001-v2`、可查看同号全部历史版本（只读抽屉，按 `version_no` 倒序）；已修订/已取消版本不可编辑不可删除。

**Blocked by:** 08（报价单骨架端到端）

**Status:** ready-for-agent

- [ ] Controller 加 `POST /revise?id=`（返新版本草稿 id）；权限复用 update
- [ ] revise 守卫：仅「同号最大版本且 = CONFIRMED」可操作（`SELECT FOR UPDATE` 同号行或唯一索引串行化）；非最新版报 `1_030_034_103`
- [ ] revise 同事务：旧版条件更新为 SUPERSEDED、插入 `version_no+1` 的 DRAFT（头行复制、有效期重置为今天起/至空、成本快照保留并标记「复制自 vN」）
- [ ] 同号草稿冲突由部分唯一索引兜底，转 `1_030_034_104`
- [ ] DRAFT 新版删除后该号止于 SUPERSEDED，不可「复活」旧版
- [ ] SUPERSEDED/CANCELLED 版本不可编辑不可删除（API 层守卫报 `1_030_034_102`）
- [ ] 前端列表/详情把编号展示为 `QT20260001-v2`；详情页加「同号版本历史」抽屉（按 `version_no` 倒序，只读）；修订/取消/删除按钮按状态版本权限启停
- [ ] `QuotationServiceImplTest` 单测：revise 仅最新 CONFIRMED 可操作、旧版 SUPERSEDED、同号草稿冲突 104、DRAFT 删除后该号止于 SUPERSEDED、SUPERSEDED/CANCELLED 不可改删
- [ ] 验收：v1 确认 → 修订出 v2 草稿 → 编辑确认 → 同号版本抽屉看到 v1（已修订只读）+ v2（已确认）；再修订出 v3；旧版不再可修订
