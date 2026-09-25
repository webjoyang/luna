package com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import org.apache.ibatis.annotations.Mapper;

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

}
