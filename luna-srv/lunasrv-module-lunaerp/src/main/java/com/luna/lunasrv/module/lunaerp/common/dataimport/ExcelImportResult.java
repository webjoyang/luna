package com.luna.lunasrv.module.lunaerp.common.dataimport;

import com.luna.lunasrv.framework.common.util.http.HttpUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Excel 导入处理结果
 *
 * @author luna
 */
@Getter
public class ExcelImportResult {

    /**
     * 是否存在任何校验错误
     */
    private final boolean hasError;

    /**
     * 成功入库记录数（全量通过时生效）
     */
    private final int successCount;

    /**
     * 错误 Excel 文件字节流（存在错误时非空）
     */
    private final byte[] errorExcelBytes;

    /**
     * 错误行汇总信息
     */
    private final List<String> errorSummaries;

    private ExcelImportResult(boolean hasError, int successCount, byte[] errorExcelBytes, List<String> errorSummaries) {
        this.hasError = hasError;
        this.successCount = successCount;
        this.errorExcelBytes = errorExcelBytes;
        this.errorSummaries = errorSummaries != null ? errorSummaries : Collections.emptyList();
    }

    /**
     * 构造成功结果
     *
     * @param count 成功插入条数
     */
    public static ExcelImportResult success(int count) {
        return new ExcelImportResult(false, count, null, Collections.emptyList());
    }

    /**
     * 构造失败结果（附带错误 Excel 字节流）
     *
     * @param errorExcelBytes 包含错误原因末列的 Excel 文件字节流
     * @param errorSummaries  错误概要列表
     */
    public static ExcelImportResult error(byte[] errorExcelBytes, List<String> errorSummaries) {
        return new ExcelImportResult(true, 0, errorExcelBytes, errorSummaries);
    }

    public boolean hasError() {
        return hasError;
    }

    public boolean isSuccess() {
        return !hasError;
    }

    public byte[] getErrorStream() {
        return errorExcelBytes;
    }

    /**
     * 若存在错误，将错误 Excel 作为 attachment 流写入 HttpServletResponse
     *
     * @param response      HTTP 响应
     * @param errorFileName 错误 Excel 下载文件名（如 "客户导入错误明细.xlsx"）
     * @throws IOException IO异常
     */
    public void writeResponse(HttpServletResponse response, String errorFileName) throws IOException {
        if (!hasError || errorExcelBytes == null) {
            return;
        }
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(errorFileName));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.getOutputStream().write(errorExcelBytes);
        response.getOutputStream().flush();
    }

}
