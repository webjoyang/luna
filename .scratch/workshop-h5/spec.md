# 车间移动 H5（Workshop H5）

Status: ready-for-agent（15 号 grilling 两轮 Q1–Q15 全部按推荐定稿，可按 session 施工）

## Problem Statement

商业化 W4「触点与验收」（Q6=A / Q14-i）需要一个车间现场移动入口，承载四个场景：

1. **布票扫码报工**：挡车工扫布票二维码 → 对机台任务进度回报；
2. **卷级扫码质检**：检验员扫布票码/卷码 → 录检验结果（对接检验单）；
3. **领纱确认**：机台领纱单的移动发纱确认；
4. **进度回报拍照**：照片挂异常报工与质检记录。

已查清的现状事实（先读实现再定复用点）：

- **自织进度的载体是布票，不是进度反馈**：术语表明确 ProgressFeedback 是外发染厂跟单覆盖层（PC 跟单员代录），"自织织造进度已由布票体系自动推导，不重复建设"。ticket 原文"复用 progress-feedback 接口"据此修正为"复用 weaving-execution **布票**接口；progress-feedback 不在 H5 范围"。
- 布票后端 [WeavingTicketController](../../../luna-srv/lunasrv-module-lunaerp/src/main/java/com/luna/lunasrv/module/lunaerp/controller/production/weavingorder/ticket/WeavingTicketController.java) 已有 `/get-by-code`、`/start`、`/complete`、`/exception`（ADR-0024 状态机白名单守卫+条件更新）；开工 VO 可写 worker/实际机台。
- 胚布"完工交接检验"已有幂等端点 `POST /lunaerp/production/weaving-order/handoff-inspection`（按胚布单归一 find-or-create **仅建检验单表头**，要求至少一张 COMPLETED 票，ADR-0025）。
- 录检明细 [InspectionOrderItemController](../../../luna-srv/lunasrv-module-lunaerp/src/main/java/com/luna/lunasrv/module/lunaerp/controller/production/inspectionorder/item/InspectionOrderItemController.java) 为独立子资源逐条 CRUD；PIECE 模式可回填 `weavingTicketId`（审核保存回写布票 INSPECTED 的链路不变）；ROLL 模式 `(vatNo, rollNo)` 单内唯一。
- 领纱明细已有 `/dispatch`（同事务写 YARN_OUT 扣库存、校验余额、推导 PARTIAL/DISPATCHED，ADR-0027）与 `/reverse-dispatch`、库存余额查询。
- 框架自带 Redis `@Idempotent` 幂等 starter（Expression/User/Default 三解析器）；附件全项目先例为 jsonb 存 infra 文件 URL 数组（样布照片、报销附件）。
- 认证：芋道 `/system/auth/login` 账号密码 + accessToken/refreshToken，社交登录框架在但零 corp 配置；机台任务分页已支持 `workerUserId` 过滤。
- **冻结契约**（04 号统一打印）：布票二维码编码 `{luna.h5.base-url}/h5/scan/weaving-ticket?code={布票code}`，`infra_config` 键 `luna.h5.base-url`。
- 并行的 14 号客户门户已定工程落点 = luna-ui 单工程内 `/portal/*` 路由区（spec `.scratch/customer-portal/spec.md`、ADR-0044），其后果第 3 条明确"15 号车间 H5 复用免登路由区+独立前端边界的工程模式，但凭证机制不同，两票共享模式不共享令牌"。

## Grilling 定稿决策（15 号 ticket，Q1–Q15，全部按推荐）

