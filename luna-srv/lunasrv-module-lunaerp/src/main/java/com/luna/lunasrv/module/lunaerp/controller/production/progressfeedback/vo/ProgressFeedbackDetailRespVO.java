package com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产管理 - 进度反馈缸详情 Response VO
 *
 * 聚合缸字段 + 五阶段日期 + 颜色行/染色单实时信息（零快照） + 延期与覆盖率派生指标
 *
 * @author luna
 */
@Schema(description = "生产管理 - 进度反馈缸详情 Response VO")
@Data
public class ProgressFeedbackDetailRespVO {

    // ==================== 反馈缸基础字段 ====================

    @Schema(description = "反馈缸ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long id;

    @Schema(description = "染色单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long dyeingOrderId;

    @Schema(description = "染色颜色行ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    private Long dyeingColorId;

    @Schema(description = "缸号", requiredMode = Schema.RequiredMode.REQUIRED, example = "VAT2026-001")
    private String vatNo;

    @Schema(description = "本缸数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    private BigDecimal quantity;

    @Schema(description = "匹数", example = "20")
    private Integer pieces;

    @Schema(description = "进度状态（SCHEDULED/DYEING/FIXING/QUALITY_CHECK/COMPLETED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "SCHEDULED")
    private String status;

    @Schema(description = "排缸日期（阶段一日期戳）", example = "2026-09-28")
    private LocalDate scheduledDate;

    @Schema(description = "染色开始日期（阶段二日期戳）", example = "2026-09-29")
    private LocalDate dyeingStartedDate;

    @Schema(description = "固色开始日期（阶段三日期戳）", example = "2026-09-30")
    private LocalDate fixingStartedDate;

    @Schema(description = "质检日期（阶段四日期戳）", example = "2026-10-01")
    private LocalDate qualityCheckDate;

    @Schema(description = "完成日期（阶段五日期戳）", example = "2026-10-02")
    private LocalDate completedDate;

    @Schema(description = "延期原因", example = "等原料")
    private String delayReason;

    @Schema(description = "备注", example = "加急")
    private String remark;

    @Schema(description = "创建时间", example = "2026-09-28 10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "创建人", example = "admin")
    private String creator;

    @Schema(description = "更新时间", example = "2026-09-28 12:00:00")
    private LocalDateTime updateTime;

    @Schema(description = "更新人", example = "admin")
    private String updater;

    // ==================== 染色单与颜色行实时字段（零快照） ====================

    @Schema(description = "销售合同ID", example = "501")
    private Long salesContractId;

    @Schema(description = "销售合同号", example = "HT-2026-001")
    private String contractNo;

    @Schema(description = "客户ID", example = "301")
    private Long customerId;

    @Schema(description = "客户名称", example = "广州盛达纺织")
    private String clientName;

    @Schema(description = "承染方供应商ID", example = "201")
    private Long dyeingFactoryId;

    @Schema(description = "承染方供应商名称", example = "江阴市恒丰印染有限公司")
    private String dyeingFactoryName;

    @Schema(description = "染色单交货日期", example = "2026-10-05")
    private LocalDate deliveryDate;

    @Schema(description = "产品编号", example = "PRD-2026-001")
    private String productCode;

    @Schema(description = "产品名称", example = "纯棉斜纹布")
    private String productName;

    @Schema(description = "染色单状态（ACTIVE/COMPLETED）", example = "ACTIVE")
    private String dyeingOrderStatus;

    @Schema(description = "颜色明细行号", example = "1")
    private Integer lineNo;

    @Schema(description = "客户色号", example = "PANTONE 19-4052")
    private String customerColorNo;

    @Schema(description = "颜色名称", example = "经典蓝")
    private String colorName;

    @Schema(description = "颜色行染色需求数量", example = "1000.00")
    private BigDecimal colorQuantity;

    @Schema(description = "颜色行需求匹数", example = "40")
    private Integer colorPieces;

    @Schema(description = "染色输入单位（KG/M）", example = "KG")
    private String unit;

    // ==================== 派生计算指标 ====================

    @Schema(description = "是否延期（染色单交货日期 < 今天 且缸非 COMPLETED）", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean delayed;

    @Schema(description = "未决问题数", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer unresolvedIssueCount;

    @Schema(description = "颜色行下已建缸总数量", example = "800.00")
    private BigDecimal totalVatQuantity;

    @Schema(description = "颜色行下已建缸总匹数", example = "32")
    private Integer totalVatPieces;

    @Schema(description = "排产覆盖率（%）", example = "80.00")
    private BigDecimal coveragePercent;

    @Schema(description = "是否超排（总缸数量 > 需求数量）", example = "false")
    private Boolean overScheduled;

}
