package com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 业务管理-销售合同 Response VO（不含商品明细）")
@Data
public class SalesContractRespVO {

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CH20260001")
    private String contractNo;

    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long customerId;

    @Schema(description = "客户名称快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "杭州织造有限公司")
    private String clientName;

    @Schema(description = "客户联系人", example = "张三")
    private String clientContact;

    @Schema(description = "客户地址", example = "浙江省杭州市余杭区")
    private String clientAddress;

    @Schema(description = "客户合同号", example = "KH-2026-001")
    private String clientContractNo;

    @Schema(description = "业务员用户ID", example = "1")
    private Long salespersonUserId;

    @Schema(description = "签订日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-18")
    private LocalDate signDate;

    @Schema(description = "交货日期", example = "2026-10-18")
    private LocalDate deliveryDate;

    @Schema(description = "单位（字典 lunaerp_sales_contract_unit）", example = "KG")
    private String unit;

    @Schema(description = "币种（字典 lunaerp_sales_contract_currency）", example = "RMB")
    private String currency;

    @Schema(description = "贸易类型（字典 lunaerp_sales_contract_trade_type）", example = "DOMESTIC")
    private String tradeType;

    @Schema(description = "订单属性（字典 lunaerp_sales_contract_order_type）", example = "BULK")
    private String orderType;

    @Schema(description = "总数量（本票固定 0，02 票接管重算）", example = "0")
    private BigDecimal totalQuantity;

    @Schema(description = "总金额（本票固定 0，02 票接管重算）", example = "0")
    private BigDecimal totalAmount;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "合同状态（字典 lunaerp_sales_contract_status）", example = "ACTIVE")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
