package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.producttag;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.producttag.ProductTagDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ProductTagMapper extends BaseMapperX<ProductTagDO> {

    default ProductTagDO selectByName(String name) {
        return selectOne(ProductTagDO::getName, name);
    }

    default List<ProductTagDO> selectListByNames(Collection<String> names) {
        return selectList(new LambdaQueryWrapperX<ProductTagDO>()
                .in(ProductTagDO::getName, names));
    }

    default List<ProductTagDO> selectListOrderByCreateTimeDesc() {
        return selectList(new LambdaQueryWrapperX<ProductTagDO>()
                .orderByDesc(ProductTagDO::getCreateTime));
    }

}
