package com.luna.lunasrv.module.lunaerp.service.production.weavingorder;

/**
 * 织造单级联逻辑删除监听器接口
 *
 * 供机台任务（Issue 04）、布票（Issue 05）实现，当织造单在待排产（DRAFT）状态被逻辑删除时触发。
 *
 * @author luna
 */
public interface WeavingOrderCascadeDeleteListener {

    /**
     * 当织造单被逻辑删除时触发，用于级联删除下属子资源（如机台任务、布票等）
     *
     * @param weavingOrderId 被删除的织造单ID
     */
    void onWeavingOrderDeleted(Long weavingOrderId);

}
