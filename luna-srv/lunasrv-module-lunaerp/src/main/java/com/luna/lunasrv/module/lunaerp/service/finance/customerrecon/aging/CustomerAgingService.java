package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging;

import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryRespVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 客户 FIFO 账龄 Service 接口
 *
 * @author luna
 */
public interface CustomerAgingService {

    /**
     * 获得客户账龄汇总列表（按客户一行）
     *
     * @param reqVO 查询参数
     * @return 账龄汇总列表
     */
    List<CustomerAgingSummaryRespVO> getAgingSummary(@Valid CustomerAgingSummaryReqVO reqVO);

    /**
     * 获得客户未核销明细分录（下钻）
     *
     * @param reqVO 下钻查询参数
     * @return 未核销明细分录列表
     */
    List<CustomerAgingDetailRespVO> getAgingDetail(@Valid CustomerAgingDetailReqVO reqVO);
}
