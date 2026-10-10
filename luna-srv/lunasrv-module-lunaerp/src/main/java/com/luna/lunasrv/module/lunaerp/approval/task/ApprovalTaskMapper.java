package com.luna.lunasrv.module.lunaerp.approval.task;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 审批任务 Mapper
 *
 * @author luna
 */
@Mapper
public interface ApprovalTaskMapper extends BaseMapperX<ApprovalTaskDO> {

    default List<ApprovalTaskDO> selectListByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getInstanceId, instanceId)
                .orderByAsc(ApprovalTaskDO::getNodeSort)
                .orderByAsc(ApprovalTaskDO::getId));
    }

    default List<ApprovalTaskDO> selectPendingListByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getInstanceId, instanceId)
                .eq(ApprovalTaskDO::getStatus, ApprovalTaskDO.STATUS_PENDING));
    }

}
