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
     * 合同行预填（剩余可下达量 + 客户/合同已有打色单）
     *
     * @param contractId 销售合同ID
     * @return 预填信息
     */
    ReleaseFromContractRespVO getReleaseFromContract(Long contractId);

    /**
     * 下达生产（同事务按同产品分组创建胚布单、染色单、检验单，累加 released_qty）
     *
     * @param reqVO 下达请求
     * @return 生成单据结果
     */
    ReleaseProductionRespVO releaseProduction(ReleaseProductionReqVO reqVO);

    /**
     * 从染色单便利生成成品检验单
     *
     * @param dyeingOrderId 染色单ID
     * @return 检验单ID
     */
    Long createFinishedInspection(Long dyeingOrderId);

}
