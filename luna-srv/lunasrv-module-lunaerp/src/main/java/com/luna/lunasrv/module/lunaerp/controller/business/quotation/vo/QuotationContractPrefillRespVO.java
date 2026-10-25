package com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 报价单转销售合同预填 Response VO")
@Data
public class QuotationContractPrefillRespVO {

    @Schema(description = "来源报价单ID", example = "1024")
    private Long sourceQuotationId;

    @Schema(description = "来源报价单号快照（带版本号）", example = "QT20260001-v1")
    private String sourceQuotationNo;

    @Schema(description = "客户ID", example = "2048")
    private Long customerId;

    @Schema(description = "客户名称快照", example = "示例面料商")
    private String clientName;

    @Schema(description = "客户联系人", example = "张三")
    private String clientContact;

    @Schema(description = "联系电话", example = "13800000000")
    private String phone;

    @Schema(description = "币种（锁定不可改）", example = "RMB")
    private String currency;

    @Schema(description = "贸易类型（缺省内销 DOMESTIC）", example = "DOMESTIC")
    private String tradeType;

    @Schema(description = "业务员用户ID（继承报价单业务员，停用则回退登录用户）", example = "1")
    private Long salespersonUserId;

    @Schema(description = "归属部门ID（根据业务员重新推导）", example = "10")
    private Long deptId;

    @Schema(description = "是否已失效警告（validTo < 当前日期）", example = "false")
    private Boolean expiredWarning;

    @Schema(description = "预填明细商品列表（不含任何成本/毛利字段）")
    private List<QuotationContractPrefillItemRespVO> items;

}
