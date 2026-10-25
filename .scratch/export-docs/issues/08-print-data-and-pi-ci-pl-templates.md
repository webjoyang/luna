# 08: 打印（print-data + PI/CI/PL 三模板 + 二维码 + 签章）

**What to build:** 出口单证三单套打全链路接入 04 号统一打印内核（grilling Q4 定稿）。后端 `GET /{id}/print-data?template=PI|CI|PL` 返回**独立打印 VO**（豁免 @Sensitive 脱敏，含拼好的 qrUrl + 银行全量字段），方法上加 `@LogRecord(type="打印", ...)` 留痕入 system_operate_log；DRAFT/FINALIZED 均可调。前端三模板 `EXPORT_DOC_PI` / `EXPORT_DOC_CI` / `EXPORT_DOC_PL` + 共用 `ExportDocGoodsTable` 组件落在 `views/lunaerp-elmui/business/export-doc/components/print/`，注册到 W0 静态模板注册表 registry.ts，经 `<PrintDispatcher biz-type="EXPORT_DOC" :biz-id="id" />` 版式选择，PrintPaper/QrSlot 外壳 + usePrintHtml 打印。A4 纵向、内容宽 760px、Arial；结构照老系统三单同构（PI：PROFORMA INVOICE 标题 + To/PI No/Valid Until/Contract + Incoterm/港口/装运期 + 货物表+TOTAL + Payment(含定金%) + Beneficiary bank + Partial/Transshipment + Commission + Remarks；CI：COMMERCIAL INVOICE + Invoice No/Date/L/C No/Contract + 货物表 + Payment + 原产地声明(缺省 CHINA) + 真实性认证句；PL：PACKING LIST + PL No/Date/Contract + 唛头/港口 + 货物表(Packages/Qty/N.W./G.W./CBM+TOTAL) + Remark）。三单 Description 优先英文名回退中文名（nameEn||nameCn）。金额带币种符号。新增两处：① 底部签章区 `For and on behalf of {seller_name}` + 手签/公司章留白框 + 日期（三单同构）；② 二维码经 QrSlot 渲染右下角，内容 `{luna.portal.base-url}/portal/export-doc?no={doc_no}`，基址取 infra_config 键 `luna.portal.base-url`，未配置时不阻断打印、二维码位渲染"门户地址未配置"提示（与发货单同规则）。PL 的 TOTAL 行：显示 pl_meta 手填汇总（若填），否则服务端求和值——"手填优先、空则求和"。一期不做服务端 PDF/Excel 导出、不做整套 zip（浏览器打印即得 PDF）。

**Blocked by:** 03（DO/Service）, 04（卖方银行字段）, common-foundation 03（统一打印内核 PrintDispatcher/QrSlot/usePrintHtml/registry）

**Status:** ready-for-human

- [x] `GET /{id}/print-data?template=PI|CI|PL` 独立打印 VO（豁免脱敏、含拼好 qrUrl + 银行全量字段）+ `@LogRecord` 留痕；DRAFT/FINALIZED 均可调；权限 `lunaerp:business:export-doc:print`
- [x] 三模板 EXPORT_DOC_PI/CI/PL + 共用 ExportDocGoodsTable 落在 `components/print/`，注册进 registry.ts，经 PrintDispatcher 分发
- [x] A4 纵向 760px Arial 版式照老系统三单结构；Description 优先英文回退中文；金额带币种符号
- [x] 底部签章区 `For and on behalf of {seller_name}` + 留白 + 日期（三单同构）
- [x] 二维码编码完整 URL `{luna.portal.base-url}/portal/export-doc?no={doc_no}`；基址未配置时不阻断、渲染提示
- [x] PL TOTAL 行：pl_meta 手填汇总优先、空则服务端求和
- [x] 单测：print-data URL 拼接断言；基址未配置不报错且返回未配置标记；PL 汇总手填优先 vs 求和

