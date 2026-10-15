package com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 库存管理 - 入库单 Response VO")
@Data
public class StockInRespVO {

    @Schema(description = "入库单ID", example = "1024")
    private Long id;

    @Schema(description = "入库单号（IN-YYYYMMDD-NNN）", example = "IN-20260924-001")
    private String inNo;

    @Schema(description = "入库类型（字典 lunaerp_stock_in_type：PURCHASE/PRODUCTION/RETURN/OTHER）", example = "PURCHASE")
    private String inType;

    @Schema(description = "入库单状态（字典 lunaerp_stock_in_status：DRAFT/COMPLETED/CANCELLED）", example = "DRAFT")
    private String status;

    @Schema(description = "入库仓库ID", example = "100")
    private Long warehouseId;

    @Schema(description = "仓库名称快照", example = "柯桥一号仓")
    private String warehouseName;

    @Schema(description = "供应商ID", example = "200")
    private Long supplierId;

    @Schema(description = "供应商名称快照", example = "盛泽纺织厂")
    private String supplierName;

    @Schema(description = "关联合同ID", example = "300")
    private Long salesContractId;

    @Schema(description = "关联合同号快照", example = "CH20260001")
    private String contractNo;

    @Schema(description = "关联胚布单ID", example = "400")
    private Long greigeOrderId;

    @Schema(description = "关联胚布单号快照", example = "PB0924001")
    private String greigeOrderNo;

    @Schema(description = "关联染色单ID", example = "500")
    private Long dyeingOrderId;

    @Schema(description = "关联胚布品种ID", example = "600")
    private Long greigeTypeId;

    @Schema(description = "胚布品种名称快照", example = "40S 纯棉单面")
    private String greigeTypeName;

    @Schema(description = "关联检验单ID", example = "700")
    private Long inspectionOrderId;

    @Schema(description = "关联检验单号快照", example = "INS-20260927-001")
    private String inspectionOrderNo;

    @Schema(description = "关联纱线采购单ID", example = "800")
    private Long yarnOrderId;

    @Schema(description = "关联纱线采购单号快照", example = "YO-20261015-001")
    private String yarnOrderNo;

    @Schema(description = "金额合计（明细金额之和，服务端重算）", example = "50000.00")
    private BigDecimal totalAmount;

    @Schema(description = "单据日期", example = "2026-09-24")
    private LocalDate inDate;

    @Schema(description = "备注", example = "大货入库")
    private String remark;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "入库明细列表")
    private List<StockInItemRespVO> items;

}
