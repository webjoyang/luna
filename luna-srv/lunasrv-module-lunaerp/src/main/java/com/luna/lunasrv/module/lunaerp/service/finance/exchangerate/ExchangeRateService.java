package com.luna.lunasrv.module.lunaerp.service.finance.exchangerate;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRatePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.exchangerate.ExchangeRateDO;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务管理 - 汇率主档服务接口（SPOT 即期牌价）
 *
 * @author luna
 */
public interface ExchangeRateService {

    /**
     * 获取指定币种在业务日期的有效即期汇率（1 外币 = rate 本位币）
     * 规则：
     * 1. 人民币（CNY / RMB）内置 rate = 1.00000000，不建行直接返回；
     * 2. 其他外币：取生效日 <= bizDate 且 status='ENABLE' 的最新一条记录；
     * 3. 若查不到，抛出 1_030_020_200 EXCHANGE_RATE_NOT_FOUND 异常。
     *
     * @param currency 币种
     * @param bizDate 业务日期
     * @return 8位精度汇率
     */
    BigDecimal getRate(String currency, LocalDate bizDate);

    /**
     * 创建汇率主档记录
     * 规则：
     * 1. 本位币（CNY/RMB）禁止配置，抛出 1_030_020_202 EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN；
     * 2. 同租户同币种同生效日已存在抛出 1_030_020_201 EXCHANGE_RATE_NO_DUPLICATE。
     *
     * @param reqVO 保存请求
     * @return 汇率ID
     */
    Long createExchangeRate(ExchangeRateSaveReqVO reqVO);

    /**
     * 更新汇率主档（仅允许停用/启用及修改备注）
     *
     * @param reqVO 更新请求
     */
    void updateExchangeRate(ExchangeRateSaveReqVO reqVO);

    /**
     * 获取汇率详情
     *
     * @param id 汇率ID
     * @return 汇率DO
     */
    ExchangeRateDO getExchangeRate(Long id);

    /**
     * 分页查询汇率主档
     *
     * @param reqVO 分页条件
     * @return 分页结果
     */
    PageResult<ExchangeRateDO> getExchangeRatePage(ExchangeRatePageReqVO reqVO);

}
