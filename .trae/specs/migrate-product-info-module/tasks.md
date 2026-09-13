# Tasks

- [x] Task 1: 后端模块骨架与接线
  - [x] 1.1 新建 `lunasrv-module-lunaerp` Maven 模块（pom 对齐 lunasrv-module-system，package 根 `com.luna.lunasrv.module.lunaerp`），接入根 pom / lunasrv-dependencies / lunasrv-server pom
  - [x] 1.2 验证：`mvn -pl lunasrv-module-lunaerp -am compile` 通过，lunasrv-server 能引用到新模块

- [x] Task 2: 数据库与菜单 SQL
  - [x] 2.1 编写 `luna-srv/sql/postgresql/lunaerp-baseinfo-product.sql`：lunaerp_baseinfo_product / lunaerp_baseinfo_product_image / lunaerp_baseinfo_product_tag / lunaerp_baseinfo_product_tag_rel / lunaerp_baseinfo_product_greige_type 五表 DDL（均以 lunaerp_baseinfo_product 为前缀；含芋道标准字段、`WHERE deleted = 0` 部分唯一索引、字段 COMMENT）
  - [x] 2.2 同文件追加菜单 SQL（id 号段 10000-10999；菜单显示不含 lunaerp 层）：id=10000 目录「基本信息」（type=1, parent_id=0, path=/baseinfo）→ id=10010 菜单「产品信息」（component lunaerp/baseinfo/product/index，component_name LunaerpBaseinfoProduct，菜单行权限 lunaerp:baseinfo:product:query）+ 按钮 10011/10012/10013（create/update/delete）→ id=10020 菜单「胚布品种」（component lunaerp/baseinfo/greige-type/index，component_name LunaerpBaseinfoGreigeType，菜单行权限 lunaerp:baseinfo:product:greige:query）+ 按钮 10021/10022/10023（greige:create/update/delete）
  - [x] 2.3 验证：SQL 可在 PostgreSQL 执行（语法检查/手工执行）

- [x] Task 3: 产品 CRUD 后端（baseinfo.product）
  - [x] 3.1 ProductDO / ProductImageDO（dal/dataobject/baseinfo/product）+ Mapper（dal/mysql/baseinfo/product）+ VO（controller/baseinfo/product/vo：PageReqVO、RespVO、SaveReqVO）
  - [x] 3.2 ProductService（service/baseinfo/product）：page（分词搜索、状态/字段/克重区间过滤、附带 images/标签/供应商名）、get、get-by-code、create（编号唯一校验、yarnSpecs/costItems/tags/images 事务写入）、update（含标签整体替换、图片替换）、delete、delete-list（逻辑删除置 status=DELETED）、next-code（前缀+数字递增、排除已删除）
  - [x] 3.3 ProductController（/lunaerp/baseinfo/product/*，@PreAuthorize lunaerp:baseinfo:product:*；包在 controller.baseinfo 故显式 /admin-api 前缀）
  - [x] 3.4 验证：模块编译通过，接口语义对照 spec 场景

- [x] Task 4: 产品标签后端（baseinfo.producttag）
  - [x] 4.1 ProductTagDO / ProductTagRelDO + Mapper + VO
  - [x] 4.2 ProductTagService：list（租户内）、create/update/delete（同名唯一）；产品创建/更新时的标签 co-update（自动建档、同名同色）
  - [x] 4.3 ProductTagController（/lunaerp/baseinfo/product-tag/*；显式 /admin-api 前缀）
  - [x] 4.4 验证：模块编译通过

- [x] Task 5: 胚布品种后端（baseinfo.greigetype）
  - [x] 5.1 GreigeTypeDO + Mapper + VO（PageReqVO/RespVO/SaveReqVO/SimpleRespVO）
  - [x] 5.2 GreigeTypeService：page/simple-list/get/create（生成 GT-XXXX 流水号 + matchKey，matchKey 查重）/update/delete；matchKey 生成规则与原 buildGreigeMatchKey 一致（布种+成分+纱支+坯克重+坯门幅）
  - [x] 5.3 GreigeTypeController（/lunaerp/baseinfo/greige-type/*；显式 /admin-api 前缀）
  - [x] 5.4 验证：模块编译通过（已统一 mvn compile；5 表序列已补入 SQL）

- [x] Task 6: 前端 API 层
  - [x] 6.1 `luna-ui/src/api/lunaerp/baseinfo/product/index.ts`（page/get/get-by-code/create/update/delete/delete-list/next-code/update-tags）、`product-tag.ts`（list/create/update/delete）、`greige-type.ts`（page/simple-list/get/create/update/delete），类型定义对齐后端 VO
  - [x] 6.2 验证：luna-ui 类型检查通过（新增文件零报错；仓库既有 28 个无关报错）

- [x] Task 7: 产品信息前端页面
  - [x] 7.1 `src/views/lunaerp/baseinfo/product/index.vue` 主页面：灵动岛工具栏（搜索、grid/table 切换、新建）、状态分段筛选（在售/草稿）、分页
  - [x] 7.2 ProductGrid / ProductTable 双视图组件（字段与操作按钮对齐 oldweb）
  - [x] 7.3 编辑弹窗 ProductEditModal：基本信息、纱线规格动态行、成本项动态行、标签区（选择/新建）、图片上传（infra 上传 + 封面 + 排序）、胚布品种选择器弹窗；保存后"新增插入当前页/编辑替换原位"缓存策略
  - [x] 7.4 删除与批量删除流程（确认提示 + delete/delete-list）
  - [x] 7.5 v-hasPermi 权限指令接入（lunaerp:baseinfo:product:*）；motion-v 动画对齐原体验
  - [x] 7.6 验证：IDE 诊断 0 报错（Task 9 统一 vite build）

- [x] Task 8: 胚布品种管理前端页面
  - [x] 8.1 `src/views/lunaerp/baseinfo/greige-type/index.vue`：列表/新增/编辑/删除，matchKey 查重提示
  - [x] 8.2 验证：IDE 诊断 0 报错（Task 9 统一 vite build）

- [x] Task 9: 整体验证
  - [x] 9.1 后端 `mvn -pl lunasrv-module-lunaerp -am package` 通过
  - [x] 9.2 前端类型检查 + build 通过（build:local SUCCESS）
  - [x] 9.3 对照 checklist.md 逐项核验（接口语义/权限标识/菜单路径/体验要点）——0 FAIL，1 WARN 为 spec 已登记的供应商简化项

# Task Dependencies
- Task 2 可与 Task 1 并行
- Task 3/4/5 依赖 Task 1（可并行）
- Task 6 依赖 Task 2（接口/权限标识确定后即可开工，也可与 Task 3-5 并行）
- Task 7 依赖 Task 6；Task 8 依赖 Task 6
- Task 9 依赖 Task 3-8 全部完成
