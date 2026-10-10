# 01: 打色指示单地基：空列表可访问

**What to build:** 用户从"生产管理 → 打色指示单"菜单进入，看到一个可正常分页、可按单号/客户/产品（编码或名称模糊）/客户色号/状态/单据日期区间查询的空列表（数据为空时显示空状态，不报错）。整条路径从数据库、后端到前端菜单全部打通，作为后续票据的地基。

**Blocked by:** None (can start immediately)

**Status:** completed

- [x] 主表 `lunaerp_production_color_instruction` + 颜色行子表 `lunaerp_production_color_instruction_color` 已建（含主键序列、`tenant_id`+create/update/deleted 框架列、`UNIQUE (tenant_id, instruction_no)` 全量唯一含已删行仿 ADR-0006、`(tenant_id, customer_id)`/`(tenant_id, status)`/`(tenant_id, instruction_date)`/颜色行 `(tenant_id, instruction_id)`/`(tenant_id, status)` 索引、全列注释）
- [x] "生产管理"目录（10200）下注册菜单 10230 打色指示单（type=2，sort=0，时序在胚布单之前）+ 按钮 10231-10235（创建/修改/删除/确认/打印）；脚本事务内先 DELETE 再 INSERT 可重复执行
- [x] 字典初始化：`lunaerp_color_light_source`（2255，data 3736-3741：D65/TL84/CWF/A/UV/TL83）、`lunaerp_color_line_status`（2256，data 3742-3744：PENDING/APPROVED/REJECTED）、`lunaerp_color_instruction_status`（2257，data 3745-3749：PENDING/IN_PROGRESS/CONFIRMED/IN_PRODUCTION/VOID）
- [x] 错误码段位 `1-030-032`（000-007 + 行 100）入 shared GLOSSARY 段位总表与 production GLOSSARY（打色指示单/客户色号/灯源/色卡参考/打色轮次）
- [x] 后端四层骨架 `production.colorinstruction`（DO/Mapper/Service 接口与 Impl 骨架/Controller）就绪；GET `/lunaerp/production/color-instruction/page` 可用，支持 instructionNo/customerId/productCodeOrName 模糊/customerColorNo/status/instructionDateRange 过滤，默认 instructionDate DESC
- [x] 列表查询不 N+1：颜色行摘要（色号 chips + 轮次数）走详情子查询或聚合 SQL，列表主查询不联表
- [x] 前端列表页 `lunaerp-elmui/production/color-instruction/index.vue` 可访问：搜索区（上述筛选）、列结构（单号/客户/产品编号+名称/颜色行摘要 chips/状态 DictTag/单据日期/要求完成日/确认类型/操作占位）、分页组件、空态
- [x] 前端隐藏编辑路由 `edit/:id?` 注册（remaining.ts 或等价），本期页面可为占位
- [x] API TS `src/api/lunaerp-elmui/production/color-instruction/index.ts` 建好骨架（page/get 占位，create/update/delete 等留给后续票据）
- [x] 测试覆盖：分页条件（各筛选字段）正确透传到查询构造；列表查询不 N+1（SQL 执行计划或测试断言）
- [x] 全部查询带 tenant_id；他租户 id 访问 404/越权拦截可验
