package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging;

import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryRespVO;

import java.util.List;

/**
 * 客户 FIFO 账龄分析服务接口
 *
 * @author luna
 */
public interface CustomerAgingService {

    /**
     * 获得客户账龄汇总列表（按客户一行）
     *
     * @param reqVO 查询参数
     * @return 汇总列表
     */
    List<CustomerAgingSummaryRespVO> getAgingSummary(CustomerAgingSummaryReqVO reqVO);

    /**
     * 获得单个客户的未核销账龄明细分录列表
     *
     * @param reqVO 下钻参数
     * @return 明细列表
     */
    List<CustomerAgingDetailRespVO> getAgingDetail(CustomerAgingDetailReqVO reqVO);

}