1. **Q1 报工边界**：扫票三动作=开工/完工/异常登记，全部复用布票状态机；机台任务 actualQty/状态继续由布票自动推导，H5 不直接写机台任务（除自助认领派工）。作废、批量操作、染厂进度反馈建缸/推进留 PC。
2. **Q2 质检双类分流**：胚布（GREIGE/PIECE）扫布票码定位；成品（FINISHED/ROLL）从可录检单列表进单逐卷录，卷号摄像头扫一维码或手输。**不新增卷标签打印**；质检确认（责任认定/扣款）留 PC。
3. **Q3 领纱=移动发纱台**：领发一次完成，复用 `/dispatch`，零新状态/新签收链；领纱单不印二维码，入口="我的待领纱单"列表。
4. **Q4 工程落点 = luna-ui 内 `/h5/**` 路由树**，与 14 号门户同一决策互引；移动端组件库选 **Vant 4**（门户刻意零新依赖、本票刻意引，理由见 ADR-0045）。
5. **Q5 认证**：复用员工账号密码登录与同一份 token；企微/钉钉/浏览器一期只做 WebView 页面适配，不做 OAuth 免登/JS-SDK；seed 两个无 PC 菜单的 H5 角色。
6. **Q6 扫码终端**：新增 html5-qrcode（二维码+Code128 一维码，摄像头不可用降级）；**键盘楔入为一等公民**（PDA 头扫/USB 扫码枪自动聚焦输入框+回车触发，HTTP 环境唯一保障）；摄像头路径明确要求 HTTPS。
7. **Q7 数据范围**：H5 专用端点硬过滤 `worker_user_id=登录人`，写操作服务端二次校验归属；未派工任务支持扫票**自助认领**，派给他人拦截；检验员不受机台归属限制；开工 worker 一律以登录人为准，忽略客户端传值。
8. **Q8 照片两个挂点**：布票 `exception_photos` jsonb（异常登记 ≤6 张）、录检明细 `photos` jsonb（疵点 ≤6 张）；先传文件拿 URL 再随表单提交，前端压缩（长边 ≤1920、≤2MB）；ProgressNote 不补照片（该模块冻结只文字）。
9. **Q9 弱网**：发纱增量操作加客户端 requestId+`@Idempotent`（重复提交不重复扣库存）；其余写靠状态机守卫+按钮防连点；照片独立重试；表单 localStorage 草稿；**不做离线队列/PWA**，弱网可重试、断网不可用。
10. **Q10 IA**：Vant 底部 Tab（报工/质检/领纱/我的，按角色显隐）+ 独立登录页与扫码落点页。
11. **Q11 胚布扫码遇未交接**：H5 端点内部幂等调用 `handoffInspection(织造单)` 完成 find-or-create（不要求检验员有 PC `:handoff-inspection` 权限），再定位/新建录检行；票须 COMPLETED，pieceNo 缺省=布票完整 code，胚布 vatNo 可空（同步放宽 PC 端 `@NotBlank` 为条件校验）。
12. **Q12 成品 ROLL 录检流**：进单逐卷一行；扫码/手输卷号定位，缸号记住上次值，`(缸号,卷号)` 重复定位到已有行改录而非新建。
13. **Q13 权限**：隐藏菜单 10530 权限容器 + 10531–10533 三个按钮位；SQL seed `workshop_worker`/`workshop_inspector` 两角色（可叠加）；岗位（挡车工 LUNA_WORKER）与角色正交，沿用 ADR-0021。
14. **Q14 默认仓库**：`infra_config` 键 `luna.workshop.default-warehouse-id`（系统管理参数页维护，零新页面），发纱表单默认带入可改，未配置则强制选择并提示管理员补值。
15. **Q15 DoD**：越权/认领/自动交接幂等/发纱幂等/租户隔离必测；五类终端四流程冒烟；非目标见后。

## Solution 总览

```
手机/PDA（浏览器/企微/钉钉 WebView）
  │  账号密码登录 /admin-api/system/auth/login（员工 token）
  ▼
luna-ui /h5/** 独立移动路由树（Vant 4 + H5Layout，懒加载 chunk）
  │  /admin-api/lunaerp/h5/workshop/**（标准 admin 鉴权 + 三个 H5 权限码）
  ▼
lunaerp.h5.workshop（新横切接入层：身份强校验/归属过滤/H5 VO 裁剪）
  │  服务层直接复用，零业务规则复制
  ├─ weavingorder.ticket.WeavingTicketService（开工/完工/异常，含自助认领）
  ├─ weavingorder.WeavingOrderService.handoffInspection（胚布扫码自动交接）
  ├─ inspectionorder.item.InspectionOrderItemService（逐条录检）
  └─ weavingorder.requisition.item.YarnRequisitionItemService（发纱扣库存，@Idempotent）
```

**零新表，2 处 ALTER 加列**。H5 层不持有业务状态机，只做：登录人身份注入、本人范围过滤、扫码定位/自动交接编排、H5 专属 VO 与幂等包装。

---

## 1. 数据模型变更

### 1.1 布票加异常照片列

```sql
ALTER TABLE lunaerp_production_weaving_ticket
    ADD COLUMN IF NOT EXISTS exception_photos jsonb;
COMMENT ON COLUMN lunaerp_production_weaving_ticket.exception_photos
    IS '异常现场照片 URL JSON 数组（车间 H5 异常登记上传，≤6 张，infra 文件服务）';
```

