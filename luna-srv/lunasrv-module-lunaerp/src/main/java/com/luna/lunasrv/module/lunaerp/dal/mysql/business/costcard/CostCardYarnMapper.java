package com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard.CostCardYarnDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务管理 - 成本卡纱线明细行 Mapper
 *
 * @author luna
 */
@Mapper
public interface CostCardYarnMapper extends BaseMapperX<CostCardYarnDO> {

    /**
     * 按卡ID查询全部纱线明细（按 line_no 升序）
     */
    default List<CostCardYarnDO> selectListByCardId(Long cardId) {
        return selectList(new LambdaQueryWrapperX<CostCardYarnDO>()
                .eq(CostCardYarnDO::getCardId, cardId)
                .orderByAsc(CostCardYarnDO::getLineNo));
    }

    /**
     * 按卡ID逻辑删除全部纱线明细
     */
    default int deleteByCardId(Long cardId) {
        return delete(new LambdaQueryWrapperX<CostCardYarnDO>()
                .eq(CostCardYarnDO::getCardId, cardId));
    }

    /**
     * 级联恢复成本卡纱线明细
     */
    int restoreByCardId(@Param("cardId") Long cardId, @Param("tenantId") Long tenantId,
                        @Param("updater") String updater, @Param("now") LocalDateTime now);

    /**
     * 物理删除成本卡纱线明细
     */
    int purgeByCardId(@Param("cardId") Long cardId, @Param("tenantId") Long tenantId);

}
