package com.luna.lunasrv.module.lunaerp.approval.instance;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批实例 Mapper
 *
 * @author luna
 */
@Mapper
public interface ApprovalInstanceMapper extends BaseMapperX<ApprovalInstanceDO> {

    default ApprovalInstanceDO selectLatestByBiz(String bizType, Long bizId) {
        return selectOne(new LambdaQueryWrapperX<ApprovalInstanceDO>()
                .eq(ApprovalInstanceDO::getBizType, bizType)
                .eq(ApprovalInstanceDO::getBizId, bizId)
                .orderByDesc(ApprovalInstanceDO::getId)
                .last("LIMIT 1"));
    }

}
