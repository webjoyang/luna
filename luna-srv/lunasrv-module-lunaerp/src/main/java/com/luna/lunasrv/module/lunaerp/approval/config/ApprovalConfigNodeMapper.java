package com.luna.lunasrv.module.lunaerp.approval.config;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 流程节点定义表 Mapper
 *
 * @author luna
 */
@Mapper
public interface ApprovalConfigNodeMapper extends BaseMapperX<ApprovalConfigNodeDO> {

    default List<ApprovalConfigNodeDO> selectListByConfigId(Long configId) {
        if (configId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ApprovalConfigNodeDO>()
                .eq(ApprovalConfigNodeDO::getConfigId, configId)
                .orderByAsc(ApprovalConfigNodeDO::getSort));
    }

    default List<ApprovalConfigNodeDO> selectListByConfigIds(Collection<Long> configIds) {
        if (configIds == null || configIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ApprovalConfigNodeDO>()
                .in(ApprovalConfigNodeDO::getConfigId, configIds)
                .orderByAsc(ApprovalConfigNodeDO::getSort));
    }

    default int deleteByConfigId(Long configId) {
        if (configId == null) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<ApprovalConfigNodeDO>()
                .eq(ApprovalConfigNodeDO::getConfigId, configId));
    }

}
