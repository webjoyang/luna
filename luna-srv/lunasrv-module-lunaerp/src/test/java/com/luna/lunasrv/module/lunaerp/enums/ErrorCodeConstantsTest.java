package com.luna.lunasrv.module.lunaerp.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 针对 {@link ErrorCodeConstants} 进度反馈（1-030-017-xxx 段）的单元测试
 */
public class ErrorCodeConstantsTest {

    @Test
    void testProgressFeedbackErrorCodes() {
        assertEquals(1_030_017_000, ErrorCodeConstants.PROGRESS_FEEDBACK_VAT_NOT_EXISTS.getCode());
        assertEquals("反馈缸不存在", ErrorCodeConstants.PROGRESS_FEEDBACK_VAT_NOT_EXISTS.getMsg());

        assertEquals(1_030_017_001, ErrorCodeConstants.PROGRESS_FEEDBACK_VAT_NO_DUPLICATE.getCode());
        assertTrue(ErrorCodeConstants.PROGRESS_FEEDBACK_VAT_NO_DUPLICATE.getMsg().contains("缸号"));

        assertEquals(1_030_017_002, ErrorCodeConstants.PROGRESS_FEEDBACK_DYEING_ORDER_STATUS_NOT_ALLOWED.getCode());

        assertEquals(1_030_017_003, ErrorCodeConstants.PROGRESS_FEEDBACK_DYEING_COLOR_NOT_EXISTS.getCode());

        assertEquals(1_030_017_004, ErrorCodeConstants.PROGRESS_FEEDBACK_COLOR_HAS_VATS.getCode());
        assertTrue(ErrorCodeConstants.PROGRESS_FEEDBACK_COLOR_HAS_VATS.getMsg().contains("缸进度反馈"));

        assertEquals(1_030_017_100, ErrorCodeConstants.PROGRESS_FEEDBACK_NOTE_NOT_EXISTS.getCode());
        assertEquals("反馈记录不存在", ErrorCodeConstants.PROGRESS_FEEDBACK_NOTE_NOT_EXISTS.getMsg());

        assertEquals(1_030_017_101, ErrorCodeConstants.PROGRESS_FEEDBACK_NOTE_ALREADY_RESOLVED.getCode());
        assertEquals("问题已闭环不可操作", ErrorCodeConstants.PROGRESS_FEEDBACK_NOTE_ALREADY_RESOLVED.getMsg());

        assertEquals(1_030_017_102, ErrorCodeConstants.PROGRESS_FEEDBACK_NOTE_NOT_AN_ISSUE.getCode());
        assertEquals("该记录非问题不可闭环", ErrorCodeConstants.PROGRESS_FEEDBACK_NOTE_NOT_AN_ISSUE.getMsg());
    }

    @Test
    void testSampleAnalysisErrorCodes() {
        assertEquals(1_030_040_000, SampleErrorCodeConstants.SAMPLE_ANALYSIS_NOT_EXISTS.getCode());
        assertEquals("样品分析不存在", SampleErrorCodeConstants.SAMPLE_ANALYSIS_NOT_EXISTS.getMsg());

        assertEquals(1_030_040_001, SampleErrorCodeConstants.SAMPLE_ANALYSIS_NO_DUPLICATE.getCode());
        assertTrue(SampleErrorCodeConstants.SAMPLE_ANALYSIS_NO_DUPLICATE.getMsg().contains("样品编号"));

        assertEquals(1_030_040_002, SampleErrorCodeConstants.SAMPLE_ANALYSIS_REF_NOT_EXISTS.getCode());
        assertEquals("关联引用不存在", SampleErrorCodeConstants.SAMPLE_ANALYSIS_REF_NOT_EXISTS.getMsg());

        assertEquals(1_030_040_003, SampleErrorCodeConstants.SAMPLE_ANALYSIS_LOSS_RATE_INVALID.getCode());
        assertTrue(SampleErrorCodeConstants.SAMPLE_ANALYSIS_LOSS_RATE_INVALID.getMsg().contains("损耗率"));

        assertEquals(1_030_040_004, SampleErrorCodeConstants.SAMPLE_ANALYSIS_PRODUCT_NAME_EMPTY.getCode());
        assertEquals("产品名称不能为空", SampleErrorCodeConstants.SAMPLE_ANALYSIS_PRODUCT_NAME_EMPTY.getMsg());

        assertEquals(1_030_040_005, SampleErrorCodeConstants.SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID.getCode());
        assertTrue(SampleErrorCodeConstants.SAMPLE_ANALYSIS_STITCH_ARRANGE_INVALID.getMsg().contains("三角排列画板"));

        assertEquals(1_030_040_006, SampleErrorCodeConstants.SAMPLE_ANALYSIS_PHOTO_COUNT_EXCEED.getCode());
        assertEquals("样品照片最多上传 9 张", SampleErrorCodeConstants.SAMPLE_ANALYSIS_PHOTO_COUNT_EXCEED.getMsg());

        assertEquals(1_030_040_100, SampleErrorCodeConstants.SAMPLE_ANALYSIS_YARN_SPEC_NOT_EXISTS.getCode());
        assertTrue(SampleErrorCodeConstants.SAMPLE_ANALYSIS_YARN_SPEC_NOT_EXISTS.getMsg().contains("纱线规格"));
    }

