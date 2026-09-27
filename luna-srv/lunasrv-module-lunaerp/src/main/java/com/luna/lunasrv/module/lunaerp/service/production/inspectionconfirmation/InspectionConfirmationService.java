package com.luna.lunasrv.module.lunaerp.service.production.inspectionconfirmation;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationConfirmReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationGenerateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationGenerateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationManualCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRevokeReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRevokeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationPayrollSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationPrintViewRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.WorkerOptionRespVO;

import java.time.LocalDate;
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

    /**
     * 责任认定（状态由 PENDING 变为 CONFIRMED）
     *
     * @param reqVO 责任认定参数
     * @return 质检确认记录ID
     */
    Long confirmInspectionConfirmation(InspectionConfirmationConfirmReqVO reqVO);

    /**
     * 撤销重录（旧记录冻结为 REVOKED，克隆新 PENDING 记录挂同链）
     *
     * @param reqVO 撤销参数
     * @return 撤销结果（含已撤销ID与新克隆ID）
     */
    InspectionConfirmationRevokeRespVO revokeInspectionConfirmation(InspectionConfirmationRevokeReqVO reqVO);

    /**
     * 获得质检确认记录详情（附带同认定链全部历史记录）
     *
     * @param id 记录ID
     * @return 质检确认详情（含 chainRecords）
     */
    InspectionConfirmationRespVO getInspectionConfirmation(Long id);

    /**
     * 获得按日批量打印视图（按发现日 create_time 当日全部状态记录，正序）
     *
     * @param date 发现日期（缺省今天）
     * @return 质检确认记录列表
     */
    List<InspectionConfirmationPrintViewRespVO> getPrintView(LocalDate date);

    /**
     * 获得工资扣款汇总（按确认日 confirmed_at 当日已认定操作失误记录，按挡车工聚合）
     *
     * @param date 确认日期（缺省今天）
     * @return 工资扣款汇总结果
     */
    InspectionConfirmationPayrollSummaryRespVO getPayrollSummary(LocalDate date);

}

