# 03: 布票 SQL 加列 + DO/VO（exception_photos）

**What to build:** 落盘布票异常照片的存储与字段链路。ALTER `lunaerp_production_weaving_ticket` ADD `exception_photos` jsonb + COMMENT（"异常现场照片 URL JSON 数组（车间 H5 异常登记上传，≤6 张，infra 文件服务）"）；`WeavingTicketDO` 加 `List<String> exceptionPhotos`（MyBatis JacksonTypeHandler，同 `samplePhotos` 先例）；`WeavingTicketExceptionReqVO` 加同名字段（PC/Web 与 H5 共用异常端点的写 VO，H5 走自己的 VO 组装后调同一 service，service 落库时写入）；`WeavingTicketRespVO` 输出该字段供 PC 只读展示。语义：照片挂在当前异常事实上（与 exceptionType/exceptionNote 同生共死），再次异常登记覆盖；正常开工/完工不带照片。本票不交付用户行为，是 12 异常登记端点的前置。

**Blocked by:** 01（错误码已落）

**Status:** ready-for-agent

- [ ] ALTER `lunaerp_production_weaving_ticket` ADD COLUMN IF NOT EXISTS `exception_photos` jsonb
- [ ] COMMENT ON COLUMN `lunaerp_production_weaving_ticket.exception_photos` IS '异常现场照片 URL JSON 数组（车间 H5 异常登记上传，≤6 张，infra 文件服务）'
- [ ] `WeavingTicketDO` 加 `List<String> exceptionPhotos`，使用 MyBatis JacksonTypeHandler（同 `samplePhotos` 先例，typeHandler 配置一致）
- [ ] `WeavingTicketExceptionReqVO` 加 `private List<String> exceptionPhotos`（可空，H5 端点组装后传给同一 service）
- [ ] `WeavingTicketRespVO` 输出 `exceptionPhotos` 字段供 PC 只读展示（不提供 PC 上传入口）
- [ ] 语义文档：照片挂在当前异常事实上（与 exceptionType/exceptionNote 同生共死），再次异常登记覆盖；正常开工/完工不带照片
- [ ] 脚本幂等可重复执行
