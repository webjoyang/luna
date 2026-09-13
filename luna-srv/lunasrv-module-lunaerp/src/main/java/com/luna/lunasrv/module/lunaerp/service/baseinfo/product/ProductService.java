package com.luna.lunasrv.module.lunaerp.service.baseinfo.product;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductTagRefVO;

import java.util.List;

/**
 * 基本信息-产品信息 Service 接口
 */
public interface ProductService {

    /**
     * 创建产品
     *
     * @param createReqVO 产品
     * @return 产品ID
     */
    Long createProduct(ProductSaveReqVO createReqVO);

    /**
     * 修改产品（图片整体替换；tags 非空时整体替换）
     *
     * @param updateReqVO 产品
     */
    void updateProduct(ProductSaveReqVO updateReqVO);

    /**
     * 删除产品（先置 status=DELETED，再逻辑删除产品，并级联逻辑删除图片与标签关联）
     *
     * @param id 产品ID
     */
    void deleteProduct(Long id);

    /**
     * 批量删除产品，语义同 {@link #deleteProduct(Long)}
     *
     * @param ids 产品ID列表
     */
    void deleteProductList(List<Long> ids);

    /**
     * 获得产品分页（每条附带图片与标签）
     *
     * @param reqVO 分页条件
     * @return 产品分页
     */
    PageResult<ProductRespVO> getProductPage(ProductPageReqVO reqVO);

    /**
     * 获得产品（附带图片与标签）
     *
     * @param id 产品ID
     * @return 产品
     */
    ProductRespVO getProduct(Long id);

    /**
     * 按产品编号获得产品（附带图片与标签）
     *
     * @param code 产品编号
     * @return 产品
     */
    ProductRespVO getProductByCode(String code);

    /**
     * 生成下一个产品编号：当前租户同前缀最大数字 + 1，4 位补零（排除已删除产品）
     *
     * @param prefix 编号前缀，为空时默认 P
     * @return 下一个产品编号
     */
    String getNextCode(String prefix);

    /**
     * 仅更新产品标签（标签 co-update）
     *
     * @param id   产品ID
     * @param tags 目标标签列表
     * @return 落库后的标签引用列表
     */
    List<ProductTagRefVO> updateProductTags(Long id, List<ProductTagRefVO> tags);

}
