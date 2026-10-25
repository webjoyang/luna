# 07: 验收库重建脚本 + JUnit 四链冒烟基座

**What to build:** 四条业务链冒烟共用的地基：一条命令把验收库从零重建（down -v → up → install → seed）；一个 JUnit 集成测试基座，造数全程走 API、按链路分四个顺序测试类、链路内共享单据上下文、链路间可各自重建库。脚本保持线性可读，不写自清理逻辑。

**Blocked by:** 04

**Status:** ready-for-agent

- [ ] 重建脚本：`compose down -v` → `up` → install 编号总入口 → 默认 seed；每轮全链冒烟可一键重建；脚本不写自清理逻辑
- [ ] JUnit 集成测试基座沿用 luna-srv 现有测试基座与 Spring 测试库；包内按链路分四个顺序测试类（主链/辅链/采购链/资金链）
- [ ] 造数走 API（禁止 DB 直插造数，断言读库除外）；链路内用例严格顺序执行、共享单据上下文；链路间允许各自重建库
- [ ] 通用断言助手：HTTP 状态/业务码 + 单据 DB 状态双断言；库存台账断言（txn_type 与符号：STOCK_IN(+)/STOCK_IN_REVERSAL(-)/SHIPMENT_OUT(-)/YARN_OUT(-)）；经营分录断言（category/金额/汇率/本位额/ref 回指）
- [ ] 冒烟入口与重建脚本纳入 `luna-srv/script/acceptance/smoke/`，命令一键执行，失败时输出停在第几步与实际/期望值
