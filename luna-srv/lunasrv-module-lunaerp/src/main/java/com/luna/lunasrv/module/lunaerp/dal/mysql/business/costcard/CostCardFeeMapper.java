package com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard.CostCardFeeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务管理 - 成本卡费用明细行 Mapper
 *
 * @author luna
 */
@Mapper
public interface CostCardFeeMapper extends BaseMapperX<CostCardFeeDO> {

    /**
     * 按卡ID查询全部费用明细（按 line_no 升序）
     */
    default List<CostCardFeeDO> selectListByCardId(Long cardId) {
        return selectList(new LambdaQueryWrapperX<CostCardFeeDO>()
                .eq(CostCardFeeDO::getCardId, cardId)
                .orderByAsc(CostCardFeeDO::getLineNo));
    }

    /**
     * 按卡ID逻辑删除全部费用明细
     */
    default int deleteByCardId(Long cardId) {
        return delete(new LambdaQueryWrapperX<CostCardFeeDO>()
                .eq(CostCardFeeDO::getCardId, cardId));
    }

    /**
     * 级联恢复成本卡费用明细
     */
    int restoreByCardId(@Param("cardId") Long cardId, @Param("tenantId") Long tenantId,
                        @Param("updater") String updater, @Param("now") LocalDateTime now);

    /**
     * 物理删除成本卡费用明细
     */
    int purgeByCardId(@Param("cardId") Long cardId, @Param("tenantId") Long tenantId);

}
