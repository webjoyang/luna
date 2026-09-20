package com.luna.lunasrv.module.lunaerp.service.production.stockledger;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * 生产管理-纱线库存流水台账 Service 接口
 *
 * 库存台账为 append-only 流水，无 CRUD 端点，仅由后端服务内部调用 + 暴露余额查询接口。
 *
 * @author luna
 */
public interface StockLedgerService {

    /**
     * 查询单规格纱线在库余额
     *
     * MVP = SUM(quantity) WHERE yarn_spec_id=? AND type='STOCK_IN' AND deleted=0
     *
     * @param yarnSpecId 纱线规格ID
     * @return 库存余额（无记录返回 0）
     */
    BigDecimal getBalance(Long yarnSpecId);

    /**
     * 批量查询纱线规格在库余额
     *
     * 一次性查询返回 map，key 为 yarnSpecId，value 为库存余额
     *
     * @param yarnSpecIds 纱线规格ID集合
     * @return 规格ID -> 库存余额 Map
     */
    Map<Long, BigDecimal> getBalances(Collection<Long> yarnSpecIds);

    /**
     * 写入一条 STOCK_IN 入库台账记录
     *
     * @param yarnSpecId 纱线规格ID
     * @param quantity 入库数量
     * @param relatedDocType 关联单据类型（如 YARN_ORDER）
     * @param relatedDocId 关联单据ID
     * @param batchNo 批号（可空）
     * @param remark 备注（可空）
     */
    void writeStockIn(Long yarnSpecId, BigDecimal quantity, String relatedDocType, Long relatedDocId, String batchNo, String remark);

    /**
     * 检查指定单据是否已写入台账（幂等检查）
     *
     * @param relatedDocType 关联单据类型
     * @param relatedDocId 关联单据ID
     * @return 是否已存在流水
     */
    boolean existsByRelatedDoc(String relatedDocType, Long relatedDocId);

}
