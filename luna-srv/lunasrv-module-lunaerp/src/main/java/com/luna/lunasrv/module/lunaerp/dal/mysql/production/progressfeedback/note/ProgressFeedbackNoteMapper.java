package com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.note;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.note.ProgressFeedbackNoteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 生产管理-进度反馈记录流水表 Mapper
 *
 * @author luna
 */
@Mapper
public interface ProgressFeedbackNoteMapper extends BaseMapperX<ProgressFeedbackNoteDO> {

    default List<ProgressFeedbackNoteDO> selectListByFeedbackId(Long feedbackId) {
        if (feedbackId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ProgressFeedbackNoteDO>()
                .eq(ProgressFeedbackNoteDO::getFeedbackId, feedbackId)
                .orderByDesc(ProgressFeedbackNoteDO::getCreateTime)
                .orderByDesc(ProgressFeedbackNoteDO::getId));
    }

    default List<ProgressFeedbackNoteDO> selectListByFeedbackIds(Collection<Long> feedbackIds) {
        if (feedbackIds == null || feedbackIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ProgressFeedbackNoteDO>()
                .in(ProgressFeedbackNoteDO::getFeedbackId, feedbackIds)
                .orderByDesc(ProgressFeedbackNoteDO::getCreateTime));
    }

    default List<ProgressFeedbackNoteDO> selectUnresolvedListByFeedbackIds(Collection<Long> feedbackIds) {
        if (feedbackIds == null || feedbackIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ProgressFeedbackNoteDO>()
                .in(ProgressFeedbackNoteDO::getFeedbackId, feedbackIds)
                .eq(ProgressFeedbackNoteDO::getIssueFlag, true)
                .isNull(ProgressFeedbackNoteDO::getResolvedAt));
    }

}
