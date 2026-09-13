# Checklist

## 后端（lunasrv-module-lunaerp）
- [x] lunasrv-module-lunaerp 模块接入根 pom / lunasrv-dependencies / lunasrv-server，编译通过
- [x] 包结构：com.luna.lunasrv.module.lunaerp 下 controller/baseinfo/{product,producttag,greigetype}（Controller 层不带 admin 段）、service/baseinfo/...、dal/dataobject/baseinfo/...、dal/mysql/baseinfo/...
- [x] 5 张表（均以 lunaerp_baseinfo_product 为前缀：lunaerp_baseinfo_product、lunaerp_baseinfo_product_image、lunaerp_baseinfo_product_tag、lunaerp_baseinfo_product_tag_rel、lunaerp_baseinfo_product_greige_type）DDL 符合芋道规范（creator/create_time/updater/update_time/deleted/tenant_id），唯一约束为 `WHERE deleted = 0` 部分唯一索引
- [x] 菜单 SQL：id 号段 10000-10999；id=10000 目录「基本信息」+ 10010 菜单「产品信息」（按钮 10011-10013）+ 10020 菜单「胚布品种」（按钮 10021-10023）；组件路径 lunaerp/baseinfo/product/index、lunaerp/baseinfo/greige-type/index；菜单显示不含 lunaerp 层；query 权限挂菜单行、按钮仅 create/update/delete
- [x] GET /lunaerp/baseinfo/product/page：支持 pageNo/pageSize/search/code/name/composition/type/minWeight/maxWeight/status，仅返回 ACTIVE/DRAFT，createdAt 倒序，返回 images/标签/供应商名（注：Controller 显式 /admin-api 前缀；supplierCompany 字段已预留，供应商模块迁移前恒 null——spec 已登记简化项 5）
- [x] search 关键词分词 AND/OR 多字段模糊（code/name/supplierCode/composition/type，LOWER 忽略大小写）
- [x] POST /lunaerp/baseinfo/product/create：编号租户级唯一，冲突提示"编号已被占用，请重新获取编号"；yarnSpecs/costItems/tags/images 事务写入；新标签自动建档
- [x] GET /lunaerp/baseinfo/product/next-code：沿用前缀+数字递增、排除已删除规则（默认前缀 P、4 位补零、内存取数字 max 避免字符串序问题）
- [x] PUT /lunaerp/baseinfo/product/update：标签整体替换（同事务；tags 为 null 时保留），图片整体替换
- [x] DELETE 单条与 delete-list：逻辑删除且 status=DELETED，级联逻辑删 image/rel（标签定义保留）
- [x] /lunaerp/baseinfo/product-tag：list/create/update/delete，租户内同名唯一、同名同色（co-update 同名复用保留原色）
- [x] /lunaerp/baseinfo/greige-type：page/simple-list/get/create/update/delete；创建生成 GT-XXXX 流水号；matchKey（布种+成分+纱支+坯克重+坯门幅）租户内唯一并查重
- [x] 所有写接口 @PreAuthorize 权限标识：lunaerp:baseinfo:product:create/update/delete、lunaerp:baseinfo:product:tag:*、lunaerp:baseinfo:product:greige:*
- [x] 响应统一 CommonResult 包装，分页为 PageResult
- [x] 5 个主键序列已在 SQL 创建（CREATE SEQUENCE IF NOT EXISTS），与 5 个 DO 的 @KeySequence 一一对应

## 前端（luna-ui）
- [x] api/lunaerp/baseinfo/product、product-tag、greige-type 封装完整，类型对齐后端 VO
- [x] 产品页灵动岛工具栏：搜索框、grid/table 视图切换、新建按钮
- [x] 状态分段筛选：在售（ACTIVE）/ 草稿（DRAFT）
- [x] 网格视图与表格视图字段、操作按钮（编辑/删除）与 oldweb 对齐
- [x] 分页控件与后端分页参数正确对接
- [x] 编辑弹窗分区：基本信息 / 纱线规格动态行 / 成本项动态行 / 标签区 / 图片上传（封面+排序）/ 胚布品种选择器
- [x] 保存后列表缓存按"新增插入当前页 / 编辑替换原位"更新
- [x] 删除与批量删除带确认提示，调用对应接口
- [x] v-hasPermi 按钮级权限接入（lunaerp:baseinfo:product:create/update/delete）
- [x] 关键动画使用 motion-v 实现（工具栏/列表/弹窗过渡），体验与原 React 一致
- [x] 胚布品种管理页：列表/新增/编辑/删除，查重提示（后端 matchKey 冲突文案经全局拦截器弹出）
- [x] 前端文件路径遵循 lunaerp/baseinfo 规范（api 与 views），路由 URL 为 /baseinfo/product、/baseinfo/greige-type
- [x] defineOptions name 与菜单 component_name 一致（LunaerpBaseinfoProduct / LunaerpBaseinfoGreigeType，支持 keep-alive）

## 验证
- [x] mvn -pl lunasrv-module-lunaerp -am package 通过（lunasrv-module-lunaerp-2026.08-SNAPSHOT.jar 已产出）
- [x] luna-ui 类型检查 + vite build 通过（build:local ✓ built 22.25s；新增文件零 TS 报错，既有 28 个无关报错非本次引入）
- [x] 简化项已按 spec 记录执行（无审批流、无 AI 搜索、权限收敛、costItems 直写、supplier_id 无外键、无回收站）