- DO `WeavingTicketDO` 加 `List<String> exceptionPhotos`（MyBatis JacksonTypeHandler，同 samplePhotos 先例）；
- `WeavingTicketExceptionReqVO` 加同名字段（PC/Web 与 H5 共用异常端点的写 VO，H5 走自己的 VO 组装后调同一 service，service 落库时写入）；`WeavingTicketRespVO` 输出该字段供 PC 只读展示；
- 语义：照片挂在**当前异常事实**上（与 exceptionType/exceptionNote 同生共死），再次异常登记覆盖；正常开工/完工不带照片。

### 1.2 录检明细加疵点照片列

```sql
ALTER TABLE lunaerp_production_inspection_order_item
    ADD COLUMN IF NOT EXISTS photos jsonb;
COMMENT ON COLUMN lunaerp_production_inspection_order_item.photos
    IS '疵点照片 URL JSON 数组（车间 H5 录检上传，≤6 张，infra 文件服务）';
```

- DO `InspectionOrderItemDO`、Save/Resp VO 同步加 `List<String> photos`；PC ItemsTab 只读缩略图（点击看大图），PC 不提供上传入口（一期照片仅现场产生）。

### 1.3 缸号校验放宽（行为变更，显式确认）

`InspectionOrderItemSaveReqVO.vatNo` 现为 `@NotBlank`，与术语表"胚布场景缸号可空"矛盾。改为**Service 层条件校验**：

- 检验单 `type=FINISHED` 或 `packageMode=ROLL`：vatNo 必填（不变）；
- `type=GREIGE AND packageMode=PIECE`：vatNo 可空（H5 胚布扫码录检不带缸号；PC 同步放宽，保存空缸号不再报错）。

### 1.4 配置键

| 键 | 用途 | 缺失行为 |
|---|---|---|
| `luna.h5.base-url` | 布票二维码域名（04 号已冻结） | 打印侧已处理；H5 不新增逻辑 |
| `luna.workshop.default-warehouse-id` | H5 发纱默认仓库 | 发纱页不预选、强制手选，顶部黄条提示管理员配置 |

均走芋道 `infra_config`（租户级参数，系统管理→参数设置维护），零新页面、零新表。

## 2. 认证与工程形态

### 2.1 认证（员工体系，非门户令牌）

- 登录走 `POST /admin-api/system/auth/login`（用户名+密码，可复用验证码开关），成功后 accessToken/refreshToken 存主站既有 localStorage 键，H5 与 PC **同源共享登录态**；401 走既有 refresh-token 轮换，失败清态。
- H5 端点全部挂 `/admin-api` 标准 admin 鉴权（非 14 号门户的 `/app-api`+PortalToken：工人是 system_users 员工，复用芋道 RBAC/租户/操作日志全套）。
- 新增两个角色（见 §5），**只授 H5 按钮权限、不挂任何 PC 菜单**：工人误打开 PC 地址时菜单为空，仅有个人中心类框架页。
- 企微/钉钉/普通手机浏览器一期均为"内置浏览器打开 H5 地址"，不做 corp OAuth 免登、JS-SDK 鉴权、分享配置（私有化各客户 corp 差异大，列非目标）。

### 2.2 前端工程形态（ADR-0045）

- luna-ui 内新增**独立移动路由树 `/h5/**`** 与 `H5Layout`（Vant Tabbar 布局），不嵌 PC 菜单框架；路由全部懒加载，Vant/html5-qrcode 单独 split chunk（vite `manualChunks`），PC 首屏零增量；
- 移动子树禁止引用 `lunaerp-elmui/**` PC 组件与 ElementPlus 业务组件（van-/el- 前缀不冲突，但隔离依赖方向）；
- 请求层复用主站 axios 客户端与 token 拦截器，H5 自定义响应分支：401 → `/h5/login?redirect=`（不跳 PC `/login`）；
- 依赖新增：`vant@^4`（经 unplugin-vue-components 的 VantResolver 按需）、`html5-qrcode@^2`（二维码+Code128，MIT）。

## 3. 后端 H5 端点

新包：`com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop`（Controller/VO）+ `service/h5/workshop`（编排 Service）。URL 前缀 `/lunaerp/h5/workshop`（网关后完整路径 `/admin-api/lunaerp/h5/workshop/**`）。所有端点：

- `@PreAuthorize` 按 §5 三个权限码鉴权；
- 数据范围在 H5 Service 内强制（不信任前端传 userId/workerId）；
- 写库一律调既有生产域 Service，**不复制状态机/库存规则**；
- 成功出参用 H5 精简 VO（只含现场需要字段），错误直接抛既有错误码（015 段布票/领纱、010 段检验），H5 自有错误码仅限新场景（§6）。

