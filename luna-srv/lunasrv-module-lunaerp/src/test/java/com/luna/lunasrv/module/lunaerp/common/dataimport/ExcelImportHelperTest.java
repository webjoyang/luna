package com.luna.lunasrv.module.lunaerp.common.dataimport;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.read.listener.ReadListener;
import com.luna.lunasrv.framework.common.biz.system.dict.DictDataCommonApi;
import com.luna.lunasrv.framework.common.biz.system.dict.dto.DictDataRespDTO;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.dict.core.DictFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerImportVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.luna.lunasrv.module.lunaerp.common.dataimport.DataImportConstants.*;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.EXCEL_IMPORT_FILE_EMPTY;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.EXCEL_IMPORT_ROW_LIMIT_EXCEEDED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 通用 Excel 导入引擎 {@link ExcelImportHelper} 单元测试
 */
class ExcelImportHelperTest {

    private DictDataCommonApi dictDataApi;

    @BeforeEach
    void setUp() {
        dictDataApi = Mockito.mock(DictDataCommonApi.class);
        DictFrameworkUtils.init(dictDataApi);
        DictFrameworkUtils.clearCache();

        // 模拟客户类型字典
        DictDataRespDTO d1 = new DictDataRespDTO();
        d1.setDictType("lunaerp_customer_client_type");
        d1.setLabel("终端客户");
        d1.setValue("TERMINAL");

        DictDataRespDTO d2 = new DictDataRespDTO();
        d2.setDictType("lunaerp_customer_client_type");
        d2.setLabel("贸易商");
        d2.setValue("TRADER");

        when(dictDataApi.getDictDataList("lunaerp_customer_client_type"))
                .thenReturn(Arrays.asList(d1, d2));
    }

    @AfterEach
    void tearDown() {
        DictFrameworkUtils.clearCache();
    }

