package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * 财务管理 - 经营分录账 Mapper（域公共 append-only）
 *
 * @author luna
 */
@Mapper
public interface LedgerEntryMapper extends BaseMapperX<LedgerEntryDO> {

    /**
     * 按往来主体与日期区间查询分录列表
     */
    default List<LedgerEntryDO> selectListByParty(String partyType, Long partyId, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eqIfPresent(LedgerEntryDO::getPartyType, partyType)
                .eqIfPresent(LedgerEntryDO::getPartyId, partyId)
                .geIfPresent(LedgerEntryDO::getBizDate, startDate)
                .leIfPresent(LedgerEntryDO::getBizDate, endDate)
                .orderByAsc(LedgerEntryDO::getBizDate)
                .orderByAsc(LedgerEntryDO::getId));
    }

    /**
     * 按业务单据回指查询分录列表
     */
    default List<LedgerEntryDO> selectListByRef(String refType, Long refId) {
        return selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getRefType, refType)
                .eq(LedgerEntryDO::getRefId, refId)
                .orderByAsc(LedgerEntryDO::getId));
    }

    /**
     * 按业务单据及明细行回指查询分录
     */
    default LedgerEntryDO selectOneByRefItem(String refType, Long refId, Long refItemId) {
        return selectOne(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getRefType, refType)
                .eq(LedgerEntryDO::getRefId, refId)
                .eq(LedgerEntryDO::getRefItemId, refItemId)
                .orderByAsc(LedgerEntryDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 按业务源单ID查询所有分录（包含明细行与运费等）
     */
    default List<LedgerEntryDO> selectListByRefId(Long refId) {
        return selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getRefId, refId)
                .orderByAsc(LedgerEntryDO::getId));
    }

    /**
     * 按红冲回指原分录ID查询
     */
    default List<LedgerEntryDO> selectListByReversalOfId(Long reversalOfId) {
        return selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getReversalOfId, reversalOfId)
                .orderByAsc(LedgerEntryDO::getId));
    }

    /**
     * 按资金账户与日期区间查询分录列表（资金流水视图）
     */
    default List<LedgerEntryDO> selectListByAccount(Long accountId, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getAccountId, accountId)
                .geIfPresent(LedgerEntryDO::getBizDate, startDate)
                .leIfPresent(LedgerEntryDO::getBizDate, endDate)
                .orderByAsc(LedgerEntryDO::getBizDate)
                .orderByAsc(LedgerEntryDO::getId));
    }

    /**
     * 分页查询经营分录账
     */
    default com.luna.lunasrv.framework.common.pojo.PageResult<LedgerEntryDO> selectPage(com.luna.lunasrv.module.lunaerp.controller.finance.ledger.vo.LedgerPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LedgerEntryDO>()
                .eqIfPresent(LedgerEntryDO::getPartyType, reqVO.getPartyType())
                .eqIfPresent(LedgerEntryDO::getPartyId, reqVO.getPartyId())
                .eqIfPresent(LedgerEntryDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(LedgerEntryDO::getCategory, reqVO.getCategory())
                .eqIfPresent(LedgerEntryDO::getDirection, reqVO.getDirection())
                .eqIfPresent(LedgerEntryDO::getCurrency, reqVO.getCurrency())
                .geIfPresent(LedgerEntryDO::getBizDate, reqVO.getStartDate())
                .leIfPresent(LedgerEntryDO::getBizDate, reqVO.getEndDate())
                .orderByDesc(LedgerEntryDO::getBizDate)
                .orderByDesc(LedgerEntryDO::getId));
    }

    /**
     * 统计指定资金账户引用的分录数量
     */
    default Long selectCountByAccountId(Long accountId) {
        return selectCount(new LambdaQueryWrapperX<LedgerEntryDO>()
                .eq(LedgerEntryDO::getAccountId, accountId));
    }

}
