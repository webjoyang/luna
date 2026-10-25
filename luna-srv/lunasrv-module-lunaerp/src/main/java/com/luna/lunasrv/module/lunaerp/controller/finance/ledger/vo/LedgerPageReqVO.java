package com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * 财务管理 - 经营分录账/资金流水分页 Request VO
 *
 * @author luna
 */
@Schema(description = "管理后台 - 资金流水分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LedgerPageReqVO extends PageParam {

    @Schema(description = "资金账户ID", example = "1001")
    private Long accountId;

    @Schema(description = "分录类别（字典 lunaerp_finance_ledger_category）", example = "OUT_EXPENSE")
    private String category;

    @Schema(description = "收支方向（DEBIT 借方流入 / CREDIT 贷方流出）", example = "CREDIT")
    private String direction;

    @Schema(description = "往来方类型（CUSTOMER / SUPPLIER）", example = "SUPPLIER")
    private String partyType;

    @Schema(description = "往来方ID", example = "2001")
    private Long partyId;

    @Schema(description = "往来主体名称", example = "张三")
    private String partyName;

    @Schema(description = "来源单号", example = "BX202610190001")
    private String refNo;

    @Schema(description = "期间开始日期", example = "2026-10-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodStart;

    @Schema(description = "期间结束日期", example = "2026-10-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate periodEnd;

    @Schema(description = "综合搜索关键字（模糊匹配 ref_no 或 party_name）", example = "BX2026")
    private String keyword;

}
