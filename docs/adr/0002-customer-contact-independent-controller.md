# 0002 — 客户联系人作为独立 Controller，不嵌套父级保存

## 状态

accepted

## 上下文与决定

`CustomerContact`（客户联系人）作为独立子资源，拥有自己的 `/lunaerp/baseinfo/customer-contact/*` Controller、自己的权限码 `lunaerp:baseinfo:customer-contact:*`，UI 工作台"联系人"Tab 内对单条联系人的新增/编辑/删除都走独立端点。`CustomerRespVO` 仍嵌套 `contacts` 列表用于展示，但写操作不走父级 `CustomerSaveReqVO` 整体替换。

## 理由

同一仓库已有"子资源嵌在父级保存载荷里整体替换"的先例：产品图片（`ProductImage`）通过 `ProductSaveReqVO.images` 整体替换，无独立 Controller。客户联系人本可以照搬，但本次刻意不走该模式：

1. **使用形态不同**：产品图片是"建档时一次配齐、偶尔替换"的附件性质；客户联系人是"业务运行中持续增删"的独立资源，单客户 10+ 联系人常见。整体替换意味着每次单条新增/编辑/删除都要前端重发整个 contacts 数组、后端事务先按 customerId 删旧再插新，性能与并发都差。
2. **权限粒度不同**：图片通常与产品同权限；联系人常需分权（销售可看不可改、主管可改）。独立权限码 `lunaerp:baseinfo:customer-contact:*` 才能落地分权。
3. **UI 同构**：联系人独立 Tab + 独立 Controller + 独立权限码，UI 与后端结构同构，前端代码更直。
4. **无历史包袱**：`ProductImageDO.productId` 用 `String` 是为了兼容旧 UUID；新 `CustomerContactDO.customerId` 直接用 `Long`，无历史包袱。

## 考虑过的备选

- **跟 product-image 一致（嵌套整体替换）**：复用现有模式，开发量小。但性能/权限/UX 三方面都不合适，理由如上。
- **混合模式（list/get 走父级嵌套返回，写操作走独立端点）**：实际就是本决定的展示侧行为（`CustomerRespVO.contacts` 嵌套展示，写走独立端点），不算真正备选。

## 后果

- 后端多一整套 CustomerContact 的 Controller/Service/Mapper/VO/权限码，工作量比"嵌套整体替换"大。
- 菜单 SQL 需为 customer-contact 注册独立权限节点（隐藏菜单，type=2 visible='0'）。
- 错误码段共用 `1-030-002-xxx`，customer 与 customer-contact 错误码在同一段内分号。
