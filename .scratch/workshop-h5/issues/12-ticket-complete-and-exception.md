# 12: 完工（幂等友好）+ 异常登记（照片）+ 票卡完工/异常表单

**What to build:** 落成布票完工与异常登记端点及票卡对应表单。POST `/lunaerp/h5/workshop/ticket/complete`（`{code}`；调 `completeTicket`；**幂等友好**：票已 COMPLETED/INSPECTED/STOCKED → 返回成功+`alreadyCompleted=true` 不报错，供弱网重试；非 STARTED/已完成态抛既有 015-111；`@PreAuthorize lunaerp:workshop:h5:report`；`@LogRecord`）。POST `/lunaerp/h5/workshop/ticket/exception`（`{code, exceptionType, exceptionNote?, exceptionPhotos?[]}`；调 `exceptionTicket`，照片 ≤6 张仅图片 URL 超限 039-008；异常类型走既有字典校验 015-113；写入 `exception_photos` jsonb 与 exceptionType/exceptionNote 同生共死再次异常覆盖；`@LogRecord`）。前端 `src/views/h5/ticket.vue` 票卡完工按钮（重复完工 toast 提示为成功）+ 异常登记表单（类型+说明+≤6 张照片消费 08 的 PhotoUploader，照片先传文件拿 URL 再随表单提交）。可 demo：扫票→异常（类型+说明+3 张照片）→完工；重复完工提示为成功。

**Blocked by:** 11（票卡开工已就位，复用票卡页面）, 03（`exception_photos` DO/VO 字段已落）, 08（PhotoUploader 拍照组件已就位）

**Status:** closed

- [x] POST `/lunaerp/h5/workshop/ticket/complete`（`{code}`；调 `completeTicket`；幂等友好 `alreadyCompleted=true`；非 STARTED/已完成态抛 015-111；`@LogRecord`）
- [x] POST `/lunaerp/h5/workshop/ticket/exception`（`{code, exceptionType, exceptionNote?, exceptionPhotos?[]}`；调 `exceptionTicket`；照片 ≤6 张超限 039-008；异常类型字典 015-113；写入 `exception_photos` jsonb；`@LogRecord`）
- [x] 语义：照片挂在当前异常事实上（与 exceptionType/exceptionNote 同生共死），再次异常登记覆盖；正常开工/完工不带照片
- [x] `@PreAuthorize lunaerp:workshop:h5:report`
- [x] 前端 `src/views/h5/ticket.vue` 票卡完工按钮（重复完工 toast 提示为成功）+ 异常登记表单（类型+说明+≤6 张照片消费 PhotoUploader）
- [x] 照片先传文件拿 URL 再随表单提交（multipart 携带同一 token）
- [x] 弱网交互：完工/开工遇"已在目标态"响应按成功处理（对应幂等友好）；提交按钮防连点；表单 localStorage 草稿按 code 分键提交成功清除
- [x] 测试：完工幂等友好票已 COMPLETED 再调 complete → 成功 alreadyCompleted；VOID 票各动作 → 既有守卫码；照片守卫 7 张/非图片 URL → 039-008；照片随异常覆盖落库
- [x] demo：扫票→异常（类型+说明+3 张照片）→完工；重复完工提示为成功
