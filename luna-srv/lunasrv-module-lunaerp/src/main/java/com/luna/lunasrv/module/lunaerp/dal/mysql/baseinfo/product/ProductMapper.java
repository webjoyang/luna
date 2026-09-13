package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapperX<ProductDO> {

    default ProductDO selectByCode(String code) {
        return selectOne(ProductDO::getCode, code);
    }

    /**
     * 查询当前租户下、指定编号前缀的全部产品（含非 DELETED 状态）。
     *
     * 逻辑删除（deleted=0）与租户条件由框架自动追加。
     *
     * @param prefix 编号前缀
     * @return 产品列表
     */
    default List<ProductDO> selectListByCodePrefixAndNotDeleted(String prefix) {
        return selectList(new LambdaQueryWrapperX<ProductDO>()
                .likeRight(ProductDO::getCode, prefix)
                .ne(ProductDO::getStatus, "DELETED"));
    }

}
