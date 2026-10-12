package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.exchangerate;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceAccountStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 财务管理 - 汇率主档 Mapper
 *
 * @author luna
 */
@Mapper
public interface ExchangeRateMapper extends BaseMapperX<ExchangeRateDO> {

    /**
     * 查询生效日 <= bizDate 且 status='ENABLE' 的最新一条汇率
     */
    default ExchangeRateDO selectLatestEffectiveRate(String currency, LocalDate bizDate) {
        return selectOne(new LambdaQueryWrapperX<ExchangeRateDO>()
                .eq(ExchangeRateDO::getCurrency, currency)
                .eq(ExchangeRateDO::getStatus, FinanceAccountStatusEnum.ENABLE.getValue())
                .le(ExchangeRateDO::getEffectiveDate, bizDate)
                .orderByDesc(ExchangeRateDO::getEffectiveDate)
                .orderByDesc(ExchangeRateDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 按币种与生效日精确查询（含软删行，防唯一索引冲突）
     */
    @Select("SELECT id, currency, rate, effective_date, status, remark, creator, create_time, "
            + "updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_exchange_rate "
            + "WHERE currency = #{currency} AND effective_date = #{effectiveDate} LIMIT 1")
    ExchangeRateDO selectByCurrencyAndEffectiveDateIncludingDeleted(@Param("currency") String currency,
                                                                    @Param("effectiveDate") LocalDate effectiveDate);

    /**
     * 按币种与生效日查询（未软删）
     */
    default ExchangeRateDO selectByCurrencyAndEffectiveDate(String currency, LocalDate effectiveDate) {
        return selectOne(new LambdaQueryWrapperX<ExchangeRateDO>()
                .eq(ExchangeRateDO::getCurrency, currency)
                .eq(ExchangeRateDO::getEffectiveDate, effectiveDate));
    }

}
