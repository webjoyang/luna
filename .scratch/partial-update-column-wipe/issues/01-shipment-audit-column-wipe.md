# 01: 发货单审核/作废/红冲局部 DO 更新洗空 ALWAYS 列

**What to build:** 修复 `ShipmentServiceImpl` 中三处「只带 id + status 的局部 DO」调 `updateById` 的代码——它们会静默把 12 个声明了 `FieldStrategy.ALWAYS` 的列刷成 NULL。

## 问题机制

`ShipmentDO` 中有 12 个字段声明了 `@TableField(updateStrategy = FieldStrategy.ALWAYS)`：

- `salesContractId`、`contractNo`
- `shippingAddress`、`receiveAddress`
- `carrier`、`trackingNumber`
- `shippingDate`、`expectedDeliveryDate`
- `freightFee`
- `salespersonUserId`、`salespersonName`
- `remark`

`ALWAYS` 的语义是「无论字段值是否为 null，都进 UPDATE 的 SET 子句」。当前三处代码的写法是：

```java
ShipmentDO updateDO = new ShipmentDO();
updateDO.setId(existing.getId());
updateDO.setStatus("COMPLETED" /* 或 CANCELLED */);
shipmentMapper.updateById(updateDO);
```

`updateById` 拼出的 SQL 会是：

```sql
UPDATE lunaerp_shipment
SET status = 'COMPLETED',
    sales_contract_id = NULL, contract_no = NULL,
    shipping_address = NULL, receive_address = NULL,
    carrier = NULL, tracking_number = NULL,
    shipping_date = NULL, expected_delivery_date = NULL,
    freight_fee = NULL,
    salesperson_user_id = NULL, salesperson_name = NULL,
    remark = NULL,
    update_time = NOW(), updater = '...'
WHERE id = ?
```

每次审核/作废/红冲都会把业务字段静默清空，且不可逆。

## 受影响代码位置

`luna-srv/.../service/business/shipment/ShipmentServiceImpl.java`：

1. **L444-447 `auditShipment`**（DRAFT → COMPLETED）
2. **L486-489 `cancelShipment` 草稿作废分支**（DRAFT → CANCELLED）
3. **L497-500 `cancelShipment` 红冲分支**（COMPLETED → CANCELLED）

三处上方都已有 `ShipmentDO existing = validateShipmentExists(id)` 拿到完整 DO，可直接复用。

## 历史佐证

`commit 2bb6c66`（2026-09-28）已经把签收路径（`registerShipmentReceipt`）的 3 个 ALWAYS 列（`signedByName`/`signedAt`/`signRemark`）摘掉，并改用 `LambdaUpdateWrapper`。说明作者知道这个坑，但漏了上述 3 处。

## 测试盲区

`ShipmentServiceImplTest` 中 `auditShipment_*` 用例的 `ArgumentCaptor<ShipmentDO>` 只断言 `getStatus() == "COMPLETED"`，且 fixture 没给 `contractNo`/`carrier`/`freightFee` 赋值——即便代码改对也看不出差别。需要补强。

## 修复方案（选项 A：基于完整实体更新）

把三处局部 DO 替换为复用 `existing`：

```java
existing.setStatus(OrderStatusEnum.COMPLETED.getName());
// 或 CANCELLED
shipmentMapper.updateById(existing);
```

`existing` 是 `validateShipmentExists` 通过 `selectById` 查出的完整 DO，所有字段都是库里的真实值；只改 `status` 后调 `updateById`，ALWAYS 列会按原值重写一遍（值不变），其他 NOT_NULL 列也保持不变。零额外查询。

> 备选方案 B 是用 `LambdaUpdateWrapper` 只 `set(status)`，跟 2bb6c66 一致；但会绕过 `update_time` / `updater` 的自动填充，不推荐。

## 验收清单

- [x] `auditShipment`：改 `existing.setStatus(...); updateById(existing)`
- [x] `cancelShipment` 草稿作废分支：同上
- [x] `cancelShipment` 红冲分支：同上
- [x] `ShipmentServiceImplTest` 补强：在 `auditShipment_*` / `cancelShipment_*` 用例的 fixture 里给 `contractNo`、`carrier`、`freightFee` 赋值，断言 captor 抓到的 DO 这三个字段值保留（不被冲成 null）
- [x] 跑 `mvn -o test -pl lunasrv-module-lunaerp` 全绿

## 不在范围

- 不动 `updateShipment`（L223 走 `BeanUtils.toBean(updateReqVO, ShipmentDO.class)` 是完整 DO，无此问题）
- 不动签收路径（commit 2bb6c66 已修）
- 不改 `ShipmentDO` 的 ALWAYS 注解
- 不查库、不修历史数据（如已产生脏数据需另开工单处理）

**Blocked by:** 无

**Status:** done
