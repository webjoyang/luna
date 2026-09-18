# 03: 建档联动增强（产品库回填、客户带出联系人/地址、取号预览）

**What to build:** 录合同的三处远程联动到位，减少重复录入：① 明细行输入产品编号时远程搜索产品库，选中后自动回填品名/成分/门幅/克重/单价；② 表单选中客户后自动带出联系人与地址；③ 编号输入框旁"获取编号"按钮可实时预览下一个自动编号。所有回填值仍允许手工修改，产品编号也允许直接手填产品库之外的值。

**Blocked by:** 01: 销售合同骨架端到端（主表 CRUD 跑通）；02: 商品明细聚合端到端（子表 + 服务端重算 + 级联删除）

**Status:** ready-for-human（代码实现与单测完成，运行时验收待人工执行，见 Comments）

- [x] 后端新增 `GET /admin-api/lunaerp/business/sales-contract/get-next-no`：复用 01/02 的取号入口返回当前租户当年下一个合同编号字符串；挂 query 权限 `lunaerp:business:sales-contract:query`；只读、不占号（多次调用在无新建时返回相同值）
- [x] 前端编号输入框旁增加"获取编号"按钮，点击调用 get-next-no 回填合同编号输入框；用户已手工输入时不自动覆盖（或仅在输入为空/占位时回填）
- [x] 前端客户远程选择增强：选中客户后调用客户详情，自动回填 clientContact（取主联系人姓名，可空则留空）与 clientAddress，两字段仍可手工修改；快照 clientName 仍按 01 票保存
- [x] 前端明细行产品编号改为远程搜索 el-select：调用现有产品分页/查询接口按编号或品名模糊搜索；下拉显示编号+品名；选中后回填 productName/composition/width/weight/unitPrice（字段名以产品库实际模型为准，缺失字段留空不报错）
- [x] 回填后所有明细字段保持可编辑；允许不选择下拉项直接手填任意 product_code（定制品种），手填不触发回填、不做存在性校验
- [x] 单元测试补一例：get-next-no 的 Service 行为与自动取号一致（当年无合同时返回 CH{year}0001；已有最大号时 +1；含已删除行占用的序号）
- [ ] 验收：新建合同点"获取编号"显示预期下一号，连点两次值不变且用该号保存成功后再次获取已顺延；选择客户后联系人/地址自动出现并可改、保存后以修改值落库；明细行远程搜产品选中后五个字段自动回填，改单价后行金额与汇总照常重算；手填一个产品库不存在的编号也能保存；编辑老合同回显不受远程回填影响（以已存快照为准，不被产品库当前值覆盖）

## Comments

- 2026-09-18（agent）：代码实现完成。后端 Service 新增 `getNextContractNo()`（直接复用 `generateContractNo()` 扫描，含已删除行，ADR-0004；不查重不写库），Controller 挂 `/get-next-no` + query 权限；`SalesContractServiceImplTest` 补 3 例（无合同→CH{year}0001 且不占号；普通最大号 +1；已删除最大号占用→+1），lunaerp 模块全量单测（52 例）通过。前端：编号输入框 append"获取编号"按钮（仅新建态显示，输入非空不覆盖，回填前后双重判空）；客户选中后 `Promise.allSettled` 并发拉客户详情与联系人分页（isPrimary DESC 取主联系人），任一接口失败不影响另一个回填，整体失败不阻断选择；明细产品编号改为 filterable+remote+allow-create 的 el-select，每行候选/加载状态以行对象为 key 存 reactive Map（不落库，删行同步清理），选中产品回填品名/成分/门幅/克重——产品库模型无销售单价字段（仅 widthCm/weightGsm），按"缺失字段留空"清空 unitPrice 由用户手填，避免切换产品残留旧单价；手填编码（值不在候选）不回填、不校验。编辑回显只 Object.assign、不触发 change/remote，快照不被覆盖。ESLint 零问题；新增代码 prettier 合规（存量格式问题不属本票）；vue-tsc 对改动文件零错误（存量错误属其他模块）。/code-review 两轴结论：Standards 无硬伤（仅 Middle Man/Data Clumps 等可接受判断项），Spec 无越界、无缺失（评审意见已落实：allSettled 独立回填、切换产品清空单价、补普通最大号用例、删行清 Map）。运行时验收待人工执行后勾选末项。
