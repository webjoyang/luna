package com.luna.lunasrv.module.lunaerp.common.desensitize;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.luna.lunasrv.framework.common.util.spring.SpringUtils;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 基于权限校验的条件脱敏序列化器
 *
 * 序列化器在 Jackson 输出时调用 SecurityFrameworkService.hasPermission(seePermission)：
 * 有权限出原文，无权限脱敏；无登录上下文（定时任务/MQ）按脱敏处理。
 *
 * @author luna
 */
public class SensitiveSerializer extends StdSerializer<Object> implements ContextualSerializer {

    private final Sensitive sensitive;

    public SensitiveSerializer() {
        this(null);
    }

    public SensitiveSerializer(Sensitive sensitive) {
        super(Object.class);
        this.sensitive = sensitive;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property == null) {
            return this;
        }
        Sensitive ann = property.getAnnotation(Sensitive.class);
        if (ann == null) {
            ann = property.getContextAnnotation(Sensitive.class);
        }
        if (ann != null) {
            return new SensitiveSerializer(ann);
        }
        return this;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider prov) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        if (sensitive == null) {
            writeValue(value, gen);
            return;
        }

        // 权限校验
        if (hasPermission(sensitive.seePermission())) {
            // 有权限，原样输出
            writeValue(value, gen);
            return;
        }

        // 无权限，脱敏输出
        serializeMasked(value, sensitive.type(), gen);
    }

    /**
     * 判断当前上下文是否有权限查看明文
     */
    private boolean hasPermission(String permission) {
        if (StrUtil.isBlank(permission)) {
            return true;
        }
        try {
            SecurityFrameworkService securityService = SpringUtils.getBean(SecurityFrameworkService.class);
            if (securityService != null) {
                return securityService.hasPermission(permission);
            }
        } catch (Exception ignored) {
        }
        // 无 SecurityFrameworkService 或未登录/无权限上下文，按脱敏处理
        return false;
    }

    /**
     * 输出原文
     */
    private void writeValue(Object value, JsonGenerator gen) throws IOException {
        if (value instanceof BigDecimal) {
            gen.writeNumber((BigDecimal) value);
        } else if (value instanceof Integer) {
            gen.writeNumber((Integer) value);
        } else if (value instanceof Long) {
            gen.writeNumber((Long) value);
        } else if (value instanceof Double) {
            gen.writeNumber((Double) value);
        } else if (value instanceof Float) {
            gen.writeNumber((Float) value);
        } else if (value instanceof Number) {
            gen.writeNumber(value.toString());
        } else if (value instanceof Boolean) {
            gen.writeBoolean((Boolean) value);
        } else {
            gen.writeString(value.toString());
        }
    }

    /**
     * 输出脱敏值
     */
    private void serializeMasked(Object value, SensitiveType type, JsonGenerator gen) throws IOException {
        if (type == SensitiveType.AMOUNT) {
            gen.writeString(AmountDesensitizationHandler.desensitize(value));
            return;
        }

        String str = value.toString();
        if (StrUtil.isBlank(str)) {
            gen.writeString(str);
            return;
        }

        switch (type) {
            case MOBILE:
                gen.writeString(slider(str, 3, 4, "*"));
                break;
            case CHINESE_NAME:
                gen.writeString(slider(str, 1, 0, "*"));
                break;
            case BANK_CARD:
                gen.writeString(slider(str, 6, 2, "*"));
                break;
            case FIXED_PHONE:
                gen.writeString(slider(str, 4, 2, "*"));
                break;
            case EMAIL:
                gen.writeString(str.replaceAll("(^.)[^@]*(@.*$)", "$1****$2"));
                break;
            default:
                gen.writeString(str);
                break;
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
