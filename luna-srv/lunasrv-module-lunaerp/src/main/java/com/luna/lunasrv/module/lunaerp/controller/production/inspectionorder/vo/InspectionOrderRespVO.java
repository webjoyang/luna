package com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产管理-检验单 Response VO")
@Data
public class InspectionOrderRespVO {

    @Schema(description = "检验单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "检验单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "INS-20260921-001")
    private String code;

    @Schema(description = "检验类型（GREIGE 胚布检验 / FINISHED 成品检验）", requiredMode = Schema.RequiredMode.REQUIRED, example = "GREIGE")
    private String type;

    @Schema(description = "检验单状态（字典 lunaerp_inspection_order_status）", requiredMode = Schema.RequiredMode.REQUIRED, example = "DRAFT")
    private String status;

    @Schema(description = "关联产品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long productId;

    @Schema(description = "产品编码快照", example = "P20260901")
    private String productCode;

    @Schema(description = "产品名称快照", example = "全棉汗布")
    private String productName;

    @Schema(description = "关联胚布单ID", example = "201")
    private Long greigeOrderId;

    @Schema(description = "关联染色单ID", example = "301")
    private Long dyeingOrderId;

    @Schema(description = "承检方供应商ID", example = "401")
    private Long inspectionFactoryId;

    @Schema(description = "承检方公司名快照", example = "盛泽染整一厂")
    private String inspectionFactoryName;

    @Schema(description = "关联销售合同ID", example = "501")
    private Long salesContractId;

    @Schema(description = "合同号快照", example = "CH20260001")
    private String contractNo;

    @Schema(description = "目标重量 KG", example = "1200.50")
    private BigDecimal targetWeight;

    @Schema(description = "备注", example = "优先检验 A 等品")
    private String remark;

    @Schema(description = "单据日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-21")
    private LocalDate orderDate;

    @Schema(description = "已录卷数", example = "10")
    private Integer rollCount;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
