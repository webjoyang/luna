package com.luna.lunasrv.module.lunaerp.common.dataimport;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.read.listener.ReadListener;
import com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil;
import com.luna.lunasrv.framework.dict.core.DictFrameworkUtils;
import com.luna.lunasrv.framework.excel.core.annotations.DictFormat;
import com.luna.lunasrv.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.luna.lunasrv.module.lunaerp.common.dataimport.DataImportConstants.ERROR_COLUMN_NAME;
import static com.luna.lunasrv.module.lunaerp.common.dataimport.DataImportConstants.MAX_IMPORT_ROW_COUNT;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.EXCEL_IMPORT_FILE_EMPTY;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.EXCEL_IMPORT_ROW_LIMIT_EXCEEDED;

/**
 * Excel 通用导入工具类
 *
 * 核心能力：
 * 1. 逐行校验收集全部错误（单行多错误使用分号分隔）。
 * 2. 5000 行上限校验（超限报 1_030_031_100）、空文件校验（空报 1_030_031_101）。
 * 3. 自动扫描 VO 上的 @DictFormat 进行字典值存在性校验。
 * 4. 有任一错误整批不写库，生成保留原列并在末列追加"错误原因"的错误明细 Excel。
 * 5. 全量校验通过后单事务批量持久化入库，返回成功条数。
 *
 * @author luna
 */
@Slf4j
public class ExcelImportHelper {

    /**
     * 生成 Excel 导入模板（包含表头及注解约束，无数据行）
     *
     * @param voClass 导入 VO 类
     * @param <T>     VO 类型
     * @return 模板 Excel 字节数组
     */
    public static <T> byte[] generateTemplate(Class<T> voClass) {
        return generateTemplate(voClass, "导入模板");
    }

    /**
     * 生成 Excel 导入模板（包含表头及注解约束，无数据行）
     *
     * @param voClass   导入 VO 类
     * @param sheetName Sheet 页名称
     * @param <T>       VO 类型
     * @return 模板 Excel 字节数组
     */
    public static <T> byte[] generateTemplate(Class<T> voClass, String sheetName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FastExcelFactory.write(out, voClass)
                .sheet(sheetName)
                .doWrite(Collections.emptyList());
        return out.toByteArray();
    }

    /**
     * 执行 Excel 导入（基于 MultipartFile）
     *
     * @param file           上传的 Excel 文件
     * @param voClass        导入 VO 类定义
     * @param rowValidator   业务自定义逐行校验器（收集行错误）
     * @param batchPersister 校验通过时的批量入库持久化回调
     * @param <T>            VO 类型
     * @return 导入处理结果
     * @throws IOException IO 异常
     */
    public static <T> ExcelImportResult importExcel(MultipartFile file,
                                                    Class<T> voClass,
                                                    Consumer<List<ImportRow<T>>> rowValidator,
                                                    Consumer<List<T>> batchPersister) throws IOException {
        if (file == null || file.isEmpty()) {
            throw ServiceExceptionUtil.exception(EXCEL_IMPORT_FILE_EMPTY);
        }
        return importExcel(file.getBytes(), voClass, rowValidator, batchPersister);
    }

    /**
     * 执行 Excel 导入（基于 InputStream）
     *
     * @param inputStream    Excel 输入流
     * @param voClass        导入 VO 类定义
     * @param rowValidator   业务自定义逐行校验器
     * @param batchPersister 校验通过时的批量入库持久化回调
     * @param <T>            VO 类型
     * @return 导入处理结果
     * @throws IOException IO 异常
     */
    public static <T> ExcelImportResult importExcel(InputStream inputStream,
                                                    Class<T> voClass,
                                                    Consumer<List<ImportRow<T>>> rowValidator,
                                                    Consumer<List<T>> batchPersister) throws IOException {
        if (inputStream == null) {
            throw ServiceExceptionUtil.exception(EXCEL_IMPORT_FILE_EMPTY);
        }
        byte[] bytes = inputStream.readAllBytes();
        return importExcel(bytes, voClass, rowValidator, batchPersister);
    }

