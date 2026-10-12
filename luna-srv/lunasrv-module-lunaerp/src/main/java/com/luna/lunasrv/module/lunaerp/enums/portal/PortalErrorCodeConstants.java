package com.luna.lunasrv.module.lunaerp.enums.portal;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 客户门户错误码枚举常量接口
 *
 * lunaerp 系统，使用 1-030-038-xxx 段
 *
 * @author luna
 */
public interface PortalErrorCodeConstants {

    ErrorCode PORTAL_TOKEN_INVALID = new ErrorCode(1_030_038_000, "门户令牌无效或已过期");
    ErrorCode PORTAL_DATA_ACCESS_DENIED = new ErrorCode(1_030_038_001, "门户数据归属不符");
    ErrorCode PORTAL_RATE_LIMIT_EXCEEDED = new ErrorCode(1_030_038_002, "门户请求过于频繁，请稍后再试");

}