    private byte[] createExcelBytes(List<CustomerImportVO> dataList) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FastExcelFactory.write(out, CustomerImportVO.class)
                .sheet("客户导入")
                .doWrite(dataList);
        return out.toByteArray();
    }

    private byte[] createRawExcelBytes(List<String> headers, List<List<Object>> dataList) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<List<String>> headList = headers.stream()
                .map(Collections::singletonList)
                .toList();
        FastExcelFactory.write(out)
                .head(headList)
                .sheet("客户导入")
                .doWrite(dataList);
        return out.toByteArray();
    }

    @Test
    @DisplayName("下载模板：应生成带有完整表头与下拉约束的空 Excel")
    void generateTemplate_success() {
        byte[] bytes = ExcelImportHelper.generateTemplate(CustomerImportVO.class);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        List<String> headers = new ArrayList<>();
        FastExcelFactory.read(new ByteArrayInputStream(bytes), new ReadListener<Map<Integer, String>>() {
            @Override
            public void invokeHead(Map<Integer, cn.idev.excel.metadata.data.ReadCellData<?>> headMap, AnalysisContext context) {
                int maxCol = headMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
                for (int i = 0; i <= maxCol; i++) {
                    cn.idev.excel.metadata.data.ReadCellData<?> cell = headMap.get(i);
                    headers.add(cell != null ? cell.getStringValue() : "");
                }
            }

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {}

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}
        }).sheet().doRead();

        assertFalse(headers.isEmpty());
        assertEquals("企业名称", headers.get(0));
        assertTrue(headers.contains("客户类型"));
        assertTrue(headers.contains("统一社会信用代码"));
    }

    @Test
    @DisplayName("导入成功：全部合法时执行持久化回调并返回成功件数")
    void importExcel_success() throws Exception {
        List<String> headers = Arrays.asList(
                "企业名称", "简称", "客户类型", "行业", "规模", "来源", "合作状态",
                "统一社会信用代码", "英文名称", "英文地址", "备注"
        );
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("杭州纺织一厂", "纺织一厂", "终端客户", "", "", "", "", "91330100MA27XXXX01", "", "", ""));
        rows.add(Arrays.asList("绍兴印染二厂", "", "贸易商", "", "", "", "", "91330100MA27XXXX02", "", "", ""));

        byte[] bytes = createRawExcelBytes(headers, rows);
        MockMultipartFile file = new MockMultipartFile("file", "customer.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        AtomicBoolean persisterCalled = new AtomicBoolean(false);

        ExcelImportResult result = ExcelImportHelper.importExcel(
                file,
                CustomerImportVO.class,
                r -> {}, // 无校验错误
                r -> {
                    persisterCalled.set(true);
                }
        );

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSuccessCount());
        assertNull(result.getErrorStream());
        assertTrue(persisterCalled.get());
    }

    @Test
    @DisplayName("空文件导入：无数据行时抛出 EXCEL_IMPORT_FILE_EMPTY")
    void importExcel_emptyFile_throwsException() {
        byte[] bytes = createExcelBytes(Collections.emptyList());
        MockMultipartFile file = new MockMultipartFile("file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                ExcelImportHelper.importExcel(file, CustomerImportVO.class, null, rows -> {}));
        assertEquals(EXCEL_IMPORT_FILE_EMPTY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("超过5000行导入：抛出 EXCEL_IMPORT_ROW_LIMIT_EXCEEDED")
    void importExcel_exceed5000Rows_throwsException() {
        List<String> headers = Collections.singletonList("企业名称");
        List<List<Object>> rows = new ArrayList<>();
        for (int i = 0; i <= MAX_IMPORT_ROW_COUNT; i++) {
            rows.add(Collections.singletonList("企业" + i));
        }
        byte[] bytes = createRawExcelBytes(headers, rows);
        MockMultipartFile file = new MockMultipartFile("file", "huge.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                ExcelImportHelper.importExcel(file, CustomerImportVO.class, null, r -> {}));
        assertEquals(EXCEL_IMPORT_ROW_LIMIT_EXCEEDED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("字典校验失败：生成错误 Excel 且不执行持久化回调")
    void importExcel_dictValidationFail_generatesErrorExcel() throws Exception {
        List<String> headers = Arrays.asList(
                "企业名称", "简称", "客户类型", "行业", "规模", "来源", "合作状态",
                "统一社会信用代码", "英文名称", "英文地址", "备注"
        );
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("杭州纺织一厂", "", "不存在的客户类型", "", "", "", "", "91330100MA27XXXX01", "", "", ""));

        byte[] bytes = createRawExcelBytes(headers, rows);
        MockMultipartFile file = new MockMultipartFile("file", "customer.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        AtomicBoolean persisterCalled = new AtomicBoolean(false);

        ExcelImportResult result = ExcelImportHelper.importExcel(
                file,
                CustomerImportVO.class,
                null,
                r -> {
                    persisterCalled.set(true);
                }
        );

        assertFalse(result.isSuccess());
        assertEquals(0, result.getSuccessCount());
        assertFalse(persisterCalled.get(), "存在校验错误时不可调用持久化回调");
        assertNotNull(result.getErrorStream());

        // 读取错误 Excel，验证是否包含最后一列【错误原因】
        List<String> readHeaders = new ArrayList<>();
        List<List<String>> readRows = new ArrayList<>();

        FastExcelFactory.read(new ByteArrayInputStream(result.getErrorStream()), new ReadListener<Map<Integer, String>>() {
            @Override
            public void invokeHead(Map<Integer, cn.idev.excel.metadata.data.ReadCellData<?>> headMap, AnalysisContext context) {
                int maxCol = headMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
                for (int i = 0; i <= maxCol; i++) {
                    cn.idev.excel.metadata.data.ReadCellData<?> cell = headMap.get(i);
                    readHeaders.add(cell != null ? cell.getStringValue() : "");
                }
            }

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                int maxCol = data.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
                List<String> row = new ArrayList<>();
                for (int i = 0; i <= maxCol; i++) {
                    row.add(data.get(i));
                }
                readRows.add(row);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}
        }).sheet().doRead();

        assertEquals(ERROR_COLUMN_NAME, readHeaders.get(readHeaders.size() - 1));
        assertEquals(1, readRows.size());
        List<String> errorRow = readRows.get(0);
        String errorMsg = errorRow.get(errorRow.size() - 1);
        assertTrue(errorMsg.contains("客户类型【不存在的客户类型】不在字典中"), "错误信息应提示字典值不在字典中");
    }

    @Test
    @DisplayName("业务校验失败多原因：以分号分隔并写入错误 Excel")
    void importExcel_multiErrors_separatedWithSemicolon() throws Exception {
        List<CustomerImportVO> list = new ArrayList<>();
        CustomerImportVO v1 = new CustomerImportVO();
        v1.setCompanyName("企业A");
        list.add(v1);

        byte[] bytes = createExcelBytes(list);
        MockMultipartFile file = new MockMultipartFile("file", "customer.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        AtomicBoolean persisterCalled = new AtomicBoolean(false);

        ExcelImportResult result = ExcelImportHelper.importExcel(
                file,
                CustomerImportVO.class,
                rows -> {
                    for (ImportRow<CustomerImportVO> row : rows) {
                        row.addError("企业名称已在系统存在");
                        row.addError("统一社会信用代码长度必须为18位");
                    }
                },
                rows -> {
                    persisterCalled.set(true);
                }
        );

        assertFalse(result.isSuccess());
        assertFalse(persisterCalled.get());
        assertNotNull(result.getErrorStream());

        List<List<String>> readRows = new ArrayList<>();
        FastExcelFactory.read(new ByteArrayInputStream(result.getErrorStream()), new ReadListener<Map<Integer, String>>() {
            @Override
            public void invokeHead(Map<Integer, cn.idev.excel.metadata.data.ReadCellData<?>> headMap, AnalysisContext context) {}

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                int maxCol = data.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
                List<String> row = new ArrayList<>();
                for (int i = 0; i <= maxCol; i++) {
                    row.add(data.get(i));
                }
                readRows.add(row);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}
        }).sheet().doRead();

        assertEquals(1, readRows.size());
        List<String> errorRow = readRows.get(0);
        String errorMsg = errorRow.get(errorRow.size() - 1);
        assertEquals("企业名称已在系统存在;统一社会信用代码长度必须为18位", errorMsg);
    }

}
