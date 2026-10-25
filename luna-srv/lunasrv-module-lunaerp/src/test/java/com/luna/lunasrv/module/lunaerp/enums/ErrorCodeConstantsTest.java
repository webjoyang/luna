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

    @Test
    void testColorInstructionErrorCodes() {
        // 主表 1-030-032-000 ~ 099
        assertEquals(1_030_032_000, ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_NOT_EXISTS.getCode());
        assertEquals("打色单不存在", ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_NOT_EXISTS.getMsg());

        assertEquals(1_030_032_001, ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_NO_DUPLICATE.getCode());
        assertTrue(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_NO_DUPLICATE.getMsg().contains("打色单号"));

        assertEquals(1_030_032_002, ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode());
        assertEquals("打色单当前状态不允许该操作", ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getMsg());

        assertEquals(1_030_032_003, ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_REFERENCE_NOT_FOUND.getCode());
        assertTrue(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_REFERENCE_NOT_FOUND.getMsg().contains("关联引用不存在"));

        assertEquals(1_030_032_004, ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET.getCode());
        assertTrue(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET.getMsg().contains("存在未决颜色行"));

        assertEquals(1_030_032_005, ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE.getCode());
        assertTrue(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE.getMsg().contains("已被下游有效单据引用"));

        assertEquals(1_030_032_006, ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_LINE_FROZEN.getCode());
        assertTrue(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_LINE_FROZEN.getMsg().contains("颜色行当前状态不允许修改或删除"));

        assertEquals(1_030_032_007, ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT.getCode());
        assertEquals("灯源或确认类型非法", ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT.getMsg());

        // 颜色行 1-030-032-100 ~ 199
        assertEquals(1_030_032_100, ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_COLOR_NOT_EXISTS.getCode());
        assertEquals("颜色行不存在", ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_COLOR_NOT_EXISTS.getMsg());

        // 验证 ErrorCodeConstants 继承所有 ColorInstructionErrorCodeConstants 错误码
        assertEquals(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_NOT_EXISTS, ErrorCodeConstants.COLOR_INSTRUCTION_NOT_EXISTS);
        assertEquals(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_NO_DUPLICATE, ErrorCodeConstants.COLOR_INSTRUCTION_NO_DUPLICATE);
        assertEquals(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_STATUS_NOT_ALLOWED, ErrorCodeConstants.COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
        assertEquals(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_REFERENCE_NOT_FOUND, ErrorCodeConstants.COLOR_INSTRUCTION_REFERENCE_NOT_FOUND);
        assertEquals(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET, ErrorCodeConstants.COLOR_INSTRUCTION_CONFIRM_CONDITION_NOT_MET);
        assertEquals(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE, ErrorCodeConstants.COLOR_INSTRUCTION_HAS_DOWNSTREAM_REFERENCE);
        assertEquals(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_LINE_FROZEN, ErrorCodeConstants.COLOR_INSTRUCTION_LINE_FROZEN);
        assertEquals(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT, ErrorCodeConstants.COLOR_INSTRUCTION_INVALID_CONFIRM_TYPE_OR_LIGHT);
        assertEquals(ColorInstructionErrorCodeConstants.COLOR_INSTRUCTION_COLOR_NOT_EXISTS, ErrorCodeConstants.COLOR_INSTRUCTION_COLOR_NOT_EXISTS);
    }

    @Test
    void testMachineTaskConflictErrorCode() {
        assertEquals(1_030_036_000, ErrorCodeConstants.MACHINE_TASK_TIME_CONFLICT.getCode());
        assertTrue(ErrorCodeConstants.MACHINE_TASK_TIME_CONFLICT.getMsg().contains("计划时段冲突"));
    }

    @Test
    void testFinanceErrorCodes() {
        // 供应商对账 1-030-022-000 ~ 007
        assertEquals(1_030_022_000, ErrorCodeConstants.SUPPLIER_RECON_NOT_EXISTS.getCode());
        assertEquals("对账单不存在", ErrorCodeConstants.SUPPLIER_RECON_NOT_EXISTS.getMsg());
        assertEquals(1_030_022_001, ErrorCodeConstants.SUPPLIER_RECON_STATUS_NOT_ALLOWED.getCode());
        assertEquals(1_030_022_002, ErrorCodeConstants.SUPPLIER_RECON_SUPPLIER_NOT_EXISTS.getCode());
        assertEquals(1_030_022_003, ErrorCodeConstants.SUPPLIER_RECON_NO_SOURCE_ITEMS.getCode());
        assertEquals(1_030_022_004, ErrorCodeConstants.SUPPLIER_RECON_SOURCE_REFERENCED.getCode());
        assertEquals("源明细已被对账单【{}】引用", ErrorCodeConstants.SUPPLIER_RECON_SOURCE_REFERENCED.getMsg());
        assertEquals(1_030_022_005, ErrorCodeConstants.SUPPLIER_RECON_ADJUST_LINE_INVALID.getCode());
        assertEquals(1_030_022_006, ErrorCodeConstants.SUPPLIER_RECON_HAS_PAYMENT_APPLY.getCode());
        assertEquals(1_030_022_007, ErrorCodeConstants.SUPPLIER_RECON_APPLY_AMOUNT_EXCEED.getCode());
        assertEquals("累计申请金额【{}】超出对账确认金额", ErrorCodeConstants.SUPPLIER_RECON_APPLY_AMOUNT_EXCEED.getMsg());

        // 收付款与核销 1-030-023-000 ~ 007
        assertEquals(1_030_023_000, ErrorCodeConstants.PAYMENT_APPLY_NOT_EXISTS.getCode());
        assertEquals("付款申请不存在", ErrorCodeConstants.PAYMENT_APPLY_NOT_EXISTS.getMsg());
        assertEquals(1_030_023_001, ErrorCodeConstants.PAYMENT_APPLY_STATUS_NOT_ALLOWED.getCode());
        assertEquals(1_030_023_002, ErrorCodeConstants.PAYMENT_APPLY_RECON_NOT_CONFIRMED.getCode());
        assertEquals(1_030_023_003, ErrorCodeConstants.PAYMENT_ALLOCATE_EXCEED.getCode());
        assertEquals(1_030_023_004, ErrorCodeConstants.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH.getCode());
        assertEquals(1_030_023_005, ErrorCodeConstants.PAYMENT_PREPAY_BALANCE_NOT_ENOUGH.getCode());
        assertEquals(1_030_023_006, ErrorCodeConstants.PAYMENT_ACCOUNT_NOT_AVAILABLE.getCode());
        assertEquals(1_030_023_007, ErrorCodeConstants.PAYMENT_AMOUNT_INVALID.getCode());
        assertEquals(1_030_023_008, ErrorCodeConstants.PAYMENT_APPLY_NO_EXHAUSTED.getCode());
    }

    @Test
    void testPortalErrorCodes() {
        assertEquals(1_030_038_000, PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getCode());
        assertEquals("门户令牌无效或已过期", PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getMsg());

        assertEquals(1_030_038_001, PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode());
        assertEquals("门户数据归属不符", PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getMsg());

        assertEquals(1_030_038_002, PortalErrorCodeConstants.PORTAL_REQUEST_TOO_FREQUENT.getCode());
        assertEquals("门户请求过于频繁", PortalErrorCodeConstants.PORTAL_REQUEST_TOO_FREQUENT.getMsg());
    }

    @Test
    void testSalesReturnErrorCodes() {
        // 销售退货 1-030-037-000 ~ 014
        assertEquals(1_030_037_000, ErrorCodeConstants.SALES_RETURN_NOT_EXISTS.getCode());
        assertEquals("退货单不存在", ErrorCodeConstants.SALES_RETURN_NOT_EXISTS.getMsg());
        assertEquals(1_030_037_001, ErrorCodeConstants.SALES_RETURN_NO_DUPLICATE.getCode());
        assertEquals("退货单号【{}】已存在", ErrorCodeConstants.SALES_RETURN_NO_DUPLICATE.getMsg());
        assertEquals(1_030_037_002, ErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode());
        assertEquals(1_030_037_003, ErrorCodeConstants.SALES_RETURN_SHIPMENT_NOT_COMPLETED.getCode());
        assertEquals(1_030_037_004, ErrorCodeConstants.SALES_RETURN_ITEM_LINEAGE_INVALID.getCode());
        assertEquals(1_030_037_005, ErrorCodeConstants.SALES_RETURN_QTY_EXCEED_AVAILABLE.getCode());
        assertEquals(1_030_037_006, ErrorCodeConstants.SALES_RETURN_AMOUNT_EXCEED_AVAILABLE.getCode());
        assertEquals(1_030_037_007, ErrorCodeConstants.SALES_RETURN_ROLL_INVALID.getCode());
        assertEquals(1_030_037_008, ErrorCodeConstants.SALES_RETURN_APPROVAL_NOT_APPROVED.getCode());
        assertEquals(1_030_037_009, ErrorCodeConstants.SALES_RETURN_STOCK_IN_STATUS_INVALID.getCode());
        assertEquals(1_030_037_010, ErrorCodeConstants.SALES_RETURN_ROLL_QC_UNRESOLVED.getCode());
        assertEquals(1_030_037_011, ErrorCodeConstants.SALES_RETURN_RESTOCK_QTY_MISMATCH.getCode());
        assertEquals(1_030_037_012, ErrorCodeConstants.SALES_RETURN_STOCK_IN_NOT_COMPLETED.getCode());
        assertEquals(1_030_037_013, ErrorCodeConstants.SALES_RETURN_CURRENCY_MISMATCH.getCode());
        assertEquals(1_030_037_014, ErrorCodeConstants.SALES_RETURN_EXCHANGE_RATE_MISSING.getCode());
    }

    @Test
    void testWorkshopH5ErrorCodes() {
        // 车间移动 H5 1-030-039-001 ~ 009
        assertEquals(1_030_039_001, WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode());
        assertEquals("布票不存在或已作废", WorkshopH5ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getMsg());
        assertEquals(1_030_039_001, ErrorCodeConstants.H5_WEAVING_TICKET_NOT_EXISTS.getCode());

        assertEquals(1_030_039_002, WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER.getCode());
        assertEquals("该机台任务已派给其他挡车工，无权操作", WorkshopH5ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER.getMsg());
        assertEquals(1_030_039_002, ErrorCodeConstants.H5_MACHINE_TASK_ASSIGNED_TO_OTHER.getCode());

        assertEquals(1_030_039_003, WorkshopH5ErrorCodeConstants.H5_TICKET_NOT_COMPLETED.getCode());
        assertEquals("布票尚未完工，不能扫码质检", WorkshopH5ErrorCodeConstants.H5_TICKET_NOT_COMPLETED.getMsg());
        assertEquals(1_030_039_003, ErrorCodeConstants.H5_TICKET_NOT_COMPLETED.getCode());

        assertEquals(1_030_039_004, WorkshopH5ErrorCodeConstants.H5_TICKET_STATUS_NOT_ALLOW_INSPECTION.getCode());
        assertEquals("布票当前状态不可录检", WorkshopH5ErrorCodeConstants.H5_TICKET_STATUS_NOT_ALLOW_INSPECTION.getMsg());
        assertEquals(1_030_039_004, ErrorCodeConstants.H5_TICKET_STATUS_NOT_ALLOW_INSPECTION.getCode());

        assertEquals(1_030_039_005, WorkshopH5ErrorCodeConstants.H5_INSPECTION_ITEM_DUPLICATE.getCode());
        assertEquals("该缸号+卷号已存在录检记录", WorkshopH5ErrorCodeConstants.H5_INSPECTION_ITEM_DUPLICATE.getMsg());
        assertEquals(1_030_039_005, ErrorCodeConstants.H5_INSPECTION_ITEM_DUPLICATE.getCode());

        assertEquals(1_030_039_006, WorkshopH5ErrorCodeConstants.H5_YARN_REQUISITION_NOT_OWNED.getCode());
        assertEquals("领纱单不属于你的机台任务，无权操作", WorkshopH5ErrorCodeConstants.H5_YARN_REQUISITION_NOT_OWNED.getMsg());
        assertEquals(1_030_039_006, ErrorCodeConstants.H5_YARN_REQUISITION_NOT_OWNED.getCode());

        assertEquals(1_030_039_007, WorkshopH5ErrorCodeConstants.H5_DEFAULT_WAREHOUSE_NOT_CONFIGURED.getCode());
        assertEquals("未配置车间默认发纱仓库", WorkshopH5ErrorCodeConstants.H5_DEFAULT_WAREHOUSE_NOT_CONFIGURED.getMsg());
        assertEquals(1_030_039_007, ErrorCodeConstants.H5_DEFAULT_WAREHOUSE_NOT_CONFIGURED.getCode());

        assertEquals(1_030_039_008, WorkshopH5ErrorCodeConstants.H5_PHOTO_COUNT_EXCEED_OR_INVALID.getCode());
        assertEquals("照片数量超过 6 张或含非图片文件", WorkshopH5ErrorCodeConstants.H5_PHOTO_COUNT_EXCEED_OR_INVALID.getMsg());
        assertEquals(1_030_039_008, ErrorCodeConstants.H5_PHOTO_COUNT_EXCEED_OR_INVALID.getCode());

        assertEquals(1_030_039_009, WorkshopH5ErrorCodeConstants.H5_RESERVED_009.getCode());
        assertEquals("预留", WorkshopH5ErrorCodeConstants.H5_RESERVED_009.getMsg());
        assertEquals(1_030_039_009, ErrorCodeConstants.H5_RESERVED_009.getCode());
    }
}



