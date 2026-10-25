package com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务管理 - 出口单证打印数据 Response VO（豁免脱敏）
 *
 * @author luna
 */
@Schema(description = "管理后台 - 业务管理 - 出口单证打印数据 Response VO")
@Data
public class ExportDocPrintRespVO {

    @Schema(description = "单证套ID", example = "1024")
    private Long id;

    @Schema(description = "单证套号", example = "ED20260001")
    private String docNo;

    @Schema(description = "单据状态", example = "DRAFT")
    private String status;

    @Schema(description = "来源类型", example = "CONTRACT")
    private String sourceType;

    @Schema(description = "销售合同ID", example = "100")
    private Long contractId;

    @Schema(description = "销售合同号", example = "SC20260001")
    private String contractNo;

    @Schema(description = "发货单ID", example = "200")
    private Long shipmentId;

    @Schema(description = "发货单号", example = "SH-20261010-001")
    private String shipmentNo;

    @Schema(description = "客户ID", example = "1")
    private Long customerId;

    @Schema(description = "客户名称快照", example = "杭州某外贸公司")
    private String customerName;

    @Schema(description = "卖方英文名称", example = "LUNA TEXTILE CO., LTD.")
    private String sellerName;

    @Schema(description = "卖方英文地址", example = "No. 88 Luna Industrial Park, Hangzhou, China")
    private String sellerAddress;

    @Schema(description = "卖方收款银行信息汇总", example = "BANK OF CHINA HANGZHOU BRANCH A/C: 123456 SWIFT: BKCHCNBJ")
    private String sellerBankInfo;

    @Schema(description = "卖方银行名称")
    private String sellerBankName;

    @Schema(description = "卖方银行账号")
    private String sellerBankAccount;

    @Schema(description = "卖方银行SWIFT码")
    private String sellerBankSwift;

    @Schema(description = "卖方银行地址")
    private String sellerBankAddress;

    @Schema(description = "买方英文名称", example = "ACME OVERSEAS TRADING LLC")
    private String buyerName;

    @Schema(description = "买方英文地址", example = "100 Broadway St, New York, USA")
    private String buyerAddress;

    @Schema(description = "买方银行信息")
    private String buyerBankInfo;

    @Schema(description = "贸易术语", example = "FOB")
    private String incoterm;

    @Schema(description = "起运港", example = "SHANGHAI")
    private String portOfLoading;

    @Schema(description = "目的港", example = "LOS ANGELES")
    private String portOfDischarge;

    @Schema(description = "目的国", example = "USA")
    private String countryOfDest;

    @Schema(description = "运输方式", example = "SEA")
    private String transportMode;

    @Schema(description = "付款方式", example = "TT")
    private String paymentTerm;

    @Schema(description = "币种", example = "USD")
    private String currency;

    @Schema(description = "唛头 Shipping Mark")
    private String shippingMark;

    @Schema(description = "PI专属元数据")
    private String piMeta;

    @Schema(description = "CI专属元数据")
    private String ciMeta;

    @Schema(description = "PL专属元数据")
    private String plMeta;

    @Schema(description = "整单备注")
    private String remarks;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "完整客户门户验真二维码 URL（未配置时为 null）")
    private String qrUrl;

    @Schema(description = "货物明细列表")
    private List<ExportDocItemRespVO> items;

    // ==================== 汇总合计（手填优先、空则服务端求和） ====================

    @Schema(description = "合计数量")
    private BigDecimal totalQuantity;

    @Schema(description = "合计金额")
    private BigDecimal totalAmount;

    @Schema(description = "合计净重 KG")
    private BigDecimal totalNetWeight;

    @Schema(description = "合计毛重 KG")
    private BigDecimal totalGrossWeight;

    @Schema(description = "合计箱数/件数")
    private Integer totalPackages;

    @Schema(description = "合计体积 CBM")
    private BigDecimal totalVolume;

    @Schema(description = "合计是否来自 plMeta 手填覆盖")
    private Boolean totalCustom;

}
