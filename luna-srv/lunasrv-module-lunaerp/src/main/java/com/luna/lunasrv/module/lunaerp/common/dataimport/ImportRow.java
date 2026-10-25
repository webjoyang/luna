package com.luna.lunasrv.module.lunaerp.common.dataimport;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excel 导入行包装对象
 *
 * @param <T> 解析后的业务 VO 类型
 * @author luna
 */
@Getter
public class ImportRow<T> {

    /**
     * 物理行号（从 1 开始计数，含表头通常第 1 行为表头，数据行从第 2 行开始）
     */
    private final int rowNum;

    /**
     * FastExcel 解析得到的业务 VO
     */
    private final T data;

    /**
     * 该行原始单元格文本（原样保留用户输入）
     */
    private final List<String> rawRow;

    /**
     * 校验错误列表
     */
    private final List<String> errors = new ArrayList<>();

    public ImportRow(int rowNum, T data, List<String> rawRow) {
        this.rowNum = rowNum;
        this.data = data;
        this.rawRow = rawRow != null ? rawRow : Collections.emptyList();
    }

    /**
     * 添加行级校验错误
     *
     * @param error 错误原因
     */
    public void addError(String error) {
        if (StrUtil.isNotBlank(error)) {
            this.errors.add(error.trim());
        }
    }

    /**
     * 当前行是否存在错误
     */
    public boolean hasError() {
        return !errors.isEmpty();
    }

    /**
     * 获取按分号拼接的错误文本
     */
    public String getErrorString() {
        return String.join(DataImportConstants.ERROR_SEPARATOR, errors);
    }

    /**
     * 获取指定列索引的原始单元格文本
     */
    public String getRawCellValue(int colIndex) {
        if (colIndex >= 0 && colIndex < rawRow.size()) {
            return rawRow.get(colIndex);
        }
        return "";
    }

}
