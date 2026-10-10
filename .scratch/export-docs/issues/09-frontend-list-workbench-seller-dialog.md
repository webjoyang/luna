# 09: 前端列表页 + 工作台编辑页 + 卖方配置弹窗

**What to build:** 出口单证完整前端交付，落 `luna-ui/src/views/lunaerp-elmui/business/export-doc/` + API 镜像 `api/lunaerp-elmui/business/export-doc/`。**列表页**：关键词 + 状态 + 客户搜索；列：单证套号/买方/合同号/币种/Incoterm/状态 DictTag/创建时间/操作（查看、编辑[仅草稿]、定稿/回退、删除[仅草稿]、打印）；顶部三个新建入口"新建（空白）/从合同生成/从发货单生成"（后两者远程选单后调 from-* 接口预填进工作台）；右上"卖方出口资料"按钮（seller-config 权限）打开配置弹窗。**工作台编辑页**（el-tabs 3 Tab）：共享抬头（买卖双方、条款、港口、唛头、备注；卖方抬头区旁"使用租户默认配置"一键带出）→ 货物明细（可编辑表格：中英品名/HS/规格/数量/单价/金额/净毛重/箱数/体积，服务端重算行金额 + 底部汇总）→ 单据页签（PI/CI/PL 三子 Tab，各编辑专属 meta + 打印按钮）。DRAFT 工作台提供"重新同步"按钮（调 resync-from-source，二次确认提示"仅补充空白内容，不覆盖已填内容"，MANUAL 来源禁用并提示）。打印按钮 DRAFT/FINALIZED 两态均显示，点击后 PrintDispatcher 选 PI/CI/PL 版式预览。工作台走前端隐藏路由（仿发货工作台）。前端不做单测（ADR-0001）。

**Blocked by:** 03, 04, 05, 06, 07, 08

**Status:** ready-for-human

- [x] 列表页：关键词+状态+客户搜索；列：单证套号/买方/合同号/币种/Incoterm/状态/创建时间/操作（查看、编辑[仅草稿]、定稿/回退、删除[仅草稿]、打印）
- [x] 三个新建入口：空白/从合同生成/从发货单生成（后两者远程选单调 from-* 接口预填进工作台）
- [x] 右上"卖方出口资料"按钮 + 配置弹窗（调 04 的 seller-config 端点）
- [x] 工作台编辑页 3 Tab：共享抬头（含"使用租户默认配置"一键带出）/ 货物明细可编辑表格（服务端重算行金额+底部汇总）/ 单据页签 PI/CI/PL 三子 Tab（各编辑专属 meta + 打印按钮）
- [x] "重新同步"按钮二次确认（MANUAL 禁用并提示）；调 resync-from-source
- [x] 打印按钮 DRAFT/FINALIZED 两态均显示，PrintDispatcher 选版式预览
- [x] 工作台前端隐藏路由（仿发货工作台）；API 镜像 `api/lunaerp-elmui/business/export-doc/`

