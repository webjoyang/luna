package com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luna.lunasrv.framework.common.pojo.PageParam;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.mybatis.core.util.MyBatisUtils;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalancePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalanceRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.StockLedgerPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 库存管理 - 库存流水台账 Mapper（ADR-0017）
 *
 * @author luna
 */
@Mapper
public interface StockLedgerMapper extends BaseMapperX<StockLedgerDO> {

    /**
     * 按关联单据查询原始入库流水列表（按主键顺序升序）
     *
     * @param relatedDocType 关联单据类型（STOCK_IN）
     * @param relatedDocId 关联单据ID
     * @return 原流水列表
     */
    default List<StockLedgerDO> selectListByRelatedDoc(String relatedDocType, Long relatedDocId) {
        return selectListByRelatedDoc(relatedDocType, relatedDocId, null);
    }

    /**
     * 按关联单据及业务类型查询流水列表（按主键顺序升序）
     *
     * @param relatedDocType 关联单据类型
     * @param relatedDocId 关联单据ID
     * @param txnType 业务动作类型（可选）
     * @return 流水列表
     */
    default List<StockLedgerDO> selectListByRelatedDoc(String relatedDocType, Long relatedDocId, String txnType) {
        return selectList(new LambdaQueryWrapperX<StockLedgerDO>()
                .eq(StockLedgerDO::getRelatedDocType, relatedDocType)
                .eq(StockLedgerDO::getRelatedDocId, relatedDocId)
                .eqIfPresent(StockLedgerDO::getTxnType, txnType)
                .orderByAsc(StockLedgerDO::getId));
    }

    /**
     * 按关联单据类型 + 单据ID集合 + 业务动作类型批量查询流水（生产进度入库/发货量聚合用，ADR-0034）。
     *
     * <p>join 反查口径：不给流水表加合同列，按 related_doc_type + related_doc_id 关联源单，
     * 再由 Service 映射回合同。返回未加工列表（含带符号 quantity 与 unit），KG 过滤由 Service 完成。</p>
     *
     * @param relatedDocType 关联单据类型（如 STOCK_IN / SHIPMENT）
     * @param relatedDocIds  关联单据ID集合
     * @param txnType        业务动作类型（如 STOCK_IN / SHIPMENT_OUT），可空
     * @return 流水列表（按主键升序）
     */
    default List<StockLedgerDO> selectListByRelatedDocIds(String relatedDocType, Collection<Long> relatedDocIds, String txnType) {
        if (relatedDocIds == null || relatedDocIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<StockLedgerDO>()
                .eq(StockLedgerDO::getRelatedDocType, relatedDocType)
                .in(StockLedgerDO::getRelatedDocId, relatedDocIds)
                .eqIfPresent(StockLedgerDO::getTxnType, txnType)
                .orderByAsc(StockLedgerDO::getId));
    }