    @Test
    void testCommonFoundationErrorCodes() {
        // 回收站 1-030-031-000 ~ 099
        assertEquals(1_030_031_000, CommonErrorCodeConstants.RECYCLE_BIN_BIZ_TYPE_NOT_SUPPORTED.getCode());
        assertEquals("不支持的回收站数据类型【{}】", CommonErrorCodeConstants.RECYCLE_BIN_BIZ_TYPE_NOT_SUPPORTED.getMsg());

        assertEquals(1_030_031_001, CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS.getCode());
        assertEquals("记录不存在或不在回收站中", CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS.getMsg());

        assertEquals(1_030_031_002, CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode());
        assertEquals("引用的【{}】仍在回收站，请先恢复后再恢复本单据", CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE.getMsg());

        assertEquals(1_030_031_003, CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode());
        assertEquals("该数据已被业务引用，无法彻底删除", CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getMsg());

        // Excel 导入 1-030-031-100 ~ 199
        assertEquals(1_030_031_100, CommonErrorCodeConstants.EXCEL_IMPORT_ROW_LIMIT_EXCEEDED.getCode());
        assertEquals("导入数据超过单次上限 {} 行，请拆分后导入", CommonErrorCodeConstants.EXCEL_IMPORT_ROW_LIMIT_EXCEEDED.getMsg());

        assertEquals(1_030_031_101, CommonErrorCodeConstants.EXCEL_IMPORT_FILE_EMPTY.getCode());
        assertEquals("导入文件为空或无有效数据行", CommonErrorCodeConstants.EXCEL_IMPORT_FILE_EMPTY.getMsg());

        assertEquals(1_030_031_102, CommonErrorCodeConstants.EXCEL_IMPORT_PERMISSION_DENIED.getCode());
        assertEquals("导入人缺少【{}】权限，无法导入含该字段的数据", CommonErrorCodeConstants.EXCEL_IMPORT_PERMISSION_DENIED.getMsg());

        // 验证 ErrorCodeConstants 继承了所有 CommonErrorCodeConstants 错误码
        assertEquals(CommonErrorCodeConstants.RECYCLE_BIN_BIZ_TYPE_NOT_SUPPORTED, ErrorCodeConstants.RECYCLE_BIN_BIZ_TYPE_NOT_SUPPORTED);
        assertEquals(CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS, ErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS);
        assertEquals(CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE, ErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE);
        assertEquals(CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE, ErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        assertEquals(CommonErrorCodeConstants.EXCEL_IMPORT_ROW_LIMIT_EXCEEDED, ErrorCodeConstants.EXCEL_IMPORT_ROW_LIMIT_EXCEEDED);
        assertEquals(CommonErrorCodeConstants.EXCEL_IMPORT_FILE_EMPTY, ErrorCodeConstants.EXCEL_IMPORT_FILE_EMPTY);
        assertEquals(CommonErrorCodeConstants.EXCEL_IMPORT_PERMISSION_DENIED, ErrorCodeConstants.EXCEL_IMPORT_PERMISSION_DENIED);
    }

    @Test
    void testWeavingTicketPrintErrorCode() {
        assertEquals(1_030_015_117, ErrorCodeConstants.WEAVING_TICKET_PRINT_H5_BASE_URL_EMPTY.getCode());
        assertTrue(ErrorCodeConstants.WEAVING_TICKET_PRINT_H5_BASE_URL_EMPTY.getMsg().contains("luna.h5.base-url"));
    }

    @Test
    void testApprovalErrorCodes() {
        assertEquals(1_030_030_000, ApprovalErrorCodeConstants.APPROVAL_CONFIG_NOT_EXISTS.getCode());
        assertEquals("审批流程不存在", ApprovalErrorCodeConstants.APPROVAL_CONFIG_NOT_EXISTS.getMsg());

        assertEquals(1_030_030_001, ApprovalErrorCodeConstants.APPROVAL_CONFIG_NOT_ENABLED.getCode());
        assertEquals("该单据类型无启用的审批流程", ApprovalErrorCodeConstants.APPROVAL_CONFIG_NOT_ENABLED.getMsg());

        assertEquals(1_030_030_002, ApprovalErrorCodeConstants.APPROVAL_INSTANCE_NOT_EXISTS.getCode());
        assertEquals("审批实例不存在", ApprovalErrorCodeConstants.APPROVAL_INSTANCE_NOT_EXISTS.getMsg());

        assertEquals(1_030_030_003, ApprovalErrorCodeConstants.APPROVAL_TASK_NOT_EXISTS.getCode());
        assertEquals("审批任务不存在", ApprovalErrorCodeConstants.APPROVAL_TASK_NOT_EXISTS.getMsg());

        assertEquals(1_030_030_004, ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR.getCode());
        assertEquals("当前状态不允许该操作", ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR.getMsg());

        assertEquals(1_030_030_005, ApprovalErrorCodeConstants.APPROVAL_NO_PERMISSION.getCode());
        assertEquals("非当前审批人无权操作", ApprovalErrorCodeConstants.APPROVAL_NO_PERMISSION.getMsg());

        assertEquals(1_030_030_006, ApprovalErrorCodeConstants.APPROVAL_REJECT_COMMENT_EMPTY.getCode());
        assertEquals("驳回意见不能为空", ApprovalErrorCodeConstants.APPROVAL_REJECT_COMMENT_EMPTY.getMsg());

        assertEquals(1_030_030_007, ApprovalErrorCodeConstants.APPROVAL_CONFIG_BIZ_TYPE_ALREADY_ENABLED.getCode());
        assertEquals("该单据类型已有启用流程，请先停用", ApprovalErrorCodeConstants.APPROVAL_CONFIG_BIZ_TYPE_ALREADY_ENABLED.getMsg());

        assertEquals(1_030_030_008, ApprovalErrorCodeConstants.APPROVAL_CONFIG_HAS_ACTIVE_INSTANCE.getCode());
        assertEquals("流程已被引用，无法删除", ApprovalErrorCodeConstants.APPROVAL_CONFIG_HAS_ACTIVE_INSTANCE.getMsg());

        assertEquals(1_030_030_009, ApprovalErrorCodeConstants.APPROVAL_TRANSFER_TO_SELF_NOT_ALLOWED.getCode());
        assertEquals("转交接收人不能是自己", ApprovalErrorCodeConstants.APPROVAL_TRANSFER_TO_SELF_NOT_ALLOWED.getMsg());

        // 验证 ErrorCodeConstants 继承了所有 ApprovalErrorCodeConstants 错误码
        assertEquals(ApprovalErrorCodeConstants.APPROVAL_CONFIG_NOT_EXISTS, ErrorCodeConstants.APPROVAL_CONFIG_NOT_EXISTS);
        assertEquals(ApprovalErrorCodeConstants.APPROVAL_CONFIG_BIZ_TYPE_ALREADY_ENABLED, ErrorCodeConstants.APPROVAL_CONFIG_BIZ_TYPE_ALREADY_ENABLED);
        assertEquals(ApprovalErrorCodeConstants.APPROVAL_CONFIG_HAS_ACTIVE_INSTANCE, ErrorCodeConstants.APPROVAL_CONFIG_HAS_ACTIVE_INSTANCE);
    }

    @Test
    void testExportDocErrorCodes() {
        assertEquals(1_030_033_000, ExportDocErrorCodeConstants.EXPORT_DOC_NOT_EXISTS.getCode());
        assertEquals("出口单证套不存在", ExportDocErrorCodeConstants.EXPORT_DOC_NOT_EXISTS.getMsg());

        assertEquals(1_030_033_001, ExportDocErrorCodeConstants.EXPORT_DOC_NO_DUPLICATE.getCode());
        assertTrue(ExportDocErrorCodeConstants.EXPORT_DOC_NO_DUPLICATE.getMsg().contains("单证套号"));

        assertEquals(1_030_033_002, ExportDocErrorCodeConstants.EXPORT_DOC_STATUS_NOT_ALLOW.getCode());
        assertEquals("当前状态不允许该操作", ExportDocErrorCodeConstants.EXPORT_DOC_STATUS_NOT_ALLOW.getMsg());

        assertEquals(1_030_033_003, ExportDocErrorCodeConstants.EXPORT_DOC_REFERENCE_NOT_FOUND.getCode());
        assertTrue(ExportDocErrorCodeConstants.EXPORT_DOC_REFERENCE_NOT_FOUND.getMsg().contains("关联的"));

        assertEquals(1_030_033_004, ExportDocErrorCodeConstants.EXPORT_DOC_ITEM_EMPTY.getCode());
        assertEquals("定稿失败：单证套没有货物明细", ExportDocErrorCodeConstants.EXPORT_DOC_ITEM_EMPTY.getMsg());

        assertEquals(1_030_033_005, ExportDocErrorCodeConstants.EXPORT_DOC_CONTRACT_NOT_EXPORT.getCode());
        assertTrue(ExportDocErrorCodeConstants.EXPORT_DOC_CONTRACT_NOT_EXPORT.getMsg().contains("外销合同"));

        assertEquals(1_030_033_006, ExportDocErrorCodeConstants.EXPORT_DOC_SOURCE_REQUIRED.getCode());
        assertTrue(ExportDocErrorCodeConstants.EXPORT_DOC_SOURCE_REQUIRED.getMsg().contains("来源单据"));

        assertEquals(1_030_033_100, ExportDocErrorCodeConstants.EXPORT_DOC_ITEM_NOT_EXISTS.getCode());
        assertEquals("明细行不存在", ExportDocErrorCodeConstants.EXPORT_DOC_ITEM_NOT_EXISTS.getMsg());

        assertEquals(1_030_033_101, ExportDocErrorCodeConstants.EXPORT_DOC_ITEM_INVALID_ID.getCode());
        assertTrue(ExportDocErrorCodeConstants.EXPORT_DOC_ITEM_INVALID_ID.getMsg().contains("不属于本单证套"));

        assertEquals(1_030_033_200, ExportDocErrorCodeConstants.EXPORT_DOC_SELLER_CONFIG_SAVE_FAILED.getCode());
        assertEquals("卖方出口配置保存失败", ExportDocErrorCodeConstants.EXPORT_DOC_SELLER_CONFIG_SAVE_FAILED.getMsg());

        // 验证 ErrorCodeConstants 继承了所有 ExportDocErrorCodeConstants 错误码
        assertEquals(ExportDocErrorCodeConstants.EXPORT_DOC_NOT_EXISTS, ErrorCodeConstants.EXPORT_DOC_NOT_EXISTS);
        assertEquals(ExportDocErrorCodeConstants.EXPORT_DOC_ITEM_EMPTY, ErrorCodeConstants.EXPORT_DOC_ITEM_EMPTY);
        assertEquals(ExportDocErrorCodeConstants.EXPORT_DOC_SELLER_CONFIG_SAVE_FAILED, ErrorCodeConstants.EXPORT_DOC_SELLER_CONFIG_SAVE_FAILED);
    }

    @Test
    void testCostCardQuotationErrorCodes() {
        // 成本卡 1-030-034-000 ~ 099
        assertEquals(1_030_034_000, CostCardQuotationErrorCodeConstants.COST_CARD_NOT_EXISTS.getCode());
        assertEquals("成本卡不存在", CostCardQuotationErrorCodeConstants.COST_CARD_NOT_EXISTS.getMsg());

        assertEquals(1_030_034_001, CostCardQuotationErrorCodeConstants.COST_CARD_NO_DUPLICATE.getCode());
        assertTrue(CostCardQuotationErrorCodeConstants.COST_CARD_NO_DUPLICATE.getMsg().contains("成本卡号"));

        assertEquals(1_030_034_002, CostCardQuotationErrorCodeConstants.COST_CARD_STATUS_NOT_ALLOWED.getCode());
        assertEquals("成本卡当前状态不允许该操作", CostCardQuotationErrorCodeConstants.COST_CARD_STATUS_NOT_ALLOWED.getMsg());

        assertEquals(1_030_034_003, CostCardQuotationErrorCodeConstants.COST_CARD_MISSING_PRICE.getCode());
        assertTrue(CostCardQuotationErrorCodeConstants.COST_CARD_MISSING_PRICE.getMsg().contains("未取到价"));

        assertEquals(1_030_034_004, CostCardQuotationErrorCodeConstants.COST_CARD_RECIPE_BOM_MISSING.getCode());
        assertTrue(CostCardQuotationErrorCodeConstants.COST_CARD_RECIPE_BOM_MISSING.getMsg().contains("无有效配比 BOM"));

        assertEquals(1_030_034_005, CostCardQuotationErrorCodeConstants.COST_CARD_KG_METER_CONVERSION_MISSING.getCode());
        assertTrue(CostCardQuotationErrorCodeConstants.COST_CARD_KG_METER_CONVERSION_MISSING.getMsg().contains("无法进行千克/米换算"));

        // 报价单 1-030-034-100 ~ 199
        assertEquals(1_030_034_100, CostCardQuotationErrorCodeConstants.QUOTATION_NOT_EXISTS.getCode());
        assertEquals("报价单不存在", CostCardQuotationErrorCodeConstants.QUOTATION_NOT_EXISTS.getMsg());

        assertEquals(1_030_034_101, CostCardQuotationErrorCodeConstants.QUOTATION_NO_DUPLICATE.getCode());
        assertTrue(CostCardQuotationErrorCodeConstants.QUOTATION_NO_DUPLICATE.getMsg().contains("报价单号"));

        assertEquals(1_030_034_102, CostCardQuotationErrorCodeConstants.QUOTATION_STATUS_NOT_ALLOWED.getCode());
        assertEquals("报价单当前状态不允许该操作", CostCardQuotationErrorCodeConstants.QUOTATION_STATUS_NOT_ALLOWED.getMsg());

        assertEquals(1_030_034_103, CostCardQuotationErrorCodeConstants.QUOTATION_NOT_LATEST_VERSION.getCode());
        assertTrue(CostCardQuotationErrorCodeConstants.QUOTATION_NOT_LATEST_VERSION.getMsg().contains("仅最新已确认版本"));

        assertEquals(1_030_034_104, CostCardQuotationErrorCodeConstants.QUOTATION_DRAFT_EXISTS.getCode());
        assertTrue(CostCardQuotationErrorCodeConstants.QUOTATION_DRAFT_EXISTS.getMsg().contains("已存在草稿版本"));

        assertEquals(1_030_034_105, CostCardQuotationErrorCodeConstants.QUOTATION_FX_RATE_REQUIRED.getCode());
        assertEquals("外币报价必须填写汇率", CostCardQuotationErrorCodeConstants.QUOTATION_FX_RATE_REQUIRED.getMsg());

        assertEquals(1_030_034_106, CostCardQuotationErrorCodeConstants.QUOTATION_MARGIN_RATE_INVALID.getCode());
        assertTrue(CostCardQuotationErrorCodeConstants.QUOTATION_MARGIN_RATE_INVALID.getMsg().contains("毛利率必须介于"));

        assertEquals(1_030_034_107, CostCardQuotationErrorCodeConstants.QUOTATION_CURRENCY_MISMATCH.getCode());
        assertTrue(CostCardQuotationErrorCodeConstants.QUOTATION_CURRENCY_MISMATCH.getMsg().contains("与合同币种不一致"));

        // 纱价追加码 1-030-008-008 ~ 009
        assertEquals(1_030_008_008, CostCardQuotationErrorCodeConstants.YARN_PRICE_NOT_EXISTS.getCode());
        assertEquals("纱价记录不存在", CostCardQuotationErrorCodeConstants.YARN_PRICE_NOT_EXISTS.getMsg());

        assertEquals(1_030_008_009, CostCardQuotationErrorCodeConstants.YARN_PRICE_DUPLICATE_DATE.getCode());
        assertTrue(CostCardQuotationErrorCodeConstants.YARN_PRICE_DUPLICATE_DATE.getMsg().contains("已存在记录"));

        // 验证 ErrorCodeConstants 继承了所有 CostCardQuotationErrorCodeConstants 错误码
        assertEquals(CostCardQuotationErrorCodeConstants.COST_CARD_NOT_EXISTS, ErrorCodeConstants.COST_CARD_NOT_EXISTS);
        assertEquals(CostCardQuotationErrorCodeConstants.COST_CARD_RECIPE_BOM_MISSING, ErrorCodeConstants.COST_CARD_RECIPE_BOM_MISSING);
        assertEquals(CostCardQuotationErrorCodeConstants.QUOTATION_NOT_EXISTS, ErrorCodeConstants.QUOTATION_NOT_EXISTS);
        assertEquals(CostCardQuotationErrorCodeConstants.QUOTATION_CURRENCY_MISMATCH, ErrorCodeConstants.QUOTATION_CURRENCY_MISMATCH);
        assertEquals(CostCardQuotationErrorCodeConstants.YARN_PRICE_NOT_EXISTS, ErrorCodeConstants.YARN_PRICE_NOT_EXISTS);
        assertEquals(CostCardQuotationErrorCodeConstants.YARN_PRICE_DUPLICATE_DATE, ErrorCodeConstants.YARN_PRICE_DUPLICATE_DATE);
    }
}

