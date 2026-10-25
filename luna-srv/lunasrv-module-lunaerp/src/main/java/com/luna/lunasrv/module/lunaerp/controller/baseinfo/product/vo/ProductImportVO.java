package com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo;

import cn.idev.excel.annotation.ExcelProperty;
import com.luna.lunasrv.framework.excel.core.annotations.DictFormat;
import com.luna.lunasrv.framework.excel.core.annotations.ExcelColumnSelect;
import com.luna.lunasrv.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 管理后台 - 产品 Excel 导入 VO
 *
 * @author luna
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "管理后台 - 产品 Excel 导入 VO")
public class ProductImportVO {

    @ExcelProperty("产品编号")
    @Schema(description = "产品编号（必填，租户内唯一）")
    private String code;

    @ExcelProperty("产品名称")
    @Schema(description = "产品名称（必填）")
    private String name;

    @ExcelProperty(value = "类别", converter = DictConvert.class)
    @DictFormat("lunaerp_product_category")
    @ExcelColumnSelect(dictType = "lunaerp_product_category")
    @Schema(description = "产品类别（字典 lunaerp_product_category）")
    private String type;

    @ExcelProperty(value = "布种", converter = DictConvert.class)
    @DictFormat("lunaerp_fabric_type")
    @ExcelColumnSelect(dictType = "lunaerp_fabric_type")
    @Schema(description = "布种（字典 lunaerp_fabric_type）")
    private String fabricType;

    @ExcelProperty("织物组织")
    @Schema(description = "织物组织（KNIT 针织 / WOVEN 梭织）")
    private String fabricConstruction;

    @ExcelProperty("成分")
    @Schema(description = "成分（必填，如 NR / TR / CVC）")
    private String composition;

    @ExcelProperty("成品门幅")
    @Schema(description = "成品门幅（厘米，必填）")
    private BigDecimal widthCm;

    @ExcelProperty("成品克重")
    @Schema(description = "成品克重（g/m²，必填）")
    private BigDecimal weightGsm;

    @ExcelProperty("颜色")
    @Schema(description = "颜色")
    private String color;

    @ExcelProperty("工艺类型")
    @Schema(description = "工艺类型")
    private String craftType;

    @ExcelProperty("机型")
    @Schema(description = "机型")
    private String machineType;

    @ExcelProperty("供应商样号")
    @Schema(description = "供应商样号")
    private String supplierCode;

    @ExcelProperty("标准米长")
    @Schema(description = "标准米长")
    private String standardLength;

    @ExcelProperty("状态")
    @Schema(description = "状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE")
    private String status;

    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;

}
