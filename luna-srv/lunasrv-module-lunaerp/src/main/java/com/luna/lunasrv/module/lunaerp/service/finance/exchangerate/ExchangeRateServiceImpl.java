package com.luna.lunasrv.module.lunaerp.service.finance.exchangerate;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.exchangerate.ExchangeRateMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 财务管理 - 汇率主档 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ExchangeRateServiceImpl implements ExchangeRateService {

    public static final BigDecimal RMB_RATE = BigDecimal.ONE.setScale(8, RoundingMode.UNNECESSARY);

    @Resource
    private ExchangeRateMapper exchangeRateMapper;

    @Override
    public BigDecimal getRate(String currency, LocalDate bizDate) {
        if (StrUtil.isBlank(currency) || "RMB".equalsIgnoreCase(currency) || "CNY".equalsIgnoreCase(currency)) {
            return RMB_RATE;
        }

        LocalDate effectiveDate = bizDate != null ? bizDate : LocalDate.now();
        ExchangeRateDO rateDO = exchangeRateMapper.selectLatestEffectiveRate(currency.toUpperCase(), effectiveDate);
        if (rateDO == null || rateDO.getRate() == null) {
            log.warn("[ExchangeRateService] 未找到指定币种生效汇率: currency={}, bizDate={}", currency, effectiveDate);
            throw exception(FinanceErrorCodeConstants.EXCHANGE_RATE_NOT_FOUND);
        }

        return rateDO.getRate();
    }

}
