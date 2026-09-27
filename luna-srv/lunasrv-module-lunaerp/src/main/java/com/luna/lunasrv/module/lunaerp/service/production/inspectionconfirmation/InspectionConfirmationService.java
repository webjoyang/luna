package com.luna.lunasrv.module.lunaerp.service.production.inspectionconfirmation;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.WorkerOptionRespVO;

import java.util.List;

/**
 * 生产管理-质检确认 Service 接口
 *
 * @author luna
 */
public interface InspectionConfirmationService {

    /**
     * 获得质检确认分页列表
     *
     * @param reqVO 分页查询条件
     * @return 质检确认分页结果
     */
    PageResult<InspectionConfirmationRespVO> getInspectionConfirmationPage(InspectionConfirmationPageReqVO reqVO);

    /**
     * 获得挡车工候选列表
     *
     * @return 挡车工用户候选列表
     */
    List<WorkerOptionRespVO> getWorkerOptions();

}
