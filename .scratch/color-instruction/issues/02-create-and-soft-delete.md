# 02: 打色单新建、单号取号与软删

**What to build:** 用户点"新建打色单"进入整页表单，录入单据头（客户/联系人/产品/打色承接方/日期/要求完成日/备注）与多行颜色明细（客户色号/颜色名/厂内色号/色值/主辅灯源/色卡参考/样布照片/份数）后保存，列表中立即出现 PENDING 新单。编号 `DS+8位年月日+3位日流水`按天取号、含软删不释放（ADR-0006，无连字符风格，仿胚布单号/YP 单号）；可手填，撞号报 1_030_032_001；引用客户/产品/供应商不存在报 1_030_032_003；灯源非法报 1_030_032_007。列表可删除（仅 PENDING，级联逻辑删颜色行，编号不释放）。本期颜色行采用"删旧插新"简单保存，差异保存留 03 票。

**Blocked by:** 01 打色指示单地基：空列表可访问

**Status:** ready-for-agent

- [ ] 新建接口 POST `/lunaerp/production/color-instruction/create`（`:create` 权限）与"预览下一个编号"接口 GET `/get-next-no`（复用 `:query` 权限）；编号 `DS+yyyyMMdd+3位流水`按天取号，扫描含已删除单据（仿 ADR-0006 胚布单 Mapper 手写 SQL 绕过逻辑删除过滤）
- [ ] 手填编号与任意单据（含已删除）撞号报 1_030_032_001、不顺延；自动号并发撞号有限重试
- [ ] 主表字段齐备：customer/clientName/contactPerson/product/productCode/productName/labFactory/labFactoryName/composition/width/gsm/instructionDate（缺省今天）/deliveryDate/status（缺省 PENDING）/remark；保存客户/产品/承接方 id 与名称快照
- [ ] 引用校验：客户/产品/供应商不存在或不属本租户报 1_030_032_003；产品为强关联（同胚布单先例）；labFactory 可空=本厂打色房自做（不用占位供应商，仿 ADR-0022 自织先例）
- [ ] 颜色行简单保存（本期按 instruction_id 删旧插新 + 重排 lineNo，差异保存留 03 票）：每行 customerColorNo/colorName 必填、lightPrimary 必填（字典校验，非法报 1_030_032_007）、lightSecondary 可空、factoryColorNo/colorHex/colorCardRef/samplePhotos(JSON 数组)/copies/round(缺省 1)/status(缺省 PENDING)/rejectReason/remark 可空
- [ ] samplePhotos 走芋道 UploadImgs + infra 文件服务，`[{"name":"...","url":"..."}]` JSON 形态
- [ ] 软删接口 DELETE `/delete?id=`（`:delete` 权限）：仅 PENDING 可删（其他态报 1_030_032_002 状态不允许），事务内级联逻辑删除主表 + 颜色行；删除不存在报 1_030_032_000
- [ ] 前端编辑页 `edit.vue` 整页新建表单：客户选择后带联系人候选、产品强选产品档案带出编号/名称/成分/幅宽/克重快照可改、打色承接方供应商远程搜索可空、日期缺省今天；颜色行可编辑表格（行内取色器 colorHex、主辅灯源下拉、色卡参考文本、UploadImgs 多图、份数）；保存成功回列表
- [ ] 前端列表"新建"按钮 + 操作列"删除"按钮（`v-hasPermi` delete 码、danger、二次确认、仅 PENDING 行显示、删除后列表即时刷新）
- [ ] 测试覆盖：自动取号（max+1、当天从 001 起、跨天重置）、手填撞软删报错不顺延、引用校验四类、灯源非法、软删级联主表+颜色行、编号不重用、缺省值（日期/状态/round=1）
