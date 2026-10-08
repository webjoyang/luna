# 19: 默认仓库配置消费 + 弱网双击幂等验证 + requestId 持久化

**What to build:** 落成领纱发纱的弱网交互完善与端到端验证。前端默认仓库消费：取 `luna.workshop.default-warehouse-id`（存在且启用才预选，warehouseId 仍必填校验），未配置或未启用则发纱页不预选、强制手选，顶部黄条提示管理员配置（依赖 02 的 infra_config seed）。requestId 一次生成随表单 localStorage 持久化，重试沿用同一 id（换 requestId 视为新请求）。弱网双击只扣一次的端到端验证（与 18 的 @Idempotent 协同）。表单 localStorage 草稿（按 id 分键，提交成功清除，依赖 08 的 h5-draft 工具）。可 demo：默认仓库预选/黄条提示 + 弱网双击只扣一次 + 断网保留草稿恢复后重试。

**Blocked by:** 18（发纱端点与表单已就位）, 08（h5-draft/h5-submit 弱网工具已就位）

**Status:** ready-for-agent

- [ ] 前端默认仓库消费：取 `luna.workshop.default-warehouse-id`，存在且启用才预选，warehouseId 仍必填校验
- [ ] 未配置或未启用则发纱页不预选、强制手选，顶部黄条提示管理员配置
- [ ] requestId 一次生成随表单 localStorage 持久化，重试沿用同一 id（换 requestId 视为新请求）
- [ ] 弱网双击只扣一次的端到端验证（与 18 的 @Idempotent 协同）
- [ ] 表单 localStorage 草稿（按 id 分键，提交成功清除）
- [ ] 网络错误与业务错误分色提示，网络错误文案"网络异常，单据未提交，可点重试"，不暗示已成功
- [ ] 提交按钮提交中禁用防连点
- [ ] 测试：弱网双击只扣一次（与 18 协同）；断网保留草稿恢复后一键重试成功；默认仓库预选/黄条提示
- [ ] demo：默认仓库预选/黄条提示 + 弱网双击只扣一次 + 断网保留草稿恢复后重试
