package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * LunaERP 商业地基通用件错误码枚举类
 *
 * 使用 1-030-031-xxx 段：
 *   000-099 回收站
 *   100-199 Excel 导入
 * （脱敏、消息、打印复用通用错误码，不单独占码，见 ADR-0038 与 shared GLOSSARY）
 *
 * @author luna
 */
public interface CommonErrorCodeConstants {

    // ========== 回收站模块 1-030-031-000 ~ 099 ==========
    ErrorCode RECYCLE_BIN_BIZ_TYPE_NOT_SUPPORTED = new ErrorCode(1_030_031_000, "不支持的回收站数据类型【{}】");
    ErrorCode RECYCLE_BIN_RECORD_NOT_EXISTS = new ErrorCode(1_030_031_001, "记录不存在或不在回收站中");
    ErrorCode RECYCLE_BIN_REFERENCE_IN_RECYCLE = new ErrorCode(1_030_031_002, "引用的【{}】仍在回收站，请先恢复后再恢复本单据");
    ErrorCode RECYCLE_BIN_HAS_ACTIVE_REFERENCE = new ErrorCode(1_030_031_003, "该数据已被业务引用，无法彻底删除");

    // 便捷别名
    ErrorCode RECYCLE_BIN_RECORD_NOT_FOUND = RECYCLE_BIN_RECORD_NOT_EXISTS;
    ErrorCode RECYCLE_BIN_REF_IN_RECYCLE = RECYCLE_BIN_REFERENCE_IN_RECYCLE;
    ErrorCode RECYCLE_BIN_ACTIVE_REF_EXISTS = RECYCLE_BIN_HAS_ACTIVE_REFERENCE;

    // ========== Excel 导入模块 1-030-031-100 ~ 199 ==========
    ErrorCode EXCEL_IMPORT_ROW_LIMIT_EXCEEDED = new ErrorCode(1_030_031_100, "导入数据超过单次上限 {} 行，请拆分后导入");
    ErrorCode EXCEL_IMPORT_FILE_EMPTY = new ErrorCode(1_030_031_101, "导入文件为空或无有效数据行");
    ErrorCode EXCEL_IMPORT_PERMISSION_DENIED = new ErrorCode(1_030_031_102, "导入人缺少【{}】权限，无法导入含该字段的数据");

    // 便捷别名
    ErrorCode EXCEL_IMPORT_NO_PERMISSION = EXCEL_IMPORT_PERMISSION_DENIED;
    ErrorCode EXCEL_IMPORT_EMPTY_FILE = EXCEL_IMPORT_FILE_EMPTY;

}
