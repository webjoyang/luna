package com.luna.lunasrv.module.lunaerp.service.sample.sampleanalysis;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPaymentReqVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisPaymentStatsRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisRespVO;
import com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo.SampleAnalysisSaveReqVO;
import jakarta.validation.Valid;

/**
 * 样品管理-样品分析 Service 接口
 *
 * @author luna
 */
public interface SampleAnalysisService {

    /**
     * 创建样品分析
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSampleAnalysis(@Valid SampleAnalysisSaveReqVO createReqVO);

    /**
     * 更新样品分析
     *
     * @param updateReqVO 更新信息
     */
    void updateSampleAnalysis(@Valid SampleAnalysisSaveReqVO updateReqVO);

    /**
     * 删除样品分析（逻辑删除）
     *
     * @param id 样品分析ID
     */
    void deleteSampleAnalysis(Long id);

    /**
     * 获得样品分析详情
     *
     * @param id 样品分析ID
     * @return 样品分析详情
     */
    SampleAnalysisRespVO getSampleAnalysis(Long id);

    /**
     * 获得样品分析分页
     *
     * @param pageReqVO 分页查询条件
     * @return 样品分析分页
     */
    PageResult<SampleAnalysisRespVO> getSampleAnalysisPage(SampleAnalysisPageReqVO pageReqVO);

    /**
     * 预览下一个样品编号（只读、不占号）
     *
     * @return 样品编号，如 SA20260001
     */
    String getNextSampleAnalysisNo();

    /**
     * 获得样品分析付款统计（未付金额/已付金额/任务总数）
     *
     * @param reqVO 查询条件（与分页同筛选口径）
     * @return 统计结果
     */
    SampleAnalysisPaymentStatsRespVO getSampleAnalysisPaymentStats(SampleAnalysisPageReqVO reqVO);

    /**
     * 更新样品分析付款状态（单条与批量共用）
     *
     * @param reqVO 付款更新请求
     */
    void updateSampleAnalysisPayment(@Valid SampleAnalysisPaymentReqVO reqVO);

}

