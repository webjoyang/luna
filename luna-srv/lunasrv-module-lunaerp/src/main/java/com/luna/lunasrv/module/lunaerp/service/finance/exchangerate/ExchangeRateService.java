package com.luna.lunasrv.module.lunaerp.service.finance.exchangerate;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRatePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateUpdateReqVO;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务管理 - 汇率主档 Service 接口（SPOT 即期牌价）
 *
 * @author luna
 */
public interface ExchangeRateService {

    /**
     * 创建汇率主档记录
     *
     * @param createReqVO 创建请求参数
     * @return 汇率记录ID
     */
    Long createExchangeRate(@Valid ExchangeRateSaveReqVO createReqVO);

    /**
     * 更新汇率主档（停用/修改备注/维护汇率）
     *
     * @param updateReqVO 更新请求参数
     */
    void updateExchangeRate(@Valid ExchangeRateUpdateReqVO updateReqVO);

    /**
     * 获得汇率主档记录详情
     *
     * @param id 记录ID
     * @return 汇率详情 VO
     */
    ExchangeRateRespVO getExchangeRate(Long id);

    /**
     * 分页查询汇率主档记录
     *
     * @param pageReqVO 分页查询请求参数
     * @return 分页结果
     */
    PageResult<ExchangeRateRespVO> getExchangeRatePage(ExchangeRatePageReqVO pageReqVO);

    /**
     * 获得指定币种在指定业务日的生效汇率
     * <p>
     * 规则：
     * 1. RMB 内置 rate = 1.00000000，不查库直接返回；
     * 2. 外币查询 effective_date <= bizDate 且 status = 'ENABLE' 的最新一条记录；
     * 3. 若无匹配汇率则抛出 1_030_020_200 EXCHANGE_RATE_NOT_FOUND。
     * </p>
     *
     * @param currency 币种（如 RMB, USD, EUR, HKD）
     * @param bizDate 业务日期
     * @return 入账汇率（1 外币 = rate 本位币）
     */
    BigDecimal getRate(String currency, LocalDate bizDate);

}
