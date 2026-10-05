package com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPaymentReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPaymentStatsRespVO;
import com.luna.lunasrv.module.lunaerp.service.sample.sampleanalysis.SampleAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SampleAnalysisControllerTest {

    @Mock
    private SampleAnalysisService sampleAnalysisService;

    @InjectMocks
    private SampleAnalysisController sampleAnalysisController;

    @Test
    @DisplayName("Controller更新付款状态：委托Service并返回success(true)")
    void updateSampleAnalysisPayment_success() {
        SampleAnalysisPaymentReqVO reqVO = new SampleAnalysisPaymentReqVO(List.of(1L, 2L), true);
        doNothing().when(sampleAnalysisService).updateSampleAnalysisPayment(reqVO);

        CommonResult<Boolean> result = sampleAnalysisController.updateSampleAnalysisPayment(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(sampleAnalysisService).updateSampleAnalysisPayment(reqVO);
    }

    @Test
    @DisplayName("Controller获取付款统计：委托Service并返回统计结果")
    void getSampleAnalysisPaymentStats_success() {
        SampleAnalysisPageReqVO reqVO = new SampleAnalysisPageReqVO();
        reqVO.setKeyword("全棉");

        SampleAnalysisPaymentStatsRespVO statsRespVO = new SampleAnalysisPaymentStatsRespVO(
                new BigDecimal("60.00"),
                new BigDecimal("30.00"),
                3L
        );
        when(sampleAnalysisService.getSampleAnalysisPaymentStats(reqVO)).thenReturn(statsRespVO);

        CommonResult<SampleAnalysisPaymentStatsRespVO> result = sampleAnalysisController.getSampleAnalysisPaymentStats(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(statsRespVO, result.getData());
        verify(sampleAnalysisService).getSampleAnalysisPaymentStats(reqVO);
    }
}
