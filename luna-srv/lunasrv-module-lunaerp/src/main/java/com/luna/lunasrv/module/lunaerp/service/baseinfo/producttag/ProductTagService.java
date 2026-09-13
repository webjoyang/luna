package com.luna.lunasrv.module.lunaerp.service.baseinfo.producttag;

import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductTagRefVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.producttag.vo.ProductTagSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.producttag.ProductTagDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 基本信息-产品标签 Service 接口
 *
 * 同时承担产品写操作中的标签 co-update（复用/新建租户标签字典 + 重建产品-标签关联）。
 */
public interface ProductTagService {

    /**
     * 创建产品标签
     *
     * @param createReqVO 标签
     * @return 标签ID
     */
    Long createTag(ProductTagSaveReqVO createReqVO);

    /**
     * 修改产品标签
     *
     * @param updateReqVO 标签
     */
    void updateTag(ProductTagSaveReqVO updateReqVO);

    /**
     * 删除产品标签（同时逻辑删除该标签的全部产品关联）
     *
     * @param id 标签ID
     */
    void deleteTag(Long id);

    /**
     * 获得产品标签
     *
     * @param id 标签ID
     * @return 标签
     */
    ProductTagDO getTag(Long id);

    /**
     * 获得当前租户的全部产品标签（按创建时间倒序）
     *
     * @return 标签列表
     */
    List<ProductTagDO> getTagList();

    /**
     * 产品标签 co-update：按名称复用当前租户已有标签（保留原色），不存在则新建；
     * 删除该产品的旧关联，按列表顺序重建关联（sortOrder=下标）。
     *
     * 需与产品写操作在同一事务内执行。
     *
     * @param productId 产品ID
     * @param tags      目标标签列表（name/color）
     * @return 落库后的标签引用列表（id/name/color/sortOrder）
     */
    List<ProductTagRefVO> coUpdateProductTags(Long productId, List<ProductTagRefVO> tags);

    /**
     * 获得单个产品的标签列表（按关联 sortOrder 升序）
     *
     * @param productId 产品ID
     * @return 标签引用列表
     */
    List<ProductTagRefVO> getProductTags(Long productId);

    /**
     * 批量获得多个产品的标签列表（按关联 sortOrder 升序）
     *
     * @param productIds 产品ID集合
     * @return 产品ID → 标签引用列表
     */
    Map<Long, List<ProductTagRefVO>> getProductTagMap(Collection<Long> productIds);

}
