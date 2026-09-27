package com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 业务管理 - 发货单 Response VO")
@Data
public class ShipmentRespVO {

    @Schema(description = "发货单ID", example = "10")
    private Long id;

    @Schema(description = "发货单号（SH-YYYYMMDD-NNN）", example = "SH-20260927-001")
    private String shipmentNo;

    @Schema(description = "发货状态（字典 lunaerp_shipment_status：DRAFT/COMPLETED/CANCELLED）", example = "DRAFT")
    private String status;

    @Schema(description = "客户ID", example = "100")
    private Long customerId;

    @Schema(description = "客户名称快照", example = "广州某某服饰有限公司")
    private String customerName;

    @Schema(description = "销售合同ID", example = "200")
    private Long salesContractId;

    @Schema(description = "销售合同号快照", example = "CH20260001")
    private String contractNo;

    @Schema(description = "发货仓库ID", example = "300")
    private Long warehouseId;

    @Schema(description = "发货仓库名称快照", example = "柯桥成品一号仓")
    private String warehouseName;

    @Schema(description = "发货地址", example = "浙江省绍兴市柯桥区XX路1号")
    private String shippingAddress;

    @Schema(description = "收货地址", example = "广东省广州市海珠区中大纺织城")
    private String receiveAddress;

    @Schema(description = "承运商/物流公司", example = "顺丰速运")
    private String carrier;

    @Schema(description = "物流单号", example = "SF123456789")
    private String trackingNumber;

    @Schema(description = "发货日期", example = "2026-09-27")
    private LocalDate shippingDate;

    @Schema(description = "预计到货日期", example = "2026-09-30")
    private LocalDate expectedDeliveryDate;

    @Schema(description = "运费（独立于货物金额）", example = "150.00")
    private BigDecimal freightFee;

    @Schema(description = "货物金额合计（服务端重算）", example = "5000.00")
    private BigDecimal totalAmount;

    @Schema(description = "总匹数（服务端重算）", example = "10")
    private Integer totalPieces;

    @Schema(description = "业务员用户ID", example = "1")
    private Long salespersonUserId;

    @Schema(description = "业务员姓名快照", example = "张业务")
    private String salespersonName;

    @Schema(description = "币种（字典 lunaerp_sales_contract_currency：RMB/USD/EUR/HKD）", example = "RMB")
    private String currency;

    @Schema(description = "签收状态（字典 lunaerp_shipment_sign_status：PENDING/SIGNED/REFUSED）", example = "PENDING")
    private String signStatus;

    @Schema(description = "签收人姓名", example = "李收货")
    private String signedByName;

    @Schema(description = "签收时间")
    private LocalDateTime signedAt;

    @Schema(description = "签收备注", example = "外包装完好，件数核对一致")
    private String signRemark;

    @Schema(description = "发货备注", example = "加急快件")
    private String remark;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "发货明细列表")
    private List<ShipmentItemRespVO> items;

}
