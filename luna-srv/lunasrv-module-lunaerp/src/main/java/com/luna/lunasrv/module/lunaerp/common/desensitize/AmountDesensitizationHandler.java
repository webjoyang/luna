package com.luna.lunasrv.module.lunaerp.common.desensitize;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.luna.lunasrv.framework.common.util.json.JsonUtils;

/**
 * AMOUNT 金额脱敏处理器
 *
 * 规则：
 * 1. 数值类型（BigDecimal、Number）无权限脱敏为字符串 "****"；
 * 2. costItems JSON 字符串：无权限将内部每个对象的 price（或 unitPrice）替换为 "****"，保留外层 JSON 结构；
 * 3. 其他普通金额字符串无权限脱敏为 "****"；
 * 4. 有权限时原样输出。
 *
 * @author luna
 */
public class AmountDesensitizationHandler {

    public static final String AMOUNT_MASK = "****";

    /**
     * 对无权限值执行脱敏
     *
     * @param value 原始值
     * @return 脱敏后的值
     */
    public static String desensitize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String str = (String) value;
            if (StrUtil.isBlank(str)) {
                return str;
            }
            return desensitizeString(str);
        }
        return AMOUNT_MASK;
    }

    /**
     * 脱敏字符串值：若是 JSON 格式则脱敏内部 price/unitPrice，否则直接返回 "****"
     *
     * @param str 字符串
     * @return 脱敏字符串
     */
    public static String desensitizeString(String str) {
        String trimmed = str.trim();
        if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            try {
                JsonNode rootNode = JsonUtils.parseTree(trimmed);
                if (rootNode != null) {
                    maskPrices(rootNode);
                    return JsonUtils.toJsonString(rootNode);
                }
            } catch (Exception ignored) {
                // 非合法 JSON 则直接回退为普通字符串脱敏
            }
        }
        return AMOUNT_MASK;
    }

    private static void maskPrices(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            if (objNode.has("price")) {
                objNode.put("price", AMOUNT_MASK);
            }
            if (objNode.has("unitPrice")) {
                objNode.put("unitPrice", AMOUNT_MASK);
            }
            objNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode child = objNode.get(fieldName);
                if (child != null && child.isContainerNode()) {
                    maskPrices(child);
                }
            });
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode item : arrayNode) {
                maskPrices(item);
            }
        }
    }

}
