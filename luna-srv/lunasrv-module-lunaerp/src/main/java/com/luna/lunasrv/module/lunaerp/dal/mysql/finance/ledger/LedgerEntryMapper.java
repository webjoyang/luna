package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 财务管理 - 经营分录账 Mapper（append-only 唯一账本）
 *
 * @author luna
 */
@Mapper
public interface LedgerEntryMapper extends BaseMapperX<LedgerEntryDO> {

    /**
     * 按业务回指主单查询分录列表
     *
     * @param refType 业务回指类型
     * @param refId 业务回指主单ID
     * @return 分录列表
     */
    default List<LedgerEntryDO> selectListByRef(String refType, Long refId) {
        return selectList(new LambdaQueryWrapper<LedgerEntryDO>()
                .eq(LedgerEntryDO::getRefType, refType)
                .eq(LedgerEntryDO::getRefId, refId)
                .orderByAsc(LedgerEntryDO::getId));
    }

    /**
     * 按业务回指主单与明细行查询分录列表
     *
     * @param refType 业务回指类型
     * @param refId 业务回指主单ID
     * @param refItemId 业务回指明细ID
     * @return 分录列表
     */
    default List<LedgerEntryDO> selectListByRefAndItemId(String refType, Long refId, Long refItemId) {
        return selectList(new LambdaQueryWrapper<LedgerEntryDO>()
                .eq(LedgerEntryDO::getRefType, refType)
                .eq(LedgerEntryDO::getRefId, refId)
                .eq(LedgerEntryDO::getRefItemId, refItemId)
                .orderByAsc(LedgerEntryDO::getId));
    }

    /**
     * 查询指定原分录的红冲分录列表
     *
     * @param reversalOfId 原分录ID
     * @return 红冲分录列表
     */
    default List<LedgerEntryDO> selectListByReversalOfId(Long reversalOfId) {
        return selectList(new LambdaQueryWrapper<LedgerEntryDO>()
                .eq(LedgerEntryDO::getReversalOfId, reversalOfId)
                .orderByAsc(LedgerEntryDO::getId));
    }

    /**
     * 批量查询指定原分录的红冲分录列表（防 N+1）
     *
     * @param reversalOfIds 原分录ID集合
     * @return 红冲分录列表
     */
    default List<LedgerEntryDO> selectListByReversalOfIds(java.util.Collection<Long> reversalOfIds) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(reversalOfIds)) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<LedgerEntryDO>()
                .in(LedgerEntryDO::getReversalOfId, reversalOfIds)
                .orderByAsc(LedgerEntryDO::getId));
    }

    /**
     * 统计指定资金账户的分录数量（用于账户删除保护）
     *
     * @param accountId 资金账户ID
     * @return 引用数量
     */
    default Long selectCountByAccountId(Long accountId) {
        return selectCount(new LambdaQueryWrapper<LedgerEntryDO>()
                .eq(LedgerEntryDO::getAccountId, accountId));
    }

}
