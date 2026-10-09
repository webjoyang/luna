package com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.ticket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 织造布票打印标签 Response VO（独立 VO，豁免脱敏）")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeavingTicketPrintRespVO {

    @Schema(description = "布票ID", example = "1024")
    private Long id;

    @Schema(description = "布票编号（BT-{机台任务号去符号}-{4 位流水}）", example = "BT-MTZZPB09190010101-0001")
    private String code;

    @Schema(description = "关联织造单ID", example = "100")
    private Long weavingOrderId;

    @Schema(description = "织造单号", example = "ZZ-PB0919001-01")
    private String weavingOrderCode;

    @Schema(description = "关联机台任务ID", example = "200")
    private Long machineTaskId;

    @Schema(description = "机台任务号", example = "MT-ZZPB09190010101")
    private String machineTaskCode;

    @Schema(description = "机台号", example = "LM20260001")
    private String loomNo;

    @Schema(description = "品名", example = "40S双纱平纹汗布")
    private String productName;

    @Schema(description = "颜色", example = "原白")
    private String color;

    @Schema(description = "匹序", example = "1")
    private Integer pieceIndex;

    @Schema(description = "单匹计划数量/重量", example = "25.00")
    private BigDecimal quantity;

    @Schema(description = "单位", example = "KG")
    private String unit;

    @Schema(description = "挡车工姓名", example = "张三")
    private String workerName;

    @Schema(description = "原料批号", example = "YB20260901-01")
    private String materialBatchNo;

    @Schema(description = "二维码完整URL（{luna.h5.base-url}/h5/scan/weaving-ticket?code={布票code}）", example = "https://erp.example.com/h5/scan/weaving-ticket?code=BT-MTZZPB09190010101-0001")
    private String qrUrl;

    @Schema(description = "打印数据生成时间")
    private LocalDateTime printTime;

}
