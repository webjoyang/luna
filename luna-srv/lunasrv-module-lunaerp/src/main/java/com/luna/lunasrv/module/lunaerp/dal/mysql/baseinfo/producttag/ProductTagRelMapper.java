package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.producttag;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.producttag.ProductTagRelDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ProductTagRelMapper extends BaseMapperX<ProductTagRelDO> {

    default List<ProductTagRelDO> selectListByProductId(String productId) {
        return selectList(new LambdaQueryWrapperX<ProductTagRelDO>()
                .eq(ProductTagRelDO::getProductId, productId)
                .orderByAsc(ProductTagRelDO::getSortOrder));
    }

    default List<ProductTagRelDO> selectListByProductIds(Collection<String> productIds) {
        return selectList(new LambdaQueryWrapperX<ProductTagRelDO>()
                .in(ProductTagRelDO::getProductId, productIds)
                .orderByAsc(ProductTagRelDO::getSortOrder));
    }

    default int deleteByProductIds(Collection<String> productIds) {
        return delete(new LambdaQueryWrapperX<ProductTagRelDO>()
                .in(ProductTagRelDO::getProductId, productIds));
    }

    default int deleteByTagIds(Collection<String> tagIds) {
        return delete(new LambdaQueryWrapperX<ProductTagRelDO>()
                .in(ProductTagRelDO::getTagId, tagIds));
    }

    /**
     * 级联恢复产品标签关联（手写 XML：显式 UPDATE ... SET deleted = 0 WHERE product_id = #{productId} AND deleted = 1 AND tenant_id = #{tenantId}）
     */
    int restoreTagRelsByProductId(@Param("productId") String productId,
                                  @Param("tenantId") Long tenantId,
                                  @Param("updater") String updater,
                                  @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除产品标签关联（手写 XML：显式 DELETE WHERE product_id = #{productId} AND tenant_id = #{tenantId}）
     */
    int purgeTagRelsByProductId(@Param("productId") String productId,
                                @Param("tenantId") Long tenantId);

}
