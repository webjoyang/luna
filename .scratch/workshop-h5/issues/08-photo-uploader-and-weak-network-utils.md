# 08: 通用拍照压缩 + 上传 + 弱网交互通用模式

**What to build:** 落成 H5 全场景复用的拍照上传与弱网交互工具集。`src/views/h5/components/PhotoUploader.vue`（`<van-uploader>` 或 `<input type=file accept="image/*" capture="environment">`，多张 ≤6；统一压缩工具 canvas 长边 ≤1920、JPEG ≤2MB 后 `POST /admin-api/infra/file/upload` multipart 携带同一 token，拿 URL 再随业务表单提交；上传失败可单独重传，不阻塞表单其余字段）；`src/utils/h5-draft.ts`（表单内容实时存 localStorage 草稿按 code/id 分键，提交成功后清除）；`src/utils/h5-submit.ts`（提交按钮提交中禁用防连点；业务错误与网络错误分色提示，网络错误文案"网络异常，单据未提交，可点重试"，不暗示已成功）。可 demo：拍照压缩上传拿 URL / 草稿存取 / 防连点与分色提示。

**Blocked by:** 06（H5Layout 已就位，组件挂在 H5 子树）

**Status:** ready-for-agent

- [ ] `src/views/h5/components/PhotoUploader.vue`（van-uploader 或 input file `capture=environment`，多张 ≤6）
- [ ] 统一压缩工具：canvas 长边 ≤1920、JPEG ≤2MB
- [ ] POST `/admin-api/infra/file/upload` multipart 携带同一 token，拿 URL 再随业务表单提交
- [ ] 上传失败可单独重传，不阻塞表单其余字段
- [ ] `src/utils/h5-draft.ts`（表单内容实时存 localStorage 草稿按 code/id 分键，提交成功后清除）
- [ ] `src/utils/h5-submit.ts`（提交按钮提交中禁用防连点；业务错误与网络错误分色提示，网络错误文案"网络异常，单据未提交，可点重试"）
- [ ] 不做后台同步、冲突合并、离线扫码队列（弱网可重试、断网不可用）
- [ ] demo：拍照压缩上传拿 URL / 草稿存取 / 防连点与分色提示
