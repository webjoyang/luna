package com.luna.lunasrv.module.lunaerp.service.finance.ledger;

import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;

import java.util.Collection;
import java.util.List;

/**
 * 财务管理 - 经营分录账 Service 接口（append-only 唯一账本，ADR-0043）
 *
 * @author luna
 */
public interface LedgerService {

    /**
     * 追加经营分录（唯一写入通道）
     *
     * @param entry 分录数据对象
     * @return 分录ID
     */
    Long append(LedgerEntryDO entry);

    /**
     * 按业务回指查询分录列表
     *
     * @param refType 业务回指类型
     * @param refId 业务回指主单ID
     * @return 分录列表
     */
    List<LedgerEntryDO> getEntriesByRef(String refType, Long refId);

    /**
     * 按业务回指主单与明细行查询分录列表
     *
     * @param refType 业务回指类型
     * @param refId 业务回指主单ID
     * @param refItemId 业务回指明细ID
     * @return 分录列表
     */
    List<LedgerEntryDO> getEntriesByRefAndItemId(String refType, Long refId, Long refItemId);

    /**
     * 查询指定原分录的红冲分录列表
     *
     * @param reversalOfId 原分录ID
     * @return 红冲分录列表
     */
    List<LedgerEntryDO> getReversalEntries(Long reversalOfId);

    /**
     * 批量查询指定原分录集合的红冲分录列表（防 N+1）
     *
     * @param reversalOfIds 原分录ID集合
     * @return 红冲分录列表
     */
    List<LedgerEntryDO> getReversalEntriesByReversalOfIds(Collection<Long> reversalOfIds);

    /**
     * 查询资金账户被分录引用的数量
     *
     * @param accountId 资金账户ID
     * @return 引用数量
     */
    Long countEntriesByAccountId(Long accountId);

    /**
     * 追加销售退货应收冲减分录（13号销售退货冻结契约）
     *
     * @param customerId 客户ID
     * @param shipmentItemId 发货明细ID
     * @param returnQty 退货数量
     * @param origAmount 原币冲减金额
     * @param currency 币种
     * @param exchangeRate 原发货入账汇率快照
     * @param returnOrderId 退货单ID
     * @return 分录ID
     */
    Long recordArReturnReversal(Long customerId, Long shipmentItemId, java.math.BigDecimal returnQty,
                                java.math.BigDecimal origAmount, String currency,
                                java.math.BigDecimal exchangeRate, Long returnOrderId);

}

