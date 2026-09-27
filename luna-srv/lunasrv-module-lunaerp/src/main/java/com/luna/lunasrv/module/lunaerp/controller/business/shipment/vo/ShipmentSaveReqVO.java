package com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 业务管理 - 发货单创建/修改 Request VO")
@Data
public class ShipmentSaveReqVO {

    @Schema(description = "发货单ID", example = "10")
    private Long id;

    @Schema(description = "发货单号（SH-YYYYMMDD-NNN，为空自动按天生成）", example = "SH-20260927-001")
    private String shipmentNo;

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "销售合同ID", example = "200")
    private Long salesContractId;

    @Schema(description = "发货仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "300")
    @NotNull(message = "发货仓库不能为空")
    private Long warehouseId;

    @Schema(description = "发货地址", example = "浙江省绍兴市柯桥区XX路1号")
    private String shippingAddress;

    @Schema(description = "收货地址", example = "广东省广州市海珠区中大纺织城")
    private String receiveAddress;

    @Schema(description = "承运商/物流公司", example = "顺丰速运")
    private String carrier;

    @Schema(description = "物流单号", example = "SF123456789")
    private String trackingNumber;

    @Schema(description = "发货日期", example = "2026-09-27")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate shippingDate;

    @Schema(description = "预计到货日期", example = "2026-09-30")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expectedDeliveryDate;

    @Schema(description = "运费（独立于货物金额）", example = "150.00")
    private BigDecimal freightFee;

    @Schema(description = "业务员用户ID", example = "1")
    private Long salespersonUserId;

    @Schema(description = "币种（字典 lunaerp_sales_contract_currency：RMB/USD/EUR/HKD）", example = "RMB")
    private String currency;

    @Schema(description = "发货备注", example = "加急快件")
    private String remark;

    @Schema(description = "发货明细列表")
    @Valid
    private List<ShipmentItemSaveReqVO> items;

}
