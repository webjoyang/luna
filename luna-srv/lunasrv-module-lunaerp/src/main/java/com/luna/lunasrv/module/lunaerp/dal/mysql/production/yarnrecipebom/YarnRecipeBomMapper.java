package com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 生产管理-纱线配比BOM主表 Mapper
 *
 * 唯一索引 (tenant_id, product_id) 包含软删行。
 * 查重扫描覆盖已删除行，手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
 *
 * @author luna
 */
@Mapper
public interface YarnRecipeBomMapper extends BaseMapperX<YarnRecipeBomDO> {

    /**
     * 分页查询配比BOM
     *
     * @param reqVO 分页与过滤参数
     * @return 配比BOM分页结果
     */
    default PageResult<YarnRecipeBomDO> selectPage(YarnRecipeBomPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<YarnRecipeBomDO>()
                .eqIfPresent(YarnRecipeBomDO::getProductId, reqVO.getProductId())
                .likeIfPresent(YarnRecipeBomDO::getProductCode, reqVO.getProductCode())
                .likeIfPresent(YarnRecipeBomDO::getProductName, reqVO.getProductName())
                .eqIfPresent(YarnRecipeBomDO::getStatus, reqVO.getStatus())
                .orderByDesc(YarnRecipeBomDO::getId));
    }

    /**
     * 按产品ID查询配比BOM（自动追加 deleted=0 逻辑删除过滤）
     *
     * @param productId 产品档案ID
     * @return 配比BOM DO，不存在返回 null
     */
    default YarnRecipeBomDO selectByProductId(Long productId) {
        return selectOne(YarnRecipeBomDO::getProductId, productId);
    }

    /**
     * 按产品ID列表查询配比BOM
     *
     * @param productIds 产品档案ID集合
     * @return 配比BOM列表
     */
    default java.util.List<YarnRecipeBomDO> selectListByProductIds(java.util.Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<YarnRecipeBomDO>()
                .in(YarnRecipeBomDO::getProductId, productIds));
    }

    /**
     * 按产品ID查询配比BOM，【包含已逻辑删除的行】。
     *
     * 唯一索引 (tenant_id, product_id) 包含软删行；
     * 手写 SQL 绕过逻辑删除自动过滤（deleted=0 不会追加）；
     * tenant_id 由租户拦截器自动追加，租户隔离不受影响。
     *
     * @param productId 产品档案ID
     * @return 配比BOM DO（含已删除行），不存在返回 null
     */
    @Select("SELECT id, product_id, product_code, product_name, remark, status, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_yarn_recipe_bom WHERE product_id = #{productId} LIMIT 1")
    YarnRecipeBomDO selectByProductIdIncludingDeleted(@Param("productId") Long productId);

}
