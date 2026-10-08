# 10: 术语入 GLOSSARY + 全链路冒烟 + 性能 P95

**What to build:** 收口验收票。业务术语归入 `docs/contexts/business/GLOSSARY.md`（出口单证套/单证套号/定稿/卖方出口配置/唛头/resync 六个术语，遵循 domain-modeling skill 词汇口径）。全链路冒烟覆盖验收清单全流程：维护卖方资料 → 从外销合同/发货单生成 → 补录 → 重新同步 → 定稿/回退 → 打印三单（PI/CI/PL）。性能验证：常规列表查询 P95<1s（万级数据量级下）。本票不新增业务代码，只补 GLOSSARY 文档 + 跑全链路冒烟 + 性能压测，作为本模块 done-done gate。

**Blocked by:** 09（前端交付完成后才能跑全链路冒烟）

**Status:** ready-for-agent

- [ ] 术语入 `docs/contexts/business/GLOSSARY.md`：出口单证套/单证套号/定稿/卖方出口配置/唛头/resync（遵循 domain-modeling 词汇口径）
- [ ] 全链路冒烟：维护卖方资料 → 从外销合同/发货单生成 → 补录 → 重新同步 → 定稿/回退 → 打印三单（PI/CI/PL）全流程绿
- [ ] 性能：常规列表查询 P95<1s（万级数据量级）
- [ ] 验收清单逐项核对（对照 spec 验收标准统一 DoD 映射）
