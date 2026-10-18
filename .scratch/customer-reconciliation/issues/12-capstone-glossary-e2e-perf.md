# 12: Capstone — GLOSSARY/ADR 注记 + 端到端冒烟 + 性能

**What to build:** finance 域术语回填 finance GLOSSARY（对账单状态机/核销/预收余额/账龄五桶/收款单/发票申请/汇率主档）；错误码 020/021/023/026 回填 finance GLOSSARY 错误码节、shared 段位总表注记；ADR-0037 增补注记（10780 汇率菜单、020 段域公共表 DDL 归属、10740 三 Tab）；共享件收口清单 12 项与 11/12/13 号 spec 无冲突。端到端冒烟：建汇率→收款单（审批通过过账+自动分配核销）→预收转核销→生成对账单→发送→代客确认→打印（二维码）→账龄汇总下钻→发票申请→登记回填。性能：常规列表 P95<1s（万级），分录账聚合查询走索引（party+biz_date）。

**Blocked by:** 03 汇率主档 CRUD、05 收款单 — 红冲 + 预收转核销 + 并发核销专测、08 门户对账 4 端点 + 客户隔离专测、09 FIFO 账龄（汇总 + 下钻）、10 发票申请 — 创建/提交/登记/驳回、11 回收站前向 + 脱敏 + 消息

**Status:** closed

- [x] finance GLOSSARY 术语回填：对账单状态机（DRAFT/SENT/CONFIRMED/REJECTED/VOID）、核销（write-off）、预收余额、账龄五桶、收款单、发票申请、汇率主档
- [x] 错误码 020/021/023/026 回填 finance GLOSSARY 错误码节，shared GLOSSARY 段位总表注记
- [x] ADR-0037 增补注记：10780 汇率菜单（sort 8）、020 段域公共表（ledger_entry/account/exchange_rate）DDL 归属本票、10740 收付款三 Tab 页面归属
- [x] 共享件收口清单 12 项与 11/12/13 号 spec 无冲突复核
- [x] 端到端冒烟：建汇率 → 收款单（审批通过过账 + 自动分配核销）→ 预收转核销 → 生成对账单 → 发送 → 代客确认 → 打印（二维码）→ 账龄汇总下钻 → 发票申请 → 登记回填
- [x] 性能：常规列表 P95<1s（万级数据）；分录账聚合查询走索引 (tenant_id, party_type, party_id, biz_date)
- [x] 芋道权限码逐位断言 + 租户隔离与横向/纵向越权测试（含 print-data / portal / aging）
- [x] 取号测试：CS/RC/IA 格式、含软删行扫描、撞号重试、手填撞号报错
