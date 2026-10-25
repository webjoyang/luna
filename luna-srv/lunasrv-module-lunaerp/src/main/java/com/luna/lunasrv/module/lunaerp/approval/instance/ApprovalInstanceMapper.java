package com.luna.lunasrv.module.lunaerp.approval.instance;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceMyPageReqVO;
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

    default PageResult<ApprovalInstanceDO> selectMyPage(Long submitterId, ApprovalInstanceMyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ApprovalInstanceDO>()
                .eq(ApprovalInstanceDO::getSubmitterId, submitterId)
                .eq(StrUtil.isNotBlank(reqVO.getBizType()), ApprovalInstanceDO::getBizType, reqVO.getBizType())
                .like(StrUtil.isNotBlank(reqVO.getBizNo()), ApprovalInstanceDO::getBizNo, reqVO.getBizNo())
                .eq(StrUtil.isNotBlank(reqVO.getStatus()), ApprovalInstanceDO::getStatus, reqVO.getStatus())
                .orderByDesc(ApprovalInstanceDO::getId));
    }

}
