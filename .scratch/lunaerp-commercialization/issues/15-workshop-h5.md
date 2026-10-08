# 15-workshop-h5: 车间移动 H5

Type: grilling
Status: resolved
Blocked by: 

## Question

设计车间移动 H5 并产出 spec（W4，Q6=A / Q14-i），grilling 时先调 Skill "grilling" 与 "domain-modeling"：

1. 四个场景：
   - 布票扫码报工：扫布票二维码 → 机台任务进度回报（复用 progress-feedback 与 weaving-execution 现有接口，先读实现再定复用点）；
   - 卷级扫码质检：扫卷码 → 录检验结果（对接检验单）；
   - 领纱确认：机台领纱单的移动确认（yarn requisition）；
   - 进度回报拍照：照片上传挂进度/质检记录；
2. 认证：与主站共用账号体系（车间工人账号由系统管理开通），浏览器/企微/钉钉内打开的适配；
3. 工程落点：luna-ui 内移动路由 vs 独立 H5 工程（与 14 号保持同一决策，互相引用）；
4. 容错：弱网下的提交失败重试、扫码枪/摄像头兼容；
5. 权限：工人角色只能看分配给自己的机台任务（数据范围）。

产出：`.scratch/workshop-h5/spec.md`（按统一 DoD）。

## Answer

两轮 15 问全部按推荐定稿，spec 已落 [.scratch/workshop-h5/spec.md](../../workshop-h5/spec.md)（ready-for-agent）。

**核心结论**：

1. **报工边界修正**：ticket 原文"复用 progress-feedback"有误——ProgressFeedback 是外发染厂 PC 跟单覆盖层；自织进度回报 = 扫布票复用 weaving-execution 布票接口（start/complete/exception，ADR-0024 状态机零复制），机台任务状态继续由布票推导。
2. **零新表、2 ALTER**：布票加 `exception_photos`、录检明细加 `photos`（各 jsonb ≤6 URL，infra 文件服务，前端压缩长边 1920/2MB）；配套放宽 vatNo `@NotBlank` 为胚布 PIECE 可空的条件校验（唯一 PC 行为变更）。
3. **质检双类分流**：胚布扫布票码→H5 内部幂等调 `handoffInspection` 自动交接（免 PC 权限点）→按 weavingTicketId 定位/建录检行（pieceNo 缺省=布票 code）；成品从 DRAFT/ACTIVE 单进单逐卷录，`(缸号,卷号)` 重复定位改录；卷标签打印、质检确认（责任认定）留 PC。
4. **领纱=移动发纱台**：复用 `/dispatch`（同事务 YARN_OUT），H5 加 requestId+`@Idempotent`（30s 表达式键）保证弱网重试不重复扣库存；"我的待领纱单"按任务 worker=登录人 EXISTS 过滤；默认仓库走新 infra_config 键 `luna.workshop.default-warehouse-id`。
5. **认证与权限**：员工账号密码登录（/admin-api 标准 token，与 PC 同源），企微/钉钉一期只做 WebView 适配不做 OAuth；隐藏菜单 10530+按钮 10531–10533，seed `workshop_worker`（report+dispatch）/`workshop_inspector`（inspection）两角色，无 PC 菜单；工人范围硬过滤+未派工扫票自助认领（worker IS NULL 条件更新），开工 worker 强制取登录人。
6. **工程落点（与 14 号同决策互引，ADR-0045）**：luna-ui 内独立 `/h5/**` 路由树+H5Layout 懒加载 chunk；与门户刻意分化——门户零新依赖，H5 引 Vant 4 + html5-qrcode（扫码/拍照重交互）；键盘楔入（PDA/USB 枪）为一等公民，摄像头仅 HTTPS 增强、HTTP 局域网自动降级。
7. **编号避让**：并行的 14 号已占错误码 038 与 ADR-0044，本票顺延 **1-030-039**（001–008 全登记）与 **ADR-0045**；菜单 10530–10533（避让排产 10510/10520）；零新字典；`_order.md` 序号 30。
8. **DoD**：11 项后端必测（横向/纵向越权、认领并发、自动交接幂等、ROLL 查重、发纱幂等只扣一次、租户隔离等）+ 五终端（Android Chrome/企微/钉钉/iPhone/PDA 扫码枪）× 四流程 W4 冒烟矩阵。
9. **非目标**：企微钉钉 OAuth/JS-SDK、短信登录、PWA 离线队列、小程序、卷标签、质检确认移动化、撤销发纱、ProgressFeedback 移动化与照片。

**产出物**：spec `.scratch/workshop-h5/spec.md`；ADR-0045；production GLOSSARY（车间移动 H5/自助认领/胚布扫码自动交接三术语+模块边界+布票/录检明细照片字段+039 段）；shared GLOSSARY 总表 039 行与避让记录；`_order.md` 序号 30。
