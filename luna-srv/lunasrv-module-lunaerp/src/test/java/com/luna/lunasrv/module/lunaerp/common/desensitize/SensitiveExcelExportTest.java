package com.luna.lunasrv.module.lunaerp.common.desensitize;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.annotation.ExcelProperty;
import com.luna.lunasrv.framework.common.util.spring.SpringUtils;
import com.luna.lunasrv.framework.excel.core.handler.SensitiveCellWriteHandler;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import lombok.Data;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * FastExcel 导出敏感字段脱敏测试
 */
public class SensitiveExcelExportTest {

    private SecurityFrameworkService securityFrameworkService;
    private ApplicationContext applicationContext;

    @Data
    public static class ExportDemoVO {
        @ExcelProperty("姓名")
        @Sensitive(seePermission = SensitiveConstants.PERMISSION_CONTACT, type = SensitiveType.CHINESE_NAME)
        private String name;

        @ExcelProperty("手机")
        @Sensitive(seePermission = SensitiveConstants.PERMISSION_CONTACT, type = SensitiveType.MOBILE)
        private String mobile;

        @ExcelProperty("参考单价")
        @Sensitive(seePermission = SensitiveConstants.PERMISSION_AMOUNT, type = SensitiveType.AMOUNT)
        private BigDecimal refPrice;
    }

    @BeforeEach
    public void setUp() {
        securityFrameworkService = mock(SecurityFrameworkService.class);
        applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(SecurityFrameworkService.class)).thenReturn(securityFrameworkService);
        when(applicationContext.getBean("ss")).thenReturn(securityFrameworkService);
        when(applicationContext.getBean("securityFrameworkService")).thenReturn(securityFrameworkService);
        new SpringUtils().setApplicationContext(applicationContext);
    }

    @AfterEach
    public void tearDown() {
        new SpringUtils().setApplicationContext(null);
    }

    @Test
    public void testExport_WithoutPermissions() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_CONTACT))).thenReturn(false);
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(false);

        ExportDemoVO vo = new ExportDemoVO();
        vo.setName("李四");
        vo.setMobile("13911112222");
        vo.setRefPrice(new BigDecimal("18.80"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FastExcelFactory.write(out, ExportDemoVO.class)
                .registerWriteHandler(new SensitiveCellWriteHandler())
                .sheet("测试")
                .doWrite(Collections.singletonList(vo));

        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()));
        Sheet sheet = workbook.getSheetAt(0);
        Row row = sheet.getRow(1);

        assertEquals("李*", row.getCell(0).getStringCellValue());
        assertEquals("139****2222", row.getCell(1).getStringCellValue());
        assertEquals("****", row.getCell(2).getStringCellValue());
        workbook.close();
    }

    @Test
    public void testExport_WithPermissions() throws Exception {
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_CONTACT))).thenReturn(true);
        when(securityFrameworkService.hasPermission(eq(SensitiveConstants.PERMISSION_AMOUNT))).thenReturn(true);

        ExportDemoVO vo = new ExportDemoVO();
        vo.setName("李四");
        vo.setMobile("13911112222");
        vo.setRefPrice(new BigDecimal("18.80"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FastExcelFactory.write(out, ExportDemoVO.class)
                .registerWriteHandler(new SensitiveCellWriteHandler())
                .sheet("测试")
                .doWrite(Collections.singletonList(vo));

        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()));
        Sheet sheet = workbook.getSheetAt(0);
        Row row = sheet.getRow(1);

        assertEquals("李四", row.getCell(0).getStringCellValue());
        assertEquals("13911112222", row.getCell(1).getStringCellValue());
        assertEquals(18.80, row.getCell(2).getNumericCellValue(), 0.001);
        workbook.close();
    }
}