### 3.1 扫码报工（权限 `lunaerp:workshop:h5:report`）

| 方法 | 路径 | 入参（要点） | 说明 |
|---|---|---|---|
| GET | `/my-tasks` | status 可选（默认 PENDING/IN_PROGRESS/HALTED）、分页 | **硬过滤** machine_task.worker_user_id=登录人；每任务附织造单号/品名快照/计划匹数/actualQty/布票各状态计数 |
| GET | `/ticket/get-by-code` | code | 扫码/手输定位。返回布票卡（票号/织造单/机台/品名颜色/状态/任务）+ `claimable` 标志。范围：票非 VOID；任务 worker 为空或=登录人，否则 039-002；票不存在 039-001 |
| POST | `/ticket/start` | `{code, actualMachine?}` | **自助认领+开工**：任务 worker 为空→先把 machine_task.worker 写为登录人（条件更新 `WHERE worker_user_id IS NULL`，并发落败 039-002）再开工；worker=登录人直接开工。调 `startTicket`，workerUserId/workerName 强制取登录人，忽略客户端值；actualMachine 可空 |
| POST | `/ticket/complete` | `{code}` | 调 `completeTicket`；**幂等友好**：票已 COMPLETED/INSPECTED/STOCKED → 返回成功+`alreadyCompleted=true`（不报错），供弱网重试；非 STARTED/已完成态抛既有 015-111 |
| POST | `/ticket/exception` | `{code, exceptionType, exceptionNote?, exceptionPhotos?[]}` | 调 `exceptionTicket`，照片 ≤6 张仅图片 URL（超限 039-008）；异常类型走既有字典校验 015-113 |

不开放：void、批量 start/complete、改派他人、停机/恢复（HALTED 由 PC 领班操作）。

### 3.2 移动质检（权限 `lunaerp:workshop:h5:inspection`）

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/inspection/page` | 状态默认 DRAFT/ACTIVE、type 可选、分页 | 检验员可看**全部租户内**可录检单（不按机台过滤）；返回单号/类型标签/承检方/品名/录检进度 |
| GET | `/inspection/get` | id | 表头+逐行（含 photos）；仅 DRAFT/ACTIVE 可进，COMPLETED 只读 |
| GET | `/inspection/ticket-scan` | code（布票码） | **胚布扫码定位编排**，规则见下 |
| POST | `/inspection/item/save` | H5 录检 VO | 新建/改录一条，规则见下；无删除端点（删除留 PC） |

`ticket-scan` 规则：

1. 按 code 查布票（不存在/VOID → 039-001）；状态必须 COMPLETED/INSPECTED/STOCKED 之一，未完工 → 039-003，其余不可录态 → 039-004；
2. 内部直接调 `weavingOrderService.handoffInspection(ticket.weavingOrderId)`（幂等：已交接复用，按胚布单归一）——H5 Service 调用绕过 Web 权限点，不要求检验员具备 `:handoff-inspection`；
3. 按 `weavingTicketId` 查录检行：存在 → 返回 `{inspectionOrderId, item}` 供改录；不存在 → 返回 `{inspectionOrderId, item:null, suggestedPieceNo: 布票code}`；
4. 该检验单已 COMPLETED/CANCELLED → 010-002。

`item/save` 规则：

- **胚布 PIECE**（来自 ticket-scan）：`weavingTicketId` 必填带回，pieceNo 缺省=布票完整 code（可改，单内 (vatNo,pieceNo) 唯一性仍由既有 service 校验），vatNo 可空；机台号/产品编码品名/颜色快照**由服务端从布票→织造单/胚布单补齐**，不信任也不采用客户端快照；
- **成品 ROLL**（来自检验单内逐卷录）：vatNo、rollNo 必填；保存前按 `(inspectionOrderId, vatNo, rollNo)` 查重，命中已存在行 → **不覆盖**，返回 039-005 + 该行数据，前端定位改录；
- 公共字段：grade（字典 2232）、measuredWeight、lengthM、remarks、photos（≤6）；第一卷保存时 DRAFT→ACTIVE 的既有便利逻辑不变；
- 改录仅在检验单 DRAFT/ACTIVE 允许；布票 INSPECTED 回写、(未来)入库链路零改动。

### 3.3 移动领纱（权限 `lunaerp:workshop:h5:dispatch`）

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/requisition/page` | 状态默认 ACTIVE/PARTIAL、分页 | "我的待领纱单"：领纱单→织造单→机台任务 EXISTS `worker_user_id=登录人` 去重；DISPATCHED/CLOSED/DRAFT 不列 |
| GET | `/requisition/get` | id | 归属校验同上（不符 039-007）；返回单头+逐行（规格名/需求/已发/单位）+ 默认仓库 id |
| GET | `/warehouse/simple-list` | — | 启用中仓库 id+name（H5 精简 VO，供默认仓库缺失时手选） |
| GET | `/yarn-balance` | warehouseId, yarnSpecId | 复用既有余额查询 service（与 PC 端点同源） |
| POST | `/requisition-item/dispatch` | `{itemId, warehouseId, locationId?, dispatchQty, remark?, requestId}` | 见下 |

