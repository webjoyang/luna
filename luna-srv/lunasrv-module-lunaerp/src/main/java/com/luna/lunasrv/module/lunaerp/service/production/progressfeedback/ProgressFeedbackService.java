package com.luna.lunasrv.module.lunaerp.service.production.progressfeedback;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackColorSectionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackStatisticsRespVO;

import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackAdvanceReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCalendarReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackCalendarRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.progressfeedback.vo.ProgressFeedbackUpdateReqVO;

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
     * 获得排缸日历扁平缸数据
     *
     * @param reqVO 筛选条件（month 或 from/to，可选 status、dyeingFactoryId）
     * @return 扁平缸列表响应
     */
    ProgressFeedbackCalendarRespVO getCalendar(ProgressFeedbackCalendarReqVO reqVO);

    /**
     * 新建反馈缸
     *
     * @param reqVO 建缸信息
     * @return 反馈缸 ID
     */
    Long createProgressFeedback(ProgressFeedbackCreateReqVO reqVO);

    /**
     * 获得反馈缸详情（缸字段 + 五阶段日期 + 颜色行/染色单实时信息 + 延期/覆盖率）
     *
     * @param id 反馈缸 ID
     * @return 反馈缸详情
     */
    ProgressFeedbackDetailRespVO getProgressFeedback(Long id);

    /**
     * 编辑反馈缸信息、阶段日期与状态回退
     *
     * @param reqVO 编辑信息
     */
    void updateProgressFeedback(ProgressFeedbackUpdateReqVO reqVO);

    /**
     * 推进反馈缸工序阶段（严格顺序逐格，自动盖当天日期戳）
     *
     * @param reqVO 推进参数
     */
    void advanceProgressFeedback(ProgressFeedbackAdvanceReqVO reqVO);

    /**
     * 删除反馈缸（同事务级联软删其下所有反馈记录）
     *
     * @param id 反馈缸 ID
     */
    void deleteProgressFeedback(Long id);

}
