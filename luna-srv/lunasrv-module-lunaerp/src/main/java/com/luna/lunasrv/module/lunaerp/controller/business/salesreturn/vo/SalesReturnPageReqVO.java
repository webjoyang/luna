package com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 业务管理 - 销售退货分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SalesReturnPageReqVO extends PageParam {

    @Schema(description = "客户ID", example = "200")
    private Long customerId;

    @Schema(description = "发货单号（支持模糊）", example = "SH-20261018")
    private String shipmentNo;

    @Schema(description = "业务状态（DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED）", example = "DRAFT")
    private String status;

    @Schema(description = "审批状态（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）", example = "PENDING")
    private String approvalStatus;

    @Schema(description = "综合关键字（退货单号或客户名称）", example = "盛泽")
    private String keyword;

    @Schema(description = "单据日期区间 [开始日期, 结束日期]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] returnDate;

}
