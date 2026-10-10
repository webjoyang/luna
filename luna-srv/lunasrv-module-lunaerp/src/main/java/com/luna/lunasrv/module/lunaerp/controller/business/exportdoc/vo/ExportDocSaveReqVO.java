package com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 业务管理-出口单证套创建/修改 Request VO")
@Data
public class ExportDocSaveReqVO {

    @Schema(description = "单证套ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "单证套号（留空时后端自动生成 ED{yyyy}{4位流水}）", example = "ED20260001")
    @Size(max = 32, message = "单证套号长度不能超过 32 位")
    private String docNo;

    @Schema(description = "来源类型（MANUAL 手工 / CONTRACT 合同 / SHIPMENT 发货单），缺省 MANUAL", example = "MANUAL")
    private String sourceType;

    @Schema(description = "来源合同ID", example = "101")
    private Long contractId;

    @Schema(description = "来源合同号快照", example = "CH20260001")
    private String contractNo;

    @Schema(description = "来源发货单ID", example = "201")
    private Long shipmentId;

    @Schema(description = "来源发货单号快照", example = "SH-20261010-001")
    private String shipmentNo;

    @Schema(description = "买方客户ID（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "买方客户名称快照", example = "ABC TRADING LLC")
    private String customerName;

    @Schema(description = "卖方英文名（抬头/签章，缺省从租户卖方配置带出）", example = "LUNA TEXTILE CO., LTD.")
    private String sellerName;

    @Schema(description = "卖方英文地址", example = "NO. 88 SILK ROAD, HANGZHOU, ZHEJIANG, CHINA")
    private String sellerAddress;

    @Schema(description = "卖方收款银行信息", example = "BANK OF CHINA HANGZHOU BRANCH A/C: 1234567890 SWIFT: BKCHCNBJ")
    private String sellerBankInfo;

    @Schema(description = "买方英文名", example = "ABC TRADING LLC")
    private String buyerName;

    @Schema(description = "买方英文地址", example = "123 MAIN ST, LOS ANGELES, CA, USA")
    private String buyerAddress;

    @Schema(description = "买方银行信息（随单手填）", example = "JPMORGAN CHASE BANK N.A.")
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

    @Schema(description = "币种（复用字典 lunaerp_sales_contract_currency），缺省 USD", example = "USD")
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

    @Schema(description = "货物明细列表（随套聚合保存整体替换）")
    @Valid
    private List<ExportDocItemSaveReqVO> items;

}
