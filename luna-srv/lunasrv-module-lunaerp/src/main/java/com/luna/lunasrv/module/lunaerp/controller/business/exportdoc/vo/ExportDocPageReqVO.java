package com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 业务管理-出口单证套分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExportDocPageReqVO extends PageParam {

    @Schema(description = "搜索关键词（套号/客户名称/合同号/发货单号模糊匹配）", example = "ED2026")
    private String keyword;

    @Schema(description = "单证状态（字典 lunaerp_export_doc_status：DRAFT/FINALIZED）", example = "DRAFT")
    private String status;

    @Schema(description = "买方客户ID", example = "1024")
    private Long customerId;

    @Schema(description = "来源合同号", example = "CH20260001")
    private String contractNo;

}
