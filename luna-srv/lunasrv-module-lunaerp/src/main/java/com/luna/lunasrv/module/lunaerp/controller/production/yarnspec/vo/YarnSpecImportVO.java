package com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo;

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
 * 管理后台 - 纱线规格 Excel 导入 VO
 *
 * @author luna
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "管理后台 - 纱线规格 Excel 导入 VO")
public class YarnSpecImportVO {

    @ExcelProperty("材质名称")
    @Schema(description = "材质名称（必填，精确匹配启用状态材质）")
    private String yarnTypeName;

    @ExcelProperty("规格名称")
    @Schema(description = "规格名称（必填，展示用）")
    private String specName;

    @ExcelProperty("标准名称")
    @Schema(description = "标准名称（必填，租户+材质内唯一）")
    private String standardName;

    @ExcelProperty(value = "纺纱工艺", converter = DictConvert.class)
    @DictFormat("lunaerp_yarn_spin_method")
    @ExcelColumnSelect(dictType = "lunaerp_yarn_spin_method")
    @Schema(description = "纺纱工艺（字典 lunaerp_yarn_spin_method）")
    private String spinMethod;

    @ExcelProperty(value = "光泽", converter = DictConvert.class)
    @DictFormat("lunaerp_yarn_luster")
    @ExcelColumnSelect(dictType = "lunaerp_yarn_luster")
    @Schema(description = "光泽（字典 lunaerp_yarn_luster）")
    private String luster;

    @ExcelProperty("参考单价")
    @Schema(description = "参考单价（非负金额，受 lunaerp:sensitive:amount 管控）")
    private BigDecimal refPrice;

    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;

}
