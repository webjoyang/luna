package com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnprice;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo.YarnPricePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnprice.YarnPriceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产管理 - 纱价 Mapper
 *
 * @author luna
 */
@Mapper
public interface YarnPriceMapper extends BaseMapperX<YarnPriceDO> {

    default PageResult<YarnPriceDO> selectPage(YarnPricePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<YarnPriceDO>()
                .eqIfPresent(YarnPriceDO::getYarnSpecId, reqVO.getYarnSpecId())
                .eqIfPresent(YarnPriceDO::getYarnTypeId, reqVO.getYarnTypeId())
                .eqIfPresent(YarnPriceDO::getPriceType, reqVO.getPriceType())
                .geIfPresent(YarnPriceDO::getEffectiveDate, reqVO.getEffectiveDateGe())
                .leIfPresent(YarnPriceDO::getEffectiveDate, reqVO.getEffectiveDateLe())
                .orderByDesc(YarnPriceDO::getEffectiveDate)
                .orderByDesc(YarnPriceDO::getId));
    }

    default List<YarnPriceDO> selectListBySpec(Long yarnSpecId, String priceType) {
        return selectList(new LambdaQueryWrapperX<YarnPriceDO>()
                .eq(YarnPriceDO::getYarnSpecId, yarnSpecId)
                .eqIfPresent(YarnPriceDO::getPriceType, priceType)
                .orderByDesc(YarnPriceDO::getEffectiveDate)
                .orderByDesc(YarnPriceDO::getId));
    }

    /**
     * 查询同规格同价类同生效日期的未删除记录（查重）
     */
    default YarnPriceDO selectBySpecTypeAndDate(Long yarnSpecId, String priceType, LocalDate effectiveDate) {
        return selectOne(new LambdaQueryWrapperX<YarnPriceDO>()
                .eq(YarnPriceDO::getYarnSpecId, yarnSpecId)
                .eq(YarnPriceDO::getPriceType, priceType)
                .eq(YarnPriceDO::getEffectiveDate, effectiveDate)
                .last("LIMIT 1"));
    }

    /**
     * 取不晚于指定日期的最新一条生效价
     */
    default YarnPriceDO selectLatestEffectivePrice(Long yarnSpecId, String priceType, LocalDate date) {
        return selectOne(new LambdaQueryWrapperX<YarnPriceDO>()
                .eq(YarnPriceDO::getYarnSpecId, yarnSpecId)
                .eq(YarnPriceDO::getPriceType, priceType)
                .le(YarnPriceDO::getEffectiveDate, date)
                .orderByDesc(YarnPriceDO::getEffectiveDate)
                .orderByDesc(YarnPriceDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 查询最近一条已审核/存在的采购明细单价（只读聚合）
     */
    @Select("SELECT unit_price FROM lunaerp_production_yarn_order_item "
            + "WHERE yarn_spec_id = #{yarnSpecId} AND deleted = 0 "
            + "ORDER BY id DESC LIMIT 1")
    BigDecimal selectLatestPurchasedUnitPrice(@Param("yarnSpecId") Long yarnSpecId);

    /**
     * 回收站分页查询已软删纱价
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<?> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的纱价记录
     */
    YarnPriceDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复纱价记录
     */
    int restoreYarnPrice(@Param("id") Long id,
                         @Param("tenantId") Long tenantId,
                         @Param("updater") String updater,
                         @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除纱价记录
     */
    int purgeYarnPrice(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查纱价是否存在活跃业务引用
     */
    Long selectActiveReferenceCount(@Param("yarnPriceId") Long yarnPriceId, @Param("tenantId") Long tenantId);

}
