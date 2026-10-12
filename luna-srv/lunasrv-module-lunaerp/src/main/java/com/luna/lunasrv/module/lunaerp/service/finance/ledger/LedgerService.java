package com.luna.lunasrv.module.lunaerp.service.finance.ledger;

import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 财务管理 - 经营分录账服务接口（域公共 append-only）
 *
 * @author luna
 */
public interface LedgerService {

    /**
     * 追加经营分录（唯一写入通道）
     *
     * @param entry 分录实体
     * @return 分录ID
     */
    Long append(LedgerEntryDO entry);

    /**
     * 更新分录（禁止调用，直接抛出 LEDGER_IMMUTABLE 异常）
     *
     * @param entry 分录实体
     */
    void update(LedgerEntryDO entry);

    /**
     * 删除分录（禁止调用，直接抛出 LEDGER_IMMUTABLE 异常）
     *
     * @param id 分录ID
     */
    void delete(Long id);

    /**
     * 记录销售退货冲减分录（Issue 13 接缝契约）
     *
     * @param customerId 客户ID
     * @param shipmentItemId 原发货明细ID
     * @param returnQty 退货数量
     * @param origAmount 原币冲减金额
     * @param currency 币种
     * @param exchangeRate 原发货汇率快照
     * @param returnOrderId 退货单ID
     * @return 分录ID
     */
    Long recordArReturnReversal(Long customerId, Long shipmentItemId, BigDecimal returnQty,
                                BigDecimal origAmount, String currency, BigDecimal exchangeRate,
                                Long returnOrderId);

    /**
     * 按往来主体与日期区间查询分录列表
     */
    List<LedgerEntryDO> getEntriesByParty(String partyType, Long partyId, LocalDate startDate, LocalDate endDate);

    /**
     * 按业务单据回指查询分录列表
     */
    List<LedgerEntryDO> getEntriesByRef(String refType, Long refId);

    /**
     * 按业务源单ID查询所有分录
     */
    List<LedgerEntryDO> getEntriesByRefId(Long refId);

    /**
     * 按ID查询分录
     */
    LedgerEntryDO getEntryById(Long id);

    /**
     * 按红冲回指原分录ID查询
     */
    List<LedgerEntryDO> getEntriesByReversalOfId(Long reversalOfId);

    /**
     * 按资金账户与日期区间查询分录列表（资金流水视图）
     */
    List<LedgerEntryDO> getEntriesByAccount(Long accountId, LocalDate startDate, LocalDate endDate);

    /**
     * 统计指定资金账户引用的分录数量
     */
    long countEntriesByAccountId(Long accountId);

}
