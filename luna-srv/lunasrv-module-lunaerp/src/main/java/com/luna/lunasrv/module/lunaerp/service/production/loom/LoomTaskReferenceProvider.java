package com.luna.lunasrv.module.lunaerp.service.production.loom;

/**
 * 织机任务引用检查提供者接口
 *
 * 供机台任务模块（Issue 04+）实现，织机删除时检查是否被机台任务引用。
 *
 * @author luna
 */
public interface LoomTaskReferenceProvider {

    /**
     * 检查指定织机是否被机台任务引用
     *
     * @param loomId 织机ID
     * @return 是否被引用
     */
    boolean isLoomReferencedByTask(Long loomId);

}
