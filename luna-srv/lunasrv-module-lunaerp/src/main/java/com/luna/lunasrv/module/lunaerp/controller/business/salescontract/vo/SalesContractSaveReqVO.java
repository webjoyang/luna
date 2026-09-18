package com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 业务管理-销售合同创建/修改 Request VO")
@Data
public class SalesContractSaveReqVO {

    @Schema(description = "合同ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "合同编号（留空时后端自动生成 CH+年份+4位顺序号）", example = "CH20260001")
    @Size(max = 32, message = "合同编号长度不能超过 32 位")
    private String contractNo;

    @Schema(description = "客户ID（客户档案远程选择）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "客户名称快照（为空时后端用客户档案公司名称回填）", example = "杭州织造有限公司")
    @Size(max = 128, message = "客户名称长度不能超过 128 位")
    private String clientName;

    @Schema(description = "客户联系人", example = "张三")
    private String clientContact;

    @Schema(description = "客户地址", example = "浙江省杭州市余杭区")
    private String clientAddress;

    @Schema(description = "客户合同号", example = "KH-2026-001")
    private String clientContractNo;

    @Schema(description = "业务员用户ID（系统用户远程选择，可空）", example = "1")
    private Long salespersonUserId;

    @Schema(description = "签订日期（缺省当天）", example = "2026-09-18")
    private LocalDate signDate;

    @Schema(description = "交货日期", example = "2026-10-18")
    private LocalDate deliveryDate;

    @Schema(description = "单位（字典 lunaerp_sales_contract_unit），缺省 KG", example = "KG")
    private String unit;

    @Schema(description = "币种（字典 lunaerp_sales_contract_currency），缺省 RMB", example = "RMB")
    private String currency;

    @Schema(description = "贸易类型（字典 lunaerp_sales_contract_trade_type），缺省 DOMESTIC", example = "DOMESTIC")
    private String tradeType;

    @Schema(description = "订单属性（字典 lunaerp_sales_contract_order_type），缺省 BULK", example = "BULK")
    private String orderType;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "合同状态（字典 lunaerp_sales_contract_status），缺省 ACTIVE", example = "ACTIVE")
    private String status;

}
