package com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 业务管理-出口单证套 Response VO")
@Data
public class ExportDocRespVO {

    @Schema(description = "单证套ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "单证套号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ED20260001")
    private String docNo;

    @Schema(description = "单证状态（字典 lunaerp_export_doc_status：DRAFT/FINALIZED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "DRAFT")
    private String status;

    @Schema(description = "来源类型（MANUAL 手工 / CONTRACT 合同 / SHIPMENT 发货单）", requiredMode = Schema.RequiredMode.REQUIRED, example = "MANUAL")
    private String sourceType;

    @Schema(description = "来源合同ID", example = "101")
    private Long contractId;

    @Schema(description = "来源合同号快照", example = "CH20260001")
    private String contractNo;

    @Schema(description = "来源发货单ID", example = "201")
    private Long shipmentId;

    @Schema(description = "来源发货单号快照", example = "SH-20261010-001")
    private String shipmentNo;

    @Schema(description = "买方客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long customerId;

    @Schema(description = "买方客户名称快照", example = "ABC TRADING LLC")
    private String customerName;

    @Schema(description = "卖方英文名", example = "LUNA TEXTILE CO., LTD.")
    private String sellerName;

    @Schema(description = "卖方英文地址", example = "NO. 88 SILK ROAD, HANGZHOU, ZHEJIANG, CHINA")
    private String sellerAddress;

    @Schema(description = "卖方收款银行信息", example = "BANK OF CHINA HANGZHOU BRANCH A/C: 1234567890 SWIFT: BKCHCNBJ")
    private String sellerBankInfo;

    @Schema(description = "买方英文名", example = "ABC TRADING LLC")
    private String buyerName;

    @Schema(description = "买方英文地址", example = "123 MAIN ST, LOS ANGELES, CA, USA")
    private String buyerAddress;

    @Schema(description = "买方银行信息", example = "JPMORGAN CHASE BANK N.A.")
    private String buyerBankInfo;

    @Schema(description = "贸易术语（字典 lunaerp_incoterm：FOB/CIF/CFR/EXW）", example = "FOB")
    private String incoterm;

    @Schema(description = "起运港", example = "SHANGHAI")
    private String portOfLoading;

    @Schema(description = "目的港", example = "LOS ANGELES")
    private String portOfDischarge;

    @Schema(description = "目的国", example = "USA")
    private String countryOfDest;

    @Schema(description = "运输方式（字典 lunaerp_transport_mode：SEA/AIR/LAND/EXPRESS）", example = "SEA")
    private String transportMode;

    @Schema(description = "付款方式（字典 lunaerp_payment_term：TT/LC/DP/DA/OA）", example = "TT")
    private String paymentTerm;

    @Schema(description = "币种（复用字典 lunaerp_sales_contract_currency）", example = "USD")
    private String currency;

    @Schema(description = "唛头", example = "N/M")
    private String shippingMark;

    @Schema(description = "形式发票 PI 专属元数据 JSON", example = "{\"validUntil\":\"2026-11-01\"}")
    private String piMeta;

    @Schema(description = "商业发票 CI 专属元数据 JSON", example = "{\"originDeclaration\":\"COUNTRY OF ORIGIN: CHINA\"}")
    private String ciMeta;

    @Schema(description = "装箱单 PL 专属元数据 JSON", example = "{\"totalPackages\":10}")
    private String plMeta;

    @Schema(description = "备注", example = "出口报关单证")
    private String remarks;

    @Schema(description = "货物明细列表（按 line_no 升序）")
    private List<ExportDocItemRespVO> items;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
