package com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YarnPriceImportVO {

    @ExcelProperty("标准名称")
    private String standardName;

    @ExcelProperty("纱线材质")
    private String yarnTypeName;

    @ExcelProperty("规格名称")
    private String specName;

    @ExcelProperty("价类")
    private String priceType;

    @ExcelProperty("生效日期")
    private String effectiveDate;

    @ExcelProperty("单价(元/KG)")
    private BigDecimal price;

    @ExcelProperty("备注")
    private String remark;

}
