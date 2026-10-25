package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 样品管理 - 样品分析错误码枚举类
 *
 * lunaerp 模块使用 1-030-040-xxx 段（主表 + 纱线规格明细/绕纱行/供应商报价三个聚合子表共用；
 * 2026-10 自 1-030-018 段迁入避让发染记录重号，见 ADR-0036、shared GLOSSARY 历史避让记录）
 *
 * @author luna
 */
public interface SampleErrorCodeConstants {

    // ========== 样品分析 1-030-040-000 ==========
    ErrorCode SAMPLE_ANALYSIS_NOT_EXISTS = new ErrorCode(1_030_040_000, "样品分析不存在");
    ErrorCode SAMPLE_ANALYSIS_NO_DUPLICATE = new ErrorCode(1_030_040_001, "样品编号【{}】已存在");
    ErrorCode SAMPLE_ANALYSIS_REF_NOT_EXISTS = new ErrorCode(1_030_040_002, "关联引用不存在");
    ErrorCode SAMPLE_ANALYSIS_LOSS_RATE_INVALID = new ErrorCode(1_030_040_003, "损耗率取值非法（必须 ≥ 0 且 < 100）");
    ErrorCode SAMPLE_ANALYSIS_PRODUCT_NAME_EMPTY = new ErrorCode(1_030_040_004, "产品名称不能为空");
    ErrorCode SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID = new ErrorCode(1_030_040_005, "三角排列画板数据非法：{}");
    ErrorCode SAMPLE_ANALYSIS_PHOTO_COUNT_EXCEED = new ErrorCode(1_030_040_006, "样品照片最多上传 9 张");
    ErrorCode SAMPLE_ANALYSIS_YARN_SPEC_NOT_EXISTS = new ErrorCode(1_030_040_100, "纱线规格不存在");

}
