# 05: 联系人 EXISTS 继承 + 四入口一致性 + 客户批量删除强校验

**What to build:** 客户联系人列表/分页在现有 wrapper 上追加裸 EXISTS 子查询（JSqlParser 给子查询中 `c` 表自动注入数据权限与租户条件；contact 表未注册不被改写），实时继承客户归属。联系人 detail/update/delete/create 先 `customerMapper.selectById(customerId)`（自动受规则过滤）→ 不可见统一抛 `CUSTOMER_CONTACT_NOT_EXISTS`（1_030_004_001），客户真不存在与存在但不可见对外同一回答，杜绝逐 id 探测。客户批量删 `deleteCustomerList` 改为逐个 id 可见性校验，任一不可见整体抛 `CUSTOMER_NOT_EXISTS`、零部分成功。

**Blocked by:** 02

**Status:** completed

- [x] §5.3 联系人列表/分页：`CustomerContactMapper.selectPage` 在现有 wrapper 上追加裸 EXISTS（无外部参数、注释防注入）：`wrapper.exists("SELECT 1 FROM lunaerp_baseinfo_customer c WHERE c.id = lunaerp_baseinfo_customer_contact.customer_id")`。JSqlParser 解析子查询时对 `c`（注册表）自动追加数据权限与租户条件；contact 表未注册不被改写
- [x] §5.3 联系人详情/update/delete/新建：service 先取联系人（或直接用入参 customerId）→ `customerMapper.selectById(customerId)` 可见性校验（自动受规则过滤）→ 为空统一抛 `CUSTOMER_CONTACT_NOT_EXISTS`（1_030_004_001）。包括"客户真不存在"与"客户存在但不可见"两种情况，对外同一回答
- [x] §5.3 导出契约：当前 5 表均无 export 端点；未来导出必须复用同一 selectPage/过滤 mapper，不得另开无过滤查询（与 16 号 §6.2"四入口一致"对齐）。本票只在 spec/Comments 落契，不实现导出
- [x] §5.4 客户批量删除 `deleteCustomerList`：现状 `selectBatchIds` 自动过滤会静默少删；改为逐个 id 可见性校验，任一不可见即整体抛 `CUSTOMER_NOT_EXISTS`（1_030_004_000），禁止部分成功
- [x] §5.4 单条 get/update/delete/状态动作（合同 update/delete、发货 update/audit/cancel/update-sign/delete、客户与联系人 update/delete）维持现有"先 selectById 校验存在再写"模式——受限用户在校验步即得 NOT_EXISTS，MP 对 UPDATE/DELETE 的改写为第二道保险
- [x] §9.2 用例 2（四入口一致性）通过：列表过滤、直连 id 详情、update/delete/状态动作、（未来）导出——不可见行统一返回该表 NOT_EXISTS 文案；写操作断言 DB 零变更
- [x] §9.2 用例 3（联系人继承）通过：a 只见本人客户名下联系人；直连他人联系人 id（列表/详情/改/删/新建五种调法）一律 `CUSTOMER_CONTACT_NOT_EXISTS`
- [x] §9.2 用例 4（批量删除）通过：ids 中混入不可见 id → 整体失败且零删除