    /**
     * 执行 Excel 导入（基于 byte 数组）
     *
     * @param fileBytes      Excel 文件字节数组
     * @param voClass        导入 VO 类定义
     * @param rowValidator   业务自定义逐行校验器
     * @param batchPersister 校验通过时的批量入库持久化回调
     * @param <T>            VO 类型
     * @return 导入处理结果
     */
    public static <T> ExcelImportResult importExcel(byte[] fileBytes,
                                                    Class<T> voClass,
                                                    Consumer<List<ImportRow<T>>> rowValidator,
                                                    Consumer<List<T>> batchPersister) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw ServiceExceptionUtil.exception(EXCEL_IMPORT_FILE_EMPTY);
        }

        // 1. 读取 Excel 文件头与各行原始数据
        List<String> headers = new ArrayList<>();
        List<ImportRow<T>> rows = new ArrayList<>();

        try {
            FastExcelFactory.read(new ByteArrayInputStream(fileBytes), voClass, new ReadListener<T>() {
                @Override
                public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
                    if (headMap != null && headers.isEmpty()) {
                        int maxCol = headMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
                        for (int i = 0; i <= maxCol; i++) {
                            ReadCellData<?> cell = headMap.get(i);
                            headers.add(cell != null && cell.getStringValue() != null ? cell.getStringValue().trim() : "");
                        }
                    }
                }

                @Override
                public void invoke(T data, AnalysisContext context) {
                    int currentRowNum = context.readRowHolder().getRowIndex() + 1;
                    Map<Integer, ?> cellMap = context.readRowHolder().getCellMap();
                    List<String> rawRow = new ArrayList<>();
                    int maxCol = Math.max(headers.size() - 1,
                            cellMap != null ? cellMap.keySet().stream().mapToInt(c -> (Integer) c).max().orElse(-1) : -1);

                    boolean isAllBlank = true;
                    for (int i = 0; i <= maxCol; i++) {
                        Object cell = cellMap != null ? cellMap.get(i) : null;
                        String text = formatCellData(cell);
                        if (StrUtil.isNotBlank(text)) {
                            isAllBlank = false;
                        }
                        rawRow.add(text);
                    }

                    // 过滤全空行
                    if (!isAllBlank) {
                        rows.add(new ImportRow<>(currentRowNum, data, rawRow));
                        if (rows.size() > MAX_IMPORT_ROW_COUNT) {
                            throw ServiceExceptionUtil.exception(EXCEL_IMPORT_ROW_LIMIT_EXCEEDED, MAX_IMPORT_ROW_COUNT);
                        }
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {}
            }).sheet(0).doRead();
        } catch (com.luna.lunasrv.framework.common.exception.ServiceException se) {
            throw se;
        } catch (Exception ex) {
            log.error("[importExcel] 解析 Excel 失败: {}", ex.getMessage(), ex);
            throw ServiceExceptionUtil.exception(EXCEL_IMPORT_FILE_EMPTY);
        }

        // 2. 空文件校验（无任何有效数据行）
        if (rows.isEmpty()) {
            throw ServiceExceptionUtil.exception(EXCEL_IMPORT_FILE_EMPTY);
        }

        // 3. 行数上限校验（超过 5000 行）
        if (rows.size() > MAX_IMPORT_ROW_COUNT) {
            throw ServiceExceptionUtil.exception(EXCEL_IMPORT_ROW_LIMIT_EXCEEDED, MAX_IMPORT_ROW_COUNT);
        }

        // 4. 自动字典列校验
        validateDictColumns(voClass, headers, rows);

        // 5. 业务自定义逐行校验（重名、格式、必填等）
        if (rowValidator != null) {
            rowValidator.accept(rows);
        }

        // 6. 检查是否存在任意错误
        boolean hasAnyError = rows.stream().anyMatch(ImportRow::hasError);
        if (hasAnyError) {
            // 生成标好错误原因的 Excel 并返回
            byte[] errorBytes = buildErrorExcel(headers, rows);
            List<String> summaries = rows.stream()
                    .filter(ImportRow::hasError)
                    .map(r -> "第 " + r.getRowNum() + " 行: " + r.getErrorString())
                    .collect(Collectors.toList());
            return ExcelImportResult.error(errorBytes, summaries);
        }

        // 7. 全量通过，执行批量持久化
        List<T> validList = rows.stream().map(ImportRow::getData).collect(Collectors.toList());
        if (batchPersister != null) {
            batchPersister.accept(validList);
        }
        return ExcelImportResult.success(validList.size());
    }

    /**
     * 自动校验 VO 中声明了 @DictFormat 的字典字段
     */
    private static <T> void validateDictColumns(Class<T> voClass, List<String> headers, List<ImportRow<T>> rows) {
        List<DictFieldMeta> dictFields = parseDictFields(voClass, headers);
        if (dictFields.isEmpty()) {
            return;
        }

        for (ImportRow<T> row : rows) {
            for (DictFieldMeta meta : dictFields) {
                String rawVal = row.getRawCellValue(meta.getColIndex());
                if (StrUtil.isBlank(rawVal)) {
                    continue;
                }
                String trimmed = rawVal.trim();
                String parsedValue = DictFrameworkUtils.parseDictDataValue(meta.getDictType(), trimmed);
                boolean valid = (parsedValue != null);
                if (!valid) {
                    List<String> values = DictFrameworkUtils.getDictDataValueList(meta.getDictType());
                    if (values != null && values.contains(trimmed)) {
                        valid = true;
                        parsedValue = trimmed;
                    }
                }

                if (!valid) {
                    row.addError(meta.getColumnName() + "【" + trimmed + "】不在字典中");
                } else {
                    // 若 VO 中对应字段为 null，尝试将字典 value 回填到 VO
                    try {
                        Object currentVal = ReflectUtil.getFieldValue(row.getData(), meta.getField());
                        if (currentVal == null) {
                            ReflectUtil.setFieldValue(row.getData(), meta.getField(),
                                    Convert.convert(meta.getField().getType(), parsedValue));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    /**
     * 解析 VO 类中带有 @DictFormat 的字段元数据
     */
    private static <T> List<DictFieldMeta> parseDictFields(Class<T> voClass, List<String> headers) {
        List<DictFieldMeta> result = new ArrayList<>();
        Field[] fields = ReflectUtil.getFields(voClass);
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            DictFormat dictFormat = field.getAnnotation(DictFormat.class);
            if (dictFormat == null) {
                continue;
            }
            String dictType = dictFormat.value();
            ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
            String columnName = field.getName();
            int colIndex = i;

            if (excelProperty != null) {
                if (excelProperty.value().length > 0 && StrUtil.isNotBlank(excelProperty.value()[0])) {
                    columnName = excelProperty.value()[0];
                }
                if (excelProperty.index() >= 0) {
                    colIndex = excelProperty.index();
                } else if (!headers.isEmpty()) {
                    int matchedIndex = headers.indexOf(columnName);
                    if (matchedIndex >= 0) {
                        colIndex = matchedIndex;
                    }
                }
            }
            result.add(new DictFieldMeta(field, dictType, columnName, colIndex));
        }
        return result;
    }

    /**
     * 生成包含"错误原因"末列的错误明细 Excel 字节流
     */
    private static <T> byte[] buildErrorExcel(List<String> headers, List<ImportRow<T>> rows) {
        List<List<String>> headList = new ArrayList<>(headers.size() + 1);
        for (String h : headers) {
            headList.add(Collections.singletonList(h));
        }
        headList.add(Collections.singletonList(ERROR_COLUMN_NAME));

        List<List<Object>> dataList = new ArrayList<>(rows.size());
        for (ImportRow<T> row : rows) {
            List<Object> rowCells = new ArrayList<>(row.getRawRow());
            while (rowCells.size() < headers.size()) {
                rowCells.add("");
            }
            rowCells.add(row.getErrorString());
            dataList.add(rowCells);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FastExcelFactory.write(out)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                .head(headList)
                .sheet("错误明细")
                .doWrite(dataList);
        return out.toByteArray();
    }

    /**
     * 格式化单元格原始数据为纯文本
     */
    public static String formatCellData(Object cell) {
        if (cell == null) {
            return "";
        }
        if (cell instanceof ReadCellData<?> rcd) {
            if (rcd.getStringValue() != null) {
                return rcd.getStringValue();
            }
            if (rcd.getNumberValue() != null) {
                BigDecimal num = rcd.getNumberValue();
                if (num.scale() <= 0 || num.stripTrailingZeros().scale() <= 0) {
                    return num.toBigInteger().toString();
                }
                return num.stripTrailingZeros().toPlainString();
            }
            if (rcd.getBooleanValue() != null) {
                return String.valueOf(rcd.getBooleanValue());
            }
            return "";
        }
        return cell.toString();
    }

    private static class DictFieldMeta {
        private final Field field;
        private final String dictType;
        private final String columnName;
        private final int colIndex;

        public DictFieldMeta(Field field, String dictType, String columnName, int colIndex) {
            this.field = field;
            this.dictType = dictType;
            this.columnName = columnName;
            this.colIndex = colIndex;
        }

        public Field getField() { return field; }
        public String getDictType() { return dictType; }
        public String getColumnName() { return columnName; }
        public int getColIndex() { return colIndex; }
    }

}
