# 04: 发货单/装箱清单打印迁移

**What to build:** 销售内勤在发货单上点打印，选择发货单或装箱清单版式，预览后送打印机（行为与现状一致，但内核替换为公共 composable、模板拆出注册到同一注册表）。将现有 `PrintShipmentDialog.vue` 的两套版式（发货单、装箱清单）拆为 `business/shipment/components/print/` 下两个模板组件 `SHIPMENT_DOC` 与 `PACKING_LIST`，打印内核替换为 03 号的公共 `usePrintHtml` composable，版式与行为不变；后端 shipment print-data 接口（独立打印 VO，豁免脱敏，沿用现有 `lunaerp:business:shipment:print` 权限码）+ `@LogRecord`；infra_config 种子 `luna.portal.base-url`（14 号门户上线前可空，发货单打印不阻断，仅二维码提示）。发货单二维码编码 `{luna.portal.base-url}/portal/shipment?no={发货单号}`（W0 只印，链接可暂不通，14 号接签收页）。

**Blocked by:** 03

**Status:** closed

- [x] `PrintShipmentDialog.vue` 两套版式拆为 `SHIPMENT_DOC` / `PACKING_LIST` 两个模板组件，落在 `business/shipment/components/print/`，注册到同一 `registry.ts`
- [x] 打印内核替换为 03 号公共 `usePrintHtml` composable，版式与行为不变（肉眼核对预览/送打印机一致）
- [x] 后端 `GET /lunaerp/business/shipment/{id}/print-data`（权限 `lunaerp:business:shipment:print`，沿用现有权限码）返回打印 VO（豁免脱敏），方法上加 `@LogRecord(type="打印", success="打印了发货单 {{#no}}")`
- [x] infra_config 种子 `luna.portal.base-url` 默认空（14 号前可空，不阻断发货单打印，仅二维码提示）
- [x] 发货单二维码编码 `{luna.portal.base-url}/portal/shipment?no={发货单号}`（W0 只印，链接可暂不通，14 号接签收页）
- [x] 测试：发货单/装箱清单打印行为/版式与迁移前一致；二维码码格式冻结；打印留痕
