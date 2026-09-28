package com.luna.lunasrv.module.lunaerp.service.production.progressfeedback;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackColorSectionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackStatisticsRespVO;

import java.util.List;

/**
 * 生产管理-进度反馈 Service 接口
 *
 * @author luna
 */
public interface ProgressFeedbackService {

    /**
     * 获得进度反馈覆盖层分页（以染色颜色明细行为骨架）
     *
     * @param pageReqVO 分页与筛选参数
     * @return 颜色行骨架节分页
     */
    PageResult<ProgressFeedbackColorSectionRespVO> getProgressFeedbackPage(ProgressFeedbackPageReqVO pageReqVO);

    /**
     * 根据染色单ID获得其下全部颜色行的进度反馈节列表（供染色单工作台页签使用）
     *
     * @param dyeingOrderId 染色单ID
     * @return 颜色行骨架节列表
     */
    List<ProgressFeedbackColorSectionRespVO> getProgressFeedbackListByOrder(Long dyeingOrderId);

    /**
     * 获得进度反馈统计条数据（与当前筛选同口径）
     *
     * @param reqVO 筛选参数
     * @return 统计条数据
     */
    ProgressFeedbackStatisticsRespVO getProgressFeedbackStatistics(ProgressFeedbackPageReqVO reqVO);

    /**
     * 新建反馈缸
     *
     * @param reqVO 建缸信息
     * @return 反馈缸 ID
     */
    Long createProgressFeedback(ProgressFeedbackCreateReqVO reqVO);

}
