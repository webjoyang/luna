package com.luna.lunasrv.module.lunaerp.service.finance.exchangerate;

import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRatePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateUpdateReqVO;
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
import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.*;

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
    public Long createExchangeRate(ExchangeRateSaveReqVO createReqVO) {
        String currency = createReqVO.getCurrency() != null ? createReqVO.getCurrency().trim().toUpperCase() : "";
        if (isBaseCurrency(currency)) {
            throw exception(EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN);
        }

        // 唯一索引 (tenant_id, currency, effective_date) 校验
        ExchangeRateDO existing = exchangeRateMapper.selectOne(new LambdaQueryWrapperX<ExchangeRateDO>()
                .eq(ExchangeRateDO::getCurrency, currency)
                .eq(ExchangeRateDO::getEffectiveDate, createReqVO.getEffectiveDate()));
        if (existing != null) {
            throw exception(EXCHANGE_RATE_NO_DUPLICATE);
        }

        ExchangeRateDO rateDO = BeanUtils.toBean(createReqVO, ExchangeRateDO.class);
        rateDO.setCurrency(currency);
        // 新增汇率默认一律为 ENABLE 启用状态（契约最小化）
        rateDO.setStatus("ENABLE");
        exchangeRateMapper.insert(rateDO);
        log.info("[createExchangeRate] 创建汇率主档成功: id={}, currency={}, effectiveDate={}, rate={}",
                rateDO.getId(), currency, rateDO.getEffectiveDate(), rateDO.getRate());
        return rateDO.getId();
    }

    @Override
    public void updateExchangeRate(ExchangeRateUpdateReqVO updateReqVO) {
        ExchangeRateDO existing = exchangeRateMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw exception(EXCHANGE_RATE_NOT_FOUND);
        }

        ExchangeRateDO updateDO = new ExchangeRateDO();
        updateDO.setId(updateReqVO.getId());
        updateDO.setStatus(updateReqVO.getStatus());
        // 防纯停用静默清空：若未显式传 remark，保留原备注；若显式传了（包括空串置空），则更新
        updateDO.setRemark(updateReqVO.getRemark() != null ? updateReqVO.getRemark() : existing.getRemark());
        exchangeRateMapper.updateById(updateDO);
        log.info("[updateExchangeRate] 更新汇率主档成功: id={}, status={}, remark={}",
                updateReqVO.getId(), updateReqVO.getStatus(), updateDO.getRemark());
    }

    @Override
    public ExchangeRateRespVO getExchangeRate(Long id) {
        if (id == null) {
            throw exception(EXCHANGE_RATE_NOT_FOUND);
        }
        ExchangeRateDO rateDO = exchangeRateMapper.selectById(id);
        if (rateDO == null) {
            throw exception(EXCHANGE_RATE_NOT_FOUND);
        }
        return BeanUtils.toBean(rateDO, ExchangeRateRespVO.class);
    }

    @Override
    public PageResult<ExchangeRateRespVO> getExchangeRatePage(ExchangeRatePageReqVO pageReqVO) {
        String currency = StrUtil.isNotBlank(pageReqVO.getCurrency()) ? pageReqVO.getCurrency().trim().toUpperCase() : null;
        LambdaQueryWrapperX<ExchangeRateDO> queryWrapper = new LambdaQueryWrapperX<ExchangeRateDO>()
                .eqIfPresent(ExchangeRateDO::getCurrency, currency)
                .eqIfPresent(ExchangeRateDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(ExchangeRateDO::getEffectiveDate, pageReqVO.getEffectiveDate())
                .orderByDesc(ExchangeRateDO::getEffectiveDate)
                .orderByDesc(ExchangeRateDO::getId);

        PageResult<ExchangeRateDO> pageResult = exchangeRateMapper.selectPage(pageReqVO, queryWrapper);
        return BeanUtils.toBean(pageResult, ExchangeRateRespVO.class);
    }

    @Override
    public BigDecimal getRate(String currency, LocalDate bizDate) {
        if (StrUtil.isBlank(currency) || isBaseCurrency(currency)) {
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

    private boolean isBaseCurrency(String currency) {
        if (StrUtil.isBlank(currency)) {
            return false;
        }
        String upper = currency.trim().toUpperCase();
        return "RMB".equals(upper) || "CNY".equals(upper);
    }

}
