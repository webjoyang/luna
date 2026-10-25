package com.luna.lunasrv.module.lunaerp.common.dataimport;

/**
 * Excel 通用导入常量
 *
 * @author luna
 */
public interface DataImportConstants {

    /**
     * 单次导入最大允许行数（5000 行）
     */
    int MAX_IMPORT_ROW_COUNT = 5000;

    /**
     * 错误 Excel 末列标题
     */
    String ERROR_COLUMN_NAME = "错误原因";

    /**
     * 一行多错误原因分隔符
     */
    String ERROR_SEPARATOR = ";";

}
