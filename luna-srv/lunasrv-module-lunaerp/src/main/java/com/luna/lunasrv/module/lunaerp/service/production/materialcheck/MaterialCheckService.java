package com.luna.lunasrv.module.lunaerp.service.production.materialcheck;

import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckRespVO;

/**
 * 生产管理 - 齐套检查 Service 接口（只读试算，ADR-0041）
 *
 * @author luna
 */
public interface MaterialCheckService {

    /**
     * 执行齐套检查只读试算
     *
     * @param reqVO 试算请求参数
     * @return 试算结果
     */
    MaterialCheckRespVO runMaterialCheck(MaterialCheckReqVO reqVO);

}
