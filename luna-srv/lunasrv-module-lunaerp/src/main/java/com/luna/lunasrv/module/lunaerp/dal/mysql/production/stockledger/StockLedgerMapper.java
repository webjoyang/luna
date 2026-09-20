package com.luna.lunasrv.module.lunaerp.dal.mysql.production.stockledger;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.stockledger.dto.StockBalanceDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 生产管理-纱线库存流水台账 Mapper
 *
 * @author luna
 */
@Mapper
public interface StockLedgerMapper extends BaseMapperX<StockLedgerDO> {

    /**
     * 查询单规格纱线入库余额汇总
     *
     * @param yarnSpecId 纱线规格ID
     * @return 余额
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM lunaerp_production_stock_ledger "
            + "WHERE yarn_spec_id = #{yarnSpecId} AND type = 'STOCK_IN' AND deleted = 0")
    BigDecimal selectBalanceByYarnSpecId(@Param("yarnSpecId") Long yarnSpecId);

    /**
     * 批量查询纱线规格入库余额汇总
     *
     * @param yarnSpecIds 纱线规格ID集合
     * @return 各规格余额汇总列表
     */
    @Select("<script>"
            + "SELECT yarn_spec_id AS yarnSpecId, COALESCE(SUM(quantity), 0) AS balance "
            + "FROM lunaerp_production_stock_ledger "
            + "WHERE yarn_spec_id IN "
            + "<foreach collection='yarnSpecIds' item='id' open='(' separator=',' close=')'>"
            + "#{id}"
            + "</foreach> "
            + "AND type = 'STOCK_IN' AND deleted = 0 "
            + "GROUP BY yarn_spec_id"
            + "</script>")
    List<StockBalanceDTO> selectBalancesByYarnSpecIds(@Param("yarnSpecIds") Collection<Long> yarnSpecIds);

    /**
     * 按关联单据类型与单据ID统计记录数
     *
     * @param relatedDocType 关联单据类型
     * @param relatedDocId 关联单据ID
     * @return 记录数
     */
    @Select("SELECT COUNT(1) FROM lunaerp_production_stock_ledger "
            + "WHERE related_doc_type = #{relatedDocType} AND related_doc_id = #{relatedDocId} AND deleted = 0")
    Long selectCountByRelatedDoc(@Param("relatedDocType") String relatedDocType,
                                 @Param("relatedDocId") Long relatedDocId);

}
