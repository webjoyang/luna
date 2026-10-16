package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.exchangerate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;

/**
 * 财务管理 - 汇率主档 Mapper
 *
 * @author luna
 */
@Mapper
public interface ExchangeRateMapper extends BaseMapperX<ExchangeRateDO> {

    /**
     * 查询生效日 <= 业务日的最新一条启用汇率
     *
     * @param currency 外币币种
     * @param bizDate 业务日期
     * @return 汇率记录
     */
    default ExchangeRateDO selectLatestEffectiveRate(String currency, LocalDate bizDate) {
        return selectOne(new LambdaQueryWrapper<ExchangeRateDO>()
                .eq(ExchangeRateDO::getCurrency, currency)
                .eq(ExchangeRateDO::getStatus, "ENABLE")
                .le(ExchangeRateDO::getEffectiveDate, bizDate)
                .orderByDesc(ExchangeRateDO::getEffectiveDate)
                .orderByDesc(ExchangeRateDO::getId)
                .last("LIMIT 1"));
    }

}
