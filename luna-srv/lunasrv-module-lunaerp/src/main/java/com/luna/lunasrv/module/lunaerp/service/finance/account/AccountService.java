package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.AccountUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import jakarta.validation.Valid;

import java.math.BigDecimal;

/**
 * 财务管理 - 资金账户 Service 接口（域公共最小集，ADR-0043）
 *
 * @author luna
 */
public interface AccountService {

    /**
     * 创建资金账户（opening_balance 仅此可写）
     *
     * @param createReqVO 创建参数
     * @return 账户ID
     */
    Long createAccount(@Valid AccountSaveReqVO createReqVO);

    /**
     * 修改资金账户（仅备注/状态/名称可改，期初与当前余额只读）
     *
     * @param updateReqVO 修改参数
     */
    void updateAccount(@Valid AccountUpdateReqVO updateReqVO);

    /**
     * 删除资金账户（存在分录引用的账户禁止删除）
     *
     * @param id 账户ID
     */
    void deleteAccount(Long id);

    /**
     * 条件变更账户余额（唯一变更通道）
     *
     * @param id 账户ID
     * @param amountDelta 金额变动（正数流入，负数流出）
     */
    void updateBalance(Long id, BigDecimal amountDelta);

    /**
     * 校验资金账户存在并返回
     *
     * @param id 账户ID
     * @return 账户DO
     */
    AccountDO validateAccountExists(Long id);

    /**
     * 校验资金账户可用（存在且状态为 ENABLE）
     *
     * @param id 账户ID
     * @return 账户DO
     */
    AccountDO validateAccountAvailable(Long id);

    /**
     * 查询资金账户详情
     *
     * @param id 账户ID
     * @return 账户详情VO
     */
    AccountRespVO getAccount(Long id);

}
