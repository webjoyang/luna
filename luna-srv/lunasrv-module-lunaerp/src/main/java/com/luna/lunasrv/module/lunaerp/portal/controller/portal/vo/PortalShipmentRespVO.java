package com.luna.lunasrv.module.lunaerp.portal.controller.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "客户门户 - 发货单 Response VO")
@Data
public class PortalShipmentRespVO {

    @Schema(description = "发货单ID", example = "1001")
    private Long id;

    @Schema(description = "发货单号", example = "SH-20260927-001")
    private String shipmentNo;

    @Schema(description = "客户名称", example = "常州盛达纺织有限公司")
    private String customerName;

    @Schema(description = "发货日期", example = "2026-09-27")
    private LocalDate shippingDate;

    @Schema(description = "发货日期文本（前端兼容）", example = "2026-09-27")
    private String shipmentDate;

    @Schema(description = "收货地址", example = "江苏省常州市武进区高新产业园8号")
    private String receiveAddress;

    @Schema(description = "承运商/物流公司", example = "顺丰速运")
    private String carrier;

    @Schema(description = "物流公司别名（前端兼容）", example = "顺丰速运")
    private String logisticsCompany;

    @Schema(description = "物流单号", example = "SF123456789")
    private String trackingNumber;

    @Schema(description = "物流单号别名（前端兼容）", example = "SF123456789")
    private String logisticsNo;

    @Schema(description = "运费（金额透明，不脱敏）", example = "150.00")
    private BigDecimal freightFee;

    @Schema(description = "运费别名（前端兼容）", example = "150.00")
    private BigDecimal freightAmount;

    @Schema(description = "货物金额合计（金额透明，不脱敏）", example = "5000.00")
    private BigDecimal totalAmount;

    @Schema(description = "总匹数/卷数", example = "10")
    private Integer totalPieces;

    @Schema(description = "币种", example = "RMB")
    private String currency;

    @Schema(description = "签收状态（PENDING/SIGNED/REFUSED）", example = "PENDING")
    private String signStatus;

    @Schema(description = "签收人姓名", example = "李收货")
    private String signedByName;

    @Schema(description = "签收时间")
    private LocalDateTime signedAt;

    @Schema(description = "签收备注", example = "外包装完好，件数核对一致")
    private String signRemark;

    @Schema(description = "发货明细列表")
    private List<PortalShipmentItemRespVO> items;

}
