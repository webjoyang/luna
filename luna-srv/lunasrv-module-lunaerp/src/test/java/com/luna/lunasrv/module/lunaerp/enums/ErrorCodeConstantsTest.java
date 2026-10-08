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
}