发纱规则：

- H5 专用 VO 带 `requestId`（前端 UUID，必填），Controller 注解：
  `@Idempotent(timeout=30, timeUnit=SECONDS, keyResolver=ExpressionIdempotentKeyResolver.class, keyArg="#reqVO.requestId", message="发纱正在处理，请勿重复提交")`；
  窗口内同 requestId 重复提交直接拦截/返回首次结果，**YARN_OUT 与库存扣减只发生一次**；
- 服务端复用 `YarnRequisitionItemService.dispatchYarn`：ACTIVE/PARTIAL 守卫、余额不足 015-311、同事务 YARN_OUT+dispatchedQty+状态推导（ADR-0027）全部不变；
- 默认仓库：取 `luna.workshop.default-warehouse-id`（存在且启用才预选），warehouseId 仍必填校验；未配置不阻断接口（由前端强制选择）；
- 不开放撤销发纱（reverse-dispatch 留 PC，纠错需仓管介入）。

## 4. 前端（luna-ui）

### 4.1 目录与依赖

- 视图 `src/views/h5/`、API `src/api/h5/workshop/`、布局 `src/views/h5/layout/H5Layout.vue`（正式目录，非 elmui 临时区）；
- `package.json` 加 `vant@^4`、`html5-qrcode@^2`；构建配置加 VantResolver 与 `manualChunks`（vant、html5-qrcode 独立 chunk）；
- 全局只在 `/h5` 子树引入 Vant 样式与移动 rem/viewport 约定（Vant 默认 px + 浏览器视口，不引入 postcss-px-to-viewport，与门户 max-width 方案并存不冲突）。

### 4.2 路由与守卫

| 路径 | 页面 | 角色可见 |
|---|---|---|
| `/h5/login` | 账号密码登录（带 redirect 回跳） | 白名单 |
| `/h5/report` | Tab 报工：大扫码按钮 + 我的机台任务列表 | report |
| `/h5/inspection` | Tab 质检：扫码 + 可录检单列表 | inspection |
| `/h5/requisition` | Tab 领纱：我的待领纱单 | dispatch |
| `/h5/me` | Tab 我的：姓名/角色、退出登录 | 全部 |
| `/h5/scan/weaving-ticket?code=` | **冻结二维码落点**：登录后按角色分流 | — |
| `/h5/ticket?code=` | 布票报工卡（开工/完工/异常登记+拍照） | report |
| `/h5/inspection/order?id=` | 检验单逐卷/逐匹录检 | inspection |
| `/h5/requisition/detail?id=` | 领纱单逐行发纱 | dispatch |
| `/h5/403` | 无车间角色提示（联系管理员开通） | — |

`src/permission.ts` 改动：`/h5/login` 入白名单；`/h5/**` 无 token → `/h5/login?redirect=...`（不跳 PC 登录）；有 token 但三个权限码全无 → `/h5/403`；Tab 按 `get-permission-info` 权限位显隐，直接输 URL 同样做按钮级守卫。

扫码落点分流：仅 report → 票卡；仅 inspection → 走 ticket-scan 进胚布录检；两者都有 → ActionSheet 选择并记住上次选择（可在"我的"里重置）。

### 4.3 扫码与拍照（终端兼容）

