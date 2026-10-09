package com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo;

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
 * 管理后台 - 供应商 Excel 导入 VO
 *
 * @author luna
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "管理后台 - 供应商 Excel 导入 VO")
public class SupplierImportVO {

    @ExcelProperty("供应商编号")
    @Schema(description = "供应商编号（必填，租户内唯一含软删行）")
    private String supplierNo;

    @ExcelProperty("企业名称")
    @Schema(description = "企业名称（必填）")
    private String companyName;

    @ExcelProperty("简称")
    @Schema(description = "简称（为空时用企业名称回填）")
    private String shortName;

    @ExcelProperty(value = "供应商类型", converter = DictConvert.class)
    @DictFormat("lunaerp_supplier_type")
    @ExcelColumnSelect(dictType = "lunaerp_supplier_type")
    @Schema(description = "供应商类型（字典 lunaerp_supplier_type）")
    private String supplierType;

    @ExcelProperty("机型")
    @Schema(description = "主营机型/设备说明")
    private String machineType;

    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;

}
