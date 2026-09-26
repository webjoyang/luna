package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket;

import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.WeavingOrderCascadeDeleteListener;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 织造单级联删除布票监听器实现类
 *
 * 当织造单在待排产（DRAFT）状态被逻辑删除时触发，级联逻辑删除下属布票。
 *
 * @author luna
 */
@Component
public class WeavingTicketCascadeDeleteListenerImpl implements WeavingOrderCascadeDeleteListener {

    @Resource
    private WeavingTicketService weavingTicketService;

    @Override
    public void onWeavingOrderDeleted(Long weavingOrderId) {
        if (weavingOrderId != null) {
            weavingTicketService.deleteByWeavingOrderId(weavingOrderId);
        }
    }

}
