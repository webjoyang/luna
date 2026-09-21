package com.luna.lunasrv.module.lunaerp.service.production.inspectionorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;

/**
 * 生产管理-检验单 Service 接口
 *
 * @author luna
 */
public interface InspectionOrderService {

    /**
     * 获得检验单分页
     *
     * @param pageReqVO 分页查询
     * @return 检验单分页
     */
    PageResult<InspectionOrderRespVO> getInspectionOrderPage(InspectionOrderPageReqVO pageReqVO);

}
