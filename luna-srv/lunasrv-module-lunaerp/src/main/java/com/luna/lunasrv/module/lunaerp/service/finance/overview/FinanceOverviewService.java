package com.luna.lunasrv.module.lunaerp.service.finance.overview;

import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.GrossMarginReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.GrossMarginRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.OverviewSummaryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.overview.vo.OverviewSummaryRespVO;

/**
 * 财务管理 - 收支总览 Service 接口
 *
 * @author luna
 */
public interface FinanceOverviewService {

    /**
     * 获得收支总览汇总（账户期初/流入/流出/期末 + 类别汇总）
     *
     * @param reqVO 查询参数
     * @return 汇总结果
     */
    OverviewSummaryRespVO getOverviewSummary(OverviewSummaryReqVO reqVO);

    /**
     * 获得经营毛利参考（管理口径）
     *
     * @param reqVO 查询参数
     * @return 经营毛利参考
     */
    GrossMarginRespVO getGrossMargin(GrossMarginReqVO reqVO);

}
