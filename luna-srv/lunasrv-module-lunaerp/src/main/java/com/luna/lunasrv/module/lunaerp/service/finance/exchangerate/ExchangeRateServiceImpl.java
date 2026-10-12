package com.luna.lunasrv.module.lunaerp.service.finance.exchangerate;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRatePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.exchangerate.ExchangeRateMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceAccountStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

/**
 * 财务管理 - 汇率主档服务实现类（SPOT 即期牌价）
 *
 * @author luna
 */
@Slf4j
@Service
@Validated
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final BigDecimal BASE_RATE = BigDecimal.ONE.setScale(8, RoundingMode.UNNECESSARY);

    @Resource
    private ExchangeRateMapper exchangeRateMapper;

    @Override
    public BigDecimal getRate(String currency, LocalDate bizDate) {
        // 1. 人民币/本位币内置 rate=1
        if (isBaseCurrency(currency)) {
            return BASE_RATE;
        }

        if (bizDate == null) {
            bizDate = LocalDate.now();
        }

        // 2. 查找生效日 <= bizDate 且 status='ENABLE' 的最新一条
        ExchangeRateDO record = exchangeRateMapper.selectLatestEffectiveRate(currency, bizDate);
        if (record == null) {
            log.warn("[getRate] 未找到对应生效汇率: currency={}, bizDate={}", currency, bizDate);
            throw exception(EXCHANGE_RATE_NOT_FOUND);
        }

        return record.getRate().setScale(8, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createExchangeRate(ExchangeRateSaveReqVO reqVO) {
        String currency = reqVO.getCurrency();
        // 1. 本位币无需配置
        if (isBaseCurrency(currency)) {
            throw exception(EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN);
        }

        // 2. 唯一性校验（同币种同生效日，含软删行防撞唯一索引）
        ExchangeRateDO existing = exchangeRateMapper.selectByCurrencyAndEffectiveDateIncludingDeleted(
                currency, reqVO.getEffectiveDate());
        if (existing != null) {
            throw exception(EXCHANGE_RATE_NO_DUPLICATE);
        }

        String status = StrUtil.isNotBlank(reqVO.getStatus())
                ? reqVO.getStatus()
                : FinanceAccountStatusEnum.ENABLE.getValue();

        BigDecimal rate = reqVO.getRate().setScale(8, RoundingMode.HALF_UP);

        ExchangeRateDO rateDO = ExchangeRateDO.builder()
                .currency(currency)
                .rate(rate)
                .effectiveDate(reqVO.getEffectiveDate())
                .status(status)
                .remark(reqVO.getRemark())
                .build();

        exchangeRateMapper.insert(rateDO);
        log.info("[createExchangeRate] 创建汇率成功: id={}, currency={}, rate={}, effectiveDate={}",
                rateDO.getId(), currency, rate, reqVO.getEffectiveDate());
        return rateDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExchangeRate(ExchangeRateSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw exception(EXCHANGE_RATE_NOT_FOUND);
        }
        ExchangeRateDO existing = exchangeRateMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw exception(EXCHANGE_RATE_NOT_FOUND);
        }

        if (isBaseCurrency(existing.getCurrency())) {
            throw exception(EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN);
        }

        // 仅允许修改状态与备注
        ExchangeRateDO updateObj = new ExchangeRateDO();
        updateObj.setId(existing.getId());
        if (StrUtil.isNotBlank(reqVO.getStatus())) {
            updateObj.setStatus(reqVO.getStatus());
        }
        updateObj.setRemark(reqVO.getRemark());

        exchangeRateMapper.updateById(updateObj);
        log.info("[updateExchangeRate] 更新汇率成功: id={}, status={}", existing.getId(), reqVO.getStatus());
    }

    @Override
    public ExchangeRateDO getExchangeRate(Long id) {
        return exchangeRateMapper.selectById(id);
    }

    @Override
    public PageResult<ExchangeRateDO> getExchangeRatePage(ExchangeRatePageReqVO reqVO) {
        return exchangeRateMapper.selectPage(reqVO, new LambdaQueryWrapperX<ExchangeRateDO>()
                .eqIfPresent(ExchangeRateDO::getCurrency, reqVO.getCurrency())
                .eqIfPresent(ExchangeRateDO::getStatus, reqVO.getStatus())
                .orderByDesc(ExchangeRateDO::getEffectiveDate)
                .orderByDesc(ExchangeRateDO::getId));
    }

    private boolean isBaseCurrency(String currency) {
        return StrUtil.isBlank(currency)
                || "CNY".equalsIgnoreCase(currency)
                || "RMB".equalsIgnoreCase(currency);
    }

}
