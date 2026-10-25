# 04: 录检明细 SQL 加列 + DO/VO（photos）

**What to build:** 落盘录检明细疵点照片的存储与字段链路。ALTER `lunaerp_production_inspection_order_item` ADD `photos` jsonb + COMMENT（"疵点照片 URL JSON 数组（车间 H5 录检上传，≤6 张，infra 文件服务）"）；`InspectionOrderItemDO` 加 `List<String> photos`（MyBatis JacksonTypeHandler）；录检 Save/Resp VO 同步加同名字段；PC ItemsTab 只读缩略图（点击看大图），PC 不提供上传入口（一期照片仅现场产生）。本票不交付用户行为，是 15 录检 PIECE 保存与 16 ROLL 保存的前置。

**Blocked by:** 01（错误码已落）

**Status:** closed

- [x] ALTER `lunaerp_production_inspection_order_item` ADD COLUMN IF NOT EXISTS `photos` jsonb
- [x] COMMENT ON COLUMN `lunaerp_production_inspection_order_item.photos` IS '疵点照片 URL JSON 数组（车间 H5 录检上传，≤6 张，infra 文件服务）'
- [x] `InspectionOrderItemDO` 加 `List<String> photos`，使用 MyBatis JacksonTypeHandler
- [x] `InspectionOrderItemSaveReqVO` 加 `private List<String> photos`（可空）
- [x] `InspectionOrderItemRespVO` 加 `private List<String> photos`
- [x] PC ItemsTab 只读展示 `photos` 缩略图（点击看大图），PC 不提供上传入口（一期照片仅现场产生）
- [x] 脚本幂等可重复执行
