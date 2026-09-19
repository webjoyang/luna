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

    /**
     * 获得染色单详情
     *
     * @param id 染色单ID
     * @return 染色单详情
     */
    DyeingOrderRespVO getDyeingOrder(Long id);

    /**
     * 更新染色单（整体保存）
     *
     * @param updateReqVO 更新信息
     */
    void updateDyeingOrder(DyeingOrderSaveReqVO updateReqVO);

    /**
     * 生效/下达染色单（DRAFT -> ACTIVE）
     *
     * @param id 染色单ID
     */
    void effectDyeingOrder(Long id);

    /**
     * 完成染色单（ACTIVE -> COMPLETED）
     *
     * @param id 染色单ID
     */
    void completeDyeingOrder(Long id);

    /**
     * 取消染色单（ACTIVE -> CANCELLED）
     *
     * @param id 染色单ID
     */
    void cancelDyeingOrder(Long id);

}
