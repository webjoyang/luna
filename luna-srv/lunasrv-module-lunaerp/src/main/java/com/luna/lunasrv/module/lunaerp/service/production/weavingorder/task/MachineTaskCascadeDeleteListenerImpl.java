package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.task;

import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.WeavingOrderCascadeDeleteListener;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 织造单级联删除机台任务监听器实现类
 *
 * 当织造单在待排产（DRAFT）状态被逻辑删除时触发，级联逻辑删除下属机台任务。
 *
 * @author luna
 */
@Component
public class MachineTaskCascadeDeleteListenerImpl implements WeavingOrderCascadeDeleteListener {

    @Resource
    private MachineTaskService machineTaskService;

    @Override
    public void onWeavingOrderDeleted(Long weavingOrderId) {
        if (weavingOrderId != null) {
            machineTaskService.deleteByWeavingOrderId(weavingOrderId);
        }
    }

}
