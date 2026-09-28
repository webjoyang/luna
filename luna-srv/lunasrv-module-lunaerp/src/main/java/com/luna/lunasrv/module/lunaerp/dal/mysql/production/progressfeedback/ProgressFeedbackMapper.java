package com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.ProgressFeedbackDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 生产管理-进度反馈缸表 Mapper
 *
 * @author luna
 */
@Mapper
public interface ProgressFeedbackMapper extends BaseMapperX<ProgressFeedbackDO> {

    default List<ProgressFeedbackDO> selectListByColorId(Long colorId) {
        if (colorId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ProgressFeedbackDO>()
                .eq(ProgressFeedbackDO::getDyeingColorId, colorId)
                .orderByAsc(ProgressFeedbackDO::getVatNo)
                .orderByAsc(ProgressFeedbackDO::getId));
    }

    default List<ProgressFeedbackDO> selectListByColorIds(Collection<Long> colorIds) {
        if (colorIds == null || colorIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ProgressFeedbackDO>()
                .in(ProgressFeedbackDO::getDyeingColorId, colorIds)
                .orderByAsc(ProgressFeedbackDO::getVatNo)
                .orderByAsc(ProgressFeedbackDO::getId));
    }

    default List<ProgressFeedbackDO> selectListByOrderId(Long orderId) {
        if (orderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ProgressFeedbackDO>()
                .eq(ProgressFeedbackDO::getDyeingOrderId, orderId)
                .orderByAsc(ProgressFeedbackDO::getVatNo)
                .orderByAsc(ProgressFeedbackDO::getId));
    }

    default Long selectCountByColorId(Long colorId) {
        if (colorId == null) {
            return 0L;
        }
        return selectCount(new LambdaQueryWrapperX<ProgressFeedbackDO>()
                .eq(ProgressFeedbackDO::getDyeingColorId, colorId));
    }

}
