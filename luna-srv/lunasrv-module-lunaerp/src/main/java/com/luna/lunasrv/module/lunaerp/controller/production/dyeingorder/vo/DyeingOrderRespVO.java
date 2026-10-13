package com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 生产管理-染色单 Response VO")
@Data
public class DyeingOrderRespVO {

    @Schema(description = "染色单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "销售合同ID", example = "2048")
    private Long salesContractId;

    @Schema(description = "合同号快照", example = "CH20260001")
    private String contractNo;

    @Schema(description = "打色指示单ID", example = "5001")
    private Long colorInstructionId;

    @Schema(description = "打色指示单号快照", example = "DS20261007001")
    private String colorInstructionNo;

    @Schema(description = "客户ID", example = "1024")
    private Long customerId;

    @Schema(description = "客户名快照", example = "某某纺织有限公司")
    private String clientName;

    @Schema(description = "承染方供应商ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long dyeingFactoryId;

    @Schema(description = "承染方公司名快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "某某染厂")
    private String dyeingFactoryName;

    @Schema(description = "单据日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate orderDate;

    @Schema(description = "交货日期")
    private LocalDate deliveryDate;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

    @Schema(description = "产品名称", example = "全棉汗布")
    private String productName;

    @Schema(description = "成分", example = "100%棉")
    private String composition;

    @Schema(description = "幅宽", example = "185cm")
    private String width;

    @Schema(description = "克重", example = "180g")
    private String gsm;

    @Schema(description = "缩水率", example = "±3%")
    private String shrinkage;

    @Schema(description = "损耗率", example = "5%")
    private String loss;

    @Schema(description = "pH值", example = "6.5")
    private String ph;

    @Schema(description = "水洗牢度", example = "4级")
    private String fastnessWash;

    @Schema(description = "皂洗牢度", example = "4级")
    private String fastnessSoap;

    @Schema(description = "干摩擦牢度", example = "4级")
    private String fastnessDryRub;

    @Schema(description = "湿摩擦牢度", example = "3-4级")
    private String fastnessWetRub;

    @Schema(description = "日晒牢度", example = "4级")
    private String fastnessLight;

    @Schema(description = "汗渍牢度", example = "4级")
    private String fastnessPerspiration;

    @Schema(description = "干洗牢度", example = "4级")
    private String fastnessDryClean;

    @Schema(description = "六千沾色牢度", example = "4级")
    private String fastnessSixFiber;

    @Schema(description = "其他要求")
    private String otherReq;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "染色单状态：DRAFT/ACTIVE/COMPLETED/CANCELLED", requiredMode = Schema.RequiredMode.REQUIRED, example = "DRAFT")
    private String status;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "颜色明细列表（详情接口嵌套返回，按 line_no 排序；列表接口不返回）")
    private List<DyeingColorRespVO> colors;

}