- 每个扫码场景提供两条输入路径：① 摄像头组件（html5-qrcode，布票二维码；成品卷支持一维 Code128 切换），失败/无权限/非安全上下文显示降级提示；② **常驻自动聚焦输入框+软回车按钮**，PDA 头扫与 USB 扫码枪以键盘楔入回车触发（HTTP 局域网环境的唯一保证路径）；
- 部署说明写进 spec 与上线手册：**摄像头必须 HTTPS**（内网域名/自签证书）或 localhost；`http://192.168.*` 下自动隐藏摄像头入口、只留楔入输入，不报错卡死；
- 拍照：`<van-uploader>` 或 `<input type=file accept="image/*" capture="environment">`，多张 ≤6；统一压缩工具（canvas 长边 ≤1920、JPEG ≤2MB）后 `POST /admin-api/infra/file/upload`（multipart，携带同一 token），拿 URL 再随业务表单提交；上传失败可单独重传，不阻塞表单其余字段；
- 企微/钉钉/手机 Safari/Chrome 均使用系统 WebView 标准能力，无 JS-SDK 依赖。

#### 4.3.1 扫码终端上线与运维部署说明（摄像头 HTTPS 与键盘楔入双路径）

1. **浏览器安全策略与摄像头约束**：
   - 现代移动端浏览器（Safari / Chrome / 企微 / 钉钉内置 WebView）严格遵循 W3C Secure Contexts 规范，`navigator.mediaDevices.getUserMedia` 仅在安全上下文（HTTPS 或 `localhost` / `127.0.0.1`）开放；
   - 在局域网纯 HTTP（如 `http://192.168.*.*`、`http://10.*.*.*`）部署时，H5 扫码组件根据 `isSecureContext=false` 与 `protocol=http:` 自动隐藏摄像头取景入口，避免抛出 `NotAllowedError` 或导致页面卡死；
2. **常驻键盘楔入作为一等公民**：
   - 工厂常见工业安卓 PDA（激光/红外头扫）及 USB 扫码枪，输出均配置为键盘楔入（Keyboard Wedge）附加回车后缀（`\r` 或 `\n`）；
   - 常驻自动聚焦输入框与软回车按钮是纯 HTTP 局域网环境下的 100% 可用保证路径；
3. **车间现场推荐部署案**：
   - **方案 A（手机/平板触摸摄像头扫码）**：Nginx 配置内网 HTTPS 域名或自建 CA 签发证书（手机安装信任证书），开放 443 端口，工人即可使用手机自带摄像头扫码（支持布票二维码与一维 Code128 切换）；
   - **方案 B（工业 PDA / 扫码枪作业）**：沿用内网纯 HTTP 部署，PDA 头扫即用，无需配置证书，免除终端证书信任与更新维护成本。

### 4.4 弱网交互

- 所有提交按钮提交中禁用防连点；业务错误（状态码/守卫）与网络错误分色提示（网络错误文案"网络异常，单据未提交，可点重试"，不暗示已成功）；
- 报工/异常/录检/发纱表单内容实时存 localStorage 草稿（按 code/id 分键），提交成功后清除；
- 完工/开工遇"已在目标态"响应按成功处理（对应 3.1 幂等友好）；
- 发纱 requestId 一次生成随表单持久化，重试沿用同一 id（换 requestId 视为新请求）；
- 不做后台同步、冲突合并、离线扫码队列。

### 4.5 PC 侧配套小改（只读）

- 织造单工作台布票 Tab：异常票展示 `exceptionPhotos` 缩略图与查看大图；
- 检验单工作台 ItemsTab：录检行展示 `photos` 缩略图。
- 不新增任何 PC 菜单项。

## 5. 菜单、权限码与角色 seed

SQL：`luna-srv/sql/postgresql/lunaerp-workshop-h5.sql`（可重复执行，风格同 inspection-confirmation SQL）。

| id | 类型 | 名称 | 权限码/挂载 |
|---|---|---|---|
| 10530 | 菜单（**visible=0 隐藏**，无 component/path 占位） | 车间 H5（权限容器） | parent=10200，不占导航 |
| 10531 | 按钮 | 移动报工 | `lunaerp:workshop:h5:report` |
| 10532 | 按钮 | 移动质检 | `lunaerp:workshop:h5:inspection` |
| 10533 | 按钮 | 移动发纱 | `lunaerp:workshop:h5:dispatch` |

角色 seed（DO 块按 code 幂等 upsert，不固定 id，tenant_id=1，仿 LUNA_WORKER 岗位 seed）：

| code | 名称 | 授权 |
|---|---|---|
| `workshop_worker` | 车间工人 | 10531+10533 |
| `workshop_inspector` | 车间检验员 | 10532 |

