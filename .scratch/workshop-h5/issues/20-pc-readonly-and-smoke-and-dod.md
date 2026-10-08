# 20: PC 配套只读 + 五终端冒烟 + DoD 收口

**What to build:** 落成 PC 侧只读配套与全量收口。PC 侧：织造单工作台布票 Tab 异常票展示 `exceptionPhotos` 缩略图与查看大图；检验单工作台 ItemsTab 录检行展示 `photos` 缩略图（PC 不提供上传入口，一期照片仅现场产生）；不新增任何 PC 菜单项。五终端 × 四流程冒烟矩阵：Android Chrome HTTPS / 企业微信 WebView HTTPS / 钉钉 WebView HTTPS / iPhone Safari HTTPS / 安卓 PDA（头扫键盘楔入）/ PC+USB 扫码枪 HTTP 局域网；HTTP 环境只验收楔入路径；任一流程失败阻断 W4 验收。性能验证：my-tasks/我的领纱单走 worker 索引+EXISTS 半连接，万级任务量 P95<1s（EXPLAIN 验证，必要时补 `machine_task(worker_user_id,status)` 索引）。`@LogRecord` 留痕核对（开工/完工/异常/录检/发纱五动作均落 system_operate_log）。统一 DoD 对照清单收口。跨票契约核对（04 通用件冻结 URL 消费、14 客户门户 ADR-0044 互引、11 weaving-execution 零签名破坏、inspection-order vatNo 条件校验 PC 同步生效）。

**Blocked by:** 12（报工三动作完成）, 16（成品 ROLL 录检完成）, 19（领纱弱网幂等完善完成）, 08（拍照/弱网组件就位）

**Status:** ready-for-agent

- [ ] 织造单工作台布票 Tab：异常票展示 `exceptionPhotos` 缩略图，点击查看大图
- [ ] 检验单工作台 ItemsTab：录检行展示 `photos` 缩略图，点击查看大图
- [ ] PC 不提供照片上传入口（一期照片仅现场产生）
- [ ] 不新增任何 PC 菜单项
- [ ] 五终端 × 四流程冒烟全绿（报工/胚布质检/成品质检/领纱发纱）
  - [ ] Android Chrome HTTPS
  - [ ] 企业微信 WebView HTTPS
  - [ ] 钉钉 WebView HTTPS
  - [ ] iPhone Safari HTTPS
  - [ ] 安卓 PDA（头扫键盘楔入）/ PC+USB 扫码枪 HTTP 局域网（HTTP 环境只验收楔入路径）
- [ ] 性能：my-tasks/我的领纱单 EXPLAIN 验证走 worker 索引+EXISTS 半连接，万级任务量 P95<1s
- [ ] 必要时补 `machine_task(worker_user_id,status)` 索引
- [ ] `@LogRecord` 留痕核对：开工/完工/异常/录检/发纱五动作均落 system_operate_log
- [ ] 统一 DoD 对照清单全部勾选（spec §统一 DoD 对照）
- [ ] 跨票契约核对：
  - [ ] 04 通用件冻结 URL `/h5/scan/weaving-ticket?code=` 消费零回改
  - [ ] 14 客户门户 ADR-0044 互引（同模式异凭证）
  - [ ] 11 weaving-execution 布票/领纱 service 零签名破坏式改动（异常 VO、明细 VO 仅加可空字段）
  - [ ] inspection-order vatNo 条件校验 PC 同步生效
- [ ] GLOSSARY/错误码/ADR-0045 收尾核对
