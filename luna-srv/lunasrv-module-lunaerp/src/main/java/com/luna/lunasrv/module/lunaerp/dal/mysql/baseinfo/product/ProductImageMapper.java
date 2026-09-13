package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductImageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ProductImageMapper extends BaseMapperX<ProductImageDO> {

    default List<ProductImageDO> selectListByProductId(String productId) {
        return selectList(new LambdaQueryWrapperX<ProductImageDO>()
                .eq(ProductImageDO::getProductId, productId)
                .orderByAsc(ProductImageDO::getSortOrder));
    }

    default List<ProductImageDO> selectListByProductIds(Collection<String> productIds) {
        return selectList(new LambdaQueryWrapperX<ProductImageDO>()
                .in(ProductImageDO::getProductId, productIds)
                .orderByAsc(ProductImageDO::getSortOrder));
    }

    default int deleteByProductIds(Collection<String> productIds) {
        return delete(new LambdaQueryWrapperX<ProductImageDO>()
                .in(ProductImageDO::getProductId, productIds));
    }

}
