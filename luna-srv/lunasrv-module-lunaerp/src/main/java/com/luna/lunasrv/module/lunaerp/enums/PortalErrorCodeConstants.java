package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 客户门户错误码枚举
 *
 * 使用 1-030-038-xxx 段（客户门户，见 ADR-0044 与 shared GLOSSARY）
 *
 * @author luna
 */
public interface PortalErrorCodeConstants {

    // ========== 客户门户 1-030-038-000 ~ 009 ==========
    ErrorCode PORTAL_TOKEN_INVALID = new ErrorCode(1_030_038_000, "门户令牌无效或已过期");
    ErrorCode PORTAL_DATA_ACCESS_DENIED = new ErrorCode(1_030_038_001, "门户数据归属不符");
    ErrorCode PORTAL_REQUEST_TOO_FREQUENT = new ErrorCode(1_030_038_002, "门户请求过于频繁");

}
