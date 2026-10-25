package com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard.CostCardDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务管理 - 成本卡主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface CostCardMapper extends BaseMapperX<CostCardDO> {

    /**
     * 按卡号精确查询成本卡，【包含已逻辑删除的行】（ADR-0004）。
     */
    @DataPermission(enable = false)
    @Select("SELECT * FROM lunaerp_business_cost_card WHERE card_no = #{cardNo} LIMIT 1")
    CostCardDO selectByCardNoIncludingDeleted(@Param("cardNo") String cardNo);

    /**
     * 按卡号前缀查询成本卡，【包含已逻辑删除的行】（ADR-0004）。
     */
    @DataPermission(enable = false)
    @Select("SELECT * FROM lunaerp_business_cost_card WHERE card_no LIKE CONCAT(#{prefix}, '%')")
    List<CostCardDO> selectListByCardNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 将同产品的旧 EFFECTIVE 卡顶为 EXPIRED
     */
    @Update("UPDATE lunaerp_business_cost_card SET status = 'EXPIRED', updater = #{updater}, update_time = #{now} "
            + "WHERE product_id = #{productId} AND status = 'EFFECTIVE' AND deleted = 0")
    int expireEffectiveCardsByProductId(@Param("productId") Long productId,
                                        @Param("updater") String updater,
                                        @Param("now") LocalDateTime now);

    /**
     * 手工将生效卡失效（仅 EFFECTIVE -> EXPIRED 条件更新守卫）
     */
    @Update("UPDATE lunaerp_business_cost_card SET status = 'EXPIRED', updater = #{updater}, update_time = #{now} "
            + "WHERE id = #{id} AND status = 'EFFECTIVE' AND deleted = 0")
    int expireCardById(@Param("id") Long id,
                       @Param("updater") String updater,
                       @Param("now") LocalDateTime now);

    /**
     * 将草稿成本卡生效（仅 DRAFT -> EFFECTIVE 条件更新守卫）
     */
    @Update("UPDATE lunaerp_business_cost_card SET status = 'EFFECTIVE', effective_date = #{effectiveDate}, "
            + "updater = #{updater}, update_time = #{now} "
            + "WHERE id = #{id} AND status = 'DRAFT' AND deleted = 0")
    int effectCardById(@Param("id") Long id,
                       @Param("effectiveDate") LocalDate effectiveDate,
                       @Param("updater") String updater,
                       @Param("now") LocalDateTime now);

    /**
     * 查询指定产品的生效成本卡（至多一张）
     */
    default CostCardDO selectEffectiveByProductId(Long productId) {
        return selectOne(new LambdaQueryWrapperX<CostCardDO>()
                .eq(CostCardDO::getProductId, productId)
                .eq(CostCardDO::getStatus, "EFFECTIVE"));
    }

    /**
     * 回收站分页查询已软删草稿成本卡
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿成本卡
     */
    CostCardDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿成本卡
     */
    int restoreCostCard(@Param("id") Long id, @Param("tenantId") Long tenantId,
                        @Param("updater") String updater, @Param("now") LocalDateTime now);

    /**
     * 物理删除草稿成本卡
     */
    int purgeCostCard(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查成本卡是否存在活跃业务引用
     */
    Long selectActiveReferenceCount(@Param("cardId") Long cardId, @Param("tenantId") Long tenantId);

}
