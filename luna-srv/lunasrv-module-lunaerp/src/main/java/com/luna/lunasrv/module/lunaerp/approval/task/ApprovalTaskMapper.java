package com.luna.lunasrv.module.lunaerp.approval.task;

import com.luna.lunasrv.framework.common.pojo.PageResult;
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

    default List<ApprovalTaskDO> selectListByInstanceIdAndNodeSort(Long instanceId, Integer nodeSort) {
        if (instanceId == null || nodeSort == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getInstanceId, instanceId)
                .eq(ApprovalTaskDO::getNodeSort, nodeSort)
                .orderByAsc(ApprovalTaskDO::getId));
    }

    default List<ApprovalTaskDO> selectPendingListByInstanceIdAndNodeSort(Long instanceId, Integer nodeSort) {
        if (instanceId == null || nodeSort == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ApprovalTaskDO>()
                .eq(ApprovalTaskDO::getInstanceId, instanceId)
                .eq(ApprovalTaskDO::getNodeSort, nodeSort)
                .eq(ApprovalTaskDO::getStatus, ApprovalTaskDO.STATUS_PENDING));
    }

    default PageResult<ApprovalTaskDO> selectTodoPage(Long approverId, com.luna.lunasrv.module.lunaerp.approval.task.vo.ApprovalTaskTodoPageReqVO reqVO) {
        com.github.yulichang.wrapper.MPJLambdaWrapper<ApprovalTaskDO> wrapper = new com.github.yulichang.wrapper.MPJLambdaWrapper<ApprovalTaskDO>()
                .selectAll(ApprovalTaskDO.class)
                .innerJoin(com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceDO.class,
                        com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceDO::getId,
                        ApprovalTaskDO::getInstanceId)
                .eq(ApprovalTaskDO::getApproverId, approverId)
                .eq(ApprovalTaskDO::getStatus, ApprovalTaskDO.STATUS_PENDING)
                .eq(cn.hutool.core.util.StrUtil.isNotBlank(reqVO.getBizType()),
                        com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceDO::getBizType, reqVO.getBizType())
                .like(cn.hutool.core.util.StrUtil.isNotBlank(reqVO.getBizNo()),
                        com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceDO::getBizNo, reqVO.getBizNo())
                .orderByDesc(ApprovalTaskDO::getId);
        return selectPage(reqVO, wrapper);
    }

}
