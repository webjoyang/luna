package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo;

import cn.idev.excel.annotation.ExcelProperty;
import com.luna.lunasrv.framework.excel.core.annotations.DictFormat;
import com.luna.lunasrv.framework.excel.core.annotations.ExcelColumnSelect;
import com.luna.lunasrv.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理后台 - 客户 Excel 导入 VO
 *
 * @author luna
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "管理后台 - 客户 Excel 导入 VO")
public class CustomerImportVO {

    @ExcelProperty("企业名称")
    @Schema(description = "企业名称（必填，重名检查）")
    private String companyName;

    @ExcelProperty("简称")
    @Schema(description = "简称（为空时用企业名称回填）")
    private String shortName;

    @ExcelProperty(value = "客户类型", converter = DictConvert.class)
    @DictFormat("lunaerp_customer_client_type")
    @ExcelColumnSelect(dictType = "lunaerp_customer_client_type")
    @Schema(description = "客户类型（字典 lunaerp_customer_client_type）")
    private String clientType;

    @ExcelProperty(value = "行业", converter = DictConvert.class)
    @DictFormat("lunaerp_customer_industry")
    @ExcelColumnSelect(dictType = "lunaerp_customer_industry")
    @Schema(description = "所属行业（字典 lunaerp_customer_industry）")
    private String industry;

    @ExcelProperty(value = "规模", converter = DictConvert.class)
    @DictFormat("lunaerp_customer_scale")
    @ExcelColumnSelect(dictType = "lunaerp_customer_scale")
    @Schema(description = "企业规模（字典 lunaerp_customer_scale）")
    private String scale;

    @ExcelProperty(value = "来源", converter = DictConvert.class)
    @DictFormat("lunaerp_customer_source")
    @ExcelColumnSelect(dictType = "lunaerp_customer_source")
    @Schema(description = "客户来源（字典 lunaerp_customer_source）")
    private String source;

    @ExcelProperty(value = "合作状态", converter = DictConvert.class)
    @DictFormat("lunaerp_customer_cooperation_status")
    @ExcelColumnSelect(dictType = "lunaerp_customer_cooperation_status")
    @Schema(description = "合作状态（字典 lunaerp_customer_cooperation_status）")
    private String cooperationStatus;

    @ExcelProperty("统一社会信用代码")
    @Schema(description = "统一社会信用代码（18 位格式校验）")
    private String unifiedSocialCreditCode;

    @ExcelProperty("英文名称")
    @Schema(description = "英文名称")
    private String nameEn;

    @ExcelProperty("英文地址")
    @Schema(description = "英文地址")
    private String addressEn;

    @ExcelProperty("开票抬头")
    @Schema(description = "开票抬头（为空时用企业名称回填）")
    private String invoiceTitle;

    @ExcelProperty("开票地址")
    @Schema(description = "开票地址")
    private String invoiceAddress;

    @ExcelProperty("开户银行")
    @Schema(description = "开户银行")
    private String bankName;

    @ExcelProperty("开户行账号")
    @Schema(description = "开户行账号")
    private String bankAccountNo;

    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;

}
