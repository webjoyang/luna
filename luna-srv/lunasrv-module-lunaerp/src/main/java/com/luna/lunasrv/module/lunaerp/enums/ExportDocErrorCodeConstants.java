package com.luna.lunasrv.module.lunaerp.enums;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 出口单证错误码常量
 *
 * 规范段位：1-030-033-xxx
 * 分区：000 单证套 / 100 明细 / 200 卖方配置
 *
 * @author luna
 */
public interface ExportDocErrorCodeConstants {

    // ========== 单证套 1-030-033-000 ~ 099 ==========
    ErrorCode EXPORT_DOC_NOT_EXISTS = new ErrorCode(1_030_033_000, "出口单证套不存在");
    ErrorCode EXPORT_DOC_NO_DUPLICATE = new ErrorCode(1_030_033_001, "单证套号【{}】已存在");
    ErrorCode EXPORT_DOC_STATUS_NOT_ALLOW = new ErrorCode(1_030_033_002, "当前状态不允许该操作");
    ErrorCode EXPORT_DOC_REFERENCE_NOT_FOUND = new ErrorCode(1_030_033_003, "关联的{}不存在或不可用");
    ErrorCode EXPORT_DOC_ITEM_EMPTY = new ErrorCode(1_030_033_004, "定稿失败：单证套没有货物明细");
    ErrorCode EXPORT_DOC_CONTRACT_NOT_EXPORT = new ErrorCode(1_030_033_005, "该合同不是外销合同，不能生成出口单证");
    ErrorCode EXPORT_DOC_SOURCE_REQUIRED = new ErrorCode(1_030_033_006, "手工新建的单证套没有来源单据，不能重新同步");

    // ========== 单证明细 1-030-033-100 ~ 199 ==========
    ErrorCode EXPORT_DOC_ITEM_NOT_EXISTS = new ErrorCode(1_030_033_100, "明细行不存在");
    ErrorCode EXPORT_DOC_ITEM_INVALID_ID = new ErrorCode(1_030_033_101, "明细行 ID【{}】不属于本单证套，禁止跨单修改");

    // ========== 卖方配置 1-030-033-200 ~ 299 ==========
    ErrorCode EXPORT_DOC_SELLER_CONFIG_SAVE_FAILED = new ErrorCode(1_030_033_200, "卖方出口配置保存失败");

}
