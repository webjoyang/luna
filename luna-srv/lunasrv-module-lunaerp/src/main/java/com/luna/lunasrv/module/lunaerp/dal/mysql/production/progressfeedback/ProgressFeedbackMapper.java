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

    /**
     * 按染色单ID集合批量查询缸表（生产进度缸状态汇总/覆盖率聚合用，ADR-0034）。
     *
     * <p>返回未加工列表，状态汇总与延期判定由 Service 内存完成，便于单测。</p>
     *
     * @param dyeingOrderIds 染色单ID集合
     * @return 缸表列表
     */
    default List<ProgressFeedbackDO> selectListByDyeingOrderIds(Collection<Long> dyeingOrderIds) {
        if (dyeingOrderIds == null || dyeingOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ProgressFeedbackDO>()
                .in(ProgressFeedbackDO::getDyeingOrderId, dyeingOrderIds)
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

    default ProgressFeedbackDO selectByColorIdAndVatNo(Long colorId, String vatNo) {
        if (colorId == null || vatNo == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<ProgressFeedbackDO>()
                .eq(ProgressFeedbackDO::getDyeingColorId, colorId)
                .eq(ProgressFeedbackDO::getVatNo, vatNo));
    }

}
