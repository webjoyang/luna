# 08: 测试 + GLOSSARY + ADR-0042 收口

**What to build:** 销售退货 Service 层全套单测与 finance listener 测试覆盖 10 类业务行为；业务术语回填 business GLOSSARY 与 shared 段位总表；ADR-0042（销售退货复用入库单作为库存台账唯一正数写入口）落地。

**Blocked by:** 07 (打印 / 回收站 / 脱敏 / 操作日志 + 详情页)

**Status:** closed

- [x] `SalesReturnServiceImplTest` 覆盖：制单取数（仅 COMPLETED 可发起、非 COMPLETED/他租户报 003、头快照正确、行/卷不属于头单报 004/007）
- [x] 保存校验：行缺原因、卷缺卷号、卷重量米数双空分别报错；金额服务端重算覆盖前端入参；行费卷量差异不拦截
- [x] 防超退：数量超可退报 005、金额超可退额报 006；APPROVING/APPROVED 占量、REJECTED/PENDING/CANCELLED 释放；模拟并发两单行锁后第二单超退失败
- [x] 审批 Handler：APPROVED 幂等建一张 DRAFT 入库单（inType=RETURN、source 回指、金额 0、明细全 RESTOCK 预生成）；重复事件不重建；REJECTED 不建单；handler 抛错审批事务回滚
- [x] 到货质检：未判定卷报 010；RESTOCK 卷缺行单位量报 011；部分判废重写入库单仅 RESTOCK 卷并审核，断言台账写入类型 STOCK_IN、placement 与卷一一对应；全废自动取消 DRAFT 入库单且退货单→RECEIVED；入库单被人工先审核/取消报 009；restock_qty/scrap_qty KG/M 双口径重算正确
- [x] 作废：DRAFT 作废不触入库；APPROVED 作废同事务取消 DRAFT 入库单且无库存流水；APPROVING 直接作废拒绝；RECEIVED/POSTED 作废报 002
- [x] 过账：RECEIVED→POSTED 条件更新防双过账；有 RESTOCK 卷但入库单非 COMPLETED 报 012；断言发布事件载荷（客户/币种/逐行原币金额）一次；全废单可正常过账
- [x] `SalesReturnLedgerListenerTest`：按原发货汇率调 recordArReturnReversal（category=AR_RETURN_REVERSAL、refType=SALES_RETURN、原币/汇率/本位额正确）；汇率缺失报 014；listener 抛错退货过账回滚
- [x] 租户与越权：写接口租户隔离断言；横向越权（他租户单据访问报不存在）、纵向越权（无 :receive/:post 权限 403）
- [x] 分页性能：万级退货单常规分页 P95<1s 口径（EXPLAIN 验证占量聚合走 `(tenant_id, shipment_item_id)` 索引）
- [x] 回收站/打印：handler 仅收 DRAFT、级联复活；print-data VO 不脱敏而列表 VO 脱敏
- [x] GLOSSARY：回填销售退货单/退货行/退货卷/退货单业务状态/质检判定（可二次入库·废品）/可退量 到 `docs/contexts/business/GLOSSARY.md`；字典 2279–2281 与错误码 1-030-037 同步登记 business GLOSSARY 与 shared 段位总表
- [x] ADR-0042：`docs/adr/0042-sales-return-reuse-stock-in.md` 落地（不新增 RETURN_IN 台账类型，复用入库单为唯一正数写入口）
