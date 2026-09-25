package com.luna.lunasrv.module.lunaerp.service.production.greigeorder;

import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderWeavingProgressVO;

/**
 * 胚布单自织进度提供者接口
 *
 * 供织造模块（Issue 03+）实现，胚布单详情页实时聚合自织单进度时调用。
 *
 * @author luna
 */
public interface GreigeOrderWeavingProgressProvider {

    /**
     * 获取指定胚布单的织造进度（聚合其下有效织造单的计划匹数与已完工匹数）
     *
     * @param greigeOrderId 胚布单ID
     * @return 织造进度
     */
    GreigeOrderWeavingProgressVO getWeavingProgress(Long greigeOrderId);

}
