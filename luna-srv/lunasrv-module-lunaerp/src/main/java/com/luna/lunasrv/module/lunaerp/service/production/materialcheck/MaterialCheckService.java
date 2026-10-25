package com.luna.lunasrv.module.lunaerp.service.production.materialcheck;

import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckRespVO;

/**
 * 生产管理 - 齐套检查服务接口（只读试算，ADR-0041）
 *
 * @author luna
 */
public interface MaterialCheckService {

    /**
     * 运行齐套检查只读试算
     *
     * @param reqVO 试算请求参数（织造单集合、仓库集合、在途徽标开关）
     * @return 试算结果（聚合行、缺少BOM单、缺少重量单、汇总指标）
     */
    MaterialCheckRespVO calculateMaterialCheck(MaterialCheckReqVO reqVO);

}