- 一人可双角色；挡车工身份仍由机台任务 `worker_user_id` 关联（system_users + LUNA_WORKER 岗位，ADR-0021），**岗位与角色正交**：岗位表达"这人是挡车工"，角色表达"这人能用 H5 哪些功能"；
- super_admin/admin 自动获全部 H5 权限（既有无视菜单授权逻辑，SQL 同步插 role_menu 保持 get-permission-info 一致）；
- 多租户环境：seed 仅租户 1；新租户由租户管理员在系统管理复制角色或自行创建（一期私有化买断单租户为主，不做租户模板自动化）。

## 6. 错误码（段位 `1-030-039`，避让 14 号门户 038）

H5 只新增"接入层新场景"错误码；业务守卫全部复用 015（布票/领纱）、010（检验）既有码：

| 码 | 含义 |
|---|---|
| `1-030-039-001` | 布票不存在或已作废（扫码查无此票，含租户隔离落空，统一文案防枚举） |
| `1-030-039-002` | 该机台任务已派给其他挡车工，无权操作（请联系领班在 PC 改派；含自助认领并发落败） |
| `1-030-039-003` | 布票尚未完工，不能扫码质检（请先由挡车工报工完工） |
| `1-030-039-004` | 布票当前状态不可录检 |
| `1-030-039-005` | 该缸号+卷号已存在录检记录（返回已存在行，前端定位改录，非致命） |
| `1-030-039-006` | 领纱单不属于你的机台任务，无权操作 |
| `1-030-039-007` | 未配置车间默认发纱仓库（保留段，前端通常在选择器侧引导，不阻断） |
| `1-030-039-008` | 照片数量超过 6 张或含非图片文件 |
| `1-030-039-009` 起 | 预留 |

## 7. 字典

**零新增字典**。异常类型 2244、检验等级 2232、布票/任务/领纱/检验单状态字典全部复用；角色编码不进字典（system 域数据）。

## 8. 回收站与打印

- 不涉及回收站（H5 不产生独立主数据，不删任何单据）；
- 打印零改动：布票标签与二维码由 04 号统一打印交付，本票只接其冻结落点 URL。

## Testing Decisions

后端测试（`lunasrv-module-lunaerp`，金额无关但状态/库存副作用必测，沿用既有 ServiceTest 基座）：

1. **横向越权**：工人 A 的 token 操作工人 B 的布票（开工/完工/异常各一）→ 039-002；操作 B 的领纱单 → 039-006；
2. **纵向越权**：无 report/inspection/dispatch 权限的登录态调各端点 → 403/鉴权异常；未登录 → 401；
3. **自助认领**：未派工任务扫票开工→任务 worker=登录人且布票 STARTED（worker 字段=登录人，构造的客户端 workerUserId 被忽略）；并发两次不同人认领→一人成功一人 039-002；已派他人→039-002；
4. **胚布扫码自动交接**：未交接织造单首扫→检验单表头创建+录检行关联 ticket；连扫两次同一票（第二票/重放同票）→同一检验单、不重复建行；无完工票 → 015-005 透传；
5. **ROLL 查重**：同 (缸号,卷号) 二次保存 → 039-005 且不产生新行；
6. **发纱幂等**：同 requestId 连发两次（含模拟首请求超时重放）→ 一条 YARN_OUT、库存只扣一次、dispatchedQty 正确；不同 requestId 正常二次发纱；余额不足 → 015-311 且幂等键释放可重试；
7. **完工幂等友好**：票已 COMPLETED 再调 complete → 成功 alreadyCompleted；VOID 票各动作 → 既有守卫码；
8. **缸号条件校验**：GREIGE+PIECE 空 vatNo 可存；FINISHED/ROLL 空 vatNo → 校验错；
9. **照片守卫**：7 张/非图片 URL → 039-008；照片随异常覆盖、随录检更新落库；
10. **租户隔离**：写/查端点跨租户构造 id/code → 查无（039-001 等统一文案），断言无数据泄漏；
11. 状态机条件更新、库存同事务等既有规则不在本票重复测全量，仅覆盖 H5 编排触达路径。

## 前端验收清单

- [ ] 登录/过期/退出：登录回跳原落点；token 过期自动 refresh；401 回 H5 登录页；无角色进 403
- [ ] 报工全流程：我的任务（只看自己）→ 扫票/手输码 → 开工（含未派工认领）→ 异常（类型+说明+3 张照片）→ 完工；重复完工提示为成功
- [ ] 质检胚布：扫完工票→自动建交接单→录等级/重量/米数/照片→PC 检验单与布票 INSPECTED 可见；重扫同票改录原行
- [ ] 质检成品：进 DRAFT/ACTIVE 单→逐卷扫码/手输→缸号记忆→重复卷定位改录
- [ ] 领纱：待领纱单只列自己任务的单→逐行填量（默认仓库/改仓/查余额）→发纱成功→弱网双击只扣一次
- [ ] 弱网：飞行模式提交保留草稿与明确失败文案；恢复后一键重试成功；照片先传后提交顺序正确
- [ ] 摄像头在 HTTPS 下可用、HTTP 下自动隐藏仅留楔入输入且页面可用
- [ ] Tab/路由按两个角色四种授权组合显隐正确；PC 误登录无业务菜单

