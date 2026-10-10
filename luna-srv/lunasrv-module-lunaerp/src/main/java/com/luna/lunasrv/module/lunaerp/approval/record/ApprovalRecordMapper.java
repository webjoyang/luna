package com.luna.lunasrv.module.lunaerp.approval.record;

import cn.hutool.core.util.StrUtil;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceDO;
import com.luna.lunasrv.module.lunaerp.approval.task.vo.ApprovalTaskDonePageReqVO;
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

    default PageResult<ApprovalRecordDO> selectDonePage(Long operatorId, ApprovalTaskDonePageReqVO reqVO) {
        MPJLambdaWrapper<ApprovalRecordDO> wrapper = new MPJLambdaWrapper<ApprovalRecordDO>()
                .selectAll(ApprovalRecordDO.class)
                .innerJoin(ApprovalInstanceDO.class,
                        ApprovalInstanceDO::getId,
                        ApprovalRecordDO::getInstanceId)
                .eq(ApprovalRecordDO::getOperatorId, operatorId);
        if (StrUtil.isNotBlank(reqVO.getAction())) {
            wrapper.eq(ApprovalRecordDO::getAction, reqVO.getAction());
        } else {
            wrapper.notIn(ApprovalRecordDO::getAction, List.of(ApprovalRecordDO.ACTION_SUBMIT, ApprovalRecordDO.ACTION_WITHDRAW));
        }
        wrapper.eq(StrUtil.isNotBlank(reqVO.getBizType()),
                        ApprovalInstanceDO::getBizType, reqVO.getBizType())
                .like(StrUtil.isNotBlank(reqVO.getBizNo()),
                        ApprovalInstanceDO::getBizNo, reqVO.getBizNo())
                .orderByDesc(ApprovalRecordDO::getOperateTime)
                .orderByDesc(ApprovalRecordDO::getId);
        return selectPage(reqVO, wrapper);
    }

}
