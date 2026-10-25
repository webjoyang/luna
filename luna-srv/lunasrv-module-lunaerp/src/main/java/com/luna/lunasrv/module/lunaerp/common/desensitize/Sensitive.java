package com.luna.lunasrv.module.lunaerp.common.desensitize;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限条件脱敏注解
 *
 * 在 Jackson 输出时调用 SecurityFrameworkService.hasPermission(seePermission)：
 * 有权限出原文，无权限脱敏；无登录上下文（定时任务/MQ）按脱敏处理。
 *
 * @author luna
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {

    /**
     * 查看明文所需的权限码，例如 "lunaerp:sensitive:amount"、"lunaerp:sensitive:contact"
     */
    String seePermission();

    /**
     * 脱敏类型
     */
    SensitiveType type();

}
