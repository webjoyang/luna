# 01: 进度反馈 SQL 基建（2 表 + 10400 菜单/10401-10405 + 字典 2247 + 017 错误码）

**What to build:** 执行建表脚本后，反馈缸与反馈记录 2 张表（含序列、索引、TenantBaseDO 标准列与列注释）就位；"生产管理"目录下出现"进度反馈"菜单（10400）及建缸/编辑/推进/删除/反馈记录五个按钮权限（10401-10405）；进度状态字典（2247，六态含虚拟未排缸）可在字典管理查看；后端错误码 1-030-017 段常量定义就位。脚本可重复执行。此时尚无业务页面与接口，仅地基。

**Blocked by:** 阶段一 `.scratch/aggregate-differential-save/` 票 01-07 全部完成（染色颜色行稳定 id 是覆盖层关联的硬前置，ADR-0032）

**Status:** resolved

- [x] 2 张表 DDL 落库：`lunaerp_production_progress_feedback`（dyeing_order_id、dyeing_color_id、vat_no、quantity、pieces、status、五个阶段日期戳 scheduled_date/dyeing_started_date/fixing_started_date/quality_check_date/completed_date、delay_reason、remark + 标准审计列）与 `lunaerp_production_progress_feedback_note`（feedback_id、content、issue_flag、resolved_at + 标准审计列）
- [x] 两表均不建数据库外键、不建缸号唯一约束；普通索引：反馈缸 (tenant_id,dyeing_order_id,dyeing_color_id) 与 status；记录 feedback_id
- [x] 表上不含任何名称展示快照列（合同号/客户/染厂/产品/颜色）——零快照决策（ADR-0031）
- [x] 10400 进度反馈菜单挂 10200 生产管理（query 权限，组件路径 lunaerp-elmui/production/progress-feedback/index）+ 10401 create / 10402 update / 10403 advance / 10404 delete / 10405 note；SQL 注释写明跳号原因（10290-10298 织造领纱已占、10300+ 库存目录已占）
- [x] 字典 2247 `lunaerp_progress_feedback_status`：NOT_SCHEDULED 未排缸（虚拟态，仅用于渲染不持久化）/SCHEDULED 已排缸/DYEING 染色中/FIXING 固色中/QUALITY_CHECK 质检中/COMPLETED 已完成，标签颜色与 DictTag 一致
- [x] 后端 ErrorCodeConstants 落地 017 段：000-004 缸段、100-101 记录段
- [x] 脚本幂等（事务内先 DELETE 再 INSERT），仅语法自查不直连共享库

## Comments

- 创建 SQL 脚本 `luna-srv/sql/postgresql/lunaerp-production-progress-feedback.sql`：
  - 反馈缸表 `lunaerp_production_progress_feedback`：20 列，含 5 个阶段日期戳、TenantBaseDO 审计列、主键约束判存 `pk_lb_prog_fb`、表与列全注释，索引 `(tenant_id, dyeing_order_id, dyeing_color_id)` 与 `status`
  - 反馈记录流水表 `lunaerp_production_progress_feedback_note`：10 列，含 `feedback_id`、`content` (text)、`issue_flag` (boolean)、`resolved_at` (timestamp)、审计列、主键约束 `pk_lb_prog_fb_note`、索引 `feedback_id`
  - 序列：`lunaerp_production_progress_feedback_seq`、`lunaerp_production_progress_feedback_note_seq`
  - 菜单权限：10400 进度反馈菜单（挂 10200 生产管理，sort 10）+ 10401-10405 五按钮权限（create/update/advance/delete/note），SQL 注释注明跳号原因（10290-10298 领纱已占、10300+ 库存已占）
  - 字典数据：2247 进度反馈状态 `lunaerp_progress_feedback_status`（六态含虚拟态 NOT_SCHEDULED，dict_data id 3696-3701），头部含 2247 占用避让至 2248 的幂等 DO 块
- 避让调整：
  - `lunaerp-stock.sql` 的 `lunaerp_stock_item_type` 字典类型 ID 统一调为 2248，避免与进度反馈 2247 冲突
- 后端错误码与单测：
  - `ErrorCodeConstants.java` 注册 017 段：000-004 缸段、100-101 记录段
  - `ErrorCodeConstantsTest.java` 覆盖 017 段错误码验证，通过单测
- 前端字典定义：
  - `luna-ui/src/utils/dict.ts` 注册 `LUNAERP_PROGRESS_FEEDBACK_STATUS` 字典常量
