package com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 业务管理-销售合同明细创建/修改 Request VO")
@Data
public class SalesContractItemSaveReqVO {

    @Schema(description = "明细ID（更新时携带可空行 id，新增时留空）", example = "1024")
    private Long id;

    @Schema(description = "产品编号（本票手填输入，03 票接入产品库远程搜索）", example = "P001")
    @Size(max = 64, message = "产品编号长度不能超过 64 位")
    private String productCode;

    @Schema(description = "品名", example = "纯棉平布")
    @Size(max = 128, message = "品名长度不能超过 128 位")
    private String productName;

    @Schema(description = "客户色号", example = "RED-001")
    @Size(max = 64, message = "客户色号长度不能超过 64 位")
    private String customerColor;

    @Schema(description = "成分", example = "100%棉")
    @Size(max = 128, message = "成分长度不能超过 128 位")
    private String composition;

    @Schema(description = "门幅", example = "150cm")
    @Size(max = 64, message = "门幅长度不能超过 64 位")
    private String width;

    @Schema(description = "克重", example = "200g/m2")
    @Size(max = 64, message = "克重长度不能超过 64 位")
    private String weight;

    @Schema(description = "数量（null 按 0 处理）", example = "100.00")
    private BigDecimal quantity;

    @Schema(description = "单价（null 按 0 处理）", example = "15.50")
    private BigDecimal unitPrice;

    @Schema(description = "其他费用（null 按 0 处理）", example = "0.00")
    private BigDecimal otherFees;

    @Schema(description = "行备注", example = "加急")
    @Size(max = 256, message = "行备注长度不能超过 256 位")
    private String remark;

}
