# 15: 录检行 PIECE 保存 + 胚布录检前端

**What to build:** 落成胚布 PIECE 录检行保存端点与胚布录检前端。POST `/lunaerp/h5/workshop/inspection/item/save` PIECE 路径（`@PreAuthorize lunaerp:workshop:h5:inspection`；`@LogRecord`）。规则：胚布 PIECE（来自 ticket-scan）：`weavingTicketId` 必填带回，pieceNo 缺省=布票完整 code（可改，单内 `(vatNo,pieceNo)` 唯一性仍由既有 service 校验），vatNo 可空（依赖 05 的条件校验放宽）；机台号/产品编码品名/颜色快照**由服务端从布票→织造单/胚布单补齐**，不信任也不采用客户端快照；公共字段：grade（字典 2232）、measuredWeight、lengthM、remarks、photos（≤6）；第一卷保存时 DRAFT→ACTIVE 的既有便利逻辑不变；改录仅在检验单 DRAFT/ACTIVE 允许；布票 INSPECTED 回写、(未来)入库链路零改动。无删除端点（删除留 PC）。前端 `src/views/h5/inspection/order.vue` 胚布扫码路径（扫码→自动交接→录等级/重量/米数/照片；重扫同票定位改录原行）。可 demo：扫完工票→自动建交接单→录等级/重量/米数/照片；重扫同票改录原行。

**Blocked by:** 14（ticket-scan 自动交接已就位）, 04（录检明细 `photos` DO/VO 字段已落）, 08（PhotoUploader 拍照组件已就位）

**Status:** closed

- [x] POST `/lunaerp/h5/workshop/inspection/item/save` PIECE 路径
- [x] 胚布 PIECE：`weavingTicketId` 必填带回，pieceNo 缺省=布票完整 code 可改，vatNo 可空
- [x] 机台号/产品编码品名/颜色快照由服务端从布票→织造单/胚布单补齐，不信任也不采用客户端快照
- [x] 公共字段：grade（字典 2232）、measuredWeight、lengthM、remarks、photos（≤6）
- [x] 单内 `(vatNo,pieceNo)` 唯一性仍由既有 service 校验
- [x] 第一卷保存时 DRAFT→ACTIVE 的既有便利逻辑不变
- [x] 改录仅在检验单 DRAFT/ACTIVE 允许；布票 INSPECTED 回写、(未来)入库链路零改动
- [x] 无删除端点（删除留 PC）
- [x] `@PreAuthorize lunaerp:workshop:h5:inspection`；`@LogRecord` 留痕
- [x] 前端 `src/views/h5/inspection/order.vue` 胚布扫码路径（扫码→自动交接→录等级/重量/米数/照片；重扫同票定位改录原行）
- [x] 照片先传文件拿 URL 再随表单提交；表单 localStorage 草稿按 id 分键提交成功清除
- [x] 测试：胚布扫码自动交接幂等（与 14 协同）；缸号条件校验 GREIGE+PIECE 空 vatNo 可存；照片守卫 7 张→039-008；照片随录检更新落库；PC 检验单与布票 INSPECTED 可见
- [x] demo：扫完工票→自动建交接单→录等级/重量/米数/照片；重扫同票改录原行
