package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * LunaERP 胚布相关错误码枚举类
 *
 * lunaerp 模块使用 1-030-xxx-xxx 段：
 *   001 产品、002 胚布品种
 *
 * @author luna
 */
public interface GreigeErrorCodeConstants {

    // ========== 胚布品种 1-030-002-000 ==========
    ErrorCode GREIGE_TYPE_NOT_EXISTS = new ErrorCode(1_030_002_000, "胚布品种不存在");
    ErrorCode GREIGE_TYPE_CODE_DUPLICATE = new ErrorCode(1_030_002_001, "胚布品种编号【{}】已存在");
    ErrorCode GREIGE_TYPE_MATCH_KEY_DUPLICATE = new ErrorCode(1_030_002_002,
            "胚布品种已存在：相同布种/成分/纱支/坯克重/坯门幅的品种已建档，请勿重复创建");

}
