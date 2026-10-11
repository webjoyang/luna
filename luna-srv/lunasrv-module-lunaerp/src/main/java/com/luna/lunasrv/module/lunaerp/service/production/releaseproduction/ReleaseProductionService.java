package com.luna.lunasrv.module.lunaerp.service.production.releaseproduction;

import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseFromContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionRespVO;

/**
 * 生产管理 - 下达生产编排 Service 接口
 *
 * @author luna
 */
public interface ReleaseProductionService {

    /**
     * 从销售合同预填下达生产信息（剩余可下达量与已有打色单）
     *
     * @param contractId 销售合同ID
     * @return 预填信息
     */
    ReleaseFromContractRespVO getReleaseFromContract(Long contractId);

    /**
     * 销售合同下达生产
     *
     * @param reqVO 下达生产请求参数
     * @return 下达生成的单据信息
     */
    ReleaseProductionRespVO releaseProduction(ReleaseProductionReqVO reqVO);

}