    /**
     * 统计指定仓库下某纱线规格的当前库存可用余额（SUM(quantity)）
     *
     * @param warehouseId 仓库ID
     * @param yarnSpecId 纱线规格ID
     * @return 当前可用余额（null 时由 COALESCE 兜底返回 0）
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM lunaerp_stock_ledger " +
            "WHERE warehouse_id = #{warehouseId} AND yarn_spec_id = #{yarnSpecId} AND deleted = 0")
    BigDecimal sumQuantityByWarehouseAndYarnSpec(@Param("warehouseId") Long warehouseId,
                                                 @Param("yarnSpecId") Long yarnSpecId);

    /**
     * 统计指定仓库下某产品在特定单位下的当前库存可用余额（SUM(quantity)）
     *
     * @param warehouseId 仓库ID
     * @param productId 产品ID
     * @param unit 单位
     * @return 当前可用余额（null 时由 COALESCE 兜底返回 0）
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM lunaerp_stock_ledger " +
            "WHERE warehouse_id = #{warehouseId} AND product_id = #{productId} AND unit = #{unit} AND deleted = 0")
    BigDecimal sumQuantityByWarehouseAndProductAndUnit(@Param("warehouseId") Long warehouseId,
                                                       @Param("productId") Long productId,
                                                       @Param("unit") String unit);

    /**
     * 实时聚合库存余额分页查询
     *
     * 按 item_type, product_id, yarn_spec_id, warehouse_id, unit 分组求和 SUM(quantity)；
     * 支持物品类型、仓库ID与关键词（产品编码/产品名称/纱线规格）筛选。
     *
     * @param page 分页参数
     * @param reqVO 筛选条件
     * @return 分页结果
     */
    @Select("<script>"
            + "SELECT item_type, "
            + "product_id, "
            + "MAX(product_code) AS product_code, "
            + "MAX(product_name) AS product_name, "
            + "yarn_spec_id, "
            + "MAX(yarn_spec_name) AS yarn_spec_name, "
            + "warehouse_id, "
            + "MAX(warehouse_name) AS warehouse_name, "
            + "unit, "
            + "SUM(quantity) AS quantity "
            + "FROM lunaerp_stock_ledger "
            + "WHERE deleted = 0 "
            + "<if test='reqVO.itemType != null and reqVO.itemType != \"\"'> AND item_type = #{reqVO.itemType} </if>"
            + "<if test='reqVO.warehouseId != null'> AND warehouse_id = #{reqVO.warehouseId} </if>"
            + "<if test='reqVO.keyword != null and reqVO.keyword != \"\"'> "
            + "AND (LOWER(product_code) LIKE LOWER(CONCAT('%', #{reqVO.keyword}, '%')) "
            + "OR LOWER(product_name) LIKE LOWER(CONCAT('%', #{reqVO.keyword}, '%')) "
            + "OR LOWER(yarn_spec_name) LIKE LOWER(CONCAT('%', #{reqVO.keyword}, '%'))) "
            + "</if>"
            + "<if test='(reqVO.keyword == null or reqVO.keyword == \"\") and reqVO.productKeyword != null and reqVO.productKeyword != \"\"'> "
            + "AND (LOWER(product_code) LIKE LOWER(CONCAT('%', #{reqVO.productKeyword}, '%')) "
            + "OR LOWER(product_name) LIKE LOWER(CONCAT('%', #{reqVO.productKeyword}, '%')) "
            + "OR LOWER(yarn_spec_name) LIKE LOWER(CONCAT('%', #{reqVO.productKeyword}, '%'))) "
            + "</if>"
            + "GROUP BY item_type, product_id, yarn_spec_id, warehouse_id, unit "
            + "ORDER BY item_type, product_id, yarn_spec_id, warehouse_id, unit"
            + "</script>")
    IPage<InventoryBalanceRespVO> selectBalancePage(IPage<?> page, @Param("reqVO") InventoryBalancePageReqVO reqVO);

    /**
     * 实时聚合库存余额分页查询封装
     *
     * @param reqVO 分页与筛选参数
     * @return PageResult
     */
    default PageResult<InventoryBalanceRespVO> selectBalancePage(InventoryBalancePageReqVO reqVO) {
        if (PageParam.PAGE_SIZE_NONE.equals(reqVO.getPageSize())) {
            Page<InventoryBalanceRespVO> page = new Page<>(1, -1);
            page.setSearchCount(false);
            selectBalancePage(page, reqVO);
            return new PageResult<>(page.getRecords(), (long) page.getRecords().size());
        }
        IPage<InventoryBalanceRespVO> mpPage = MyBatisUtils.buildPage(reqVO);
        selectBalancePage(mpPage, reqVO);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    /**
     * 库存流水台账只读分页查询
     *
     * @param reqVO 筛选与分页参数
     * @return PageResult
     */
    default PageResult<StockLedgerDO> selectLedgerPage(StockLedgerPageReqVO reqVO) {
        LambdaQueryWrapperX<StockLedgerDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(StockLedgerDO::getItemType, reqVO.getItemType())
                .eqIfPresent(StockLedgerDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(StockLedgerDO::getLocationId, reqVO.getLocationId())
                .eqIfPresent(StockLedgerDO::getTxnType, reqVO.getTxnType())
                .likeIfPresent(StockLedgerDO::getRelatedDocNo, reqVO.getRelatedDocNo())
                .betweenIfPresent(StockLedgerDO::getCreateTime, reqVO.getCreateTime());

        String keyword = StringUtils.hasText(reqVO.getKeyword()) ? reqVO.getKeyword().trim()
                : (StringUtils.hasText(reqVO.getProductKeyword()) ? reqVO.getProductKeyword().trim() : "");
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w.apply("LOWER(product_code) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(yarn_spec_name) LIKE LOWER({0})", like));
        }

        wrapper.orderByDesc(StockLedgerDO::getCreateTime)
                .orderByDesc(StockLedgerDO::getId);

        return selectPage(reqVO, wrapper);
    }

}
