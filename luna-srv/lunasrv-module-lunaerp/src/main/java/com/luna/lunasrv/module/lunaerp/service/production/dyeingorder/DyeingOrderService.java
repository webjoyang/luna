package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderSaveReqVO;

/**
 * 生产管理-染色单 Service 接口
 *
 * @author luna
 */
public interface DyeingOrderService {

    /**
     * 获得染色单分页
     *
     * @param pageReqVO 分页查询
     * @return 染色单分页
     */
    PageResult<DyeingOrderRespVO> getDyeingOrderPage(DyeingOrderPageReqVO pageReqVO);

    /**
     * 创建染色单
     *
     * @param createReqVO 创建信息
     * @return 染色单ID
     */
    Long createDyeingOrder(DyeingOrderSaveReqVO createReqVO);

}
