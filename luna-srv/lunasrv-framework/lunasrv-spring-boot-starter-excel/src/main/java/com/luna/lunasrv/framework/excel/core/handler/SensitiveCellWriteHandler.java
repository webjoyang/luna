package com.luna.lunasrv.framework.excel.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import cn.idev.excel.write.handler.CellWriteHandler;
import cn.idev.excel.write.handler.context.CellWriteHandlerContext;
import com.luna.lunasrv.framework.common.util.spring.SpringUtils;
import org.apache.poi.ss.usermodel.Cell;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * FastExcel 导出敏感字段脱敏处理器
 *
 * 扫描字段上的 @Sensitive 注解，无权限时按脱敏规则改写单元格内容，与 Jackson 序列化器保持同口径。
 *
 * @author luna
 */
public class SensitiveCellWriteHandler implements CellWriteHandler {

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        if (Boolean.TRUE.equals(context.getHead())) {
            return;
        }
        ExcelContentProperty contentProperty = context.getExcelContentProperty();
        if (contentProperty == null || contentProperty.getField() == null) {
            return;
        }
        Field field = contentProperty.getField();
        Annotation sensitiveAnn = findSensitiveAnnotation(field);
        if (sensitiveAnn == null) {
            return;
        }

        try {
            Method seePermissionMethod = sensitiveAnn.annotationType().getMethod("seePermission");
            String seePermission = (String) seePermissionMethod.invoke(sensitiveAnn);

            boolean hasPerm = checkPermission(seePermission);
            if (hasPerm) {
                return;
            }

            Method typeMethod = sensitiveAnn.annotationType().getMethod("type");
            Object typeObj = typeMethod.invoke(sensitiveAnn);
            String typeName = typeObj != null ? typeObj.toString() : "";

            Cell cell = context.getCell();
            Object originalValue = context.getOriginalValue();
            String desensitized = desensitizeValue(originalValue, typeName);

            if (cell != null) {
                cell.setCellValue(desensitized);
            }
            if (context.getFirstCellData() != null) {
                context.getFirstCellData().setStringValue(desensitized);
            }
        } catch (Exception ignored) {
        }
    }

    private Annotation findSensitiveAnnotation(Field field) {
        for (Annotation ann : field.getAnnotations()) {
            if ("Sensitive".equals(ann.annotationType().getSimpleName())) {
                return ann;
            }
        }
        return null;
    }

    private boolean checkPermission(String permission) {
        if (StrUtil.isBlank(permission)) {
            return true;
        }
        try {
            Object service = null;
            try {
                service = SpringUtils.getBean("ss");
            } catch (Exception ignored) {
            }
            if (service == null) {
                try {
                    service = SpringUtils.getBean("securityFrameworkService");
                } catch (Exception ignored) {
                }
            }
            if (service != null) {
                Method method = service.getClass().getMethod("hasPermission", String.class);
                return Boolean.TRUE.equals(method.invoke(service, permission));
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String desensitizeValue(Object value, String type) {
        if (value == null) {
            return "";
        }
        String origin = value.toString();
        if (StrUtil.isBlank(origin)) {
            return origin;
        }
        switch (type) {
            case "AMOUNT":
                return "****";
            case "MOBILE":
                return slider(origin, 3, 4, "*");
            case "CHINESE_NAME":
                return slider(origin, 1, 0, "*");
            case "BANK_CARD":
                return slider(origin, 6, 2, "*");
            case "FIXED_PHONE":
                return slider(origin, 4, 2, "*");
            case "EMAIL":
                return origin.replaceAll("(^.)[^@]*(@.*$)", "$1****$2");
            default:
                return origin;
        }
    }

    private String slider(String origin, int prefixKeep, int suffixKeep, String replacer) {
        int length = origin.length();
        int interval = length - prefixKeep - suffixKeep;
        if (interval <= 0) {
            return replacer.repeat(length);
        }
        return origin.substring(0, prefixKeep) + replacer.repeat(interval) + origin.substring(prefixKeep + interval);
    }

}
