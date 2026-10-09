package com.luna.lunasrv.module.lunaerp.approval.config;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.approval.config.vo.ApprovalConfigPageReqVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 审批流程定义表 Mapper
 *
 * @author luna
 */
@Mapper
public interface ApprovalConfigMapper extends BaseMapperX<ApprovalConfigDO> {

    default PageResult<ApprovalConfigDO> selectPage(ApprovalConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ApprovalConfigDO>()
                .eqIfPresent(ApprovalConfigDO::getBizType, reqVO.getBizType())
                .likeIfPresent(ApprovalConfigDO::getName, reqVO.getName())
                .eqIfPresent(ApprovalConfigDO::getStatus, reqVO.getStatus())
                .orderByDesc(ApprovalConfigDO::getId));
    }

    default ApprovalConfigDO selectByBizTypeAndStatus(String bizType, String status) {
        return selectOne(new LambdaQueryWrapperX<ApprovalConfigDO>()
                .eq(ApprovalConfigDO::getBizType, bizType)
                .eq(ApprovalConfigDO::getStatus, status));
    }

    default List<ApprovalConfigDO> selectListByBizType(String bizType) {
        return selectList(new LambdaQueryWrapperX<ApprovalConfigDO>()
                .eq(ApprovalConfigDO::getBizType, bizType));
    }

}
