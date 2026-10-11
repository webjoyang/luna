package com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-染色单创建/修改 Request VO")
@Data
public class DyeingOrderSaveReqVO {

    @Schema(description = "染色单ID（修改时必填）", example = "1024")
    private Long id;

    @Schema(description = "销售合同ID（远程选择销售合同，必选）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "销售合同不能为空")
    private Long salesContractId;

    @Schema(description = "合同号快照（为空时后端用合同档案合同号回填）", example = "CH20260001")
    @Size(max = 32, message = "合同号长度不能超过 32 位")
    private String contractNo;

    @Schema(description = "客户ID（从合同带出）", example = "1001")
    private Long customerId;

    @Schema(description = "客户名快照（从合同带出）", example = "杭州优衣纺织有限公司")
    @Size(max = 128, message = "客户名称长度不能超过 128 位")
    private String clientName;

    @Schema(description = "承染方供应商ID（供应商远程选择，必选）", requiredMode = Schema.RequiredMode.REQUIRED, example = "3072")
    @NotNull(message = "承染方不能为空")
    private Long dyeingFactoryId;

    @Schema(description = "承染方公司名快照（为空时后端用供应商档案公司名称回填）", example = "海宁染整厂")
    @Size(max = 128, message = "承染方公司名长度不能超过 128 位")
    private String dyeingFactoryName;

    @Schema(description = "单据日期（缺省当天）", example = "2026-09-19")
    private LocalDate orderDate;

    @Schema(description = "交货日期", example = "2026-10-19")
    private LocalDate deliveryDate;

    @Schema(description = "产品编码", example = "P001")
    @Size(max = 64, message = "产品编码长度不能超过 64 位")
    private String productCode;

    @Schema(description = "产品名称", example = "全棉汗布")
    @Size(max = 128, message = "产品名称长度不能超过 128 位")
    private String productName;

    @Schema(description = "成分", example = "95棉5氨")
    @Size(max = 128, message = "成分长度不能超过 128 位")
    private String composition;

    @Schema(description = "幅宽", example = "160cm 有效")
    @Size(max = 64, message = "幅宽长度不能超过 64 位")
    private String width;

    @Schema(description = "克重", example = "180g")
    @Size(max = 64, message = "克重长度不能超过 64 位")
    private String gsm;

    @Schema(description = "缩水率", example = "≤5%")
    @Size(max = 64, message = "缩水率长度不能超过 64 位")
    private String shrinkage;

    @Schema(description = "损耗率", example = "5%")
    @Size(max = 64, message = "损耗率长度不能超过 64 位")
    private String loss;

    @Schema(description = "pH 值", example = "6.5")
    @Size(max = 64, message = "pH 值长度不能超过 64 位")
    private String ph;

    @Schema(description = "水洗牢度", example = "4级")
    @Size(max = 64, message = "水洗牢度长度不能超过 64 位")
    private String fastnessWash;

    @Schema(description = "皂洗牢度", example = "4级")
    @Size(max = 64, message = "皂洗牢度长度不能超过 64 位")
    private String fastnessSoap;

    @Schema(description = "干摩擦牢度", example = "4级")
    @Size(max = 64, message = "干摩擦牢度长度不能超过 64 位")
    private String fastnessDryRub;

    @Schema(description = "湿摩擦牢度", example = "3-4级")
    @Size(max = 64, message = "湿摩擦牢度长度不能超过 64 位")
    private String fastnessWetRub;

    @Schema(description = "日晒牢度", example = "4级")
    @Size(max = 64, message = "日晒牢度长度不能超过 64 位")
    private String fastnessLight;

    @Schema(description = "汗渍牢度", example = "4级")
    @Size(max = 64, message = "汗渍牢度长度不能超过 64 位")
    private String fastnessPerspiration;

    @Schema(description = "干洗牢度", example = "4级")
    @Size(max = 64, message = "干洗牢度长度不能超过 64 位")
    private String fastnessDryClean;

    @Schema(description = "六千沾色牢度", example = "4级")
    @Size(max = 64, message = "六千沾色牢度长度不能超过 64 位")
    private String fastnessSixFiber;

    @Schema(description = "其他要求", example = "手感柔软，不起毛")
    private String otherReq;

    @Schema(description = "备注", example = "客户急单")
    private String remark;

    @Schema(description = "染色单状态（字典 lunaerp_dyeing_order_status），缺省 DRAFT", example = "DRAFT")
    private String status;

    @Schema(description = "打色指示单ID（逻辑关联可空）", example = "1024")
    private Long colorInstructionId;

    @Schema(description = "打色单号快照", example = "DS20261010001")
    @Size(max = 32, message = "打色单号长度不能超过 32 位")
    private String colorInstructionNo;

    @Schema(description = "颜色明细列表（整体替换；为空表示零明细）")
    @Valid
    private List<DyeingColorSaveReqVO> colors;

}
