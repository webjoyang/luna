package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.task;

import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import com.luna.lunasrv.module.lunaerp.service.production.loom.LoomTaskReferenceProvider;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 织机任务引用检查实现类
 *
 * 织机删除守卫：检查指定织机是否被有效机台任务引用。
 *
 * @author luna
 */
@Component
public class MachineTaskLoomReferenceProviderImpl implements LoomTaskReferenceProvider {

    @Resource
    private MachineTaskMapper machineTaskMapper;

    @Override
    public boolean isLoomReferencedByTask(Long loomId) {
        if (loomId == null) {
            return false;
        }
        return machineTaskMapper.selectCountByLoomId(loomId) > 0;
    }

}
