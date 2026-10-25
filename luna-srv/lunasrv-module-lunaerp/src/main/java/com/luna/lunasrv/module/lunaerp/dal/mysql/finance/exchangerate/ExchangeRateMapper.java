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
    @org.apache.ibatis.annotations.Select("SELECT id, currency, rate, effective_date, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_exchange_rate "
            + "WHERE currency = #{currency} AND status = 'ENABLE' AND effective_date <= #{bizDate} AND deleted = 0 "
            + "ORDER BY effective_date DESC, id DESC LIMIT 1")
    ExchangeRateDO selectLatestEffectiveRate(@org.apache.ibatis.annotations.Param("currency") String currency,
                                             @org.apache.ibatis.annotations.Param("bizDate") LocalDate bizDate);

}
