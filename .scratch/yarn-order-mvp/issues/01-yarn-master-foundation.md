# 01: 纱线档案地基（材质+规格）

**What to build:** 用户从"生产管理 → 纱线档案"菜单进入，看到上下两栏布局：上方纱线材质列表（棉/涤/人棉/氨纶/锦纶/粘胶/混纺 7 项 seed 数据已存在），下方按选中材质过滤的纱线规格列表。可新建/编辑/停用材质，可新建/编辑/删除规格（standard_name 在材质内唯一）。前端提供两级级联带搜索的下拉数据源接口（cascade-options），供采购单/配比 BOM 编辑页使用。材质不入 seed 表不入字典（ADR-0011）。

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

- [x] `luna-srv/sql/postgresql/lunaerp-production-yarn-master.sql` 已落：
  - [x] 表 1 `lunaerp_production_yarn_type` DDL（id/name/sort/status + 框架列）+ 主键 DO 块 + 列注释 + 序列
  - [x] 唯一索引 `(tenant_id, name)`（含软删行，软删不释放材质名）
  - [x] 表 2 `lunaerp_production_yarn_spec` DDL（yarn_type_id/spec_name/standard_name/spin_method/luster/ref_price/remark/status + 框架列）+ 主键 DO 块 + 列注释 + 序列
  - [x] 唯一索引 `(tenant_id, yarn_type_id, standard_name)` + 普通索引 `(yarn_type_id)` `(status)`
  - [x] 材质 seed 7 项（棉 sort=1 / 涤 sort=2 / 人棉 sort=3 / 氨纶 sort=4 / 锦纶 sort=5 / 粘胶 sort=6 / 混纺 sort=7，status=ACTIVE，先 DELETE WHERE id IN (1..7) 再 INSERT 保证可重复执行）
  - [x] 菜单 10240 `纱线档案`（parent=10200，component `lunaerp-elmui/production/yarn-master/index`，componentName `LunaerpElmuiProductionYarnMaster`）+ 按钮 10241/10242/10243/10244（query/create/update/delete）
  - [x] 无字典（材质不入字典决策）
- [x] 后端 `production.yarntype` 子包：
  - [x] `YarnTypeDO`（继承 TenantBaseDO）
  - [x] `YarnTypeMapper`（extends BaseMapperX）
  - [x] `YarnTypeSaveReqVO` / `YarnTypePageReqVO` / `YarnTypeRespVO`（Swagger + jakarta.validation 注解）
  - [x] `YarnTypeService` + `YarnTypeServiceImpl`（CRUD，撞名校验 1_030_008_001）
  - [x] `YarnTypeController`（5 端点：page/get/create/update/delete，`@PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-type:*')")`）
- [x] 后端 `production.yarnspec` 子包：
  - [x] `YarnSpecDO`（继承 TenantBaseDO）
  - [x] `YarnSpecMapper`
  - [x] `YarnSpecSaveReqVO` / `YarnSpecPageReqVO`（含 yarnTypeId 过滤）/ `YarnSpecRespVO` / `YarnSpecCascadeOptionVO`（两级级联出参）
  - [x] `YarnSpecService` + `YarnSpecServiceImpl`（CRUD，yarn_type_id 校验 1_030_008_000，standard_name 撞名 1_030_008_003）
  - [x] `YarnSpecController`（6 端点：5 个 CRUD + `GET /cascade-options` 返回 `[{yarnTypeId, yarnTypeName, specs:[{id, specName, standardName, refPrice, spinMethod, luster}]}]`，支持 keyword 过滤 spec_name）
- [x] 后端单测：
  - [x] `YarnTypeServiceImplTest`（创建撞名/更新不存在/删除逻辑删/分页条件）
  - [x] `YarnSpecServiceImplTest`（创建 yarn_type_id 不存在/standard_name 撞名/cascade-options 两级结构与 keyword 过滤）
- [x] 前端 API `luna-ui/src/api/lunaerp-elmui/production/yarn-master/index.ts`：
  - [x] yarn-type CRUD（URL 前缀 `/lunaerp/production/yarn-type`）
  - [x] yarn-spec CRUD + cascade-options（URL 前缀 `/lunaerp/production/yarn-spec`）
  - [x] TS 类型：YarnTypeVO / YarnSpecVO / YarnSpecCascadeOptionVO
- [x] 前端视图 `luna-ui/src/views/lunaerp-elmui/production/yarn-master/index.vue`：
  - [x] 上下两栏布局（el-row 或 el-container）
  - [x] 上方材质 el-table 单选 + 新建/编辑/停用材质按钮（el-dialog 弹窗：name/sort/status）
  - [x] 下方规格 el-table（按选中材质过滤）+ 新建/编辑/删除规格按钮（el-dialog 弹窗：yarn_type_id 只读带入 + spec_name/standard_name/spin_method/luster/ref_price/remark/status）
  - [x] 权限按钮 `v-hasPermi="['lunaerp:production:yarn-type:create']"` 等
  - [x] 删除前二次确认
- [x] 验证：
  - [x] 后端 `mvn test -pl lunasrv-module-lunaerp -Dtest=YarnTypeServiceImplTest,YarnSpecServiceImplTest` 全绿
  - [x] 前端 `pnpm build:local` 与 `pnpm ts:check` 改动文件零报错
  - [ ] 手动跑 SQL 脚本（在 dev 库），7 项 seed 数据可见，菜单可见