## 波次冒烟（W4）

终端矩阵 × 四流程（报工/胚布质检/成品质检/领纱发纱）：

| 终端 | 环境 |
|---|---|
| Android Chrome | HTTPS |
| 企业微信 WebView | HTTPS |
| 钉钉 WebView | HTTPS |
| iPhone Safari | HTTPS |
| 安卓 PDA（头扫键盘楔入）/ PC+USB 扫码枪 | HTTP 局域网 |

HTTP 环境只验收楔入路径；任一流程失败阻断 W4 验收。

## Out of Scope（一期不做）

- 微信/企业微信/钉钉 OAuth 免登、JS-SDK、消息分享、corp 通讯录同步；短信验证码登录（端点现成不启用）；
- 离线队列/后台同步/PWA 安装包、原生 App、微信小程序（Q6=A）；
- 卷标签二维码打印（成品卷无系统码，靠染厂吊牌/手输；二期若贴内场卷码再立项）；
- 质检确认（责任认定/挡车工扣款）移动化、检验单删除/作废、布票作废/批量操作、HALTED 停机恢复；
- 撤销发纱（reverse-dispatch）、领纱单二维码打印与独立签收链；
- ProgressFeedback（外发染厂跟单）的任何移动能力与照片回补；
- 工人侧改密码、消息中心、工资查询、排班；PC 侧照片上传入口；
- 多租户角色模板自动下发。

## 统一 DoD 对照

- [x] **状态机白名单+终态守卫+并发条件更新**：零新增状态机，全部复用布票（ADR-0024）、检验单、领纱（ADR-0027）既有守卫；自助认领新增一处条件更新（worker IS NULL）
- [x] **权限+脱敏**：三权限码+两角色 seed，H5 端点本人范围硬过滤，横/纵向越权必测；H5 VO 不输出任何成本/内部备注字段
- [x] **列表/详情/删除进回收站**：不适用（无新主数据、H5 不提供删除）
- [x] **打印+操作/审批日志**：打印零改动（04 号布票标签）；H5 关键写动作（开工/完工/异常/录检/发纱）加 `@LogRecord` 留痕，复用 system_operate_log，不新建日志表
- [x] **核心状态流转与库存副作用测试**：见 Testing Decisions 1–11（发纱扣库存幂等为重点）
- [x] **前端验收清单+波次冒烟**：见上两节五终端矩阵
- [x] **术语与错误码归位**：production GLOSSARY 增补车间移动 H5 词条/模块边界/照片字段；shared 总表登记 1-030-039
- [x] **性能/安全硬指标**：my-tasks/我的领纱单走 worker 索引+EXISTS 半连接，万级任务量 P95<1s（EXPLAIN 验证，必要时补 machine_task(worker_user_id,status) 索引）；写接口租户隔离+横向越权必测
- [x] **工程决策 ADR**：ADR-0045（与 14 号 ADR-0044 互引）

## Further Notes

### 跨票契约

- **04 通用件**：消费冻结的布票二维码 URL `/h5/scan/weaving-ticket?code=` 与 `luna.h5.base-url`，码格式零回改；
- **14 客户门户**：工程模式（单工程独立路由区/独立布局/懒加载/自有 401 分支）互引，身份机制刻意不同（admin token vs 门户令牌），ADR-0044 后果第 3 条收口于 ADR-0045；
- **11 weaving-execution**：布票/领纱 service 零签名破坏式改动（异常 VO、明细 VO 仅加可空字段）；
- **inspection-order**：vatNo 条件校验为唯一行为放宽点，PC 同步生效。

### 关联文档

- ADR-0045 车间 H5 工程形态与组件库分化；ADR-0024 布票状态机；ADR-0025 ROLL/PIECE；ADR-0027 领纱发纱；ADR-0021 工人复用 system 用户；ADR-0044 客户门户（同模式异凭证）。
- 术语：production GLOSSARY「车间移动 H5」「布票」「录检明细」。
- `_order.md` 序号 30（W4，依赖 11 织造执行、10 检验、18 通用件打印契约）。
