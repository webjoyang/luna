# 03: 质检确认列表只读切片（分页多条件检索 + 挡车工候选端点）

**What to build:** 用户从"生产管理 → 质检确认"菜单进入一个可正常打开、可检索、可分页的列表页：看到质检确认记录表格（检验单号/卷号/缸号/等级/产品名称/机台号/来源/责任类型/挡车工/状态/认定人/认定时间/创建时间），缺省只显示待确认+已认定的有效行，可显式筛选已撤销的冻结行；各筛选条件可用；状态与责任类型用字典标签渲染；挡车工筛选下拉由新端点供给（岗位未配置时为空列表不报错）。本票完成后页面框架与查询链路打通（此时尚无造数入口，数据可手工插库或由后续票填充）。

**Blocked by:** 01 (SQL 基建)、02 (PostApi getPostByCode)

**Status:** ready-for-human

- [x] 后端新增 `production.inspectionconfirmation` 子包（单包单实体）：DO（继承 TenantBaseDO，@KeySequence 指向 01 票序列）+ Mapper + PageReqVO/RespVO + Service/ServiceImpl + Controller
- [x] `GET /lunaerp/production/inspection-confirmation/page`：支持 pageNo/pageSize 与筛选 status（缺省 PENDING+CONFIRMED 两态；传 REVOKED 才查冻结行）、source、responsibility、workerUserId、inspectionOrderId、keyword（对 roll_no/vat_no/product_name/inspection_order_code 做 OR 模糊）、createTime 发现日区间；默认 create_time DESC；返回 PageResult
- [x] `GET /lunaerp/production/inspection-confirmation/worker-options`（:query 权限）：经 PostApi.getPostByCode("LUNA_WORKER") 取岗位 → AdminUserApi.getUserListByPostIds 返回 [{id,nickname}]；岗位不存在/无用户返回空数组不报错
- [x] 两端点均 `@PreAuthorize` 挂 `lunaerp:production:inspection-confirmation:query`
- [x] 前端新增视图 `views/lunaerp-elmui/production/inspection-confirmation/index.vue` 与 API 封装 `api/lunaerp-elmui/production/inspection-confirmation/index.ts`（含 TS 类型，字段名与后端 VO camelCase 对齐）
- [x] 搜索区：关键词 + 状态（缺省"待确认+已认定"）+ 责任类型 + 来源 + 挡车工（worker-options 下拉）+ 检验单远程选择 + 发现日区间；表格列与 spec 一致；状态/责任用 DictTag、来源用标签；Pagination 分页
- [x] 操作列先放占位（认定/撤销/查看链按钮在 06 票接入），本票页面只读
- [x] Service 层纯 Mockito 单测（InspectionConfirmationServiceImplTest 起步）：status 缺省两态过滤、显式 REVOKED 可查、各筛选参数正确透传查询构造、worker-options 命中/岗位缺失两路径
