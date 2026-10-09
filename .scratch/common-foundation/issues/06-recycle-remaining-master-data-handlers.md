# 06: 回收站剩余主数据 handlers（供应商/产品/纱线材质/纱线规格）

**What to build:** 回收站新增 4 个主数据 tab，可恢复各自删除的主数据为 INACTIVE；纱线规格恢复时若其材质仍在回收站则被阻断并提示先恢复材质。在 05 号 SPI 框架基础上落地 SUPPLIER/PRODUCT/YARN_TYPE/YARN_SPEC 四个 handler，沿用 05 号的手写 XML pattern、恢复规则（INACTIVE）、引用阻断、幂等、`@DataPermission(enable=false)`、`@LogRecord`。主数据之间存在父子依赖（纱线规格→纱线材质）同理先父后子。前端 4 个新 tab 接入。

**Blocked by:** 05

**Status:** closed

- [x] SUPPLIER handler（含供应商联系人级联复活）
- [x] PRODUCT handler（含标签等子表级联复活）
- [x] YARN_TYPE handler（有规格引用时阻断删除/提示）
- [x] YARN_SPEC handler（恢复时若其 YARN_TYPE 仍在回收站报 002，先父后子）
- [x] 4 个 handler 均走手写 XML（显式 `deleted=1 AND tenant_id`）、恢复 INACTIVE、引用阻断、幂等、`@DataPermission(enable=false)`、`@LogRecord`
- [x] 前端回收站页新增 4 个 tab（bizType 维护在前端路由表）
- [x] 测试：各 handler 恢复/彻底删除/引用守卫；纱线规格→纱线材质父子依赖阻断；并发条件更新；租户隔离；横向越权
