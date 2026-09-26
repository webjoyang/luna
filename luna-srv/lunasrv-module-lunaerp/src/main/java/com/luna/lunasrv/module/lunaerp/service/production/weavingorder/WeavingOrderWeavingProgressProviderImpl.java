package com.luna.lunasrv.module.lunaerp.service.production.weavingorder;

import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderWeavingProgressVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.service.production.greigeorder.GreigeOrderWeavingProgressProvider;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 胚布单自织进度提供者实现类
 *
 * 聚合指定自织胚布单下有效织造单（非 CANCELLED、未删除）的计划匹数与已完工匹数（零回写、不落库，ADR-0022）。
 *
 * @author luna
 */
@Service
public class WeavingOrderWeavingProgressProviderImpl implements GreigeOrderWeavingProgressProvider {

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Override
    public GreigeOrderWeavingProgressVO getWeavingProgress(Long greigeOrderId) {
        if (greigeOrderId == null) {
            return new GreigeOrderWeavingProgressVO(0, 0);
        }
        List<WeavingOrderDO> orders = weavingOrderMapper.selectActiveListByGreigeOrderId(greigeOrderId);
        int plannedPieces = 0;
        int completedPieces = 0;
        for (WeavingOrderDO order : orders) {
            if (order.getPlannedQty() != null) {
                plannedPieces += order.getPlannedQty();
                if ("COMPLETED".equals(order.getStatus())) {
                    completedPieces += order.getPlannedQty();
                }
            }
        }
        return new GreigeOrderWeavingProgressVO(plannedPieces, completedPieces);
    }

}
