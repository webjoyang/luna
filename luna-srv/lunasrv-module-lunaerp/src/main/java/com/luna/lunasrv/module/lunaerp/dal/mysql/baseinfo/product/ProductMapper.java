package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
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

    /**
     * 分页查询已软删产品（手写 XML：显式 WHERE deleted = 1 AND tenant_id = #{tenantId}）
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<?> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的产品记录
     */
    ProductDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复产品（并发条件更新带 deleted=1 AND tenant_id=?）
     */
    int restoreProduct(@Param("id") Long id,
                       @Param("tenantId") Long tenantId,
                       @Param("updater") String updater,
                       @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除产品（物理删除带 deleted=1 AND tenant_id=?）
     */
    int purgeProduct(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查产品是否存在活跃业务引用（deleted=0 且 tenant_id=?）
     */
    Long selectActiveReferenceCount(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

}
