package com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-胚布单创建/修改 Request VO")
@Data
public class GreigeOrderSaveReqVO {

    @Schema(description = "胚布单ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "胚布单号（留空时后端自动生成 PB+月日+3位日流水，按天取号见 ADR-0006）", example = "PB0919001")
    @Size(max = 32, message = "胚布单号长度不能超过 32 位")
    private String orderNo;

    @Schema(description = "销售合同ID（远程选择销售合同，可空）", example = "2048")
    private Long salesContractId;

    @Schema(description = "合同号快照（为空时后端用合同档案合同号回填）", example = "CH20260001")
    @Size(max = 32, message = "合同号长度不能超过 32 位")
    private String contractNo;

    @Schema(description = "打色指示单ID（远程选择打色单，可空）", example = "5001")
    private Long colorInstructionId;

    @Schema(description = "打色指示单号快照（选中打色单时保存，可空）", example = "DS20261007001")
    @Size(max = 32, message = "打色单号长度不能超过 32 位")
    private String colorInstructionNo;

    @Schema(description = "承织坯布厂供应商ID（供应商远程选择，外发 OUTSOURCE 必填、自织 SELF 可空，见 ADR-0007 / ADR-0022）", example = "3072")
    private Long greigeFactoryId;

    @Schema(description = "坯布厂公司名快照（为空时后端用供应商档案公司名称回填）", example = "海宁坯布厂")
    @Size(max = 128, message = "坯布厂公司名长度不能超过 128 位")
    private String greigeFactoryName;

    @Schema(description = "指定染厂供应商ID（供应商远程选择，可空，引用不做类型强过滤见 ADR-0007）", example = "3073")
    private Long dyeingFactoryId;

    @Schema(description = "染厂公司名快照（为空时后端用供应商档案公司名称回填）", example = "嘉兴染整厂")
    @Size(max = 128, message = "染厂公司名长度不能超过 128 位")
    private String dyeingFactoryName;

    @Schema(description = "单据日期（缺省当天）", example = "2026-09-19")
    private LocalDate orderDate;

    @Schema(description = "交货日期", example = "2026-10-19")
    private LocalDate deliveryDate;

    @Schema(description = "产品档案ID（远程搜索产品档案，强关联，用于纱线测算查配比BOM，可空）", example = "4096")
    private Long productId;

    @Schema(description = "产品编号（可远程搜索产品档案回填后再改）", example = "P001")
    @Size(max = 64, message = "产品编号长度不能超过 64 位")
    private String productCode;

    @Schema(description = "产品名称", example = "全棉汗布")
    @Size(max = 128, message = "产品名称长度不能超过 128 位")
    private String productName;

    @Schema(description = "成分", example = "95棉5氨")
    @Size(max = 128, message = "成分长度不能超过 128 位")
    private String composition;

    @Schema(description = "幅宽（保留文本，兼容\"160cm 有效\"等写法）", example = "160cm 有效")
    @Size(max = 64, message = "幅宽长度不能超过 64 位")
    private String width;

    @Schema(description = "克重（保留文本）", example = "180g")
    @Size(max = 64, message = "克重长度不能超过 64 位")
    private String gsm;

    @Schema(description = "缩水率（描述性文本）", example = "≤5%")
    @Size(max = 64, message = "缩水率长度不能超过 64 位")
    private String shrinkage;

    @Schema(description = "匹重 KG（缺省 25）", example = "25.00")
    private BigDecimal pieceWeight;

    @Schema(description = "匹数（前端按 ceil(投胚量÷匹重) 自动算，可手改；服务端不重算）", example = "40")
    private Integer pieceCount;

    @Schema(description = "计划投胚量 KG", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "计划投胚量不能为空")
    private BigDecimal plannedQty;

    @Schema(description = "下单成品数量 KG", example = "950.00")
    private BigDecimal finishedQty;

    @Schema(description = "预设损耗（小数，0.05 = 5%，缺省 0）", example = "0.05")
    private BigDecimal suggestedLoss;

    @Schema(description = "加工费", example = "3000.00")
    private BigDecimal processingFee;

    @Schema(description = "所属胚布品种（档案 id 为字符串，手选不自动识别，可空）", example = "12")
    @Size(max = 64, message = "胚布品种标识长度不能超过 64 位")
    private String greigeTypeId;

    @Schema(description = "生产工艺备注")
    private String productionRemark;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "胚布单状态（字典 lunaerp_greige_order_status），缺省 ACTIVE", example = "ACTIVE")
    private String status;

    @Schema(description = "生产模式（字典 lunaerp_greige_production_mode：SELF 自织 / OUTSOURCE 外发，缺省 OUTSOURCE）", example = "OUTSOURCE")
    private String productionMode;

    @Schema(description = "颜色投胚分配行列表（整体替换：服务端按数组顺序重排 line_no、"
            + "权威重算投胚需求 = 成品量÷(1−损耗率)；为 null/空表示零颜色单据，合法）")
    @Valid
    private List<GreigeOrderColorSaveReqVO> colors;

}
