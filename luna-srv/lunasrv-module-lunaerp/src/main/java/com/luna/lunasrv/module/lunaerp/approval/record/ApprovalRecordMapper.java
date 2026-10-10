package com.luna.lunasrv.module.lunaerp.approval.record;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 审批操作记录 Mapper
 *
 * @author luna
 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapperX<ApprovalRecordDO> {

    default List<ApprovalRecordDO> selectListByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ApprovalRecordDO>()
                .eq(ApprovalRecordDO::getInstanceId, instanceId)
                .orderByAsc(ApprovalRecordDO::getOperateTime)
                .orderByAsc(ApprovalRecordDO::getId));
    }

}
