package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 样品管理 - 样品分析错误码枚举类
 *
 * lunaerp 模块使用 1-030-018-xxx 段（主表 + 纱线规格明细/绕纱行/供应商报价三个聚合子表共用）
 *
 * @author luna
 */
public interface SampleErrorCodeConstants {

    // ========== 样品分析 1-030-018-000 ==========
    ErrorCode SAMPLE_ANALYSIS_NOT_EXISTS = new ErrorCode(1_030_018_000, "样品分析不存在");
    ErrorCode SAMPLE_ANALYSIS_NO_DUPLICATE = new ErrorCode(1_030_018_001, "样品编号【{}】已存在");
    ErrorCode SAMPLE_ANALYSIS_REF_NOT_EXISTS = new ErrorCode(1_030_018_002, "关联引用不存在");
    ErrorCode SAMPLE_ANALYSIS_LOSS_RATE_INVALID = new ErrorCode(1_030_018_003, "损耗率取值非法（必须 ≥ 0 且 < 100）");
    ErrorCode SAMPLE_ANALYSIS_PRODUCT_NAME_EMPTY = new ErrorCode(1_030_018_004, "产品名称不能为空");
    ErrorCode SAMPLE_ANALYSIS_YARN_SPEC_NOT_EXISTS = new ErrorCode(1_030_018_100, "纱线规格不存在或已停用");

}
