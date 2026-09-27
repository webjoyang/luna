package com.luna.lunasrv.module.lunaerp.service.production.inspectionconfirmation;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationGenerateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationGenerateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationManualCreateReqVO;
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

    /**
     * 手工登记质检确认（直接落库已认定记录）
     *
     * @param reqVO 手工登记参数
     * @return 质检确认记录ID
     */
    Long manualCreateInspectionConfirmation(InspectionConfirmationManualCreateReqVO reqVO);

    /**
     * 从检验单幂等生成质检确认
     *
     * @param reqVO 检验单生成参数
     * @return 生成与跳过计数
     */
    InspectionConfirmationGenerateRespVO generateFromOrder(InspectionConfirmationGenerateReqVO reqVO);

}
