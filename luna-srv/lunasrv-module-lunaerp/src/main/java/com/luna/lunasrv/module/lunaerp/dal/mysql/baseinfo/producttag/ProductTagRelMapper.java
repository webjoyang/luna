package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.producttag;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.producttag.ProductTagRelDO;
import org.apache.ibatis.annotations.Mapper;

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

}
