package com.luna.lunasrv.module.lunaerp.service.finance.ledger;

import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;

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
     * 更新分录（禁止操作，抛出 1_030_020_000 LEDGER_IMMUTABLE）
     *
     * @param entry 分录数据对象
     */
    void updateEntry(LedgerEntryDO entry);

    /**
     * 删除分录（禁止操作，抛出 1_030_020_000 LEDGER_IMMUTABLE）
     *
     * @param id 分录ID
     */
    void deleteEntry(Long id);

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
     * 查询资金账户被分录引用的数量
     *
     * @param accountId 资金账户ID
     * @return 引用数量
     */
    Long countEntriesByAccountId(Long accountId);

}
